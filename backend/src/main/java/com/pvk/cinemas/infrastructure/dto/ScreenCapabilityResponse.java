package com.pvk.cinemas.infrastructure.dto;

public class ScreenCapabilityResponse {

    private Long screenCapabilityId;
    private Long screenId;
    private Long presentationFormatId;
    private String presentationFormatCode;
    private String presentationFormatName;
    private Long audioFormatId;
    private String audioFormatCode;
    private String audioFormatName;

    public ScreenCapabilityResponse() {}

    public ScreenCapabilityResponse(Long screenCapabilityId, Long screenId,
                                    Long presentationFormatId, String presentationFormatCode, String presentationFormatName,
                                    Long audioFormatId, String audioFormatCode, String audioFormatName) {
        this.screenCapabilityId = screenCapabilityId;
        this.screenId = screenId;
        this.presentationFormatId = presentationFormatId;
        this.presentationFormatCode = presentationFormatCode;
        this.presentationFormatName = presentationFormatName;
        this.audioFormatId = audioFormatId;
        this.audioFormatCode = audioFormatCode;
        this.audioFormatName = audioFormatName;
    }

    public Long getScreenCapabilityId() {
        return screenCapabilityId;
    }

    public void setScreenCapabilityId(Long screenCapabilityId) {
        this.screenCapabilityId = screenCapabilityId;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public Long getPresentationFormatId() {
        return presentationFormatId;
    }

    public void setPresentationFormatId(Long presentationFormatId) {
        this.presentationFormatId = presentationFormatId;
    }

    public String getPresentationFormatCode() {
        return presentationFormatCode;
    }

    public void setPresentationFormatCode(String presentationFormatCode) {
        this.presentationFormatCode = presentationFormatCode;
    }

    public String getPresentationFormatName() {
        return presentationFormatName;
    }

    public void setPresentationFormatName(String presentationFormatName) {
        this.presentationFormatName = presentationFormatName;
    }

    public Long getAudioFormatId() {
        return audioFormatId;
    }

    public void setAudioFormatId(Long audioFormatId) {
        this.audioFormatId = audioFormatId;
    }

    public String getAudioFormatCode() {
        return audioFormatCode;
    }

    public void setAudioFormatCode(String audioFormatCode) {
        this.audioFormatCode = audioFormatCode;
    }

    public String getAudioFormatName() {
        return audioFormatName;
    }

    public void setAudioFormatName(String audioFormatName) {
        this.audioFormatName = audioFormatName;
    }
}
