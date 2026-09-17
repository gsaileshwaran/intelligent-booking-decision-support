"""
PVK Cinemas Search Service — Index Management Endpoints
Internal contracts: POST /internal/v1/index, GET /internal/v1/index/status
Authority: API-SPEC §23, ARCH §11, FR-096, FR-097, FR-098, AI-009.
"""

import logging
from fastapi import APIRouter, BackgroundTasks, Request
from pydantic import BaseModel

from app.domain.index_document import IndexStatusResponse

logger = logging.getLogger(__name__)

router = APIRouter()


class ReindexResponse(BaseModel):
    triggered: bool
    message: str


@router.post(
    "/internal/v1/index",
    response_model=ReindexResponse,
    tags=["index"],
    summary="Trigger full search index rebuild",
    description=(
        "Rebuilds BM25 and vector indexes from authoritative MySQL data. "
        "Idempotent: rerunning produces the same logical index state. "
        "Called by Spring Boot POST /admin/search/reindex."
    ),
)
async def reindex(background_tasks: BackgroundTasks, req: Request) -> ReindexResponse:
    """
    Trigger asynchronous full reindex.
    Authority: AI-009 (rebuildable from authoritative source data).
    """
    pipeline = req.app.state.pipeline

    def _run_reindex():
        try:
            result = pipeline.index_all()
            logger.info("Reindex complete: %s", result)
        except Exception as exc:
            logger.error("Reindex failed: %s", exc)

    background_tasks.add_task(_run_reindex)

    return ReindexResponse(
        triggered=True,
        message="Full reindex triggered asynchronously",
    )


@router.get(
    "/internal/v1/index/status",
    response_model=IndexStatusResponse,
    tags=["index"],
    summary="Get search index statistics",
    description="Returns SEARCH_INDEX_DOCUMENT aggregate counts by status.",
)
async def index_status(req: Request) -> IndexStatusResponse:
    """
    Return index status from MySQL SEARCH_INDEX_DOCUMENT and in-process stores.
    Authority: FR-097, FR-098, NFR-012.
    """
    db_reader = req.app.state.db_reader
    bm25_store = req.app.state.bm25_store
    pipeline = req.app.state.pipeline

    try:
        stats = db_reader.get_index_stats()
        last_indexed = db_reader.get_last_indexed_at()
    except Exception as exc:
        logger.error("Failed to read index stats from DB: %s", exc)
        stats = {"ACTIVE": 0, "STALE": 0, "DELETED": 0}
        last_indexed = None

    return IndexStatusResponse(
        total_documents=sum(stats.values()),
        active_documents=stats.get("ACTIVE", 0),
        stale_documents=stats.get("STALE", 0),
        deleted_documents=stats.get("DELETED", 0),
        pipeline_version="v4.0",
        last_indexed_at=last_indexed,
    )
