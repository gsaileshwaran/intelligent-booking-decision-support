package com.pvk.cinemas.security.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import com.pvk.cinemas.security.dto.PermissionResponse;
import com.pvk.cinemas.security.dto.RoleResponse;
import com.pvk.cinemas.security.dto.UpdateRolePermissionsRequest;
import com.pvk.cinemas.security.service.RolePermissionManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRoleController {

    private final RolePermissionManagementService rolePermissionService;

    public AdminRoleController(RolePermissionManagementService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> roles = rolePermissionService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @PostMapping("/roles/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissionsPost(
            @PathVariable Integer roleId,
            @RequestBody UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        RoleResponse updated = rolePermissionService.updateRolePermissions(roleId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Role permissions updated successfully", updated));
    }

    @PatchMapping("/roles/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissionsPatch(
            @PathVariable Integer roleId,
            @RequestBody UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        RoleResponse updated = rolePermissionService.updateRolePermissions(roleId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Role permissions updated successfully", updated));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        List<PermissionResponse> permissions = rolePermissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }
}
