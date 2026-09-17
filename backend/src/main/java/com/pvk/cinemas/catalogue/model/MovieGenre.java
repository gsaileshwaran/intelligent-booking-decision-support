package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "movie_genre")
public class MovieGenre {

    @EmbeddedId
    private MovieGenreId id;

    public MovieGenre() {}

    public MovieGenre(Long movieId, Long genreId) {
        this.id = new MovieGenreId(movieId, genreId);
    }

    public MovieGenre(Long movieId, Integer genreId) {
        this.id = new MovieGenreId(movieId, genreId != null ? Long.valueOf(genreId) : null);
    }

    public MovieGenreId getId() { return id; }
    public void setId(MovieGenreId id) { this.id = id; }

    @Embeddable
    public static class MovieGenreId implements Serializable {
        @Column(name = "movie_id")
        private Long movieId;

        @Column(name = "genre_id")
        private Long genreId;

        public MovieGenreId() {}
        public MovieGenreId(Long movieId, Long genreId) {
            this.movieId = movieId;
            this.genreId = genreId;
        }

        public MovieGenreId(Long movieId, Integer genreId) {
            this.movieId = movieId;
            this.genreId = genreId != null ? Long.valueOf(genreId) : null;
        }

        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }

        public Long getGenreId() { return genreId; }
        public void setGenreId(Long genreId) { this.genreId = genreId; }
        public void setGenreId(Integer genreId) { this.genreId = genreId != null ? Long.valueOf(genreId) : null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MovieGenreId that)) return false;
            return Objects.equals(movieId, that.movieId) && Objects.equals(genreId, that.genreId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(movieId, genreId);
        }
    }
}
