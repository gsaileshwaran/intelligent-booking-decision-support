package com.booking.intelligent.dto;

import com.booking.intelligent.enums.ShowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponseDto {
    private Long showId;
    private Long movieId;
    private String movieTitle;
    private String movieGenre;
    private Integer movieDuration;
    private Long theatreId;
    private String theatreName;
    private Long screenId;
    private String screenName;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal ticketPrice;
    private ShowStatus status;

    // Seat statistics
    private int totalCapacity;
    private int availableSeats;
    private int heldSeats;
    private int confirmedSeats;
    private boolean hasConfirmedBookings;
}
