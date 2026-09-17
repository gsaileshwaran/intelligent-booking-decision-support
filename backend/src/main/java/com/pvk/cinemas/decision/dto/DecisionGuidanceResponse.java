package com.pvk.cinemas.decision.dto;

import java.util.List;

public class DecisionGuidanceResponse {

    private int overallQualityScore;
    private String overallRating; // OPTIMAL, PRIME, GOOD, FAIR
    private List<String> keyStrengths;
    private List<FrictionAlert> frictionAlerts;
    private List<RecoveryStrategy> recoveryStrategies;

    public DecisionGuidanceResponse() {}

    public DecisionGuidanceResponse(int overallQualityScore, String overallRating, List<String> keyStrengths, List<FrictionAlert> frictionAlerts, List<RecoveryStrategy> recoveryStrategies) {
        this.overallQualityScore = overallQualityScore;
        this.overallRating = overallRating;
        this.keyStrengths = keyStrengths;
        this.frictionAlerts = frictionAlerts;
        this.recoveryStrategies = recoveryStrategies;
    }

    public int getOverallQualityScore() { return overallQualityScore; }
    public void setOverallQualityScore(int overallQualityScore) { this.overallQualityScore = overallQualityScore; }

    public String getOverallRating() { return overallRating; }
    public void setOverallRating(String overallRating) { this.overallRating = overallRating; }

    public List<String> getKeyStrengths() { return keyStrengths; }
    public void setKeyStrengths(List<String> keyStrengths) { this.keyStrengths = keyStrengths; }

    public List<FrictionAlert> getFrictionAlerts() { return frictionAlerts; }
    public void setFrictionAlerts(List<FrictionAlert> frictionAlerts) { this.frictionAlerts = frictionAlerts; }

    public List<RecoveryStrategy> getRecoveryStrategies() { return recoveryStrategies; }
    public void setRecoveryStrategies(List<RecoveryStrategy> recoveryStrategies) { this.recoveryStrategies = recoveryStrategies; }
}
