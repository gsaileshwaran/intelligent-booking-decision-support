package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "presentation_format")
public class PresentationFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "presentation_format_id")
    private Long presentationFormatId;

    @Column(name = "format_code", nullable = false, unique = true, length = 50)
    private String formatCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public PresentationFormat() {}

    public PresentationFormat(Long presentationFormatId, String formatCode, String name, String description) {
        this.presentationFormatId = presentationFormatId;
        this.formatCode = formatCode;
        this.name = name;
        this.description = description;
    }

    public Long getPresentationFormatId() { return presentationFormatId; }
    public void setPresentationFormatId(Long presentationFormatId) { this.presentationFormatId = presentationFormatId; }
    public void setPresentationFormatId(Integer presentationFormatId) { this.presentationFormatId = presentationFormatId != null ? Long.valueOf(presentationFormatId) : null; }

    public String getFormatCode() { return formatCode; }
    public void setFormatCode(String formatCode) { this.formatCode = formatCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
