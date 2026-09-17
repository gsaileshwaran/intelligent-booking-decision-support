package com.pvk.cinemas.decision.dto;

public class ShowtimeAlternative {

    private Long showId;
    private String showTime;
    private int availableCount;
    private int maxContiguousBlock;
    private int qualityScore;

    public ShowtimeAlternative() {}

    public ShowtimeAlternative(Long showId, String showTime, int availableCount, int maxContiguousBlock, int qualityScore) {
        this.showId = showId;
        this.showTime = showTime;
        this.availableCount = availableCount;
        this.maxContiguousBlock = maxContiguousBlock;
        this.qualityScore = qualityScore;
    }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public int getAvailableCount() { return availableCount; }
    public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }

    public int getMaxContiguousBlock() { return maxContiguousBlock; }
    public void setMaxContiguousBlock(int maxContiguousBlock) { this.maxContiguousBlock = maxContiguousBlock; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
}
