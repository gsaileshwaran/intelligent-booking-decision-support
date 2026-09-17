package com.pvk.cinemas.decision.dto;

import java.util.List;

public class SeatRecommendationResponse {

    private Long showId;
    private int partySize;
    private String preference;
    private List<RecommendedBlock> recommendations;

    public SeatRecommendationResponse() {}

    public SeatRecommendationResponse(Long showId, int partySize, String preference, List<RecommendedBlock> recommendations) {
        this.showId = showId;
        this.partySize = partySize;
        this.preference = preference;
        this.recommendations = recommendations;
    }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }

    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }

    public List<RecommendedBlock> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendedBlock> recommendations) { this.recommendations = recommendations; }
}
