"""
PVK Cinemas Search Service — Index Document Domain Model
Authority: SRS FR-096, FR-097, FR-098, AI-001, AI-002, AI-003, AI-004.
"""

from typing import Optional
from pydantic import BaseModel, Field


class IndexDocument(BaseModel):
    """
    A document submitted to the indexing pipeline.
    Created by the document builder from authoritative MySQL records.
    Authority: AI-002 (identify source entity), AI-003 (index timestamp + version).
    """
    entity_type: str = Field(..., description="MOVIE or THEATRE")
    entity_id: int = Field(..., description="Primary key of the authoritative entity")
    searchable_text: str = Field(..., description="Concatenated text representation for BM25 + embedding")
    display_title: str = Field(..., description="Human-readable title for result cards")
    metadata: dict = Field(default_factory=dict, description="Additional display metadata")
    index_status: str = Field("ACTIVE", description="ACTIVE | STALE | DELETED")
    pipeline_version: str = Field("v4.0", description="Pipeline version (FR-098)")


class IndexBatchRequest(BaseModel):
    """
    Batch of documents to index, sent by Spring Boot or startup pipeline.
    """
    documents: list[IndexDocument]
    force_rebuild: bool = Field(False, description="If true, clears existing index before rebuilding")


class IndexStatusResponse(BaseModel):
    """
    Index statistics for /internal/v1/index/status.
    Mirrors SEARCH_INDEX_DOCUMENT aggregate state.
    """
    total_documents: int
    active_documents: int
    stale_documents: int
    deleted_documents: int
    pipeline_version: str
    last_indexed_at: Optional[str] = None
