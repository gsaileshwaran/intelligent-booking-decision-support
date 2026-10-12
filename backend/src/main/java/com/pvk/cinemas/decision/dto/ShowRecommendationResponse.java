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

    // Diagnostic evaluation counts
    private int totalShowsEvaluated;
    private int eligibleCandidatesCount;
    private int excludedCity;
    private int excludedDate;
    private int excludedLanguage;
    private int excludedFormat;
    private int excludedTime;
    private int excludedSeats;
    private int excludedBudget;

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
        this.eligibleCandidatesCount = candidates != null ? candidates.size() : 0;
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

    public int getTotalShowsEvaluated() { return totalShowsEvaluated; }
    public void setTotalShowsEvaluated(int totalShowsEvaluated) { this.totalShowsEvaluated = totalShowsEvaluated; }

    public int getEligibleCandidatesCount() { return eligibleCandidatesCount; }
    public void setEligibleCandidatesCount(int eligibleCandidatesCount) { this.eligibleCandidatesCount = eligibleCandidatesCount; }

    public int getExcludedCity() { return excludedCity; }
    public void setExcludedCity(int excludedCity) { this.excludedCity = excludedCity; }

    public int getExcludedDate() { return excludedDate; }
    public void setExcludedDate(int excludedDate) { this.excludedDate = excludedDate; }

    public int getExcludedLanguage() { return excludedLanguage; }
    public void setExcludedLanguage(int excludedLanguage) { this.excludedLanguage = excludedLanguage; }

    public int getExcludedFormat() { return excludedFormat; }
    public void setExcludedFormat(int excludedFormat) { this.excludedFormat = excludedFormat; }

    public int getExcludedTime() { return excludedTime; }
    public void setExcludedTime(int excludedTime) { this.excludedTime = excludedTime; }

    public int getExcludedSeats() { return excludedSeats; }
    public void setExcludedSeats(int excludedSeats) { this.excludedSeats = excludedSeats; }

    public int getExcludedBudget() { return excludedBudget; }
    public void setExcludedBudget(int excludedBudget) { this.excludedBudget = excludedBudget; }
}
