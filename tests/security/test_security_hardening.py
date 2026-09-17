"""
PVK Cinemas — Automated Security Hardening & Negative Security Test Suite
Phase: P14-D (Security Hardening & Security Verification)
Authority: P14 Baseline §13, SRS §11, Test Strategy §19, ARCH §14.

Covers:
1. Authentication Security & Credential Protection
2. JWT Security & Token Revocation
3. RBAC & Role Authorization Boundaries
4. Horizontal Manager Scope Isolation (BR-006)
5. Input Validation & Controlled Error Responses
6. Injection Resistance (SQLi & XSS)
7. CORS Policy Lockdown
8. HTTP Security Headers
9. Sensitive Information Disclosure Audit
10. Search Service Boundary Security
11. Audit Trail Governance & Immutability
"""

import base64
import json
import pytest
import requests
import pymysql
from tests.config.test_accounts import ACCOUNTS
from tests.config.test_environment import ENV

# ============================================================================
# 1. AUTHENTICATION SECURITY & CREDENTIAL PROTECTION
# ============================================================================

class TestAuthSecurity:
    """Verifies authentication integrity, password security, and credential protection."""

    def test_unauthenticated_access_returns_401(self, client_anonymous):
        """Unauthenticated access to protected endpoints must return 401 Unauthorized."""
        resp = client_anonymous.get("/auth/me")
        assert resp.status_code == 401
        data = resp.json()
        assert data["code"] == "AUTH_INVALID_CREDENTIALS"

    def test_login_invalid_password_returns_401(self, client):
        """Login with incorrect password returns 401 without specific reason disclosure."""
        resp = client.post("/auth/login", json={
            "email": ACCOUNTS["customer"].email,
            "password": "WrongPassword123!"
        })
        assert resp.status_code == 401
        data = resp.json()
        assert data["code"] == "AUTH_INVALID_CREDENTIALS"

    def test_login_nonexistent_email_returns_401(self, client):
        """Login with non-existent email returns 401 with uniform message (anti-enumeration)."""
        resp = client.post("/auth/login", json={
            "email": "nonexistent_ghost_user_9999@pvk.com",
            "password": "AnyPassword123!"
        })
        assert resp.status_code == 401
        data = resp.json()
        assert data["code"] == "AUTH_INVALID_CREDENTIALS"

    def test_password_never_exposed_in_auth_responses(self, client):
        """Password and passwordHash must never be exposed in registration or login responses."""
        resp = client.post("/auth/login", json={
            "email": ACCOUNTS["customer"].email,
            "password": ACCOUNTS["customer"].password
        })
        assert resp.status_code == 200
        data = resp.json()
        resp_str = json.dumps(data).lower()
        assert "password" not in resp_str
        assert "passwordhash" not in resp_str

    def test_password_never_exposed_in_profile(self, client_customer):
        """User profile responses must never contain password or hash fields."""
        resp = client_customer.get("/auth/me")
        assert resp.status_code == 200
        resp_str = json.dumps(resp.json()).lower()
        assert "password" not in resp_str
        assert "passwordhash" not in resp_str

    def test_password_never_exposed_in_admin_user_list(self, client_super_admin):
        """Admin user listing must exclude password and passwordHash fields."""
        resp = client_super_admin.get("/admin/users")
        assert resp.status_code == 200
        resp_str = json.dumps(resp.json()).lower()
        assert "password" not in resp_str
        assert "passwordhash" not in resp_str

    def test_registration_duplicate_email_conflict(self, client):
        """Registration with existing email must return 409 Conflict."""
        resp = client.post("/auth/register", json={
            "firstName": "Duplicate",
            "lastName": "User",
            "email": ACCOUNTS["customer"].email,
            "phone": "+919999900001",
            "password": "ValidPassword123!"
        })
        assert resp.status_code == 409
        assert resp.json()["code"] == "CONFLICT"

    def test_registration_duplicate_phone_conflict(self, client):
        """Registration with existing phone must return 409 Conflict."""
        import uuid
        uid = uuid.uuid4().hex[:6]
        phone = f"+9198765{uid[:5]}"
        # Register user 1
        r1 = client.post("/auth/register", json={
            "firstName": "First",
            "lastName": "User",
            "email": f"phone_user1_{uid}@pvk.com",
            "phone": phone,
            "password": "ValidPassword123!"
        })
        assert r1.status_code == 201

        # Attempt registering user 2 with same phone
        r2 = client.post("/auth/register", json={
            "firstName": "Second",
            "lastName": "User",
            "email": f"phone_user2_{uid}@pvk.com",
            "phone": phone,
            "password": "ValidPassword123!"
        })
        assert r2.status_code == 409
        assert r2.json()["code"] == "CONFLICT"


# ============================================================================
# 2. JWT SECURITY & TOKEN REVOCATION
# ============================================================================

class TestJwtSecurity:
    """Verifies JWT cryptographic integrity, signature verification, and revocation."""

    def test_tampered_signature_returns_401(self, client_customer, api_base_url):
        """Modifying the signature of a valid JWT must result in immediate 401 rejection."""
        token = client_customer.token
        parts = token.split(".")
        assert len(parts) == 3
        # Tamper with the signature portion
        tampered_sig = parts[2][:-4] + "AAAA"
        tampered_token = f"{parts[0]}.{parts[1]}.{tampered_sig}"

        headers = {"Authorization": f"Bearer {tampered_token}"}
        resp = requests.get(f"{api_base_url}/auth/me", headers=headers)
        assert resp.status_code == 401

    def test_tampered_payload_returns_401(self, client_customer, api_base_url):
        """Modifying the payload claims of a valid JWT must fail signature verification."""
        token = client_customer.token
        parts = token.split(".")
        # Decode and alter payload (elevate role to SUPER_ADMIN)
        payload_bytes = base64.urlsafe_b64decode(parts[1] + "==")
        payload = json.loads(payload_bytes)
        payload["roles"] = ["ROLE_SUPER_ADMIN"]
        tampered_payload = base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip("=")
        tampered_token = f"{parts[0]}.{tampered_payload}.{parts[2]}"

        headers = {"Authorization": f"Bearer {tampered_token}"}
        resp = requests.get(f"{api_base_url}/admin/audit-logs", headers=headers)
        assert resp.status_code == 401

    def test_unsigned_alg_none_token_rejected(self, api_base_url):
        """Tokens using alg: none or omitting signature must be rejected."""
        header = base64.urlsafe_b64encode(json.dumps({"alg": "none", "typ": "JWT"}).encode()).decode().rstrip("=")
        payload = base64.urlsafe_b64encode(json.dumps({"sub": "1", "email": "admin@pvk.com", "roles": ["ROLE_SUPER_ADMIN"]}).encode()).decode().rstrip("=")
        unsigned_token = f"{header}.{payload}."

        headers = {"Authorization": f"Bearer {unsigned_token}"}
        resp = requests.get(f"{api_base_url}/admin/audit-logs", headers=headers)
        assert resp.status_code == 401

    def test_garbage_token_rejected(self, api_base_url):
        """Completely random or malformed token strings must return 401."""
        headers = {"Authorization": "Bearer not-a-real-jwt-token-at-all"}
        resp = requests.get(f"{api_base_url}/auth/me", headers=headers)
        assert resp.status_code == 401

    def test_missing_bearer_prefix_rejected(self, client_customer, api_base_url):
        """Token passed without Bearer scheme must be treated as unauthenticated."""
        headers = {"Authorization": client_customer.token}
        resp = requests.get(f"{api_base_url}/auth/me", headers=headers)
        assert resp.status_code == 401

    def test_token_revocation_on_logout(self, api_base_url):
        """Logging out must add the token's JTI to TokenRevocationStore, invalidating subsequent requests."""
        # 1. Login to get fresh token
        login_resp = requests.post(f"{api_base_url}/auth/login", json={
            "email": ACCOUNTS["customer"].email,
            "password": ACCOUNTS["customer"].password
        })
        assert login_resp.status_code == 200
        token = login_resp.json()["token"]
        auth_headers = {"Authorization": f"Bearer {token}"}

        # 2. Verify token works
        check1 = requests.get(f"{api_base_url}/auth/me", headers=auth_headers)
        assert check1.status_code == 200

        # 3. Call logout
        logout_resp = requests.post(f"{api_base_url}/auth/logout", headers=auth_headers)
        assert logout_resp.status_code == 200
        assert logout_resp.json()["status"] == "SUCCESS"

        # 4. Immediate subsequent request with same token must be 401
        check2 = requests.get(f"{api_base_url}/auth/me", headers=auth_headers)
        assert check2.status_code == 401


# ============================================================================
# 3. RBAC & ROLE AUTHORIZATION BOUNDARIES
# ============================================================================

class TestRbacBoundaries:
    """Verifies role-based access control across Anonymous, Customer, Manager, and Super Admin."""

    def test_anonymous_cannot_access_manager_or_admin(self, client_anonymous):
        """Anonymous callers must be rejected from manager and admin resources with 401."""
        assert client_anonymous.get("/manager/movies").status_code == 401
        assert client_anonymous.get("/manager/theatres/1/screens").status_code == 401
        assert client_anonymous.get("/admin/users").status_code == 401
        assert client_anonymous.get("/admin/audit-logs").status_code == 401

    def test_customer_cannot_access_manager_resources(self, client_customer):
        """Customer persona must receive 403 Forbidden when accessing manager endpoints."""
        assert client_customer.get("/manager/movies").status_code == 403
        assert client_customer.get("/manager/theatres/1/screens").status_code == 403
        assert client_customer.get("/manager/theatres/1/shows").status_code == 403

    def test_customer_cannot_access_admin_resources(self, client_customer):
        """Customer persona must receive 403 Forbidden when accessing admin endpoints."""
        assert client_customer.get("/admin/users").status_code == 403
        assert client_customer.get("/admin/audit-logs").status_code == 403
        assert client_customer.post("/admin/search/reindex").status_code == 403

    def test_manager_cannot_access_admin_resources(self, client_manager_a):
        """Theatre Manager persona must receive 403 Forbidden on admin endpoints."""
        assert client_manager_a.get("/admin/users").status_code == 403
        assert client_manager_a.get("/admin/audit-logs").status_code == 403
        assert client_manager_a.post("/admin/search/reindex").status_code == 403

    def test_super_admin_has_global_access(self, client_super_admin):
        """Super Admin persona must have access to administrative and managerial resources."""
        assert client_super_admin.get("/admin/users").status_code == 200
        assert client_super_admin.get("/admin/audit-logs").status_code == 200
        assert client_super_admin.get("/manager/theatres/1/screens").status_code == 200
        assert client_super_admin.get("/manager/theatres/2/screens").status_code == 200


# ============================================================================
# 4. HORIZONTAL MANAGER SCOPE ISOLATION (BR-006)
# ============================================================================

class TestManagerScopeIsolation:
    """Verifies strict horizontal isolation between Theatre Manager A and Manager B."""

    def test_manager_a_allowed_theatre_1(self, client_manager_a):
        """Manager A has authorized access to assigned Theatre 1."""
        resp = client_manager_a.get("/manager/theatres/1/screens")
        assert resp.status_code == 200

    def test_manager_a_denied_theatre_2(self, client_manager_a):
        """Manager A is strictly denied access to Theatre 2 (403 Forbidden)."""
        resp = client_manager_a.get("/manager/theatres/2/screens")
        assert resp.status_code == 403
        assert resp.json()["code"] == "ACCESS_DENIED"

    def test_manager_b_allowed_theatre_2(self, client_manager_b):
        """Manager B has authorized access to assigned Theatre 2."""
        resp = client_manager_b.get("/manager/theatres/2/screens")
        assert resp.status_code == 200

    def test_manager_b_denied_theatre_1(self, client_manager_b):
        """Manager B is strictly denied access to Theatre 1 (403 Forbidden)."""
        resp = client_manager_b.get("/manager/theatres/1/screens")
        assert resp.status_code == 403
        assert resp.json()["code"] == "ACCESS_DENIED"

    def test_manager_a_cannot_schedule_show_in_theatre_2(self, client_manager_a):
        """Manager A cannot create shows in Theatre 2 via path manipulation."""
        resp = client_manager_a.post("/manager/theatres/2/shows", json={
            "movieId": 1,
            "movieLanguageId": 1,
            "screenId": 4,  # Screen in Theatre 2
            "screenCapabilityId": 4,
            "startAt": "2026-12-01T10:00:00Z",
            "endAt": "2026-12-01T12:30:00Z",
            "showStatus": "SCHEDULED"
        })
        assert resp.status_code == 403

    def test_manager_a_cannot_access_theatre_2_screen_seats(self, client_manager_a):
        """Manager A cannot inspect seats of a screen belonging to Theatre 2."""
        # Query MySQL for a screen in Theatre 2
        conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
        cur = conn.cursor()
        cur.execute("SELECT screen_id FROM screen WHERE theatre_id = 2 LIMIT 1")
        row = cur.fetchone()
        cur.close()
        conn.close()

        if row:
            theatre_2_screen_id = row[0]
            resp = client_manager_a.get(f"/manager/screens/{theatre_2_screen_id}/seats")
            assert resp.status_code == 403


# ============================================================================
# 5. INPUT VALIDATION & CONTROLLED ERROR RESPONSES
# ============================================================================

class TestInputValidationSecurity:
    """Verifies that invalid or malformed input returns controlled 4xx with zero 500 errors."""

    def test_malformed_json_returns_400_not_500(self, api_base_url):
        """Sending syntax-invalid JSON body returns 400 MALFORMED_REQUEST without stack trace."""
        headers = {"Content-Type": "application/json"}
        resp = requests.post(f"{api_base_url}/auth/login", data="{bad_json_syntax:", headers=headers)
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "MALFORMED_REQUEST"
        assert "stacktrace" not in json.dumps(data).lower()
        assert "traceId" in data or "traceid" in json.dumps(data).lower()

    def test_path_variable_type_mismatch_returns_400(self, client):
        """Non-numeric string in integer/long path variable returns 400 INVALID_PARAMETER."""
        resp = client.get("/movies/not-a-number-id")
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "INVALID_PARAMETER"
        assert "movieId" in data["message"]

    def test_missing_required_registration_fields_returns_400(self, client):
        """Registration payload missing mandatory fields returns 400 VALIDATION_FAILED."""
        resp = client.post("/auth/register", json={
            "email": "incomplete@pvk.com"
        })
        assert resp.status_code == 400
        data = resp.json()
        assert data["code"] == "VALIDATION_FAILED"
        assert "fieldErrors" in data

    def test_unsupported_media_type_returns_415(self, api_base_url):
        """Sending Content-Type text/plain on JSON-expecting endpoint returns 415."""
        headers = {"Content-Type": "text/plain"}
        resp = requests.post(f"{api_base_url}/auth/login", data="some text", headers=headers)
        assert resp.status_code == 415
        data = resp.json()
        assert data["code"] == "UNSUPPORTED_MEDIA_TYPE"

    def test_method_not_allowed_returns_405(self, client_super_admin):
        """Invoking unmapped HTTP method on existing route returns 405 METHOD_NOT_ALLOWED."""
        resp = client_super_admin.delete("/movies/1")
        assert resp.status_code == 405
        data = resp.json()
        assert data["code"] == "METHOD_NOT_ALLOWED"


# ============================================================================
# 6. INJECTION RESISTANCE (SQLi & XSS)
# ============================================================================

class TestInjectionResistance:
    """Verifies resistance against SQL injection, XSS, and command injection."""

    def test_sqli_in_movie_filter_safely_handled(self, client):
        """SQL injection attempt in query string must not trigger database error or data leak."""
        sqli_payloads = [
            "' OR '1'='1",
            "'; DROP TABLE movie; --",
            "' UNION SELECT 1,2,3,4,5,6,7,8,9,10--",
            "1' AND SLEEP(2)--"
        ]
        for payload in sqli_payloads:
            resp = client.get(f"/movies?status={payload}")
            # Must return clean 200 (empty or filtered) or 400, NEVER 500
            assert resp.status_code in [200, 400]
            resp_str = json.dumps(resp.json()).lower()
            assert "syntax error" not in resp_str
            assert "mysql" not in resp_str
            assert "sql" not in resp_str

    def test_sqli_in_search_query_safely_handled(self, client):
        """SQL injection attempt in search query must execute safely through parameterized backend."""
        sqli_query = "Inception' OR 1=1;--"
        resp = client.get(f"/search?query={sqli_query}")
        assert resp.status_code in [200, 400]
        resp_str = json.dumps(resp.json()).lower()
        assert "syntax error" not in resp_str
        assert "mysql" not in resp_str

    def test_sqli_in_login_credentials_safely_rejected(self, client):
        """Classic ' OR '1'='1 injection in login credentials must fail authentication cleanly."""
        resp = client.post("/auth/login", json={
            "email": "' OR '1'='1' --",
            "password": "' OR '1'='1"
        })
        assert resp.status_code in [400, 401]
        assert resp.json()["code"] in ["VALIDATION_FAILED", "AUTH_INVALID_CREDENTIALS"]

    def test_xss_in_search_query_echoed_as_data_not_html(self, client):
        """XSS script payloads in search query are treated strictly as string data."""
        xss_payload = "<script>alert('xss')</script>"
        resp = client.get(f"/search?query={xss_payload}")
        assert resp.status_code == 200
        assert "application/json" in resp.headers.get("Content-Type", "")
        assert resp.text.startswith("{")

    def test_xss_in_registration_name_stored_safely(self, client):
        """XSS payloads in user profile fields are stored as literal string data."""
        xss_payload = "<img src=x onerror=alert(1)>"
        unique_email = f"xss_test_{base64.b16encode(xss_payload.encode())[:8].decode()}@pvk.com"
        resp = client.post("/auth/register", json={
            "firstName": xss_payload,
            "lastName": "SecurityTest",
            "email": unique_email,
            "phone": "+919876543299",
            "password": "ValidPassword123!"
        })
        if resp.status_code == 201:
            data = resp.json()
            assert data["firstName"] == xss_payload


# ============================================================================
# 7. CORS POLICY LOCKDOWN
# ============================================================================

class TestCorsSecurity:
    """Verifies CORS origin allow-listing and rejection of unauthorized origins."""

    def test_allowed_origin_receives_cors_headers(self, api_base_url):
        """Requests from authorized origin http://localhost:3000 receive Access-Control-Allow-Origin."""
        resp = requests.get(f"{api_base_url}/movies", headers={"Origin": "http://localhost:3000"})
        assert resp.status_code == 200
        assert resp.headers.get("Access-Control-Allow-Origin") == "http://localhost:3000"
        assert resp.headers.get("Access-Control-Allow-Credentials") == "true"

    def test_loopback_ip_origin_receives_cors_headers(self, api_base_url):
        """Requests from authorized origin http://127.0.0.1:3000 receive Access-Control-Allow-Origin."""
        resp = requests.get(f"{api_base_url}/movies", headers={"Origin": "http://127.0.0.1:3000"})
        assert resp.status_code == 200
        assert resp.headers.get("Access-Control-Allow-Origin") == "http://127.0.0.1:3000"

    def test_unauthorized_origin_rejected_or_denied_cors_header(self, api_base_url):
        """Requests from unauthorized origin (http://evil-attacker.com) are denied CORS access."""
        resp = requests.get(f"{api_base_url}/movies", headers={"Origin": "http://evil-attacker.com"})
        # Spring Security CORS either returns 403 Forbidden or omits Access-Control-Allow-Origin
        if resp.status_code == 200:
            assert resp.headers.get("Access-Control-Allow-Origin") != "http://evil-attacker.com"
        else:
            assert resp.status_code == 403


# ============================================================================
# 8. HTTP SECURITY HEADERS
# ============================================================================

class TestSecurityHeaders:
    """Verifies injection of standard HTTP security headers on all Spring Boot responses."""

    def test_nosniff_header_present(self, api_base_url):
        """X-Content-Type-Options: nosniff must be present."""
        resp = requests.get(f"{api_base_url}/movies")
        assert resp.headers.get("X-Content-Type-Options") == "nosniff"

    def test_frame_options_deny_present(self, api_base_url):
        """X-Frame-Options: DENY must be present."""
        resp = requests.get(f"{api_base_url}/movies")
        assert resp.headers.get("X-Frame-Options") == "DENY"

    def test_csp_header_present_and_restricts_frame_ancestors(self, api_base_url):
        """Content-Security-Policy header must be present and declare frame-ancestors 'none'."""
        resp = requests.get(f"{api_base_url}/movies")
        csp = resp.headers.get("Content-Security-Policy")
        assert csp is not None
        assert "default-src 'self'" in csp
        assert "frame-ancestors 'none'" in csp

    def test_referrer_policy_present(self, api_base_url):
        """Referrer-Policy header must be strict-origin-when-cross-origin."""
        resp = requests.get(f"{api_base_url}/movies")
        assert resp.headers.get("Referrer-Policy") == "strict-origin-when-cross-origin"

    def test_permissions_policy_present(self, api_base_url):
        """Permissions-Policy header must restrict camera, microphone, and geolocation."""
        resp = requests.get(f"{api_base_url}/movies")
        pp = resp.headers.get("Permissions-Policy")
        assert pp is not None
        assert "camera=()" in pp
        assert "microphone=()" in pp


# ============================================================================
# 9. SENSITIVE INFORMATION DISCLOSURE AUDIT
# ============================================================================

class TestInformationDisclosure:
    """Verifies that error responses never leak internal paths, SQL details, or stack traces."""

    def test_error_envelope_does_not_contain_stack_trace(self, client):
        """Error responses must strictly match ErrorResponse schema without stack traces."""
        resp = client.get("/movies/999999999")
        assert resp.status_code == 404
        data = resp.json()
        assert "status" in data
        assert "code" in data
        assert "message" in data
        assert "traceId" in data
        resp_text = json.dumps(data)
        assert "exception" not in resp_text.lower()
        assert "at com.pvk" not in resp_text
        assert "org.springframework" not in resp_text

    def test_error_envelope_does_not_contain_filesystem_paths(self, client_customer):
        """Error responses must never leak server filesystem paths."""
        resp = client_customer.get("/nonexistent-endpoint-404")
        assert resp.status_code == 404
        resp_text = json.dumps(resp.json())
        assert "c:\\" not in resp_text.lower()
        assert "/home/" not in resp_text.lower()
        assert "/usr/" not in resp_text.lower()

    def test_error_envelope_does_not_leak_database_credentials(self, client):
        """Error responses must never leak database usernames, passwords, or connection strings."""
        resp = client.get("/movies?status=INVALID_STATUS")
        resp_text = json.dumps(resp.json()).lower()
        assert "pvk_cinemas_db" not in resp_text
        assert "jdbc:" not in resp_text
        assert "password" not in resp_text


# ============================================================================
# 10. SEARCH SERVICE BOUNDARY SECURITY
# ============================================================================

class TestSearchServiceBoundary:
    """Verifies that search service is internal-only and validates inputs strictly."""

    def test_search_service_cors_restricts_to_loopback(self):
        """FastAPI search service CORS only permits requests from loopback port 8080."""
        search_health_url = f"{ENV.search_base_url}/health"
        resp = requests.get(search_health_url, headers={"Origin": "http://evil-external-site.com"})
        assert resp.status_code == 200
        # CORS middleware does not reflect unauthorized external origin
        assert resp.headers.get("Access-Control-Allow-Origin") != "http://evil-external-site.com"

    def test_oversized_search_query_rejected_or_bounded(self, client):
        """Submitting an excessively long query (>500 chars) is safely handled without error."""
        oversized_query = "A" * 600
        resp = client.get(f"/search?query={oversized_query}")
        # Must return controlled 400/422 or empty 200, never 500
        assert resp.status_code in [200, 400, 422]

    def test_search_reindex_requires_super_admin(self, client_customer, client_manager_a):
        """Triggering index rebuild is restricted to Super Admin; customer and manager get 403."""
        assert client_customer.post("/admin/search/reindex").status_code == 403
        assert client_manager_a.post("/admin/search/reindex").status_code == 403


# ============================================================================
# 11. AUDIT TRAIL GOVERNANCE & IMMUTABILITY
# ============================================================================

class TestAuditSecurity:
    """Verifies that audit logs are restricted, append-only, and contain no credentials."""

    def test_audit_logs_restricted_to_super_admin(self, client_customer, client_manager_a, client_super_admin):
        """Audit trail is accessible only to Super Admin; Customer and Manager get 403."""
        assert client_customer.get("/admin/audit-logs").status_code == 403
        assert client_manager_a.get("/admin/audit-logs").status_code == 403
        assert client_super_admin.get("/admin/audit-logs").status_code == 200

    def test_audit_log_details_never_contain_passwords(self, client_super_admin):
        """Audit records for AUTH_LOGIN or AUTH_REGISTER must never store plaintext passwords or hashes."""
        resp = client_super_admin.get("/admin/audit-logs?page=0&size=50")
        assert resp.status_code == 200
        data = resp.json()["data"]["content"]
        for record in data:
            action = record.get("actionType", "")
            details = record.get("details", "") or ""
            if "AUTH" in action:
                assert "password" not in details.lower()
                assert "passwordhash" not in details.lower()
                assert "secret" not in details.lower()
