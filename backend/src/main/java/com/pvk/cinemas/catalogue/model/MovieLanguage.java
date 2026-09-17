package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "movie_language", uniqueConstraints = {
    @UniqueConstraint(name = "uq_movie_language", columnNames = {"movie_id", "language_id", "language_role"})
})
public class MovieLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_language_id")
    private Long movieLanguageId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    @Column(name = "language_role", nullable = false, length = 20)
    private String languageRole = "ORIGINAL";

    public MovieLanguage() {}

    public MovieLanguage(Long movieId, Long languageId, String languageRole) {
        this.movieId = movieId;
        this.languageId = languageId;
        this.languageRole = languageRole;
    }

    public MovieLanguage(Long movieId, Integer languageId, String languageRole) {
        this.movieId = movieId;
        this.languageId = languageId != null ? Long.valueOf(languageId) : null;
        this.languageRole = languageRole;
    }

    public Long getMovieLanguageId() { return movieLanguageId; }
    public void setMovieLanguageId(Long movieLanguageId) { this.movieLanguageId = movieLanguageId; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getLanguageId() { return languageId; }
    public void setLanguageId(Long languageId) { this.languageId = languageId; }
    public void setLanguageId(Integer languageId) { this.languageId = languageId != null ? Long.valueOf(languageId) : null; }

    public String getLanguageRole() { return languageRole; }
    public void setLanguageRole(String languageRole) { this.languageRole = languageRole; }
}
