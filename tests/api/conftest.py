"""
PVK Cinemas — Pytest Fixtures for API Testing
Provides shared fixtures for P14 API regression tests.
"""

import pytest
from tests.api.api_client import PvkApiClient
from tests.config.test_environment import ENV
from tests.config.test_accounts import ACCOUNTS
from tests.fixtures.test_fixtures import FixtureRegistry, ensure_canonical_test_accounts

@pytest.fixture(scope="session", autouse=True)
def setup_test_database():
    """
    Session-wide database fixture ensuring all required testing personas,
    theatres, and role assignments are deterministically present.
    """
    ensure_canonical_test_accounts()

@pytest.fixture(scope="session")
def api_base_url():
    return ENV.backend_base_url

@pytest.fixture(scope="session")
def client(api_base_url):
    """
    Session-wide unauthenticated API client.
    """
    return PvkApiClient(base_url=api_base_url)

@pytest.fixture(scope="function")
def fixture_registry():
    """
    Function-scoped fixture tracking registry.
    """
    registry = FixtureRegistry()
    yield registry
    registry.clear()

@pytest.fixture(scope="function")
def client_anonymous(api_base_url):
    c = PvkApiClient(base_url=api_base_url)
    c.login_as("anonymous")
    return c

@pytest.fixture(scope="function")
def client_customer(api_base_url):
    c = PvkApiClient(base_url=api_base_url)
    c.login_as("customer")
    return c

@pytest.fixture(scope="function")
def client_manager_a(api_base_url):
    c = PvkApiClient(base_url=api_base_url)
    c.login_as("manager_a")
    return c

@pytest.fixture(scope="function")
def client_manager_b(api_base_url):
    c = PvkApiClient(base_url=api_base_url)
    c.login_as("manager_b")
    return c

@pytest.fixture(scope="function")
def client_super_admin(api_base_url):
    c = PvkApiClient(base_url=api_base_url)
    c.login_as("super_admin")
    return c

