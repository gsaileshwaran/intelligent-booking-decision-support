"""
PVK Cinemas — Pytest Fixtures for Security Testing
Re-exports fixtures from tests.api.conftest for security hardening tests.
"""

from tests.api.conftest import (
    setup_test_database,
    api_base_url,
    client,
    fixture_registry,
    client_anonymous,
    client_customer,
    client_manager_a,
    client_manager_b,
    client_super_admin,
)

__all__ = [
    "setup_test_database",
    "api_base_url",
    "client",
    "fixture_registry",
    "client_anonymous",
    "client_customer",
    "client_manager_a",
    "client_manager_b",
    "client_super_admin",
]
