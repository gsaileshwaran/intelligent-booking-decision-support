package com.pvk.cinemas.scheduling.controller;

import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shows")
public class ShowController {

    private final ShowSchedulingService showSchedulingService;
    private final SeatAvailabilityService seatAvailabilityService;

    public ShowController(ShowSchedulingService showSchedulingService, SeatAvailabilityService seatAvailabilityService) {
        this.showSchedulingService = showSchedulingService;
        this.seatAvailabilityService = seatAvailabilityService;
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShow(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.ok(showSchedulingService.getShowById(showId)));
    }

    @GetMapping("/{showId}/seats")
    public ResponseEntity<ApiResponse<ShowSeatAvailabilityResponse>> getShowSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.ok(seatAvailabilityService.getShowSeatAvailability(showId)));
    }
}
