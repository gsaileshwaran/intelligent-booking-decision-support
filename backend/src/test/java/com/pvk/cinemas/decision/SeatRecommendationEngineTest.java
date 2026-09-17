package com.pvk.cinemas.decision;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.SeatRecommendationRequest;
import com.pvk.cinemas.decision.dto.SeatRecommendationResponse;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.decision.engine.SeatRecommendationEngine;
import com.pvk.cinemas.decision.engine.SeatScoringEngine;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class SeatRecommendationEngineTest {

    private ShowSeatRepository showSeatRepository;
    private SeatRepository seatRepository;
    private SeatScoringEngine seatScoringEngine;
    private com.pvk.cinemas.booking.service.SeatPricingService seatPricingService;
    private SeatRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        showSeatRepository = Mockito.mock(ShowSeatRepository.class);
        seatRepository = Mockito.mock(SeatRepository.class);
        seatScoringEngine = Mockito.mock(SeatScoringEngine.class);
        seatPricingService = Mockito.mock(com.pvk.cinemas.booking.service.SeatPricingService.class);
        engine = new SeatRecommendationEngine(showSeatRepository, seatRepository, seatScoringEngine, seatPricingService);
    }

    private Seat createSeat(Long seatId, Long screenId, String row, String num) {
        Seat seat = new Seat(screenId, 1L, row, num);
        seat.setSeatId(seatId);
        return seat;
    }

    @Test
    @DisplayName("Recommend contiguous pair of seats in Row C with highest average score")
    void testContiguousPairRecommendation() {
        Long showId = 1001L;

        // 10 seats in Row C: C1 to C10
        List<ShowSeat> showSeats = new ArrayList<>();
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Long seatId = 200L + i;
            String status = (i == 3) ? "BOOKED" : "AVAILABLE"; // C3 is booked
            showSeats.add(new ShowSeat(showId, seatId, status));
            seats.add(createSeat(seatId, 1L, "C", String.valueOf(i)));

            SeatScoreDTO score = new SeatScoreDTO();
            score.setSeatId(seatId);
            score.setRowLabel("C");
            score.setSeatNumber(String.valueOf(i));
            score.setScore(i == 5 || i == 6 ? 95 : 75);
            score.setPrice(BigDecimal.valueOf(150.00));
            when(seatScoringEngine.scoreSeat(seats.get(i - 1))).thenReturn(score);
        }

        when(showSeatRepository.findByIdShowId(showId)).thenReturn(showSeats);
        when(seatRepository.findAllById(anyList())).thenReturn(seats);

        SeatRecommendationResponse response = engine.recommend(showId, new SeatRecommendationRequest(2, "BEST_VIEW"));

        assertNotNull(response);
        assertFalse(response.getRecommendations().isEmpty());

        RecommendedBlock topBlock = response.getRecommendations().get(0);
        assertEquals(2, topBlock.getSeatIds().size());
        assertEquals(List.of("C5", "C6"), topBlock.getSeatLabels());
        assertEquals(95, topBlock.getAverageScore());

        // Ensure booked seat C3 is never included in any candidate block
        for (RecommendedBlock b : response.getRecommendations()) {
            assertFalse(b.getSeatLabels().contains("C3"), "Booked seat C3 must not be recommended");
        }
    }
}
