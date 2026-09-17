package com.pvk.cinemas.security.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "role_permission")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    public RolePermission() {}

    public RolePermission(Long roleId, Long permissionId) {
        this.id = new RolePermissionId(roleId, permissionId);
    }

    public RolePermission(Integer roleId, Integer permissionId) {
        this(roleId != null ? roleId.longValue() : null, permissionId != null ? permissionId.longValue() : null);
    }

    public RolePermission(Integer roleId, Long permissionId) {
        this(roleId != null ? roleId.longValue() : null, permissionId);
    }

    public RolePermission(Long roleId, Integer permissionId) {
        this(roleId, permissionId != null ? permissionId.longValue() : null);
    }

    public RolePermissionId getId() { return id; }
    public void setId(RolePermissionId id) { this.id = id; }

    @Embeddable
    public static class RolePermissionId implements Serializable {
        @Column(name = "role_id")
        private Long roleId;

        @Column(name = "permission_id")
        private Long permissionId;

        public RolePermissionId() {}
        public RolePermissionId(Long roleId, Long permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        public RolePermissionId(Integer roleId, Integer permissionId) {
            this(roleId != null ? roleId.longValue() : null, permissionId != null ? permissionId.longValue() : null);
        }

        public RolePermissionId(Integer roleId, Long permissionId) {
            this(roleId != null ? roleId.longValue() : null, permissionId);
        }

        public RolePermissionId(Long roleId, Integer permissionId) {
            this(roleId, permissionId != null ? permissionId.longValue() : null);
        }

        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public void setRoleId(Integer roleId) { this.roleId = roleId != null ? roleId.longValue() : null; }

        public Long getPermissionId() { return permissionId; }
        public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
        public void setPermissionId(Integer permissionId) { this.permissionId = permissionId != null ? permissionId.longValue() : null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RolePermissionId that)) return false;
            return Objects.equals(roleId, that.roleId) && Objects.equals(permissionId, that.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permissionId);
        }
    }
}
