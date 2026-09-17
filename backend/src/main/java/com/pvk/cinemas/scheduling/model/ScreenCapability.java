package com.pvk.cinemas.scheduling.model;

import jakarta.persistence.*;

@Entity
@Table(name = "screen_capability", uniqueConstraints = {
    @UniqueConstraint(name = "uq_screen_capability", columnNames = {"screen_id", "presentation_format_id", "audio_format_id"})
})
public class ScreenCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screen_capability_id")
    private Long screenCapabilityId;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "presentation_format_id", nullable = false)
    private Long presentationFormatId;

    @Column(name = "audio_format_id", nullable = false)
    private Long audioFormatId;

    public ScreenCapability() {}

    public ScreenCapability(Long screenId, Long presentationFormatId, Long audioFormatId) {
        this.screenId = screenId;
        this.presentationFormatId = presentationFormatId;
        this.audioFormatId = audioFormatId;
    }

    public ScreenCapability(Integer screenId, Integer presentationFormatId, Integer audioFormatId) {
        this.screenId = screenId != null ? screenId.longValue() : null;
        this.presentationFormatId = presentationFormatId != null ? presentationFormatId.longValue() : null;
        this.audioFormatId = audioFormatId != null ? audioFormatId.longValue() : null;
    }

    public Long getScreenCapabilityId() { return screenCapabilityId; }
    public void setScreenCapabilityId(Long screenCapabilityId) { this.screenCapabilityId = screenCapabilityId; }
    public void setScreenCapabilityId(Integer screenCapabilityId) { this.screenCapabilityId = screenCapabilityId != null ? screenCapabilityId.longValue() : null; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? screenId.longValue() : null; }

    public Long getPresentationFormatId() { return presentationFormatId; }
    public void setPresentationFormatId(Long presentationFormatId) { this.presentationFormatId = presentationFormatId; }
    public void setPresentationFormatId(Integer presentationFormatId) { this.presentationFormatId = presentationFormatId != null ? presentationFormatId.longValue() : null; }

    public Long getAudioFormatId() { return audioFormatId; }
    public void setAudioFormatId(Long audioFormatId) { this.audioFormatId = audioFormatId; }
    public void setAudioFormatId(Integer audioFormatId) { this.audioFormatId = audioFormatId != null ? audioFormatId.longValue() : null; }
}
