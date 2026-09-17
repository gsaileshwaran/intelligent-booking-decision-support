package com.pvk.cinemas.scheduling.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
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
@RequestMapping("/api/v1/manager/theatres/{theatreId}/shows")
public class ManagerSchedulingController {

    private final ShowSchedulingService showSchedulingService;

    public ManagerSchedulingController(ShowSchedulingService showSchedulingService) {
        this.showSchedulingService = showSchedulingService;
    }

    @GetMapping
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId)")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShows(@PathVariable Integer theatreId) {
        return ResponseEntity.ok(ApiResponse.ok(showSchedulingService.getShowsForTheatre(theatreId)));
    }

    @PostMapping
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId)")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(
            @PathVariable Integer theatreId,
            @Valid @RequestBody ShowRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ShowResponse resp = showSchedulingService.createShow(theatreId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Show scheduled successfully", resp));
    }

    @PatchMapping("/{showId}")
    @PreAuthorize("@theatreScopeService.hasAccessToTheatre(#theatreId) and @theatreScopeService.isShowInTheatre(#showId, #theatreId)")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(
            @PathVariable Integer theatreId,
            @PathVariable Long showId,
            @RequestBody ShowRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        ShowResponse resp = showSchedulingService.updateShow(theatreId, showId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Show updated successfully", resp));
    }
}
