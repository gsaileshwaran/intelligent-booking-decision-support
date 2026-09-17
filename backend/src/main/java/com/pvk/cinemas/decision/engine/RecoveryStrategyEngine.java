package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.decision.dto.FrictionAlert;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.RecoveryStrategy;
import com.pvk.cinemas.decision.dto.SeatRecommendationRequest;
import com.pvk.cinemas.decision.dto.SeatRecommendationResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecoveryStrategyEngine {

    private final SeatRecommendationEngine seatRecommendationEngine;

    public RecoveryStrategyEngine(SeatRecommendationEngine seatRecommendationEngine) {
        this.seatRecommendationEngine = seatRecommendationEngine;
    }

    public List<RecoveryStrategy> generateRecoveryStrategies(Long showId, List<Long> selectedSeatIds, List<FrictionAlert> alerts) {
        List<RecoveryStrategy> strategies = new ArrayList<>();
        if (alerts == null || alerts.isEmpty() || selectedSeatIds == null || selectedSeatIds.isEmpty()) {
            return strategies;
        }

        boolean hasSplit = alerts.stream().anyMatch(a -> "SPLIT_GROUP".equalsIgnoreCase(a.getType()));
        boolean hasSuboptimal = alerts.stream().anyMatch(a -> "SUBOPTIMAL_VIEW".equalsIgnoreCase(a.getType()));
        boolean hasOrphan = alerts.stream().anyMatch(a -> "ORPHAN_SEAT".equalsIgnoreCase(a.getType()));

        if (hasSplit || hasSuboptimal) {
            SeatRecommendationResponse recs = seatRecommendationEngine.recommend(
                    showId,
                    new SeatRecommendationRequest(selectedSeatIds.size(), "BEST_VIEW")
            );
            if (!recs.getRecommendations().isEmpty()) {
                RecommendedBlock best = recs.getRecommendations().get(0);
                if (!best.getSeatIds().equals(selectedSeatIds)) {
                    strategies.add(new RecoveryStrategy(
                            "Auto-Align to Prime Contiguous Seating",
                            "Shift to " + String.join(", ", best.getSeatLabels()) + " (" + best.getAverageScore() + "/100 quality score) to sit together in the optimal viewing tier.",
                            "AUTO_SELECT_CONTIGUOUS",
                            best.getSeatIds(),
                            best.getSeatLabels()
                    ));
                }
            }
        }

        if (hasOrphan) {
            SeatRecommendationResponse balanced = seatRecommendationEngine.recommend(
                    showId,
                    new SeatRecommendationRequest(selectedSeatIds.size(), "BALANCED")
            );
            if (!balanced.getRecommendations().isEmpty()) {
                RecommendedBlock block = balanced.getRecommendations().get(0);
                if (!block.getSeatIds().equals(selectedSeatIds)) {
                    strategies.add(new RecoveryStrategy(
                            "Shift One Seat to Maintain Clean Paired Seating",
                            "Select " + String.join(", ", block.getSeatLabels()) + " to eliminate the stranded single seat and keep contiguous aisles.",
                            "SHIFT_SEATS",
                            block.getSeatIds(),
                            block.getSeatLabels()
                    ));
                }
            }
        }

        return strategies;
    }
}
