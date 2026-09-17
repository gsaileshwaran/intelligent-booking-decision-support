package com.pvk.cinemas.catalogue.controller;

import com.pvk.cinemas.catalogue.dto.MovieRequest;
import com.pvk.cinemas.catalogue.dto.MovieResponse;
import com.pvk.cinemas.catalogue.service.MovieService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/movies")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMovieController {

    private final MovieService movieService;

    public AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMovies(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovies(status, PageRequest.of(page, size))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(
            @Valid @RequestBody MovieRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        MovieResponse resp = movieService.createMovie(request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Movie created successfully", resp));
    }

    @PatchMapping("/{movieId}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Long movieId,
            @RequestBody MovieRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        MovieResponse resp = movieService.updateMovie(movieId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Movie updated successfully", resp));
    }
}
