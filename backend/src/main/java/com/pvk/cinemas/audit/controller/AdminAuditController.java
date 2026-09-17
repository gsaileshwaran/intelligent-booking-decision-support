package com.pvk.cinemas.audit.controller;

import com.pvk.cinemas.audit.model.AuditLog;
import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogService.getAuditLogs(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
