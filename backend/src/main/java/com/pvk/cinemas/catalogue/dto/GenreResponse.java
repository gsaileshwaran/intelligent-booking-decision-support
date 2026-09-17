package com.pvk.cinemas.catalogue.dto;

public class GenreResponse {
    private Long genreId;
    private String genreName;
    private String description;

    public GenreResponse() {}

    public GenreResponse(Long genreId, String genreName, String description) {
        this.genreId = genreId;
        this.genreName = genreName;
        this.description = description;
    }

    public GenreResponse(Integer genreId, String genreName, String description) {
        this(genreId != null ? genreId.longValue() : null, genreName, description);
    }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }
    public void setGenreId(Integer genreId) { this.genreId = genreId != null ? genreId.longValue() : null; }

    public String getGenreName() { return genreName; }
    public void setGenreName(String genreName) { this.genreName = genreName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
