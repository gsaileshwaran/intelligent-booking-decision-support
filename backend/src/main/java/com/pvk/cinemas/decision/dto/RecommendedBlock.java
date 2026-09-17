package com.pvk.cinemas.decision.dto;

import java.math.BigDecimal;
import java.util.List;

public class RecommendedBlock {

    private String title;
    private String category; // OPTIMAL_VIEW, BALANCED, ACOUSTIC, VALUE
    private List<Long> seatIds;
    private List<String> seatLabels;
    private int averageScore;
    private BigDecimal totalPrice;
    private String rationale;

    public RecommendedBlock() {}

    public RecommendedBlock(String title, String category, List<Long> seatIds, List<String> seatLabels, int averageScore, BigDecimal totalPrice, String rationale) {
        this.title = title;
        this.category = category;
        this.seatIds = seatIds;
        this.seatLabels = seatLabels;
        this.averageScore = averageScore;
        this.totalPrice = totalPrice;
        this.rationale = rationale;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }

    public List<String> getSeatLabels() { return seatLabels; }
    public void setSeatLabels(List<String> seatLabels) { this.seatLabels = seatLabels; }

    public int getAverageScore() { return averageScore; }
    public void setAverageScore(int averageScore) { this.averageScore = averageScore; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
