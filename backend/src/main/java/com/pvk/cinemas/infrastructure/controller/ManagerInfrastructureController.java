package com.pvk.cinemas.infrastructure.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.infrastructure.dto.ScreenRequest;
import com.pvk.cinemas.infrastructure.dto.ScreenResponse;
import com.pvk.cinemas.infrastructure.dto.SeatRequest;
import com.pvk.cinemas.infrastructure.dto.SeatResponse;
import com.pvk.cinemas.infrastructure.service.ScreenService;
import com.pvk.cinemas.infrastructure.service.SeatService;
import com.pvk.cinemas.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager")
public class ManagerInfrastructureController {

    private final ScreenService screenService;
    private final SeatService seatService;

    public ManagerInfrastructureController(ScreenService screenService, SeatService seatService) {
        this.screenService = screenService;
        this.seatService = seatService;
    }

    // Screens in Theatre (BR-006 Scope Check)
    @GetMapping("/theatres/{theatreId}/screens")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId)")
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> getScreens(@PathVariable Integer theatreId) {
        return ResponseEntity.ok(ApiResponse.ok(screenService.getScreensByTheatre(theatreId)));
    }

    @PostMapping("/theatres/{theatreId}/screens")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId)")
    public ResponseEntity<ApiResponse<ScreenResponse>> createScreen(
            @PathVariable Integer theatreId,
            @Valid @RequestBody ScreenRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ScreenResponse resp = screenService.createScreen(theatreId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Screen created successfully", resp));
    }

    @PatchMapping("/theatres/{theatreId}/screens/{screenId}")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId) and @theatreScopeService.isScreenInTheatre(#screenId, #theatreId)")
    public ResponseEntity<ApiResponse<ScreenResponse>> updateScreen(
            @PathVariable Integer theatreId,
            @PathVariable Integer screenId,
            @RequestBody ScreenRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ScreenResponse resp = screenService.updateScreen(theatreId, screenId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Screen updated successfully", resp));
    }

    // Seats in Screen (BR-006 Scope Check)
    @GetMapping("/screens/{screenId}/seats")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Integer screenId) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getSeatsByScreen(screenId)));
    }

    @PostMapping("/screens/{screenId}/seats")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<SeatResponse>> createSeat(
            @PathVariable Integer screenId,
            @Valid @RequestBody SeatRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        SeatResponse resp = seatService.createSeat(screenId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Seat created successfully", resp));
    }

    @PatchMapping("/screens/{screenId}/seats/{seatId}")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeat(
            @PathVariable Integer screenId,
            @PathVariable Long seatId,
            @RequestBody SeatRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        SeatResponse resp = seatService.updateSeat(screenId, seatId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Seat updated successfully", resp));
    }
}
