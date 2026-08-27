package com.booking.intelligent.service;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerConvenienceService {

    @Autowired
    private MovieFavoriteRepository favoriteRepository;

    @Autowired
    private CustomerNotificationRepository notificationRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    // --- FAVORITES ---
    @Transactional
    public boolean toggleFavorite(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));

        boolean exists = favoriteRepository.existsByUserUserIdAndMovieMovieId(userId, movieId);
        if (exists) {
            favoriteRepository.deleteByUserUserIdAndMovieMovieId(userId, movieId);
            return false;
        } else {
            MovieFavorite favorite = MovieFavorite.builder()
                    .user(user)
                    .movie(movie)
                    .createdAt(LocalDateTime.now())
                    .build();
            favoriteRepository.save(favorite);
            return true;
        }
    }

    public List<Movie> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(MovieFavorite::getMovie)
                .collect(Collectors.toList());
    }

    public boolean isFavorite(Long userId, Long movieId) {
        if (userId == null) return false;
        return favoriteRepository.existsByUserUserIdAndMovieMovieId(userId, movieId);
    }

    // --- NOTIFICATIONS ---
    @Transactional
    public CustomerNotification createNotification(Long userId, String title, String message, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        CustomerNotification notification = CustomerNotification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type != null ? type : "SYSTEM")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    public List<CustomerNotification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        CustomerNotification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUser().getUserId().equals(userId)) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    // --- PREFERENCES ---
    public UserPreference getUserPreferences(Long userId) {
        return userPreferenceRepository.findByUserUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return null;
            return userPreferenceRepository.save(UserPreference.builder()
                    .user(user)
                    .budgetLimit(BigDecimal.valueOf(500.00))
                    .preferredTime("EVENING")
                    .preferredSeatType("PREMIUM")
                    .groupSize(2)
                    .build());
        });
    }

    @Transactional
    public UserPreference updateUserPreferences(Long userId, BigDecimal budgetLimit, String preferredTime, String preferredSeatType, Integer groupSize) {
        UserPreference pref = getUserPreferences(userId);
        if (pref != null) {
            if (budgetLimit != null) pref.setBudgetLimit(budgetLimit);
            if (preferredTime != null) pref.setPreferredTime(preferredTime);
            if (preferredSeatType != null) pref.setPreferredSeatType(preferredSeatType);
            if (groupSize != null) pref.setGroupSize(groupSize);
            return userPreferenceRepository.save(pref);
        }
        return null;
    }

    // --- DEMO OFFERS ---
    public List<OfferDto> getAvailableOffers() {
        List<OfferDto> offers = new ArrayList<>();
        offers.add(OfferDto.builder()
                .offerId(1L)
                .code("PVKWEEKEND")
                .title("PVK Cinema Weekend Special")
                .description("Get flat 15% discount on all PREMIUM & BALCONY tickets for PVK cinema branches.")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(15))
                .validTill("2026-12-31")
                .bannerUrl("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800")
                .build());

        offers.add(OfferDto.builder()
                .offerId(2L)
                .code("FIRSTBOOK")
                .title("First Time Moviegoer Bonus")
                .description("Flat ₹50 instant discount on your first ticket booking across all branches.")
                .discountType("FLAT")
                .discountValue(BigDecimal.valueOf(50))
                .validTill("2026-12-31")
                .bannerUrl("https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800")
                .build());

        offers.add(OfferDto.builder()
                .offerId(3L)
                .code("IMAXSPECIAL")
                .title("IMAX Experience Offer")
                .description("Complimentary ₹30 concession voucher with every IMAX screen showtime booking.")
                .discountType("FLAT")
                .discountValue(BigDecimal.valueOf(30))
                .validTill("2026-12-31")
                .bannerUrl("https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=800")
                .build());

        return offers;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OfferDto {
        private Long offerId;
        private String code;
        private String title;
        private String description;
        private String discountType;
        private BigDecimal discountValue;
        private String validTill;
        private String bannerUrl;
    }
}
