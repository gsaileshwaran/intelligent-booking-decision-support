package com.pvk.cinemas.search.dto;

public class SearchCardResponse {
    private String entityType;
    private Long entityId;
    private String title;
    private String subtitle;
    private String badge;
    private int rankPosition;
    private String retrievalMethod;
    private String posterUrl;

    public SearchCardResponse() {}

    public SearchCardResponse(String entityType, Long entityId, String title, String subtitle, String badge, int rankPosition, String retrievalMethod) {
        this(entityType, entityId, title, subtitle, badge, rankPosition, retrievalMethod, null);
    }

    public SearchCardResponse(String entityType, Long entityId, String title, String subtitle, String badge, int rankPosition, String retrievalMethod, String posterUrl) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.rankPosition = rankPosition;
        this.retrievalMethod = retrievalMethod;
        this.posterUrl = posterUrl;
    }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public int getRankPosition() { return rankPosition; }
    public void setRankPosition(int rankPosition) { this.rankPosition = rankPosition; }

    public String getRetrievalMethod() { return retrievalMethod; }
    public void setRetrievalMethod(String retrievalMethod) { this.retrievalMethod = retrievalMethod; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
}
