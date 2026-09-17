package com.pvk.cinemas.booking.controller;

import com.pvk.cinemas.booking.dto.BookingResponse;
import com.pvk.cinemas.booking.dto.CheckoutRequest;
import com.pvk.cinemas.booking.service.BookingService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings/checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BookingResponse response = bookingService.checkout(request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/bookings")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<BookingResponse> bookings = bookingService.getUserBookings(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @GetMapping("/bookings/reference/{reference}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isSuperAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        BookingResponse booking = bookingService.getBookingByReference(reference, principal.getUserId(), isSuperAdmin);
        return ResponseEntity.ok(ApiResponse.ok(booking));
    }
}
