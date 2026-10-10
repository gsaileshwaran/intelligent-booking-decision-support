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
    private final com.pvk.cinemas.infrastructure.service.ScreenCapabilityService screenCapabilityService;

    public ManagerInfrastructureController(ScreenService screenService,
                                           SeatService seatService,
                                           com.pvk.cinemas.infrastructure.service.ScreenCapabilityService screenCapabilityService) {
        this.screenService = screenService;
        this.seatService = seatService;
        this.screenCapabilityService = screenCapabilityService;
    }

    // Screens in Theatre (BR-006 Scope Check)
    @GetMapping("/theatres/{theatreId}/screens")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId)")
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> getScreens(@PathVariable Long theatreId) {
        return ResponseEntity.ok(ApiResponse.ok(screenService.getScreensByTheatre(theatreId)));
    }

    @PostMapping("/theatres/{theatreId}/screens")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId)")
    public ResponseEntity<ApiResponse<ScreenResponse>> createScreen(
            @PathVariable Long theatreId,
            @Valid @RequestBody ScreenRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ScreenResponse resp = screenService.createScreen(theatreId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Screen created successfully", resp));
    }

    @PatchMapping("/theatres/{theatreId}/screens/{screenId}")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId) and @theatreScopeService.isScreenInTheatre(#screenId, #theatreId)")
    public ResponseEntity<ApiResponse<ScreenResponse>> updateScreen(
            @PathVariable Long theatreId,
            @PathVariable Long screenId,
            @RequestBody ScreenRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ScreenResponse resp = screenService.updateScreen(theatreId, screenId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Screen updated successfully", resp));
    }

    // Screen Capabilities (BR-003 Scope Check)
    @GetMapping("/screens/{screenId}/capabilities")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<List<com.pvk.cinemas.infrastructure.dto.ScreenCapabilityResponse>>> getCapabilities(
            @PathVariable Long screenId) {
        return ResponseEntity.ok(ApiResponse.ok(screenCapabilityService.getCapabilitiesByScreen(screenId)));
    }

    @PostMapping("/screens/{screenId}/capabilities")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<com.pvk.cinemas.infrastructure.dto.ScreenCapabilityResponse>> addCapability(
            @PathVariable Long screenId,
            @Valid @RequestBody com.pvk.cinemas.infrastructure.dto.ScreenCapabilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        com.pvk.cinemas.infrastructure.dto.ScreenCapabilityResponse resp =
                screenCapabilityService.addCapabilityToScreen(screenId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Capability added to screen successfully", resp));
    }

    @DeleteMapping("/screens/{screenId}/capabilities/{capabilityId}")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId) and @theatreScopeService.isCapabilityInScreen(#capabilityId, #screenId)")
    public ResponseEntity<ApiResponse<String>> deleteCapability(
            @PathVariable Long screenId,
            @PathVariable Long capabilityId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        screenCapabilityService.deleteCapability(screenId, capabilityId, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Capability removed from screen successfully", null));
    }

    // Seats in Screen (BR-006 Scope Check)
    @GetMapping("/screens/{screenId}/seats")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Long screenId) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getSeatsByScreen(screenId)));
    }

    @PostMapping("/screens/{screenId}/seats")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")
    public ResponseEntity<ApiResponse<SeatResponse>> createSeat(
            @PathVariable Long screenId,
            @Valid @RequestBody SeatRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        SeatResponse resp = seatService.createSeat(screenId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Seat created successfully", resp));
    }

    @PatchMapping("/screens/{screenId}/seats/{seatId}")
    @PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId) and @theatreScopeService.isSeatInScreen(#seatId, #screenId)")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeat(
            @PathVariable Long screenId,
            @PathVariable Long seatId,
            @RequestBody SeatRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        SeatResponse resp = seatService.updateSeat(screenId, seatId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Seat updated successfully", resp));
    }
}
