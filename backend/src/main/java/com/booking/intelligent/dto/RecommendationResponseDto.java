package com.booking.intelligent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponseDto {
    private String engineVersion;
    private Integer totalEvaluated;
    private List<RankedResult> rankedResults;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RankedResult {
        private Integer rank;
        private Long showId;
        private String movieTitle;
        private String theatreName;
        private String showDate;
        private String startTime;
        private BigDecimal price;
        private Integer availableSeats;
        private BigDecimal suitabilityScore;
        private List<String> explanationFactors;
        private boolean isBestMatch;
        private String alternativeNotice;
    }
}
