package com.pvk.cinemas.catalogue;

import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.service.MovieLifecycleService;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Part 2 — Movie Lifecycle & Mathematical Stage Calculation Unit Tests")
class MovieLifecycleServiceTest {

    private BusinessDateProvider businessDateProvider;
    private MovieLifecycleService lifecycleService;

    // Anchor: 2026-09-18
    private final LocalDate anchorDate = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        businessDateProvider = new BusinessDateProvider("2026-09-18", 7);
        lifecycleService = new MovieLifecycleService(businessDateProvider);
    }

    @Test
    @DisplayName("Upcoming movie: elapsed < 0 -> stage is UPCOMING, not active")
    void testUpcomingMovie() {
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 9, 25));
        movie.setExhibitionEndDate(LocalDate.of(2026, 10, 25));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("UPCOMING", stage);
        assertFalse(lifecycleService.isActive(movie, anchorDate));
        assertEquals("UPCOMING", lifecycleService.determineEffectiveStatus(movie));
    }

    @Test
    @DisplayName("Opening stage: rho in [0.00, 0.15) -> OPENING")
    void testOpeningStage() {
        // Window: 2026-09-15 to 2026-10-14 -> duration = 30 days
        // On 2026-09-18: elapsed = 3 days -> rho = 3/30 = 0.10 (< 0.15)
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 9, 15));
        movie.setExhibitionEndDate(LocalDate.of(2026, 10, 14));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("OPENING", stage);
        assertTrue(lifecycleService.isActive(movie, anchorDate));
        assertEquals("AIRING", lifecycleService.determineEffectiveStatus(movie));
    }

    @Test
    @DisplayName("Peak stage: rho in [0.15, 0.40) -> PEAK")
    void testPeakStage() {
        // Window: 2026-09-08 to 2026-10-07 -> duration = 30 days
        // On 2026-09-18: elapsed = 10 days -> rho = 10/30 = 0.333 (< 0.40)
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 9, 8));
        movie.setExhibitionEndDate(LocalDate.of(2026, 10, 7));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("PEAK", stage);
        assertTrue(lifecycleService.isActive(movie, anchorDate));
        assertEquals("AIRING", lifecycleService.determineEffectiveStatus(movie));
    }

    @Test
    @DisplayName("Regular run: rho in [0.40, 0.70) -> REGULAR_RUN")
    void testRegularRunStage() {
        // Window: 2026-08-25 to 2026-10-03 -> duration = 40 days
        // On 2026-09-18: elapsed = 24 days -> rho = 24/40 = 0.60
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 8, 25));
        movie.setExhibitionEndDate(LocalDate.of(2026, 10, 3));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("REGULAR_RUN", stage);
        assertTrue(lifecycleService.isActive(movie, anchorDate));
        assertEquals("AIRING", lifecycleService.determineEffectiveStatus(movie));
    }

    @Test
    @DisplayName("Reduced run: rho in [0.70, 0.90) -> REDUCED_RUN")
    void testReducedRunStage() {
        // Window: 2026-08-10 to 2026-09-28 -> duration = 50 days
        // On 2026-09-18: elapsed = 39 days -> rho = 39/50 = 0.78
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 8, 10));
        movie.setExhibitionEndDate(LocalDate.of(2026, 9, 28));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("REDUCED_RUN", stage);
        assertTrue(lifecycleService.isActive(movie, anchorDate));
        assertEquals("AIRING", lifecycleService.determineEffectiveStatus(movie));
    }

    @Test
    @DisplayName("Final days stage: rho in [0.90, 1.00) -> FINAL_DAYS")
    void testFinalDaysStage() {
        // Window: 2026-08-15 to 2026-09-20 -> duration = 37 days
        // On 2026-09-18: elapsed = 34 days -> rho = 34/37 = 0.9189
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 8, 15));
        movie.setExhibitionEndDate(LocalDate.of(2026, 9, 20));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("FINAL_DAYS", stage);
        assertTrue(lifecycleService.isActive(movie, anchorDate));
        assertEquals("AIRING", lifecycleService.determineEffectiveStatus(movie));
    }

    @Test
    @DisplayName("Boundary dates: first day (elapsed = 0) -> OPENING; last day (elapsed = D-1) -> FINAL_DAYS; day after end (elapsed = D) -> ENDED")
    void testBoundaryDates() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 10); // D = 10 days

        // On start date (2026-09-01): elapsed = 0, rho = 0.0 -> OPENING
        assertEquals("OPENING", lifecycleService.evaluateStage(start, end, start));

        // On last date (2026-09-10): elapsed = 9, rho = 9/10 = 0.90 -> FINAL_DAYS
        assertEquals("FINAL_DAYS", lifecycleService.evaluateStage(start, end, end));

        // On day after end date (2026-09-11): elapsed = 10 >= D -> ENDED
        assertEquals("ENDED", lifecycleService.evaluateStage(start, end, end.plusDays(1)));

        // Day before start date: elapsed = -1 < 0 -> UPCOMING
        assertEquals("UPCOMING", lifecycleService.evaluateStage(start, end, start.minusDays(1)));
    }

    @Test
    @DisplayName("Expired / Ended movie: elapsed >= D -> stage is ENDED, not active")
    void testEndedMovie() {
        Movie movie = new Movie();
        movie.setExhibitionStartDate(LocalDate.of(2026, 6, 1));
        movie.setExhibitionEndDate(LocalDate.of(2026, 7, 15));

        String stage = lifecycleService.calculateLifecycleStage(movie);
        assertEquals("ENDED", stage);
        assertFalse(lifecycleService.isActive(movie, anchorDate));
        assertEquals("ENDED", lifecycleService.determineEffectiveStatus(movie));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLOCKBUSTER", "MAJOR_REGIONAL", "MID_TIER", "NICHE", "RE_RELEASE"})
    @DisplayName("Demand tier validation: valid tiers are accepted")
    void testValidDemandTiers(String tier) {
        assertDoesNotThrow(() ->
                lifecycleService.validateMovieMetadata(tier, new BigDecimal("8.50"), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_BLOCKBUSTER", "FLOP", "UNKNOWN", "INDIE"})
    @DisplayName("Demand tier validation: invalid tiers throw BadRequestException")
    void testInvalidDemandTiers(String invalidTier) {
        assertThrows(BadRequestException.class, () ->
                lifecycleService.validateMovieMetadata(invalidTier, new BigDecimal("8.50"), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));
    }

    @Test
    @DisplayName("Popularity score validation: bounds [0.00, 10.00]")
    void testPopularityScoreBounds() {
        assertDoesNotThrow(() ->
                lifecycleService.validateMovieMetadata("MID_TIER", BigDecimal.ZERO, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));
        assertDoesNotThrow(() ->
                lifecycleService.validateMovieMetadata("MID_TIER", BigDecimal.TEN, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));

        assertThrows(BadRequestException.class, () ->
                lifecycleService.validateMovieMetadata("MID_TIER", new BigDecimal("-0.01"), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));
        assertThrows(BadRequestException.class, () ->
                lifecycleService.validateMovieMetadata("MID_TIER", new BigDecimal("10.01"), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));
    }

    @Test
    @DisplayName("Date order validation: endDate must be >= startDate")
    void testDateOrderValidation() {
        assertDoesNotThrow(() ->
                lifecycleService.validateMovieMetadata("MID_TIER", new BigDecimal("5.00"), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)));
        assertDoesNotThrow(() ->
                lifecycleService.validateMovieMetadata("MID_TIER", new BigDecimal("5.00"), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20)));

        assertThrows(BadRequestException.class, () ->
                lifecycleService.validateMovieMetadata("MID_TIER", new BigDecimal("5.00"), LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 10)));
    }
}
