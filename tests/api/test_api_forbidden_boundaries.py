"""
PVK Cinemas — API Regression Suite: Forbidden Boundaries & Non-Existent Functionality
Verifies strict adherence to UI-08 display-only seat availability and absolute
absence of e-commerce / transaction endpoints (P14-B regression mandate).

Negative assertions verify that all requests to forbidden routes return:
- 404 Not Found (or 405 Method Not Allowed)
- No handler mapping or controller exists for transaction, hold, reservation, or payment endpoints
"""

import pytest

FORBIDDEN_ENDPOINTS = [
    "/bookings",
    "/bookings/1",
    "/bookings/create",
    "/reservations",
    "/reservations/1",
    "/payments",
    "/payments/charge",
    "/payments/refund",
    "/checkout",
    "/checkout/process",
    "/cart",
    "/cart/items",
    "/seat-holds",
    "/seat-holds/1",
    "/holds",
    "/refunds",
    "/orders",
    "/transactions",
]

class TestForbiddenBoundariesRegression:

    @pytest.mark.parametrize("endpoint", FORBIDDEN_ENDPOINTS)
    def test_forbidden_endpoints_return_not_found_get(self, client_customer, endpoint):
        """Verify GET to forbidden endpoint returns 404/405 (no route mapped)"""
        resp = client_customer.get(endpoint)
        assert resp.status_code in [404, 405], f"Expected 404/405 for GET {endpoint}, got {resp.status_code}"

    @pytest.mark.parametrize("endpoint", FORBIDDEN_ENDPOINTS)
    def test_forbidden_endpoints_return_not_found_post(self, client_customer, endpoint):
        """Verify POST to forbidden endpoint returns 404/405 (no transaction creation possible)"""
        resp = client_customer.post(endpoint, json={"showSeatId": 1, "amount": 100})
        assert resp.status_code in [404, 405], f"Expected 404/405 for POST {endpoint}, got {resp.status_code}"

    def test_seat_availability_strictly_display_only(self, client_customer):
        """
        Verify OP-40 GET /shows/{id}/seats strictly exposes only display status:
        AVAILABLE, BOOKED, BLOCKED
        And does NOT expose hold, reservation, cart, lock tokens, or checkout links.
        """
        resp_shows = client_customer.get("/theatres/1/shows")
        shows = resp_shows.json().get("data", []) if resp_shows.status_code == 200 else []
        show_id = shows[0]["showId"] if shows else 4

        resp = client_customer.get(f"/shows/{show_id}/seats")
        data = client_customer.assert_success_envelope(resp, expected_status=200)
        seats = data.get("data", {}).get("seats", [])
        assert len(seats) > 0

        forbidden_keys = {"holdToken", "lockId", "lockedUntil", "reservationId", "cartId", "bookingUrl", "paymentUrl"}
        allowed_statuses = {"AVAILABLE", "BOOKED", "BLOCKED"}

        for s in seats:
            assert s.get("availabilityStatus") in allowed_statuses, f"Invalid seat status: {s.get('availabilityStatus')}"
            for fk in forbidden_keys:
                assert fk not in s, f"Seat object contained forbidden transaction property: {fk}"
