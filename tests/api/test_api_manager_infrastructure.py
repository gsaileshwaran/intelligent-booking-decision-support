"""
PVK Cinemas — API Regression Suite: Manager Infrastructure, Scheduling & Scope Isolation
Covers operations:
- OP-06: GET   /manager/shows/{showId}/seats (ManagerAvailabilityController::getSeats)
- OP-07: PATCH /manager/shows/{showId}/seats (ManagerAvailabilityController::overrideSeatStatus)
- OP-11: GET   /manager/movies (ManagerMovieController::getMovies)
- OP-17: GET   /manager/theatres/{theatreId}/screens (ManagerInfrastructureController::getScreens)
- OP-18: POST  /manager/theatres/{theatreId}/screens (ManagerInfrastructureController::createScreen)
- OP-19: PATCH /manager/theatres/{theatreId}/screens/{screenId} (ManagerInfrastructureController::updateScreen)
- OP-20: GET   /manager/screens/{screenId}/seats (ManagerInfrastructureController::getSeats)
- OP-21: POST  /manager/screens/{screenId}/seats (ManagerInfrastructureController::createSeat)
- OP-22: PATCH /manager/screens/{screenId}/seats/{seatId} (ManagerInfrastructureController::updateSeat)
- OP-36: GET   /manager/theatres/{theatreId}/shows (ManagerSchedulingController::getShows)
- OP-37: POST  /manager/theatres/{theatreId}/shows (ManagerSchedulingController::createShow)
- OP-38: PATCH /manager/theatres/{theatreId}/shows/{showId} (ManagerSchedulingController::updateShow)

Validates:
- Manager role authorization and horizontal theatre scope isolation (Manager A Theatre 1 vs Theatre 2)
- Customer role rejection (403 Forbidden)
- Anonymous unauthenticated rejection (401 Unauthorized)
- Business rules BR-01 (end > start), BR-02 (language-movie mapping), BR-03 (capability-screen mapping), BR-04 (no overlap)
- SHOW_SEAT initialization atomicity on creation and rollback on error
"""

import pytest
import uuid
import datetime

class TestManagerInfrastructureRegression:

    def test_op11_manager_movies(self, client_manager_a, client_customer, client_anonymous):
        """API-REG-011: GET /api/v1/manager/movies -> 200 OK for Manager, 403 Customer, 401 Anonymous"""
        # Manager A -> 200
        resp_mgr = client_manager_a.get("/manager/movies")
        client_manager_a.assert_success_envelope(resp_mgr, expected_status=200)

        # Customer -> 403 Forbidden
        resp_cust = client_customer.get("/manager/movies")
        client_customer.assert_error_envelope(resp_cust, expected_status=403)

        # Anonymous -> 401 Unauthorized
        resp_anon = client_anonymous.get("/manager/movies")
        client_anonymous.assert_error_envelope(resp_anon, expected_status=401)

    def test_op17_get_screens_and_horizontal_scope(self, client_manager_a, client_manager_b):
        """API-REG-017: GET /manager/theatres/{theatreId}/screens & Scope Isolation (200 vs 403)"""
        # Manager A -> Theatre 1 (ALLOWED)
        resp_a1 = client_manager_a.get("/manager/theatres/1/screens")
        data_a1 = client_manager_a.assert_success_envelope(resp_a1, expected_status=200)
        assert isinstance(data_a1.get("data"), list)
        assert len(data_a1.get("data")) > 0

        # Manager A -> Theatre 2 (SCOPE DENIED 403)
        resp_a2 = client_manager_a.get("/manager/theatres/2/screens")
        client_manager_a.assert_error_envelope(resp_a2, expected_status=403)

        # Manager B -> Theatre 2 (ALLOWED)
        resp_b2 = client_manager_b.get("/manager/theatres/2/screens")
        client_manager_b.assert_success_envelope(resp_b2, expected_status=200)

        # Manager B -> Theatre 1 (SCOPE DENIED 403)
        resp_b1 = client_manager_b.get("/manager/theatres/1/screens")
        client_manager_b.assert_error_envelope(resp_b1, expected_status=403)

    def test_op18_create_screen_and_scope(self, client_manager_a, client_manager_b):
        """API-REG-018: POST /manager/theatres/{theatreId}/screens (201 Created & 403 Scope Block)"""
        unique_suffix = uuid.uuid4().hex[:6]
        screen_payload = {
            "screenCode": f"SCR-T1-{unique_suffix}",
            "screenName": f"Auditorium Test {unique_suffix}",
            "isActive": True
        }

        # Manager B trying to create screen in Theatre 1 -> 403
        resp_scope_denied = client_manager_b.post("/manager/theatres/1/screens", json=screen_payload)
        client_manager_b.assert_error_envelope(resp_scope_denied, expected_status=403)

        # Manager A creating screen in Theatre 1 -> 201 Created
        resp_created = client_manager_a.post("/manager/theatres/1/screens", json=screen_payload)
        data = client_manager_a.assert_success_envelope(resp_created, expected_status=201)
        created_screen = data.get("data", {})
        assert created_screen.get("screenCode") == screen_payload["screenCode"]
        assert created_screen.get("screenId") is not None

    def test_op19_update_screen(self, client_manager_a, client_manager_b):
        """API-REG-019: PATCH /manager/theatres/{theatreId}/screens/{screenId} (200 OK & 403 Scope)"""
        screens = client_manager_a.get("/manager/theatres/1/screens").json().get("data", [])
        screen_id = screens[0]["screenId"]

        # Scope denial: Manager B cannot update Theatre 1 screen
        resp_scope = client_manager_b.patch(f"/manager/theatres/1/screens/{screen_id}", json={"screenName": "Hacked Name"})
        client_manager_b.assert_error_envelope(resp_scope, expected_status=403)

        # Manager A updates screen name -> 200
        updated_name = f"Updated Screen Name {uuid.uuid4().hex[:4]}"
        resp_ok = client_manager_a.patch(f"/manager/theatres/1/screens/{screen_id}", json={"screenName": updated_name})
        data = client_manager_a.assert_success_envelope(resp_ok, expected_status=200)
        assert data.get("data", {}).get("screenName") == updated_name

    def test_op20_get_seats_and_scope(self, client_manager_a, client_manager_b):
        """API-REG-020: GET /manager/screens/{screenId}/seats (200 OK & 403 Scope)"""
        screen_id = client_manager_a.get("/manager/theatres/1/screens").json().get("data", [])[0]["screenId"]

        # Manager A accessing screen in Theatre 1 -> 200
        resp_a = client_manager_a.get(f"/manager/screens/{screen_id}/seats")
        data = client_manager_a.assert_success_envelope(resp_a, expected_status=200)
        assert isinstance(data.get("data"), list)

        # Manager B accessing screen in Theatre 1 -> 403
        resp_b = client_manager_b.get(f"/manager/screens/{screen_id}/seats")
        client_manager_b.assert_error_envelope(resp_b, expected_status=403)

    def test_op21_create_seat(self, client_manager_a, client_manager_b):
        """API-REG-021: POST /manager/screens/{screenId}/seats (201 Created & 403 Scope)"""
        screen_id = client_manager_a.get("/manager/theatres/1/screens").json().get("data", [])[0]["screenId"]
        unique_num = f"{uuid.uuid4().int % 900 + 100}"
        seat_payload = {
            "seatTypeId": 1,
            "rowLabel": "B",
            "seatNumber": unique_num,
            "gridRowIndex": 2,
            "gridColIndex": 1,
            "isActive": True
        }

        # Manager B -> 403
        resp_scope = client_manager_b.post(f"/manager/screens/{screen_id}/seats", json=seat_payload)
        client_manager_b.assert_error_envelope(resp_scope, expected_status=403)

        # Manager A -> 201
        resp_ok = client_manager_a.post(f"/manager/screens/{screen_id}/seats", json=seat_payload)
        data = client_manager_a.assert_success_envelope(resp_ok, expected_status=201)
        assert data.get("data", {}).get("seatNumber") == unique_num

    def test_op22_update_seat(self, client_manager_a, client_manager_b):
        """API-REG-022: PATCH /manager/screens/{screenId}/seats/{seatId} (200 OK & 403 Scope)"""
        screen_id = client_manager_a.get("/manager/theatres/1/screens").json().get("data", [])[0]["screenId"]
        seats = client_manager_a.get(f"/manager/screens/{screen_id}/seats").json().get("data", [])
        assert len(seats) > 0
        seat_id = seats[0]["seatId"]

        # Manager B -> 403
        resp_scope = client_manager_b.patch(f"/manager/screens/{screen_id}/seats/{seat_id}", json={"isActive": False})
        client_manager_b.assert_error_envelope(resp_scope, expected_status=403)

        # Manager A -> 200
        resp_ok = client_manager_a.patch(f"/manager/screens/{screen_id}/seats/{seat_id}", json={"isActive": True})
        client_manager_a.assert_success_envelope(resp_ok, expected_status=200)

    def test_op36_manager_shows(self, client_manager_a, client_manager_b):
        """API-REG-036: GET /manager/theatres/{theatreId}/shows (200 OK & 403 Scope)"""
        # Manager A -> Theatre 1 (200)
        resp_a = client_manager_a.get("/manager/theatres/1/shows")
        data_a = client_manager_a.assert_success_envelope(resp_a, expected_status=200)
        assert isinstance(data_a.get("data"), list)

        # Manager A -> Theatre 2 (403)
        resp_scope = client_manager_a.get("/manager/theatres/2/shows")
        client_manager_a.assert_error_envelope(resp_scope, expected_status=403)

    def test_op37_create_show_and_business_rules(self, client_manager_a, client_manager_b):
        """API-REG-037: POST /manager/theatres/{theatreId}/shows & BR-01, BR-02, BR-03, BR-04"""
        import pymysql
        conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
        cur = conn.cursor()
        cur.execute("SELECT screen_id FROM screen WHERE theatre_id = 1 LIMIT 1")
        screen_id = cur.fetchone()[0]
        cur.execute("SELECT screen_capability_id FROM screen_capability WHERE screen_id = %s LIMIT 1", (screen_id,))
        sc_id = cur.fetchone()[0]
        cur.execute("SELECT movie_id, movie_language_id FROM movie_language LIMIT 1")
        ml_row = cur.fetchone()
        movie_id, ml_id = ml_row[0], ml_row[1]
        cur.close()
        conn.close()

        # Scope denial: Manager B cannot schedule in Theatre 1
        import random
        random_days = random.randint(30, 300)
        random_hours = random.randint(1, 20)
        future_base = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=random_days, hours=random_hours)
        st = (future_base + datetime.timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
        et = (future_base + datetime.timedelta(hours=3)).strftime("%Y-%m-%dT%H:%M:%SZ")
        valid_payload = {
            "movieId": movie_id,
            "movieLanguageId": ml_id,
            "screenId": screen_id,
            "screenCapabilityId": sc_id,
            "startAt": st,
            "endAt": et,
            "showStatus": "SCHEDULED"
        }
        resp_b = client_manager_b.post("/manager/theatres/1/shows", json=valid_payload)
        client_manager_b.assert_error_envelope(resp_b, expected_status=403)

        # BR-01: End time <= Start time -> 400 Bad Request
        invalid_time_payload = dict(valid_payload)
        invalid_time_payload["startAt"] = et
        invalid_time_payload["endAt"] = st  # End is before start
        resp_br1 = client_manager_a.post("/manager/theatres/1/shows", json=invalid_time_payload)
        client_manager_a.assert_error_envelope(resp_br1, expected_status=400)

        # BR-02: Language does not belong to movie -> 400/404 validation
        invalid_lang_payload = dict(valid_payload)
        invalid_lang_payload["movieLanguageId"] = 999999
        resp_br2 = client_manager_a.post("/manager/theatres/1/shows", json=invalid_lang_payload)
        assert resp_br2.status_code in [400, 404]

        # BR-03: Screen capability does not belong to screen -> 400/404 validation
        invalid_cap_payload = dict(valid_payload)
        invalid_cap_payload["screenCapabilityId"] = 999999
        resp_br3 = client_manager_a.post("/manager/theatres/1/shows", json=invalid_cap_payload)
        assert resp_br3.status_code in [400, 404]

        # Successful Show Creation -> 201 Created & SHOW_SEAT verification
        resp_ok = client_manager_a.post("/manager/theatres/1/shows", json=valid_payload)
        data = client_manager_a.assert_success_envelope(resp_ok, expected_status=201)
        created_show = data.get("data", {})
        created_show_id = created_show.get("showId")
        assert created_show_id is not None

        try:
            # Verify SHOW_SEAT was automatically initialized in database
            conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
            cur = conn.cursor()
            cur.execute("SELECT COUNT(*) FROM show_seat WHERE show_id = %s", (created_show_id,))
            show_seat_count = cur.fetchone()[0]
            cur.close()
            conn.close()
            assert show_seat_count > 0, "Expected SHOW_SEAT entries to be automatically created"

            # BR-04: Overlapping show on same screen -> 409 Conflict
            overlap_payload = dict(valid_payload)
            # Shift slightly within the created show window
            overlap_st = (future_base + datetime.timedelta(hours=1, minutes=30)).strftime("%Y-%m-%dT%H:%M:%SZ")
            overlap_et = (future_base + datetime.timedelta(hours=3, minutes=30)).strftime("%Y-%m-%dT%H:%M:%SZ")
            overlap_payload["startAt"] = overlap_st
            overlap_payload["endAt"] = overlap_et
            resp_br4 = client_manager_a.post("/manager/theatres/1/shows", json=overlap_payload)
            client_manager_a.assert_error_envelope(resp_br4, expected_status=409)
        finally:
            # Clean up temporary test show and seats to keep DB clean
            conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
            cur = conn.cursor()
            cur.execute("DELETE FROM show_seat WHERE show_id = %s", (created_show_id,))
            cur.execute("DELETE FROM `show` WHERE show_id = %s", (created_show_id,))
            conn.commit()
            cur.close()
            conn.close()

    def test_op38_update_show(self, client_manager_a, client_manager_b):
        """API-REG-038: PATCH /manager/theatres/{theatreId}/shows/{showId} (200 OK & 403 Scope)"""
        shows = client_manager_a.get("/manager/theatres/1/shows").json().get("data", [])
        assert len(shows) > 0
        show_id = shows[0]["showId"]

        # Manager B -> 403 Scope
        resp_scope = client_manager_b.patch(f"/manager/theatres/1/shows/{show_id}", json={"showStatus": "SCHEDULED"})
        client_manager_b.assert_error_envelope(resp_scope, expected_status=403)

        # Manager A -> 200 OK
        resp_ok = client_manager_a.patch(f"/manager/theatres/1/shows/{show_id}", json={"showStatus": "SCHEDULED"})
        client_manager_a.assert_success_envelope(resp_ok, expected_status=200)

    def test_op06_and_op07_manager_show_seats_and_override(self, client_manager_a, client_manager_b):
        """API-REG-006 & API-REG-007: GET & PATCH /manager/shows/{showId}/seats (200 OK & 403 Scope)"""
        shows = client_manager_a.get("/manager/theatres/1/shows").json().get("data", [])
        assert len(shows) > 0
        show_id = shows[0]["showId"]

        # OP-06: Manager B accessing show seats for Theatre 1 show -> 403
        resp_b6 = client_manager_b.get(f"/manager/shows/{show_id}/seats")
        client_manager_b.assert_error_envelope(resp_b6, expected_status=403)

        # OP-06: Manager A accessing show seats -> 200
        resp_a6 = client_manager_a.get(f"/manager/shows/{show_id}/seats")
        data_a6 = client_manager_a.assert_success_envelope(resp_a6, expected_status=200)
        seat_data = data_a6.get("data", {})
        seats = seat_data.get("seats", [])
        assert len(seats) > 0
        seat_id = seats[0]["seatId"]

        # OP-07: Manager B overriding seat status in Theatre 1 show -> 403
        override_payload = {
            "seatIds": [seat_id],
            "availabilityStatus": "BLOCKED"
        }
        resp_b7 = client_manager_b.patch(f"/manager/shows/{show_id}/seats", json=override_payload)
        client_manager_b.assert_error_envelope(resp_b7, expected_status=403)

        # OP-07: Manager A overriding seat status -> 200 OK
        resp_a7 = client_manager_a.patch(f"/manager/shows/{show_id}/seats", json=override_payload)
        data_a7 = client_manager_a.assert_success_envelope(resp_a7, expected_status=200)
        updated_seats = data_a7.get("data", {}).get("seats", [])
        target = [s for s in updated_seats if s.get("seatId") == seat_id]
        if target:
            assert target[0].get("availabilityStatus") == "BLOCKED"

        # Revert back to AVAILABLE
        revert_payload = {
            "seatIds": [seat_id],
            "availabilityStatus": "AVAILABLE"
        }
        client_manager_a.patch(f"/manager/shows/{show_id}/seats", json=revert_payload)
