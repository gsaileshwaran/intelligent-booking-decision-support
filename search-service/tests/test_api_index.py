"""
PVK Cinemas Search Service — API Index Endpoint Tests
Authority: API-SPEC §23, ARCH §11, FR-096, FR-097, FR-098, AI-009.
"""

import pytest
from unittest.mock import MagicMock
from fastapi.testclient import TestClient

from app.store.bm25_store import BM25Store
from app.store.vector_store import VectorStore
from app.indexing.document_builder import DocumentBuilder
from app.search.query_processor import QueryProcessor
from app.search.lexical_engine import LexicalEngine
from app.search.semantic_engine import SemanticEngine
from app.search.hybrid_ranker import HybridRanker
from app.main import create_app


def _build_test_app():
    """Build test FastAPI app with mocked pipeline and db_reader."""
    bm25_store = BM25Store()
    vector_store = VectorStore()

    mock_model = MagicMock()
    lexical_engine = LexicalEngine(bm25_store)
    semantic_engine = SemanticEngine(vector_store, mock_model)
    hybrid_ranker = HybridRanker(lexical_weight=0.4, semantic_weight=0.6)
    query_processor = QueryProcessor()

    mock_db = MagicMock()
    mock_db.get_index_stats.return_value = {"ACTIVE": 15, "STALE": 2, "DELETED": 1}
    mock_db.get_last_indexed_at.return_value = "2026-09-05 12:00:00"

    mock_pipeline = MagicMock()
    mock_pipeline.index_all.return_value = {"movies": 10, "theatres": 5, "total": 15}

    app = create_app(enable_lifespan=False)
    app.state.bm25_store = bm25_store
    app.state.vector_store = vector_store
    app.state.db_reader = mock_db
    app.state.lexical_engine = lexical_engine
    app.state.semantic_engine = semantic_engine
    app.state.hybrid_ranker = hybrid_ranker
    app.state.query_processor = query_processor
    app.state.pipeline = mock_pipeline
    app.state.document_builder = DocumentBuilder()

    return app, mock_pipeline, mock_db


class TestIndexEndpoints:
    """Test suite for /internal/v1/index and /internal/v1/index/status."""

    def test_reindex_triggers_pipeline(self):
        app, mock_pipeline, _ = _build_test_app()
        client = TestClient(app)

        response = client.post("/internal/v1/index")
        assert response.status_code == 200
        data = response.json()
        assert data["triggered"] is True
        assert "Full reindex triggered" in data["message"]
        # Background task executed
        mock_pipeline.index_all.assert_called_once()

    def test_index_status_returns_counts(self):
        app, _, mock_db = _build_test_app()
        client = TestClient(app)

        response = client.get("/internal/v1/index/status")
        assert response.status_code == 200
        data = response.json()
        assert data["total_documents"] == 18
        assert data["active_documents"] == 15
        assert data["stale_documents"] == 2
        assert data["deleted_documents"] == 1
        assert data["pipeline_version"] == "v4.0"
        assert data["last_indexed_at"] == "2026-09-05 12:00:00"

    def test_index_status_handles_db_exception(self):
        app, _, mock_db = _build_test_app()
        mock_db.get_index_stats.side_effect = Exception("DB error")
        mock_db.get_last_indexed_at.side_effect = Exception("DB error")
        client = TestClient(app)

        response = client.get("/internal/v1/index/status")
        assert response.status_code == 200
        data = response.json()
        assert data["total_documents"] == 0
        assert data["active_documents"] == 0
        assert data["last_indexed_at"] is None
