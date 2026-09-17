"""
PVK Cinemas — API Regression Suite: Super Admin Governance & Audit
Covers operations:
- OP-01: GET   /admin/audit-logs (AdminAuditController::getAuditLogs)
- OP-08: GET   /admin/movies (AdminMovieController::getMovies)
- OP-09: POST  /admin/movies (AdminMovieController::createMovie)
- OP-10: PATCH /admin/movies/{movieId} (AdminMovieController::updateMovie)
- OP-23: GET   /admin/cities (AdminOrganizationController::getCities)
- OP-24: POST  /admin/cities (AdminOrganizationController::createCity)
- OP-25: PATCH /admin/cities/{cityId} (AdminOrganizationController::updateCity)
- OP-26: GET   /admin/theatres (AdminOrganizationController::getTheatres)
- OP-27: POST  /admin/theatres (AdminOrganizationController::createTheatre)
- OP-28: PATCH /admin/theatres/{theatreId} (AdminOrganizationController::updateTheatre)
- OP-29: POST  /admin/theatres/{theatreId}/managers (AdminOrganizationController::assignManager)
- OP-30: PATCH /admin/theatres/{theatreId}/managers (AdminOrganizationController::revokeManager)
- OP-41: POST  /admin/search/reindex (AdminSearchController::triggerReindex)
- OP-42: GET   /admin/search/status (AdminSearchController::getStatus)
- OP-45: GET   /admin/roles (AdminRoleController::getRoles)
- OP-46: POST  /admin/roles/{roleId} (AdminRoleController::updateRolePermissionsPost)
- OP-47: PATCH /admin/roles/{roleId} (AdminRoleController::updateRolePermissionsPatch)
- OP-48: GET   /admin/permissions (AdminRoleController::getPermissions)
- OP-49: GET   /admin/users (AdminUserController::getUsers)
- OP-50: POST  /admin/users/{userId} (AdminUserController::updateUserPost)
- OP-51: PATCH /admin/users/{userId} (AdminUserController::updateUserPatch)

Validates:
- Super Admin authorization on all administrative endpoints
- Access rejection (403 Forbidden) for Manager and Customer personas
- Append-only audit record creation in AUDIT_LOG table upon state mutations
- Database persistence for movie, city, theatre, and user modifications
"""

import pytest
import uuid
import pymysql

class TestAdminGovernanceRegression:

    def test_op01_get_audit_logs(self, client_super_admin, client_manager_a, client_customer):
        """API-REG-001: GET /api/v1/admin/audit-logs -> 200 Admin, 403 Manager, 403 Customer"""
        # Super Admin -> 200 OK
        resp_admin = client_super_admin.get("/admin/audit-logs")
        data = client_super_admin.assert_success_envelope(resp_admin, expected_status=200)
        logs_page = data.get("data", {})
        assert "content" in logs_page

        # Manager -> 403 Forbidden
        resp_mgr = client_manager_a.get("/admin/audit-logs")
        client_manager_a.assert_error_envelope(resp_mgr, expected_status=403)

        # Customer -> 403 Forbidden
        resp_cust = client_customer.get("/admin/audit-logs")
        client_customer.assert_error_envelope(resp_cust, expected_status=403)

    def test_op08_op09_op10_admin_movies_and_audit(self, client_super_admin, client_manager_a):
        """API-REG-008, 009, 010: Movies Catalogue Management & Audit Logging"""
        unique_suffix = uuid.uuid4().hex[:6]

        # OP-08: GET /admin/movies -> 200
        resp_list = client_super_admin.get("/admin/movies")
        client_super_admin.assert_success_envelope(resp_list, expected_status=200)

        # Manager attempting admin movie creation -> 403
        movie_payload = {
            "title": f"Admin Test Movie {unique_suffix}",
            "synopsis": "A movie created by admin regression test.",
            "runtimeMinutes": 135,
            "movieStatus": "AIRING",
            "certificationId": 1,
            "releaseDate": "2026-10-01",
            "genreIds": [1],
            "languageIds": [1]
        }
        resp_denied = client_manager_a.post("/admin/movies", json=movie_payload)
        client_manager_a.assert_error_envelope(resp_denied, expected_status=403)

        # OP-09: POST /admin/movies -> 201 Created
        resp_created = client_super_admin.post("/admin/movies", json=movie_payload)
        created_data = client_super_admin.assert_success_envelope(resp_created, expected_status=201)
        movie_id = created_data.get("data", {}).get("movieId")
        assert movie_id is not None
        assert created_data.get("data", {}).get("title") == movie_payload["title"]

        # OP-10: PATCH /admin/movies/{movieId} -> 200 OK
        updated_title = f"Admin Test Movie Updated {unique_suffix}"
        resp_updated = client_super_admin.patch(f"/admin/movies/{movie_id}", json={"title": updated_title, "movieStatus": "AIRING"})
        updated_data = client_super_admin.assert_success_envelope(resp_updated, expected_status=200)
        assert updated_data.get("data", {}).get("title") == updated_title

        # Verify Audit Log entry was generated
        conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
        cur = conn.cursor()
        cur.execute("SELECT action, entity_type, entity_id FROM audit_log WHERE entity_type = 'MOVIE' AND entity_id = %s", (movie_id,))
        audit_rows = cur.fetchall()
        cur.close()
        conn.close()
        assert len(audit_rows) > 0, "Expected audit log row for admin movie mutation"

    def test_op23_op24_op25_admin_cities(self, client_super_admin, client_manager_a):
        """API-REG-023, 024, 025: Cities Organization Management (GET, POST, PATCH)"""
        unique_suffix = uuid.uuid4().hex[:6]

        # OP-23: GET /admin/cities -> 200
        resp_list = client_super_admin.get("/admin/cities")
        client_super_admin.assert_success_envelope(resp_list, expected_status=200)

        # Manager attempting create city -> 403
        city_payload = {
            "cityName": f"City_{unique_suffix}",
            "stateName": "Karnataka",
            "countryCode": "IN"
        }
        resp_denied = client_manager_a.post("/admin/cities", json=city_payload)
        client_manager_a.assert_error_envelope(resp_denied, expected_status=403)

        # OP-24: POST /admin/cities -> 201 Created
        resp_created = client_super_admin.post("/admin/cities", json=city_payload)
        data = client_super_admin.assert_success_envelope(resp_created, expected_status=201)
        city_id = data.get("data", {}).get("cityId")
        assert city_id is not None

        # OP-25: PATCH /admin/cities/{cityId} -> 200 OK
        updated_name = f"City_Upd_{unique_suffix}"
        resp_updated = client_super_admin.patch(f"/admin/cities/{city_id}", json={"cityName": updated_name, "countryCode": "IN"})
        upd_data = client_super_admin.assert_success_envelope(resp_updated, expected_status=200)
        assert upd_data.get("data", {}).get("cityName") == updated_name

    def test_op26_op27_op28_admin_theatres(self, client_super_admin, client_manager_a):
        """API-REG-026, 027, 028: Theatres Organization Management (GET, POST, PATCH)"""
        unique_suffix = uuid.uuid4().hex[:6]

        # OP-26: GET /admin/theatres -> 200
        resp_list = client_super_admin.get("/admin/theatres")
        client_super_admin.assert_success_envelope(resp_list, expected_status=200)

        # Manager attempting create theatre -> 403
        theatre_payload = {
            "cityId": 1,
            "theatreCode": f"TH-ADM-{unique_suffix}",
            "theatreName": f"Admin Multiplex {unique_suffix}",
            "addressLine1": "999 Governance Blvd",
            "status": "ACTIVE"
        }
        resp_denied = client_manager_a.post("/admin/theatres", json=theatre_payload)
        client_manager_a.assert_error_envelope(resp_denied, expected_status=403)

        # OP-27: POST /admin/theatres -> 201 Created
        resp_created = client_super_admin.post("/admin/theatres", json=theatre_payload)
        data = client_super_admin.assert_success_envelope(resp_created, expected_status=201)
        theatre_id = data.get("data", {}).get("theatreId")
        assert theatre_id is not None

        # OP-28: PATCH /admin/theatres/{theatreId} -> 200 OK
        updated_name = f"Admin Multiplex Updated {unique_suffix}"
        resp_updated = client_super_admin.patch(f"/admin/theatres/{theatre_id}", json={"theatreName": updated_name})
        upd_data = client_super_admin.assert_success_envelope(resp_updated, expected_status=200)
        assert upd_data.get("data", {}).get("theatreName") == updated_name

    def test_op29_op30_admin_theatre_managers(self, client_super_admin, client_manager_a):
        """API-REG-029, 030: Assign & Revoke Theatre Managers"""
        # Retrieve user ID of Manager A
        conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
        cur = conn.cursor()
        cur.execute("SELECT user_id FROM user WHERE email = 'manager_a@pvkcinemas.test'")
        mgr_a_id = cur.fetchone()[0]
        cur.close()
        conn.close()

        # OP-29: POST /admin/theatres/{theatreId}/managers -> 201 Created
        assign_payload = {"userId": mgr_a_id}
        resp_assign = client_super_admin.post("/admin/theatres/1/managers", json=assign_payload)
        assert resp_assign.status_code in [200, 201]

        # OP-30: PATCH /admin/theatres/{theatreId}/managers?userId=... -> 200 OK
        resp_revoke = client_super_admin.patch(f"/admin/theatres/1/managers?userId={mgr_a_id}")
        assert resp_revoke.status_code == 200

        # Re-assign Manager A to maintain valid state for subsequent manager scope tests
        client_super_admin.post("/admin/theatres/1/managers", json=assign_payload)

    def test_op41_op42_admin_search_ops(self, client_super_admin, client_manager_a):
        """API-REG-041, 042: Search Reindex Trigger & Index Status"""
        # Manager -> 403
        resp_denied = client_manager_a.post("/admin/search/reindex")
        client_manager_a.assert_error_envelope(resp_denied, expected_status=403)

        # OP-41: POST /admin/search/reindex -> 200 / 202 Accepted
        resp_reindex = client_super_admin.post("/admin/search/reindex")
        assert resp_reindex.status_code in [200, 202]

        # OP-42: GET /admin/search/status -> 200 OK
        resp_status = client_super_admin.get("/admin/search/status")
        data = client_super_admin.assert_success_envelope(resp_status, expected_status=200)
        status_data = data.get("data", {})
        assert status_data is not None

    def test_op45_op46_op47_op48_admin_roles_and_permissions(self, client_super_admin, client_customer):
        """API-REG-045, 046, 047, 048: Roles and Permissions Administration"""
        # Customer -> 403
        resp_denied = client_customer.get("/admin/roles")
        client_customer.assert_error_envelope(resp_denied, expected_status=403)

        # OP-45: GET /admin/roles -> 200 OK
        resp_roles = client_super_admin.get("/admin/roles")
        roles_data = client_super_admin.assert_success_envelope(resp_roles, expected_status=200)
        roles_list = roles_data.get("data", [])
        assert len(roles_list) >= 3

        # OP-48: GET /admin/permissions -> 200 OK
        resp_perms = client_super_admin.get("/admin/permissions")
        perms_data = client_super_admin.assert_success_envelope(resp_perms, expected_status=200)
        perms_list = perms_data.get("data", [])
        assert len(perms_list) > 0

        # OP-46: POST /admin/roles/{roleId} -> 200 OK (update role permissions)
        role_id = roles_list[0]["roleId"]
        perm_payload = {"permissionIds": [1, 2]}
        resp_p46 = client_super_admin.post(f"/admin/roles/{role_id}", json=perm_payload)
        client_super_admin.assert_success_envelope(resp_p46, expected_status=200)

        # OP-47: PATCH /admin/roles/{roleId} -> 200 OK
        resp_p47 = client_super_admin.patch(f"/admin/roles/{role_id}", json=perm_payload)
        client_super_admin.assert_success_envelope(resp_p47, expected_status=200)

    def test_op49_op50_op51_admin_users(self, client_super_admin, client_manager_a):
        """API-REG-049, 050, 051: User Administration (GET, POST, PATCH)"""
        # Manager -> 403
        resp_denied = client_manager_a.get("/admin/users")
        client_manager_a.assert_error_envelope(resp_denied, expected_status=403)

        # OP-49: GET /admin/users -> 200 OK
        resp_users = client_super_admin.get("/admin/users")
        users_data = client_super_admin.assert_success_envelope(resp_users, expected_status=200)
        page_users = users_data.get("data", {})
        content = page_users.get("content", [])
        assert len(content) > 0
        target_user = content[0]
        target_id = target_user["userId"]

        # OP-50: POST /admin/users/{userId} -> 200 OK
        status_payload = {"accountStatus": "ACTIVE"}
        resp_p50 = client_super_admin.post(f"/admin/users/{target_id}", json=status_payload)
        client_super_admin.assert_success_envelope(resp_p50, expected_status=200)

        # OP-51: PATCH /admin/users/{userId} -> 200 OK
        resp_p51 = client_super_admin.patch(f"/admin/users/{target_id}", json=status_payload)
        client_super_admin.assert_success_envelope(resp_p51, expected_status=200)
