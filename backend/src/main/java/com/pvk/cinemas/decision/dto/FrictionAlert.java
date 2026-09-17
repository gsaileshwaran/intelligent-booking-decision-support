package com.pvk.cinemas.decision.dto;

public class FrictionAlert {

    private String type; // ORPHAN_SEAT, SPLIT_GROUP, SUBOPTIMAL_VIEW, HOLD_EXPIRING
    private String severity; // WARNING, INFO
    private String title;
    private String message;

    public FrictionAlert() {}

    public FrictionAlert(String type, String severity, String title, String message) {
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
