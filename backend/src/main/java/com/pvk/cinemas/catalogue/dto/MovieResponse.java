package com.pvk.cinemas.catalogue.dto;

import java.time.LocalDate;
import java.util.List;

public class MovieResponse {
    private Long movieId;
    private Long certificationId;
    private String certificationCode;
    private String title;
    private String synopsis;
    private Integer runtimeMinutes;
    private LocalDate releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private String movieStatus;
    private List<String> genres;
    private List<String> languages;

    public MovieResponse() {}

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getCertificationId() { return certificationId; }
    public void setCertificationId(Long certificationId) { this.certificationId = certificationId; }
    public void setCertificationId(Integer certificationId) { this.certificationId = certificationId != null ? certificationId.longValue() : null; }

    public String getCertificationCode() { return certificationCode; }
    public void setCertificationCode(String certificationCode) { this.certificationCode = certificationCode; }

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

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
}
