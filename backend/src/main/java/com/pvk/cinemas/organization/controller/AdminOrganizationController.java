package com.pvk.cinemas.organization.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.organization.dto.*;
import com.pvk.cinemas.organization.service.CityService;
import com.pvk.cinemas.organization.service.TheatreService;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminOrganizationController {

    private final CityService cityService;
    private final TheatreService theatreService;

    public AdminOrganizationController(CityService cityService, TheatreService theatreService) {
        this.cityService = cityService;
        this.theatreService = theatreService;
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCities() {
        return ResponseEntity.ok(ApiResponse.ok(cityService.getAllCities()));
    }

    @PostMapping("/cities")
    public ResponseEntity<ApiResponse<CityResponse>> createCity(
            @Valid @RequestBody CityRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        CityResponse resp = cityService.createCity(request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("City created successfully", resp));
    }

    @PatchMapping("/cities/{cityId}")
    public ResponseEntity<ApiResponse<CityResponse>> updateCity(
            @PathVariable Integer cityId,
            @RequestBody CityRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        CityResponse resp = cityService.updateCity(cityId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("City updated successfully", resp));
    }

    @GetMapping("/theatres")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getTheatres() {
        return ResponseEntity.ok(ApiResponse.ok(theatreService.getAllTheatres()));
    }

    @PostMapping("/theatres")
    public ResponseEntity<ApiResponse<TheatreResponse>> createTheatre(
            @Valid @RequestBody TheatreRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        TheatreResponse resp = theatreService.createTheatre(request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Theatre created successfully", resp));
    }

    @PatchMapping("/theatres/{theatreId}")
    public ResponseEntity<ApiResponse<TheatreResponse>> updateTheatre(
            @PathVariable Integer theatreId,
            @RequestBody TheatreRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        TheatreResponse resp = theatreService.updateTheatre(theatreId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Theatre updated successfully", resp));
    }

    @PostMapping("/theatres/{theatreId}/managers")
    public ResponseEntity<ApiResponse<String>> assignManager(
            @PathVariable Integer theatreId,
            @Valid @RequestBody AssignManagerRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        theatreService.assignManager(theatreId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Manager assigned to theatre successfully", null));
    }

    @PatchMapping("/theatres/{theatreId}/managers")
    public ResponseEntity<ApiResponse<String>> revokeManager(
            @PathVariable Integer theatreId,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        theatreService.revokeManager(theatreId, userId, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Manager revoked from theatre successfully", null));
    }
}
