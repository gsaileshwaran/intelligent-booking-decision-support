package com.pvk.cinemas.decision.dto;

import java.util.List;

public class InteractionFrictionRequest {

    private int filterChangeCount = 0;
    private int theatreChangeCount = 0;
    private int timeSelectionCount = 0;
    private int seatToggleCount = 0;
    private int backNavigationCount = 0;
    private int failedHoldAttempts = 0;
    private int timeWindowSeconds = 120;
    private List<String> recentSelectedTimes;

    public InteractionFrictionRequest() {}

    public InteractionFrictionRequest(int timeSelectionCount, int seatToggleCount, int filterChangeCount) {
        this.timeSelectionCount = timeSelectionCount;
        this.seatToggleCount = seatToggleCount;
        this.filterChangeCount = filterChangeCount;
    }

    public int getFilterChangeCount() { return filterChangeCount; }
    public void setFilterChangeCount(int filterChangeCount) { this.filterChangeCount = filterChangeCount; }

    public int getTheatreChangeCount() { return theatreChangeCount; }
    public void setTheatreChangeCount(int theatreChangeCount) { this.theatreChangeCount = theatreChangeCount; }

    public int getTimeSelectionCount() { return timeSelectionCount; }
    public void setTimeSelectionCount(int timeSelectionCount) { this.timeSelectionCount = timeSelectionCount; }

    public int getSeatToggleCount() { return seatToggleCount; }
    public void setSeatToggleCount(int seatToggleCount) { this.seatToggleCount = seatToggleCount; }

    public int getBackNavigationCount() { return backNavigationCount; }
    public void setBackNavigationCount(int backNavigationCount) { this.backNavigationCount = backNavigationCount; }

    public int getFailedHoldAttempts() { return failedHoldAttempts; }
    public void setFailedHoldAttempts(int failedHoldAttempts) { this.failedHoldAttempts = failedHoldAttempts; }

    public int getTimeWindowSeconds() { return timeWindowSeconds; }
    public void setTimeWindowSeconds(int timeWindowSeconds) { this.timeWindowSeconds = timeWindowSeconds; }

    public List<String> getRecentSelectedTimes() { return recentSelectedTimes; }
    public void setRecentSelectedTimes(List<String> recentSelectedTimes) { this.recentSelectedTimes = recentSelectedTimes; }
}
