package com.pvk.cinemas.decision;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.SeatRecommendationRequest;
import com.pvk.cinemas.decision.dto.SeatRecommendationResponse;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.decision.engine.SeatGroupPlanner;
import com.pvk.cinemas.decision.engine.SeatRecommendationEngine;
import com.pvk.cinemas.decision.engine.SeatScoringEngine;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class SyntheticLayoutSeatScoringTest {

    private SeatRepository seatRepository;
    private SeatTypeRepository seatTypeRepository;
    private ShowSeatRepository showSeatRepository;
    private SeatPricingService seatPricingService;
    private SeatScoringEngine scoringEngine;
    private SeatRecommendationEngine recommendationEngine;

    @BeforeEach
    void setUp() {
        seatRepository = Mockito.mock(SeatRepository.class);
        seatTypeRepository = Mockito.mock(SeatTypeRepository.class);
        showSeatRepository = Mockito.mock(ShowSeatRepository.class);
        seatPricingService = Mockito.mock(SeatPricingService.class);

        scoringEngine = new SeatScoringEngine(seatRepository, seatTypeRepository, null);
        SeatGroupPlanner seatGroupPlanner = new SeatGroupPlanner(scoringEngine, seatPricingService);
        recommendationEngine = new SeatRecommendationEngine(showSeatRepository, seatRepository, seatGroupPlanner);

        when(seatTypeRepository.findById(1L)).thenReturn(Optional.of(new SeatType(1L, "STANDARD", "Standard", "Standard")));
        when(seatTypeRepository.findById(2L)).thenReturn(Optional.of(new SeatType(2L, "PREMIUM", "Premium", "Premium")));
        when(seatTypeRepository.findById(3L)).thenReturn(Optional.of(new SeatType(3L, "RECLINER", "Recliner", "Recliner")));
    }

    private Seat createSeat(Long id, Long screenId, String row, int num, String zone, boolean aisleAfter) {
        Seat s = new Seat(screenId, "PREMIUM".equalsIgnoreCase(zone) ? 2L : 1L, row, String.valueOf(num), zone, aisleAfter);
        s.setSeatId(id);
        s.setGridColIndex(num);
        return s;
    }

    @Test
    @DisplayName("Synthetic 1: 10 rows x 12 seats layout — center seats score higher horizontally than edges")
    void test10x12Layout_centerScoresHigherHorizontally() {
        // In row F (row 6 of 10) with 12 seats, center is (12+1)/2 = 6.5
        Seat centerSeat = createSeat(106L, 1L, "F", 6, "STANDARD", false);
        Seat edgeSeat = createSeat(101L, 1L, "F", 1, "STANDARD", false);

        SeatScoreDTO centerScore = scoringEngine.scoreSeat(centerSeat, 10, 12);
        SeatScoreDTO edgeScore = scoringEngine.scoreSeat(edgeSeat, 10, 12);

        assertTrue(centerScore.getLateralFactor() > edgeScore.getLateralFactor(),
                "Center seat lateral factor (" + centerScore.getLateralFactor() + ") must beat edge seat (" + edgeScore.getLateralFactor() + ")");
        assertTrue(centerScore.getScore() > edgeScore.getScore(),
                "Center seat composite score (" + centerScore.getScore() + ") must beat edge seat (" + edgeScore.getScore() + ")");
        assertTrue(centerScore.getLateralFactor() >= 0.95, "Center lateral factor should be >= 0.95");
        assertTrue(edgeScore.getLateralFactor() <= 0.65, "Edge lateral factor should be <= 0.65");
    }

    @Test
    @DisplayName("Synthetic 2: 15 rows x 20 seats layout — optical depth curve adapts without hardcoding")
    void test15x20Layout_opticalViewingCurveAdapts() {
        // Ideal viewing depth is ~65% of 15 rows -> Row 10 (index 10 / 15 = 0.67)
        Seat frontSeat = createSeat(201L, 2L, "A", 10, "VALUE", false);      // Row 1 / 15 = 0.07 (extreme front)
        Seat sweetSpotSeat = createSeat(210L, 2L, "J", 10, "PREMIUM", false); // Row 10 / 15 = 0.67 (ideal depth)
        Seat rearSeat = createSeat(215L, 2L, "O", 10, "STANDARD", false);     // Row 15 / 15 = 1.00 (back row)

        SeatScoreDTO frontScore = scoringEngine.scoreSeat(frontSeat, 15, 20);
        SeatScoreDTO sweetSpotScore = scoringEngine.scoreSeat(sweetSpotSeat, 15, 20);
        SeatScoreDTO rearScore = scoringEngine.scoreSeat(rearSeat, 15, 20);

        assertTrue(sweetSpotScore.getDistanceFactor() > frontScore.getDistanceFactor(),
                "Sweet spot distance factor must beat extreme front row");
        assertTrue(sweetSpotScore.getDistanceFactor() > rearScore.getDistanceFactor(),
                "Sweet spot distance factor must beat extreme rear row");
        assertTrue(sweetSpotScore.getScore() > frontScore.getScore(),
                "Sweet spot composite score must beat front row");
        assertTrue(frontScore.getDistanceFactor() <= 0.65, "Extreme front row must receive neck strain penalty");
    }

    @Test
    @DisplayName("Synthetic 3: Rows with aisles — seats across aisle are not contiguous")
    void testRowsWithAisles_notContiguousAcrossAisle() {
        Long showId = 501L;
        // Row D: 8 seats. Aisle after seat 4 (Bank 1: 1-4, Bank 2: 5-8)
        List<Seat> seats = new ArrayList<>();
        List<ShowSeat> showSeats = new ArrayList<>();

        for (int i = 1; i <= 8; i++) {
            Long sid = 500L + i;
            boolean aisle = (i == 4);
            Seat s = createSeat(sid, 3L, "D", i, "STANDARD", aisle);
            seats.add(s);
            showSeats.add(new ShowSeat(showId, sid, "AVAILABLE"));
        }

        when(showSeatRepository.findByIdShowId(showId)).thenReturn(showSeats);
        when(seatRepository.findAllById(anyList())).thenReturn(seats);

        // Request party size 4
        SeatRecommendationResponse resp = recommendationEngine.recommend(showId, new SeatRecommendationRequest(4, "BEST_VIEW"));

        assertNotNull(resp);
        assertFalse(resp.getRecommendations().isEmpty());

        // Recommended block must either be Bank 1 (D1-D4) or Bank 2 (D5-D8)
        // It must NOT be D3, D4, D5, D6 spanning across the aisle
        for (RecommendedBlock b : resp.getRecommendations()) {
            boolean crossesAisle = b.getSeatLabels().contains("D4") && b.getSeatLabels().contains("D5");
            assertFalse(crossesAisle, "Recommended group must not cross physical walkway aisle between D4 and D5");
        }
    }

    @Test
    @DisplayName("Synthetic 4: Uneven row lengths — row centering adapts dynamically")
    void testUnevenRowLengths_centerAdaptsDynamically() {
        // Row A has 8 seats -> center is (8+1)/2 = 4.5 (Seat 4 and 5 are center)
        Seat rowASeat4 = createSeat(304L, 4L, "A", 4, "STANDARD", false);
        Seat rowASeat1 = createSeat(301L, 4L, "A", 1, "STANDARD", false);

        // Row B has 16 seats -> center is (16+1)/2 = 8.5 (Seat 8 and 9 are center)
        Seat rowBSeat8 = createSeat(318L, 4L, "B", 8, "STANDARD", false);
        Seat rowBSeat4 = createSeat(314L, 4L, "B", 4, "STANDARD", false); // Seat 4 in 16-seat row is side!

        SeatScoreDTO scoreA4 = scoringEngine.scoreSeat(rowASeat4, 2, 8);
        SeatScoreDTO scoreA1 = scoringEngine.scoreSeat(rowASeat1, 2, 8);
        SeatScoreDTO scoreB8 = scoringEngine.scoreSeat(rowBSeat8, 2, 16);
        SeatScoreDTO scoreB4 = scoringEngine.scoreSeat(rowBSeat4, 2, 16);

        assertTrue(scoreA4.getLateralFactor() > scoreA1.getLateralFactor(), "Seat 4 is center in 8-seat row");
        assertTrue(scoreB8.getLateralFactor() > scoreB4.getLateralFactor(), "Seat 8 is center in 16-seat row");
        assertTrue(scoreA4.getLateralFactor() >= 0.90, "Seat 4 in row A should have high lateral factor (>= 0.90)");
        assertTrue(scoreB8.getLateralFactor() >= 0.95, "Seat 8 in row B should have center lateral factor (>= 0.95)");
        assertTrue(scoreB4.getLateralFactor() < 0.85, "Seat 4 in 16-seat row is off-center");
    }

    @Test
    @DisplayName("Synthetic 5: Standard center seat beats extreme-side Premium seat")
    void testStandardCenterBeatsExtremePremiumSide() {
        // Row G (row 7 of 12, depth = 0.58).
        // Center Standard seat: Col 5 in 10-seat row
        // Extreme side Premium seat: Col 1 in 10-seat row
        Seat standardCenter = createSeat(405L, 5L, "G", 5, "STANDARD", false);
        Seat premiumSide = createSeat(401L, 5L, "G", 1, "PREMIUM", false);

        SeatScoreDTO standardScore = scoringEngine.scoreSeat(standardCenter, 12, 10);
        SeatScoreDTO premiumScore = scoringEngine.scoreSeat(premiumSide, 12, 10);

        assertTrue(standardScore.getScore() > premiumScore.getScore(),
                "Center Standard seat (" + standardScore.getScore() + ") must beat extreme-side Premium seat (" + premiumScore.getScore() + ")");
    }

    @Test
    @DisplayName("Synthetic 6: Booked and held seats are excluded from recommendations")
    void testBookedAndHeldSeatsExcluded() {
        Long showId = 601L;
        List<Seat> seats = new ArrayList<>();
        List<ShowSeat> showSeats = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            Long sid = 600L + i;
            seats.add(createSeat(sid, 6L, "C", i, "STANDARD", false));
            // C3 is BOOKED, C4 is HELD
            String status = (i == 3) ? "BOOKED" : (i == 4) ? "HELD" : "AVAILABLE";
            showSeats.add(new ShowSeat(showId, sid, status));
        }

        when(showSeatRepository.findByIdShowId(showId)).thenReturn(showSeats);
        when(seatRepository.findAllById(anyList())).thenReturn(seats);

        SeatRecommendationResponse resp = recommendationEngine.recommend(showId, new SeatRecommendationRequest(2, "BEST_VIEW"));

        assertNotNull(resp);
        for (RecommendedBlock b : resp.getRecommendations()) {
            assertFalse(b.getSeatLabels().contains("C3"), "Booked seat C3 must not be recommended");
            assertFalse(b.getSeatLabels().contains("C4"), "Held seat C4 must not be recommended");
        }
    }

    @Test
    @DisplayName("Synthetic 7: Contiguous group preferred over scattered individual seats")
    void testContiguousGroupPreferredOverScatteredSeats() {
        Long showId = 701L;
        // Row E has a contiguous block E4, E5
        // Row A (front) has scattered seats A1, A6
        List<Seat> seats = List.of(
                createSeat(701L, 7L, "E", 4, "STANDARD", false),
                createSeat(702L, 7L, "E", 5, "STANDARD", false),
                createSeat(703L, 7L, "A", 1, "VALUE", false),
                createSeat(704L, 7L, "A", 6, "VALUE", false)
        );

        List<ShowSeat> showSeats = seats.stream()
                .map(s -> new ShowSeat(showId, s.getSeatId(), "AVAILABLE"))
                .toList();

        when(showSeatRepository.findByIdShowId(showId)).thenReturn(showSeats);
        when(seatRepository.findAllById(anyList())).thenReturn(seats);

        SeatRecommendationResponse resp = recommendationEngine.recommend(showId, new SeatRecommendationRequest(2, "BEST_VIEW"));

        assertNotNull(resp);
        assertFalse(resp.getRecommendations().isEmpty());
        RecommendedBlock topPick = resp.getRecommendations().get(0);

        assertEquals(List.of("E4", "E5"), topPick.getSeatLabels(),
                "Contiguous pair E4-E5 must be selected over scattered seats");
    }

    @Test
    @DisplayName("Synthetic 8: Fallback split-party recommendation when no single contiguous block of 4 exists")
    void testFallbackWhenNoContiguousBlockLargeEnough() {
        Long showId = 801L;
        // Only pairs available: Row D has 2 seats (D1, D2), Row E has 2 seats (E1, E2)
        List<Seat> seats = List.of(
                createSeat(801L, 8L, "D", 1, "STANDARD", false),
                createSeat(802L, 8L, "D", 2, "STANDARD", false),
                createSeat(803L, 8L, "E", 1, "STANDARD", false),
                createSeat(804L, 8L, "E", 2, "STANDARD", false)
        );

        List<ShowSeat> showSeats = seats.stream()
                .map(s -> new ShowSeat(showId, s.getSeatId(), "AVAILABLE"))
                .toList();

        when(showSeatRepository.findByIdShowId(showId)).thenReturn(showSeats);
        when(seatRepository.findAllById(anyList())).thenReturn(seats);

        // Request party size 4 (no single row has 4 seats)
        SeatRecommendationResponse resp = recommendationEngine.recommend(showId, new SeatRecommendationRequest(4, "BEST_VIEW"));

        assertNotNull(resp);
        assertFalse(resp.getRecommendations().isEmpty(), "System must return split group fallback rather than failing");
        RecommendedBlock rec = resp.getRecommendations().get(0);
        assertEquals(4, rec.getSeatIds().size());
        assertTrue(rec.getRationale().contains("Paired") || rec.getRationale().contains("Clustered"),
                "Rationale should clearly explain the split group trade-off");
    }
}
