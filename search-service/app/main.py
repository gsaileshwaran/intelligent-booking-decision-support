"""
PVK Cinemas Search Service — FastAPI Application Factory
Authority: ARCH §11, §17, §18 (Environment Architecture).

Startup sequence:
1. Initialize settings
2. Initialize shared stores (BM25Store, VectorStore)
3. Load embedding model (sentence-transformers)
4. Initialize DbReader (SQLAlchemy connection pool)
5. Initialize DocumentBuilder, SemanticEngine, LexicalEngine, HybridRanker
6. Initialize IndexingPipeline
7. If SEARCH_INDEX_ON_STARTUP=true, run index_all() on startup
8. Register API routers
"""

import logging
import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.store.bm25_store import BM25Store
from app.store.vector_store import VectorStore
from app.indexing.db_reader import DbReader
from app.indexing.document_builder import DocumentBuilder
from app.indexing.pipeline import IndexingPipeline
from app.search.query_processor import QueryProcessor
from app.search.lexical_engine import LexicalEngine
from app.search.semantic_engine import SemanticEngine
from app.search.hybrid_ranker import HybridRanker
from app.api.v1 import health, search, index

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
    stream=sys.stdout,
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI lifespan context manager.
    Initializes all shared service instances on startup.
    """
    logger.info("PVK Cinemas Search Service starting up...")

    # --- Stores ---
    bm25_store = BM25Store()
    vector_store = VectorStore()

    # --- Embedding model ---
    logger.info("Loading embedding model: %s", settings.embedding_model)
    try:
        from sentence_transformers import SentenceTransformer
        embedding_model = SentenceTransformer(settings.embedding_model)
        logger.info("Embedding model loaded successfully")
    except Exception as exc:
        logger.error("Failed to load embedding model: %s", exc)
        # Use a no-op model stub so service can still serve lexical-only search
        embedding_model = _NoOpEmbeddingModel()

    # --- Database reader ---
    db_reader = DbReader(settings.db_url)
    db_available = db_reader.test_connection()
    if not db_available:
        logger.warning(
            "Database connection unavailable at startup — index will be empty until reindex is triggered"
        )

    # --- Domain components ---
    document_builder = DocumentBuilder()
    semantic_engine = SemanticEngine(
        vector_store,
        embedding_model,
        min_score=settings.semantic_min_score,
    )
    lexical_engine = LexicalEngine(bm25_store)
    hybrid_ranker = HybridRanker(
        lexical_weight=settings.lexical_weight,
        semantic_weight=settings.semantic_weight,
    )
    query_processor = QueryProcessor(max_length=settings.max_query_length)

    # --- Pipeline ---
    pipeline = IndexingPipeline(
        db_reader=db_reader,
        document_builder=document_builder,
        bm25_store=bm25_store,
        vector_store=vector_store,
        semantic_engine=semantic_engine,
    )

    # --- Attach to app state for router access ---
    app.state.bm25_store = bm25_store
    app.state.vector_store = vector_store
    app.state.db_reader = db_reader
    app.state.document_builder = document_builder
    app.state.semantic_engine = semantic_engine
    app.state.lexical_engine = lexical_engine
    app.state.hybrid_ranker = hybrid_ranker
    app.state.query_processor = query_processor
    app.state.pipeline = pipeline

    # --- Startup indexing ---
    if settings.index_on_startup and db_available:
        logger.info("Running startup index_all()...")
        try:
            result = pipeline.index_all()
            logger.info("Startup indexing complete: %s", result)
        except Exception as exc:
            logger.error("Startup indexing failed (service will start with empty index): %s", exc)
    else:
        logger.info("Startup indexing skipped (SEARCH_INDEX_ON_STARTUP=%s, db_available=%s)",
                    settings.index_on_startup, db_available)

    logger.info("PVK Cinemas Search Service ready on port %d", settings.service_port)
    yield

    logger.info("PVK Cinemas Search Service shutting down...")


def create_app(enable_lifespan: bool = True) -> FastAPI:
    """
    FastAPI application factory.
    """
    app = FastAPI(
        title="PVK Cinemas Search Service",
        description=(
            "AI-powered cinema search service. Internal use only — not exposed publicly. "
            "Provides hybrid BM25 + semantic retrieval for cinema entities. "
            "Authority: PVK-ARCH-001 §11, PVK-SRS-001 FR-090–FR-098."
        ),
        version="4.0.0",
        lifespan=lifespan if enable_lifespan else None,
        docs_url="/docs",   # Disable in production by setting to None
        redoc_url="/redoc",
    )

    # CORS: only allow internal loopback (service is internal-only)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:8080", "http://localhost:8080"],
        allow_methods=["GET", "POST"],
        allow_headers=["Content-Type"],
    )

    # Register routers
    app.include_router(health.router)
    app.include_router(search.router)
    app.include_router(index.router)

    return app


class _NoOpEmbeddingModel:
    """
    Fallback embedding model stub when sentence-transformers fails to load.
    Returns a zero vector of the expected dimension.
    Enables lexical-only mode graceful degradation.
    """

    def encode(self, text: str, **kwargs):
        import numpy as np
        return np.zeros(384, dtype=np.float32)


app = create_app()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.service_host,
        port=settings.service_port,
        reload=False,
        log_level="info",
    )
