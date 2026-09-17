package com.pvk.cinemas.decision.dto;

public class InteractionFrictionResponse {

    private boolean detected;
    private String frictionType; // e.g. "TIME_SELECTION_INSTABILITY", "SEAT_SELECTION_CHURN", "FILTER_INSTABILITY", "CONCURRENCY_CONFLICT", "NONE"
    private String severity; // "LOW", "MEDIUM", "HIGH", "NONE"
    private String title;
    private String description;
    private String suggestedAction;
    private int triggerCount;

    public InteractionFrictionResponse() {
        this.detected = false;
        this.frictionType = "NONE";
        this.severity = "NONE";
        this.title = "Normal Interaction";
        this.description = "No interaction friction signals detected.";
        this.suggestedAction = "PROCEED";
        this.triggerCount = 0;
    }

    public InteractionFrictionResponse(boolean detected, String frictionType, String severity,
                                     String title, String description, String suggestedAction, int triggerCount) {
        this.detected = detected;
        this.frictionType = frictionType;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.suggestedAction = suggestedAction;
        this.triggerCount = triggerCount;
    }

    public boolean isDetected() { return detected; }
    public void setDetected(boolean detected) { this.detected = detected; }

    public String getFrictionType() { return frictionType; }
    public void setFrictionType(String frictionType) { this.frictionType = frictionType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public int getTriggerCount() { return triggerCount; }
    public void setTriggerCount(int triggerCount) { this.triggerCount = triggerCount; }
}
