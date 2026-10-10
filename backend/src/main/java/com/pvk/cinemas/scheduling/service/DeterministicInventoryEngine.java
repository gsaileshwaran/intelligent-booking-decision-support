package com.pvk.cinemas.scheduling.service;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.infrastructure.model.Seat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative Deterministic Inventory Engine (§8.1).
 *
 * Implements the two canonical pure functions:
 * 1. Function G: TargetOccupancy = G(demandTier, popularityScore, rho, dayType, theatreTier, zone, geom)
 *    Bounded strictly in [0.05, 0.98].
 * 2. Function F: SeatState = F(datasetVersion, globalSeed, showId, seatId, scheduleDate, TargetOccupancy)
 *    MD5 hash fraction evaluated against TargetOccupancy.
 *
 * Guaranteed reproducible across eager active seeding, JIT advance materialization,
 * and AI recommendation read paths.
 */
@Service
public class DeterministicInventoryEngine {

    private final String datasetVersion;
    private final String globalSeed;

    public DeterministicInventoryEngine(
            @Value("${pvk.dataset.version:2026.10}") String datasetVersion,
            @Value("${pvk.dataset.seed:PVK-PHASE1-SEED-42}") String globalSeed) {
        this.datasetVersion = datasetVersion;
        this.globalSeed = globalSeed;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public String getGlobalSeed() {
        return globalSeed;
    }

    /**
     * Function G: Pure Target Occupancy calculation.
     * Evaluates TargetOccupancy bounded strictly in [0.05, 0.98].
     */
    public double calculateTargetOccupancy(
            String demandTier,
            BigDecimal popularityScore,
            double rho,
            String dayType,
            String theatreTier,
            String pricingZone,
            double geometricWeight) {

        // 1. Base occupancy by demand tier
        double base;
        if ("BLOCKBUSTER".equalsIgnoreCase(demandTier)) {
            base = 0.65;
        } else if ("MAJOR_REGIONAL".equalsIgnoreCase(demandTier)) {
            base = 0.55;
        } else if ("MID_TIER".equalsIgnoreCase(demandTier)) {
            base = 0.45;
        } else if ("RE_RELEASE".equalsIgnoreCase(demandTier)) {
            base = 0.40;
        } else {
            base = 0.35; // NICHE or fallback
        }

        // 2. Popularity score adjustment (scale 0-10, centered at 8.0)
        double pop = (popularityScore != null) ? popularityScore.doubleValue() : 8.0;
        double popAdj = (pop - 8.0) * 0.05;

        // 3. Lifecycle stage (rho) adjustment
        double stageAdj;
        if (rho < 0.15) {
            stageAdj = 0.15; // OPENING
        } else if (rho < 0.40) {
            stageAdj = 0.10; // PEAK
        } else if (rho < 0.70) {
            stageAdj = 0.00; // REGULAR_RUN
        } else if (rho < 0.90) {
            stageAdj = -0.15; // REDUCED_RUN
        } else {
            stageAdj = -0.25; // FINAL_DAYS
        }

        // 4. Day type adjustment
        double dayAdj = "WEEKEND".equalsIgnoreCase(dayType) ? 0.15 : -0.05;

        // 5. Theatre tier adjustment
        double theatreAdj;
        if ("FLAGSHIP".equalsIgnoreCase(theatreTier)) {
            theatreAdj = 0.08;
        } else if ("BOUTIQUE".equalsIgnoreCase(theatreTier)) {
            theatreAdj = -0.05;
        } else {
            theatreAdj = 0.00; // SUBURBAN
        }

        // 6. Pricing zone adjustment
        double zoneAdj;
        if ("PREMIUM".equalsIgnoreCase(pricingZone) || "VIP".equalsIgnoreCase(pricingZone) || "PRIME".equalsIgnoreCase(pricingZone)) {
            zoneAdj = 0.05;
        } else if ("VALUE".equalsIgnoreCase(pricingZone) || "ECONOMY".equalsIgnoreCase(pricingZone)) {
            zoneAdj = 0.02;
        } else {
            zoneAdj = 0.00;
        }

        // 7. Geometric weight adjustment (normalized in [-1.0, 1.0])
        double geomAdj = geometricWeight * 0.05;

        double occupancy = base + popAdj + stageAdj + dayAdj + theatreAdj + zoneAdj + geomAdj;

        // Clamp strictly within [0.05, 0.98] per architecture §8.1
        return Math.max(0.05, Math.min(0.98, occupancy));
    }

    /**
     * Computes the normalized HashFraction using the canonical MD5 hash formula (§8.1):
     * HashFraction = CONV(SUBSTRING(MD5(CONCAT(version, ':', seed, ':', showId, ':', seatId, ':', date)), 1, 8), 16, 10) / 4294967295.0
     */
    public double calculateHashFraction(
            String version,
            String seed,
            Long showId,
            Long seatId,
            LocalDate scheduleDate) {
        String effectiveVersion = (version != null && !version.isBlank()) ? version : this.datasetVersion;
        String effectiveSeed = (seed != null && !seed.isBlank()) ? seed : this.globalSeed;
        String input = effectiveVersion + ":" + effectiveSeed + ":" + showId + ":" + seatId + ":" + scheduleDate;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String hex = Integer.toHexString(0xff & digest[i]);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            long hexVal = Long.parseLong(sb.toString(), 16);
            return (double) hexVal / 4294967295.0;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 digest algorithm not available", e);
        }
    }

    /**
     * Function F: Pure Seat State Evaluation (§8.1).
     * Returns:
     * - 'BLOCKED' if physical SEAT.status == 'BLOCKED' or 'INACTIVE'
     * - 'BOOKED' if HashFraction < TargetOccupancy
     * - 'AVAILABLE' otherwise
     */
    public String evaluateSeatState(
            String version,
            String seed,
            Long showId,
            Long seatId,
            LocalDate scheduleDate,
            double targetOccupancy,
            String physicalSeatStatus) {

        if ("BLOCKED".equalsIgnoreCase(physicalSeatStatus) || "INACTIVE".equalsIgnoreCase(physicalSeatStatus)) {
            return "BLOCKED";
        }

        double hashFraction = calculateHashFraction(version, seed, showId, seatId, scheduleDate);
        if (hashFraction < targetOccupancy) {
            return "BOOKED";
        }
        return "AVAILABLE";
    }

    /**
     * Calculates the geometric weight for a physical seat based on zone and grid position.
     * Center seats in prime rows have highest positive weight (+1.0); front edge seats have negative weight (-1.0).
     */
    public double calculateGeometricWeight(Seat seat) {
        if (seat == null) return 0.0;
        double weight = 0.0;
        String zone = seat.getPricingZone();
        if ("PREMIUM".equalsIgnoreCase(zone)) {
            weight += 0.5;
        } else if ("VALUE".equalsIgnoreCase(zone)) {
            weight -= 0.5;
        }

        // Slight boost for central columns (normalized between 0.0 and 1.0)
        Integer col = seat.getGridColIndex();
        if (col != null && col > 0) {
            // Typical screen width 12-20: middle columns get boost
            double normCol = Math.sin((col / 20.0) * Math.PI);
            weight += (normCol - 0.5);
        }
        return Math.max(-1.0, Math.min(1.0, weight));
    }

    /**
     * Generates all ShowSeat instances for a given show and physical seats.
     */
    public List<ShowSeat> generateShowSeats(
            Long showId,
            LocalDate showDate,
            List<Seat> physicalSeats,
            String demandTier,
            BigDecimal popularityScore,
            double rho,
            String dayType,
            String theatreTier) {

        List<ShowSeat> showSeats = new ArrayList<>(physicalSeats.size());
        for (Seat seat : physicalSeats) {
            double geomWeight = calculateGeometricWeight(seat);
            double targetOccupancy = calculateTargetOccupancy(
                    demandTier,
                    popularityScore,
                    rho,
                    dayType,
                    theatreTier,
                    seat.getPricingZone(),
                    geomWeight
            );

            String status = evaluateSeatState(
                    this.datasetVersion,
                    this.globalSeed,
                    showId,
                    seat.getSeatId(),
                    showDate,
                    targetOccupancy,
                    seat.getStatus()
            );

            showSeats.add(new ShowSeat(showId, seat.getSeatId(), status));
        }
        return showSeats;
    }
}
