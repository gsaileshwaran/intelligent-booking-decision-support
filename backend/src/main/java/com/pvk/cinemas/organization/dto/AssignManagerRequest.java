package com.pvk.cinemas.organization.dto;

import jakarta.validation.constraints.NotNull;

public class AssignManagerRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    public AssignManagerRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
