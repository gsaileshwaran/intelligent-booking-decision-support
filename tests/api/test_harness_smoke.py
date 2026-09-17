"""
PVK Cinemas — P14-A Test Infrastructure Smoke Verification
Validates that all test-harness components, catalog invariants,
account strategies, and envelope validators operate correctly.
"""

import pytest
import requests
from unittest.mock import MagicMock
from tests.config.endpoints_catalog import AUTHORITATIVE_53_OPERATIONS, verify_catalog_integrity
from tests.config.test_accounts import ACCOUNTS, RoleType
from tests.config.test_environment import ENV
from tests.fixtures.test_fixtures import FixtureRegistry
from tests.api.api_client import PvkApiClient, ApiResponseValidationError

class TestInfrastructureBasics:

    def test_01_catalog_has_exactly_53_operations(self):
        """Verify the 53-operation regression catalog invariant."""
        result = verify_catalog_integrity()
        assert result["total_operations"] == 53
        assert result["total_controllers"] == 17
        assert result["forbidden_endpoints"] == 0
        assert result["status"] == "PASS"

    def test_02_test_accounts_strategy(self):
        """Verify the 5 distinct test personas and their scope isolation."""
        assert len(ACCOUNTS) == 5
        assert "anonymous" in ACCOUNTS
        assert "customer" in ACCOUNTS
        assert "manager_a" in ACCOUNTS
        assert "manager_b" in ACCOUNTS
        assert "super_admin" in ACCOUNTS

        # Manager scope separation
        mgr_a = ACCOUNTS["manager_a"]
        mgr_b = ACCOUNTS["manager_b"]
        assert mgr_a.assigned_theatre_id != mgr_b.assigned_theatre_id
        assert mgr_a.assigned_theatre_id == 1
        assert mgr_b.assigned_theatre_id == 2

    def test_03_test_environment_ports(self):
        """Verify correct ports are configured (specifically search port 8001, not 8000)."""
        assert ":8080" in ENV.backend_base_url
        assert ":3000" in ENV.frontend_base_url
        assert ":8001" in ENV.search_base_url, "CRITICAL: Search service must be configured on port 8001, not 8000"
        assert ENV.database_port == 3306

    def test_04_fixture_generator_uniqueness(self):
        """Verify fixture registry produces unique, isolated test entity data."""
        reg1 = FixtureRegistry("run1")
        reg2 = FixtureRegistry("run2")
        assert reg1.get_test_prefix() != reg2.get_test_prefix()

        email1 = reg1.generate_unique_email("test")
        email2 = reg1.generate_unique_email("test")
        assert email1 != email2
        assert "@pvkcinemas.test" in email1

        movie_payload = reg1.generate_movie_payload()
        assert "title" in movie_payload
        assert "genreIds" in movie_payload
        assert "languageIds" in movie_payload

    def test_05_client_envelope_validation_success(self):
        """Verify API client success envelope assertion logic."""
        mock_resp = MagicMock(spec=requests.Response)
        mock_resp.status_code = 200
        mock_resp.text = '{"success": true, "data": {"id": 10}, "timestamp": "2026-09-05T20:00:00Z"}'
        mock_resp.json.return_value = {"success": True, "data": {"id": 10}, "timestamp": "2026-09-05T20:00:00Z"}

        data = PvkApiClient.assert_success_envelope(mock_resp, expected_status=200)
        assert data["data"]["id"] == 10

    def test_06_client_envelope_validation_failure_on_mismatched_status(self):
        """Verify API client raises error on unexpected status code."""
        mock_resp = MagicMock(spec=requests.Response)
        mock_resp.status_code = 500
        mock_resp.text = "Internal Server Error"

        with pytest.raises(ApiResponseValidationError) as exc:
            PvkApiClient.assert_success_envelope(mock_resp, expected_status=200)
        assert "Expected status 200, but received 500" in str(exc.value)

    def test_07_client_error_envelope_validation(self):
        """Verify API client error envelope assertion logic."""
        mock_resp = MagicMock(spec=requests.Response)
        mock_resp.status_code = 403
        mock_resp.text = '{"code": "ACCESS_DENIED", "message": "Cross-theatre access prohibited", "status": 403}'
        mock_resp.json.return_value = {"code": "ACCESS_DENIED", "message": "Cross-theatre access prohibited", "status": 403}

        error_body = PvkApiClient.assert_error_envelope(mock_resp, expected_status=403, expected_code="ACCESS_DENIED")
        assert error_body["code"] == "ACCESS_DENIED"

    def test_08_client_token_header_injection(self):
        """Verify Bearer token injection and clearing in HTTP headers."""
        client = PvkApiClient("http://mock-server/api/v1")
        assert "Authorization" not in client.session.headers

        client.set_token("test-jwt-token")
        assert client.session.headers.get("Authorization") == "Bearer test-jwt-token"

        client.set_token(None)
        assert "Authorization" not in client.session.headers
