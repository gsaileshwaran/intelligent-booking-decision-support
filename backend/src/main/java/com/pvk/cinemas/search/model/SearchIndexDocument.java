package com.pvk.cinemas.search.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_index_document")
public class SearchIndexDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "searchable_text", columnDefinition = "TEXT")
    private String searchableText;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @Column(name = "embedding_reference", length = 500)
    private String embeddingReference;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt = Instant.now();

    @Column(name = "index_version", nullable = false, length = 50)
    private String indexVersion = "v1.0";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public SearchIndexDocument() {}

    public SearchIndexDocument(String entityType, Long entityId, String documentPayload) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.searchableText = documentPayload;
        this.metadataJson = documentPayload;
        this.indexVersion = "v1.0";
        this.status = "ACTIVE";
        this.indexedAt = Instant.now();
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getSearchableText() { return searchableText; }
    public void setSearchableText(String searchableText) { this.searchableText = searchableText; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public String getEmbeddingReference() { return embeddingReference; }
    public void setEmbeddingReference(String embeddingReference) { this.embeddingReference = embeddingReference; }

    public Instant getIndexedAt() { return indexedAt; }
    public void setIndexedAt(Instant indexedAt) { this.indexedAt = indexedAt; }

    public String getIndexVersion() { return indexVersion; }
    public void setIndexVersion(String indexVersion) { this.indexVersion = indexVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Backward-compatible delegates
    public String getDocumentPayload() { return searchableText != null ? searchableText : metadataJson; }
    public void setDocumentPayload(String payload) { this.searchableText = payload; this.metadataJson = payload; }

    public String getDocumentStatus() { return status; }
    public void setDocumentStatus(String docStatus) { this.status = docStatus; }

    public String getPipelineVersion() { return indexVersion; }
    public void setPipelineVersion(String v) { this.indexVersion = v; }

    public Instant getUpdatedAt() { return indexedAt; }
    public void setUpdatedAt(Instant u) { this.indexedAt = u; }
}
