package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(
            @Valid @RequestBody SeatHoldRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse response = bookingService.holdSeats(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Seats reserved successfully. Please complete payment before hold expires.", response));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "MOCK_CARD") String paymentMethod,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse response = bookingService.confirmBooking(id, paymentMethod, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully!", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse response = bookingService.cancelBooking(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully.", response));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookingHistory(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BookingResponse> history = bookingService.getUserBookingHistory(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking history retrieved", history));
    }
}
