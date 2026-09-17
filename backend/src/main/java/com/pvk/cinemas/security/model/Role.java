package com.pvk.cinemas.security.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope = "PLATFORM";

    public Role() {}

    public Role(Long roleId, String roleCode, String name, String description, String scope) {
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.name = name;
        this.description = description;
        this.scope = scope;
    }

    public Role(Integer roleId, String roleCode, String name, String description, String scope) {
        this(roleId != null ? roleId.longValue() : null, roleCode, name, description, scope);
    }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId != null ? roleId.longValue() : null; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRoleName() { return name; }
    public void setRoleName(String roleName) { this.name = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
