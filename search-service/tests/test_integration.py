"""
PVK Cinemas Search Service — End-to-End Integration Tests
Authority: SPEC-BASELINE §I.3, SRS AI-001–AI-009, FR-090–FR-098.
"""

import pytest
import numpy as np
from unittest.mock import MagicMock

from app.store.bm25_store import BM25Store
from app.store.vector_store import VectorStore
from app.indexing.document_builder import DocumentBuilder
from app.indexing.pipeline import IndexingPipeline
from app.search.query_processor import QueryProcessor
from app.search.lexical_engine import LexicalEngine
from app.search.semantic_engine import SemanticEngine
from app.search.hybrid_ranker import HybridRanker


class TestEndToEndSearchPipeline:
    """
    Simulates complete pipeline:
    MySQL (mocked reader) -> IndexingPipeline -> BM25 + Vector Stores
    -> QueryProcessor -> LexicalEngine + SemanticEngine -> HybridRanker.
    """

    @pytest.fixture
    def setup_system(self):
        bm25_store = BM25Store()
        vector_store = VectorStore()
        document_builder = DocumentBuilder()

        # Deterministic mock embedding model
        embedding_model = MagicMock()
        def encode(text, **kwargs):
            # Seed based on content for reproducible vector similarity
            seed = sum(ord(c) for c in text) % (2**31)
            rng = np.random.default_rng(seed)
            v = rng.random(384).astype(np.float32)
            norm = np.linalg.norm(v)
            return v / norm if norm > 0 else v
        embedding_model.encode.side_effect = encode

        semantic_engine = SemanticEngine(vector_store, embedding_model)
        lexical_engine = LexicalEngine(bm25_store)
        hybrid_ranker = HybridRanker(lexical_weight=0.4, semantic_weight=0.6)
        query_processor = QueryProcessor()

        # Mock DB reader with sample catalogue
        mock_db = MagicMock()
        mock_db.fetch_movies.return_value = [
            {
                "movie_id": 1,
                "title": "KGF Chapter 2",
                "synopsis": "Rocky becomes the ruler of KGF gold mines facing Adheera.",
                "runtime_minutes": 168,
                "movie_status": "PUBLISHED",
                "release_date": "2022-04-14",
                "rating": "UA",
                "genres": ["Action", "Period"],
                "languages": ["Kannada", "Hindi", "Telugu"],
            },
            {
                "movie_id": 2,
                "title": "Inception",
                "synopsis": "A thief who steals corporate secrets through dream-sharing technology.",
                "runtime_minutes": 148,
                "movie_status": "PUBLISHED",
                "release_date": "2010-07-16",
                "rating": "UA",
                "genres": ["Sci-Fi", "Action"],
                "languages": ["English"],
            },
            {
                "movie_id": 3,
                "title": "Interstellar",
                "synopsis": "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                "runtime_minutes": 169,
                "movie_status": "PUBLISHED",
                "release_date": "2014-11-07",
                "rating": "UA",
                "genres": ["Sci-Fi", "Drama"],
                "languages": ["English"],
            },
        ]

        mock_db.fetch_theatres.return_value = [
            {
                "theatre_id": 10,
                "theatre_name": "PVK Grand Hyderabad",
                "theatre_code": "PVK-HYD-01",
                "theatre_status": "ACTIVE",
                "address_line1": "Road No 2, Banjara Hills",
                "city_name": "Hyderabad",
            },
            {
                "theatre_id": 20,
                "theatre_name": "PVK IMAX Bangalore",
                "theatre_code": "PVK-BLR-01",
                "theatre_status": "ACTIVE",
                "address_line1": "Koramangala 4th Block",
                "city_name": "Bangalore",
            },
        ]

        pipeline = IndexingPipeline(
            db_reader=mock_db,
            bm25_store=bm25_store,
            vector_store=vector_store,
            document_builder=document_builder,
            semantic_engine=semantic_engine,
        )

        # Run full index
        result = pipeline.index_all()
        assert result["total"] == 5

        return {
            "query_processor": query_processor,
            "lexical_engine": lexical_engine,
            "semantic_engine": semantic_engine,
            "hybrid_ranker": hybrid_ranker,
            "bm25_store": bm25_store,
            "vector_store": vector_store,
        }

    def test_exact_title_query(self, setup_system):
        qp = setup_system["query_processor"]
        lex = setup_system["lexical_engine"]
        sem = setup_system["semantic_engine"]
        ranker = setup_system["hybrid_ranker"]

        clean_query = qp.process("Inception")
        assert clean_query.lower() == "inception"

        lexical_hits = lex.search(clean_query, top_n=10)
        semantic_hits = sem.search(clean_query, top_n=10)

        results = ranker.rank(lexical_hits, semantic_hits, top_n=10)
        assert len(results) > 0
        # Inception movie should be ranked top
        top_hit = results[0]
        assert top_hit["entity_type"] == "MOVIE"
        assert top_hit["entity_id"] == 2
        assert top_hit["rank_position"] == 1

    def test_theatre_location_query(self, setup_system):
        qp = setup_system["query_processor"]
        lex = setup_system["lexical_engine"]
        sem = setup_system["semantic_engine"]
        ranker = setup_system["hybrid_ranker"]

        clean_query = qp.process("Hyderabad Banjara")
        lexical_hits = lex.search(clean_query, top_n=10)
        semantic_hits = sem.search(clean_query, top_n=10)

        results = ranker.rank(lexical_hits, semantic_hits, top_n=10)
        assert len(results) > 0
        # Should include PVK Grand Hyderabad (ID 10)
        hyd_theatre = next((r for r in results if r["entity_type"] == "THEATRE" and r["entity_id"] == 10), None)
        assert hyd_theatre is not None

    def test_semantic_theme_query(self, setup_system):
        qp = setup_system["query_processor"]
        lex = setup_system["lexical_engine"]
        sem = setup_system["semantic_engine"]
        ranker = setup_system["hybrid_ranker"]

        clean_query = qp.process("space travel wormhole survival")
        lexical_hits = lex.search(clean_query, top_n=10)
        semantic_hits = sem.search(clean_query, top_n=10)

        results = ranker.rank(lexical_hits, semantic_hits, top_n=10)
        assert len(results) > 0
        # Interstellar (ID 3) should be among the top candidates
        entity_ids = [r["entity_id"] for r in results if r["entity_type"] == "MOVIE"]
        assert 3 in entity_ids

    def test_filter_by_entity_type(self, setup_system):
        qp = setup_system["query_processor"]
        lex = setup_system["lexical_engine"]
        sem = setup_system["semantic_engine"]
        ranker = setup_system["hybrid_ranker"]

        clean_query = qp.process("PVK")
        lexical_hits = lex.search(clean_query, entity_type="THEATRE", top_n=10)
        semantic_hits = sem.search(clean_query, entity_type="THEATRE", top_n=10)

        results = ranker.rank(lexical_hits, semantic_hits, top_n=10)
        # All returned results must be THEATRE
        for r in results:
            assert r["entity_type"] == "THEATRE"

    def test_scores_are_normalized_and_bounded(self, setup_system):
        qp = setup_system["query_processor"]
        lex = setup_system["lexical_engine"]
        sem = setup_system["semantic_engine"]
        ranker = setup_system["hybrid_ranker"]

        clean_query = qp.process("gold mines action")
        lexical_hits = lex.search(clean_query, top_n=10)
        semantic_hits = sem.search(clean_query, top_n=10)

        results = ranker.rank(lexical_hits, semantic_hits, top_n=10)
        for r in results:
            assert 0.0 <= r["relevance_score"] <= 1.0
            assert r["retrieval_method"] in ("HYBRID", "LEXICAL", "SEMANTIC")
