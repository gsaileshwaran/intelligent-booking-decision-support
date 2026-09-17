package com.pvk.cinemas.audit.service;

import com.pvk.cinemas.audit.model.AuditLog;
import com.pvk.cinemas.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog logAction(Long actorUserId, String action, String entityType, Long entityId, String oldValue, String newValue) {
        String safeAction = action != null ? (action.length() > 50 ? action.substring(0, 50) : action) : "ACTION";
        String safeEntityType = entityType != null ? (entityType.length() > 50 ? entityType.substring(0, 50) : entityType) : "SYSTEM";
        Long safeActorUserId = actorUserId;
        Long safeEntityId = entityId != null ? entityId : 0L;

        AuditLog log = new AuditLog(
                safeActorUserId,
                safeAction,
                safeEntityType,
                safeEntityId,
                oldValue,
                newValue
        );
        return auditLogRepository.save(log);
    }

    @Transactional
    public AuditLog logAction(Long actorUserId, String actionType, String entityType, String entityId, String details, String ipAddress) {
        Long parsedEntityId = 0L;
        if (entityId != null) {
            try {
                parsedEntityId = Long.parseLong(entityId);
            } catch (NumberFormatException e) {
                parsedEntityId = 0L;
            }
        }
        String jsonNewValue = formatAsJson(details, ipAddress);
        return logAction(actorUserId, actionType, entityType, parsedEntityId, null, jsonNewValue);
    }

    private String formatAsJson(String details, String ipAddress) {
        String escapedDetails = details != null ? details.replace("\"", "\\\"").replace("\n", " ") : "";
        String escapedIp = ipAddress != null ? ipAddress.replace("\"", "\\\"") : "";
        return "{\"details\":\"" + escapedDetails + "\",\"ipAddress\":\"" + escapedIp + "\"}";
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
