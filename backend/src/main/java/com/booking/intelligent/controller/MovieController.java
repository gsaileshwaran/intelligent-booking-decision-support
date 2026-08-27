package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getMovies(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        List<Movie> movies = movieService.filterMovies(language, genre, status, search, sort);
        return ResponseEntity.ok(ApiResponse.success("Movies retrieved successfully", movies));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Movie>>> searchMovies(@RequestParam String query) {
        List<Movie> movies = movieService.searchMovies(query);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", movies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> getMovieById(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return ResponseEntity.ok(ApiResponse.success("Movie details retrieved", movie));
    }
}
