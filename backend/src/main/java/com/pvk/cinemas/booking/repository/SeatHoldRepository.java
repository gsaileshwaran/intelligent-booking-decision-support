package com.pvk.cinemas.booking.repository;

import com.pvk.cinemas.booking.model.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    List<SeatHold> findByHoldToken(String holdToken);

    List<SeatHold> findByHoldTokenAndStatus(String holdToken, String status);

    List<SeatHold> findByShowIdAndStatus(Long showId, String status);

    List<SeatHold> findByStatusAndExpiresAtBefore(String status, Instant threshold);

    Optional<SeatHold> findByShowIdAndSeatIdAndStatus(Long showId, Long seatId, String status);

    @Query("SELECT sh FROM SeatHold sh WHERE sh.showId = :showId AND sh.seatId IN :seatIds AND sh.status = 'ACTIVE' AND sh.expiresAt > :now")
    List<SeatHold> findActiveHoldsForShowAndSeats(@Param("showId") Long showId,
                                                 @Param("seatIds") Collection<Long> seatIds,
                                                 @Param("now") Instant now);
}
