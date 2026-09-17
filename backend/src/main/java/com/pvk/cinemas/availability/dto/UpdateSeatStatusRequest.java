package com.pvk.cinemas.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UpdateSeatStatusRequest {
    @NotEmpty(message = "Seat IDs cannot be empty")
    private List<Long> seatIds;

    @NotBlank(message = "Availability status is required")
    private String availabilityStatus; // AVAILABLE, BLOCKED

    public UpdateSeatStatusRequest() {}

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
}
