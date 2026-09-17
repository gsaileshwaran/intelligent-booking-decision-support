package com.pvk.cinemas.decision;

import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class SeatScoringEngineTest {

    private SeatRepository seatRepository;
    private SeatTypeRepository seatTypeRepository;
    private SeatHoldService seatHoldService;
    private SeatScoringEngine engine;

    @BeforeEach
    void setUp() {
        seatRepository = Mockito.mock(SeatRepository.class);
        seatTypeRepository = Mockito.mock(SeatTypeRepository.class);
        seatHoldService = Mockito.mock(SeatHoldService.class);
        engine = new SeatScoringEngine(seatRepository, seatTypeRepository, seatHoldService);

        when(seatHoldService.determineSeatPrice(anyLong())).thenReturn(BigDecimal.valueOf(150.00));
        when(seatTypeRepository.findById(1L)).thenReturn(Optional.of(new SeatType(1L, "STANDARD", "Standard", "Standard Tier")));
        when(seatTypeRepository.findById(2L)).thenReturn(Optional.of(new SeatType(2L, "RECLINER", "Recliner", "Recliner Tier")));
    }

    private Seat createSeat(Long seatId, Long screenId, Long seatTypeId, String row, String num) {
        Seat seat = new Seat(screenId, seatTypeId, row, num);
        seat.setSeatId(seatId);
        return seat;
    }

    @Test
    @DisplayName("Center seat in preferred viewing area (Row H in 12-row screen) receives EXCELLENT score (>= 90) and Atmos sweet spot")
    void testCenterSeatReceivesOptimalScore() {
        Seat sweetSpotSeat = createSeat(101L, 1L, 1L, "H", "5");

        // Explicitly score in a 12-row x 10-seat auditorium
        SeatScoreDTO score = engine.scoreSeat(sweetSpotSeat, 12, 10);

        assertNotNull(score);
        assertEquals(101L, score.getSeatId());
        assertEquals("H", score.getRowLabel());
        assertEquals("5", score.getSeatNumber());
        assertTrue(score.getScore() >= 90, "Center seat score in sweet spot should be >= 90, got: " + score.getScore());
        assertEquals("EXCELLENT", score.getBadge());
        assertTrue(score.getDistanceFactor() >= 0.95, "Row H should have high distance factor, got: " + score.getDistanceFactor());
        assertTrue(score.getLateralFactor() >= 0.90, "Center lateral factor should be high, got: " + score.getLateralFactor());
        assertEquals(1.00, score.getAcousticFactor());
        assertFalse(score.getReasons().isEmpty());
        assertTrue(score.getReasons().stream().anyMatch(r -> r.contains("optical") || r.contains("sweet spot") || r.contains("Sweet Spot")));
    }

    @Test
    @DisplayName("Front row center seat (A6) has lower score than sweet spot (H6) due to screen proximity")
    void testFrontRowCenterSeatHasLowerScoreThanSweetSpot() {
        Seat frontCenter = createSeat(105L, 1L, 1L, "A", "6");
        Seat midCenter = createSeat(106L, 1L, 1L, "H", "6");

        SeatScoreDTO frontScore = engine.scoreSeat(frontCenter, 12, 10);
        SeatScoreDTO midScore = engine.scoreSeat(midCenter, 12, 10);

        assertNotNull(frontScore);
        assertNotNull(midScore);
        assertTrue(midScore.getScore() > frontScore.getScore(),
                "Mid-auditorium center seat (" + midScore.getScore() + ") must outscore front row center (" + frontScore.getScore() + ")");
        assertTrue(frontScore.getDistanceFactor() <= 0.65, "Front row distance factor must reflect screen proximity penalty");
        assertTrue(frontScore.getScore() <= 75, "Front row center seat score must be reasonable (<= 75), got: " + frontScore.getScore());
    }

    @Test
    @DisplayName("Edge seat in Row A (A1) receives lower score and LESS_SUITABLE/FAIR badge")
    void testEdgeSeatInFrontRowReceivesLowerScore() {
        Seat edgeFrontSeat = createSeat(102L, 1L, 1L, "A", "1");

        SeatScoreDTO score = engine.scoreSeat(edgeFrontSeat, 12, 10);

        assertNotNull(score);
        assertTrue(score.getDistanceFactor() <= 0.60, "Row A distance factor should be <= 0.60, got: " + score.getDistanceFactor());
        assertTrue(score.getLateralFactor() < 0.65, "Edge seat visual angle factor should be < 0.65");
        assertTrue(score.getScore() < 60, "Row A edge seat should be < 60, got: " + score.getScore());
        assertEquals("LESS_SUITABLE", score.getBadge());
    }

    @Test
    @DisplayName("Recliner seat receives comfort multiplier bonus")
    void testReclinerReceivesComfortBonus() {
        Seat standardSeat = createSeat(103L, 1L, 1L, "D", "5");
        Seat reclinerSeat = createSeat(104L, 1L, 2L, "D", "5");

        SeatScoreDTO standardScore = engine.scoreSeat(standardSeat);
        SeatScoreDTO reclinerScore = engine.scoreSeat(reclinerSeat);

        assertTrue(reclinerScore.getScore() >= standardScore.getScore(),
                "Recliner score (" + reclinerScore.getScore() + ") should be >= standard score (" + standardScore.getScore() + ")");
        assertTrue(reclinerScore.getReasons().stream().anyMatch(r -> r.contains("recliner") || r.contains("comfort")));
    }
}
