package com.pvk.cinemas.catalogue.service;

import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Authoritative Movie Lifecycle & Demand Classification Service.
 *
 * Implements the locked mathematical lifecycle model from:
 * PART_3_YEAR_LONG_SHOW_SCHEDULING_ARCHITECTURE.md (§5.1)
 *
 * Math Formulation:
 *   durationDays = endDate - startDate + 1  (D >= 1)
 *   elapsedDays  = refDate - startDate
 *   rho          = elapsedDays / durationDays
 *
 * Disjoint Stage Intervals:
 *   elapsedDays < 0           ==> UPCOMING (0 shows)
 *   elapsedDays >= D          ==> ENDED (0 shows)
 *   rho in [0.00, 0.15)       ==> OPENING
 *   rho in [0.15, 0.40)       ==> PEAK
 *   rho in [0.40, 0.70)       ==> REGULAR_RUN
 *   rho in [0.70, 0.90)       ==> REDUCED_RUN
 *   rho in [0.90, 1.00)       ==> FINAL_DAYS (strictly 1 show/day)
 */
@Service
public class MovieLifecycleService {

    public static final Set<String> ALLOWED_DEMAND_TIERS = Set.of(
            "BLOCKBUSTER",
            "MAJOR_REGIONAL",
            "MID_TIER",
            "NICHE",
            "RE_RELEASE"
    );

    public static final Set<String> ACTIVE_STAGES = Set.of(
            "OPENING",
            "PEAK",
            "REGULAR_RUN",
            "REDUCED_RUN",
            "FINAL_DAYS"
    );

    private final BusinessDateProvider businessDateProvider;

    public MovieLifecycleService(BusinessDateProvider businessDateProvider) {
        this.businessDateProvider = businessDateProvider;
    }

    /**
     * Evaluates the lifecycle stage of a movie against the authoritative business date.
     */
    public String calculateLifecycleStage(Movie movie) {
        LocalDate refDate = (businessDateProvider != null) ? businessDateProvider.getBusinessDate() : LocalDate.now();
        return calculateLifecycleStage(movie, refDate);
    }

    /**
     * Evaluates the lifecycle stage of a movie against an explicit reference date.
     */
    public String calculateLifecycleStage(Movie movie, LocalDate refDate) {
        if (movie == null || movie.getExhibitionStartDate() == null || movie.getExhibitionEndDate() == null) {
            return (movie != null && movie.getMovieStatus() != null) ? movie.getMovieStatus().toUpperCase() : "UPCOMING";
        }
        return evaluateStage(movie.getExhibitionStartDate(), movie.getExhibitionEndDate(), refDate);
    }

    /**
     * Pure mathematical calculation of lifecycle stage given window dates and reference date.
     */
    public String evaluateStage(LocalDate startDate, LocalDate endDate, LocalDate refDate) {
        if (startDate == null || endDate == null) {
            return "UPCOMING";
        }
        if (refDate == null) {
            refDate = (businessDateProvider != null) ? businessDateProvider.getBusinessDate() : LocalDate.now();
        }

        // Multi-year future horizon mapping: map forward demo years to catalogue reference window
        if (endDate != null && refDate.isAfter(endDate) && refDate.getYear() > endDate.getYear()) {
            refDate = refDate.minusYears(refDate.getYear() - endDate.getYear());
        }

        long durationDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDays < 1) {
            durationDays = 1;
        }

        long elapsedDays = ChronoUnit.DAYS.between(startDate, refDate);

        if (elapsedDays < 0) {
            return "UPCOMING";
        }
        if (elapsedDays >= durationDays) {
            return "ENDED";
        }

        double rho = (double) elapsedDays / (double) durationDays;

        if (rho < 0.15) {
            return "OPENING";
        } else if (rho < 0.40) {
            return "PEAK";
        } else if (rho < 0.70) {
            return "REGULAR_RUN";
        } else if (rho < 0.90) {
            return "REDUCED_RUN";
        } else {
            return "FINAL_DAYS";
        }
    }

    /**
     * Calculates the continuous lifecycle ratio rho = elapsedDays / durationDays in [0.0, 1.0].
     */
    public double calculateRho(LocalDate startDate, LocalDate endDate, LocalDate refDate) {
        if (startDate == null || endDate == null) return 0.0;
        long durationDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDays < 1) durationDays = 1;

        LocalDate effRef = (refDate != null) ? refDate : ((businessDateProvider != null) ? businessDateProvider.getBusinessDate() : LocalDate.now());
        if (endDate != null && effRef.isAfter(endDate) && effRef.getYear() > endDate.getYear()) {
            effRef = effRef.minusYears(effRef.getYear() - endDate.getYear());
        }

        long elapsedDays = ChronoUnit.DAYS.between(startDate, effRef);
        if (elapsedDays < 0) return 0.0;
        if (elapsedDays >= durationDays) return 1.0;
        return (double) elapsedDays / (double) durationDays;
    }

    /**
     * Determines whether the movie is currently active (airing) on the reference date.
     */
    public boolean isActive(Movie movie, LocalDate refDate) {
        String stage = calculateLifecycleStage(movie, refDate);
        return ACTIVE_STAGES.contains(stage);
    }

    /**
     * Determines high-level effective status: UPCOMING, AIRING, or ENDED.
     */
    public String determineEffectiveStatus(Movie movie, LocalDate refDate) {
        String stage = calculateLifecycleStage(movie, refDate);
        if ("UPCOMING".equals(stage)) {
            return "UPCOMING";
        }
        if ("ENDED".equals(stage)) {
            return "ENDED";
        }
        return "AIRING";
    }

    public String determineEffectiveStatus(Movie movie) {
        LocalDate refDate = (businessDateProvider != null) ? businessDateProvider.getBusinessDate() : LocalDate.now();
        return determineEffectiveStatus(movie, refDate);
    }

    /**
     * Validates Part 2 movie demand and lifecycle fields.
     */
    public void validateMovieMetadata(String demandTier, BigDecimal popularityScore, LocalDate startDate, LocalDate endDate) {
        if (demandTier != null && !demandTier.isBlank()) {
            String upper = demandTier.trim().toUpperCase();
            if (!ALLOWED_DEMAND_TIERS.contains(upper)) {
                throw new BadRequestException("Invalid demand_tier: " + demandTier + ". Allowed: " + ALLOWED_DEMAND_TIERS);
            }
        }

        if (popularityScore != null) {
            if (popularityScore.compareTo(BigDecimal.ZERO) < 0 || popularityScore.compareTo(BigDecimal.TEN) > 0) {
                throw new BadRequestException("popularity_score must be between 0.00 and 10.00 (received: " + popularityScore + ")");
            }
        }

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new BadRequestException("exhibition_end_date (" + endDate + ") must be >= exhibition_start_date (" + startDate + ")");
            }
        }
    }
}
