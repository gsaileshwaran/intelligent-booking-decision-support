package com.pvk.cinemas.security.dto;

import java.util.List;

public class RoleResponse {

    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private List<String> permissions;

    public RoleResponse() {}

    public RoleResponse(Long roleId, String roleCode, String roleName, String description, List<String> permissions) {
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.description = description;
        this.permissions = permissions;
    }

    public RoleResponse(Integer roleId, String roleCode, String roleName, String description, List<String> permissions) {
        this(roleId != null ? roleId.longValue() : null, roleCode, roleName, description, permissions);
    }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId != null ? roleId.longValue() : null; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
