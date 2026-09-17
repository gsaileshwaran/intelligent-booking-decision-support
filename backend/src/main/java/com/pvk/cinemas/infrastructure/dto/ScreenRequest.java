package com.pvk.cinemas.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ScreenRequest {
    @NotBlank(message = "Screen code is required")
    @Size(max = 50)
    private String screenCode;

    @NotBlank(message = "Screen name is required")
    @Size(max = 100)
    private String screenName;

    private Boolean isActive = true;

    public ScreenRequest() {}

    public String getScreenCode() { return screenCode; }
    public void setScreenCode(String screenCode) { this.screenCode = screenCode; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
