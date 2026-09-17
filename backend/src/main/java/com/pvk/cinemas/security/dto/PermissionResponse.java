package com.pvk.cinemas.security.dto;

public class PermissionResponse {

    private Long permissionId;
    private String permissionCode;
    private String description;

    public PermissionResponse() {}

    public PermissionResponse(Long permissionId, String permissionCode, String description) {
        this.permissionId = permissionId;
        this.permissionCode = permissionCode;
        this.description = description;
    }

    public PermissionResponse(Integer permissionId, String permissionCode, String description) {
        this(permissionId != null ? permissionId.longValue() : null, permissionCode, description);
    }

    public Long getPermissionId() { return permissionId; }
    public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
    public void setPermissionId(Integer permissionId) { this.permissionId = permissionId != null ? permissionId.longValue() : null; }

    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
