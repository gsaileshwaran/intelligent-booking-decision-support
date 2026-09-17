"""
PVK Cinemas Search Service — Semantic Engine
Sentence-transformer embedding retrieval over the in-process vector store.
Authority: SPEC-BASELINE §I.3 (Semantic Retrieval), SRS AI-008.
"""

import logging
from typing import Optional
import numpy as np

logger = logging.getLogger(__name__)


class SemanticEngine:
    """
    Executes semantic vector similarity retrieval.
    Uses the shared VectorStore instance (injected).
    Embedding model is injected as a dependency for testability.
    Authority: SPEC-BASE §I.3 — Vector similarity matching between query embeddings
    and pre-computed movie/show document embeddings.
    """

    def __init__(self, vector_store, embedding_model, min_score: float = 0.45) -> None:
        """
        :param vector_store: VectorStore instance
        :param embedding_model: sentence_transformers.SentenceTransformer instance
        :param min_score: minimum raw cosine similarity required to qualify as semantic candidate
        """
        self._vector_store = vector_store
        self._model = embedding_model
        self._min_score = min_score

    def embed_query(self, query: str) -> np.ndarray:
        """
        Compute embedding for a query string.
        Returns 384-dim float32 numpy array (all-MiniLM-L6-v2).
        """
        embedding = self._model.encode(query, convert_to_numpy=True)
        return embedding.astype(np.float32)

    def embed_text(self, text: str) -> np.ndarray:
        """Compute embedding for indexable document text."""
        embedding = self._model.encode(text, convert_to_numpy=True)
        return embedding.astype(np.float32)

    def search(
        self,
        query: str,
        entity_type: Optional[str] = None,
        top_n: int = 20,
        min_score: Optional[float] = None,
    ) -> list[tuple[str, int, float]]:
        """
        Execute semantic retrieval with relevance cutoff.
        Applies raw cosine cutoff before candidate filtering and normalization.
        :param query: normalized query string
        :param entity_type: filter to 'MOVIE' or 'THEATRE', or None for all
        :param top_n: maximum candidates
        :param min_score: minimum raw cosine similarity threshold (defaults to self._min_score)
        :return: list of (entity_type, entity_id, cosine_score) sorted desc
        """
        threshold = self._min_score if min_score is None else min_score
        query_embedding = self.embed_query(query)
        raw_candidates = self._vector_store.search(query_embedding, entity_type=entity_type, top_n=top_n)
        return [c for c in raw_candidates if c[2] >= threshold]
