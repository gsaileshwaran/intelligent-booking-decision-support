package com.pvk.cinemas.decision;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.decision.dto.*;
import com.pvk.cinemas.decision.engine.DecisionNegotiationEngine;
import com.pvk.cinemas.decision.engine.SeatRecommendationEngine;
import com.pvk.cinemas.decision.engine.SeatScoringEngine;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DecisionNegotiationEngineTest {

    private ShowRepository showRepository;
    private ShowSeatRepository showSeatRepository;
    private ScreenCapabilityRepository screenCapabilityRepository;
    private ScreenRepository screenRepository;
    private TheatreRepository theatreRepository;
    private SeatRepository seatRepository;
    private SeatScoringEngine seatScoringEngine;
    private SeatRecommendationEngine recommendationEngine;
    private DecisionNegotiationEngine engine;

    @BeforeEach
    void setUp() {
        showRepository = Mockito.mock(ShowRepository.class);
        showSeatRepository = Mockito.mock(ShowSeatRepository.class);
        screenCapabilityRepository = Mockito.mock(ScreenCapabilityRepository.class);
        screenRepository = Mockito.mock(ScreenRepository.class);
        theatreRepository = Mockito.mock(TheatreRepository.class);
        seatRepository = Mockito.mock(SeatRepository.class);
        seatScoringEngine = Mockito.mock(SeatScoringEngine.class);
        recommendationEngine = Mockito.mock(SeatRecommendationEngine.class);

        engine = new DecisionNegotiationEngine(
                showRepository, showSeatRepository, screenCapabilityRepository,
                screenRepository, theatreRepository, seatRepository,
                seatScoringEngine, recommendationEngine
        );
    }

    private Seat createSeat(Long seatId, Long screenId, String row, String num) {
        Seat seat = new Seat(screenId, 1L, row, num);
        seat.setSeatId(seatId);
        return seat;
    }

    @Test
    @DisplayName("Negotiate 2-row adjacent split when party of 4 cannot find contiguous block")
    void testNegotiateAdjacentTwoRowSplit() {
        Long showId = 5001L;

        Show show = new Show();
        show.setShowId(showId);
        show.setMovieLanguageId(1L);
        show.setScreenCapabilityId(1L);
        when(showRepository.findById(showId)).thenReturn(Optional.of(show));

        RecommendedBlock rowCBlock = new RecommendedBlock("Row C", "OPTIMAL", List.of(5L, 6L), List.of("C5", "C6"), 95, BigDecimal.valueOf(300.0), "Prime");
        when(recommendationEngine.recommend(eq(showId), any(SeatRecommendationRequest.class)))
                .thenAnswer(inv -> {
                    SeatRecommendationRequest req = inv.getArgument(1);
                    if (req.getPartySize() == 4) {
                        return new SeatRecommendationResponse(showId, 4, "BEST_VIEW", List.of());
                    } else {
                        return new SeatRecommendationResponse(showId, req.getPartySize(), "BEST_VIEW", List.of(rowCBlock));
                    }
                });

        // ShowSeats available in show: 5L, 6L, 15L, 16L
        when(showSeatRepository.findByIdShowId(showId)).thenReturn(List.of(
                new ShowSeat(showId, 5L, "AVAILABLE"),
                new ShowSeat(showId, 6L, "AVAILABLE"),
                new ShowSeat(showId, 15L, "AVAILABLE"),
                new ShowSeat(showId, 16L, "AVAILABLE")
        ));

        // Available seats in Row D: D5 (15L), D6 (16L)
        Seat seatD5 = createSeat(15L, 1L, "D", "5");
        Seat seatD6 = createSeat(16L, 1L, "D", "6");
        when(seatRepository.findAllById(any())).thenReturn(List.of(seatD5, seatD6));

        NegotiationResponse response = engine.negotiate(showId, new NegotiationRequest(4, null));

        assertNotNull(response);
        assertEquals(4, response.getPartySize());
        assertFalse(response.getAdjacentSplitOptions().isEmpty(), "Should offer 2-row adjacent split option");
        RecommendedBlock split = response.getAdjacentSplitOptions().get(0);
        assertEquals("ADJACENT_SPLIT", split.getCategory());
        assertEquals(4, split.getSeatIds().size());
        assertTrue(split.getSeatLabels().containsAll(List.of("C5", "C6", "D5", "D6")));
    }
}
