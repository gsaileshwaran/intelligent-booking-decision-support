package com.pvk.cinemas.booking.dto;

import java.math.BigDecimal;

public class SeatDetail {

    private Long seatId;
    private String rowCode;
    private Integer seatNumber;
    private String seatType;
    private BigDecimal price;

    public SeatDetail() {}

    public SeatDetail(Long seatId, String rowCode, Integer seatNumber, String seatType, BigDecimal price) {
        this.seatId = seatId;
        this.rowCode = rowCode;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.price = price;
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public String getRowCode() { return rowCode; }
    public void setRowCode(String rowCode) { this.rowCode = rowCode; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
