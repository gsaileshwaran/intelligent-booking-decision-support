package com.pvk.cinemas.organization.repository;

import com.pvk.cinemas.organization.model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    Optional<Theatre> findByTheatreCode(String theatreCode);

    @Query("SELECT t FROM Theatre t WHERE t.cityId = :cityId AND t.status = 'ACTIVE'")
    List<Theatre> findByCityIdAndIsActiveTrue(@Param("cityId") Long cityId);
    default List<Theatre> findByCityIdAndIsActiveTrue(Integer cityId) {
        return cityId != null ? findByCityIdAndIsActiveTrue(cityId.longValue()) : java.util.Collections.emptyList();
    }

    @Query("SELECT t FROM Theatre t WHERE t.status = 'ACTIVE'")
    List<Theatre> findByIsActiveTrue();

    List<Theatre> findByCityIdAndStatus(Long cityId, String status);
    default List<Theatre> findByCityIdAndStatus(Integer cityId, String status) {
        return cityId != null ? findByCityIdAndStatus(cityId.longValue(), status) : java.util.Collections.emptyList();
    }

    List<Theatre> findByStatus(String status);

    default Optional<Theatre> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
