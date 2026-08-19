package com.booking.intelligent.service;

import com.booking.intelligent.dto.RecommendationRequestDto;
import com.booking.intelligent.dto.RecommendationResponseDto;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.ShowSeat;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.repository.ShowRepository;
import com.booking.intelligent.repository.ShowSeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    public RecommendationResponseDto evaluateRecommendations(RecommendationRequestDto request) {
        List<Show> candidateShows;
        if (request.getMovieId() != null) {
            candidateShows = showRepository.findByMovieMovieIdAndStatus(request.getMovieId(), ShowStatus.ACTIVE);
        } else {
            candidateShows = showRepository.findAll().stream()
                    .filter(s -> s.getStatus() == ShowStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        List<RecommendationResponseDto.RankedResult> rankedResults = new ArrayList<>();

        for (Show show : candidateShows) {
            List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(show.getShowId());
            long availableCount = showSeats.stream()
                    .filter(ss -> ss.getStatus() == ShowSeatStatus.AVAILABLE)
                    .count();

            int requiredGroup = request.getGroupSize() != null ? request.getGroupSize() : 1;

            // 1. Hard Constraint Filter: Group Size Availability
            if (availableCount < requiredGroup) {
                continue;
            }

            // Hard Constraint Filter: Maximum Budget
            if (request.getMaxBudget() != null && show.getTicketPrice().compareTo(request.getMaxBudget()) > 0) {
                continue;
            }

            // 2. Multi-Criteria Scoring (MCDM) Formula: S_i = Sum(w_j * x_ij)
            double priceScore = 1.0;
            if (request.getMaxBudget() != null && request.getMaxBudget().compareTo(BigDecimal.ZERO) > 0) {
                priceScore = Math.max(0.0, 1.0 - (show.getTicketPrice().doubleValue() / request.getMaxBudget().doubleValue()));
            }

            double timeScore = 0.85; // Base timing score
            double seatScore = 0.90; // Preferred seat availability score

            double compositeScore = (0.40 * priceScore) + (0.35 * timeScore) + (0.25 * seatScore);
            double suitabilityPercentage = Math.round(compositeScore * 1000.0) / 10.0;

            List<String> factors = new ArrayList<>();
            factors.add("Within max budget limit (" + show.getTicketPrice() + ")");
            factors.add("Available seats (" + availableCount + " open)");
            factors.add("Auditorium: " + show.getScreen().getName());

            RecommendationResponseDto.RankedResult result = RecommendationResponseDto.RankedResult.builder()
                    .showId(show.getShowId())
                    .movieTitle(show.getMovie().getTitle())
                    .theatreName(show.getScreen().getTheatre().getName())
                    .showDate(show.getShowDate().toString())
                    .startTime(show.getStartTime().toString())
                    .price(show.getTicketPrice())
                    .availableSeats((int) availableCount)
                    .suitabilityScore(BigDecimal.valueOf(suitabilityPercentage))
                    .explanationFactors(factors)
                    .isBestMatch(false)
                    .build();

            rankedResults.add(result);
        }

        // Rank descending by suitability score
        rankedResults.sort(Comparator.comparing(RecommendationResponseDto.RankedResult::getSuitabilityScore).reversed());

        for (int i = 0; i < rankedResults.size(); i++) {
            rankedResults.get(i).setRank(i + 1);
            if (i == 0) {
                rankedResults.get(i).setBestMatch(true);
            }
        }

        return RecommendationResponseDto.builder()
                .engineVersion("v1.0-local-mcdm-fallback")
                .totalEvaluated(candidateShows.size())
                .rankedResults(rankedResults)
                .build();
    }
}
