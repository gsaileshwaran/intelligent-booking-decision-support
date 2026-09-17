"""
PVK Cinemas Search Service — Pipeline Tests
Authority: AI-009, FR-096, FR-097, FR-098.
"""

import pytest
from unittest.mock import MagicMock, patch
import numpy as np

from app.store.bm25_store import BM25Store
from app.store.vector_store import VectorStore
from app.indexing.document_builder import DocumentBuilder, PIPELINE_VERSION
from app.indexing.pipeline import IndexingPipeline


def _make_semantic_engine_mock():
    """Create a SemanticEngine mock returning random embeddings."""
    mock = MagicMock()
    mock.embed_text.side_effect = lambda text: np.random.default_rng(abs(hash(text)) % (2**31)).random(384).astype(np.float32)
    return mock


def _make_db_reader_mock(movies=None, theatres=None):
    """Create a DbReader mock with preset data."""
    mock = MagicMock()
    mock.fetch_movies.return_value = movies or []
    mock.fetch_theatres.return_value = theatres or []
    mock.upsert_search_index_document.return_value = None
    return mock


SAMPLE_MOVIE_ROWS = [
    {
        "movie_id": 1, "title": "KGF Chapter 2",
        "original_title": "KGF", "synopsis": "Action drama",
        "runtime_minutes": 168, "status": "AIRING",
        "poster_url": None, "certification_code": "UA",
        "genre_names": "Action Drama", "language_names": "Kannada Hindi",
    },
    {
        "movie_id": 2, "title": "RRR",
        "original_title": None, "synopsis": "Epic war drama",
        "runtime_minutes": 182, "status": "AIRING",
        "poster_url": None, "certification_code": "UA",
        "genre_names": "Action", "language_names": "Telugu Hindi",
    },
]

SAMPLE_THEATRE_ROWS = [
    {
        "theatre_id": 10, "theatre_code": "PVK-HYD-001",
        "theatre_name": "PVK Hyderabad", "address_line_1": "Banjara Hills",
        "address_line_2": None, "postal_code": "500034",
        "status": "ACTIVE", "city_name": "Hyderabad",
        "state_name": "Telangana", "country_code": "IN",
    },
]


class TestIndexingPipeline:
    """Component tests for IndexingPipeline."""

    def setup_method(self):
        self.bm25 = BM25Store()
        self.vectors = VectorStore()
        self.builder = DocumentBuilder()
        self.semantic = _make_semantic_engine_mock()
        self.db = _make_db_reader_mock(movies=SAMPLE_MOVIE_ROWS, theatres=SAMPLE_THEATRE_ROWS)
        self.pipeline = IndexingPipeline(
            db_reader=self.db,
            document_builder=self.builder,
            bm25_store=self.bm25,
            vector_store=self.vectors,
            semantic_engine=self.semantic,
        )

    # TS-PIPELINE-001: index_all returns correct counts
    def test_index_all_counts(self):
        result = self.pipeline.index_all()
        assert result["movies"] == 2
        assert result["theatres"] == 1
        assert result["total"] == 3

    # TS-PIPELINE-002: BM25 index populated after index_all
    def test_bm25_populated(self):
        self.pipeline.index_all()
        assert self.bm25.document_count("MOVIE") == 2
        assert self.bm25.document_count("THEATRE") == 1

    # TS-PIPELINE-003: Vector store populated after index_all
    def test_vector_store_populated(self):
        self.pipeline.index_all()
        assert self.vectors.document_count("MOVIE") == 2
        assert self.vectors.document_count("THEATRE") == 1

    # TS-PIPELINE-004: SEARCH_INDEX_DOCUMENT upserted for each entity
    def test_mysql_upsert_called(self):
        self.pipeline.index_all()
        # Should be called once per movie + once per theatre
        assert self.db.upsert_search_index_document.call_count == 3

    # TS-PIPELINE-005: Rebuild clears and rebuilds (idempotency)
    def test_rebuild_idempotent(self):
        self.pipeline.index_all()
        count_after_first = self.bm25.document_count()
        self.pipeline.index_all()
        count_after_second = self.bm25.document_count()
        assert count_after_first == count_after_second

    # TS-PIPELINE-006: Empty movie list handled gracefully
    def test_empty_movies_no_crash(self):
        self.db.fetch_movies.return_value = []
        result = self.pipeline.index_all()
        assert result["movies"] == 0

    # TS-PIPELINE-007: last_indexed_at set after index_all
    def test_last_indexed_at_set(self):
        assert self.pipeline.last_indexed_at is None
        self.pipeline.index_all()
        assert self.pipeline.last_indexed_at is not None

    # TS-PIPELINE-008: Pipeline version in upsert calls
    def test_pipeline_version_in_upsert(self):
        self.pipeline.index_all()
        for call in self.db.upsert_search_index_document.call_args_list:
            kwargs = call.kwargs if call.kwargs else {}
            args = call.args
            # pipeline_version should be v4.0
            version = kwargs.get("pipeline_version") or (args[4] if len(args) > 4 else None)
            assert version == PIPELINE_VERSION
