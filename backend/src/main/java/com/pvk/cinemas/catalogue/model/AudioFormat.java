package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "audio_format")
public class AudioFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audio_format_id")
    private Long audioFormatId;

    @Column(name = "format_code", nullable = false, unique = true, length = 50)
    private String formatCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public AudioFormat() {}

    public AudioFormat(Long audioFormatId, String formatCode, String name, String description) {
        this.audioFormatId = audioFormatId;
        this.formatCode = formatCode;
        this.name = name;
        this.description = description;
    }

    public Long getAudioFormatId() { return audioFormatId; }
    public void setAudioFormatId(Long audioFormatId) { this.audioFormatId = audioFormatId; }
    public void setAudioFormatId(Integer audioFormatId) { this.audioFormatId = audioFormatId != null ? Long.valueOf(audioFormatId) : null; }

    public String getFormatCode() { return formatCode; }
    public void setFormatCode(String formatCode) { this.formatCode = formatCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
