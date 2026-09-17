package com.pvk.cinemas.infrastructure.repository;

import com.pvk.cinemas.infrastructure.model.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatTypeRepository extends JpaRepository<SeatType, Long> {
    Optional<SeatType> findByTypeCode(String typeCode);

    default Optional<SeatType> findBySeatTypeCode(String seatTypeCode) {
        return findByTypeCode(seatTypeCode);
    }

    default Optional<SeatType> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
