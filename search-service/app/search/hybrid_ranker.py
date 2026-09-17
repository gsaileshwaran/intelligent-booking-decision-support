"""
PVK Cinemas Search Service — Hybrid Ranker
Score fusion combining normalized lexical (BM25) and semantic (cosine) scores.
Authority: SPEC-BASELINE §I.3 (Hybrid Retrieval), SRS AI-008, P4-DEC-001 (weights).
"""

import logging
from dataclasses import dataclass
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class _Candidate:
    entity_type: str
    entity_id: int
    lexical_score_raw: float = 0.0
    semantic_score_raw: float = 0.0
    lexical_score_norm: float = 0.0
    semantic_score_norm: float = 0.0
    hybrid_score: float = 0.0


class HybridRanker:
    """
    Combines lexical and semantic retrieval results into a single ranked list.

    Algorithm (P4-DEC-001):
    1. Collect candidates from lexical engine (raw BM25 scores)
    2. Collect candidates from semantic engine (raw cosine scores)
    3. Union all candidates by (entity_type, entity_id)
    4. Normalize lexical scores to [0.0, 1.0] via min-max
    5. Normalize semantic scores to [0.0, 1.0] via min-max
    6. Compute: hybrid_score = (LEXICAL_WEIGHT * lexical_norm) + (SEMANTIC_WEIGHT * semantic_norm)
    7. Sort descending by hybrid_score; ties broken by entity_id ascending (deterministic)
    8. Assign 1-based rank positions
    9. Classify retrieval_method per result: LEXICAL | SEMANTIC | HYBRID

    Authority: NFR-003 (deterministic for identical inputs).
    """

    def __init__(self, lexical_weight: float = 0.4, semantic_weight: float = 0.6) -> None:
        if abs(lexical_weight + semantic_weight - 1.0) > 1e-6:
            raise ValueError("lexical_weight + semantic_weight must equal 1.0")
        self.lexical_weight = lexical_weight
        self.semantic_weight = semantic_weight

    def rank(
        self,
        lexical_hits: list[tuple[str, int, float]],  # (entity_type, entity_id, raw_bm25)
        semantic_hits: list[tuple[str, int, float]],  # (entity_type, entity_id, cosine_score)
        top_n: int = 20,
    ) -> list[dict]:
        """
        Fuse lexical and semantic results into ranked candidates.
        :return: list of ranked dicts with keys:
          entity_type, entity_id, rank_position, retrieval_method,
          relevance_score, lexical_score, semantic_score
        """
        # Build candidate map
        candidates: dict[tuple[str, int], _Candidate] = {}

        for entity_type, entity_id, score in lexical_hits:
            key = (entity_type, entity_id)
            if key not in candidates:
                candidates[key] = _Candidate(entity_type=entity_type, entity_id=entity_id)
            candidates[key].lexical_score_raw = score

        for entity_type, entity_id, score in semantic_hits:
            key = (entity_type, entity_id)
            if key not in candidates:
                candidates[key] = _Candidate(entity_type=entity_type, entity_id=entity_id)
            candidates[key].semantic_score_raw = score

        if not candidates:
            return []

        cand_list = list(candidates.values())

        # Normalize lexical scores
        lex_scores = [c.lexical_score_raw for c in cand_list]
        lex_min, lex_max = min(lex_scores), max(lex_scores)
        for c in cand_list:
            c.lexical_score_norm = self._normalize(c.lexical_score_raw, lex_min, lex_max)

        # Normalize semantic scores
        sem_scores = [c.semantic_score_raw for c in cand_list]
        sem_min, sem_max = min(sem_scores), max(sem_scores)
        for c in cand_list:
            c.semantic_score_norm = self._normalize(c.semantic_score_raw, sem_min, sem_max)

        # Compute hybrid score
        for c in cand_list:
            c.hybrid_score = (
                self.lexical_weight * c.lexical_score_norm
                + self.semantic_weight * c.semantic_score_norm
            )

        # Sort: descending hybrid_score, then ascending entity_id for determinism
        cand_list.sort(key=lambda c: (-c.hybrid_score, c.entity_id))

        # Determine retrieval method for each result
        results = []
        for rank_idx, c in enumerate(cand_list[:top_n], start=1):
            method = self._classify_method(c)
            results.append({
                "entity_type": c.entity_type,
                "entity_id": c.entity_id,
                "rank_position": rank_idx,
                "retrieval_method": method,
                "relevance_score": round(c.hybrid_score, 6),
                "lexical_score": round(c.lexical_score_norm, 6),
                "semantic_score": round(c.semantic_score_norm, 6),
            })

        return results

    def classify_batch_method(self, results: list[dict]) -> str:
        """
        Classify the primary retrieval method for the overall batch.
        Authority: FR-091 — retrieval_method recorded per result.
        """
        if not results:
            return "HYBRID"
        methods = {r["retrieval_method"] for r in results}
        if len(methods) == 1:
            return methods.pop()
        return "HYBRID"

    @staticmethod
    def _normalize(value: float, min_val: float, max_val: float) -> float:
        """Min-max normalization to [0.0, 1.0]. Returns 1.0 if single positive value, 0.0 if zero."""
        rng = max_val - min_val
        if rng == 0.0:
            return 1.0 if value > 0.0 else 0.0
        return (value - min_val) / rng

    @staticmethod
    def _classify_method(c: _Candidate) -> str:
        """Classify retrieval method based on which scores contributed."""
        has_lexical = c.lexical_score_raw > 0.0
        has_semantic = c.semantic_score_raw > 0.0
        if has_lexical and has_semantic:
            return "HYBRID"
        elif has_lexical:
            return "LEXICAL"
        elif has_semantic:
            return "SEMANTIC"
        return "HYBRID"
