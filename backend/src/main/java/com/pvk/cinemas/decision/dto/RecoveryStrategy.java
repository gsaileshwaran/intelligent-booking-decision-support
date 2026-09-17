package com.pvk.cinemas.decision.dto;

import java.util.List;

public class RecoveryStrategy {

    private String title;
    private String description;
    private String actionType; // SHIFT_SEATS, AUTO_SELECT_CONTIGUOUS, SWITCH_SHOWTIME
    private List<Long> suggestedSeatIds;
    private List<String> suggestedSeatLabels;

    public RecoveryStrategy() {}

    public RecoveryStrategy(String title, String description, String actionType, List<Long> suggestedSeatIds, List<String> suggestedSeatLabels) {
        this.title = title;
        this.description = description;
        this.actionType = actionType;
        this.suggestedSeatIds = suggestedSeatIds;
        this.suggestedSeatLabels = suggestedSeatLabels;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public List<Long> getSuggestedSeatIds() { return suggestedSeatIds; }
    public void setSuggestedSeatIds(List<Long> suggestedSeatIds) { this.suggestedSeatIds = suggestedSeatIds; }

    public List<String> getSuggestedSeatLabels() { return suggestedSeatLabels; }
    public void setSuggestedSeatLabels(List<String> suggestedSeatLabels) { this.suggestedSeatLabels = suggestedSeatLabels; }
}
