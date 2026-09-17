package com.pvk.cinemas.security.repository;

import com.pvk.cinemas.security.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);

    default Optional<Role> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
