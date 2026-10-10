package com.pvk.cinemas.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SeatRequest {
    @NotNull(message = "Seat type ID is required")
    private Integer seatTypeId;

    @NotBlank(message = "Row label is required")
    @Size(max = 10)
    private String rowLabel;

    @NotBlank(message = "Seat number is required")
    @Size(max = 10)
    private String seatNumber;

    private Integer gridRowIndex;
    private Integer gridColIndex;
    private Boolean isActive = true;
    // Physical seat status — ACTIVE or BLOCKED (overrides isActive for domain correctness)
    private String status;
    // Optional reason when blocking a seat for maintenance
    private String blockReason;
    private String pricingZone;
    private Boolean aisleAfter;
    private java.math.BigDecimal positionX;
    private java.math.BigDecimal positionY;

    public SeatRequest() {}

    public Integer getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(Integer seatTypeId) { this.seatTypeId = seatTypeId; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public Integer getGridRowIndex() { return gridRowIndex; }
    public void setGridRowIndex(Integer gridRowIndex) { this.gridRowIndex = gridRowIndex; }

    public Integer getGridColIndex() { return gridColIndex; }
    public void setGridColIndex(Integer gridColIndex) { this.gridColIndex = gridColIndex; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

    public String getPricingZone() { return pricingZone; }
    public void setPricingZone(String pricingZone) { this.pricingZone = pricingZone; }

    public Boolean getAisleAfter() { return aisleAfter; }
    public void setAisleAfter(Boolean aisleAfter) { this.aisleAfter = aisleAfter; }

    public java.math.BigDecimal getPositionX() { return positionX; }
    public void setPositionX(java.math.BigDecimal positionX) { this.positionX = positionX; }

    public java.math.BigDecimal getPositionY() { return positionY; }
    public void setPositionY(java.math.BigDecimal positionY) { this.positionY = positionY; }
}

