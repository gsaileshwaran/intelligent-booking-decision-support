package com.booking.intelligent.dto;

import com.booking.intelligent.enums.ShowSeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShowSeatResponseDto {
    private Long showSeatId;
    private BigDecimal price;
    private ShowSeatStatus status;
    private LocalDateTime heldUntil;
    private Boolean heldByCurrentUser;
    private Long bookingId;
    private SeatDto seat;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeatDto {
        private Long seatId;
        private String rowLabel;
        private Integer seatNumber;
        private String seatType;
    }
}
