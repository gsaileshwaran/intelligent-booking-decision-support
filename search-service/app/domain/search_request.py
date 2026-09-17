"""
PVK Cinemas Search Service — Domain Models
Authoritative source: SPEC-BASELINE §I, SRS §6.10, SRS §12, ARCH §11.
"""

from typing import Optional
from pydantic import BaseModel, Field, field_validator


class SearchRequest(BaseModel):
    """
    Inbound search query from Spring Boot backend.
    Authority: SRS FR-090, FR-093, FR-094, API-SPEC §23.
    """
    query: str = Field(..., description="Raw user query text")
    city_id: Optional[int] = Field(None, description="Optional city filter")
    max_results: int = Field(20, ge=1, le=50, description="Maximum results to return")

    @field_validator("query")
    @classmethod
    def query_must_not_be_empty(cls, v: str) -> str:
        stripped = v.strip()
        if not stripped:
            raise ValueError("query must not be blank")
        if len(stripped) > 500:
            raise ValueError("query must not exceed 500 characters")
        return stripped
