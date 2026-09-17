package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
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
    private final SeatScoringEngine seatScoringEngine;
    private final SeatPricingService seatPricingService;

    public SeatRecommendationEngine(ShowSeatRepository showSeatRepository,
                                    SeatRepository seatRepository,
                                    SeatScoringEngine seatScoringEngine,
                                    SeatPricingService seatPricingService) {
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
        this.seatScoringEngine = seatScoringEngine;
        this.seatPricingService = seatPricingService;
    }

    public SeatRecommendationResponse recommend(Long showId, SeatRecommendationRequest request) {
        int partySize = Math.max(1, Math.min(10, request.getPartySize()));
        String preference = request.getPreference() != null ? request.getPreference().toUpperCase() : "BEST_VIEW";

        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(showId);
        Set<Long> availableSeatIds = showSeats.stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .map(ss -> ss.getId().getSeatId())
                .collect(Collectors.toSet());

        if (availableSeatIds.isEmpty()) {
            return new SeatRecommendationResponse(showId, partySize, preference, Collections.emptyList());
        }

        List<Seat> allSeats = seatRepository.findAllById(showSeats.stream().map(ss -> ss.getId().getSeatId()).toList());
        Map<Long, Seat> seatMap = allSeats.stream().collect(Collectors.toMap(Seat::getSeatId, s -> s));
        Map<Long, SeatScoreDTO> scoreMap = allSeats.stream().collect(Collectors.toMap(Seat::getSeatId, seatScoringEngine::scoreSeat));

        SeatPricingService.ShowPricingContext showCtx = seatPricingService != null 
                ? seatPricingService.getShowPricingContext(showId) : null;

        // Group available seats by row label (excluding physically blocked seats)
        Map<String, List<Seat>> seatsByRow = allSeats.stream()
                .filter(s -> availableSeatIds.contains(s.getSeatId()) && (s.getStatus() == null || "ACTIVE".equalsIgnoreCase(s.getStatus())))
                .collect(Collectors.groupingBy(Seat::getRowLabel));

        List<RecommendedBlock> candidateBlocks = new ArrayList<>();

        // ---------------------------------------------------------------------
        // 1. Discover Single-Row Contiguous Blocks (Without Crossing Aisles)
        // ---------------------------------------------------------------------
        for (Map.Entry<String, List<Seat>> entry : seatsByRow.entrySet()) {
            String rowLabel = entry.getKey();
            List<Seat> rowSeats = new ArrayList<>(entry.getValue());
            rowSeats.sort(Comparator.comparingInt(s -> parseSeatNum(s.getSeatNumber())));

            // Split row seats into sub-banks separated by aisles or booked seats
            List<List<Seat>> contiguousBanks = new ArrayList<>();
            List<Seat> currentBank = new ArrayList<>();

            for (int k = 0; k < rowSeats.size(); k++) {
                Seat seat = rowSeats.get(k);
                currentBank.add(seat);

                boolean hasAisle = seat.getAisleAfter() != null && seat.getAisleAfter();
                boolean isLastInRow = (k == rowSeats.size() - 1);
                boolean isNumericBreak = !isLastInRow && (parseSeatNum(rowSeats.get(k + 1).getSeatNumber()) != parseSeatNum(seat.getSeatNumber()) + 1);

                if (hasAisle || isNumericBreak || isLastInRow) {
                    if (!currentBank.isEmpty()) {
                        contiguousBanks.add(new ArrayList<>(currentBank));
                        currentBank.clear();
                    }
                }
            }

            // Find windows of partySize in each bank
            for (List<Seat> bank : contiguousBanks) {
                if (bank.size() >= partySize) {
                    for (int i = 0; i <= bank.size() - partySize; i++) {
                        List<Seat> block = bank.subList(i, i + partySize);
                        RecommendedBlock rec = scoreGroupBlock(block, partySize, rowLabel, scoreMap, showCtx, false);
                        candidateBlocks.add(rec);
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 2. Fallback Split-Group Strategy (when no single contiguous block exists)
        // e.g. for partySize = 4: evaluate 2+2 split across aisle or adjacent rows
        // ---------------------------------------------------------------------
        if (candidateBlocks.isEmpty() && partySize > 1) {
            candidateBlocks.addAll(findSplitGroupCandidates(seatsByRow, partySize, scoreMap, showCtx));
        }

        // ---------------------------------------------------------------------
        // 3. Fallback Greedy Best Individual Seats (when even split blocks fail)
        // ---------------------------------------------------------------------
        if (candidateBlocks.isEmpty()) {
            List<Seat> bestIndividuals = allSeats.stream()
                    .filter(s -> availableSeatIds.contains(s.getSeatId()))
                    .sorted(Comparator.comparingInt((Seat s) -> scoreMap.get(s.getSeatId()).getScore()).reversed())
                    .limit(partySize)
                    .toList();

            if (!bestIndividuals.isEmpty()) {
                candidateBlocks.add(scoreGroupBlock(bestIndividuals, partySize, "Mixed", scoreMap, showCtx, true));
            }
        }

        // ---------------------------------------------------------------------
        // 4. Rank Candidates by User Preference
        // ---------------------------------------------------------------------
        if ("BUDGET".equalsIgnoreCase(preference)) {
            candidateBlocks.sort(Comparator.comparing(RecommendedBlock::getTotalPrice)
                    .thenComparing(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed()));
        } else {
            candidateBlocks.sort(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed()
                    .thenComparing(RecommendedBlock::getTotalPrice));
        }

        // ---------------------------------------------------------------------
        // 5. Build Curated Distinct Recommendations (Top pick + Acoustic + Value)
        // ---------------------------------------------------------------------
        List<RecommendedBlock> topRecommendations = new ArrayList<>();
        if (!candidateBlocks.isEmpty()) {
            RecommendedBlock best = candidateBlocks.get(0);
            if (!"SPLIT_GROUP".equals(best.getCategory())) {
                best.setTitle("Best Visual Balance");
                best.setCategory("OPTIMAL_VIEW");
            }
            topRecommendations.add(best);

            // Distinct Acoustic / Atmos sweet spot option (only if contiguous)
            RecommendedBlock acousticChoice = candidateBlocks.stream()
                    .filter(b -> !"SPLIT_GROUP".equals(b.getCategory()))
                    .filter(b -> !b.getSeatIds().equals(best.getSeatIds()))
                    .filter(b -> b.getRationale() != null && b.getRationale().contains("center"))
                    .findFirst()
                    .orElse(candidateBlocks.size() > 1 && !"SPLIT_GROUP".equals(candidateBlocks.get(1).getCategory()) ? candidateBlocks.get(1) : null);

            if (acousticChoice != null && !topRecommendations.contains(acousticChoice)) {
                acousticChoice.setTitle("Dolby Atmos Sweet Spot");
                acousticChoice.setCategory("ACOUSTIC");
                topRecommendations.add(acousticChoice);
            }

            // Distinct Smart Value / Budget option
            RecommendedBlock valueChoice = candidateBlocks.stream()
                    .filter(b -> topRecommendations.stream().noneMatch(t -> t.getSeatIds().equals(b.getSeatIds())))
                    .min(Comparator.comparing(RecommendedBlock::getTotalPrice))
                    .orElse(null);

            if (valueChoice != null && !topRecommendations.contains(valueChoice)) {
                valueChoice.setTitle("Smart Value Choice");
                valueChoice.setCategory("VALUE");
                topRecommendations.add(valueChoice);
            }
        }

        return new SeatRecommendationResponse(showId, partySize, preference, topRecommendations);
    }

    private RecommendedBlock scoreGroupBlock(List<Seat> block, int partySize, String rowLabel,
                                             Map<Long, SeatScoreDTO> scoreMap,
                                             SeatPricingService.ShowPricingContext showCtx,
                                             boolean isDisjoint) {
        List<Long> blockIds = block.stream().map(Seat::getSeatId).toList();
        List<String> blockLabels = block.stream().map(s -> s.getRowLabel() + s.getSeatNumber()).toList();

        // 1. Average individual seat score and weakest individual seat
        double avgIndividual = block.stream()
                .mapToInt(s -> scoreMap.get(s.getSeatId()).getScore())
                .average().orElse(70);
        int minIndividual = block.stream()
                .mapToInt(s -> scoreMap.get(s.getSeatId()).getScore())
                .min().orElse(60);

        double blendedQuality = (0.75 * avgIndividual) + (0.25 * minIndividual);

        // 2. Group horizontal center alignment
        int minCol = block.stream().mapToInt(s -> parseSeatNum(s.getSeatNumber())).min().orElse(5);
        int maxCol = block.stream().mapToInt(s -> parseSeatNum(s.getSeatNumber())).max().orElse(5);
        double groupCenter = (minCol + maxCol) / 2.0;

        // Dynamic auditorium center alignment from screen geometry (fallback to 5.5)
        double rowCenter = 5.5;
        if (seatScoringEngine != null && !block.isEmpty() && block.get(0).getScreenId() != null) {
            SeatScoringEngine.ScreenGeometry geom = seatScoringEngine.getScreenGeometry(block.get(0).getScreenId());
            if (geom != null) {
                rowCenter = (geom.getSeatsInRow(rowLabel) + 1.0) / 2.0;
            }
        }
        double lateralOffset = Math.abs(groupCenter - rowCenter);
        double maxLateral = Math.max(1.0, rowCenter - 1.0);
        double groupCenterFactor = 1.0 - (0.15 * Math.min(1.0, lateralOffset / maxLateral));

        // 3. Contiguity penalty if disjoint or split
        int contiguityPenalty = isDisjoint ? 35 : 0;

        int groupScore = (int) Math.round(Math.min(99, Math.max(35, (blendedQuality * groupCenterFactor) - contiguityPenalty)));

        // Total price
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Seat s : block) {
            BigDecimal price = seatPricingService != null 
                    ? seatPricingService.calculateSeatPrice(s, showCtx)
                    : scoreMap.get(s.getSeatId()).getPrice();
            totalCost = totalCost.add(price != null ? price : new BigDecimal("150.00"));
        }

        String zone = block.get(0).getPricingZone() != null ? block.get(0).getPricingZone() : "Standard";
        String rationale;
        String title;
        String category;
        if (!isDisjoint) {
            rationale = String.format("Contiguous %d-seat block in Row %s (%s). Near horizontal center (offset: %.1f seats), %s zone.",
                    partySize, rowLabel, String.join(", ", blockLabels), lateralOffset, zone);
            title = "Best Visual Balance";
            category = "OPTIMAL_VIEW";
        } else {
            rationale = String.format("No single-row contiguous block of %d is currently available. Alternative group seating (Clustered group): %d seats (%s) arranged in close proximity.",
                    partySize, partySize, String.join(", ", blockLabels));
            title = "Alternative Group Seating";
            category = "SPLIT_GROUP";
        }

        return new RecommendedBlock(title, category, blockIds, blockLabels, groupScore, totalCost, rationale);
    }

    private List<RecommendedBlock> findSplitGroupCandidates(Map<String, List<Seat>> seatsByRow, int partySize,
                                                           Map<Long, SeatScoreDTO> scoreMap,
                                                           SeatPricingService.ShowPricingContext showCtx) {
        List<RecommendedBlock> splits = new ArrayList<>();
        int half = partySize / 2;
        int rem = partySize - half;

        // Look for 2 pairs (or half + rem) in adjacent rows
        for (Map.Entry<String, List<Seat>> e1 : seatsByRow.entrySet()) {
            List<Seat> r1 = e1.getValue();
            if (r1.size() >= half) {
                for (Map.Entry<String, List<Seat>> e2 : seatsByRow.entrySet()) {
                    List<Seat> r2 = e2.getValue();
                    if (r2.size() >= rem && !e1.getKey().equals(e2.getKey())) {
                        List<Seat> combined = new ArrayList<>();
                        combined.addAll(r1.subList(0, half));
                        combined.addAll(r2.subList(0, rem));

                        // Ensure distinct seats
                        Set<Long> ids = combined.stream().map(Seat::getSeatId).collect(Collectors.toSet());
                        if (ids.size() == partySize) {
                            RecommendedBlock b = scoreGroupBlock(combined, partySize, e1.getKey() + "/" + e2.getKey(), scoreMap, showCtx, true);
                            b.setRationale(String.format("No single-row contiguous block of %d is currently available. Alternative group seating (Paired group): %d seats in Row %s + %d seats in Row %s (adjacent rows).",
                                    partySize, half, e1.getKey(), rem, e2.getKey()));
                            b.setTitle("Alternative Group Seating");
                            b.setCategory("SPLIT_GROUP");
                            splits.add(b);
                            if (splits.size() >= 3) return splits;
                        }
                    }
                }
            }
        }
        return splits;
    }

    private int parseSeatNum(String seatNumber) {
        if (seatNumber == null) return 5;
        try {
            return Integer.parseInt(seatNumber.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 5;
        }
    }
}
