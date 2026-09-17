package com.pvk.cinemas.security.repository;

import com.pvk.cinemas.security.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {
    List<UserRole> findByIdUserId(Long userId);
    boolean existsByIdUserIdAndIdRoleId(Long userId, Long roleId);
    default boolean existsByIdUserIdAndIdRoleId(Long userId, Integer roleId) {
        return roleId != null && existsByIdUserIdAndIdRoleId(userId, roleId.longValue());
    }
    void deleteByIdUserId(Long userId);
}
