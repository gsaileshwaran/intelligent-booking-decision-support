package com.pvk.cinemas.decision.dto;

import java.math.BigDecimal;

public class SeatScoreDTO {

    private Long seatId;
    private String rowLabel;
    private String seatNumber;
    private String seatType;
    private int score; // 0 - 100
    private String badge; // OPTIMAL, PRIME, GOOD, STANDARD
    private String viewCategory;
    private double distanceFactor;
    private double lateralFactor;
    private double acousticFactor;
    private double zoneFactor;
    private String pricingZone;
    private BigDecimal price;
    private java.util.List<String> reasons = new java.util.ArrayList<>();

    public SeatScoreDTO() {}

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getViewCategory() { return viewCategory; }
    public void setViewCategory(String viewCategory) { this.viewCategory = viewCategory; }

    public double getDistanceFactor() { return distanceFactor; }
    public void setDistanceFactor(double distanceFactor) { this.distanceFactor = distanceFactor; }

    public double getLateralFactor() { return lateralFactor; }
    public void setLateralFactor(double lateralFactor) { this.lateralFactor = lateralFactor; }

    public double getAcousticFactor() { return acousticFactor; }
    public void setAcousticFactor(double acousticFactor) { this.acousticFactor = acousticFactor; }

    public double getZoneFactor() { return zoneFactor; }
    public void setZoneFactor(double zoneFactor) { this.zoneFactor = zoneFactor; }

    public String getPricingZone() { return pricingZone; }
    public void setPricingZone(String pricingZone) { this.pricingZone = pricingZone; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public java.util.List<String> getReasons() { return reasons; }
    public void setReasons(java.util.List<String> reasons) { this.reasons = reasons; }
}
