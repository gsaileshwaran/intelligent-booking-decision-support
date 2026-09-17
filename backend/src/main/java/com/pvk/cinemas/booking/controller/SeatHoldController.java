package com.pvk.cinemas.booking.controller;

import com.pvk.cinemas.booking.dto.SeatHoldRequest;
import com.pvk.cinemas.booking.dto.SeatHoldResponse;
import com.pvk.cinemas.booking.dto.SeatReleaseRequest;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shows/{showId}/seats")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping("/hold")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> holdSeats(
            @PathVariable Long showId,
            @Valid @RequestBody SeatHoldRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SeatHoldResponse response = seatHoldService.holdSeats(showId, principal.getUserId(), request.getSeatIds());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> releaseHold(
            @PathVariable Long showId,
            @Valid @RequestBody SeatReleaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        seatHoldService.releaseHold(request.getHoldToken(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Seats released successfully"));
    }
}
