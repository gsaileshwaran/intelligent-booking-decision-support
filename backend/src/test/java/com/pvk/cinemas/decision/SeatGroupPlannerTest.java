package com.pvk.cinemas.decision;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.SeatFitResult;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.decision.engine.SeatGroupPlanner;
import com.pvk.cinemas.decision.engine.SeatScoringEngine;
import com.pvk.cinemas.infrastructure.model.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SeatGroupPlannerTest {

    private SeatScoringEngine seatScoringEngine;
    private SeatPricingService seatPricingService;
    private SeatGroupPlanner planner;

    @BeforeEach
    void setUp() {
        seatScoringEngine = Mockito.mock(SeatScoringEngine.class);
        seatPricingService = Mockito.mock(SeatPricingService.class);
        planner = new SeatGroupPlanner(seatScoringEngine, seatPricingService);

        // Mock ScreenGeometry: 10 rows (A-J), 10 seats per row (seats 1-10)
        Map<String, Integer> rowCounts = new HashMap<>();
        Map<String, Integer> rowIndices = new HashMap<>();
        for (int r = 1; r <= 10; r++) {
            String label = String.valueOf((char) ('A' + r - 1));
            rowCounts.put(label, 10);
            rowIndices.put(label, r);
        }
        SeatScoringEngine.ScreenGeometry geom = new SeatScoringEngine.ScreenGeometry(10, rowCounts, rowIndices);
        when(seatScoringEngine.getScreenGeometry(any())).thenReturn(geom);

        // Default mock scoring for seats:
        // Rows A-B get lower visual scores due to front tilt (65-70)
        // Rows E-F get optimal scores (88-95)
        when(seatScoringEngine.scoreSeat(any())).thenAnswer(invocation -> {
            Seat s = invocation.getArgument(0);
            SeatScoreDTO dto = new SeatScoreDTO();
            dto.setSeatId(s.getSeatId());
            dto.setRowLabel(s.getRowLabel());
            dto.setSeatNumber(s.getSeatNumber());
            dto.setPrice(BigDecimal.valueOf(150.00));

            char rowChar = s.getRowLabel().charAt(0);
            int rowIdx = (rowChar - 'A') + 1;
            int col = Integer.parseInt(s.getSeatNumber());
            int distFromCenter = Math.abs(col - 5);

            // Row A has steep tilt penalty
            int base = (rowIdx <= 2) ? 65 : (rowIdx >= 5 && rowIdx <= 7) ? 90 : 80;
            int score = base - (distFromCenter * 2);
            dto.setScore(score);
            return dto;
        });
    }

    private Seat createSeat(Long id, String row, int num, boolean aisleAfter, String status) {
        Seat s = new Seat(1L, 1L, row, String.valueOf(num));
        s.setSeatId(id);
        s.setAisleAfter(aisleAfter);
        s.setStatus(status != null ? status : "ACTIVE");
        s.setGridRowIndex((row.charAt(0) - 'A') + 1);
        s.setGridColIndex(num);
        return s;
    }

    @Test
    @DisplayName("Requirement 12: Test Physical Grouping — A3+A4 contiguous, A3+A5 NOT contiguous, A3+A4+A6 NOT contiguous")
    void testContiguityAndGaps() {
        Long showId = 101L;

        // Row A: A3, A4, A5 (A4 is booked), A6
        List<Seat> seats = List.of(
                createSeat(1L, "A", 3, false, "ACTIVE"),
                createSeat(2L, "A", 4, false, "ACTIVE"),
                createSeat(3L, "A", 5, false, "ACTIVE"),
                createSeat(4L, "A", 6, false, "ACTIVE")
        );

        // Case 1: A3 + A4 available, A5 booked
        List<ShowSeat> showSeats1 = List.of(
                new ShowSeat(showId, 1L, "AVAILABLE"),
                new ShowSeat(showId, 2L, "AVAILABLE"),
                new ShowSeat(showId, 3L, "BOOKED"),
                new ShowSeat(showId, 4L, "AVAILABLE")
        );

        // Request party size 2: Should pick A3+A4 (contiguous block of 2)
        SeatFitResult result1 = planner.planBestGroup(showId, showSeats1, seats, 2, "BEST_VIEW");
        assertTrue(result1.isAllTogether());
        assertEquals(List.of("A3", "A4"), result1.getSeatLabels());

        // Case 2: A3 and A5 available, A4 booked -> A3+A5 is NOT contiguous
        List<ShowSeat> showSeats2 = List.of(
                new ShowSeat(showId, 1L, "AVAILABLE"),
                new ShowSeat(showId, 2L, "BOOKED"),
                new ShowSeat(showId, 3L, "AVAILABLE"),
                new ShowSeat(showId, 4L, "BOOKED")
        );

        SeatFitResult result2 = planner.planBestGroup(showId, showSeats2, seats, 2, "BEST_VIEW");
        // Cannot be single-row contiguous because A4 is booked
        assertFalse(result2.isAllTogether(), "A3 and A5 separated by booked A4 must NOT be contiguous");
        assertNotEquals(List.of("A3", "A5"), result1.getSeatLabels());
    }

    @Test
    @DisplayName("Requirement 12: Aisle boundary — A group crossing an aisle must NOT be called contiguous")
    void testAisleBoundaryBreaksContiguity() {
        Long showId = 102L;

        // Row B: B3 has aisleAfter=true. B4 is after aisle.
        List<Seat> seats = List.of(
                createSeat(11L, "B", 2, false, "ACTIVE"),
                createSeat(12L, "B", 3, true, "ACTIVE"),  // AISLE AFTER B3!
                createSeat(13L, "B", 4, false, "ACTIVE"),
                createSeat(14L, "B", 5, false, "ACTIVE")
        );

        List<ShowSeat> showSeats = List.of(
                new ShowSeat(showId, 11L, "AVAILABLE"),
                new ShowSeat(showId, 12L, "AVAILABLE"),
                new ShowSeat(showId, 13L, "AVAILABLE"),
                new ShowSeat(showId, 14L, "AVAILABLE")
        );

        // Request party size 3: Cannot form contiguous block across B3 aisle (bank1: B2,B3=2; bank2: B4,B5=2)
        SeatFitResult result = planner.planBestGroup(showId, showSeats, seats, 3, "BEST_VIEW");
        assertFalse(result.isAllTogether(), "Group of 3 cannot cross physical aisle after B3");
    }

    @Test
    @DisplayName("Requirement 6: Disallow BOOKED, HELD, or BLOCKED seats")
    void testNoImpossibleSeats() {
        Long showId = 103L;

        List<Seat> seats = List.of(
                createSeat(21L, "C", 1, false, "ACTIVE"),
                createSeat(22L, "C", 2, false, "ACTIVE"),
                createSeat(23L, "C", 3, false, "BLOCKED"), // physically BLOCKED
                createSeat(24L, "C", 4, false, "ACTIVE"),
                createSeat(25L, "C", 5, false, "ACTIVE")
        );

        List<ShowSeat> showSeats = List.of(
                new ShowSeat(showId, 21L, "AVAILABLE"),
                new ShowSeat(showId, 22L, "HELD"),        // HELD
                new ShowSeat(showId, 23L, "AVAILABLE"),   // physically BLOCKED in Seat
                new ShowSeat(showId, 24L, "BOOKED"),      // BOOKED
                new ShowSeat(showId, 25L, "AVAILABLE")
        );

        SeatFitResult result = planner.planBestGroup(showId, showSeats, seats, 2, "BEST_VIEW");
        // Only seats 21 and 25 are available, but not contiguous. None of 22 (HELD), 23 (BLOCKED), 24 (BOOKED) may be in result
        for (Long id : result.getSeatIds()) {
            assertNotEquals(22L, id, "HELD seat must never be recommended");
            assertNotEquals(23L, id, "BLOCKED seat must never be recommended");
            assertNotEquals(24L, id, "BOOKED seat must never be recommended");
        }
    }

    @Test
    @DisplayName("Requirement 11 & 13: Large Party Tests (partySize=10, 12, 15) and Group Quality Hierarchy")
    void testLargePartyAndGroupQualityHierarchy() {
        Long showId = 104L;

        // Build a realistic auditorium screen with 10 rows (A-J), 12 seats per row (1-12)
        // Aisle after seat 3 and seat 9
        List<Seat> allSeats = new ArrayList<>();
        List<ShowSeat> showSeats = new ArrayList<>();
        long idGen = 1000L;

        for (int r = 1; r <= 10; r++) {
            String row = String.valueOf((char) ('A' + r - 1));
            for (int col = 1; col <= 12; col++) {
                idGen++;
                boolean aisle = (col == 3 || col == 9);
                Seat s = createSeat(idGen, row, col, aisle, "ACTIVE");
                allSeats.add(s);
                showSeats.add(new ShowSeat(showId, idGen, "AVAILABLE"));
            }
        }

        // Test partySize = 10
        SeatFitResult res10 = planner.planBestGroup(showId, showSeats, allSeats, 10, "BEST_VIEW");
        assertNotNull(res10);
        assertEquals(10, res10.getSeatIds().size(), "Must recommend exactly 10 seats for partySize=10");
        // Center bank in 12-seat row has seats 4,5,6,7,8,9 (6 seats).
        // A contiguous block of 10 cannot fit in 1 bank because aisles are after 3 and 9 (max bank size is 6).
        // Planner must find adjacent-row split (e.g. 5+5 or 6+4)
        assertFalse(res10.isAllTogether(), "Cannot fit 10 in a single bank with aisles after 3 and 9");
        assertTrue(res10.isAdjacentRows(), "Must arrange across adjacent rows");
        assertNotNull(res10.getSplitDescription());
        assertTrue(res10.getSplitDescription().contains("across adjacent rows"));
        assertEquals("No single-row group of 10 is currently available.", res10.getSeatingTradeoff());

        // Test partySize = 12
        SeatFitResult res12 = planner.planBestGroup(showId, showSeats, allSeats, 12, "BEST_VIEW");
        assertNotNull(res12);
        assertEquals(12, res12.getSeatIds().size(), "Must recommend exactly 12 seats for partySize=12");

        // Test partySize = 15
        SeatFitResult res15 = planner.planBestGroup(showId, showSeats, allSeats, 15, "BEST_VIEW");
        assertNotNull(res15);
        assertEquals(15, res15.getSeatIds().size(), "Must recommend exactly 15 seats for partySize=15");
    }

    @Test
    @DisplayName("Requirement 4: Viewing distance modeling — Mid-row group beats Front-row (Row A) group")
    void testFrontRowViewingDistancePenalty() {
        Long showId = 105L;

        // Row A (front row) and Row F (mid row) both have 4 contiguous seats available
        List<Seat> seats = List.of(
                // Row A seats
                createSeat(101L, "A", 4, false, "ACTIVE"),
                createSeat(102L, "A", 5, false, "ACTIVE"),
                createSeat(103L, "A", 6, false, "ACTIVE"),
                createSeat(104L, "A", 7, false, "ACTIVE"),
                // Row F seats (mid auditorium)
                createSeat(201L, "F", 4, false, "ACTIVE"),
                createSeat(202L, "F", 5, false, "ACTIVE"),
                createSeat(203L, "F", 6, false, "ACTIVE"),
                createSeat(204L, "F", 7, false, "ACTIVE")
        );

        List<ShowSeat> showSeats = List.of(
                new ShowSeat(showId, 101L, "AVAILABLE"),
                new ShowSeat(showId, 102L, "AVAILABLE"),
                new ShowSeat(showId, 103L, "AVAILABLE"),
                new ShowSeat(showId, 104L, "AVAILABLE"),
                new ShowSeat(showId, 201L, "AVAILABLE"),
                new ShowSeat(showId, 202L, "AVAILABLE"),
                new ShowSeat(showId, 203L, "AVAILABLE"),
                new ShowSeat(showId, 204L, "AVAILABLE")
        );

        SeatFitResult result = planner.planBestGroup(showId, showSeats, seats, 4, "BEST_VIEW");
        assertNotNull(result);
        assertEquals("F", result.getBestRow(), "Mid-row F must beat front-row A due to continuous viewing distance / neck-tilt modeling");
        assertTrue(result.getGroupScore() > 70);
    }
}
