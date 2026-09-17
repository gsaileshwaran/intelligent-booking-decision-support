package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "genre")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long genreId;

    @Column(name = "genre_code", nullable = false, unique = true, length = 50)
    private String genreCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public Genre() {}

    public Genre(Long genreId, String genreCode, String name) {
        this.genreId = genreId;
        this.genreCode = genreCode;
        this.name = name;
    }

    public Genre(Integer genreId, String genreCode, String name) {
        this.genreId = genreId != null ? Long.valueOf(genreId) : null;
        this.genreCode = genreCode;
        this.name = name;
    }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }
    public void setGenreId(Integer genreId) { this.genreId = genreId != null ? Long.valueOf(genreId) : null; }

    public String getGenreCode() { return genreCode; }
    public void setGenreCode(String genreCode) { this.genreCode = genreCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenreName() { return name; }
    public void setGenreName(String genreName) { this.name = genreName; }

    public String getDescription() { return null; }
    public void setDescription(String description) { /* no-op: not present in schema V1 */ }
}
