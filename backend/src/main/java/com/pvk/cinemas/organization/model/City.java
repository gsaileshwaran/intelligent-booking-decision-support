package com.pvk.cinemas.organization.model;

import jakarta.persistence.*;

@Entity
@Table(name = "city", uniqueConstraints = {
    @UniqueConstraint(name = "uq_city_location", columnNames = {"city_name", "state_name", "country_code"})
})
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Column(name = "state_name", nullable = false, length = 100)
    private String stateName;

    @Column(name = "country_code", nullable = false, columnDefinition = "CHAR(2)", length = 2)
    private String countryCode = "IN";

    public City() {}

    public City(String cityName, String stateName, String countryCode) {
        this.cityName = cityName;
        this.stateName = stateName;
        this.countryCode = countryCode != null ? countryCode : "IN";
    }

    public City(Long cityId, String cityName, String stateName, String countryCode) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.stateName = stateName;
        this.countryCode = countryCode != null ? countryCode : "IN";
    }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId != null ? Long.valueOf(cityId) : null; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
