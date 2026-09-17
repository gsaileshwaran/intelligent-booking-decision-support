"""
PVK Cinemas Search Service — API Search Endpoint Tests
Authority: API-SPEC §23, FR-090, FR-091, FR-092.
"""

import pytest
import numpy as np
from unittest.mock import MagicMock, AsyncMock
from fastapi.testclient import TestClient

from app.store.bm25_store import BM25Store
from app.store.vector_store import VectorStore
from app.search.query_processor import QueryProcessor
from app.search.lexical_engine import LexicalEngine
from app.search.semantic_engine import SemanticEngine
from app.search.hybrid_ranker import HybridRanker
from app.indexing.pipeline import IndexingPipeline
from app.indexing.document_builder import DocumentBuilder
from app.main import create_app


def _build_test_app():
    """Build a test FastAPI app with pre-seeded in-memory stores."""
    # Create stores
    bm25_store = BM25Store()
    vector_store = VectorStore()

    # Seed BM25
    bm25_store.add_documents("MOVIE", [
        (1, "KGF action drama Kannada gold mines"),
        (2, "RRR Telugu period epic action"),
        (3, "Pushpa action thriller Allu Arjun"),
    ])
    bm25_store.add_documents("THEATRE", [
        (10, "PVK Grand Hyderabad Banjara Hills"),
    ])

    # Seed vector store
    rng = np.random.default_rng(42)
    for entity_id in [1, 2, 3]:
        vector_store.add("MOVIE", entity_id, rng.random(384).astype(np.float32))
    vector_store.add("THEATRE", 10, rng.random(384).astype(np.float32))

    # Mock embedding model (deterministic)
    mock_model = MagicMock()
    def encode(text, **kwargs):
        seed = abs(hash(text)) % (2**31)
        return np.random.default_rng(seed).random(384).astype(np.float32)
    mock_model.encode.side_effect = encode

    # Build engines
    lexical_engine = LexicalEngine(bm25_store)
    semantic_engine = SemanticEngine(vector_store, mock_model)
    hybrid_ranker = HybridRanker(lexical_weight=0.4, semantic_weight=0.6)
    query_processor = QueryProcessor()

    # Mock db_reader and pipeline for API tests
    mock_db = MagicMock()
    mock_db.get_index_stats.return_value = {"ACTIVE": 4, "STALE": 0, "DELETED": 0}
    mock_db.get_last_indexed_at.return_value = "2026-09-05 10:00:00"
    mock_pipeline = MagicMock()
    mock_pipeline.index_all.return_value = {"movies": 3, "theatres": 1, "total": 4}

    # Create app without running startup lifespan
    app = create_app(enable_lifespan=False)

    # Attach state directly
    app.state.bm25_store = bm25_store
    app.state.vector_store = vector_store
    app.state.db_reader = mock_db
    app.state.lexical_engine = lexical_engine
    app.state.semantic_engine = semantic_engine
    app.state.hybrid_ranker = hybrid_ranker
    app.state.query_processor = query_processor
    app.state.pipeline = mock_pipeline
    app.state.document_builder = DocumentBuilder()

    return app


@pytest.fixture(scope="module")
def client():
    app = _build_test_app()
    with TestClient(app, raise_server_exceptions=True) as c:
        yield c


class TestSearchEndpoint:
    """API tests for POST /internal/v1/search."""

    # TS-SEARCH-API-001: Valid query returns 200 with results
    def test_valid_query_returns_200(self, client):
        resp = client.post("/internal/v1/search", json={"query": "KGF action"})
        assert resp.status_code == 200
        data = resp.json()
        assert "results" in data
        assert isinstance(data["results"], list)
        assert data["total"] >= 0

    # TS-SEARCH-API-002: Results have required fields
    def test_results_have_required_fields(self, client):
        resp = client.post("/internal/v1/search", json={"query": "action movie"})
        assert resp.status_code == 200
        results = resp.json()["results"]
        if results:
            r = results[0]
            assert "entity_type" in r
            assert "entity_id" in r
            assert "rank_position" in r
            assert "retrieval_method" in r
            assert r["rank_position"] >= 1

    # TS-SEARCH-API-003: rank_position is sequential
    def test_rank_positions_sequential(self, client):
        resp = client.post("/internal/v1/search", json={"query": "action"})
        assert resp.status_code == 200
        results = resp.json()["results"]
        for i, r in enumerate(results, start=1):
            assert r["rank_position"] == i

    # TS-SEARCH-API-004: Empty query returns 422
    def test_empty_query_returns_422(self, client):
        resp = client.post("/internal/v1/search", json={"query": "   "})
        assert resp.status_code == 422

    # TS-SEARCH-API-005: retrieval_method is valid value
    def test_retrieval_method_valid(self, client):
        resp = client.post("/internal/v1/search", json={"query": "cinema"})
        assert resp.status_code == 200
        results = resp.json()["results"]
        valid_methods = {"LEXICAL", "SEMANTIC", "HYBRID"}
        for r in results:
            assert r["retrieval_method"] in valid_methods

    # TS-SEARCH-API-006: max_results respected
    def test_max_results_respected(self, client):
        resp = client.post("/internal/v1/search", json={"query": "action", "max_results": 1})
        assert resp.status_code == 200
        assert len(resp.json()["results"]) <= 1


class TestHealthEndpoint:
    """Tests for GET /health."""

    def test_health_returns_200(self, client):
        resp = client.get("/health")
        assert resp.status_code == 200

    def test_health_status_up(self, client):
        resp = client.get("/health")
        assert resp.json()["status"] == "UP"


class TestIndexEndpoints:
    """Tests for index management endpoints."""

    def test_index_status_returns_200(self, client):
        resp = client.get("/internal/v1/index/status")
        assert resp.status_code == 200
        data = resp.json()
        assert "total_documents" in data
        assert "active_documents" in data
        assert "stale_documents" in data
        assert "pipeline_version" in data

    def test_reindex_trigger_returns_200(self, client):
        resp = client.post("/internal/v1/index")
        assert resp.status_code == 200
        data = resp.json()
        assert data["triggered"] is True
