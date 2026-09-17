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
}

