"""
PVK Cinemas — API Regression Suite: Customer Discovery & Search
Covers operations:
- OP-12: GET  /movies (MovieController::getMovies)
- OP-13: GET  /movies/{movieId} (MovieController::getMovie)
- OP-14: GET  /movies/{movieId}/genres (MovieController::getMovieGenres)
- OP-15: GET  /movies/{movieId}/languages (MovieController::getMovieLanguages)
- OP-16: GET  /movies/{movieId}/shows (MovieController::getMovieShows)
- OP-31: GET  /cities (CityController::getCities)
- OP-32: GET  /cities/{cityId}/theatres (CityController::getTheatresByCity)
- OP-33: GET  /theatres/{theatreId} (TheatreController::getTheatre)
- OP-34: GET  /theatres/{theatreId}/screens (TheatreController::getScreens)
- OP-35: GET  /theatres/{theatreId}/shows (TheatreController::getShows)
- OP-39: GET  /shows/{showId} (ShowController::getShow)
- OP-40: GET  /shows/{showId}/seats (ShowController::getShowSeats)
- OP-43: GET  /search (SearchController::searchGet)
- OP-44: POST /search (SearchController::searchPost)

Validates:
- Public unauthenticated accessibility for all 14 discovery operations
- Standard ApiResponse envelopes and expected response schema
- Read-only seat availability boundary (AVAILABLE, BOOKED, BLOCKED)
- Negative assertions: zero booking, cart, checkout, payment, hold attributes
- Search proxy operation from Spring Boot to FastAPI search service
"""

import pytest

class TestCustomerDiscoveryRegression:

    def test_op12_get_movies(self, client_anonymous):
        """API-REG-012: GET /api/v1/movies -> 200 OK (Public Movie Catalogue)"""
        resp = client_anonymous.get("/movies")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        # Spring Page response inside ApiResponse
        page_data = data.get("data", {})
        assert "content" in page_data or isinstance(data.get("data"), list)
        items = page_data.get("content", []) if isinstance(page_data, dict) else data.get("data", [])
        assert len(items) > 0, "Expected seed movies to be returned"

    def test_op13_get_movie_by_id(self, client_anonymous):
        """API-REG-013: GET /api/v1/movies/{movieId} -> 200 OK"""
        # First retrieve movies to pick a valid ID
        resp = client_anonymous.get("/movies")
        data = resp.json()
        items = data.get("data", {}).get("content", []) if isinstance(data.get("data"), dict) else data.get("data", [])
        assert len(items) > 0
        movie_id = items[0]["movieId"]

        detail_resp = client_anonymous.get(f"/movies/{movie_id}")
        detail_data = client_anonymous.assert_success_envelope(detail_resp, expected_status=200)
        assert detail_data.get("data", {}).get("movieId") == movie_id
        assert "title" in detail_data.get("data", {})

    def test_op13_get_movie_not_found(self, client_anonymous):
        """API-REG-013-ERR: GET /api/v1/movies/{movieId} -> 404 Not Found"""
        resp = client_anonymous.get("/movies/99999999")
        client_anonymous.assert_error_envelope(resp, expected_status=404)

    def test_op14_get_movie_genres(self, client_anonymous):
        """API-REG-014: GET /api/v1/movies/{movieId}/genres -> 200 OK"""
        resp = client_anonymous.get("/movies")
        movie_id = resp.json().get("data", {}).get("content", [])[0]["movieId"]

        genres_resp = client_anonymous.get(f"/movies/{movie_id}/genres")
        genres_data = client_anonymous.assert_success_envelope(genres_resp, expected_status=200)
        assert isinstance(genres_data.get("data"), list)

    def test_op15_get_movie_languages(self, client_anonymous):
        """API-REG-015: GET /api/v1/movies/{movieId}/languages -> 200 OK"""
        resp = client_anonymous.get("/movies")
        movie_id = resp.json().get("data", {}).get("content", [])[0]["movieId"]

        langs_resp = client_anonymous.get(f"/movies/{movie_id}/languages")
        langs_data = client_anonymous.assert_success_envelope(langs_resp, expected_status=200)
        assert isinstance(langs_data.get("data"), list)

    def test_op16_get_movie_shows(self, client_anonymous):
        """API-REG-016: GET /api/v1/movies/{movieId}/shows -> 200 OK"""
        resp = client_anonymous.get("/movies")
        movie_id = resp.json().get("data", {}).get("content", [])[0]["movieId"]

        shows_resp = client_anonymous.get(f"/movies/{movie_id}/shows")
        shows_data = client_anonymous.assert_success_envelope(shows_resp, expected_status=200)
        assert isinstance(shows_data.get("data"), list)

    def test_op31_get_cities(self, client_anonymous):
        """API-REG-031: GET /api/v1/cities -> 200 OK"""
        resp = client_anonymous.get("/cities")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        cities = data.get("data", [])
        assert isinstance(cities, list)
        assert len(cities) >= 4  # Seeded cities: Bengaluru, Chennai, Coimbatore, Hyderabad
        city_names = [c.get("cityName") for c in cities]
        assert "Bengaluru" in city_names

    def test_op32_get_city_theatres(self, client_anonymous):
        """API-REG-032: GET /api/v1/cities/{cityId}/theatres -> 200 OK"""
        resp = client_anonymous.get("/cities")
        city_id = resp.json().get("data", [])[0]["cityId"]

        theatres_resp = client_anonymous.get(f"/cities/{city_id}/theatres")
        theatres_data = client_anonymous.assert_success_envelope(theatres_resp, expected_status=200)
        assert isinstance(theatres_data.get("data"), list)

    def test_op33_get_theatre(self, client_anonymous):
        """API-REG-033: GET /api/v1/theatres/{theatreId} -> 200 OK"""
        resp = client_anonymous.get("/theatres/1")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        theatre = data.get("data", {})
        assert theatre.get("theatreId") == 1
        assert "theatreName" in theatre

    def test_op34_get_theatre_screens(self, client_anonymous):
        """API-REG-034: GET /api/v1/theatres/{theatreId}/screens -> 200 OK"""
        resp = client_anonymous.get("/theatres/1/screens")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        assert isinstance(data.get("data"), list)

    def test_op35_get_theatre_shows(self, client_anonymous):
        """API-REG-035: GET /api/v1/theatres/{theatreId}/shows -> 200 OK"""
        resp = client_anonymous.get("/theatres/1/shows")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        assert isinstance(data.get("data"), list)

    def test_op39_get_show_by_id(self, client_anonymous, client_manager_a):
        """API-REG-039: GET /api/v1/shows/{showId} -> 200 OK"""
        # Ensure a show exists in Theatre 1 or retrieve existing
        theatre_shows = client_anonymous.get("/theatres/1/shows").json().get("data", [])
        if theatre_shows:
            show_id = theatre_shows[0]["showId"]
        else:
            # Check all shows from DB or create a test show
            import pymysql
            conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
            cur = conn.cursor()
            cur.execute("SELECT show_id FROM `show` LIMIT 1")
            row = cur.fetchone()
            cur.close()
            conn.close()
            assert row is not None, "Expected at least one show in database"
            show_id = row[0]

        resp = client_anonymous.get(f"/shows/{show_id}")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        show = data.get("data", {})
        assert show.get("showId") == show_id
        assert "startTime" in show or "startAt" in show

    def test_op40_get_show_seats_read_only_boundary(self, client_anonymous):
        """API-REG-040: GET /api/v1/shows/{showId}/seats -> 200 OK & Read-Only Invariant Assertions"""
        import pymysql
        conn = pymysql.connect(host="127.0.0.1", port=3306, user="root", password="root", database="pvk_cinemas_db")
        cur = conn.cursor()
        cur.execute("SELECT show_id FROM `show` LIMIT 1")
        show_id = cur.fetchone()[0]
        cur.close()
        conn.close()

        resp = client_anonymous.get(f"/shows/{show_id}/seats")
        data = client_anonymous.assert_success_envelope(resp, expected_status=200)
        seat_data = data.get("data", {})

        # 1. Verify expected informational fields
        assert "showId" in seat_data
        assert "totalSeats" in seat_data
        assert "availableSeats" in seat_data
        assert "bookedSeats" in seat_data
        assert "blockedSeats" in seat_data
        assert "seats" in seat_data

        # 2. Verify seat statuses strictly within {AVAILABLE, BOOKED, BLOCKED}
        seats_list = seat_data.get("seats", [])
        valid_statuses = {"AVAILABLE", "BOOKED", "BLOCKED"}
        for s in seats_list:
            status = s.get("availabilityStatus")
            assert status in valid_statuses, f"Unexpected seat status: {status}"

        # 3. CRITICAL NON-NEGOTIABLE BOUNDARY: No booking/hold/payment attributes
        forbidden_attrs = ["reservationId", "holdToken", "checkoutUrl", "cartId", "bookingId", "paymentStatus", "lockExpiry"]
        for attr in forbidden_attrs:
            assert attr not in seat_data, f"Forbidden attribute '{attr}' leaked in show seats response"
            for s in seats_list:
                assert attr not in s, f"Forbidden attribute '{attr}' leaked in seat item"

    def test_op43_search_get(self, client_anonymous):
        """API-REG-043: GET /api/v1/search?q=Inception -> 200 OK (Hybrid Search Proxy)"""
        import time
        results = []
        for _ in range(5):
            resp = client_anonymous.get("/search?q=Inception")
            data = client_anonymous.assert_success_envelope(resp, expected_status=200)
            results = data.get("data", [])
            if len(results) > 0:
                break
            time.sleep(1.0)
        assert isinstance(results, list)
        assert len(results) > 0, "Expected search results from FastAPI search service proxy"
        first_card = results[0]
        assert "entityType" in first_card
        assert "title" in first_card
        assert "rankPosition" in first_card

    def test_op44_search_post(self, client_anonymous):
        """API-REG-044: POST /api/v1/search -> 200 OK (Hybrid Search Proxy via POST body)"""
        import time
        payload = {
            "query": "Inception",
            "cityId": 2
        }
        results = []
        for _ in range(5):
            resp = client_anonymous.post("/search", json=payload)
            data = client_anonymous.assert_success_envelope(resp, expected_status=200)
            results = data.get("data", [])
            if len(results) > 0:
                break
            time.sleep(1.0)
        assert isinstance(results, list)
        assert len(results) > 0
