package com.pvk.cinemas.decision;

import com.pvk.cinemas.decision.dto.FrictionAlert;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.RecoveryStrategy;
import com.pvk.cinemas.decision.dto.SeatRecommendationResponse;
import com.pvk.cinemas.decision.engine.RecoveryStrategyEngine;
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

class RecoveryStrategyEngineTest {

    private SeatRecommendationEngine seatRecommendationEngine;
    private RecoveryStrategyEngine recoveryEngine;

    @BeforeEach
    void setUp() {
        seatRecommendationEngine = Mockito.mock(SeatRecommendationEngine.class);
        recoveryEngine = new RecoveryStrategyEngine(seatRecommendationEngine);
    }

    @Test
    @DisplayName("Generate auto-align recovery strategy when split group is detected")
    void testGenerateRecoveryForSplitGroup() {
        Long showId = 101L;
        List<Long> splitSeatIds = List.of(1L, 20L); // across rows

        List<FrictionAlert> alerts = List.of(new FrictionAlert("SPLIT_GROUP", "WARNING", "Split Group", "Seats are in different rows"));

        RecommendedBlock primeBlock = new RecommendedBlock(
                "Optimal Block", "OPTIMAL", List.of(5L, 6L), List.of("C5", "C6"), 95, BigDecimal.valueOf(300.0), "Prime contiguous pair"
        );
        when(seatRecommendationEngine.recommend(eq(showId), any())).thenReturn(
                new SeatRecommendationResponse(showId, 2, "BEST_VIEW", List.of(primeBlock))
        );

        List<RecoveryStrategy> strategies = recoveryEngine.generateRecoveryStrategies(showId, splitSeatIds, alerts);

        assertNotNull(strategies);
        assertFalse(strategies.isEmpty());
        RecoveryStrategy strategy = strategies.get(0);
        assertEquals("AUTO_SELECT_CONTIGUOUS", strategy.getActionType());
        assertEquals(List.of(5L, 6L), strategy.getSuggestedSeatIds());
        assertEquals(List.of("C5", "C6"), strategy.getSuggestedSeatLabels());
    }
}
