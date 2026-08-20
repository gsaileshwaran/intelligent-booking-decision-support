package com.booking.intelligent.repository;

import com.booking.intelligent.entity.Show;
import com.booking.intelligent.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieMovieIdAndStatus(Long movieId, ShowStatus status);
    List<Show> findByMovieMovieIdAndShowDate(Long movieId, LocalDate showDate);
    List<Show> findByScreenTheatreTheatreId(Long theatreId);
    List<Show> findByScreenTheatreOwnerUserUserId(Long ownerUserId);
    List<Show> findByScreenScreenIdAndShowDateAndStatusNot(Long screenId, LocalDate showDate, ShowStatus status);
    long countByStatus(ShowStatus status);
}
