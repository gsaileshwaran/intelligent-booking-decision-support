package com.pvk.cinemas.decision.dto;

public class TheatreAlternative {

    private Long theatreId;
    private String theatreName;
    private Long showId;
    private String showTime;
    private int availableCount;
    private int qualityScore;

    public TheatreAlternative() {}

    public TheatreAlternative(Long theatreId, String theatreName, Long showId, String showTime, int availableCount, int qualityScore) {
        this.theatreId = theatreId;
        this.theatreName = theatreName;
        this.showId = showId;
        this.showTime = showTime;
        this.availableCount = availableCount;
        this.qualityScore = qualityScore;
    }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public String getShowTime() { return showTime; }
    public void setShowTime(String showTime) { this.showTime = showTime; }

    public int getAvailableCount() { return availableCount; }
    public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
}
