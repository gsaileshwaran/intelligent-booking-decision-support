package com.pvk.cinemas.decision.dto;

import java.util.List;

public class SeatRecommendationRequest {

    private int partySize = 2;
    private String preference = "BEST_VIEW"; // BEST_VIEW, BUDGET, BALANCED, RECLINER

    public SeatRecommendationRequest() {}

    public SeatRecommendationRequest(int partySize, String preference) {
        this.partySize = partySize;
        this.preference = preference;
    }

    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }

    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
}
