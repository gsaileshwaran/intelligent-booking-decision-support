package com.pvk.cinemas.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TheatreRequest {
    @NotNull(message = "City ID is required")
    private Integer cityId;

    @NotBlank(message = "Theatre code is required")
    @Size(max = 50)
    private String theatreCode;

    @NotBlank(message = "Theatre name is required")
    @Size(max = 150)
    private String theatreName;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    private String addressLine2;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive = true;

    public TheatreRequest() {}

    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }

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
