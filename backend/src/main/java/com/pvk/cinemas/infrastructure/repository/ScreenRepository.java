package com.pvk.cinemas.infrastructure.repository;

import com.pvk.cinemas.infrastructure.model.Screen;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    @Query("SELECT s FROM Screen s WHERE s.theatreId = :theatreId AND s.status = 'ACTIVE'")
    List<Screen> findByTheatreIdAndIsActiveTrue(@Param("theatreId") Long theatreId);
    default List<Screen> findByTheatreIdAndIsActiveTrue(Integer theatreId) {
        return theatreId != null ? findByTheatreIdAndIsActiveTrue(theatreId.longValue()) : java.util.Collections.emptyList();
    }

    List<Screen> findByTheatreIdAndStatus(Long theatreId, String status);
    default List<Screen> findByTheatreIdAndStatus(Integer theatreId, String status) {
        return theatreId != null ? findByTheatreIdAndStatus(theatreId.longValue(), status) : java.util.Collections.emptyList();
    }

    List<Screen> findByTheatreId(Long theatreId);
    default List<Screen> findByTheatreId(Integer theatreId) {
        return theatreId != null ? findByTheatreId(theatreId.longValue()) : java.util.Collections.emptyList();
    }

    long countByTheatreId(Long theatreId);
    default long countByTheatreId(Integer theatreId) {
        return theatreId != null ? countByTheatreId(theatreId.longValue()) : 0L;
    }

    Optional<Screen> findByTheatreIdAndScreenCode(Long theatreId, String screenCode);
    default Optional<Screen> findByTheatreIdAndScreenCode(Integer theatreId, String screenCode) {
        return theatreId != null ? findByTheatreIdAndScreenCode(theatreId.longValue(), screenCode) : Optional.empty();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Screen s WHERE s.screenId = :screenId")
    Optional<Screen> findByIdWithPessimisticLock(@Param("screenId") Long screenId);
    default Optional<Screen> findByIdWithPessimisticLock(Integer screenId) {
        return screenId != null ? findByIdWithPessimisticLock(screenId.longValue()) : Optional.empty();
    }

    default Optional<Screen> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
