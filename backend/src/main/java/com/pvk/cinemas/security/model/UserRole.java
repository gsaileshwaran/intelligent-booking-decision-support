package com.pvk.cinemas.security.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_role")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UserRole() {}

    public UserRole(Long userId, Long roleId) {
        this.id = new UserRoleId(userId, roleId);
        this.status = "ACTIVE";
        this.assignedAt = Instant.now();
    }

    public UserRole(Long userId, Integer roleId) {
        this(userId, roleId != null ? roleId.longValue() : null);
    }

    public UserRole(Long userId, Long roleId, Long assignedByUserId) {
        this(userId, roleId);
    }

    public UserRole(Long userId, Integer roleId, Long assignedByUserId) {
        this(userId, roleId != null ? roleId.longValue() : null);
    }

    public UserRoleId getId() { return id; }
    public void setId(UserRoleId id) { this.id = id; }

    public Long getAssignedByUserId() { return null; }
    public void setAssignedByUserId(Long assignedByUserId) { /* no-op: column does not exist in schema */ }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return assignedAt; }
    public void setCreatedAt(Instant createdAt) { this.assignedAt = createdAt; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    @Embeddable
    public static class UserRoleId implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "role_id")
        private Long roleId;

        public UserRoleId() {}
        public UserRoleId(Long userId, Long roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        public UserRoleId(Long userId, Integer roleId) {
            this(userId, roleId != null ? roleId.longValue() : null);
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public void setRoleId(Integer roleId) { this.roleId = roleId != null ? roleId.longValue() : null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserRoleId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}
