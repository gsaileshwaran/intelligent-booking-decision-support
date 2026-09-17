"""
PVK Cinemas Search Service — Lexical Engine Tests
Authority: SPEC-BASELINE §I.3, SRS AI-008.
"""

import pytest
from app.store.bm25_store import BM25Store
from app.search.lexical_engine import LexicalEngine


class TestBM25Store:
    """Unit tests for BM25Store."""

    def setup_method(self):
        self.store = BM25Store()

    def test_empty_store_returns_empty(self):
        results = self.store.search("MOVIE", "action")
        assert results == []

    def test_add_and_search_single_doc(self):
        self.store.add_documents("MOVIE", [(1, "action thriller batman")])
        results = self.store.search("MOVIE", "batman")
        assert len(results) == 1
        entity_id, score = results[0]
        assert entity_id == 1
        assert score > 0.0

    def test_no_match_returns_empty(self):
        self.store.add_documents("MOVIE", [(1, "comedy romance")])
        results = self.store.search("MOVIE", "quantum physics")
        # BM25 returns 0 scores for no match; should be filtered
        assert all(score > 0.0 for _, score in results)

    def test_multiple_docs_ranked_correctly(self):
        self.store.add_documents("MOVIE", [
            (1, "action hero batman"),
            (2, "batman sequel action"),
            (3, "comedy romance love"),
        ])
        results = self.store.search("MOVIE", "batman")
        # Both batman docs should score > 0; comedy should not
        result_ids = [r[0] for r in results]
        assert 1 in result_ids
        assert 2 in result_ids
        assert 3 not in result_ids

    def test_clear_removes_all(self):
        self.store.add_documents("MOVIE", [(1, "action movie")])
        self.store.clear()
        assert self.store.document_count("MOVIE") == 0

    def test_document_count(self):
        self.store.add_documents("MOVIE", [(1, "a"), (2, "b"), (3, "c")])
        assert self.store.document_count("MOVIE") == 3

    def test_cross_type_search(self):
        self.store.add_documents("MOVIE", [(1, "KGF rocky bhai")])
        self.store.add_documents("THEATRE", [(100, "KGF cinema theatre")])
        results = self.store.search_all_types("KGF")
        types = [r[0] for r in results]
        assert "MOVIE" in types
        assert "THEATRE" in types


class TestLexicalEngine:
    """Unit tests for LexicalEngine."""

    def setup_method(self):
        self.store = BM25Store()
        self.engine = LexicalEngine(self.store)
        self.store.add_documents("MOVIE", [
            (1, "action thriller batman dark knight"),
            (2, "romance drama love story"),
            (3, "comedy funny humour"),
        ])

    def test_exact_title_match(self):
        results = self.engine.search("batman")
        assert len(results) >= 1
        entity_ids = [r[1] for r in results]
        assert 1 in entity_ids

    def test_partial_title_match(self):
        results = self.engine.search("action")
        assert len(results) >= 1

    def test_no_result_query(self):
        results = self.engine.search("xyzzy impossible query")
        assert results == []

    def test_case_insensitive(self):
        results = self.engine.search("BATMAN")
        assert len(results) >= 1

    def test_entity_type_filter(self):
        self.store.add_documents("THEATRE", [(100, "batman cinema theatre")])
        results = self.engine.search("batman", entity_type="MOVIE")
        entity_types = [r[0] for r in results]
        assert all(et == "MOVIE" for et in entity_types)
