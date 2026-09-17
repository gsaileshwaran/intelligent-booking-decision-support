package com.pvk.cinemas.decision.dto;

import java.util.List;

public class DecisionGuidanceRequest {

    private List<Long> seatIds;

    public DecisionGuidanceRequest() {}

    public DecisionGuidanceRequest(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }
}
