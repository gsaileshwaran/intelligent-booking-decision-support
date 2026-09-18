package com.pvk.cinemas.scheduling.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "`show`")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Long showId;

    @Column(name = "movie_language_id", nullable = false)
    private Long movieLanguageId;

    @Column(name = "screen_capability_id", nullable = false)
    private Long screenCapabilityId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SCHEDULED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Show() {}

    public Show(Long movieLanguageId, Long screenCapabilityId, Instant startAt, Instant endAt, String status) {
        this.movieLanguageId = movieLanguageId;
        this.screenCapabilityId = screenCapabilityId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Show(Long movieLanguageId, Integer screenCapabilityId, Instant startAt, Instant endAt, String status) {
        this(movieLanguageId, screenCapabilityId != null ? screenCapabilityId.longValue() : null, startAt, endAt, status);
    }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public Long getMovieLanguageId() { return movieLanguageId; }
    public void setMovieLanguageId(Long movieLanguageId) { this.movieLanguageId = movieLanguageId; }

    public Long getScreenCapabilityId() { return screenCapabilityId; }
    public void setScreenCapabilityId(Long screenCapabilityId) { this.screenCapabilityId = screenCapabilityId; }
    public void setScreenCapabilityId(Integer screenCapabilityId) { this.screenCapabilityId = screenCapabilityId != null ? screenCapabilityId.longValue() : null; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShowStatus() { return status; }
    public void setShowStatus(String showStatus) { this.status = showStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public java.time.LocalDate getShowDate() {
        if (startAt == null) return null;
        return startAt.atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
    }
}
