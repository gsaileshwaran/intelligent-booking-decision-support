package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.BookingService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BookingResponse> history = bookingService.getUserBookingHistory(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User bookings retrieved", history));
    }

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(
            @Valid @RequestBody SeatHoldRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse response = bookingService.holdSeats(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Seats reserved successfully. Please complete payment before hold expires.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse response = bookingService.getBookingById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking details retrieved", response));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "MOCK_CARD") String paymentMethod,
            @RequestParam(required = false) String promoCode,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        BookingResponse response = bookingService.confirmBooking(id, paymentMethod, promoCode, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully!", response));
    }

    @PostMapping("/validate-promo")
    public ResponseEntity<ApiResponse<PromoValidationResponse>> validatePromo(
            @RequestParam String promoCode,
            @RequestParam BigDecimal amount) {
        String code = promoCode != null ? promoCode.trim().toUpperCase() : "";
        BigDecimal discount = BigDecimal.ZERO;
        boolean isValid = false;
        String message = "Invalid promo code.";

        if ("PVKWEEKEND".equals(code)) {
            discount = amount.multiply(BigDecimal.valueOf(0.15));
            isValid = true;
            message = "PVK Cinema Weekend Special (15% OFF) applied!";
        } else if ("FIRSTBOOK".equals(code)) {
            discount = BigDecimal.valueOf(50.00);
            isValid = true;
            message = "First Time Moviegoer Bonus (₹50 OFF) applied!";
        } else if ("IMAXSPECIAL".equals(code)) {
            discount = BigDecimal.valueOf(30.00);
            isValid = true;
            message = "IMAX Experience Offer (₹30 OFF) applied!";
        }

        BigDecimal finalAmount = amount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        PromoValidationResponse res = PromoValidationResponse.builder()
                .promoCode(code)
                .isValid(isValid)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .message(message)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Promo code evaluated", res));
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PromoValidationResponse {
        private String promoCode;
        private Boolean isValid;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String message;
    }
}
