package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.ShowSeat;
import com.booking.intelligent.service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
            @RequestParam Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Show> shows = showService.getShowsByMovieAndDate(movieId, date);
        return ResponseEntity.ok(ApiResponse.success("Shows retrieved successfully", shows));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Show>> getShowById(@PathVariable Long id) {
        Show show = showService.getShowById(id);
        return ResponseEntity.ok(ApiResponse.success("Show details retrieved", show));
    }

    @GetMapping("/{id}/seat-map")
    public ResponseEntity<ApiResponse<List<ShowSeat>>> getShowSeats(@PathVariable Long id) {
        List<ShowSeat> showSeats = showService.getShowSeats(id);
        return ResponseEntity.ok(ApiResponse.success("Show seats inventory retrieved", showSeats));
    }
}
