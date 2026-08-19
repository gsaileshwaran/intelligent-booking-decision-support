package com.booking.intelligent.repository;

import com.booking.intelligent.entity.ShowSeat;
import com.booking.intelligent.enums.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowShowId(Long showId);

    List<ShowSeat> findByShowShowIdAndStatus(Long showId, ShowSeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.showSeatId IN :ids")
    List<ShowSeat> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = 'HELD' AND ss.heldUntil < :now")
    List<ShowSeat> findExpiredHolds(@Param("now") LocalDateTime now);
}
