package com.pvk.cinemas.organization.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.dto.TheatreResponse;
import com.pvk.cinemas.organization.service.TheatreService;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/theatres")
public class TheatreController {

    private final TheatreService theatreService;
    private final ScreenRepository screenRepository;
    private final ShowSchedulingService showSchedulingService;

    public TheatreController(TheatreService theatreService,
                             ScreenRepository screenRepository,
                             ShowSchedulingService showSchedulingService) {
        this.theatreService = theatreService;
        this.screenRepository = screenRepository;
        this.showSchedulingService = showSchedulingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getTheatres(@RequestParam(required = false) Integer cityId) {
        if (cityId != null) {
            return ResponseEntity.ok(ApiResponse.ok(theatreService.getTheatresByCity(cityId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(theatreService.getAllTheatres()));
    }

    @GetMapping("/{theatreId}")
    public ResponseEntity<ApiResponse<TheatreResponse>> getTheatre(@PathVariable Integer theatreId) {
        return ResponseEntity.ok(ApiResponse.ok(theatreService.getTheatreById(theatreId)));
    }

    @GetMapping("/{theatreId}/screens")
    public ResponseEntity<ApiResponse<List<Screen>>> getScreens(@PathVariable Integer theatreId) {
        return ResponseEntity.ok(ApiResponse.ok(screenRepository.findByTheatreIdAndIsActiveTrue(theatreId)));
    }

    @GetMapping("/{theatreId}/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShows(
            @PathVariable Integer theatreId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(showSchedulingService.getShowsForTheatre(theatreId, date)));
    }
}
