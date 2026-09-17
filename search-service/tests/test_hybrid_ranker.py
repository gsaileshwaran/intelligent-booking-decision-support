"""
PVK Cinemas Search Service — Hybrid Ranker Tests
Authority: SPEC-BASELINE §I.3, P4-DEC-001, NFR-003 (deterministic).
"""

import pytest
from app.search.hybrid_ranker import HybridRanker


class TestHybridRanker:
    """Unit tests for HybridRanker score fusion."""

    def setup_method(self):
        self.ranker = HybridRanker(lexical_weight=0.4, semantic_weight=0.6)

    # TS-HYBRID-001: Deterministic for identical inputs (NFR-003)
    def test_determinism(self):
        lexical = [("MOVIE", 1, 2.5), ("MOVIE", 2, 1.0)]
        semantic = [("MOVIE", 1, 0.9), ("MOVIE", 2, 0.3)]
        result1 = self.ranker.rank(lexical, semantic)
        result2 = self.ranker.rank(lexical, semantic)
        assert result1 == result2

    # TS-HYBRID-002: Weight correctness with single positive candidate
    def test_weight_correctness(self):
        # Only one candidate with positive scores: normalizes to 1.0, preserving perfect candidate confidence
        lexical = [("MOVIE", 1, 5.0)]
        semantic = [("MOVIE", 1, 0.8)]
        results = self.ranker.rank(lexical, semantic)
        assert len(results) == 1
        r = results[0]
        assert r["rank_position"] == 1
        assert r["entity_type"] == "MOVIE"
        assert r["entity_id"] == 1
        assert r["relevance_score"] == 1.0
        assert r["lexical_score"] == 1.0
        assert r["semantic_score"] == 1.0

    # TS-HYBRID-NORM: Normalization edge cases
    def test_normalization_single_positive_candidate_one(self):
        # [1.0] -> [1.0]
        assert HybridRanker._normalize(1.0, 1.0, 1.0) == 1.0

    def test_normalization_single_positive_non_one_candidate(self):
        # [0.8] -> [1.0]
        assert HybridRanker._normalize(0.8, 0.8, 0.8) == 1.0

    def test_normalization_single_zero_candidate(self):
        # [0.0] -> [0.0]
        assert HybridRanker._normalize(0.0, 0.0, 0.0) == 0.0

    def test_normalization_multiple_candidates_standard_min_max(self):
        # [0.2, 0.5, 0.8] -> min=0.2, max=0.8, rng=0.6
        vals = [0.2, 0.5, 0.8]
        norm = [HybridRanker._normalize(v, 0.2, 0.8) for v in vals]
        assert pytest.approx(norm[0], 0.001) == 0.0
        assert pytest.approx(norm[1], 0.001) == 0.5
        assert pytest.approx(norm[2], 0.001) == 1.0

    # TS-HYBRID-003: Tie-breaking by entity_id ascending (deterministic)
    def test_tie_breaking(self):
        # Two candidates with same scores — should be ordered by entity_id
        lexical = [("MOVIE", 10, 1.0), ("MOVIE", 5, 1.0)]
        semantic = [("MOVIE", 10, 0.5), ("MOVIE", 5, 0.5)]
        results = self.ranker.rank(lexical, semantic)
        # Both have same hybrid_score → sorted by entity_id ascending
        assert results[0]["entity_id"] == 5
        assert results[1]["entity_id"] == 10

    # TS-HYBRID-004: Empty lexical + empty semantic returns empty
    def test_both_empty(self):
        results = self.ranker.rank([], [])
        assert results == []

    # TS-HYBRID-005: Lexical-only produces LEXICAL method
    def test_lexical_only_method(self):
        lexical = [("MOVIE", 1, 3.0), ("MOVIE", 2, 1.5)]
        results = self.ranker.rank(lexical, [])
        assert all(r["retrieval_method"] == "LEXICAL" for r in results)

    # TS-HYBRID-006: Semantic-only produces SEMANTIC method
    def test_semantic_only_method(self):
        semantic = [("MOVIE", 1, 0.9), ("MOVIE", 2, 0.5)]
        results = self.ranker.rank([], semantic)
        assert all(r["retrieval_method"] == "SEMANTIC" for r in results)

    # TS-HYBRID-007: Mixed produces HYBRID method for matched candidates
    def test_mixed_method_classification(self):
        lexical = [("MOVIE", 1, 3.0)]
        semantic = [("MOVIE", 1, 0.9), ("MOVIE", 2, 0.5)]
        results = self.ranker.rank(lexical, semantic)
        result_map = {r["entity_id"]: r for r in results}
        assert result_map[1]["retrieval_method"] == "HYBRID"
        assert result_map[2]["retrieval_method"] == "SEMANTIC"

    # TS-HYBRID-008: rank_position is 1-based and sequential
    def test_rank_positions(self):
        lexical = [("MOVIE", 1, 3.0), ("MOVIE", 2, 1.0), ("MOVIE", 3, 0.5)]
        results = self.ranker.rank(lexical, [])
        for idx, r in enumerate(results, start=1):
            assert r["rank_position"] == idx

    # TS-HYBRID-009: top_n respected
    def test_top_n_limit(self):
        lexical = [(f"MOVIE", i, float(10 - i)) for i in range(1, 11)]
        results = self.ranker.rank(lexical, [], top_n=5)
        assert len(results) == 5

    # TS-HYBRID-010: Invalid weight sum raises
    def test_invalid_weights_raise(self):
        with pytest.raises(ValueError):
            HybridRanker(lexical_weight=0.5, semantic_weight=0.6)

    # TS-HYBRID-011: classify_batch_method
    def test_classify_batch_method_hybrid(self):
        results = [
            {"retrieval_method": "HYBRID"},
            {"retrieval_method": "LEXICAL"},
        ]
        assert self.ranker.classify_batch_method(results) == "HYBRID"

    def test_classify_batch_method_uniform(self):
        results = [
            {"retrieval_method": "LEXICAL"},
            {"retrieval_method": "LEXICAL"},
        ]
        assert self.ranker.classify_batch_method(results) == "LEXICAL"
