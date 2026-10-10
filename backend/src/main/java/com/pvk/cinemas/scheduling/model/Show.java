package com.pvk.cinemas.scheduling.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Table(name = "`show`")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Long showId;

    @Column(name = "movie_language_id", nullable = false)
    private Long movieLanguageId;

    // -------------------------------------------------------------------------
    // Part 3: Physical Screen Identity (V10 columns)
    // screen_id: direct FK to SCREEN — the authoritative scheduling resource.
    // show_date: exhibition date in Asia/Kolkata (IST). Stored as DATE.
    //   A show starting 23:30 IST on Friday with end 01:45 IST Saturday
    //   belongs to Friday's schedule: show_date = Friday.
    // -------------------------------------------------------------------------

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

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
        if (startAt != null) {
            this.showDate = startAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        }
    }

    public Show(Long movieLanguageId, Long screenId, Long screenCapabilityId, Instant startAt, Instant endAt, String status) {
        this(movieLanguageId, screenCapabilityId, startAt, endAt, status);
        this.screenId = screenId;
    }

    public Show(Long movieLanguageId, Integer screenCapabilityId, Instant startAt, Instant endAt, String status) {
        this(movieLanguageId, screenCapabilityId != null ? screenCapabilityId.longValue() : null, startAt, endAt, status);
    }

    @PrePersist
    @PreUpdate
    private void syncDerivedFields() {
        if (showDate == null && startAt != null) {
            this.showDate = startAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        }
    }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public Long getMovieLanguageId() { return movieLanguageId; }
    public void setMovieLanguageId(Long movieLanguageId) { this.movieLanguageId = movieLanguageId; }

    public Long getScreenCapabilityId() { return screenCapabilityId; }
    public void setScreenCapabilityId(Long screenCapabilityId) { this.screenCapabilityId = screenCapabilityId; }
    public void setScreenCapabilityId(Integer screenCapabilityId) { this.screenCapabilityId = screenCapabilityId != null ? screenCapabilityId.longValue() : null; }

    // Part 3: physical screen identity getters/setters
    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? screenId.longValue() : null; }

    // showDate: persisted column (IST). Also derivable from startAt for backward-compat.
    public LocalDate getShowDate() {
        if (showDate != null) return showDate;
        if (startAt == null) return null;
        return startAt.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
    }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

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

}

