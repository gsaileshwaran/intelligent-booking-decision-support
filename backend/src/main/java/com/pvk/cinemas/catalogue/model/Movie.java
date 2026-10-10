package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "certification_id", nullable = false)
    private Long certificationId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "runtime_minutes", nullable = false)
    private Short runtimeMinutes;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    @Column(name = "trailer_url", length = 1000)
    private String trailerUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "UPCOMING";

    // -------------------------------------------------------------------------
    // Part 2: Movie Lifecycle & Demand Classification (V10 columns)
    // All nullable until V12 (Part 2 catalogue migration) populates them.
    // -------------------------------------------------------------------------

    @Column(name = "demand_tier", length = 20)
    private String demandTier;

    @Column(name = "popularity_score", precision = 5, scale = 2)
    private BigDecimal popularityScore;

    @Column(name = "exhibition_start_date")
    private LocalDate exhibitionStartDate;

    @Column(name = "exhibition_end_date")
    private LocalDate exhibitionEndDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Movie() {}

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getCertificationId() { return certificationId; }
    public void setCertificationId(Long certificationId) { this.certificationId = certificationId; }
    public void setCertificationId(Integer certificationId) { this.certificationId = certificationId != null ? Long.valueOf(certificationId) : null; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public Integer getRuntimeMinutes() { return runtimeMinutes != null ? runtimeMinutes.intValue() : null; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes != null ? runtimeMinutes.shortValue() : null; }
    public void setRuntimeMinutes(Short runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMovieStatus() { return status; }
    public void setMovieStatus(String movieStatus) { this.status = movieStatus; }

    // Part 2 getters/setters
    public String getDemandTier() { return demandTier; }
    public void setDemandTier(String demandTier) { this.demandTier = demandTier; }

    public BigDecimal getPopularityScore() { return popularityScore; }
    public void setPopularityScore(BigDecimal popularityScore) { this.popularityScore = popularityScore; }

    public LocalDate getExhibitionStartDate() { return exhibitionStartDate; }
    public void setExhibitionStartDate(LocalDate exhibitionStartDate) { this.exhibitionStartDate = exhibitionStartDate; }

    public LocalDate getExhibitionEndDate() { return exhibitionEndDate; }
    public void setExhibitionEndDate(LocalDate exhibitionEndDate) { this.exhibitionEndDate = exhibitionEndDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
