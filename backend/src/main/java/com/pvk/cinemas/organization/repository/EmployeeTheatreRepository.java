package com.pvk.cinemas.organization.repository;

import com.pvk.cinemas.organization.model.EmployeeTheatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeTheatreRepository extends JpaRepository<EmployeeTheatre, EmployeeTheatre.EmployeeTheatreId> {
    boolean existsByIdUserIdAndIdTheatreId(Long userId, Long theatreId);
    default boolean existsByIdUserIdAndIdRoleId(Long userId, Integer theatreId) {
        return theatreId != null && existsByIdUserIdAndIdTheatreId(userId, theatreId.longValue());
    }
    default boolean existsByIdUserIdAndIdTheatreId(Long userId, Integer theatreId) {
        return theatreId != null && existsByIdUserIdAndIdTheatreId(userId, theatreId.longValue());
    }

    List<EmployeeTheatre> findByIdUserId(Long userId);

    List<EmployeeTheatre> findByIdTheatreId(Long theatreId);
    default List<EmployeeTheatre> findByIdTheatreId(Integer theatreId) {
        return theatreId != null ? findByIdTheatreId(theatreId.longValue()) : java.util.Collections.emptyList();
    }

    void deleteByIdUserIdAndIdTheatreId(Long userId, Long theatreId);
    default void deleteByIdUserIdAndIdTheatreId(Long userId, Integer theatreId) {
        if (theatreId != null) deleteByIdUserIdAndIdTheatreId(userId, theatreId.longValue());
    }
}
