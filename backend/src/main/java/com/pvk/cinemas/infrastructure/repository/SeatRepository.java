package com.pvk.cinemas.infrastructure.repository;

import com.pvk.cinemas.infrastructure.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s FROM Seat s WHERE s.screenId = :screenId AND s.status = 'ACTIVE' ORDER BY s.rowLabel ASC, s.seatNumber ASC")
    List<Seat> findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(@Param("screenId") Long screenId);
    default List<Seat> findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(Integer screenId) {
        return screenId != null ? findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(screenId.longValue()) : java.util.Collections.emptyList();
    }

    List<Seat> findByScreenIdAndStatusOrderByRowLabelAscSeatNumberAsc(Long screenId, String status);
    default List<Seat> findByScreenIdAndStatusOrderByRowLabelAscSeatNumberAsc(Integer screenId, String status) {
        return screenId != null ? findByScreenIdAndStatusOrderByRowLabelAscSeatNumberAsc(screenId.longValue(), status) : java.util.Collections.emptyList();
    }

    List<Seat> findByScreenId(Long screenId);
    default List<Seat> findByScreenId(Integer screenId) {
        return screenId != null ? findByScreenId(screenId.longValue()) : java.util.Collections.emptyList();
    }

    Optional<Seat> findByScreenIdAndRowLabelAndSeatNumber(Long screenId, String rowLabel, String seatNumber);
    default Optional<Seat> findByScreenIdAndRowLabelAndSeatNumber(Integer screenId, String rowLabel, String seatNumber) {
        return screenId != null ? findByScreenIdAndRowLabelAndSeatNumber(screenId.longValue(), rowLabel, seatNumber) : Optional.empty();
    }

    default Optional<Seat> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
