package com.pvk.cinemas.catalogue.controller;

import com.pvk.cinemas.catalogue.dto.GenreResponse;
import com.pvk.cinemas.catalogue.dto.LanguageResponse;
import com.pvk.cinemas.catalogue.dto.MovieResponse;
import com.pvk.cinemas.catalogue.service.MovieService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;
    private final ShowSchedulingService showSchedulingService;

    public MovieController(MovieService movieService, ShowSchedulingService showSchedulingService) {
        this.movieService = movieService;
        this.showSchedulingService = showSchedulingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMovies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer cityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovies(status, cityId, PageRequest.of(page, size))));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getMovieById(movieId)));
    }

    @GetMapping("/{movieId}/genres")
    public ResponseEntity<ApiResponse<List<GenreResponse>>> getMovieGenres(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getGenresForMovie(movieId)));
    }

    @GetMapping("/{movieId}/languages")
    public ResponseEntity<ApiResponse<List<LanguageResponse>>> getMovieLanguages(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.ok(movieService.getLanguagesForMovie(movieId)));
    }

    @GetMapping("/{movieId}/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getMovieShows(
            @PathVariable Long movieId,
            @RequestParam(required = false) Integer cityId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(showSchedulingService.getShowsForMovie(movieId, cityId, date)));
    }
}
