"""
PVK Cinemas Search Service — Search API Endpoint
Internal contract: POST /internal/v1/search
Authority: API-SPEC §23, ARCH §11, PLAN §6.
"""

import logging
from fastapi import APIRouter, Depends, HTTPException, status, Request

from app.domain.search_request import SearchRequest
from app.domain.search_result import CandidateResult, SearchResponse
from app.search.query_processor import QueryProcessor
from app.search.lexical_engine import LexicalEngine
from app.search.semantic_engine import SemanticEngine
from app.search.hybrid_ranker import HybridRanker

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post(
    "/internal/v1/search",
    response_model=SearchResponse,
    tags=["search"],
    summary="Execute hybrid cinema search",
    description=(
        "Internal endpoint consumed by Spring Boot backend only. "
        "NOT publicly exposed. Executes BM25 lexical + semantic vector hybrid retrieval "
        "and returns ranked candidates. Spring Boot resolves authoritative details from MySQL."
    ),
)
async def search(request: SearchRequest, req: Request) -> SearchResponse:
    """
    Execute hybrid search for a normalized query.
    Fallback chain: hybrid BM25+semantic → prefix expansion → empty.
    Authority: FR-090, FR-091, FR-092, AI-008.
    """
    # Retrieve shared service instances from app state
    query_processor: QueryProcessor = req.app.state.query_processor
    lexical_engine: LexicalEngine = req.app.state.lexical_engine
    semantic_engine: SemanticEngine = req.app.state.semantic_engine
    hybrid_ranker: HybridRanker = req.app.state.hybrid_ranker
    bm25_store = req.app.state.bm25_store

    # Validate and normalize query
    try:
        normalized_query = query_processor.process(request.query)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        )

    top_n = min(request.max_results, 50)

    # Execute retrieval
    try:
        lexical_hits = lexical_engine.search(normalized_query, top_n=top_n * 2)
    except Exception as exc:
        logger.error("Lexical engine error: %s", exc)
        lexical_hits = []

    try:
        semantic_hits = semantic_engine.search(normalized_query, top_n=top_n * 2)
    except Exception as exc:
        logger.error("Semantic engine error: %s", exc)
        semantic_hits = []

    # Hybrid fusion and ranking
    ranked = hybrid_ranker.rank(lexical_hits, semantic_hits, top_n=top_n)

    # === PREFIX EXPANSION FALLBACK ===
    # If hybrid returns 0 results, try prefix-based matching.
    # This handles partial queries like "cine" -> "cineplex", "interstel" -> "interstellar".
    if not ranked:
        logger.info("Hybrid returned 0 results for '%s'; attempting prefix expansion", normalized_query)
        prefix_hits = bm25_store.search_prefix_all_types(normalized_query, top_n=top_n)
        if prefix_hits:
            # Convert prefix hits to ranked dicts compatible with CandidateResult
            ranked = [
                {
                    "entity_type": etype,
                    "entity_id": eid,
                    "rank_position": idx + 1,
                    "retrieval_method": "LEXICAL",
                    "relevance_score": round(score, 6),
                    "lexical_score": round(score, 6),
                    "semantic_score": 0.0,
                }
                for idx, (etype, eid, score) in enumerate(prefix_hits)
            ]
            logger.info("Prefix expansion found %d results for '%s'", len(ranked), normalized_query)

    # Build response
    candidates = [
        CandidateResult(
            entity_type=r["entity_type"],
            entity_id=r["entity_id"],
            rank_position=r["rank_position"],
            retrieval_method=r["retrieval_method"],
            relevance_score=r["relevance_score"],
            lexical_score=r["lexical_score"],
            semantic_score=r["semantic_score"],
        )
        for r in ranked
    ]

    primary_method = hybrid_ranker.classify_batch_method(ranked)

    logger.info(
        "Search query='%s' results=%d method=%s",
        normalized_query[:50], len(candidates), primary_method,
    )

    return SearchResponse(
        results=candidates,
        total=len(candidates),
        query_text=normalized_query,
        retrieval_method_used=primary_method,
    )
