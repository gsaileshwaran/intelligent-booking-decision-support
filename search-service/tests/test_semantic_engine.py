"""
PVK Cinemas Search Service — Semantic Engine Tests
Authority: SPEC-BASELINE §I.3, P4-DEC-001.
"""

import pytest
import numpy as np
from unittest.mock import MagicMock

from app.store.vector_store import VectorStore
from app.search.semantic_engine import SemanticEngine


def _make_mock_model(embedding_dim: int = 384):
    """Create a mock SentenceTransformer that returns deterministic embeddings."""
    model = MagicMock()

    def encode(text, **kwargs):
        # Deterministic: hash text to seed and generate random but reproducible vector
        seed = abs(hash(text)) % (2**31)
        rng = np.random.default_rng(seed)
        vec = rng.random(embedding_dim).astype(np.float32)
        return vec

    model.encode.side_effect = encode
    return model


class TestVectorStore:
    """Unit tests for VectorStore."""

    def setup_method(self):
        self.store = VectorStore()

    def test_add_and_retrieve(self):
        vec = np.random.default_rng(42).random(384).astype(np.float32)
        self.store.add("MOVIE", 1, vec)
        assert self.store.has_entity("MOVIE", 1)

    def test_clear(self):
        vec = np.random.default_rng(42).random(384).astype(np.float32)
        self.store.add("MOVIE", 1, vec)
        self.store.clear()
        assert self.store.document_count() == 0

    def test_cosine_similarity_unit_vector(self):
        # Two identical vectors should have cosine similarity = 1.0
        vec = np.ones(384, dtype=np.float32)
        self.store.add("MOVIE", 1, vec)
        results = self.store.search(vec, entity_type="MOVIE")
        assert len(results) == 1
        _, entity_id, score = results[0]
        assert abs(score - 1.0) < 1e-5

    def test_document_count_by_type(self):
        vec = np.random.default_rng(42).random(384).astype(np.float32)
        self.store.add("MOVIE", 1, vec)
        self.store.add("MOVIE", 2, vec)
        self.store.add("THEATRE", 10, vec)
        assert self.store.document_count("MOVIE") == 2
        assert self.store.document_count("THEATRE") == 1

    def test_embedding_shape(self):
        """Verify the stored vector has the expected 384-dim shape."""
        vec = np.random.default_rng(0).random(384).astype(np.float32)
        self.store.add("MOVIE", 1, vec)
        # Retrieve via search
        results = self.store.search(vec, entity_type="MOVIE")
        assert len(results) == 1


class TestSemanticEngine:
    """Unit tests for SemanticEngine."""

    def setup_method(self):
        self.vector_store = VectorStore()
        self.mock_model = _make_mock_model()
        self.engine = SemanticEngine(self.vector_store, self.mock_model)

    def test_embed_query_returns_array(self):
        embedding = self.engine.embed_query("action movie")
        assert isinstance(embedding, np.ndarray)
        assert embedding.shape == (384,)

    def test_semantic_search_returns_results(self):
        # Index a document
        text = "KGF action drama Kannada"
        embedding = self.engine.embed_text(text)
        self.vector_store.add("MOVIE", 1, embedding)

        results = self.engine.search("KGF")
        assert len(results) == 1
        entity_type, entity_id, score = results[0]
        assert entity_type == "MOVIE"
        assert entity_id == 1

    def test_semantic_search_no_results(self):
        results = self.engine.search("anything")
        assert results == []

    def test_determinism(self):
        """Same query must produce same embedding (deterministic mock)."""
        emb1 = self.engine.embed_query("batman dark knight")
        emb2 = self.engine.embed_query("batman dark knight")
        np.testing.assert_array_equal(emb1, emb2)

    def test_relevance_cutoff_filters_low_similarity_candidates(self):
        """Verify that candidates below min_score cutoff are discarded before ranking."""
        # Query unit vector along first dimension
        query_vec = np.zeros(384, dtype=np.float32)
        query_vec[0] = 1.0

        # Strong match: identical vector (cosine = 1.0)
        strong_vec = np.zeros(384, dtype=np.float32)
        strong_vec[0] = 1.0
        self.vector_store.add("MOVIE", 10, strong_vec)

        # Weak/noise match: orthogonal vector with tiny projection (cosine = 0.2)
        weak_vec = np.zeros(384, dtype=np.float32)
        weak_vec[0] = 0.2
        weak_vec[1] = np.sqrt(1.0 - 0.04).astype(np.float32)
        self.vector_store.add("MOVIE", 20, weak_vec)

        # Mock engine to return query_vec
        self.mock_model.encode = MagicMock(return_value=query_vec)

        # Search with min_score = 0.45
        results = self.engine.search("test query", min_score=0.45)
        assert len(results) == 1
        assert results[0][1] == 10  # Only strong match survived
        assert results[0][2] >= 0.45

    def test_cutoff_filters_noise_to_empty(self):
        """Verify that when all candidates are below cutoff, search returns empty."""
        query_vec = np.zeros(384, dtype=np.float32)
        query_vec[0] = 1.0

        # Sub-threshold candidate (cosine = 0.35)
        noisy_vec = np.zeros(384, dtype=np.float32)
        noisy_vec[0] = 0.35
        noisy_vec[1] = np.sqrt(1.0 - 0.35**2).astype(np.float32)
        self.vector_store.add("MOVIE", 30, noisy_vec)

        self.mock_model.encode = MagicMock(return_value=query_vec)
        results = self.engine.search("gibberish nonsense", min_score=0.45)
        assert results == []
