package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.MovieGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, MovieGenre.MovieGenreId> {
    List<MovieGenre> findByIdMovieId(Long movieId);
    void deleteByIdMovieId(Long movieId);
}
