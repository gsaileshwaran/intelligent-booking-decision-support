package com.pvk.cinemas.security.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.security.dto.PermissionResponse;
import com.pvk.cinemas.security.dto.RoleResponse;
import com.pvk.cinemas.security.dto.UpdateRolePermissionsRequest;
import com.pvk.cinemas.security.model.Permission;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.RolePermission;
import com.pvk.cinemas.security.repository.PermissionRepository;
import com.pvk.cinemas.security.repository.RolePermissionRepository;
import com.pvk.cinemas.security.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolePermissionManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogService auditLogService;

    public RolePermissionManagementService(RoleRepository roleRepository,
                                           PermissionRepository permissionRepository,
                                           RolePermissionRepository rolePermissionRepository,
                                           AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(this::mapToRoleResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getPermissionId(), p.getPermissionCode(), p.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updateRolePermissions(Integer roleId, UpdateRolePermissionsRequest request, Long actorUserId, String ipAddress) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        rolePermissionRepository.deleteByIdRoleId(roleId);

        if (request.getPermissionCodes() != null) {
            for (String pcode : request.getPermissionCodes()) {
                Permission p = permissionRepository.findByPermissionCode(pcode)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + pcode));
                rolePermissionRepository.save(new RolePermission(roleId, p.getPermissionId()));
            }
        }

        auditLogService.logAction(
                actorUserId,
                "ROLE_PERMISSION_UPDATE",
                "ROLE",
                String.valueOf(roleId),
                "Updated permissions for role: " + role.getRoleCode(),
                ipAddress
        );

        return mapToRoleResponse(role);
    }

    private RoleResponse mapToRoleResponse(Role role) {
        List<RolePermission> rps = rolePermissionRepository.findByIdRoleId(role.getRoleId());
        List<String> permCodes = new ArrayList<>();
        for (RolePermission rp : rps) {
            permissionRepository.findById(rp.getId().getPermissionId()).ifPresent(p -> permCodes.add(p.getPermissionCode()));
        }
        return new RoleResponse(role.getRoleId(), role.getRoleCode(), role.getRoleName(), role.getDescription(), permCodes);
    }
}
