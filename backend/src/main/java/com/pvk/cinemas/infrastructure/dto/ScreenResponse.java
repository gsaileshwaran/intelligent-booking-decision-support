package com.pvk.cinemas.infrastructure.dto;

public class ScreenResponse {
    private Long screenId;
    private Long theatreId;
    private String screenCode;
    private String screenName;
    private Boolean isActive;
    private Long screenCapabilityId;

    public ScreenResponse() {}

    public ScreenResponse(Long screenId, Long theatreId, String screenCode, String screenName, Boolean isActive) {
        this.screenId = screenId;
        this.theatreId = theatreId;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.isActive = isActive;
    }

    public ScreenResponse(Integer screenId, Integer theatreId, String screenCode, String screenName, Boolean isActive) {
        this(screenId != null ? screenId.longValue() : null, theatreId != null ? theatreId.longValue() : null, screenCode, screenName, isActive);
    }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? screenId.longValue() : null; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? theatreId.longValue() : null; }

    public String getScreenCode() { return screenCode; }
    public void setScreenCode(String screenCode) { this.screenCode = screenCode; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public Long getScreenCapabilityId() { return screenCapabilityId; }
    public void setScreenCapabilityId(Long screenCapabilityId) { this.screenCapabilityId = screenCapabilityId; }
    public void setScreenCapabilityId(Integer screenCapabilityId) { this.screenCapabilityId = screenCapabilityId != null ? screenCapabilityId.longValue() : null; }
}
