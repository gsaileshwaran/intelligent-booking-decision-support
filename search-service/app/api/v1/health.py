"""
PVK Cinemas Search Service — Health Endpoint
Authority: ARCH §10 (health/readiness endpoints).
"""

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


@router.get("/health", response_model=HealthResponse, tags=["health"])
async def health_check() -> HealthResponse:
    """
    Liveness probe endpoint.
    Returns 200 OK with service status.
    """
    return HealthResponse(
        status="UP",
        service="pvk-cinemas-search-service",
        version="4.0.0",
    )
