package com.pvk.cinemas.infrastructure.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "seat", uniqueConstraints = {
    @UniqueConstraint(name = "uq_seat_coordinate", columnNames = {"screen_id", "row_label", "seat_number"})
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "seat_type_id", nullable = false)
    private Long seatTypeId;

    @Column(name = "row_label", nullable = false, length = 20)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(name = "position_x", precision = 10, scale = 3)
    private BigDecimal positionX;

    @Column(name = "position_y", precision = 10, scale = 3)
    private BigDecimal positionY;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "pricing_zone", nullable = false, length = 20)
    private String pricingZone = "STANDARD";

    @Column(name = "aisle_after", nullable = false)
    private Boolean aisleAfter = false;

    public Seat() {}

    public Seat(Long screenId, Long seatTypeId, String rowLabel, String seatNumber) {
        this.screenId = screenId;
        this.seatTypeId = seatTypeId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.status = "ACTIVE";
        this.pricingZone = "STANDARD";
        this.aisleAfter = false;
    }

    public Seat(Long screenId, Long seatTypeId, String rowLabel, String seatNumber, String pricingZone, Boolean aisleAfter) {
        this.screenId = screenId;
        this.seatTypeId = seatTypeId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.status = "ACTIVE";
        this.pricingZone = pricingZone != null ? pricingZone : "STANDARD";
        this.aisleAfter = aisleAfter != null && aisleAfter;
    }

    public Seat(Integer screenId, Integer seatTypeId, String rowLabel, String seatNumber) {
        this.screenId = screenId != null ? Long.valueOf(screenId) : null;
        this.seatTypeId = seatTypeId != null ? Long.valueOf(seatTypeId) : null;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.status = "ACTIVE";
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? Long.valueOf(screenId) : null; }

    public Long getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(Long seatTypeId) { this.seatTypeId = seatTypeId; }
    public void setSeatTypeId(Integer seatTypeId) { this.seatTypeId = seatTypeId != null ? Long.valueOf(seatTypeId) : null; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public BigDecimal getPositionX() { return positionX; }
    public void setPositionX(BigDecimal positionX) { this.positionX = positionX; }

    public BigDecimal getPositionY() { return positionY; }
    public void setPositionY(BigDecimal positionY) { this.positionY = positionY; }

    public Integer getGridRowIndex() { return positionY != null ? positionY.intValue() : null; }
    public void setGridRowIndex(Integer gridRowIndex) { this.positionY = gridRowIndex != null ? BigDecimal.valueOf(gridRowIndex) : null; }

    public Integer getGridColIndex() { return positionX != null ? positionX.intValue() : null; }
    public void setGridColIndex(Integer gridColIndex) { this.positionX = gridColIndex != null ? BigDecimal.valueOf(gridColIndex) : null; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return "ACTIVE".equalsIgnoreCase(this.status); }
    public void setIsActive(Boolean active) { this.status = (active != null && active) ? "ACTIVE" : "INACTIVE"; }

    public String getPricingZone() { return pricingZone != null ? pricingZone : "STANDARD"; }
    public void setPricingZone(String pricingZone) { this.pricingZone = pricingZone; }

    public Boolean getAisleAfter() { return aisleAfter != null && aisleAfter; }
    public void setAisleAfter(Boolean aisleAfter) { this.aisleAfter = aisleAfter; }
}
