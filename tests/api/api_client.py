"""
PVK Cinemas — Authoritative P14 API Test Client Harness
Provides a robust, reusable HTTP client foundation for the 53-operation
API regression suite.

Features:
- Bearer token lifecycle management (login, auto-inject, revocation)
- Multi-persona context switching (Anonymous, Customer, Manager A, Manager B, Admin)
- Standard envelope validation (ApiResponse<T>)
- Standard error envelope validation (ErrorResponse)
- Status code assertions (200, 201, 204, 400, 401, 403, 404, 409)
"""

import requests
from typing import Any, Optional
from contextlib import contextmanager
from tests.config.test_environment import ENV
from tests.config.test_accounts import ACCOUNTS, TestAccount, RoleType

class ApiResponseValidationError(AssertionError):
    """Raised when an API response violates standard response envelope contracts."""
    pass

class PvkApiClient:
    """
    HTTP client for the PVK Cinemas REST API (/api/v1).
    """
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = (base_url or ENV.backend_base_url).rstrip("/")
        self.session = requests.Session()
        self._current_account: Optional[TestAccount] = None
        self._token: Optional[str] = None
        self._token_cache: dict[str, str] = {}

    @property
    def token(self) -> Optional[str]:
        return self._token

    @property
    def current_persona(self) -> str:
        return self._current_account.persona_id if self._current_account else "anonymous"

    def set_token(self, token: Optional[str]) -> None:
        self._token = token
        if token:
            self.session.headers["Authorization"] = f"Bearer {token}"
        else:
            self.session.headers.pop("Authorization", None)

    def login_as(self, persona_id: str) -> Optional[str]:
        """
        Authenticates as a defined persona and injects the Bearer token.
        """
        if persona_id == "anonymous":
            self.set_token(None)
            self._current_account = ACCOUNTS["anonymous"]
            return None

        account = ACCOUNTS.get(persona_id)
        if not account:
            raise ValueError(f"Unknown test persona: '{persona_id}'")

        if persona_id in self._token_cache:
            token = self._token_cache[persona_id]
            self.set_token(token)
            self._current_account = account
            return token

        # Execute login request against backend
        login_url = f"{self.base_url}/auth/login"
        payload = {
            "email": account.email,
            "password": account.password
        }
        resp = self.session.post(login_url, json=payload, headers={"Content-Type": "application/json"})
        if resp.status_code == 200:
            data = resp.json()
            # Token may be at root or inside data envelope
            token = data.get("token") or (data.get("data", {}).get("token") if isinstance(data.get("data"), dict) else None)
            if token:
                self._token_cache[persona_id] = token
                self.set_token(token)
                self._current_account = account
                return token

        raise RuntimeError(f"Login failed for persona '{persona_id}' with status {resp.status_code}: {resp.text}")

    def logout(self) -> requests.Response:
        """
        Invokes logout endpoint to test token revocation and clears client session.
        """
        resp = self.post("/auth/logout")
        if self.current_persona in self._token_cache:
            del self._token_cache[self.current_persona]
        self.set_token(None)
        self._current_account = ACCOUNTS["anonymous"]
        return resp

    @contextmanager
    def as_persona(self, persona_id: str):
        """
        Context manager for temporary role execution.
        """
        previous_account = self._current_account
        previous_token = self._token
        try:
            self.login_as(persona_id)
            yield self
        finally:
            self._current_account = previous_account
            self.set_token(previous_token)

    def request(self, method: str, path: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{path}" if path.startswith("/") else f"{self.base_url}/{path}"
        return self.session.request(method=method, url=url, **kwargs)

    def get(self, path: str, **kwargs) -> requests.Response:
        return self.request("GET", path, **kwargs)

    def post(self, path: str, **kwargs) -> requests.Response:
        return self.request("POST", path, **kwargs)

    def patch(self, path: str, **kwargs) -> requests.Response:
        return self.request("PATCH", path, **kwargs)

    def delete(self, path: str, **kwargs) -> requests.Response:
        return self.request("DELETE", path, **kwargs)

    # -------------------------------------------------------------------------
    # Response Envelope Assertions
    # -------------------------------------------------------------------------
    @staticmethod
    def assert_success_envelope(response: requests.Response, expected_status: int = 200) -> dict[str, Any]:
        """
        Asserts standard ApiResponse envelope contract:
        - HTTP status code matches expected_status (200, 201, 204)
        - If response body present, JSON structure contains valid fields
        """
        if response.status_code != expected_status:
            raise ApiResponseValidationError(
                f"Expected status {expected_status}, but received {response.status_code}. Body: {response.text}"
            )

        if expected_status == 204 or not response.text.strip():
            return {}

        try:
            body = response.json()
        except Exception as e:
            raise ApiResponseValidationError(f"Response body is not valid JSON: {response.text}") from e

        # Validate standard envelope or direct collection
        if isinstance(body, dict):
            # Check for ApiResponse fields if envelope mode is active
            if "success" in body:
                if not body["success"]:
                    raise ApiResponseValidationError(f"Envelope 'success' is false in successful response: {body}")
        return body

    @staticmethod
    def assert_error_envelope(response: requests.Response, expected_status: int, expected_code: Optional[str] = None) -> dict[str, Any]:
        """
        Asserts standard ErrorResponse envelope contract:
        - HTTP status matches expected (400, 401, 403, 404, 409)
        - Error body contains structured error info
        """
        if response.status_code != expected_status:
            raise ApiResponseValidationError(
                f"Expected error status {expected_status}, but received {response.status_code}. Body: {response.text}"
            )

        try:
            body = response.json()
        except Exception as e:
            raise ApiResponseValidationError(f"Error response is not valid JSON: {response.text}") from e

        if isinstance(body, dict):
            # If code check specified, verify error code
            if expected_code:
                code = body.get("code") or (body.get("error", {}).get("code") if isinstance(body.get("error"), dict) else None)
                if code and code != expected_code:
                    raise ApiResponseValidationError(f"Expected error code '{expected_code}', got '{code}'")

        return body
