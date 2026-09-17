package com.pvk.cinemas.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SeatHoldRequest {

    @NotEmpty(message = "At least one seat must be selected to hold")
    private List<Long> seatIds;

    public SeatHoldRequest() {}

    public SeatHoldRequest(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }
}
