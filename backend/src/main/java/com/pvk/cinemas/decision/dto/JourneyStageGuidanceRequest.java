package com.pvk.cinemas.decision.dto;

public class JourneyStageGuidanceRequest {

    private String currentStage; // "CITY_DISCOVERY", "MOVIE_SELECTION", "SHOWTIME_SELECTION", "SEAT_SELECTION", "SEAT_HOLD", "BOOKED"
    private Long cityId;
    private String cityName;
    private Long movieId;
    private String movieTitle;
    private Long showId;
    private int selectedSeatCount = 0;
    private int holdSecondsRemaining = 0;
    private String detectedFrictionType;

    public JourneyStageGuidanceRequest() {}

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public int getSelectedSeatCount() { return selectedSeatCount; }
    public void setSelectedSeatCount(int selectedSeatCount) { this.selectedSeatCount = selectedSeatCount; }

    public int getHoldSecondsRemaining() { return holdSecondsRemaining; }
    public void setHoldSecondsRemaining(int holdSecondsRemaining) { this.holdSecondsRemaining = holdSecondsRemaining; }

    public String getDetectedFrictionType() { return detectedFrictionType; }
    public void setDetectedFrictionType(String detectedFrictionType) { this.detectedFrictionType = detectedFrictionType; }
}
