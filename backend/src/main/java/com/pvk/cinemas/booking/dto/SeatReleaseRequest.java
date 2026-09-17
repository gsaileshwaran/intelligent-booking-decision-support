package com.pvk.cinemas.booking.dto;

import jakarta.validation.constraints.NotBlank;

public class SeatReleaseRequest {

    @NotBlank(message = "Hold token must not be blank")
    private String holdToken;

    public SeatReleaseRequest() {}

    public SeatReleaseRequest(String holdToken) {
        this.holdToken = holdToken;
    }

    public String getHoldToken() { return holdToken; }
    public void setHoldToken(String holdToken) { this.holdToken = holdToken; }
}
