package com.pvk.cinemas.availability.controller;

import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.dto.UpdateSeatStatusRequest;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/shows/{showId}/seats")
public class ManagerAvailabilityController {

    private final SeatAvailabilityService seatAvailabilityService;

    public ManagerAvailabilityController(SeatAvailabilityService seatAvailabilityService) {
        this.seatAvailabilityService = seatAvailabilityService;
    }

    @GetMapping
    @PreAuthorize("@theatreScopeService.hasAccessToShow(#showId)")
    public ResponseEntity<ApiResponse<ShowSeatAvailabilityResponse>> getSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.ok(seatAvailabilityService.getShowSeatAvailability(showId)));
    }

    @PatchMapping
    @PreAuthorize("@theatreScopeService.hasAccessToShow(#showId)")
    public ResponseEntity<ApiResponse<ShowSeatAvailabilityResponse>> overrideSeatStatus(
            @PathVariable Long showId,
            @Valid @RequestBody UpdateSeatStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ShowSeatAvailabilityResponse resp = seatAvailabilityService.overrideSeatStatus(showId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Seat statuses overridden successfully", resp));
    }
}
