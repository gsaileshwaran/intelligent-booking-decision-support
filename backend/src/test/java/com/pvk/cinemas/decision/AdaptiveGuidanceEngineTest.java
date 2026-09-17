package com.pvk.cinemas.decision;

import com.pvk.cinemas.decision.dto.*;
import com.pvk.cinemas.decision.engine.AdaptiveGuidanceEngine;
import com.pvk.cinemas.decision.engine.SeatRecommendationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AdaptiveGuidanceEngineTest {

    private SeatRecommendationEngine seatRecommendationEngine;
    private AdaptiveGuidanceEngine engine;

    @BeforeEach
    void setUp() {
        seatRecommendationEngine = Mockito.mock(SeatRecommendationEngine.class);
        engine = new AdaptiveGuidanceEngine(seatRecommendationEngine);
    }

    @Test
    @DisplayName("Adapt rationale for solo, couple, and group party sizes")
    void testAdaptForPartySize() {
        Long showId = 3001L;

        RecommendedBlock blockSolo = new RecommendedBlock("Solo", "OPTIMAL", List.of(1L), List.of("C5"), 95, BigDecimal.valueOf(150.0), "");
        RecommendedBlock blockCouple = new RecommendedBlock("Couple", "OPTIMAL", List.of(1L, 2L), List.of("C5", "C6"), 94, BigDecimal.valueOf(300.0), "");

        when(seatRecommendationEngine.recommend(eq(showId), any())).thenReturn(
                new SeatRecommendationResponse(showId, 1, "BEST_VIEW", List.of(blockSolo)),
                new SeatRecommendationResponse(showId, 2, "BALANCED", List.of(blockCouple))
        );

        SeatRecommendationResponse soloResp = engine.adaptForParty(showId, 1, "BEST_VIEW");
        assertTrue(soloResp.getRecommendations().get(0).getRationale().contains("Solo viewer"));

        SeatRecommendationResponse coupleResp = engine.adaptForParty(showId, 2, "BALANCED");
        assertTrue(coupleResp.getRecommendations().get(0).getRationale().contains("Couple"));
    }

    @Test
    @DisplayName("Adapt guidance prompts according to journey stage")
    void testJourneyStageGuidanceEvolution() {
        // Stage 1: City
        JourneyStageGuidanceRequest cityReq = new JourneyStageGuidanceRequest();
        cityReq.setCurrentStage("CITY_DISCOVERY");
        cityReq.setCityName("Chennai");
        JourneyStageGuidanceResponse cityResp = engine.evaluateJourneyGuidance(cityReq);
        assertEquals("SELECT_MOVIE", cityResp.getNextRecommendedAction());
        assertTrue(cityResp.getGuidancePrompt().contains("Chennai"));

        // Stage 2: Showtime Selection
        JourneyStageGuidanceRequest showReq = new JourneyStageGuidanceRequest();
        showReq.setCurrentStage("SHOW_SELECTION");
        showReq.setMovieTitle("Interstellar");
        JourneyStageGuidanceResponse showResp = engine.evaluateJourneyGuidance(showReq);
        assertEquals("SELECT_SHOWTIME", showResp.getNextRecommendedAction());
        assertTrue(showResp.getGuidancePrompt().contains("Interstellar"));

        // Stage 3: Seat Selection
        JourneyStageGuidanceRequest seatReq = new JourneyStageGuidanceRequest();
        seatReq.setCurrentStage("SEAT_SELECTION");
        seatReq.setSelectedSeatCount(2);
        JourneyStageGuidanceResponse seatResp = engine.evaluateJourneyGuidance(seatReq);
        assertEquals("INITIATE_HOLD", seatResp.getNextRecommendedAction());

        // Stage 4: Seat Hold Countdown
        JourneyStageGuidanceRequest holdReq = new JourneyStageGuidanceRequest();
        holdReq.setCurrentStage("SEAT_HOLD");
        holdReq.setHoldSecondsRemaining(272); // 4 mins 32 secs
        JourneyStageGuidanceResponse holdResp = engine.evaluateJourneyGuidance(holdReq);
        assertEquals("PROCEED_TO_CHECKOUT", holdResp.getNextRecommendedAction());
        assertTrue(holdResp.getGuidancePrompt().contains("04:32"));
    }
}
