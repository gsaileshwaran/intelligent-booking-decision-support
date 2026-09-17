"""
PVK Cinemas Search Service — Lexical Engine
BM25 retrieval over the in-process BM25 corpus store.
Authority: SPEC-BASELINE §I.3 (Lexical Retrieval), SRS AI-008.
"""

from app.store.bm25_store import BM25Store


class LexicalEngine:
    """
    Executes BM25-based keyword retrieval.
    Uses the shared BM25Store instance (injected).
    Returns raw (un-normalized) BM25 scores; normalization is done by HybridRanker.
    Authority: SPEC-BASE §I.3 — Keyword/BM25 token matching over titles, genres, languages, theatre names.
    """

    def __init__(self, bm25_store: BM25Store) -> None:
        self._store = bm25_store

    def search(
        self,
        query: str,
        entity_type: str | None = None,
        top_n: int = 20,
    ) -> list[tuple[str, int, float]]:
        """
        Execute lexical BM25 retrieval.
        :param query: normalized query string
        :param entity_type: filter to 'MOVIE' or 'THEATRE', or None for all types
        :param top_n: maximum candidates
        :return: list of (entity_type, entity_id, raw_bm25_score) sorted desc
        """
        if entity_type:
            raw_hits = self._store.search(entity_type, query, top_n=top_n)
            return [(entity_type, eid, score) for eid, score in raw_hits]
        else:
            return self._store.search_all_types(query, top_n=top_n)
