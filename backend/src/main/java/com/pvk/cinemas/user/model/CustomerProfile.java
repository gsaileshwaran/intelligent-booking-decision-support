package com.pvk.cinemas.user.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "customer_profile")
public class CustomerProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "preferred_language_id")
    private Long preferredLanguageId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CustomerProfile() {}

    public CustomerProfile(Long userId, Long preferredLanguageId, LocalDate dateOfBirth) {
        this.userId = userId;
        this.preferredLanguageId = preferredLanguageId;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public CustomerProfile(Long userId, Integer preferredLanguageId, LocalDate dateOfBirth) {
        this(userId, preferredLanguageId != null ? preferredLanguageId.longValue() : null, dateOfBirth);
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPreferredLanguageId() { return preferredLanguageId; }
    public void setPreferredLanguageId(Long preferredLanguageId) { this.preferredLanguageId = preferredLanguageId; }
    public void setPreferredLanguageId(Integer preferredLanguageId) { this.preferredLanguageId = preferredLanguageId != null ? preferredLanguageId.longValue() : null; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
