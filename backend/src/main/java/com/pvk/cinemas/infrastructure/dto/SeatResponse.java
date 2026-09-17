package com.pvk.cinemas.infrastructure.dto;

public class SeatResponse {
    private Long seatId;
    private Long screenId;
    private Long seatTypeId;
    private String seatTypeCode;
    private String rowLabel;
    private String seatNumber;
    private Integer gridRowIndex;
    private Integer gridColIndex;
    private Boolean isActive;
    // Physical seat domain fields
    private String status;
    private String pricingZone;
    private Boolean aisleAfter;
    // Manager warning fields
    private String warningMessage;
    private Integer upcomingBookingsCount;

    public SeatResponse() {}

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? screenId.longValue() : null; }

    public Long getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(Long seatTypeId) { this.seatTypeId = seatTypeId; }
    public void setSeatTypeId(Integer seatTypeId) { this.seatTypeId = seatTypeId != null ? seatTypeId.longValue() : null; }

    public String getSeatTypeCode() { return seatTypeCode; }
    public void setSeatTypeCode(String seatTypeCode) { this.seatTypeCode = seatTypeCode; }

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

    public String getPricingZone() { return pricingZone; }
    public void setPricingZone(String pricingZone) { this.pricingZone = pricingZone; }

    public Boolean getAisleAfter() { return aisleAfter; }
    public void setAisleAfter(Boolean aisleAfter) { this.aisleAfter = aisleAfter; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }

    public Integer getUpcomingBookingsCount() { return upcomingBookingsCount; }
    public void setUpcomingBookingsCount(Integer upcomingBookingsCount) { this.upcomingBookingsCount = upcomingBookingsCount; }
}
