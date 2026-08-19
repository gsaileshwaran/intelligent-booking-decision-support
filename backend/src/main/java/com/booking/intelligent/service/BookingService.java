package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.PaymentStatus;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.exception.InvalidBookingStateException;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.exception.SeatNotAvailableException;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            // Check if seat is already booked or currently held by someone else
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

    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String paymentMethod, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new InvalidBookingStateException("Unauthorized access to booking.");
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
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .paymentMethod(paymentMethod != null ? paymentMethod : "MOCK_CARD")
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .status(PaymentStatus.SUCCESS)
                .build();
        paymentRepository.save(payment);

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

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
        return mapToBookingResponse(confirmedBooking, show, null);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new InvalidBookingStateException("Unauthorized access to booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidBookingStateException("Booking is already " + booking.getStatus());
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

        Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
        return mapToBookingResponse(cancelledBooking, show, null);
    }

    public List<BookingResponse> getUserBookingHistory(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(booking -> {
            Show show = booking.getItems().isEmpty() ? null : booking.getItems().get(0).getShowSeat().getShow();
            return mapToBookingResponse(booking, show, null);
        }).collect(Collectors.toList());
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
    }

    private BookingResponse mapToBookingResponse(Booking booking, Show show, LocalDateTime holdExpiresAt) {
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

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingRef(booking.getBookingRef())
                .userId(booking.getUser().getUserId())
                .showId(show != null ? show.getShowId() : null)
                .movieTitle(show != null ? show.getMovie().getTitle() : null)
                .theatreName(show != null ? show.getScreen().getTheatre().getName() : null)
                .screenName(show != null ? show.getScreen().getName() : null)
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .holdExpiresAt(holdExpiresAt)
                .seats(seatDtos)
                .build();
    }
}
