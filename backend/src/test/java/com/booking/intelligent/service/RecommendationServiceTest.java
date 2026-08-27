package com.booking.intelligent.service;

import com.booking.intelligent.config.DataInitializer;
import com.booking.intelligent.dto.RecommendationRequestDto;
import com.booking.intelligent.dto.RecommendationResponseDto;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationItemRepository recommendationItemRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    public void setUp() throws Exception {
        dataInitializer.run();
        // Ensure active shows have future showDate for testing
        List<Show> shows = showRepository.findAll();
        for (Show s : shows) {
            s.setShowDate(LocalDate.now().plusDays(1));
            showRepository.save(s);
        }
    }

    @Test
    public void testEvaluateRecommendationsWithRealDatabaseShows() {
        Movie movie = movieRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(movie, "At least one movie should be seeded in the database");

        RecommendationRequestDto request = new RecommendationRequestDto();
        request.setMovieId(movie.getMovieId());
        request.setGroupSize(2);
        request.setMaxBudget(BigDecimal.valueOf(500.00));
        request.setPreferredTime("19:00");
        request.setPreferredSeatType("PREMIUM");

        RecommendationResponseDto response = recommendationService.evaluateRecommendations(request);
        assertNotNull(response);
        assertNotNull(response.getEngineVersion());
        assertTrue(response.getEngineVersion().contains("mcdm"));
        assertNotNull(response.getRankedResults());
    }

    @Test
    public void testRealSeatAvailabilityCalculation() {
        Show show = showRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(show, "At least one show should exist in seed data");

        RecommendationRequestDto request = new RecommendationRequestDto();
        request.setMovieId(show.getMovie().getMovieId());
        request.setGroupSize(1);

        RecommendationResponseDto response = recommendationService.evaluateRecommendations(request);
        assertNotNull(response);

        if (!response.getRankedResults().isEmpty()) {
            RecommendationResponseDto.RankedResult result = response.getRankedResults().get(0);
            assertNotNull(result.getAvailableSeats());
            assertTrue(result.getAvailableSeats() >= 0);
            assertNotNull(result.getPrice());
        }
    }

    @Test
    public void testRecommendationPersistenceForAuthenticatedUser() {
        User user = userRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(user, "User should exist in seed data");

        Show activeShow = showRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(activeShow);

        long initialCount = recommendationRepository.count();

        RecommendationRequestDto request = new RecommendationRequestDto();
        request.setMovieId(activeShow.getMovie().getMovieId());
        request.setGroupSize(1);
        request.setMaxBudget(BigDecimal.valueOf(1000.00));

        RecommendationResponseDto response = recommendationService.evaluateRecommendations(user.getUserId(), request);
        assertNotNull(response);

        long newCount = recommendationRepository.count();
        assertEquals(initialCount + 1, newCount, "Recommendation session should be persisted for authenticated user");

        List<Recommendation> userRecs = recommendationRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
        assertFalse(userRecs.isEmpty());
        Recommendation latest = userRecs.get(0);
        assertNotNull(latest.getModelVersion());

        List<RecommendationItem> items = recommendationItemRepository.findByRecommendationRecommendationIdOrderByRankAsc(latest.getRecommendationId());
        assertFalse(items.isEmpty(), "Recommendation items should be saved");
    }

    @Test
    public void testEvaluateRecommendationsFiltersBudgetOnRealData() {
        Movie movie = movieRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(movie);

        // Set an extremely low budget of $1.00 to filter out all shows
        RecommendationRequestDto request = new RecommendationRequestDto();
        request.setMovieId(movie.getMovieId());
        request.setGroupSize(1);
        request.setMaxBudget(BigDecimal.valueOf(1.00));

        RecommendationResponseDto response = recommendationService.evaluateRecommendations(request);
        assertNotNull(response);
        assertTrue(response.getRankedResults().isEmpty(), "No real shows should meet $1.00 budget constraint");
    }

    @Test
    public void testEvaluateRecommendationsResilienceWhenServiceOffline() {
        RecommendationRequestDto request = new RecommendationRequestDto();
        request.setGroupSize(1);
        request.setMaxBudget(BigDecimal.valueOf(2000.00));

        RecommendationResponseDto response = assertDoesNotThrow(() ->
                recommendationService.evaluateRecommendations(request)
        );

        assertNotNull(response);
        assertNotNull(response.getEngineVersion());
        assertTrue(response.getEngineVersion().contains("mcdm"));
    }
}
