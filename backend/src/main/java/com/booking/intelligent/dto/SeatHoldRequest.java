package com.booking.intelligent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldRequest {
    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotEmpty(message = "At least one show seat ID must be selected")
    private List<Long> showSeatIds;
}
