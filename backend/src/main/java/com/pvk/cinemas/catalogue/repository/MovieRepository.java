package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("SELECT m FROM Movie m WHERE m.status = :status")
    List<Movie> findByMovieStatus(@Param("status") String movieStatus);

    List<Movie> findByStatus(String status);
    Page<Movie> findAll(Pageable pageable);

    @Query(value = """
        SELECT DISTINCT m.* FROM movie m
        JOIN movie_language ml ON ml.movie_id = m.movie_id
        JOIN `show` s ON s.movie_language_id = ml.movie_language_id
        JOIN screen_capability scap ON scap.screen_capability_id = s.screen_capability_id
        JOIN screen sc ON sc.screen_id = scap.screen_id
        JOIN theatre t ON t.theatre_id = sc.theatre_id
        WHERE t.city_id = :cityId
          AND (:status IS NULL OR m.status = :status)
          AND s.status = 'SCHEDULED'
        ORDER BY m.movie_id ASC
        """, nativeQuery = true)
    List<Movie> findMoviesByCityAndStatus(@Param("cityId") Integer cityId, @Param("status") String status);
}
