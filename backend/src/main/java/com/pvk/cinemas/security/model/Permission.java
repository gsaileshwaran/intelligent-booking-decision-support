package com.pvk.cinemas.security.model;

import jakarta.persistence.*;

@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public Permission() {}

    public Permission(Long permissionId, String permissionCode, String name, String description) {
        this.permissionId = permissionId;
        this.permissionCode = permissionCode;
        this.name = name != null ? name : permissionCode;
        this.description = description;
    }

    public Permission(Integer permissionId, String permissionCode, String description) {
        this.permissionId = permissionId != null ? permissionId.longValue() : null;
        this.permissionCode = permissionCode;
        this.name = permissionCode;
        this.description = description;
    }

    public Long getPermissionId() { return permissionId; }
    public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
    public void setPermissionId(Integer permissionId) { this.permissionId = permissionId != null ? permissionId.longValue() : null; }

    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
