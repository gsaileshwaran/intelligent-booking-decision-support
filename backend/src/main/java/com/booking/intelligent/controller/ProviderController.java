package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.Theatre;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.MovieService;
import com.booking.intelligent.service.ShowService;
import com.booking.intelligent.service.TheatreService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider")
@PreAuthorize("hasAnyAuthority('ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN')")
public class ProviderController {

    @Autowired
    private TheatreService theatreService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ShowService showService;

    @GetMapping("/theatres")
    public ResponseEntity<ApiResponse<List<Theatre>>> getMyTheatres(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Theatre> theatres = theatreService.getTheatresByOwner(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Provider theatres retrieved", theatres));
    }

    @PostMapping("/theatres")
    public ResponseEntity<ApiResponse<Theatre>> createTheatre(
            @Valid @RequestBody Theatre theatre,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Theatre created = theatreService.createTheatre(theatre, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Theatre created successfully", created));
    }

    @PostMapping("/movies")
    public ResponseEntity<ApiResponse<Movie>> createMovie(@Valid @RequestBody Movie movie) {
        Movie created = movieService.createMovie(movie);
        return ResponseEntity.ok(ApiResponse.success("Movie added to catalogue", created));
    }

    @PostMapping("/shows")
    public ResponseEntity<ApiResponse<Show>> createShow(
            @RequestBody Show show,
            @RequestParam Long movieId,
            @RequestParam Long screenId) {
        Show created = showService.createShow(show, movieId, screenId);
        return ResponseEntity.ok(ApiResponse.success("Show session scheduled successfully", created));
    }
}
