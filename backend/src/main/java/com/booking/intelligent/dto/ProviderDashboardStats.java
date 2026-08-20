package com.booking.intelligent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDashboardStats {
    private long totalBranches;
    private long totalScreens;
    private long totalSeats;
    private long todaysShows;
    private long upcomingShows;
    private long totalBookings;
    private long confirmedBookings;
    private BigDecimal totalRevenue;
}
