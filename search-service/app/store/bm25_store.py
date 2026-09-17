"""
PVK Cinemas Search Service — BM25 Store
Thread-safe in-process BM25 corpus store.
Authority: SPEC-BASELINE §I.3 (Lexical Retrieval), DECISION-009 (local in-process store).
"""

import threading
from typing import Optional
from rank_bm25 import BM25Okapi


class BM25Store:
    """
    In-process BM25 index backed by rank-bm25 (BM25Okapi).
    Organized by entity type (MOVIE, THEATRE).
    Thread-safe: all mutation operations acquire _lock.

    Design rationale (P4-DEC-001, DECISION-009):
    - rank-bm25 BM25Okapi with k1=1.5, b=0.75 (library defaults)
    - Tokenization: lowercase, whitespace split
    - Index is rebuildable from SEARCH_INDEX_DOCUMENT + MySQL (AI-009)
    """

    def __init__(self) -> None:
        self._lock = threading.RLock()
        # Per entity-type structures
        self._indexes: dict[str, BM25Okapi] = {}
        self._entity_ids: dict[str, list[int]] = {}  # entity_type -> ordered list of entity_ids
        self._corpus: dict[str, list[list[str]]] = {}  # entity_type -> tokenized corpus

    # ------------------------------------------------------------------
    # Indexing
    # ------------------------------------------------------------------

    def add_documents(self, entity_type: str, docs: list[tuple[int, str]]) -> None:
        """
        Build/rebuild BM25 index for an entity type.
        :param entity_type: 'MOVIE' or 'THEATRE'
        :param docs: list of (entity_id, searchable_text) tuples
        """
        tokenized_corpus: list[list[str]] = []
        ids: list[int] = []

        for entity_id, text in docs:
            tokens = self._tokenize(text)
            tokenized_corpus.append(tokens)
            ids.append(entity_id)

        with self._lock:
            self._corpus[entity_type] = tokenized_corpus
            self._entity_ids[entity_type] = ids
            if tokenized_corpus:
                bm = BM25Okapi(tokenized_corpus)
                # Ensure positive IDF floor for small corpora (N <= 2) and high-frequency terms.
                # Classical Robertson BM25 formula yields idf <= 0 when doc frequency >= 50%.
                # In cinema entity retrieval, a matched keyword must always yield positive score.
                for word, idf_val in bm.idf.items():
                    if idf_val <= 0.0:
                        bm.idf[word] = 0.5
                self._indexes[entity_type] = bm
            else:
                self._indexes.pop(entity_type, None)

    def clear(self) -> None:
        """Clear all indexes (used during full rebuild)."""
        with self._lock:
            self._indexes.clear()
            self._entity_ids.clear()
            self._corpus.clear()

    def clear_entity_type(self, entity_type: str) -> None:
        """Clear index for a specific entity type."""
        with self._lock:
            self._indexes.pop(entity_type, None)
            self._entity_ids.pop(entity_type, None)
            self._corpus.pop(entity_type, None)

    # ------------------------------------------------------------------
    # Retrieval
    # ------------------------------------------------------------------

    def search(self, entity_type: str, query: str, top_n: int = 20) -> list[tuple[int, float]]:
        """
        Execute BM25 retrieval for a query against an entity type.
        Returns: list of (entity_id, raw_bm25_score) sorted descending by score.
        Raw scores are NOT normalized here; normalization happens in the hybrid ranker.
        """
        with self._lock:
            index = self._indexes.get(entity_type)
            ids = self._entity_ids.get(entity_type, [])

        if index is None or not ids:
            return []

        query_tokens = self._tokenize(query)
        scores: list[float] = index.get_scores(query_tokens).tolist()

        # Pair entity_id with score, sort descending
        paired = sorted(zip(ids, scores), key=lambda x: x[1], reverse=True)
        # Filter out zero-score results (no match at all)
        paired = [(eid, score) for eid, score in paired if score > 0.0]
        return paired[:top_n]

    def search_all_types(self, query: str, top_n: int = 20) -> list[tuple[str, int, float]]:
        """
        Execute BM25 retrieval across all indexed entity types.
        Returns: list of (entity_type, entity_id, raw_bm25_score) sorted descending.
        """
        all_results: list[tuple[str, int, float]] = []
        with self._lock:
            entity_types = list(self._indexes.keys())
        for entity_type in entity_types:
            hits = self.search(entity_type, query, top_n)
            for entity_id, score in hits:
                all_results.append((entity_type, entity_id, score))
        all_results.sort(key=lambda x: x[2], reverse=True)
        return all_results[:top_n]

    # ------------------------------------------------------------------
    # Diagnostics
    # ------------------------------------------------------------------

    def document_count(self, entity_type: Optional[str] = None) -> int:
        """Return number of indexed documents for an entity type (or total)."""
        with self._lock:
            if entity_type:
                return len(self._entity_ids.get(entity_type, []))
            return sum(len(v) for v in self._entity_ids.values())

    def indexed_entity_types(self) -> list[str]:
        with self._lock:
            return list(self._indexes.keys())

    # ------------------------------------------------------------------
    # Prefix / Partial Retrieval
    # ------------------------------------------------------------------

    def search_prefix(self, entity_type: str, query: str, top_n: int = 20) -> list[tuple[int, float]]:
        """
        Prefix-aware retrieval: matches documents whose tokens START WITH any query token.
        Used as a complement to BM25 when exact-token BM25 returns 0 results.
        Scores are synthetic (0.5 base per matching prefix token) — always lower than exact BM25.
        :param entity_type: 'MOVIE' or 'THEATRE'
        :param query: normalized query string
        :param top_n: max candidates
        :return: list of (entity_id, score) sorted descending
        """
        with self._lock:
            corpus = self._corpus.get(entity_type, [])
            ids = self._entity_ids.get(entity_type, [])

        if not corpus or not ids:
            return []

        query_tokens = self._tokenize(query)
        # Only expand tokens shorter than 8 chars — longer tokens are specific enough for BM25
        prefix_tokens = [t for t in query_tokens if len(t) >= 2]
        if not prefix_tokens:
            return []

        scored: list[tuple[int, float]] = []
        for entity_id, doc_tokens in zip(ids, corpus):
            score = 0.0
            for qt in prefix_tokens:
                for dt in doc_tokens:
                    if dt.startswith(qt) and dt != qt:
                        # Partial prefix match — lower score than exact
                        score += 0.5 * (len(qt) / len(dt))  # longer prefix = higher match quality
                    elif dt == qt:
                        # Exact token match in corpus (already covered by BM25, but count it)
                        score += 0.3
            if score > 0.0:
                scored.append((entity_id, score))

        scored.sort(key=lambda x: x[1], reverse=True)
        return scored[:top_n]

    def search_prefix_all_types(self, query: str, top_n: int = 20) -> list[tuple[str, int, float]]:
        """Prefix retrieval across all indexed entity types."""
        all_results: list[tuple[str, int, float]] = []
        with self._lock:
            entity_types = list(self._corpus.keys())
        for entity_type in entity_types:
            hits = self.search_prefix(entity_type, query, top_n)
            for entity_id, score in hits:
                all_results.append((entity_type, entity_id, score))
        all_results.sort(key=lambda x: x[2], reverse=True)
        return all_results[:top_n]

    # ------------------------------------------------------------------
    # Internal
    # ------------------------------------------------------------------

    @staticmethod
    def _tokenize(text: str) -> list[str]:
        """Simple whitespace tokenizer with lowercase normalization."""
        return text.lower().split()
