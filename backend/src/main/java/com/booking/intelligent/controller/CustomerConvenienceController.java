package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.entity.CustomerNotification;
import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.entity.UserPreference;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.CustomerConvenienceService;
import com.booking.intelligent.service.CustomerConvenienceService.OfferDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CustomerConvenienceController {

    @Autowired
    private CustomerConvenienceService convenienceService;

    // --- FAVORITES ---
    @PostMapping("/customer/favorites/{movieId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleFavorite(
            @PathVariable Long movieId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boolean isFav = convenienceService.toggleFavorite(currentUser.getId(), movieId);
        return ResponseEntity.ok(ApiResponse.success(isFav ? "Added to Watchlist" : "Removed from Watchlist", isFav));
    }

    @GetMapping("/customer/favorites")
    public ResponseEntity<ApiResponse<List<Movie>>> getFavorites(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Movie> favorites = convenienceService.getUserFavorites(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User watchlist retrieved", favorites));
    }

    @GetMapping("/customer/favorites/check/{movieId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @PathVariable Long movieId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        boolean isFav = convenienceService.isFavorite(userId, movieId);
        return ResponseEntity.ok(ApiResponse.success("Status checked", isFav));
    }

    // --- NOTIFICATIONS ---
    @GetMapping("/customer/notifications")
    public ResponseEntity<ApiResponse<List<CustomerNotification>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<CustomerNotification> notifications = convenienceService.getUserNotifications(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notifications));
    }

    @PutMapping("/customer/notifications/{id}/read")
    public ResponseEntity<ApiResponse<String>> markNotificationRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        convenienceService.markNotificationAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", "SUCCESS"));
    }

    // --- PREFERENCES ---
    @GetMapping("/customer/preferences")
    public ResponseEntity<ApiResponse<UserPreference>> getPreferences(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserPreference pref = convenienceService.getUserPreferences(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User preferences retrieved", pref));
    }

    @PutMapping("/customer/preferences")
    public ResponseEntity<ApiResponse<UserPreference>> updatePreferences(
            @RequestParam(required = false) BigDecimal budgetLimit,
            @RequestParam(required = false) String preferredTime,
            @RequestParam(required = false) String preferredSeatType,
            @RequestParam(required = false) Integer groupSize,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserPreference pref = convenienceService.updateUserPreferences(currentUser.getId(), budgetLimit, preferredTime, preferredSeatType, groupSize);
        return ResponseEntity.ok(ApiResponse.success("User preferences updated", pref));
    }

    // --- PUBLIC OFFERS ---
    @GetMapping("/offers")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getOffers() {
        List<OfferDto> offers = convenienceService.getAvailableOffers();
        return ResponseEntity.ok(ApiResponse.success("Offers catalog retrieved", offers));
    }
}
