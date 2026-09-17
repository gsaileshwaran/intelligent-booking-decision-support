package com.pvk.cinemas.decision.dto;

import java.util.ArrayList;
import java.util.List;

public class JourneyStageGuidanceResponse {

    private String currentStage;
    private String guidanceTitle;
    private String guidancePrompt;
    private String nextRecommendedAction;
    private List<String> contextualTips = new ArrayList<>();

    public JourneyStageGuidanceResponse() {}

    public JourneyStageGuidanceResponse(String currentStage, String guidanceTitle, String guidancePrompt,
                                        String nextRecommendedAction, List<String> contextualTips) {
        this.currentStage = currentStage;
        this.guidanceTitle = guidanceTitle;
        this.guidancePrompt = guidancePrompt;
        this.nextRecommendedAction = nextRecommendedAction;
        this.contextualTips = contextualTips;
    }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public String getGuidanceTitle() { return guidanceTitle; }
    public void setGuidanceTitle(String guidanceTitle) { this.guidanceTitle = guidanceTitle; }

    public String getGuidancePrompt() { return guidancePrompt; }
    public void setGuidancePrompt(String guidancePrompt) { this.guidancePrompt = guidancePrompt; }

    public String getNextRecommendedAction() { return nextRecommendedAction; }
    public void setNextRecommendedAction(String nextRecommendedAction) { this.nextRecommendedAction = nextRecommendedAction; }

    public List<String> getContextualTips() { return contextualTips; }
    public void setContextualTips(List<String> contextualTips) { this.contextualTips = contextualTips; }
}
