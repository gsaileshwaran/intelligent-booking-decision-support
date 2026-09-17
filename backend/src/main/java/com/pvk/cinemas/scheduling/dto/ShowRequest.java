package com.pvk.cinemas.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class ShowRequest {
    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @NotNull(message = "Movie Language ID is required")
    private Long movieLanguageId;

    @NotNull(message = "Screen ID is required")
    private Integer screenId;

    @NotNull(message = "Screen Capability ID is required")
    private Integer screenCapabilityId;

    @NotNull(message = "Start time is required")
    private Instant startAt;

    @NotNull(message = "End time is required")
    private Instant endAt;

    private String showStatus = "SCHEDULED";

    public ShowRequest() {}

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getMovieLanguageId() { return movieLanguageId; }
    public void setMovieLanguageId(Long movieLanguageId) { this.movieLanguageId = movieLanguageId; }

    public Integer getScreenId() { return screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId; }

    public Integer getScreenCapabilityId() { return screenCapabilityId; }
    public void setScreenCapabilityId(Integer screenCapabilityId) { this.screenCapabilityId = screenCapabilityId; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public String getShowStatus() { return showStatus; }
    public void setShowStatus(String showStatus) { this.showStatus = showStatus; }
}
