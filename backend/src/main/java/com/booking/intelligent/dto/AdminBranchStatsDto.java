package com.booking.intelligent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminBranchStatsDto {
    private Long theatreId;
    private String name;
    private String location;
    private String address;
    private int screenCount;
    private int totalSeatingCapacity;
    private long scheduledShowsCount;
    private long confirmedBookingsCount;
    private BigDecimal confirmedRevenue;
}
