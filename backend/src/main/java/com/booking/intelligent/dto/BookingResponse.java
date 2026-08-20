package com.booking.intelligent.dto;

import com.booking.intelligent.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String bookingRef;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private Long showId;
    private String movieTitle;
    private Long theatreId;
    private String theatreName;
    private String screenName;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime holdExpiresAt;
    private List<SeatDetailDto> seats;

    // Payment details
    private String paymentMethod;
    private String transactionRef;
    private String paymentStatus;

    // Simulated Refund details
    private String refundRef;
    private BigDecimal refundedAmount;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeatDetailDto {
        private Long showSeatId;
        private String rowLabel;
        private Integer seatNumber;
        private String seatType;
        private BigDecimal price;
    }
}
