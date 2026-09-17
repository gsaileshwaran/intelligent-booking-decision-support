package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    @Query("SELECT g FROM Genre g WHERE g.name = :genreName")
    Optional<Genre> findByGenreName(@Param("genreName") String genreName);

    Optional<Genre> findByName(String name);
    Optional<Genre> findByGenreCode(String genreCode);

    default Optional<Genre> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
