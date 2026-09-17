"""
PVK Cinemas Search Service — Indexing Pipeline
Orchestrates full-index and incremental rebuilds from MySQL operational data.

Boundary Rules (Authority: ARCH §11, BR-009):
- Reads operational tables (MOVIE, THEATRE, CITY, GENRE, LANGUAGE) via DbReader
- Writes ONLY to SEARCH_INDEX_DOCUMENT (derived projection, not operational)
- Updates in-process BM25Store and VectorStore
- NEVER mutates USER, SHOW, SHOW_SEAT, AUDIT_LOG or any other operational entity
"""

import logging
from datetime import datetime, timezone

from app.indexing.db_reader import DbReader
from app.indexing.document_builder import DocumentBuilder, PIPELINE_VERSION
from app.store.bm25_store import BM25Store
from app.store.vector_store import VectorStore

logger = logging.getLogger(__name__)


class IndexingPipeline:
    """
    Orchestrates the full indexing pipeline:
    1. Read active movies and theatres from MySQL (DbReader)
    2. Build BM25 searchable text (DocumentBuilder)
    3. Build dense vector embedding (SemanticEngine / SentenceTransformer)
    4. Upsert document into SEARCH_INDEX_DOCUMENT in MySQL
    5. Load into in-process BM25Store and VectorStore

    Authority: PVK-ARCH-001 §11, PVK-SRS-001 AI-009, FR-096–FR-098.
    """

    def __init__(
        self,
        db_reader: DbReader,
        bm25_store: BM25Store,
        vector_store: VectorStore,
        document_builder: DocumentBuilder,
        semantic_engine,
    ) -> None:
        self._db = db_reader
        self._bm25 = bm25_store
        self._vectors = vector_store
        self._builder = document_builder
        self._semantic = semantic_engine
        self._last_indexed_at: datetime | None = None
        self._indexed_count: int = 0

    def index_all(self) -> dict[str, int]:
        """
        Execute full reindex of all active movies and theatres.
        Safe to call concurrently or repeatedly (idempotent).
        Returns dict with counts of indexed entities.
        """
        logger.info(
            "IndexingPipeline.index_all() started — pipeline_version=%s",
            PIPELINE_VERSION,
        )

        # Clear in-process stores before rebuild
        self._bm25.clear()
        self._vectors.clear()

        movie_count = self._index_movies()
        theatre_count = self._index_theatres()

        total = movie_count + theatre_count
        self._indexed_count = total
        self._last_indexed_at = datetime.now(timezone.utc)

        logger.info(
            "IndexingPipeline.index_all() complete: movies=%d, theatres=%d, total=%d",
            movie_count, theatre_count, total,
        )
        return {"movies": movie_count, "theatres": theatre_count, "total": total}

    def _index_movies(self) -> int:
        """Index all movies into BM25Store, VectorStore, and SEARCH_INDEX_DOCUMENT."""
        rows = self._db.fetch_movies()
        if not rows:
            logger.warning("No movies found for indexing")
            return 0

        bm25_docs: list[tuple[int, str]] = []
        vector_docs: list[tuple[int, "numpy.ndarray"]] = []  # type: ignore

        for row in rows:
            doc = self._builder.build_movie_document(row)
            bm25_docs.append((doc["entity_id"], doc["searchable_text"]))

            # Compute embedding
            try:
                embedding = self._semantic.embed_text(doc["searchable_text"])
                vector_docs.append((doc["entity_id"], embedding))
            except Exception as exc:
                logger.error(
                    "Embedding failed for MOVIE entity_id=%s: %s",
                    doc["entity_id"], exc
                )

            # Upsert SEARCH_INDEX_DOCUMENT
            try:
                self._db.upsert_search_index_document(
                    entity_type="MOVIE",
                    entity_id=doc["entity_id"],
                    searchable_text=doc["searchable_text"],
                    metadata_json=doc["metadata_json"],
                    pipeline_version=PIPELINE_VERSION,
                )
            except Exception as exc:
                logger.error(
                    "Failed to upsert SEARCH_INDEX_DOCUMENT for MOVIE entity_id=%s: %s",
                    doc["entity_id"], exc
                )

        # Batch update in-process stores
        self._bm25.add_documents("MOVIE", bm25_docs)
        self._vectors.add_batch("MOVIE", vector_docs)

        logger.info("Indexed %d MOVIE documents", len(rows))
        return len(rows)

    def _index_theatres(self) -> int:
        """Index all theatres into BM25Store, VectorStore, and SEARCH_INDEX_DOCUMENT."""
        rows = self._db.fetch_theatres()
        if not rows:
            logger.warning("No theatres found for indexing")
            return 0

        bm25_docs: list[tuple[int, str]] = []
        vector_docs: list[tuple[int, "numpy.ndarray"]] = []  # type: ignore

        for row in rows:
            doc = self._builder.build_theatre_document(row)
            bm25_docs.append((doc["entity_id"], doc["searchable_text"]))

            # Compute embedding
            try:
                embedding = self._semantic.embed_text(doc["searchable_text"])
                vector_docs.append((doc["entity_id"], embedding))
            except Exception as exc:
                logger.error(
                    "Embedding failed for THEATRE entity_id=%s: %s",
                    doc["entity_id"], exc
                )

            # Upsert SEARCH_INDEX_DOCUMENT
            try:
                self._db.upsert_search_index_document(
                    entity_type="THEATRE",
                    entity_id=doc["entity_id"],
                    searchable_text=doc["searchable_text"],
                    metadata_json=doc["metadata_json"],
                    pipeline_version=PIPELINE_VERSION,
                )
            except Exception as exc:
                logger.error(
                    "Failed to upsert SEARCH_INDEX_DOCUMENT for THEATRE entity_id=%s: %s",
                    doc["entity_id"], exc
                )

        # Batch update in-process stores
        self._bm25.add_documents("THEATRE", bm25_docs)
        self._vectors.add_batch("THEATRE", vector_docs)

        logger.info("Indexed %d THEATRE documents", len(rows))
        return len(rows)

    @property
    def last_indexed_at(self) -> datetime | None:
        return self._last_indexed_at

    @property
    def indexed_count(self) -> int:
        return self._indexed_count
