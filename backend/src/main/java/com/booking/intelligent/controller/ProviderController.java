package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.ProviderDashboardStats;
import com.booking.intelligent.dto.ShowRequestDto;
import com.booking.intelligent.dto.ShowResponseDto;
import com.booking.intelligent.entity.Booking;
import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.entity.Screen;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.Theatre;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.BookingService;
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

    @Autowired
    private BookingService bookingService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ProviderDashboardStats>> getDashboardStats(@AuthenticationPrincipal UserPrincipal currentUser) {
        ProviderDashboardStats stats = theatreService.getProviderDashboardStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Provider dashboard statistics retrieved", stats));
    }

    @GetMapping("/theatres")
    public ResponseEntity<ApiResponse<List<Theatre>>> getMyTheatres(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Theatre> theatres = theatreService.getTheatresByOwner(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Provider theatres retrieved", theatres));
    }

    @GetMapping("/theatres/{id}")
    public ResponseEntity<ApiResponse<Theatre>> getTheatreDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Theatre theatre = theatreService.getTheatreForOwner(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Branch details retrieved", theatre));
    }

    @GetMapping("/theatres/{id}/screens")
    public ResponseEntity<ApiResponse<List<Screen>>> getTheatreScreens(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Screen> screens = theatreService.getScreensForOwnerTheatre(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Branch screens retrieved", screens));
    }

    @GetMapping("/theatres/{id}/shows")
    public ResponseEntity<ApiResponse<List<Show>>> getTheatreShows(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Show> shows = theatreService.getShowsForOwnerTheatre(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Branch shows retrieved", shows));
    }

    @GetMapping("/theatres/{id}/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> getTheatreBookings(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Booking> bookings = theatreService.getBookingsForOwnerTheatre(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Branch bookings retrieved", bookings));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getProviderBookings(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<BookingResponse> bookings = bookingService.getProviderBookings(currentUser.getId(), branchId, date, status);
        return ResponseEntity.ok(ApiResponse.success("Provider bookings retrieved", bookings));
    }

    @GetMapping("/shows")
    public ResponseEntity<ApiResponse<List<ShowResponseDto>>> getProviderShows(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long screenId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ShowResponseDto> shows = showService.getProviderShows(
                currentUser.getId(), branchId, screenId, movieId, date, status, q
        );
        return ResponseEntity.ok(ApiResponse.success("Provider shows retrieved", shows));
    }

    @PutMapping("/shows/{id}")
    public ResponseEntity<ApiResponse<ShowResponseDto>> updateShow(
            @PathVariable Long id,
            @RequestBody ShowRequestDto request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ShowResponseDto updated = showService.updateShow(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Show updated successfully", updated));
    }

    @PatchMapping("/shows/{id}/cancel")
    public ResponseEntity<ApiResponse<ShowResponseDto>> cancelShow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ShowResponseDto cancelled = showService.cancelShow(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Show cancelled successfully", cancelled));
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
