package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.SeatRecommendationRequest;
import com.pvk.cinemas.decision.dto.SeatRecommendationResponse;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SeatRecommendationEngine {

    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;
    private final SeatGroupPlanner seatGroupPlanner;

    public SeatRecommendationEngine(ShowSeatRepository showSeatRepository,
                                    SeatRepository seatRepository,
                                    SeatGroupPlanner seatGroupPlanner) {
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
        this.seatGroupPlanner = seatGroupPlanner;
    }

    public SeatRecommendationResponse recommend(Long showId, SeatRecommendationRequest request) {
        if (request == null || request.getPartySize() < 1) {
            throw new BadRequestException("Party size must be at least 1.");
        }
        int partySize = request.getPartySize();
        String preference = request.getPreference() != null ? request.getPreference().toUpperCase() : "BEST_VIEW";

        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(showId);
        Set<Long> availableSeatIds = showSeats.stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .map(ss -> ss.getId().getSeatId())
                .collect(Collectors.toSet());

        if (availableSeatIds.isEmpty()) {
            throw new BadRequestException("No available seats for this show.");
        }

        if (partySize > availableSeatIds.size()) {
            throw new BadRequestException(String.format("Requested party size of %d exceeds available capacity (%d seats) for this show.", partySize, availableSeatIds.size()));
        }

        List<Seat> allSeats = seatRepository.findAllById(showSeats.stream().map(ss -> ss.getId().getSeatId()).toList());

        // Delegate to authoritative shared group planner
        List<RecommendedBlock> candidateBlocks = seatGroupPlanner.evaluateGroupCandidates(showId, showSeats, allSeats, partySize, preference);

        // ---------------------------------------------------------------------
        // Curate Distinct Recommendations (Top pick + Acoustic/Prime + Value)
        // ---------------------------------------------------------------------
        List<RecommendedBlock> topRecommendations = new ArrayList<>();
        if (!candidateBlocks.isEmpty()) {
            RecommendedBlock best = candidateBlocks.get(0);
            topRecommendations.add(best);

            // Distinct Acoustic / Atmos sweet spot option (different block)
            RecommendedBlock acousticChoice = candidateBlocks.stream()
                    .filter(b -> !b.getSeatIds().equals(best.getSeatIds()))
                    .filter(b -> b.getRationale() != null && b.getRationale().contains("offset: 0"))
                    .findFirst()
                    .orElse(candidateBlocks.size() > 1 && !candidateBlocks.get(1).getSeatIds().equals(best.getSeatIds())
                            ? candidateBlocks.get(1) : null);

            if (acousticChoice != null && topRecommendations.stream().noneMatch(t -> t.getSeatIds().equals(acousticChoice.getSeatIds()))) {
                acousticChoice.setTitle("Dolby Atmos Sweet Spot");
                acousticChoice.setCategory("ACOUSTIC");
                topRecommendations.add(acousticChoice);
            }

            // Distinct Smart Value / Budget option
            RecommendedBlock valueChoice = candidateBlocks.stream()
                    .filter(b -> topRecommendations.stream().noneMatch(t -> t.getSeatIds().equals(b.getSeatIds())))
                    .min(Comparator.comparing(RecommendedBlock::getTotalPrice))
                    .orElse(null);

            if (valueChoice != null && topRecommendations.stream().noneMatch(t -> t.getSeatIds().equals(valueChoice.getSeatIds()))) {
                valueChoice.setTitle("Smart Value Choice");
                valueChoice.setCategory("VALUE");
                topRecommendations.add(valueChoice);
            }
        }

        return new SeatRecommendationResponse(showId, partySize, preference, topRecommendations);
    }
}
