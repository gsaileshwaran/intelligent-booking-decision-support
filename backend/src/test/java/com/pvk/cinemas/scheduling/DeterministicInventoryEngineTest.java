package com.pvk.cinemas.scheduling;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.scheduling.service.DeterministicInventoryEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicInventoryEngineTest {

    private DeterministicInventoryEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DeterministicInventoryEngine("2026.10", "PVK-PHASE1-SEED-42");
    }

    @Test
    @DisplayName("Pure Function G: Target occupancy is strictly bounded in [0.05, 0.98]")
    void testTargetOccupancyBounds() {
        String[] tiers = {"BLOCKBUSTER", "MAJOR_REGIONAL", "MID_TIER", "RE_RELEASE", "NICHE"};
        BigDecimal[] pops = {BigDecimal.valueOf(1.0), BigDecimal.valueOf(5.0), BigDecimal.valueOf(8.0), BigDecimal.valueOf(10.0)};
        double[] rhos = {0.0, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0};
        String[] dayTypes = {"WEEKEND", "WEEKDAY"};
        String[] theatreTiers = {"FLAGSHIP", "BOUTIQUE", "SUBURBAN"};
        String[] zones = {"PREMIUM", "VIP", "PRIME", "VALUE", "STANDARD"};
        double[] geomWeights = {-1.0, -0.5, 0.0, 0.5, 1.0};

        for (String tier : tiers) {
            for (BigDecimal pop : pops) {
                for (double rho : rhos) {
                    for (String dayType : dayTypes) {
                        for (String theatreTier : theatreTiers) {
                            for (String zone : zones) {
                                for (double geom : geomWeights) {
                                    double g = engine.calculateTargetOccupancy(
                                            tier, pop, rho, dayType, theatreTier, zone, geom
                                    );
                                    assertTrue(g >= 0.05, "G was below 0.05: " + g);
                                    assertTrue(g <= 0.98, "G was above 0.98: " + g);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Pure Function F: Hash fraction is strictly in [0.0, 1.0) and deterministic")
    void testHashFractionDeterminismAndBounds() {
        long showId = 12345L;
        long seatId = 6789L;
        LocalDate date = LocalDate.of(2026, 9, 18);

        double fraction1 = engine.calculateHashFraction("2026.10", "PVK-PHASE1-SEED-42", showId, seatId, date);
        double fraction2 = engine.calculateHashFraction("2026.10", "PVK-PHASE1-SEED-42", showId, seatId, date);

        assertEquals(fraction1, fraction2, "Hash fraction must be 100% deterministic");
        assertTrue(fraction1 >= 0.0 && fraction1 < 1.0, "Hash fraction must be in [0.0, 1.0)");

        // Distinct seat IDs must produce varying hash fractions
        double fractionOther = engine.calculateHashFraction("2026.10", "PVK-PHASE1-SEED-42", showId, seatId + 1, date);
        assertNotEquals(fraction1, fractionOther, "Different seats should have distinct hash fractions");
    }

    @Test
    @DisplayName("Deterministic seat inventory generation reproduces identical inventory")
    void testDeterministicInventoryGeneration() {
        long showId = 999L;
        LocalDate showDate = LocalDate.of(2026, 9, 20);

        List<Seat> physicalSeats = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            Seat seat = new Seat(10L, 1L, "A", String.valueOf(i));
            seat.setSeatId(i);
            seat.setPricingZone(i <= 20 ? "PREMIUM" : (i >= 80 ? "VALUE" : "STANDARD"));
            seat.setGridColIndex((int) (i % 20 + 1));
            seat.setStatus("ACTIVE");
            physicalSeats.add(seat);
        }

        List<ShowSeat> run1 = engine.generateShowSeats(
                showId, showDate, physicalSeats, "BLOCKBUSTER", BigDecimal.valueOf(9.5), 0.1, "WEEKEND", "FLAGSHIP"
        );
        List<ShowSeat> run2 = engine.generateShowSeats(
                showId, showDate, physicalSeats, "BLOCKBUSTER", BigDecimal.valueOf(9.5), 0.1, "WEEKEND", "FLAGSHIP"
        );

        assertEquals(100, run1.size());
        assertEquals(100, run2.size());

        for (int i = 0; i < 100; i++) {
            ShowSeat s1 = run1.get(i);
            ShowSeat s2 = run2.get(i);

            assertEquals(s1.getId().getShowId(), s2.getId().getShowId());
            assertEquals(s1.getId().getSeatId(), s2.getId().getSeatId());
            assertEquals(s1.getAvailabilityStatus(), s2.getAvailabilityStatus());
        }

        // Verify status distribution has valid values
        long bookedCount = run1.stream().filter(s -> "BOOKED".equals(s.getAvailabilityStatus())).count();
        long availableCount = run1.stream().filter(s -> "AVAILABLE".equals(s.getAvailabilityStatus())).count();
        long blockedCount = run1.stream().filter(s -> "BLOCKED".equals(s.getAvailabilityStatus())).count();

        assertEquals(100, bookedCount + availableCount + blockedCount);
        assertTrue(bookedCount > 0, "Sunday Blockbuster should have booked seats");
        assertTrue(availableCount > 0, "Should have available seats");
    }
}
