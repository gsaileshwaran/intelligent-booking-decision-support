"""
PVK Cinemas — Authoritative P14 Test Environment Configuration
Governs network endpoints, hostnames, ports, and service health targets.
"""

import os
from dataclasses import dataclass

@dataclass(frozen=True)
class ServiceEndpoints:
    backend_base_url: str
    frontend_base_url: str
    search_base_url: str
    database_host: str
    database_port: int
    database_name: str

def get_test_environment() -> ServiceEndpoints:
    """
    Resolves the active test environment configuration.
    Defaults to standard development ports as specified in P14 Baseline:
    - Backend: http://localhost:8080/api/v1
    - Frontend: http://localhost:3000
    - Search Service: http://localhost:8001 (CRITICAL: port 8001, NOT 8000)
    - Database: localhost:3306 (pvk_cinemas)
    """
    return ServiceEndpoints(
        backend_base_url=os.getenv("PVK_BACKEND_URL", "http://localhost:8080/api/v1").rstrip("/"),
        frontend_base_url=os.getenv("PVK_FRONTEND_URL", "http://localhost:3000").rstrip("/"),
        search_base_url=os.getenv("PVK_SEARCH_URL", "http://localhost:8001").rstrip("/"),
        database_host=os.getenv("PVK_DB_HOST", "localhost"),
        database_port=int(os.getenv("PVK_DB_PORT", "3306")),
        database_name=os.getenv("PVK_DB_NAME", "pvk_cinemas")
    )

ENV = get_test_environment()
