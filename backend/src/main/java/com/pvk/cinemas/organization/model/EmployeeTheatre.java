package com.pvk.cinemas.organization.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "employee_theatre")
public class EmployeeTheatre {

    @EmbeddedId
    private EmployeeTheatreId id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public EmployeeTheatre() {}

    public EmployeeTheatre(Long userId, Long theatreId) {
        this.id = new EmployeeTheatreId(userId, theatreId);
        this.assignedAt = Instant.now();
        this.status = "ACTIVE";
    }

    public EmployeeTheatre(Long userId, Integer theatreId) {
        this.id = new EmployeeTheatreId(userId, theatreId != null ? Long.valueOf(theatreId) : null);
        this.assignedAt = Instant.now();
        this.status = "ACTIVE";
    }

    public EmployeeTheatre(Long userId, Integer theatreId, Long assignedByUserId) {
        this(userId, theatreId);
    }

    public EmployeeTheatre(Long userId, Long theatreId, Long assignedByUserId) {
        this(userId, theatreId);
    }

    public EmployeeTheatreId getId() { return id; }
    public void setId(EmployeeTheatreId id) { this.id = id; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Embeddable
    public static class EmployeeTheatreId implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "theatre_id")
        private Long theatreId;

        public EmployeeTheatreId() {}
        public EmployeeTheatreId(Long userId, Long theatreId) {
            this.userId = userId;
            this.theatreId = theatreId;
        }

        public EmployeeTheatreId(Long userId, Integer theatreId) {
            this.userId = userId;
            this.theatreId = theatreId != null ? Long.valueOf(theatreId) : null;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getTheatreId() { return theatreId; }
        public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
        public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? Long.valueOf(theatreId) : null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeTheatreId that)) return false;
            return Objects.equals(userId, that.userId) && Objects.equals(theatreId, that.theatreId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, theatreId);
        }
    }
}
