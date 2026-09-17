package com.pvk.cinemas.search.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "search_result")
public class SearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_result_id")
    private Long searchResultId;

    @Column(name = "search_query_id", nullable = false)
    private Long searchQueryId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "relevance_score", precision = 8, scale = 6)
    private BigDecimal relevanceScore;

    @Column(name = "retrieval_method", nullable = false, length = 50)
    private String retrievalMethod = "HYBRID";

    public SearchResult() {}

    public SearchResult(Long searchQueryId, String entityType, Long entityId, Integer rankPosition, String retrievalMethod) {
        this.searchQueryId = searchQueryId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.rankPosition = rankPosition;
        this.retrievalMethod = retrievalMethod;
    }

    public Long getSearchResultId() { return searchResultId; }
    public void setSearchResultId(Long searchResultId) { this.searchResultId = searchResultId; }

    public Long getSearchQueryId() { return searchQueryId; }
    public void setSearchQueryId(Long searchQueryId) { this.searchQueryId = searchQueryId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public BigDecimal getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(BigDecimal relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getRetrievalMethod() { return retrievalMethod; }
    public void setRetrievalMethod(String retrievalMethod) { this.retrievalMethod = retrievalMethod; }

    public Instant getCreatedAt() { return Instant.now(); }
    public void setCreatedAt(Instant createdAt) { /* no-op: column not present in schema */ }
}
