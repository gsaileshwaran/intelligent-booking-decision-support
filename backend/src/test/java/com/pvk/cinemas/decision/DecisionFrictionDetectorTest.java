package com.pvk.cinemas.decision;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.decision.dto.FrictionAlert;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.decision.engine.DecisionFrictionDetector;
import com.pvk.cinemas.decision.engine.SeatScoringEngine;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DecisionFrictionDetectorTest {

    private SeatRepository seatRepository;
    private ShowSeatRepository showSeatRepository;
    private SeatScoringEngine seatScoringEngine;
    private DecisionFrictionDetector detector;

    @BeforeEach
    void setUp() {
        seatRepository = Mockito.mock(SeatRepository.class);
        showSeatRepository = Mockito.mock(ShowSeatRepository.class);
        seatScoringEngine = Mockito.mock(SeatScoringEngine.class);
        detector = new DecisionFrictionDetector(seatRepository, showSeatRepository, seatScoringEngine);

        SeatScoreDTO goodScore = new SeatScoreDTO();
        goodScore.setScore(85);
        when(seatScoringEngine.scoreSeat(any())).thenReturn(goodScore);
    }

    private Seat createSeat(Long seatId, Long screenId, String row, String num) {
        Seat seat = new Seat(screenId, 1L, row, num);
        seat.setSeatId(seatId);
        return seat;
    }

    @Test
    @DisplayName("Detect SPLIT_GROUP when party selects seats in different rows")
    void testDetectSplitGroupAcrossRows() {
        Long showId = 2001L;
        Seat seat1 = createSeat(1L, 1L, "C", "5");
        Seat seat2 = createSeat(2L, 1L, "D", "5");

        when(seatRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));
        when(showSeatRepository.findByIdShowId(showId)).thenReturn(List.of(
                new ShowSeat(showId, 1L, "AVAILABLE"),
                new ShowSeat(showId, 2L, "AVAILABLE")
        ));

        List<FrictionAlert> alerts = detector.detectFriction(showId, List.of(1L, 2L));

        assertNotNull(alerts);
        assertTrue(alerts.stream().anyMatch(a -> "SPLIT_GROUP".equals(a.getType())));
        assertTrue(alerts.stream().anyMatch(a -> a.getMessage().contains("different rows") || a.getMessage().contains("split across rows")));
    }

    @Test
    @DisplayName("Detect SUBOPTIMAL_VIEW when seat score is below 65 (steep front/edge)")
    void testDetectSuboptimalView() {
        Long showId = 2001L;
        Seat seat = createSeat(10L, 1L, "A", "1");

        SeatScoreDTO lowScore = new SeatScoreDTO();
        lowScore.setSeatId(10L);
        lowScore.setScore(52); // steep front
        when(seatScoringEngine.scoreSeat(seat)).thenReturn(lowScore);

        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
        when(showSeatRepository.findByIdShowId(showId)).thenReturn(List.of(new ShowSeat(showId, 10L, "AVAILABLE")));

        List<FrictionAlert> alerts = detector.detectFriction(showId, List.of(10L));

        assertNotNull(alerts);
        assertTrue(alerts.stream().anyMatch(a -> "SUBOPTIMAL_VIEW".equals(a.getType())));
    }
}
