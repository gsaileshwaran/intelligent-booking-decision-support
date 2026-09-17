package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "language")
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "language_id")
    private Long languageId;

    @Column(name = "language_code", nullable = false, unique = true, length = 10)
    private String languageCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public Language() {}

    public Language(Long languageId, String languageCode, String name) {
        this.languageId = languageId;
        this.languageCode = languageCode;
        this.name = name;
    }

    public Language(Integer languageId, String languageCode, String name) {
        this.languageId = languageId != null ? Long.valueOf(languageId) : null;
        this.languageCode = languageCode;
        this.name = name;
    }

    public Long getLanguageId() { return languageId; }
    public void setLanguageId(Long languageId) { this.languageId = languageId; }
    public void setLanguageId(Integer languageId) { this.languageId = languageId != null ? Long.valueOf(languageId) : null; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLanguageName() { return name; }
    public void setLanguageName(String languageName) { this.name = languageName; }
}
