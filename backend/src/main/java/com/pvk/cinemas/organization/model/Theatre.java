package com.pvk.cinemas.organization.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "theatre")
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theatre_id")
    private Long theatreId;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "theatre_code", nullable = false, unique = true, length = 50)
    private String theatreCode;

    @Column(name = "theatre_name", nullable = false, length = 150)
    private String theatreName;

    @Column(name = "address_line_1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "base_price_offset", precision = 10, scale = 2)
    private BigDecimal basePriceOffset = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Theatre() {}

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? Long.valueOf(theatreId) : null; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId != null ? Long.valueOf(cityId) : null; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return "ACTIVE".equalsIgnoreCase(this.status); }
    public void setIsActive(Boolean active) { this.status = (active != null && active) ? "ACTIVE" : "INACTIVE"; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getBasePriceOffset() { return basePriceOffset != null ? basePriceOffset : BigDecimal.ZERO; }
    public void setBasePriceOffset(BigDecimal basePriceOffset) { this.basePriceOffset = basePriceOffset; }
}
