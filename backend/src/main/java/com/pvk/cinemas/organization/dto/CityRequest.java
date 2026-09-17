package com.pvk.cinemas.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CityRequest {
    @NotBlank(message = "City name is required")
    @Size(max = 100)
    private String cityName;

    @NotBlank(message = "State name is required")
    @Size(max = 100)
    private String stateName;

    @Size(max = 3)
    private String countryCode = "IND";

    public CityRequest() {}

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
