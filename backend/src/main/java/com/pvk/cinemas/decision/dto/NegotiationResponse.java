package com.pvk.cinemas.decision.dto;

import java.util.List;

public class NegotiationResponse {

    private boolean requestedPartyAvailable;
    private int partySize;
    private List<RecommendedBlock> adjacentSplitOptions;
    private List<ShowtimeAlternative> alternativeShowtimes;
    private List<TheatreAlternative> alternativeTheatres;

    public NegotiationResponse() {}

    public NegotiationResponse(boolean requestedPartyAvailable, int partySize, List<RecommendedBlock> adjacentSplitOptions, List<ShowtimeAlternative> alternativeShowtimes, List<TheatreAlternative> alternativeTheatres) {
        this.requestedPartyAvailable = requestedPartyAvailable;
        this.partySize = partySize;
        this.adjacentSplitOptions = adjacentSplitOptions;
        this.alternativeShowtimes = alternativeShowtimes;
        this.alternativeTheatres = alternativeTheatres;
    }

    public boolean isRequestedPartyAvailable() { return requestedPartyAvailable; }
    public void setRequestedPartyAvailable(boolean requestedPartyAvailable) { this.requestedPartyAvailable = requestedPartyAvailable; }

    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }

    public List<RecommendedBlock> getAdjacentSplitOptions() { return adjacentSplitOptions; }
    public void setAdjacentSplitOptions(List<RecommendedBlock> adjacentSplitOptions) { this.adjacentSplitOptions = adjacentSplitOptions; }

    public List<ShowtimeAlternative> getAlternativeShowtimes() { return alternativeShowtimes; }
    public void setAlternativeShowtimes(List<ShowtimeAlternative> alternativeShowtimes) { this.alternativeShowtimes = alternativeShowtimes; }

    public List<TheatreAlternative> getAlternativeTheatres() { return alternativeTheatres; }
    public void setAlternativeTheatres(List<TheatreAlternative> alternativeTheatres) { this.alternativeTheatres = alternativeTheatres; }
}
