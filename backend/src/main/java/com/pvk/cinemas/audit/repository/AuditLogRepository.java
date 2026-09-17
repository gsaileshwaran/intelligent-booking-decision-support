package com.pvk.cinemas.audit.repository;

import com.pvk.cinemas.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAll(Pageable pageable);
    List<AuditLog> findByActorUserId(Long actorUserId);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    default List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId) {
        try {
            return findByEntityTypeAndEntityId(entityType, Long.parseLong(entityId));
        } catch (Exception e) {
            return List.of();
        }
    }
}
