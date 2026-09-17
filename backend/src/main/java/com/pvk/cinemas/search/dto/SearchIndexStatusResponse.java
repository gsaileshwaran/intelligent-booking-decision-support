package com.pvk.cinemas.search.dto;

import java.time.Instant;

public class SearchIndexStatusResponse {
    private long totalDocuments;
    private long activeDocuments;
    private long staleDocuments;
    private String pipelineVersion;
    private Instant lastIndexedAt;

    public SearchIndexStatusResponse() {}

    public SearchIndexStatusResponse(long totalDocuments, long activeDocuments, long staleDocuments, String pipelineVersion, Instant lastIndexedAt) {
        this.totalDocuments = totalDocuments;
        this.activeDocuments = activeDocuments;
        this.staleDocuments = staleDocuments;
        this.pipelineVersion = pipelineVersion;
        this.lastIndexedAt = lastIndexedAt;
    }

    public long getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(long totalDocuments) { this.totalDocuments = totalDocuments; }

    public long getActiveDocuments() { return activeDocuments; }
    public void setActiveDocuments(long activeDocuments) { this.activeDocuments = activeDocuments; }

    public long getStaleDocuments() { return staleDocuments; }
    public void setStaleDocuments(long staleDocuments) { this.staleDocuments = staleDocuments; }

    public String getPipelineVersion() { return pipelineVersion; }
    public void setPipelineVersion(String pipelineVersion) { this.pipelineVersion = pipelineVersion; }

    public Instant getLastIndexedAt() { return lastIndexedAt; }
    public void setLastIndexedAt(Instant lastIndexedAt) { this.lastIndexedAt = lastIndexedAt; }
}
