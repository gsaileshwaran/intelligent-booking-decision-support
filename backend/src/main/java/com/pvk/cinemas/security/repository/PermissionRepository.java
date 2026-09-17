package com.pvk.cinemas.security.repository;

import com.pvk.cinemas.security.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionCode(String permissionCode);

    default Optional<Permission> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
