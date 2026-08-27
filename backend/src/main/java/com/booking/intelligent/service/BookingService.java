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
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional
    public BookingResponse holdSeats(SeatHoldRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", request.getShowId()));

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new InvalidBookingStateException("Cannot reserve seats for a cancelled show.");
        }

        // Validate showtime has not passed
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        if (show.getShowDate() != null) {
            if (show.getShowDate().isBefore(today)) {
                throw new InvalidBookingStateException("Cannot reserve seats for a past showtime that has already ended.");
            }
            if (show.getShowDate().isEqual(today) && show.getStartTime() != null && show.getStartTime().isBefore(nowTime)) {
                throw new InvalidBookingStateException("Cannot reserve seats for a showtime that has already started or ended.");
            }
        }

        List<ShowSeat> selectedSeats = showSeatRepository.findAllById(request.getShowSeatIds());
        if (selectedSeats.size() != request.getShowSeatIds().size()) {
            throw new ResourceNotFoundException("ShowSeat", "ids", request.getShowSeatIds());
        }

        // Lock seats with PESSIMISTIC_WRITE
        List<ShowSeat> lockedSeats = showSeatRepository.findAllByIdForUpdate(request.getShowSeatIds());
        LocalDateTime holdUntil = LocalDateTime.now().plusMinutes(10);
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<ShowSeat> seatsToHold = new ArrayList<>();
        for (ShowSeat ss : lockedSeats) {
            if (ss.getStatus() != ShowSeatStatus.AVAILABLE) {
                if (ss.getStatus() == ShowSeatStatus.HELD && ss.getHeldUntil() != null && ss.getHeldUntil().isBefore(LocalDateTime.now())) {
                    ss.setStatus(ShowSeatStatus.AVAILABLE);
                    ss.setHeldUntil(null);
                } else {
                    throw new SeatNotAvailableException("Seat " + ss.getSeat().getRowLabel() + ss.getSeat().getSeatNumber() + " is no longer available.");
                }
            }

            ss.setStatus(ShowSeatStatus.HELD);
            ss.setHeldUntil(holdUntil);
            seatsToHold.add(showSeatRepository.save(ss));
            totalAmount = totalAmount.add(ss.getPrice());
        }

        // Create Booking record in HELD status
        Booking booking = Booking.builder()
                .bookingRef("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user)
                .status(BookingStatus.HELD)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingItem> items = new ArrayList<>();
        for (ShowSeat ss : seatsToHold) {
            BookingItem item = BookingItem.builder()
                    .booking(savedBooking)
                    .showSeat(ss)
                    .price(ss.getPrice())
                    .build();
            items.add(bookingItemRepository.save(item));
        }

        savedBooking.setItems(items);
        return mapToBookingResponse(savedBooking, show, holdUntil);
    }

    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access to booking.");
        }

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
        LocalDateTime holdUntil = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getHeldUntil();

        return mapToBookingResponse(booking, show, holdUntil);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String paymentMethod, Long userId) {
        return confirmBooking(bookingId, paymentMethod, null, userId);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String paymentMethod, String promoCode, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access to booking.");
        }

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
        if (show != null && show.getStatus() == ShowStatus.CANCELLED) {
            throw new InvalidBookingStateException("Cannot confirm booking for a cancelled show.");
        }

        // If booking is already CONFIRMED, return response directly
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return mapToBookingResponse(booking, show, null);
        }

        if (booking.getStatus() != BookingStatus.HELD && booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Booking cannot be confirmed from status: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if hold has expired
        if (booking.getStatus() == BookingStatus.HELD) {
            boolean expired = false;
            for (BookingItem item : booking.getItems()) {
                ShowSeat showSeat = item.getShowSeat();
                if (showSeat.getHeldUntil() != null && showSeat.getHeldUntil().isBefore(now)) {
                    expired = true;
                    break;
                }
            }
            if (!expired && booking.getCreatedAt() != null && booking.getCreatedAt().plusMinutes(10).isBefore(now)) {
                expired = true;
            }
            if (expired) {
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);
                for (BookingItem item : booking.getItems()) {
                    ShowSeat showSeat = item.getShowSeat();
                    showSeat.setStatus(ShowSeatStatus.AVAILABLE);
                    showSeat.setHeldUntil(null);
                    showSeatRepository.save(showSeat);
                }
                throw new InvalidBookingStateException("Seat hold time has expired. Please reselect your seats.");
            }
        }

        // Apply Server-Side Promo Discount if provided
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            BigDecimal currentTotal = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            String code = promoCode.trim().toUpperCase();

            if ("PVKWEEKEND".equals(code)) {
                discount = currentTotal.multiply(BigDecimal.valueOf(0.15));
            } else if ("FIRSTBOOK".equals(code)) {
                discount = BigDecimal.valueOf(50.00);
            } else if ("IMAXSPECIAL".equals(code)) {
                discount = BigDecimal.valueOf(30.00);
            }

            BigDecimal finalAmount = currentTotal.subtract(discount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }
            booking.setTotalAmount(finalAmount);
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

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        // Process Simulated Refund
        Payment payment = paymentRepository.findByBookingBookingId(bookingId).orElse(null);
        String refundRef = null;
        BigDecimal refundedAmount = booking.getTotalAmount();
        if (payment != null) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            refundRef = "REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        BookingResponse response = mapToBookingResponse(cancelledBooking, show, null);
        response.setRefundRef(refundRef);
        response.setRefundedAmount(refundedAmount);
        return response;
    }

    @Transactional
    public void cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowSeat> expired = showSeatRepository.findExpiredHolds(now);
        for (ShowSeat ss : expired) {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHeldUntil(null);
            showSeatRepository.save(ss);
        }
    }

    public List<BookingResponse> getUserBookingHistory(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(b -> {
            Show show = b.getItems().isEmpty() ? null : b.getItems().get(0).getShowSeat().getShow();
            LocalDateTime holdUntil = b.getItems().isEmpty() ? null : b.getItems().get(0).getShowSeat().getHeldUntil();
            return mapToBookingResponse(b, show, holdUntil);
        }).collect(Collectors.toList());
    }

    public List<BookingResponse> getProviderBookings(Long ownerUserId, String searchQuery, String statusFilter) {
        List<Booking> bookings = bookingRepository.findByProviderOwnerId(ownerUserId);
        return bookings.stream()
                .filter(b -> {
                    if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.isEmpty()) {
                        if (!b.getStatus().name().equalsIgnoreCase(statusFilter)) return false;
                    }
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String q = searchQuery.toLowerCase();
                        boolean matchRef = b.getBookingRef() != null && b.getBookingRef().toLowerCase().contains(q);
                        boolean matchCustomer = b.getUser().getName() != null && b.getUser().getName().toLowerCase().contains(q);
                        boolean matchEmail = b.getUser().getEmail() != null && b.getUser().getEmail().toLowerCase().contains(q);
                        if (!matchRef && !matchCustomer && !matchEmail) return false;
                    }
                    return true;
                })
                .map(b -> {
                    Show show = b.getItems().isEmpty() ? null : b.getItems().get(0).getShowSeat().getShow();
                    LocalDateTime holdUntil = b.getItems().isEmpty() ? null : b.getItems().get(0).getShowSeat().getHeldUntil();
                    return mapToBookingResponse(b, show, holdUntil);
                })
                .collect(Collectors.toList());
    }

    public BookingResponse mapToBookingResponse(Booking booking, Show show, LocalDateTime holdUntil) {
        Payment payment = paymentRepository.findByBookingBookingId(booking.getBookingId()).orElse(null);

        List<BookingResponse.SeatDetailDto> seats = booking.getItems().stream().map(item -> {
            ShowSeat ss = item.getShowSeat();
            return BookingResponse.SeatDetailDto.builder()
                    .showSeatId(ss.getShowSeatId())
                    .rowLabel(ss.getSeat().getRowLabel())
                    .seatNumber(ss.getSeat().getSeatNumber())
                    .seatType(ss.getSeat().getSeatType().name())
                    .price(item.getPrice())
                    .build();
        }).collect(Collectors.toList());

        LocalDateTime effectiveHoldUntil = holdUntil;
        if (effectiveHoldUntil == null && booking.getStatus() == BookingStatus.HELD && booking.getCreatedAt() != null) {
            effectiveHoldUntil = booking.getCreatedAt().plusMinutes(10);
        }

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingRef(booking.getBookingRef())
                .userId(booking.getUser().getUserId())
                .customerName(booking.getUser().getName())
                .customerEmail(booking.getUser().getEmail())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .holdExpiresAt(effectiveHoldUntil)
                .showId(show != null ? show.getShowId() : null)
                .movieTitle(show != null ? show.getMovie().getTitle() : null)
                .theatreName(show != null ? show.getScreen().getTheatre().getName() : null)
                .screenName(show != null ? show.getScreen().getName() : null)
                .showDate(show != null ? show.getShowDate() : null)
                .startTime(show != null ? show.getStartTime() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : (booking.getStatus() == BookingStatus.CONFIRMED ? "SUCCESS" : "PENDING"))
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .transactionRef(payment != null ? payment.getTransactionRef() : null)
                .seats(seats)
                .refundRef(booking.getStatus() == BookingStatus.CANCELLED ? (payment != null ? "REFUND-" + booking.getBookingRef() : null) : null)
                .refundedAmount(booking.getStatus() == BookingStatus.CANCELLED ? booking.getTotalAmount() : null)
                .build();
    }
}
