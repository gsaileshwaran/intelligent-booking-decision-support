package com.booking.intelligent.repository;

import com.booking.intelligent.entity.MovieFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieFavoriteRepository extends JpaRepository<MovieFavorite, Long> {
    List<MovieFavorite> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MovieFavorite> findByUserUserIdAndMovieMovieId(Long userId, Long movieId);
    void deleteByUserUserIdAndMovieMovieId(Long userId, Long movieId);
    boolean existsByUserUserIdAndMovieMovieId(Long userId, Long movieId);
}
