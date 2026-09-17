package com.pvk.cinemas.security.repository;

import com.pvk.cinemas.security.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {
    List<RolePermission> findByIdRoleId(Long roleId);
    default List<RolePermission> findByIdRoleId(Integer roleId) {
        return roleId != null ? findByIdRoleId(roleId.longValue()) : java.util.Collections.emptyList();
    }

    void deleteByIdRoleId(Long roleId);
    default void deleteByIdRoleId(Integer roleId) {
        if (roleId != null) deleteByIdRoleId(roleId.longValue());
    }
}
