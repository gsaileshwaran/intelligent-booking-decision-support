package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.decision.dto.DecisionGuidanceRequest;
import com.pvk.cinemas.decision.dto.DecisionGuidanceResponse;
import com.pvk.cinemas.decision.dto.FrictionAlert;
import com.pvk.cinemas.decision.dto.RecoveryStrategy;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DecisionGuidanceEngine {

    private final SeatRepository seatRepository;
    private final SeatScoringEngine seatScoringEngine;
    private final DecisionFrictionDetector frictionDetector;
    private final RecoveryStrategyEngine recoveryEngine;

    public DecisionGuidanceEngine(SeatRepository seatRepository,
                                  SeatScoringEngine seatScoringEngine,
                                  DecisionFrictionDetector frictionDetector,
                                  RecoveryStrategyEngine recoveryEngine) {
        this.seatRepository = seatRepository;
        this.seatScoringEngine = seatScoringEngine;
        this.frictionDetector = frictionDetector;
        this.recoveryEngine = recoveryEngine;
    }

    public DecisionGuidanceResponse evaluateGuidance(Long showId, DecisionGuidanceRequest request) {
        if (request == null || request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            return new DecisionGuidanceResponse(0, "STANDARD", List.of("No seats currently selected"), List.of(), List.of());
        }

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        List<SeatScoreDTO> scores = seats.stream().map(seatScoringEngine::scoreSeat).toList();

        int avgScore = (int) Math.round(scores.stream().mapToInt(SeatScoreDTO::getScore).average().orElse(70));
        String rating;
        if (avgScore >= 90) rating = "OPTIMAL";
        else if (avgScore >= 80) rating = "PRIME";
        else if (avgScore >= 70) rating = "GOOD";
        else rating = "FAIR";

        List<String> keyStrengths = new ArrayList<>();
        boolean hasCenter = scores.stream().anyMatch(s -> s.getLateralFactor() >= 0.90);
        boolean hasDistance = scores.stream().anyMatch(s -> s.getDistanceFactor() >= 0.95);
        boolean hasAtmos = scores.stream().anyMatch(s -> s.getAcousticFactor() >= 0.95);

        if (hasCenter) {
            keyStrengths.add("Centered viewing perspective with balanced optical sightline to the screen.");
        }
        if (hasDistance) {
            String goodRows = scores.stream()
                    .filter(s -> s.getDistanceFactor() >= 0.90)
                    .map(SeatScoreDTO::getRowLabel)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));
            String rowPart = !goodRows.isEmpty() ? " (Row " + goodRows + ")" : "";
            keyStrengths.add("Ergonomic row placement" + rowPart + " providing comfortable viewing distance.");
        }
        if (hasAtmos) {
            keyStrengths.add("Acoustic calibration zone for balanced Dolby Atmos multi-channel surround immersion.");
        }
        if (keyStrengths.isEmpty()) {
            keyStrengths.add("Standard auditorium seating with clear sightlines to the primary screen.");
        }

        List<FrictionAlert> alerts = frictionDetector.detectFriction(showId, request.getSeatIds());
        List<RecoveryStrategy> strategies = recoveryEngine.generateRecoveryStrategies(showId, request.getSeatIds(), alerts);

        return new DecisionGuidanceResponse(avgScore, rating, keyStrengths, alerts, strategies);
    }
}
