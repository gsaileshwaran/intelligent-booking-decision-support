package com.booking.intelligent.dto;

import com.booking.intelligent.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String bookingRef;
    private Long userId;
    private Long showId;
    private String movieTitle;
    private String theatreName;
    private String screenName;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime holdExpiresAt;
    private List<SeatDetailDto> seats;

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
