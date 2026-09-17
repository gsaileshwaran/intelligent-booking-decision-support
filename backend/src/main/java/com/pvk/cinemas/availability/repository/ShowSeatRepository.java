package com.pvk.cinemas.availability.repository;

import com.pvk.cinemas.availability.model.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, ShowSeat.ShowSeatId> {
    List<ShowSeat> findByIdShowId(Long showId);
    Optional<ShowSeat> findByIdShowIdAndIdSeatId(Long showId, Long seatId);
    long countByIdShowIdAndAvailabilityStatus(Long showId, String availabilityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.id.showId = :showId AND ss.id.seatId IN :seatIds")
    List<ShowSeat> findByIdShowIdAndIdSeatIdInForUpdate(@Param("showId") Long showId, @Param("seatIds") Collection<Long> seatIds);
}
