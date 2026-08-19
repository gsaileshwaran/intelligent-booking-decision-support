package com.booking.intelligent.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecommendationRequestDto {
    private Long movieId;
    private BigDecimal maxBudget;
    private Integer groupSize = 1;
    private String preferredTime;
    private String preferredSeatType;
}
