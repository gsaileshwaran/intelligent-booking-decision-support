package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.ShowSeatResponseDto;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    @Autowired
    private ShowService showService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Show>>> getShows(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (movieId == null) {
            return ResponseEntity.ok(ApiResponse.success("Shows retrieved successfully", List.of()));
        }
        List<Show> shows = showService.getShowsByMovieAndDate(movieId, date);
        return ResponseEntity.ok(ApiResponse.success("Shows retrieved successfully", shows));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Show>> getShowById(@PathVariable Long id) {
        Show show = showService.getShowById(id);
        return ResponseEntity.ok(ApiResponse.success("Show details retrieved", show));
    }

    @GetMapping("/{id}/seat-map")
    public ResponseEntity<ApiResponse<List<ShowSeatResponseDto>>> getShowSeats(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        List<ShowSeatResponseDto> showSeats = showService.getShowSeatsWithUserOwnership(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Show seats inventory retrieved", showSeats));
    }
}
