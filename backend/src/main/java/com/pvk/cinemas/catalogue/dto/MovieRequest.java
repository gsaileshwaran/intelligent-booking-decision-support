package com.pvk.cinemas.catalogue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class MovieRequest {
    @NotNull(message = "Certification ID is required")
    private Integer certificationId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    private String synopsis;

    @NotNull(message = "Runtime minutes is required")
    @Min(value = 1, message = "Runtime must be at least 1 minute")
    private Integer runtimeMinutes;

    private LocalDate releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private String movieStatus;

    private List<Integer> genreIds;
    private List<Integer> languageIds;

    @Size(max = 20)
    private String demandTier;

    @jakarta.validation.constraints.DecimalMin(value = "0.00", message = "Popularity score must be >= 0.00")
    @jakarta.validation.constraints.DecimalMax(value = "10.00", message = "Popularity score must be <= 10.00")
    private java.math.BigDecimal popularityScore;

    private LocalDate exhibitionStartDate;
    private LocalDate exhibitionEndDate;

    public MovieRequest() {}

    public Integer getCertificationId() { return certificationId; }
    public void setCertificationId(Integer certificationId) { this.certificationId = certificationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public String getMovieStatus() { return movieStatus; }
    public void setMovieStatus(String movieStatus) { this.movieStatus = movieStatus; }

    public List<Integer> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }

    public List<Integer> getLanguageIds() { return languageIds; }
    public void setLanguageIds(List<Integer> languageIds) { this.languageIds = languageIds; }

    public String getDemandTier() { return demandTier; }
    public void setDemandTier(String demandTier) { this.demandTier = demandTier; }

    public java.math.BigDecimal getPopularityScore() { return popularityScore; }
    public void setPopularityScore(java.math.BigDecimal popularityScore) { this.popularityScore = popularityScore; }

    public LocalDate getExhibitionStartDate() { return exhibitionStartDate; }
    public void setExhibitionStartDate(LocalDate exhibitionStartDate) { this.exhibitionStartDate = exhibitionStartDate; }

    public LocalDate getExhibitionEndDate() { return exhibitionEndDate; }
    public void setExhibitionEndDate(LocalDate exhibitionEndDate) { this.exhibitionEndDate = exhibitionEndDate; }
}
