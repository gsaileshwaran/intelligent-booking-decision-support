package com.pvk.cinemas.decision.dto;

import java.util.ArrayList;
import java.util.List;

public class ShowRecommendationResponse {

    private Long movieId;
    private String movieTitle;
    private Long cityId;
    private String cityName;
    private ShowCandidateDTO preferredOption;
    private List<ShowCandidateDTO> candidates = new ArrayList<>();
    private String conflictAnalysis;

    public ShowRecommendationResponse() {}

    public ShowRecommendationResponse(Long movieId, String movieTitle, Long cityId, String cityName,
                                      ShowCandidateDTO preferredOption, List<ShowCandidateDTO> candidates,
                                      String conflictAnalysis) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.cityId = cityId;
        this.cityName = cityName;
        this.preferredOption = preferredOption;
        this.candidates = candidates;
        this.conflictAnalysis = conflictAnalysis;
    }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public ShowCandidateDTO getPreferredOption() { return preferredOption; }
    public void setPreferredOption(ShowCandidateDTO preferredOption) { this.preferredOption = preferredOption; }

    public List<ShowCandidateDTO> getCandidates() { return candidates; }
    public void setCandidates(List<ShowCandidateDTO> candidates) { this.candidates = candidates; }

    public List<ShowCandidateDTO> getRankedCandidates() { return candidates; }

    public String getConflictAnalysis() { return conflictAnalysis; }
    public void setConflictAnalysis(String conflictAnalysis) { this.conflictAnalysis = conflictAnalysis; }
}
