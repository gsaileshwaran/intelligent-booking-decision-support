package com.pvk.cinemas.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

public class ScreenCapabilityRequest {

    @NotNull(message = "Presentation format ID is required")
    private Long presentationFormatId;

    @NotNull(message = "Audio format ID is required")
    private Long audioFormatId;

    public ScreenCapabilityRequest() {}

    public ScreenCapabilityRequest(Long presentationFormatId, Long audioFormatId) {
        this.presentationFormatId = presentationFormatId;
        this.audioFormatId = audioFormatId;
    }

    public ScreenCapabilityRequest(Integer presentationFormatId, Integer audioFormatId) {
        this.presentationFormatId = presentationFormatId != null ? presentationFormatId.longValue() : null;
        this.audioFormatId = audioFormatId != null ? audioFormatId.longValue() : null;
    }

    public Long getPresentationFormatId() {
        return presentationFormatId;
    }

    public void setPresentationFormatId(Long presentationFormatId) {
        this.presentationFormatId = presentationFormatId;
    }

    public Long getAudioFormatId() {
        return audioFormatId;
    }

    public void setAudioFormatId(Long audioFormatId) {
        this.audioFormatId = audioFormatId;
    }
}
