package com.pvk.cinemas.catalogue.dto;

public class LanguageResponse {
    private Long languageId;
    private Long movieLanguageId;
    private String languageCode;
    private String languageName;
    private String languageRole;

    public LanguageResponse() {}

    public LanguageResponse(Long languageId, String languageCode, String languageName, String languageRole) {
        this.languageId = languageId;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.languageRole = languageRole;
    }

    public LanguageResponse(Integer languageId, String languageCode, String languageName, String languageRole) {
        this(languageId != null ? languageId.longValue() : null, languageCode, languageName, languageRole);
    }

    public Long getLanguageId() { return languageId; }
    public void setLanguageId(Long languageId) { this.languageId = languageId; }
    public void setLanguageId(Integer languageId) { this.languageId = languageId != null ? languageId.longValue() : null; }

    public Long getMovieLanguageId() { return movieLanguageId; }
    public void setMovieLanguageId(Long movieLanguageId) { this.movieLanguageId = movieLanguageId; }
    public void setMovieLanguageId(Integer movieLanguageId) { this.movieLanguageId = movieLanguageId != null ? movieLanguageId.longValue() : null; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public String getLanguageName() { return languageName; }
    public void setLanguageName(String languageName) { this.languageName = languageName; }

    public String getLanguageRole() { return languageRole; }
    public void setLanguageRole(String languageRole) { this.languageRole = languageRole; }
}
