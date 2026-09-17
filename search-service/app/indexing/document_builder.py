"""
PVK Cinemas Search Service — Document Builder
Constructs searchable text representations and metadata from authoritative MySQL records.
Authority: SPEC-BASELINE §I.3, AI-001, AI-002, FR-096.
"""

import json
import logging
from typing import Any

logger = logging.getLogger(__name__)

# Pipeline version recorded in SEARCH_INDEX_DOCUMENT.index_version (FR-098)
PIPELINE_VERSION = "v4.0"


class DocumentBuilder:
    """
    Converts raw MySQL row dictionaries (from DbReader) into IndexDocument data
    for the BM25 store, vector store, and SEARCH_INDEX_DOCUMENT.

    Field composition per entity type (PLAN §11):
    MOVIE:
        searchable_text = title + original_title + synopsis + genre_names + language_names + certification_code
        metadata = {movie_id, title, status, runtime_minutes, poster_url}
    THEATRE:
        searchable_text = theatre_name + city_name + state_name + address_line_1
        metadata = {theatre_id, theatre_name, theatre_code}
    """

    def build_movie_document(self, row: dict[str, Any]) -> dict[str, Any]:
        """
        Build searchable representation for a MOVIE row.
        :param row: dict from DbReader.fetch_movies()
        :return: dict with keys: entity_type, entity_id, searchable_text, display_title, metadata_json
        """
        parts = [
            self._safe_str(row.get("title")),
            self._safe_str(row.get("original_title")),
            self._safe_str(row.get("synopsis")),
            self._safe_str(row.get("genre_names")),
            self._safe_str(row.get("language_names")),
            self._safe_str(row.get("certification_code")),
        ]
        searchable_text = " ".join(p for p in parts if p).strip()

        metadata = {
            "movie_id": row.get("movie_id"),
            "title": row.get("title"),
            "status": row.get("status"),
            "runtime_minutes": row.get("runtime_minutes"),
            "poster_url": row.get("poster_url"),
            "certification_code": row.get("certification_code"),
        }

        return {
            "entity_type": "MOVIE",
            "entity_id": int(row["movie_id"]),
            "searchable_text": searchable_text,
            "display_title": self._safe_str(row.get("title")),
            "metadata_json": json.dumps(metadata),
            "pipeline_version": PIPELINE_VERSION,
        }

    def build_theatre_document(self, row: dict[str, Any]) -> dict[str, Any]:
        """
        Build searchable representation for a THEATRE row.
        :param row: dict from DbReader.fetch_theatres()
        :return: dict with keys: entity_type, entity_id, searchable_text, display_title, metadata_json
        """
        parts = [
            self._safe_str(row.get("theatre_name")),
            self._safe_str(row.get("city_name")),
            self._safe_str(row.get("state_name")),
            self._safe_str(row.get("address_line_1")),
            self._safe_str(row.get("address_line_2")),
            self._safe_str(row.get("format_names")),
        ]
        searchable_text = " ".join(p for p in parts if p).strip()

        metadata = {
            "theatre_id": row.get("theatre_id"),
            "theatre_name": row.get("theatre_name"),
            "theatre_code": row.get("theatre_code"),
            "city_name": row.get("city_name"),
            "state_name": row.get("state_name"),
        }

        return {
            "entity_type": "THEATRE",
            "entity_id": int(row["theatre_id"]),
            "searchable_text": searchable_text,
            "display_title": self._safe_str(row.get("theatre_name")),
            "metadata_json": json.dumps(metadata),
            "pipeline_version": PIPELINE_VERSION,
        }

    @staticmethod
    def _safe_str(value: Any) -> str:
        """Convert value to string, returning empty string for None."""
        if value is None:
            return ""
        return str(value).strip()
