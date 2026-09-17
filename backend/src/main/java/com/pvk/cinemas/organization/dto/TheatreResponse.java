package com.pvk.cinemas.organization.dto;

import java.math.BigDecimal;

public class TheatreResponse {
    private Long theatreId;
    private Long cityId;
    private String cityName;
    private String theatreCode;
    private String theatreName;
    private String addressLine1;
    private String addressLine2;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive;
    private Integer totalScreens;

    public TheatreResponse() {}

    public Integer getTotalScreens() { return totalScreens; }
    public void setTotalScreens(Integer totalScreens) { this.totalScreens = totalScreens; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? theatreId.longValue() : null; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId != null ? cityId.longValue() : null; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getTheatreCode() { return theatreCode; }
    public void setTheatreCode(String theatreCode) { this.theatreCode = theatreCode; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
