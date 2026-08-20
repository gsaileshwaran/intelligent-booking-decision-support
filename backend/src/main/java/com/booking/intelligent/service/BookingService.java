package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.PaymentStatus;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.exception.InvalidBookingStateException;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.exception.SeatNotAvailableException;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${app.booking.hold-duration-ms:600000}")
    private long holdDurationMs;

    @Transactional
    public BookingResponse holdSeats(SeatHoldRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", request.getShowId()));

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new InvalidBookingStateException("Cannot book seats for a cancelled show.");
        }

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        if (show.getShowDate().isBefore(today) ||
           (show.getShowDate().isEqual(today) && show.getEndTime() != null && show.getEndTime().isBefore(nowTime))) {
            throw new InvalidBookingStateException("Cannot book seats for a show session that has already ended.");
        }

        // Concurrency Lock: Lock requested seats for update to prevent race conditions / double bookings
        List<ShowSeat> requestedSeats = showSeatRepository.findAllByIdForUpdate(request.getShowSeatIds());

        if (requestedSeats.size() != request.getShowSeatIds().size()) {
            throw new ResourceNotFoundException("One or more requested show seats do not exist.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime holdExpiryTime = now.plusSeconds(holdDurationMs / 1000);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingItem> bookingItems = new ArrayList<>();

        for (ShowSeat showSeat : requestedSeats) {
            if (showSeat.getStatus() != ShowSeatStatus.AVAILABLE) {
                if (showSeat.getStatus() == ShowSeatStatus.HELD && showSeat.getHeldUntil() != null && showSeat.getHeldUntil().isBefore(now)) {
                    // Previous hold expired, can be reclaimed
                } else {
                    throw new SeatNotAvailableException("Seat " + showSeat.getSeat().getRowLabel() + showSeat.getSeat().getSeatNumber() + " is no longer available.");
                }
            }

            showSeat.setStatus(ShowSeatStatus.HELD);
            showSeat.setHeldUntil(holdExpiryTime);
            showSeatRepository.save(showSeat);

            totalAmount = totalAmount.add(showSeat.getPrice());
        }

        Booking booking = Booking.builder()
                .user(user)
                .bookingRef("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(BookingStatus.HELD)
                .totalAmount(totalAmount)
                .createdAt(now)
                .build();

        for (ShowSeat showSeat : requestedSeats) {
            BookingItem item = BookingItem.builder()
                    .booking(booking)
                    .showSeat(showSeat)
                    .price(showSeat.getPrice())
                    .build();
            bookingItems.add(item);
        }

        booking.setItems(bookingItems);
        Booking savedBooking = bookingRepository.save(booking);

        return mapToBookingResponse(savedBooking, show, holdExpiryTime);
    }

    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access to booking details.");
        }

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
        return mapToBookingResponse(booking, show, null);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String paymentMethod, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access to booking.");
        }

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();

        // Idempotency: If booking is already CONFIRMED, return response directly
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return mapToBookingResponse(booking, show, null);
        }

        if (booking.getStatus() != BookingStatus.HELD && booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Booking cannot be confirmed from status: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if hold has expired
        for (BookingItem item : booking.getItems()) {
            ShowSeat showSeat = item.getShowSeat();
            if (showSeat.getHeldUntil() != null && showSeat.getHeldUntil().isBefore(now) && booking.getStatus() == BookingStatus.HELD) {
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);
                throw new InvalidBookingStateException("Seat hold time has expired. Please reselect your seats.");
            }
        }

        // Process Payment (Sandbox / Mock Integration)
        Payment payment = paymentRepository.findByBookingBookingId(booking.getBookingId()).orElse(null);
        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .paymentMethod(paymentMethod != null ? paymentMethod : "MOCK_CARD")
                    .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                    .status(PaymentStatus.SUCCESS)
                    .build();
            paymentRepository.save(payment);
        }

        // Update Booking Status to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        // Update Seats to CONFIRMED
        for (BookingItem item : booking.getItems()) {
            ShowSeat showSeat = item.getShowSeat();
            showSeat.setStatus(ShowSeatStatus.CONFIRMED);
            showSeat.setHeldUntil(null);
            showSeatRepository.save(showSeat);
        }

        return mapToBookingResponse(confirmedBooking, show, null);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access to booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already CANCELLED.");
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidBookingStateException("Expired bookings cannot be cancelled.");
        }

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
        if (show != null) {
            LocalDate today = LocalDate.now();
            LocalTime nowTime = LocalTime.now();
            if (show.getShowDate().isBefore(today) ||
               (show.getShowDate().isEqual(today) && show.getStartTime() != null && show.getStartTime().isBefore(nowTime))) {
                throw new InvalidBookingStateException("Cannot cancel a booking for a show session that has already passed.");
            }
        }

        // Release seats back to AVAILABLE
        for (BookingItem item : booking.getItems()) {
            ShowSeat showSeat = item.getShowSeat();
            showSeat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeat.setHeldUntil(null);
            showSeatRepository.save(showSeat);
        }

        // Refund payment if payment was made
        paymentRepository.findByBookingBookingId(bookingId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        });

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        return mapToBookingResponse(cancelledBooking, show, null);
    }

    public List<BookingResponse> getUserBookingHistory(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(booking -> {
            Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
            return mapToBookingResponse(booking, show, null);
        }).collect(Collectors.toList());
    }

    public List<BookingResponse> getProviderBookings(Long ownerUserId, Long branchId, String dateFilter, String statusFilter) {
        List<Booking> bookings = (branchId != null) 
                ? bookingRepository.findByTheatreId(branchId) 
                : bookingRepository.findByProviderOwnerId(ownerUserId);

        LocalDate today = LocalDate.now();

        return bookings.stream()
                .filter(b -> {
                    if (b.getItems().isEmpty()) return false;
                    Show show = b.getItems().get(0).getShowSeat().getShow();
                    if (show == null || show.getScreen() == null || show.getScreen().getTheatre() == null) return false;
                    if (!show.getScreen().getTheatre().getOwnerUser().getUserId().equals(ownerUserId)) return false;

                    if (branchId != null && !show.getScreen().getTheatre().getTheatreId().equals(branchId)) return false;

                    if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.isEmpty()) {
                        if (!b.getStatus().name().equalsIgnoreCase(statusFilter)) return false;
                    }

                    if (dateFilter != null && !dateFilter.equalsIgnoreCase("ALL") && !dateFilter.isEmpty()) {
                        LocalDate showDate = show.getShowDate();
                        if (showDate != null) {
                            if ("TODAY".equalsIgnoreCase(dateFilter) && !showDate.isEqual(today)) return false;
                            if ("UPCOMING".equalsIgnoreCase(dateFilter) && !showDate.isAfter(today)) return false;
                            if ("PAST".equalsIgnoreCase(dateFilter) && !showDate.isBefore(today)) return false;
                        }
                    }

                    return true;
                })
                .map(b -> {
                    Show show = b.getItems().isEmpty() ? null : b.getItems().get(0).getShowSeat().getShow();
                    return mapToBookingResponse(b, show, null);
                })
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowSeat> expiredSeats = showSeatRepository.findExpiredHolds(now);
        for (ShowSeat seat : expiredSeats) {
            seat.setStatus(ShowSeatStatus.AVAILABLE);
            seat.setHeldUntil(null);
            showSeatRepository.save(seat);
        }

        List<Booking> heldBookings = bookingRepository.findByStatus(BookingStatus.HELD);
        for (Booking b : heldBookings) {
            boolean allExpired = b.getItems().stream().allMatch(item -> 
                item.getShowSeat().getHeldUntil() == null || item.getShowSeat().getHeldUntil().isBefore(now)
            );
            if (allExpired && b.getCreatedAt().plusMinutes(10).isBefore(now)) {
                b.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(b);
            }
        }
    }

    public BookingResponse mapToBookingResponse(Booking booking, Show show, LocalDateTime holdExpiresAt) {
        List<BookingResponse.SeatDetailDto> seatDtos = booking.getItems().stream().map(item -> {
            Seat seat = item.getShowSeat().getSeat();
            return BookingResponse.SeatDetailDto.builder()
                    .showSeatId(item.getShowSeat().getShowSeatId())
                    .rowLabel(seat.getRowLabel())
                    .seatNumber(seat.getSeatNumber())
                    .seatType(seat.getSeatType().name())
                    .price(item.getPrice())
                    .build();
        }).collect(Collectors.toList());

        Payment payment = paymentRepository.findByBookingBookingId(booking.getBookingId()).orElse(null);

        String refundRef = null;
        BigDecimal refundedAmount = null;
        if (payment != null && payment.getStatus() == PaymentStatus.REFUNDED) {
            refundRef = "REFUND-" + String.format("%08X", booking.getBookingId().hashCode() & 0xFFFFFFFFL);
            refundedAmount = booking.getTotalAmount();
        }

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingRef(booking.getBookingRef())
                .userId(booking.getUser().getUserId())
                .customerName(booking.getUser().getName())
                .customerEmail(booking.getUser().getEmail())
                .showId(show != null ? show.getShowId() : null)
                .movieTitle(show != null ? show.getMovie().getTitle() : null)
                .theatreId(show != null ? show.getScreen().getTheatre().getTheatreId() : null)
                .theatreName(show != null ? show.getScreen().getTheatre().getName() : null)
                .screenName(show != null ? show.getScreen().getName() : null)
                .showDate(show != null ? show.getShowDate() : null)
                .startTime(show != null ? show.getStartTime() : null)
                .endTime(show != null ? show.getEndTime() : null)
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .holdExpiresAt(holdExpiresAt)
                .seats(seatDtos)
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .transactionRef(payment != null ? payment.getTransactionRef() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : null)
                .refundRef(refundRef)
                .refundedAmount(refundedAmount)
                .build();
    }
}
