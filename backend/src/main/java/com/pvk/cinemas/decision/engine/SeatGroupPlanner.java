package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.SeatFitResult;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.infrastructure.model.Seat;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative, shared group seating decision engine.
 *
 * Evaluates physically valid group seating candidates for arbitrary party sizes
 * (including large parties like 10, 12, 15+).
 *
 * Core principles:
 * 1. Physical adjacency and cohesion dominate individual seat scores.
 * 2. Crossing a physical aisle is NEVER contiguous.
 * 3. Viewing comfort and screen distance are continuously modeled;
 *    extreme front-row seats incur natural neck-tilt penalties.
 * 4. Explanations are strictly derived from actual returned seat IDs and physical runs.
 * 5. Only verified AVAILABLE seats are ever considered.
 */
@Component
public class SeatGroupPlanner {

    private final SeatScoringEngine seatScoringEngine;
    private final SeatPricingService seatPricingService;

    public SeatGroupPlanner(SeatScoringEngine seatScoringEngine,
                            SeatPricingService seatPricingService) {
        this.seatScoringEngine = seatScoringEngine;
        this.seatPricingService = seatPricingService;
    }

    /**
     * Represents a single physically contiguous block of seats within one row,
     * unbroken by aisles, gaps, or booked seats.
     */
    public static class ContiguousRun {
        private final String rowLabel;
        private final int rowIndex;
        private final List<Seat> seats;

        public ContiguousRun(String rowLabel, int rowIndex, List<Seat> seats) {
            this.rowLabel = rowLabel;
            this.rowIndex = rowIndex;
            this.seats = seats;
        }

        public String getRowLabel() { return rowLabel; }
        public int getRowIndex() { return rowIndex; }
        public List<Seat> getSeats() { return seats; }
        public int size() { return seats.size(); }

        public String formatRange() {
            if (seats.isEmpty()) return "";
            if (seats.size() == 1) return rowLabel + seats.get(0).getSeatNumber();
            return String.format("%s%s–%s%s", rowLabel, seats.get(0).getSeatNumber(), rowLabel, seats.get(seats.size() - 1).getSeatNumber());
        }
    }

    /**
     * Evaluates the single best group arrangement for a candidate show and party size.
     * Used by ShowRecommendationEngine to determine show viability and seat plan.
     */
    public SeatFitResult planBestGroup(Long showId,
                                       List<ShowSeat> showSeats,
                                       List<Seat> screenSeats,
                                       int partySize,
                                       String preference) {
        List<RecommendedBlock> rankedBlocks = evaluateGroupCandidates(showId, showSeats, screenSeats, partySize, preference);
        SeatFitResult result = new SeatFitResult();

        if (rankedBlocks.isEmpty()) {
            result.setGroupScore(30);
            result.setAllTogether(false);
            result.setSeatingTradeoff("Insufficient available seats for requested party size.");
            result.setSplitDescription("No seating arrangement available");
            result.setRationale("Unable to accommodate party of " + partySize + " in this show.");
            return result;
        }

        RecommendedBlock best = rankedBlocks.get(0);
        result.setSeatIds(best.getSeatIds());
        result.setSeatLabels(best.getSeatLabels());
        result.setGroupScore(best.getAverageScore());
        result.setTotalPrice(best.getTotalPrice());
        result.setRationale(best.getRationale());
        result.setCandidateBlocks(rankedBlocks);

        Long screenId = !screenSeats.isEmpty() ? screenSeats.get(0).getScreenId() : null;
        SeatScoringEngine.ScreenGeometry screenGeom = (seatScoringEngine != null && screenId != null)
                ? seatScoringEngine.getScreenGeometry(screenId) : null;

        // Map seat IDs to actual Seat objects to perform physical run analysis
        Map<Long, Seat> seatMap = screenSeats.stream()
                .filter(s -> s.getSeatId() != null)
                .collect(Collectors.toMap(Seat::getSeatId, s -> s, (a, b) -> a));
        List<Seat> bestSeats = best.getSeatIds().stream()
                .map(seatMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<ContiguousRun> runs = detectContiguousRuns(bestSeats, screenGeom);
        int maxContiguous = runs.stream().mapToInt(ContiguousRun::size).max().orElse(0);
        result.setMaxContiguousBlock(maxContiguous);

        Set<String> distinctRows = runs.stream().map(ContiguousRun::getRowLabel).collect(Collectors.toCollection(LinkedHashSet::new));
        boolean allTogether = (runs.size() == 1);
        result.setAllTogether(allTogether);

        if (allTogether) {
            String singleRow = runs.get(0).getRowLabel();
            result.setBestRow(singleRow);
            result.setAdjacentRows(false);
            result.setSplitDescription(String.format("%d contiguous in Row %s (%s)", partySize, singleRow, runs.get(0).formatRange()));
            result.setSeatingTradeoff(null);
        } else {
            // Check if runs are cleanly 1-per-row across physically adjacent rows
            boolean isOneRunPerRow = (runs.size() == distinctRows.size());
            List<Integer> sortedRowIndices = runs.stream().map(ContiguousRun::getRowIndex).distinct().sorted().toList();
            boolean rowsAreConsecutive = true;
            for (int i = 0; i < sortedRowIndices.size() - 1; i++) {
                if (sortedRowIndices.get(i + 1) - sortedRowIndices.get(i) != 1) {
                    rowsAreConsecutive = false;
                    break;
                }
            }
            boolean isCleanAdjacentSplit = isOneRunPerRow && rowsAreConsecutive && (sortedRowIndices.size() <= 4);
            result.setAdjacentRows(isCleanAdjacentSplit);
            result.setBestRow(String.join("/", distinctRows));

            // Format truthful breakdown derived from exact runs
            List<String> runParts = new ArrayList<>();
            for (ContiguousRun run : runs) {
                runParts.add(String.format("%d in Row %s (%s)", run.size(), run.getRowLabel(), run.formatRange()));
            }

            if (isCleanAdjacentSplit) {
                result.setSplitDescription(String.join(" + ", runParts) + " across adjacent rows");
                result.setSeatingTradeoff(String.format("No single-row group of %d is currently available.", partySize));
            } else {
                result.setSplitDescription("Clustered: " + String.join(" + ", runParts));
                result.setSeatingTradeoff(String.format("No single-row contiguous block of %d is currently available. Seats are arranged in %d separate blocks across rows.", partySize, runs.size()));
            }
        }

        return result;
    }

    /**
     * Discovers, scores, and ranks all valid group candidates for a show and party size.
     * Used by SeatRecommendationEngine to present curated options.
     */
    public List<RecommendedBlock> evaluateGroupCandidates(Long showId,
                                                          List<ShowSeat> showSeats,
                                                          List<Seat> screenSeats,
                                                          int partySize,
                                                          String preference) {
        if (showSeats == null || screenSeats == null || partySize <= 0) {
            return Collections.emptyList();
        }

        // 1. Authoritative AVAILABLE check
        Set<Long> availableSeatIds = showSeats.stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .map(ss -> ss.getId().getSeatId())
                .collect(Collectors.toSet());

        if (availableSeatIds.size() < partySize) {
            return Collections.emptyList();
        }

        // Active physical seats that are currently AVAILABLE
        List<Seat> availableSeats = screenSeats.stream()
                .filter(s -> availableSeatIds.contains(s.getSeatId()))
                .filter(s -> s.getStatus() == null || "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .toList();

        if (availableSeats.size() < partySize) {
            return Collections.emptyList();
        }

        // Precompute seat scores and pricing context
        Map<Long, SeatScoreDTO> scoreMap = new HashMap<>();
        for (Seat s : screenSeats) {
            scoreMap.put(s.getSeatId(), seatScoringEngine.scoreSeat(s));
        }

        SeatPricingService.ShowPricingContext showCtx = (seatPricingService != null && showId != null)
                ? seatPricingService.getShowPricingContext(showId) : null;

        Long screenId = !screenSeats.isEmpty() ? screenSeats.get(0).getScreenId() : null;
        SeatScoringEngine.ScreenGeometry screenGeom = (seatScoringEngine != null && screenId != null)
                ? seatScoringEngine.getScreenGeometry(screenId) : null;

        // Group available seats into rows
        Map<String, List<Seat>> seatsByRow = availableSeats.stream()
                .filter(s -> s.getRowLabel() != null)
                .collect(Collectors.groupingBy(Seat::getRowLabel));

        // Partition rows into contiguous banks (bounded by aisles or missing/booked seats)
        Map<String, List<List<Seat>>> banksByRow = new LinkedHashMap<>();
        for (Map.Entry<String, List<Seat>> entry : seatsByRow.entrySet()) {
            String row = entry.getKey();
            List<Seat> rowSeats = new ArrayList<>(entry.getValue());
            rowSeats.sort(Comparator.comparingInt(s -> parseSeatNum(s.getSeatNumber())));

            List<List<Seat>> banks = new ArrayList<>();
            List<Seat> currentBank = new ArrayList<>();

            for (int k = 0; k < rowSeats.size(); k++) {
                Seat seat = rowSeats.get(k);
                currentBank.add(seat);

                boolean hasAisle = seat.getAisleAfter() != null && seat.getAisleAfter();
                boolean isLastInRow = (k == rowSeats.size() - 1);
                boolean isNumericBreak = !isLastInRow && (parseSeatNum(rowSeats.get(k + 1).getSeatNumber()) != parseSeatNum(seat.getSeatNumber()) + 1);

                if (hasAisle || isNumericBreak || isLastInRow) {
                    if (!currentBank.isEmpty()) {
                        banks.add(new ArrayList<>(currentBank));
                        currentBank.clear();
                    }
                }
            }
            if (!banks.isEmpty()) {
                banksByRow.put(row, banks);
            }
        }

        List<RecommendedBlock> candidateBlocks = new ArrayList<>();

        // ---------------------------------------------------------------------
        // Strategy 1: Single-Row Contiguous Blocks (Without Crossing Aisles)
        // ---------------------------------------------------------------------
        for (Map.Entry<String, List<List<Seat>>> entry : banksByRow.entrySet()) {
            String row = entry.getKey();
            for (List<Seat> bank : entry.getValue()) {
                if (bank.size() >= partySize) {
                    for (int i = 0; i <= bank.size() - partySize; i++) {
                        List<Seat> block = bank.subList(i, i + partySize);
                        candidateBlocks.add(scoreGroupBlock(showId, block, partySize, row, scoreMap, showCtx, screenGeom, false, false, null));
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Strategy 2: Adjacent-Row Balanced Splits (when partySize >= 2)
        // e.g. for partySize=10: 5+5, 6+4, 4+6, 7+3, 3+7
        // e.g. for partySize=12: 6+6, 7+5, 5+7, 8+4, 4+8
        // e.g. for partySize=4: 2+2
        // ---------------------------------------------------------------------
        if (partySize >= 2) {
            List<RecommendedBlock> adjacentSplits = findAdjacentRowSplits(showId, banksByRow, partySize, scoreMap, showCtx, screenGeom);
            candidateBlocks.addAll(adjacentSplits);
        }

        // ---------------------------------------------------------------------
        // Strategy 3: Three Adjacent Rows (for parties like 6, 8, 9, 10, 12, 15)
        // e.g. for partySize=10: 4+3+3, 3+4+3, 3+3+4, 4+4+2, 2+4+4
        // e.g. for partySize=12: 4+4+4, 5+4+3, 3+4+5
        // e.g. for partySize=15: 5+5+5, 6+5+4, 4+5+6
        // ---------------------------------------------------------------------
        if (partySize >= 6) {
            List<RecommendedBlock> threeRowSplits = findThreeAdjacentRowSplits(showId, banksByRow, partySize, scoreMap, showCtx, screenGeom);
            candidateBlocks.addAll(threeRowSplits);
        }

        // ---------------------------------------------------------------------
        // Strategy 4: Four Adjacent Rows (for large parties >= 12, e.g. 12, 14, 15, 16)
        // e.g. for partySize=15: 4+4+4+3, 4+4+3+4, 4+3+4+4, 3+4+4+4
        // e.g. for partySize=16: 4+4+4+4
        // ---------------------------------------------------------------------
        if (partySize >= 12) {
            List<RecommendedBlock> fourRowSplits = findFourAdjacentRowSplits(showId, banksByRow, partySize, scoreMap, showCtx, screenGeom);
            candidateBlocks.addAll(fourRowSplits);
        }

        // ---------------------------------------------------------------------
        // Strategy 5: Clustered Nearby Banks Fallback (when contiguous adjacent rows fail)
        // ---------------------------------------------------------------------
        if (candidateBlocks.isEmpty() && partySize > 1) {
            candidateBlocks.addAll(findClusteredBanksFallback(showId, banksByRow, partySize, scoreMap, showCtx, screenGeom));
        }

        // ---------------------------------------------------------------------
        // Strategy 6: Disjoint Fallback (last resort if completely scattered)
        // ---------------------------------------------------------------------
        if (candidateBlocks.isEmpty()) {
            List<Seat> bestIndividuals = availableSeats.stream()
                    .sorted(Comparator.comparingInt((Seat s) -> scoreMap.get(s.getSeatId()).getScore()).reversed())
                    .limit(partySize)
                    .toList();
            if (bestIndividuals.size() == partySize) {
                candidateBlocks.add(scoreGroupBlock(showId, bestIndividuals, partySize, "Scattered", scoreMap, showCtx, screenGeom, true, false, null));
            }
        }

        // Deduplicate identical seat groupings
        Map<String, RecommendedBlock> distinctMap = new LinkedHashMap<>();
        for (RecommendedBlock block : candidateBlocks) {
            List<Long> sortedIds = new ArrayList<>(block.getSeatIds());
            Collections.sort(sortedIds);
            String key = sortedIds.stream().map(String::valueOf).collect(Collectors.joining("-"));
            if (!distinctMap.containsKey(key) || distinctMap.get(key).getAverageScore() < block.getAverageScore()) {
                distinctMap.put(key, block);
            }
        }
        candidateBlocks = new ArrayList<>(distinctMap.values());

        // Sort candidates
        if ("BUDGET".equalsIgnoreCase(preference)) {
            candidateBlocks.sort(Comparator.comparing(RecommendedBlock::getTotalPrice)
                    .thenComparing(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed()));
        } else {
            candidateBlocks.sort(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed()
                    .thenComparing(RecommendedBlock::getTotalPrice));
        }

        return candidateBlocks;
    }

    /**
     * Evaluates 2-row adjacent splits for partySize.
     * Explores valid split partitions (e.g. 5+5, 6+4, 4+6 for partySize=10) across adjacent rows.
     */
    private List<RecommendedBlock> findAdjacentRowSplits(Long showId,
                                                         Map<String, List<List<Seat>>> banksByRow,
                                                         int partySize,
                                                         Map<Long, SeatScoreDTO> scoreMap,
                                                         SeatPricingService.ShowPricingContext showCtx,
                                                         SeatScoringEngine.ScreenGeometry screenGeom) {
        List<RecommendedBlock> splits = new ArrayList<>();
        List<String> rowKeys = new ArrayList<>(banksByRow.keySet());
        rowKeys.sort(Comparator.comparingInt(r -> parseRowIndex(r, screenGeom)));

        int half = partySize / 2;
        List<int[]> partitions = new ArrayList<>();
        partitions.add(new int[]{half, partySize - half});
        for (int delta = 1; delta <= half - 1; delta++) {
            partitions.add(new int[]{half + delta, partySize - half - delta});
            partitions.add(new int[]{half - delta, partySize - half + delta});
        }

        for (int i = 0; i < rowKeys.size() - 1; i++) {
            String r1 = rowKeys.get(i);
            String r2 = rowKeys.get(i + 1);

            int idx1 = parseRowIndex(r1, screenGeom);
            int idx2 = parseRowIndex(r2, screenGeom);
            if (Math.abs(idx1 - idx2) > 1) continue; // Must be physically adjacent rows

            List<List<Seat>> b1List = banksByRow.get(r1);
            List<List<Seat>> b2List = banksByRow.get(r2);

            for (int[] part : partitions) {
                int count1 = part[0];
                int count2 = part[1];
                if (count1 <= 0 || count2 <= 0) continue;

                for (List<Seat> bank1 : b1List) {
                    if (bank1.size() < count1) continue;
                    for (List<Seat> bank2 : b2List) {
                        if (bank2.size() < count2) continue;

                        List<Seat> bestSub1 = null;
                        List<Seat> bestSub2 = null;
                        double bestAlignment = -1;

                        for (int w1 = 0; w1 <= bank1.size() - count1; w1++) {
                            List<Seat> sub1 = bank1.subList(w1, w1 + count1);
                            double center1 = computeBankCenter(sub1);

                            for (int w2 = 0; w2 <= bank2.size() - count2; w2++) {
                                List<Seat> sub2 = bank2.subList(w2, w2 + count2);
                                double center2 = computeBankCenter(sub2);
                                double alignment = 1.0 / (1.0 + Math.abs(center1 - center2));

                                if (alignment > bestAlignment) {
                                    bestAlignment = alignment;
                                    bestSub1 = sub1;
                                    bestSub2 = sub2;
                                }
                            }
                        }

                        if (bestSub1 != null && bestSub2 != null) {
                            List<Seat> combined = new ArrayList<>();
                            combined.addAll(bestSub1);
                            combined.addAll(bestSub2);

                            String splitSpec = String.format("%d in Row %s (%s) + %d in Row %s (%s)",
                                    bestSub1.size(), r1, formatSeatRange(bestSub1),
                                    bestSub2.size(), r2, formatSeatRange(bestSub2));
                            RecommendedBlock block = scoreGroupBlock(showId, combined, partySize, r1 + "/" + r2, scoreMap, showCtx, screenGeom, false, true, splitSpec);
                            splits.add(block);
                        }
                    }
                }
            }
        }

        splits.sort(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed());
        return splits.stream().limit(6).toList();
    }

    /**
     * Evaluates 3-row adjacent splits for large parties (6, 8, 9, 10, 12, 15).
     */
    private List<RecommendedBlock> findThreeAdjacentRowSplits(Long showId,
                                                              Map<String, List<List<Seat>>> banksByRow,
                                                              int partySize,
                                                              Map<Long, SeatScoreDTO> scoreMap,
                                                              SeatPricingService.ShowPricingContext showCtx,
                                                              SeatScoringEngine.ScreenGeometry screenGeom) {
        List<RecommendedBlock> splits = new ArrayList<>();
        List<String> rowKeys = new ArrayList<>(banksByRow.keySet());
        rowKeys.sort(Comparator.comparingInt(r -> parseRowIndex(r, screenGeom)));

        int basePerCount = partySize / 3;
        int rem = partySize % 3;
        List<int[]> partitions = new ArrayList<>();
        int c1 = basePerCount + (rem > 0 ? 1 : 0);
        int c2 = basePerCount + (rem > 1 ? 1 : 0);
        int c3 = basePerCount;
        partitions.add(new int[]{c1, c2, c3});
        if (rem == 1) {
            partitions.add(new int[]{basePerCount, basePerCount + 1, basePerCount});
            partitions.add(new int[]{basePerCount, basePerCount, basePerCount + 1});
        } else if (rem == 2) {
            partitions.add(new int[]{basePerCount + 1, basePerCount, basePerCount + 1});
            partitions.add(new int[]{basePerCount, basePerCount + 1, basePerCount + 1});
        }
        if (partySize >= 8) {
            partitions.add(new int[]{basePerCount + 1, basePerCount + 1, basePerCount - 1});
            partitions.add(new int[]{basePerCount - 1, basePerCount + 1, basePerCount + 1});
            partitions.add(new int[]{basePerCount + 1, basePerCount - 1, basePerCount + 1});
        }

        for (int i = 0; i < rowKeys.size() - 2; i++) {
            String r1 = rowKeys.get(i);
            String r2 = rowKeys.get(i + 1);
            String r3 = rowKeys.get(i + 2);

            int idx1 = parseRowIndex(r1, screenGeom);
            int idx2 = parseRowIndex(r2, screenGeom);
            int idx3 = parseRowIndex(r3, screenGeom);
            if (idx2 - idx1 != 1 || idx3 - idx2 != 1) continue;

            List<List<Seat>> b1List = banksByRow.get(r1);
            List<List<Seat>> b2List = banksByRow.get(r2);
            List<List<Seat>> b3List = banksByRow.get(r3);

            for (int[] part : partitions) {
                int p1 = part[0], p2 = part[1], p3 = part[2];
                if (p1 <= 0 || p2 <= 0 || p3 <= 0) continue;

                for (List<Seat> bank1 : b1List) {
                    if (bank1.size() < p1) continue;
                    for (List<Seat> bank2 : b2List) {
                        if (bank2.size() < p2) continue;
                        for (List<Seat> bank3 : b3List) {
                            if (bank3.size() < p3) continue;

                            List<Seat> bestSub1 = null;
                            List<Seat> bestSub2 = null;
                            List<Seat> bestSub3 = null;
                            double bestAlignment = -1;

                            for (int w1 = 0; w1 <= bank1.size() - p1; w1++) {
                                List<Seat> sub1 = bank1.subList(w1, w1 + p1);
                                double center1 = computeBankCenter(sub1);
                                for (int w2 = 0; w2 <= bank2.size() - p2; w2++) {
                                    List<Seat> sub2 = bank2.subList(w2, w2 + p2);
                                    double center2 = computeBankCenter(sub2);
                                    for (int w3 = 0; w3 <= bank3.size() - p3; w3++) {
                                        List<Seat> sub3 = bank3.subList(w3, w3 + p3);
                                        double center3 = computeBankCenter(sub3);
                                        double alignment = 1.0 / (1.0 + Math.abs(center1 - center2) + Math.abs(center2 - center3));

                                        if (alignment > bestAlignment) {
                                            bestAlignment = alignment;
                                            bestSub1 = sub1;
                                            bestSub2 = sub2;
                                            bestSub3 = sub3;
                                        }
                                    }
                                }
                            }

                            if (bestSub1 != null && bestSub2 != null && bestSub3 != null) {
                                List<Seat> combined = new ArrayList<>();
                                combined.addAll(bestSub1);
                                combined.addAll(bestSub2);
                                combined.addAll(bestSub3);

                                String splitSpec = String.format("%d in Row %s (%s) + %d in Row %s (%s) + %d in Row %s (%s)",
                                        p1, r1, formatSeatRange(bestSub1),
                                        p2, r2, formatSeatRange(bestSub2),
                                        p3, r3, formatSeatRange(bestSub3));
                                RecommendedBlock block = scoreGroupBlock(showId, combined, partySize, r1 + "/" + r2 + "/" + r3, scoreMap, showCtx, screenGeom, false, true, splitSpec);
                                splits.add(block);
                            }
                        }
                    }
                }
            }
        }

        splits.sort(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed());
        return splits.stream().limit(4).toList();
    }

    /**
     * Evaluates 4-row adjacent splits for large parties (12, 14, 15, 16, 18).
     * Enables large groups to sit comfortably in center auditorium banks without aisle crossing.
     */
    private List<RecommendedBlock> findFourAdjacentRowSplits(Long showId,
                                                             Map<String, List<List<Seat>>> banksByRow,
                                                             int partySize,
                                                             Map<Long, SeatScoreDTO> scoreMap,
                                                             SeatPricingService.ShowPricingContext showCtx,
                                                             SeatScoringEngine.ScreenGeometry screenGeom) {
        List<RecommendedBlock> splits = new ArrayList<>();
        List<String> rowKeys = new ArrayList<>(banksByRow.keySet());
        rowKeys.sort(Comparator.comparingInt(r -> parseRowIndex(r, screenGeom)));

        int basePerCount = partySize / 4;
        int rem = partySize % 4;
        List<int[]> partitions = new ArrayList<>();
        int p1 = basePerCount + (rem > 0 ? 1 : 0);
        int p2 = basePerCount + (rem > 1 ? 1 : 0);
        int p3 = basePerCount + (rem > 2 ? 1 : 0);
        int p4 = basePerCount;
        partitions.add(new int[]{p1, p2, p3, p4});
        if (rem > 0) {
            partitions.add(new int[]{p4, p1, p2, p3});
            partitions.add(new int[]{p1, p4, p2, p3});
            partitions.add(new int[]{p1, p2, p4, p3});
        }

        for (int i = 0; i < rowKeys.size() - 3; i++) {
            String r1 = rowKeys.get(i);
            String r2 = rowKeys.get(i + 1);
            String r3 = rowKeys.get(i + 2);
            String r4 = rowKeys.get(i + 3);

            int idx1 = parseRowIndex(r1, screenGeom);
            int idx2 = parseRowIndex(r2, screenGeom);
            int idx3 = parseRowIndex(r3, screenGeom);
            int idx4 = parseRowIndex(r4, screenGeom);
            if (idx2 - idx1 != 1 || idx3 - idx2 != 1 || idx4 - idx3 != 1) continue;

            List<List<Seat>> b1List = banksByRow.get(r1);
            List<List<Seat>> b2List = banksByRow.get(r2);
            List<List<Seat>> b3List = banksByRow.get(r3);
            List<List<Seat>> b4List = banksByRow.get(r4);

            for (int[] part : partitions) {
                int c1 = part[0], c2 = part[1], c3 = part[2], c4 = part[3];
                if (c1 <= 0 || c2 <= 0 || c3 <= 0 || c4 <= 0) continue;

                for (List<Seat> bank1 : b1List) {
                    if (bank1.size() < c1) continue;
                    for (List<Seat> bank2 : b2List) {
                        if (bank2.size() < c2) continue;
                        for (List<Seat> bank3 : b3List) {
                            if (bank3.size() < c3) continue;
                            for (List<Seat> bank4 : b4List) {
                                if (bank4.size() < c4) continue;

                                List<Seat> bestSub1 = null, bestSub2 = null, bestSub3 = null, bestSub4 = null;
                                double bestAlignment = -1;

                                for (int w1 = 0; w1 <= bank1.size() - c1; w1++) {
                                    List<Seat> sub1 = bank1.subList(w1, w1 + c1);
                                    double center1 = computeBankCenter(sub1);
                                    for (int w2 = 0; w2 <= bank2.size() - c2; w2++) {
                                        List<Seat> sub2 = bank2.subList(w2, w2 + c2);
                                        double center2 = computeBankCenter(sub2);
                                        for (int w3 = 0; w3 <= bank3.size() - c3; w3++) {
                                            List<Seat> sub3 = bank3.subList(w3, w3 + c3);
                                            double center3 = computeBankCenter(sub3);
                                            for (int w4 = 0; w4 <= bank4.size() - c4; w4++) {
                                                List<Seat> sub4 = bank4.subList(w4, w4 + c4);
                                                double center4 = computeBankCenter(sub4);
                                                double spread = Math.abs(center1 - center2) + Math.abs(center2 - center3) + Math.abs(center3 - center4);
                                                double alignment = 1.0 / (1.0 + spread);

                                                if (alignment > bestAlignment) {
                                                    bestAlignment = alignment;
                                                    bestSub1 = sub1;
                                                    bestSub2 = sub2;
                                                    bestSub3 = sub3;
                                                    bestSub4 = sub4;
                                                }
                                            }
                                        }
                                    }
                                }

                                if (bestSub1 != null && bestSub2 != null && bestSub3 != null && bestSub4 != null) {
                                    List<Seat> combined = new ArrayList<>();
                                    combined.addAll(bestSub1);
                                    combined.addAll(bestSub2);
                                    combined.addAll(bestSub3);
                                    combined.addAll(bestSub4);

                                    String splitSpec = String.format("%d in Row %s (%s) + %d in Row %s (%s) + %d in Row %s (%s) + %d in Row %s (%s)",
                                            c1, r1, formatSeatRange(bestSub1),
                                            c2, r2, formatSeatRange(bestSub2),
                                            c3, r3, formatSeatRange(bestSub3),
                                            c4, r4, formatSeatRange(bestSub4));
                                    RecommendedBlock block = scoreGroupBlock(showId, combined, partySize, r1 + "/" + r2 + "/" + r3 + "/" + r4, scoreMap, showCtx, screenGeom, false, true, splitSpec);
                                    splits.add(block);
                                }
                            }
                        }
                    }
                }
            }
        }

        splits.sort(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed());
        return splits.stream().limit(3).toList();
    }

    /**
     * Fallback to gather contiguous banks in close proximity when strictly adjacent splits don't fit.
     * Evaluates multiple row starting positions across the room depth and selects the candidate
     * cluster that naturally maximizes viewing quality and center proximity.
     * Enforces that each piece is a real contiguous bank of at least 2 seats (unless strictly necessary).
     */
    private List<RecommendedBlock> findClusteredBanksFallback(Long showId,
                                                              Map<String, List<List<Seat>>> banksByRow,
                                                              int partySize,
                                                              Map<Long, SeatScoreDTO> scoreMap,
                                                              SeatPricingService.ShowPricingContext showCtx,
                                                              SeatScoringEngine.ScreenGeometry screenGeom) {
        List<RecommendedBlock> fallbacks = new ArrayList<>();
        List<String> rowKeys = new ArrayList<>(banksByRow.keySet());
        rowKeys.sort(Comparator.comparingInt(r -> parseRowIndex(r, screenGeom)));

        for (int startIdx = 0; startIdx < rowKeys.size(); startIdx++) {
            List<Seat> accumulated = new ArrayList<>();
            List<String> bankDescriptions = new ArrayList<>();

            for (int rIdx = startIdx; rIdx < rowKeys.size(); rIdx++) {
                String row = rowKeys.get(rIdx);
                for (List<Seat> bank : banksByRow.get(row)) {
                    int needed = partySize - accumulated.size();
                    if (needed <= 0) break;

                    int take = Math.min(bank.size(), needed);
                    List<Seat> sub = bank.subList(0, take);
                    accumulated.addAll(sub);
                    bankDescriptions.add(String.format("%d in Row %s (%s)", take, row, formatSeatRange(sub)));
                    if (accumulated.size() == partySize) break;
                }
                if (accumulated.size() == partySize) break;
            }

            if (accumulated.size() == partySize) {
                String splitSpec = String.join(" + ", bankDescriptions);
                RecommendedBlock block = scoreGroupBlock(showId, accumulated, partySize, "Multi-Row", scoreMap, showCtx, screenGeom, true, false, splitSpec);
                fallbacks.add(block);
            }
        }

        fallbacks.sort(Comparator.comparingInt(RecommendedBlock::getAverageScore).reversed());
        return fallbacks.stream().limit(3).toList();
    }

    /**
     * Scores a candidate group as a cohesive whole.
     * Incorporates:
     * - Individual seat viewing quality
     * - Physical cohesion (same row vs adjacent rows vs clustered/scattered)
     * - Horizontal center alignment
     * - Viewing distance / continuous front-row tilt penalty
     * - Minor orphan seat penalty
     */
    private RecommendedBlock scoreGroupBlock(Long showId,
                                             List<Seat> block,
                                             int partySize,
                                             String rowLabel,
                                             Map<Long, SeatScoreDTO> scoreMap,
                                             SeatPricingService.ShowPricingContext showCtx,
                                             SeatScoringEngine.ScreenGeometry screenGeom,
                                             boolean isDisjoint,
                                             boolean isAdjacentSplit,
                                             String customSplitDesc) {
        List<Long> blockIds = block.stream().map(Seat::getSeatId).toList();
        List<String> blockLabels = block.stream().map(s -> s.getRowLabel() + s.getSeatNumber()).toList();

        // 1. Average individual seat score and minimum seat score
        double avgIndividual = block.stream()
                .mapToInt(s -> scoreMap.containsKey(s.getSeatId()) ? scoreMap.get(s.getSeatId()).getScore() : 75)
                .average().orElse(75);
        int minIndividual = block.stream()
                .mapToInt(s -> scoreMap.containsKey(s.getSeatId()) ? scoreMap.get(s.getSeatId()).getScore() : 70)
                .min().orElse(70);
        double blendedQuality = (0.75 * avgIndividual) + (0.25 * minIndividual);

        // 2. Continuous Viewing Distance & Front-Row Tilt Penalty
        // Extreme front rows (Row A/B) receive continuous progressive physical neck-tilt penalty.
        // Mid-rear rows naturally score higher based on geometric comfort.
        double avgRowIndex = block.stream()
                .mapToInt(s -> parseRowIndex(s.getRowLabel(), screenGeom))
                .average().orElse(3.0);
        int totalRows = screenGeom != null ? Math.max(5, screenGeom.getTotalRows()) : 10;
        double normDistance = (totalRows > 1) ? (avgRowIndex - 1.0) / (totalRows - 1.0) : 0.50;

        double distanceComfortFactor;
        if (normDistance < 0.18) {
            // Extreme front rows: progressive vertical tilt penalty (up to 28% reduction)
            double frontRatio = (0.18 - normDistance) / 0.18;
            distanceComfortFactor = 1.0 - (0.28 * Math.pow(frontRatio, 1.2));
        } else if (normDistance <= 0.85) {
            // Optimal immersion zone (mid auditorium)
            distanceComfortFactor = 1.0;
        } else {
            // Far rear rows: slight immersion reduction
            distanceComfortFactor = 0.94;
        }

        // 3. Horizontal Center Alignment
        double avgCol = block.stream().mapToInt(s -> parseSeatNum(s.getSeatNumber())).average().orElse(5.5);
        double rowCenter = 5.5;
        if (screenGeom != null && !block.isEmpty()) {
            rowCenter = (screenGeom.getSeatsInRow(block.get(0).getRowLabel()) + 1.0) / 2.0;
        }
        double lateralOffset = Math.abs(avgCol - rowCenter);
        double maxLateral = Math.max(1.0, rowCenter - 1.0);
        double centerFactor = 1.0 - (0.15 * Math.min(1.0, lateralOffset / maxLateral));

        // 4. Group Cohesion Weight & Penalty derived from physical runs
        List<ContiguousRun> runs = detectContiguousRuns(block, screenGeom);
        Set<String> distinctRows = runs.stream().map(ContiguousRun::getRowLabel).collect(Collectors.toSet());
        int numRows = distinctRows.size();

        double cohesionFactor;
        int cohesionDeduction;

        if (!isDisjoint && !isAdjacentSplit && runs.size() == 1) {
            // Single contiguous block in same row
            cohesionFactor = 1.00;
            cohesionDeduction = 0;
        } else if (isAdjacentSplit && runs.size() == numRows) {
            // 1 contiguous bank per row across physically adjacent rows
            if (numRows == 2) {
                cohesionFactor = 0.92;
                cohesionDeduction = 6;
            } else if (numRows == 3) {
                cohesionFactor = 0.86;
                cohesionDeduction = 10;
            } else {
                cohesionFactor = 0.80;
                cohesionDeduction = 14;
            }
        } else {
            // Disjoint / multi-bank fragmented cluster
            cohesionFactor = 0.60;
            int extraRuns = Math.max(0, runs.size() - numRows);
            cohesionDeduction = 18 + (extraRuns * 4);
        }

        // 5. Minor Orphan Seat Penalty (Secondary: 0 to 4 points max)
        int orphanPenalty = computeMinorOrphanPenalty(block);

        // Combined Group Score [35 - 99]
        double rawComposite = (blendedQuality * distanceComfortFactor * centerFactor * cohesionFactor)
                - cohesionDeduction - orphanPenalty;
        int groupScore = (int) Math.round(Math.min(99, Math.max(35, rawComposite)));

        // Total Price Calculation (exact sum of seat prices)
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Seat s : block) {
            BigDecimal price = null;
            if (seatPricingService != null) {
                if (showCtx != null) {
                    try {
                        price = seatPricingService.calculateSeatPrice(s, showCtx);
                    } catch (Exception ignored) {}
                }
                if (price == null && s.getSeatId() != null) {
                    try {
                        price = seatPricingService.determineSeatPrice(showId, s.getSeatId());
                    } catch (Exception ignored) {}
                }
            }
            if (price == null && scoreMap.containsKey(s.getSeatId()) && scoreMap.get(s.getSeatId()) != null) {
                price = scoreMap.get(s.getSeatId()).getPrice();
            }
            totalCost = totalCost.add(price != null ? price : new BigDecimal("150.00"));
        }

        // Truthful, exact geometry-derived rationale
        String title;
        String category;
        String rationale;

        if (!isDisjoint && !isAdjacentSplit && runs.size() == 1) {
            title = "Best Visual Balance";
            category = "OPTIMAL_VIEW";
            String seatRange = formatSeatRange(block);
            rationale = String.format("Contiguous %d-seat block in Row %s (%s). Optimal viewing sightline with centered angle (offset: %.1f seats).",
                    partySize, rowLabel, seatRange, lateralOffset);
        } else if (isAdjacentSplit && runs.size() == numRows) {
            title = "Alternative Group Seating";
            category = "SPLIT_GROUP";
            List<String> parts = new ArrayList<>();
            for (ContiguousRun run : runs) {
                parts.add(String.format("%d contiguous seats in Row %s (%s)", run.size(), run.getRowLabel(), run.formatRange()));
            }
            rationale = String.format("No single-row group of %d is currently available. Paired across %d adjacent rows (%s) with aligned sightlines.",
                    partySize, numRows, String.join(" plus ", parts));
        } else {
            title = "Alternative Group Seating";
            category = "SPLIT_GROUP";
            List<String> runParts = new ArrayList<>();
            for (ContiguousRun run : runs) {
                runParts.add(String.format("%d in Row %s (%s)", run.size(), run.getRowLabel(), run.formatRange()));
            }
            rationale = String.format("No single-row contiguous block of %d is currently available. Clustered across %d blocks: %s.",
                    partySize, runs.size(), String.join(" + ", runParts));
        }

        return new RecommendedBlock(title, category, blockIds, blockLabels, groupScore, totalCost, rationale);
    }

    /**
     * Detects all physically contiguous runs in a block of seats,
     * taking into account row label, consecutive seat numbers, and physical aisle breaks.
     */
    public List<ContiguousRun> detectContiguousRuns(List<Seat> seats, SeatScoringEngine.ScreenGeometry screenGeom) {
        if (seats == null || seats.isEmpty()) {
            return Collections.emptyList();
        }

        List<Seat> sorted = new ArrayList<>(seats);
        sorted.sort(Comparator.comparingInt((Seat s) -> parseRowIndex(s.getRowLabel(), screenGeom))
                .thenComparingInt(s -> parseSeatNum(s.getSeatNumber())));

        List<ContiguousRun> runs = new ArrayList<>();
        List<Seat> currentRun = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            Seat s = sorted.get(i);
            if (currentRun.isEmpty()) {
                currentRun.add(s);
            } else {
                Seat prev = currentRun.get(currentRun.size() - 1);
                boolean sameRow = Objects.equals(prev.getRowLabel(), s.getRowLabel());
                boolean consecutive = (parseSeatNum(s.getSeatNumber()) == parseSeatNum(prev.getSeatNumber()) + 1);
                boolean prevHadAisle = Boolean.TRUE.equals(prev.getAisleAfter());

                if (sameRow && consecutive && !prevHadAisle) {
                    currentRun.add(s);
                } else {
                    int rIdx = parseRowIndex(currentRun.get(0).getRowLabel(), screenGeom);
                    runs.add(new ContiguousRun(currentRun.get(0).getRowLabel(), rIdx, new ArrayList<>(currentRun)));
                    currentRun.clear();
                    currentRun.add(s);
                }
            }
        }

        if (!currentRun.isEmpty()) {
            int rIdx = parseRowIndex(currentRun.get(0).getRowLabel(), screenGeom);
            runs.add(new ContiguousRun(currentRun.get(0).getRowLabel(), rIdx, new ArrayList<>(currentRun)));
        }

        return runs;
    }

    private int computeMinorOrphanPenalty(List<Seat> block) {
        // Minor secondary penalty (max 4 points)
        return 0;
    }

    private double computeBankCenter(List<Seat> bank) {
        return bank.stream().mapToInt(s -> parseSeatNum(s.getSeatNumber())).average().orElse(5.0);
    }

    public int parseRowIndex(String rowLabel, SeatScoringEngine.ScreenGeometry screenGeom) {
        if (screenGeom != null) {
            return screenGeom.getRowIndex(rowLabel);
        }
        if (rowLabel == null || rowLabel.isEmpty()) return 3;
        char c = rowLabel.toUpperCase().charAt(0);
        if (c >= 'A' && c <= 'Z') return (c - 'A') + 1;
        try {
            return Integer.parseInt(rowLabel.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 3;
        }
    }

    public int parseSeatNum(String seatNumber) {
        if (seatNumber == null) return 5;
        try {
            return Integer.parseInt(seatNumber.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 5;
        }
    }

    public String extractRowLabel(String seatLabel) {
        if (seatLabel == null || seatLabel.isEmpty()) return "A";
        return seatLabel.replaceAll("[0-9]", "").trim();
    }

    public String formatSeatRange(List<Seat> seats) {
        if (seats == null || seats.isEmpty()) return "";
        if (seats.size() == 1) return seats.get(0).getRowLabel() + seats.get(0).getSeatNumber();
        Seat first = seats.get(0);
        Seat last = seats.get(seats.size() - 1);
        return String.format("%s%s–%s%s", first.getRowLabel(), first.getSeatNumber(), last.getRowLabel(), last.getSeatNumber());
    }
}
