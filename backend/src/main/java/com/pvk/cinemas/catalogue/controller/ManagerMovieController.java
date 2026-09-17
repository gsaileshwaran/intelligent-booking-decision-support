package com.pvk.cinemas.catalogue.controller;

import com.pvk.cinemas.catalogue.dto.MovieResponse;
import com.pvk.cinemas.catalogue.service.MovieService;
import com.pvk.cinemas.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CHANGE-API-002 / DECISION-005:
 * Theatre Managers have READ-ONLY catalogue access.
 * POST and PATCH endpoints are strictly prohibited.
 */
@RestController
@RequestMapping("/api/v1/manager/movies")
@PreAuthorize("hasRole('THEATRE_MANAGER')")
public class ManagerMovieController {

    private final MovieService movieService;

    public ManagerMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMovies(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovies(status, PageRequest.of(page, size))));
    }
}
