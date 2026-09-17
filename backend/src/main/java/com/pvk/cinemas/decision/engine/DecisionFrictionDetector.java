package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.decision.dto.FrictionAlert;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DecisionFrictionDetector {

    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatScoringEngine seatScoringEngine;

    public DecisionFrictionDetector(SeatRepository seatRepository,
                                  ShowSeatRepository showSeatRepository,
                                  SeatScoringEngine seatScoringEngine) {
        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatScoringEngine = seatScoringEngine;
    }

    public List<FrictionAlert> detectFriction(Long showId, List<Long> selectedSeatIds) {
        List<FrictionAlert> alerts = new ArrayList<>();
        if (selectedSeatIds == null || selectedSeatIds.isEmpty()) {
            return alerts;
        }

        List<Seat> selectedSeats = seatRepository.findAllById(selectedSeatIds);
        if (selectedSeats.isEmpty()) {
            return alerts;
        }

        // 1. Check for SPLIT GROUP
        if (selectedSeats.size() > 1) {
            Set<String> rows = selectedSeats.stream().map(Seat::getRowLabel).collect(Collectors.toSet());
            if (rows.size() > 1) {
                alerts.add(new FrictionAlert(
                        "SPLIT_GROUP",
                        "WARNING",
                        "Seats in Different Rows",
                        "Your selected seats are split across rows " + String.join(", ", rows) + ". Would you prefer seating together in the same row?"
                ));
            } else {
                // Same row, check if contiguous
                List<Integer> seatNums = selectedSeats.stream()
                        .map(s -> Integer.parseInt(s.getSeatNumber()))
                        .sorted()
                        .toList();
                for (int i = 0; i < seatNums.size() - 1; i++) {
                    if (seatNums.get(i + 1) - seatNums.get(i) > 1) {
                        alerts.add(new FrictionAlert(
                                "SPLIT_GROUP",
                                "WARNING",
                                "Non-Contiguous Seats",
                                "Selected seats in Row " + selectedSeats.get(0).getRowLabel() + " have gaps between them. We recommend selecting adjacent seats for a better group experience."
                        ));
                        break;
                    }
                }
            }
        }

        // 2. Check for SUBOPTIMAL VIEW (e.g. Row A or extreme edges)
        for (Seat s : selectedSeats) {
            SeatScoreDTO score = seatScoringEngine.scoreSeat(s);
            if (score.getScore() < 65) {
                alerts.add(new FrictionAlert(
                        "SUBOPTIMAL_VIEW",
                        "INFO",
                        "Steep Viewing Angle (" + s.getRowLabel() + s.getSeatNumber() + ")",
                        "Seat " + s.getRowLabel() + s.getSeatNumber() + " is located on the outer edge or extreme front, which may cause neck strain during high-action sequences."
                ));
                break;
            }
        }

        // 3. Check for ORPHAN SINGLE SEAT (Aisle-Aware and Causality-Checked)
        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(showId);
        Map<Long, String> availabilityMap = showSeats.stream()
                .collect(Collectors.toMap(ss -> ss.getId().getSeatId(), ShowSeat::getAvailabilityStatus));

        Map<String, List<Seat>> selectedByRow = selectedSeats.stream().collect(Collectors.groupingBy(Seat::getRowLabel));
        for (Map.Entry<String, List<Seat>> entry : selectedByRow.entrySet()) {
            String rowLabel = entry.getKey();
            Long screenId = selectedSeats.get(0).getScreenId();
            List<Seat> allRowSeats = seatRepository.findByScreenIdAndStatusOrderByRowLabelAscSeatNumberAsc(screenId, "ACTIVE").stream()
                    .filter(s -> rowLabel.equalsIgnoreCase(s.getRowLabel()))
                    .sorted(Comparator.comparingInt(s -> {
                        try {
                            return Integer.parseInt(s.getSeatNumber().replaceAll("[^0-9]", ""));
                        } catch (Exception e) {
                            return 0;
                        }
                    }))
                    .toList();

            if (allRowSeats.isEmpty()) continue;

            // Partition the row into contiguous physical banks separated by walkways (aisles) or numeric gaps
            List<List<Seat>> physicalBanks = new ArrayList<>();
            List<Seat> currentBank = new ArrayList<>();

            for (int k = 0; k < allRowSeats.size(); k++) {
                Seat seat = allRowSeats.get(k);
                currentBank.add(seat);

                boolean hasAisle = Boolean.TRUE.equals(seat.getAisleAfter());
                boolean isLastInRow = (k == allRowSeats.size() - 1);
                boolean isNumericGap = false;
                if (!isLastInRow) {
                    try {
                        int curNum = Integer.parseInt(seat.getSeatNumber().replaceAll("[^0-9]", ""));
                        int nextNum = Integer.parseInt(allRowSeats.get(k + 1).getSeatNumber().replaceAll("[^0-9]", ""));
                        if (nextNum != curNum + 1) {
                            isNumericGap = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (hasAisle || isNumericGap || isLastInRow) {
                    if (!currentBank.isEmpty()) {
                        physicalBanks.add(new ArrayList<>(currentBank));
                        currentBank.clear();
                    }
                }
            }

            // Identify base unavailables vs simulated unavailables
            Set<Long> baseUnavailable = new HashSet<>();
            for (Seat s : allRowSeats) {
                String status = availabilityMap.getOrDefault(s.getSeatId(), "AVAILABLE");
                if (!"AVAILABLE".equalsIgnoreCase(status) || (s.getStatus() != null && !"ACTIVE".equalsIgnoreCase(s.getStatus()))) {
                    baseUnavailable.add(s.getSeatId());
                }
            }

            Set<Long> simulatedUnavailable = new HashSet<>(baseUnavailable);
            simulatedUnavailable.addAll(selectedSeatIds);

            // Within each physical bank, evaluate available runs before and after selection
            for (List<Seat> bank : physicalBanks) {
                boolean userSelectedInThisBank = bank.stream().anyMatch(s -> selectedSeatIds.contains(s.getSeatId()));
                if (!userSelectedInThisBank) {
                    continue; // Selection in another bank or row cannot orphan seats here
                }

                // Evaluate pre-selection available runs to ensure causality
                Set<Long> preSingleOrphans = new HashSet<>();
                List<Seat> preRun = new ArrayList<>();
                for (Seat s : bank) {
                    if (!baseUnavailable.contains(s.getSeatId())) {
                        preRun.add(s);
                    } else {
                        if (preRun.size() == 1) {
                            preSingleOrphans.add(preRun.get(0).getSeatId());
                        }
                        preRun.clear();
                    }
                }
                if (preRun.size() == 1) {
                    preSingleOrphans.add(preRun.get(0).getSeatId());
                }

                // Available runs after selection
                List<List<Seat>> postRuns = new ArrayList<>();
                List<Seat> currentRun = new ArrayList<>();
                for (Seat s : bank) {
                    if (!simulatedUnavailable.contains(s.getSeatId())) {
                        currentRun.add(s);
                    } else {
                        if (!currentRun.isEmpty()) {
                            postRuns.add(new ArrayList<>(currentRun));
                            currentRun.clear();
                        }
                    }
                }
                if (!currentRun.isEmpty()) {
                    postRuns.add(new ArrayList<>(currentRun));
                }

                // Check if any post-selection run is of length 1 that was NOT already an orphan
                for (List<Seat> run : postRuns) {
                    if (run.size() == 1) {
                        Seat loneSeat = run.get(0);
                        if (!preSingleOrphans.contains(loneSeat.getSeatId())) {
                            alerts.add(new FrictionAlert(
                                    "ORPHAN_SEAT",
                                    "INFO",
                                    "Single Orphan Seat Left (" + loneSeat.getRowLabel() + loneSeat.getSeatNumber() + ")",
                                    "This selection leaves an isolated single seat " + loneSeat.getRowLabel() + loneSeat.getSeatNumber() + ". Consider shifting by one seat to maintain paired seating."
                            ));
                            break;
                        }
                    }
                }
            }
        }

        return alerts;
    }
}
