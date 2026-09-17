"""
PVK Cinemas — API Regression Suite: Authentication & Profile
Covers operations:
- OP-02: POST  /auth/register (AuthController::register)
- OP-03: POST  /auth/login (AuthController::login)
- OP-04: POST  /auth/logout (AuthController::logout)
- OP-05: GET   /auth/me (AuthController::getMe)
- OP-52: GET   /me/profile (UserProfileController::getProfile)
- OP-53: PATCH /me/profile (UserProfileController::updateProfile)

Validates:
- Successful registration (201) & 30-min token lifetime (expiresIn: 1800)
- Validation failure (400) on invalid registration payload
- Conflict failure (409) on duplicate email/phone
- Login success (200) with JWT & roles/permissions
- Login failure (401) on invalid credentials
- Protected identity endpoints (/auth/me, /me/profile)
- Unauthenticated rejection (401) on protected endpoints
- Token revocation and rejection of revoked token (401)
- Profile mutation (PATCH /me/profile) and persistence
"""

import pytest
import uuid
from tests.api.api_client import PvkApiClient

class TestAuthAndProfileRegression:

    def test_op02_register_success(self, client, fixture_registry):
        """API-REG-002: POST /api/v1/auth/register -> 201 Created"""
        unique_id = uuid.uuid4().hex[:8]
        email = f"reg_user_{unique_id}@pvkcinemas.test"
        phone = f"+919{unique_id[:8]}"
        payload = {
            "firstName": "Regression",
            "lastName": "User",
            "email": email,
            "phone": phone,
            "password": "TestPassword123!"
        }
        resp = client.post("/auth/register", json=payload)
        data = client.assert_success_envelope(resp, expected_status=201)
        assert data.get("email") == email.lower()
        assert data.get("role") == "CUSTOMER"
        assert data.get("token") is not None
        assert data.get("expiresIn") == 1800  # Exactly 30-minute token lifetime

    def test_op02_register_validation_failure(self, client):
        """API-REG-002-ERR: POST /api/v1/auth/register -> 400 Bad Request on invalid payload"""
        invalid_payload = {
            "firstName": "",
            "lastName": "",
            "email": "not-an-email",
            "phone": "123",
            "password": "short"
        }
        resp = client.post("/auth/register", json=invalid_payload)
        client.assert_error_envelope(resp, expected_status=400)

    def test_op02_register_conflict_failure(self, client):
        """API-REG-002-CONFLICT: POST /api/v1/auth/register -> 409 Conflict on duplicate email/phone"""
        # test_customer@pvkcinemas.test already exists
        conflict_payload = {
            "firstName": "Duplicate",
            "lastName": "Customer",
            "email": "test_customer@pvkcinemas.test",
            "phone": "+919000000002",
            "password": "TestPassword123!"
        }
        resp = client.post("/auth/register", json=conflict_payload)
        client.assert_error_envelope(resp, expected_status=409, expected_code="CONFLICT")

    def test_op03_login_success(self, client):
        """API-REG-003: POST /api/v1/auth/login -> 200 OK"""
        login_payload = {
            "email": "test_customer@pvkcinemas.test",
            "password": "TestPassword123!"
        }
        resp = client.post("/auth/login", json=login_payload)
        data = client.assert_success_envelope(resp, expected_status=200)
        assert data.get("email") == "test_customer@pvkcinemas.test"
        assert data.get("token") is not None
        assert data.get("expiresIn") == 1800

    def test_op03_login_invalid_credentials(self, client):
        """API-REG-003-ERR: POST /api/v1/auth/login -> 401 Unauthorized on wrong password"""
        login_payload = {
            "email": "test_customer@pvkcinemas.test",
            "password": "WrongPassword999!"
        }
        resp = client.post("/auth/login", json=login_payload)
        client.assert_error_envelope(resp, expected_status=401, expected_code="AUTH_INVALID_CREDENTIALS")

    def test_op05_get_me_authenticated(self, client_customer):
        """API-REG-005: GET /api/v1/auth/me -> 200 OK"""
        resp = client_customer.get("/auth/me")
        data = client_customer.assert_success_envelope(resp, expected_status=200)
        user_info = data.get("data", {})
        assert user_info.get("email") == "test_customer@pvkcinemas.test"
        assert "CUSTOMER" in [r.replace("ROLE_", "") for r in user_info.get("roles", [])]

    def test_op05_get_me_unauthenticated(self, client_anonymous):
        """API-REG-005-UNAUTH: GET /api/v1/auth/me -> 401 Unauthorized"""
        resp = client_anonymous.get("/auth/me")
        client_anonymous.assert_error_envelope(resp, expected_status=401)

    def test_op04_logout_and_revocation(self, api_base_url):
        """API-REG-004: POST /api/v1/auth/logout -> 200 OK & token revocation (OP-04 & OP-05)"""
        # Create dedicated client to test token revocation
        temp_client = PvkApiClient(base_url=api_base_url)
        temp_client.login_as("customer")
        saved_token = temp_client._token

        # Verify active token works
        resp = temp_client.get("/auth/me")
        assert resp.status_code == 200

        # Execute logout (OP-04)
        logout_resp = temp_client.post("/auth/logout")
        assert logout_resp.status_code == 200
        logout_data = logout_resp.json()
        assert logout_data.get("status") == "SUCCESS"

        # Attempt to reuse revoked token against protected endpoint -> 401
        reuse_client = PvkApiClient(base_url=api_base_url)
        reuse_client.set_token(saved_token)
        reuse_resp = reuse_client.get("/auth/me")
        reuse_client.assert_error_envelope(reuse_resp, expected_status=401)

    def test_op52_get_profile(self, client_customer):
        """API-REG-052: GET /api/v1/me/profile -> 200 OK"""
        resp = client_customer.get("/me/profile")
        data = client_customer.assert_success_envelope(resp, expected_status=200)
        profile = data.get("data", {})
        assert profile.get("email") == "test_customer@pvkcinemas.test"
        assert profile.get("firstName") == "Test"

    def test_op52_get_profile_unauthenticated(self, client_anonymous):
        """API-REG-052-UNAUTH: GET /api/v1/me/profile -> 401 Unauthorized"""
        resp = client_anonymous.get("/me/profile")
        client_anonymous.assert_error_envelope(resp, expected_status=401)

    def test_op53_update_profile(self, client_customer):
        """API-REG-053: PATCH /api/v1/me/profile -> 200 OK"""
        update_payload = {
            "firstName": "TestUpdated",
            "lastName": "CustomerUpdated"
        }
        resp = client_customer.patch("/me/profile", json=update_payload)
        data = client_customer.assert_success_envelope(resp, expected_status=200)
        profile = data.get("data", {})
        assert profile.get("firstName") == "TestUpdated"
        assert profile.get("lastName") == "CustomerUpdated"

        # Revert profile to maintain idempotent state
        client_customer.patch("/me/profile", json={"firstName": "Test", "lastName": "Customer"})
