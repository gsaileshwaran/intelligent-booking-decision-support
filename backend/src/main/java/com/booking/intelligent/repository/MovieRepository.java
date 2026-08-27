package com.booking.intelligent.repository;

import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.enums.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByStatus(MovieStatus status);
    Optional<Movie> findByTitle(String title);
    List<Movie> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT m FROM Movie m WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.cast) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.director) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.genre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.language) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Movie> searchMovies(@Param("query") String query);
}
