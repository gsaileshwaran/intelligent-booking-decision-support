"""
PVK Cinemas Search Service — Search Result Domain Model
Authority: SRS FR-092, AI-006, AI-007, SPEC-BASELINE §I.3.
"""

from typing import Optional
from pydantic import BaseModel, Field


class CandidateResult(BaseModel):
    """
    A single ranked candidate returned by the search engine.
    Spring Boot uses entity_type + entity_id to resolve authoritative details from MySQL.
    Authority: ARCH §11 (backend resolves authoritative entity details after search).
    """
    entity_type: str = Field(..., description="MOVIE or THEATRE")
    entity_id: int = Field(..., description="Primary key of the operational entity")
    rank_position: int = Field(..., description="1-based rank position")
    retrieval_method: str = Field(..., description="LEXICAL | SEMANTIC | HYBRID")
    relevance_score: Optional[float] = Field(None, description="Hybrid score [0.0, 1.0]")
    lexical_score: Optional[float] = Field(None, description="Normalized BM25 score")
    semantic_score: Optional[float] = Field(None, description="Normalized cosine similarity")

    # Display hint — may be overridden by Spring Boot from authoritative MySQL data
    display_title: Optional[str] = Field(None, description="Title for display (from index, not authoritative)")


class SearchResponse(BaseModel):
    """
    Full search response envelope from Python service.
    """
    results: list[CandidateResult]
    total: int
    query_text: str
    retrieval_method_used: str = Field(..., description="Primary retrieval method for the query batch")
