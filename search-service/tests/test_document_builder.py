"""
PVK Cinemas Search Service — Document Builder Tests
Authority: SPEC-BASELINE §I.3, AI-002, FR-096.
"""

import json
import pytest
from app.indexing.document_builder import DocumentBuilder, PIPELINE_VERSION


class TestDocumentBuilder:
    """Unit tests for DocumentBuilder."""

    def setup_method(self):
        self.builder = DocumentBuilder()

    # TS-DOC-001: MOVIE document with full data
    def test_movie_document_full(self):
        row = {
            "movie_id": 1,
            "title": "KGF Chapter 2",
            "original_title": "KGF",
            "synopsis": "Action drama set in gold mines",
            "runtime_minutes": 168,
            "status": "AIRING",
            "poster_url": "https://example.com/kgf.jpg",
            "certification_code": "UA",
            "genre_names": "Action Drama",
            "language_names": "Kannada Hindi",
        }
        doc = self.builder.build_movie_document(row)

        assert doc["entity_type"] == "MOVIE"
        assert doc["entity_id"] == 1
        assert "KGF Chapter 2" in doc["searchable_text"]
        assert "Action" in doc["searchable_text"]
        assert "Kannada" in doc["searchable_text"]
        assert "UA" in doc["searchable_text"]
        assert doc["display_title"] == "KGF Chapter 2"
        assert doc["pipeline_version"] == PIPELINE_VERSION

    # TS-DOC-002: THEATRE document
    def test_theatre_document_full(self):
        row = {
            "theatre_id": 10,
            "theatre_code": "PVK-HYD-001",
            "theatre_name": "PVK Grand Hyderabad",
            "address_line_1": "Banjara Hills",
            "address_line_2": "Road No. 12",
            "postal_code": "500034",
            "status": "ACTIVE",
            "city_name": "Hyderabad",
            "state_name": "Telangana",
            "country_code": "IN",
        }
        doc = self.builder.build_theatre_document(row)

        assert doc["entity_type"] == "THEATRE"
        assert doc["entity_id"] == 10
        assert "PVK Grand Hyderabad" in doc["searchable_text"]
        assert "Hyderabad" in doc["searchable_text"]
        assert "Banjara Hills" in doc["searchable_text"]
        assert doc["display_title"] == "PVK Grand Hyderabad"

    # TS-DOC-003: Null fields handled gracefully
    def test_movie_document_null_fields(self):
        row = {
            "movie_id": 2,
            "title": "Silent Movie",
            "original_title": None,
            "synopsis": None,
            "runtime_minutes": 90,
            "status": "UPCOMING",
            "poster_url": None,
            "certification_code": "U",
            "genre_names": None,
            "language_names": None,
        }
        doc = self.builder.build_movie_document(row)
        assert doc["entity_type"] == "MOVIE"
        assert doc["entity_id"] == 2
        assert "Silent Movie" in doc["searchable_text"]
        # Should not crash and should produce valid text

    # TS-DOC-004: Genre and language concatenation
    def test_genre_language_concatenation(self):
        row = {
            "movie_id": 3,
            "title": "Test Movie",
            "original_title": None,
            "synopsis": None,
            "runtime_minutes": 120,
            "status": "UPCOMING",
            "poster_url": None,
            "certification_code": "A",
            "genre_names": "Action Comedy Thriller",
            "language_names": "Hindi Telugu Tamil",
        }
        doc = self.builder.build_movie_document(row)
        text = doc["searchable_text"]
        assert "Action" in text
        assert "Comedy" in text
        assert "Hindi" in text
        assert "Tamil" in text

    # TS-DOC-005: Metadata JSON is valid JSON
    def test_movie_metadata_is_valid_json(self):
        row = {
            "movie_id": 4,
            "title": "JSON Test",
            "original_title": None,
            "synopsis": "test",
            "runtime_minutes": 100,
            "status": "UPCOMING",
            "poster_url": None,
            "certification_code": "U",
            "genre_names": "Drama",
            "language_names": "English",
        }
        doc = self.builder.build_movie_document(row)
        metadata = json.loads(doc["metadata_json"])
        assert metadata["movie_id"] == 4
        assert metadata["title"] == "JSON Test"

    # TS-DOC-006: Pipeline version is set
    def test_pipeline_version_set(self):
        row = {
            "movie_id": 5,
            "title": "Version Test",
            "original_title": None,
            "synopsis": None,
            "runtime_minutes": 90,
            "status": "UPCOMING",
            "poster_url": None,
            "certification_code": "U",
            "genre_names": None,
            "language_names": None,
        }
        doc = self.builder.build_movie_document(row)
        assert doc["pipeline_version"] == "v4.0"
