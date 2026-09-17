package com.pvk.cinemas.organization.dto;

public class CityResponse {
    private Long cityId;
    private String cityName;
    private String stateName;
    private String countryCode;

    public CityResponse() {}

    public CityResponse(Long cityId, String cityName, String stateName, String countryCode) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.stateName = stateName;
        this.countryCode = countryCode;
    }

    public CityResponse(Integer cityId, String cityName, String stateName, String countryCode) {
        this(cityId != null ? cityId.longValue() : null, cityName, stateName, countryCode);
    }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId != null ? cityId.longValue() : null; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
