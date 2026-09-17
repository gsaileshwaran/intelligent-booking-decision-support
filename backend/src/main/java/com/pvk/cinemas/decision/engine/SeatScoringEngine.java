package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SeatScoringEngine {

    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatHoldService seatHoldService;

    private final Map<Long, SeatType> seatTypeCache = new ConcurrentHashMap<>();
    private final Map<Long, ScreenGeometry> screenGeometryCache = new ConcurrentHashMap<>();

    public SeatScoringEngine(SeatRepository seatRepository,
                             SeatTypeRepository seatTypeRepository,
                             SeatHoldService seatHoldService) {
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.seatHoldService = seatHoldService;
    }

    public static class ScreenGeometry {
        private final int totalRows;
        private final Map<String, Integer> rowSeatCounts;
        private final Map<String, Integer> rowIndices;

        public ScreenGeometry(int totalRows, Map<String, Integer> rowSeatCounts, Map<String, Integer> rowIndices) {
            this.totalRows = totalRows;
            this.rowSeatCounts = rowSeatCounts;
            this.rowIndices = rowIndices;
        }

        public int getTotalRows() { return totalRows; }
        public int getSeatsInRow(String rowLabel) {
            return rowSeatCounts.getOrDefault(rowLabel, 10);
        }
        public int getRowIndex(String rowLabel) {
            if (rowIndices != null && rowIndices.containsKey(rowLabel)) {
                return rowIndices.get(rowLabel);
            }
            if (rowLabel == null || rowLabel.trim().isEmpty()) return 3;
            String clean = rowLabel.toUpperCase().trim();
            char c = clean.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return (c - 'A') + 1;
            }
            try {
                return Integer.parseInt(clean.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                return 3;
            }
        }
    }

    public ScreenGeometry getScreenGeometry(Long screenId) {
        if (screenId == null) {
            return new ScreenGeometry(5, Collections.emptyMap(), Collections.emptyMap());
        }
        return screenGeometryCache.computeIfAbsent(screenId, id -> {
            List<Seat> screenSeats = seatRepository.findByScreenId(id);
            if (screenSeats == null || screenSeats.isEmpty()) {
                return new ScreenGeometry(5, Collections.emptyMap(), Collections.emptyMap());
            }

            // Extract distinct rows sorted naturally
            List<String> sortedRows = screenSeats.stream()
                    .map(Seat::getRowLabel)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.comparingInt(this::parseRowIndexFromLabel))
                    .toList();

            Map<String, Integer> rowIndices = new HashMap<>();
            for (int i = 0; i < sortedRows.size(); i++) {
                rowIndices.put(sortedRows.get(i), i + 1);
            }

            Map<String, Integer> rowSeatCounts = new HashMap<>();
            for (Seat s : screenSeats) {
                String r = s.getRowLabel();
                if (r != null) {
                    int seatNum = parseSeatNumber(s.getSeatNumber());
                    rowSeatCounts.put(r, Math.max(rowSeatCounts.getOrDefault(r, 0), seatNum));
                }
            }

            int totalRows = Math.max(1, sortedRows.size());
            return new ScreenGeometry(totalRows, rowSeatCounts, rowIndices);
        });
    }

    /**
     * Mathematical computation of multi-factor seat quality score derived from
     * actual physical screen geometry and seat position.
     */
    public SeatScoreDTO scoreSeat(Seat seat) {
        return scoreSeat(seat, null, null);
    }

    /**
     * Overloaded method supporting custom / synthetic layouts for testing and simulation.
     */
    public SeatScoreDTO scoreSeat(Seat seat, Integer explicitTotalRows, Integer explicitSeatsInRow) {
        SeatScoreDTO dto = new SeatScoreDTO();
        dto.setSeatId(seat.getSeatId());
        String rowLabel = seat.getRowLabel() != null ? seat.getRowLabel().toUpperCase().trim() : "C";
        dto.setRowLabel(rowLabel);
        dto.setSeatNumber(seat.getSeatNumber());

        int seatNum = parseSeatNumber(seat.getSeatNumber());

        // Resolve hardware seat type
        String typeCode = "STANDARD";
        String typeName = "Standard";
        if (seat.getSeatTypeId() != null) {
            SeatType st = seatTypeCache.computeIfAbsent(seat.getSeatTypeId(), id -> seatTypeRepository.findById(id).orElse(null));
            if (st != null) {
                typeCode = st.getTypeCode() != null ? st.getTypeCode().toUpperCase() : "STANDARD";
                typeName = st.getName() != null ? st.getName() : "Standard";
            }
        }
        dto.setSeatType(typeName);

        // Resolve spatial pricing zone (VALUE, STANDARD, PREMIUM)
        String zone = seat.getPricingZone() != null ? seat.getPricingZone().toUpperCase() : "STANDARD";
        dto.setPricingZone(zone);

        // Resolve geometry context
        int totalRows;
        int seatsInRow;
        int rowIndex;

        if (explicitTotalRows != null && explicitSeatsInRow != null) {
            totalRows = explicitTotalRows;
            seatsInRow = explicitSeatsInRow;
            rowIndex = parseRowIndexFromLabel(rowLabel);
        } else if (seat.getScreenId() != null) {
            ScreenGeometry geom = getScreenGeometry(seat.getScreenId());
            rowIndex = seat.getGridRowIndex() != null ? seat.getGridRowIndex() : geom.getRowIndex(rowLabel);
            totalRows = Math.max(geom.getTotalRows(), rowIndex);
            seatsInRow = geom.getSeatsInRow(rowLabel);
        } else {
            // Default heuristics if screenId not populated
            rowIndex = seat.getGridRowIndex() != null ? seat.getGridRowIndex() : parseRowIndexFromLabel(rowLabel);
            totalRows = Math.max(5, rowIndex);
            seatsInRow = Math.max(10, seatNum);
        }

        // Clamp row index to valid bounds
        rowIndex = Math.max(1, Math.min(totalRows, rowIndex));
        seatsInRow = Math.max(1, seatsInRow);

        // ---------------------------------------------------------------------
        // 1. Authoritative Physical Coordinate & Normalized Viewing Distance
        // Depth increases as row moves away from screen: Row A = 1, Row L = 12
        // Normalized distance: 0.0 = closest (screen edge), 1.0 = farthest (rear)
        // ---------------------------------------------------------------------
        double normDistance = (totalRows > 1) ? (double) (rowIndex - 1) / (totalRows - 1) : 0.50;
        normDistance = Math.max(0.0, Math.min(1.0, normDistance));

        // Normalized preferred viewing-depth heuristic centered around ~62% of room depth
        double idealDist = 0.62;
        double distanceFactor;
        if (normDistance <= idealDist) {
            // Front rows: steep vertical neck tilt penalty (drops to ~0.50 at front row)
            double frontRatio = (idealDist - normDistance) / idealDist;
            distanceFactor = 1.0 - (0.48 * Math.pow(frontRatio, 1.4));
        } else {
            // Rear rows: gradual immersion drop-off (drops to ~0.76 at extreme rear)
            double rearRatio = (normDistance - idealDist) / (1.0 - idealDist);
            distanceFactor = 1.0 - (0.24 * Math.pow(rearRatio, 1.3));
        }

        // ---------------------------------------------------------------------
        // 2. Horizontal Center Alignment & Physical Viewing Angle (cos theta)
        // ---------------------------------------------------------------------
        double rowCenter = (seatsInRow + 1.0) / 2.0;
        double lateralOffset = Math.abs(seatNum - rowCenter);
        double maxLateralOffset = Math.max(1.0, rowCenter - 1.0);
        double offsetRatio = Math.min(1.0, lateralOffset / maxLateralOffset);

        // Center alignment factor
        double centerFactor = 1.0 - (0.35 * Math.pow(offsetRatio, 1.3));

        // Geometric off-axis viewing angle: cos(theta) = y / sqrt((k * dx)^2 + y^2)
        // In front rows (y small), lateral offset creates steep angular distortion
        // In mid/rear rows (y larger), lateral offset has gentler angular impact
        double lateralDist = 1.25 * lateralOffset;
        double viewAngleCos = (double) rowIndex / Math.sqrt((lateralDist * lateralDist) + ((double) rowIndex * rowIndex));
        double angleFactor = Math.max(0.30, Math.min(1.0, viewAngleCos));

        // Combined visual angle and centering factor
        double visualAngleFactor = (0.50 * centerFactor) + (0.50 * angleFactor);

        // ---------------------------------------------------------------------
        // 3. Acoustic Sweet Spot Factor (Multichannel Atmos dispersion)
        // Calibrated around center region and comfortable mid-depth (45% - 80%)
        // Front-row seats are outside the surround calibration cone
        // ---------------------------------------------------------------------
        double acousticFactor = 0.72;
        if (lateralOffset <= 1.8 && normDistance >= 0.40 && normDistance <= 0.80) {
            acousticFactor = 1.00; // Atmos core sweet spot
        } else if (lateralOffset <= 2.8 && normDistance >= 0.30 && normDistance <= 0.88) {
            acousticFactor = 0.86;
        }

        // ---------------------------------------------------------------------
        // 4. Zone Weight (VALUE = 0.88, STANDARD = 0.95, PREMIUM = 1.00)
        // Zone quality influences comfort, but does not override visual physics
        // ---------------------------------------------------------------------
        double zoneFactor = switch (zone) {
            case "PREMIUM" -> 1.00;
            case "VALUE" -> 0.88;
            default -> 0.95;
        };

        // ---------------------------------------------------------------------
        // 5. Hardware Comfort Multiplier
        // ---------------------------------------------------------------------
        double typeMultiplier = switch (typeCode) {
            case "RECLINER" -> 1.05;
            case "PREMIUM" -> 1.02;
            default -> 1.00;
        };

        // ---------------------------------------------------------------------
        // 6. Composite Score [40 - 99]
        // Dominant visual comfort (80%): Distance Comfort (45%), Viewing Angle (35%)
        // Supporting factors (20%): Acoustic Sweet Spot (10%), Zone Tier (10%)
        // ---------------------------------------------------------------------
        double rawComposite = (0.45 * distanceFactor + 0.35 * visualAngleFactor + 0.10 * acousticFactor + 0.10 * zoneFactor) * typeMultiplier;
        int compositeScore = (int) Math.round(Math.min(99, Math.max(40, rawComposite * 100)));

        dto.setDistanceFactor(Math.round(distanceFactor * 100.0) / 100.0);
        dto.setLateralFactor(Math.round(visualAngleFactor * 100.0) / 100.0);
        dto.setAcousticFactor(Math.round(acousticFactor * 100.0) / 100.0);
        dto.setZoneFactor(Math.round(zoneFactor * 100.0) / 100.0);
        dto.setScore(compositeScore);

        // ---------------------------------------------------------------------
        // 7. Explainability & Human-Readable Badges (Truthful & Physically Derived)
        // ---------------------------------------------------------------------
        boolean isSweetSpot = compositeScore >= 90 && normDistance >= 0.40 && normDistance <= 0.80 && lateralOffset <= 2.0;

        List<String> reasons = new ArrayList<>();
        if (compositeScore >= 90) {
            dto.setBadge("EXCELLENT");
            dto.setViewCategory(isSweetSpot ? "Preferred Viewing Area (Sweet Spot)" : "Preferred Viewing Area");
            reasons.add(isSweetSpot
                    ? "Preferred viewing sweet spot — comfortable distance with balanced center sightline"
                    : "Comfortable viewing position with clear screen visibility");
        } else if (compositeScore >= 80) {
            dto.setBadge("VERY_GOOD");
            dto.setViewCategory("Prime Viewing Arc");
            reasons.add("Prime viewing perspective with comfortable head angle and wide sightline");
        } else if (compositeScore >= 70) {
            dto.setBadge("GOOD");
            dto.setViewCategory("Good Screen Alignment");
            reasons.add("Good sightline to screen with clear audiovisual coverage");
        } else if (compositeScore >= 60) {
            dto.setBadge("FAIR");
            dto.setViewCategory("Moderate Angle / Near Screen");
            reasons.add("Auditorium seating with moderate viewing angle or screen proximity");
        } else {
            dto.setBadge("LESS_SUITABLE");
            dto.setViewCategory("Side Angle / Extreme Front");
            reasons.add("Peripheral viewing position with noticeable angle or steep vertical perspective");
        }

        if (lateralOffset <= 1.0) {
            reasons.add(String.format("Centered viewing position aligned with screen center (offset: %.1f seats from center)", lateralOffset));
        } else if (visualAngleFactor <= 0.68) {
            reasons.add("Wider peripheral viewing angle towards the side of the auditorium");
        }

        if (normDistance >= 0.45 && normDistance <= 0.75) {
            reasons.add(String.format("Comfortable viewing distance in Row %s (row %d of %d, ~%.0f%% room depth)",
                    rowLabel, rowIndex, totalRows, normDistance * 100.0));
        } else if (normDistance <= 0.25) {
            reasons.add(String.format("Front-row seating in Row %s close to the screen (vertical neck tilt)", rowLabel));
        } else if (normDistance >= 0.85) {
            reasons.add(String.format("Rear auditorium seating in Row %s with full-screen overview", rowLabel));
        }

        if (acousticFactor >= 0.95) {
            reasons.add("Within the recommended audio position for balanced multichannel immersion");
        }

        if ("PREMIUM".equals(zone)) {
            reasons.add("Premium pricing zone with elevated viewing comfort");
        } else if ("VALUE".equals(zone)) {
            reasons.add("Value pricing zone offering budget-friendly cinema experience");
        }

        if ("RECLINER".equals(typeCode)) {
            reasons.add("Luxury motorized recliner tier (+5% comfort bonus)");
        } else if ("PREMIUM".equals(typeCode)) {
            reasons.add("Premium cushioned ergonomic seating (+2% comfort bonus)");
        }

        dto.setReasons(reasons);

        if (seatHoldService != null && seat.getSeatId() != null) {
            try {
                dto.setPrice(seatHoldService.determineSeatPrice(seat.getSeatId()));
            } catch (Exception ignored) {
                dto.setPrice(new BigDecimal("150.00"));
            }
        } else {
            dto.setPrice(new BigDecimal("150.00"));
        }

        return dto;
    }

    public SeatScoreDTO scoreSeatById(Long seatId) {
        return seatRepository.findById(seatId)
                .map(this::scoreSeat)
                .orElseGet(() -> {
                    SeatScoreDTO dto = new SeatScoreDTO();
                    dto.setSeatId(seatId);
                    dto.setScore(75);
                    dto.setBadge("GOOD");
                    dto.setViewCategory("Standard Alignment");
                    dto.setPrice(new BigDecimal("150.00"));
                    return dto;
                });
    }

    private int parseSeatNumber(String seatNumber) {
        if (seatNumber == null) return 5;
        try {
            return Integer.parseInt(seatNumber.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 5;
        }
    }

    private int parseRowIndexFromLabel(String rowLabel) {
        if (rowLabel == null || rowLabel.trim().isEmpty()) return 3;
        String clean = rowLabel.toUpperCase().trim();
        char c = clean.charAt(0);
        if (c >= 'A' && c <= 'Z') {
            return (c - 'A') + 1;
        }
        try {
            return Integer.parseInt(clean.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 3;
        }
    }
}
