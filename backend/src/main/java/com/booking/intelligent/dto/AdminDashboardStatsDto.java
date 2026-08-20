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
public class AdminDashboardStatsDto {
    // User Metrics
    private long totalUsers;
    private long totalCustomers;
    private long totalProviders;
    private long activeUsers;
    private long inactiveUsers;

    // Cinema Metrics
    private long totalBranches;
    private long totalScreens;
    private long totalPhysicalSeats;
    private long totalScheduledShows;
    private long activeShows;
    private long cancelledShows;

    // Booking Metrics
    private long totalBookings;
    private long confirmedBookings;
    private long pendingBookings;
    private long heldBookings;
    private long cancelledBookings;
    private long expiredBookings;

    // Revenue Metrics
    private BigDecimal confirmedRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
}
