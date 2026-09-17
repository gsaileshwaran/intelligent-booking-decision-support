package com.pvk.cinemas.search.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_query")
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_query_id")
    private Long searchQueryId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "query_text", nullable = false, length = 500)
    private String queryText;

    @Column(name = "result_count", nullable = false)
    private Integer resultCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SearchQuery() {}

    public SearchQuery(Long userId, String queryText, Integer resultCount) {
        this.userId = userId;
        this.queryText = queryText;
        this.resultCount = resultCount;
        this.createdAt = Instant.now();
    }

    public Long getSearchQueryId() { return searchQueryId; }
    public void setSearchQueryId(Long searchQueryId) { this.searchQueryId = searchQueryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
