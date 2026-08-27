from fastapi.testclient import TestClient
from app.main import app
from app.schemas import (
    RecommendationRequest,
    ShowCandidate,
    CustomerPreferences,
)
from app.engine import evaluate_recommendations, MODEL_VERSION

client = TestClient(app)


def test_health_check():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["modelVersion"] == MODEL_VERSION


def test_valid_recommendation_request():
    payload = {
        "customerId": 1,
        "groupSize": 2,
        "preferences": {
            "maxBudgetPerTicket": 250.0,
            "preferredTimeStart": "18:00:00",
            "preferredTimeEnd": "22:00:00",
            "preferredSeatCategory": "PREMIUM",
            "maxDistanceKm": 15.0,
        },
        "candidates": [
            {
                "showId": 101,
                "movieId": 1,
                "theatreId": 1,
                "theatreName": "PVK Multiplex Grand",
                "showTime": "19:30:00",
                "basePrice": 200.0,
                "availableSeats": 50,
                "availableCategories": ["PREMIUM", "REGULAR"],
                "distanceKm": 4.5,
            },
            {
                "showId": 102,
                "movieId": 1,
                "theatreId": 2,
                "theatreName": "PVK Cinema IMAX",
                "showTime": "14:00:00",
                "basePrice": 240.0,
                "availableSeats": 20,
                "availableCategories": ["REGULAR"],
                "distanceKm": 12.0,
            },
        ],
    }
    response = client.post("/api/v1/recommend", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["evaluatedCount"] == 2
    assert len(data["rankedResults"]) == 2
    assert data["modelVersion"] == MODEL_VERSION
    assert data["rankedResults"][0]["rank"] == 1
    assert data["rankedResults"][0]["isBestMatch"] is True
    assert data["rankedResults"][0]["showId"] == 101


def test_budget_preference_affects_ranking():
    # Lower price show should rank higher when budget is constrained
    show_cheap = ShowCandidate(
        showId=1, movieId=1, theatreId=1, showTime="19:00:00", basePrice=150.0, availableSeats=30
    )
    show_expensive = ShowCandidate(
        showId=2, movieId=1, theatreId=1, showTime="19:00:00", basePrice=280.0, availableSeats=30
    )

    req = RecommendationRequest(
        groupSize=1,
        preferences=CustomerPreferences(maxBudgetPerTicket=300.0),
        candidates=[show_expensive, show_cheap],
    )
    res = evaluate_recommendations(req)
    assert res.rankedResults[0].showId == 1
    assert res.rankedResults[0].suitabilityScore > res.rankedResults[1].suitabilityScore


def test_preferred_time_affects_ranking():
    # Show matching time window 18:00 - 22:00 should rank higher
    show_prime = ShowCandidate(
        showId=1, movieId=1, theatreId=1, showTime="19:00:00", basePrice=200.0, availableSeats=30
    )
    show_morning = ShowCandidate(
        showId=2, movieId=1, theatreId=1, showTime="09:00:00", basePrice=200.0, availableSeats=30
    )

    req = RecommendationRequest(
        groupSize=1,
        preferences=CustomerPreferences(
            preferredTimeStart="18:00:00", preferredTimeEnd="22:00:00"
        ),
        candidates=[show_morning, show_prime],
    )
    res = evaluate_recommendations(req)
    assert res.rankedResults[0].showId == 1


def test_preferred_seat_type_affects_ranking():
    show_premium = ShowCandidate(
        showId=1, movieId=1, theatreId=1, showTime="19:00:00", basePrice=200.0, availableSeats=30, availableCategories=["PREMIUM"]
    )
    show_regular = ShowCandidate(
        showId=2, movieId=1, theatreId=1, showTime="19:00:00", basePrice=200.0, availableSeats=30, availableCategories=["REGULAR"]
    )

    req = RecommendationRequest(
        groupSize=1,
        preferences=CustomerPreferences(preferredSeatCategory="PREMIUM"),
        candidates=[show_regular, show_premium],
    )
    res = evaluate_recommendations(req)
    assert res.rankedResults[0].showId == 1


def test_group_size_filters_insufficient_seats():
    show_empty = ShowCandidate(
        showId=1, movieId=1, theatreId=1, showTime="19:00:00", basePrice=200.0, availableSeats=2
    )
    show_full = ShowCandidate(
        showId=2, movieId=1, theatreId=1, showTime="19:00:00", basePrice=200.0, availableSeats=10
    )

    req = RecommendationRequest(
        groupSize=5,
        candidates=[show_empty, show_full],
    )
    res = evaluate_recommendations(req)
    assert len(res.rankedResults) == 1
    assert res.rankedResults[0].showId == 2


def test_invalid_input_rejected():
    # Negative group size
    response = client.post("/api/v1/recommend", json={"groupSize": -1, "candidates": []})
    assert response.status_code == 422

    # Negative ticket price
    bad_candidate_payload = {
        "groupSize": 1,
        "candidates": [
            {
                "showId": 1,
                "movieId": 1,
                "theatreId": 1,
                "showTime": "19:00:00",
                "basePrice": -50.0,
                "availableSeats": 10,
            }
        ],
    }
    response2 = client.post("/api/v1/recommend", json=bad_candidate_payload)
    assert response2.status_code == 422


def test_empty_candidate_list_handled():
    req = RecommendationRequest(groupSize=2, candidates=[])
    res = evaluate_recommendations(req)
    assert res.evaluatedCount == 0
    assert len(res.rankedResults) == 0
    assert res.modelVersion == MODEL_VERSION


def test_deterministic_output_for_identical_input():
    show1 = ShowCandidate(showId=1, movieId=1, theatreId=1, showTime="19:00:00", basePrice=200.0, availableSeats=20)
    show2 = ShowCandidate(showId=2, movieId=1, theatreId=1, showTime="20:00:00", basePrice=180.0, availableSeats=20)

    req = RecommendationRequest(groupSize=2, candidates=[show1, show2])
    res1 = evaluate_recommendations(req)
    res2 = evaluate_recommendations(req)

    assert res1.rankedResults[0].showId == res2.rankedResults[0].showId
    assert res1.rankedResults[0].suitabilityScore == res2.rankedResults[0].suitabilityScore


def test_explanations_are_meaningful():
    show = ShowCandidate(
        showId=1, movieId=1, theatreId=1, theatreName="PVK Cinema", showTime="19:00:00", basePrice=180.0, availableSeats=25, availableCategories=["PREMIUM"]
    )
    req = RecommendationRequest(
        groupSize=2,
        preferences=CustomerPreferences(maxBudgetPerTicket=200.0, preferredSeatCategory="PREMIUM"),
        candidates=[show],
    )
    res = evaluate_recommendations(req)
    assert len(res.rankedResults) == 1
    result = res.rankedResults[0]
    assert len(result.explanation) > 10
    assert len(result.explanationFactors) >= 3
    assert any("budget" in f.lower() or "price" in f.lower() for f in result.explanationFactors)
    assert any("premium" in f.lower() for f in result.explanationFactors)


def test_alternative_recommendations_when_preferred_candidate_unavailable():
    # Scenario: Top preference (Show 101: prime time, low price) has only 1 seat available for group of 4 (hard filter excludes it).
    # Feasible alternatives (Show 102, Show 103) have 20 seats available.
    preferred_show_sold_out = ShowCandidate(
        showId=101, movieId=1, theatreId=1, showTime="19:30:00", basePrice=150.0, availableSeats=1
    )
    alternative_show_1 = ShowCandidate(
        showId=102, movieId=1, theatreId=1, theatreName="PVK Annex", showTime="20:00:00", basePrice=180.0, availableSeats=20
    )
    alternative_show_2 = ShowCandidate(
        showId=103, movieId=1, theatreId=2, theatreName="PVK City Center", showTime="21:15:00", basePrice=220.0, availableSeats=20
    )

    req = RecommendationRequest(
        groupSize=4,
        preferences=CustomerPreferences(maxBudgetPerTicket=250.0, preferredTimeStart="18:00:00", preferredTimeEnd="22:00:00"),
        candidates=[preferred_show_sold_out, alternative_show_1, alternative_show_2],
    )
    res = evaluate_recommendations(req)

    # Preferred show 101 must be filtered out due to insufficient seat capacity
    assert all(r.showId != 101 for r in res.rankedResults)

    # Next best available candidates returned as ranked alternatives
    assert len(res.rankedResults) == 2
    assert res.rankedResults[0].showId == 102
    assert res.rankedResults[0].isBestMatch is True
    assert res.rankedResults[0].alternativeNotice is None

    assert res.rankedResults[1].showId == 103
    assert res.rankedResults[1].isBestMatch is False
    assert res.rankedResults[1].alternativeNotice is not None
    assert "Alternative Choice" in res.rankedResults[1].alternativeNotice

