"""
PVK Cinemas Search Service — Vector Store
Thread-safe in-process embedding store using numpy arrays.
Authority: SPEC-BASELINE §I.3 (Semantic Retrieval), DECISION-009 (local in-process, rebuildable).
"""

import threading
from typing import Optional
import numpy as np


class VectorStore:
    """
    In-process vector store for pre-computed sentence-transformer embeddings.
    Keys: 'entity_type:entity_id' strings.
    Vectors: 384-dimensional float32 numpy arrays (all-MiniLM-L6-v2 output).

    Design rationale (P4-DEC-001, DECISION-009):
    - All vectors stored in a dict; cosine similarity computed on demand
    - Rebuildable from SEARCH_INDEX_DOCUMENT + MySQL (AI-009)
    - Thread-safe via RLock
    - No external vector database dependency (DECISION-009 defers to future phase)
    """

    def __init__(self) -> None:
        self._lock = threading.RLock()
        # key -> embedding ndarray
        self._embeddings: dict[str, np.ndarray] = {}
        # key -> (entity_type, entity_id)
        self._metadata: dict[str, tuple[str, int]] = {}

    # ------------------------------------------------------------------
    # Write
    # ------------------------------------------------------------------

    def add(self, entity_type: str, entity_id: int, embedding: np.ndarray) -> None:
        """Store a normalized embedding for an entity."""
        key = f"{entity_type}:{entity_id}"
        # Normalize to unit vector for efficient cosine similarity via dot product
        norm = np.linalg.norm(embedding)
        unit = embedding / norm if norm > 0.0 else embedding
        with self._lock:
            self._embeddings[key] = unit.astype(np.float32)
            self._metadata[key] = (entity_type, entity_id)

    def add_batch(self, entity_type: str, docs: list[tuple[int, np.ndarray]]) -> None:
        """Batch add embeddings for a single entity type."""
        for entity_id, embedding in docs:
            self.add(entity_type, entity_id, embedding)

    def clear(self) -> None:
        """Remove all stored embeddings."""
        with self._lock:
            self._embeddings.clear()
            self._metadata.clear()

    def clear_entity_type(self, entity_type: str) -> None:
        """Remove all embeddings for a specific entity type."""
        prefix = f"{entity_type}:"
        with self._lock:
            keys_to_remove = [k for k in self._embeddings if k.startswith(prefix)]
            for k in keys_to_remove:
                del self._embeddings[k]
                del self._metadata[k]

    def remove(self, entity_type: str, entity_id: int) -> None:
        """Remove a specific embedding (used for DELETED index status)."""
        key = f"{entity_type}:{entity_id}"
        with self._lock:
            self._embeddings.pop(key, None)
            self._metadata.pop(key, None)

    # ------------------------------------------------------------------
    # Read
    # ------------------------------------------------------------------

    def search(
        self,
        query_embedding: np.ndarray,
        entity_type: Optional[str] = None,
        top_n: int = 20,
    ) -> list[tuple[str, int, float]]:
        """
        Compute cosine similarity between query_embedding and all stored embeddings.
        :param query_embedding: 384-dim query vector (will be normalized internally)
        :param entity_type: filter to specific entity type, or None for all types
        :param top_n: maximum results to return
        :return: list of (entity_type, entity_id, cosine_score) sorted descending
        """
        # Normalize query vector
        norm = np.linalg.norm(query_embedding)
        query_unit = query_embedding / norm if norm > 0.0 else query_embedding
        query_unit = query_unit.astype(np.float32)

        results: list[tuple[str, int, float]] = []

        with self._lock:
            items = list(self._metadata.items())
            embeddings = self._embeddings.copy()

        for key, (etype, eid) in items:
            if entity_type is not None and etype != entity_type:
                continue
            vec = embeddings.get(key)
            if vec is None:
                continue
            score = float(np.dot(query_unit, vec))  # cosine similarity (unit vectors)
            results.append((etype, eid, score))

        results.sort(key=lambda x: x[2], reverse=True)
        return results[:top_n]

    def document_count(self, entity_type: Optional[str] = None) -> int:
        """Return count of stored embeddings."""
        with self._lock:
            if entity_type:
                prefix = f"{entity_type}:"
                return sum(1 for k in self._embeddings if k.startswith(prefix))
            return len(self._embeddings)

    def has_entity(self, entity_type: str, entity_id: int) -> bool:
        """Check if an embedding exists for the given entity."""
        key = f"{entity_type}:{entity_id}"
        with self._lock:
            return key in self._embeddings
