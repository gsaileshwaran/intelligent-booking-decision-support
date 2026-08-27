import uuid
from datetime import datetime
from typing import List, Tuple
from app.schemas import (
    RecommendationRequest,
    RecommendationResponse,
    RankedResult,
    ScoreBreakdown,
    ShowCandidate,
)

MODEL_VERSION = "v1.0-fastapi-mcdm"

# Criteria Weights (Sum = 1.0)
WEIGHT_PRICE = 0.40
WEIGHT_TIME = 0.30
WEIGHT_SEAT = 0.15
WEIGHT_DIST = 0.15


def parse_time_to_minutes(time_str: str) -> int:
    """Helper to convert time string (HH:MM or ISO) to minutes from midnight."""
    if not time_str:
        return 720  # Default noon
    try:
        if "T" in time_str:
            dt = datetime.fromisoformat(time_str)
            return dt.hour * 60 + dt.minute
        parts = time_str.split(":")
        return int(parts[0]) * 60 + int(parts[1])
    except Exception:
        return 720


def evaluate_price_fit(candidate: ShowCandidate, max_budget: float | None) -> Tuple[float, str]:
    if max_budget is None or max_budget <= 0:
        return 0.85, f"Ticket price ₹{candidate.basePrice:.2f} is standard"
    
    # Linear fit ratio
    ratio = candidate.basePrice / max_budget
    if ratio <= 0.5:
        score = 1.0
        reason = f"Excellent price fit (₹{candidate.basePrice:.2f} is well below ₹{max_budget:.2f} max budget)"
    elif ratio <= 1.0:
        score = max(0.5, 1.0 - (ratio - 0.5))
        reason = f"Good price fit (₹{candidate.basePrice:.2f} within ₹{max_budget:.2f} max budget)"
    else:
        score = 0.0
        reason = f"Exceeds max budget (₹{candidate.basePrice:.2f} vs ₹{max_budget:.2f})"
    return score, reason


def evaluate_time_proximity(candidate: ShowCandidate, start_pref: str | None, end_pref: str | None) -> Tuple[float, str]:
    show_mins = parse_time_to_minutes(candidate.showTime)
    
    if start_pref and end_pref:
        start_mins = parse_time_to_minutes(start_pref)
        end_mins = parse_time_to_minutes(end_pref)
        
        if start_mins <= show_mins <= end_mins:
            return 1.0, f"Showtime matches preferred target window ({candidate.showTime})"
        else:
            diff_mins = min(abs(show_mins - start_mins), abs(show_mins - end_mins))
            diff_hours = diff_mins / 60.0
            score = max(0.20, 1.0 - (diff_hours * 0.20))
            return round(score, 2), f"Showtime is {diff_hours:.1f} hours outside preferred target window"
    
    # Default evening bias (18:00 to 22:00 preferred)
    if 1080 <= show_mins <= 1320:
        return 0.95, f"Prime evening showtime ({candidate.showTime})"
    return 0.80, f"Standard showtime ({candidate.showTime})"


def evaluate_seat_match(candidate: ShowCandidate, preferred_cat: str | None) -> Tuple[float, str]:
    if not preferred_cat:
        return 0.85, f"{candidate.availableSeats} seats available"
    
    pref_upper = preferred_cat.upper()
    avail_cats = [c.upper() for c in candidate.availableCategories] if candidate.availableCategories else []
    
    if pref_upper in avail_cats:
        return 1.0, f"Preferred {preferred_cat} seat category is available"
    elif avail_cats:
        return 0.50, f"Preferred {preferred_cat} unavailable, alternative categories available ({', '.join(candidate.availableCategories)})"
    return 0.60, f"{candidate.availableSeats} seats available"


def evaluate_distance(candidate: ShowCandidate, max_dist: float | None) -> Tuple[float, str]:
    if candidate.distanceKm is None:
        return 0.85, "Venue distance details standard"
    
    dist = candidate.distanceKm
    max_d = max_dist if max_dist and max_dist > 0 else 25.0
    
    if dist <= max_d:
        score = max(0.20, 1.0 - (dist / (max_d * 1.5)))
        return round(score, 2), f"Venue proximity within range ({dist:.1f} km away)"
    return 0.10, f"Venue is further than preferred max distance ({dist:.1f} km vs {max_d:.1f} km max)"


def evaluate_recommendations(request: RecommendationRequest) -> RecommendationResponse:
    if not request.candidates:
        return RecommendationResponse(
            recommendationId=f"rec_{uuid.uuid4().hex[:12]}",
            evaluatedCount=0,
            rankedResults=[],
            modelVersion=MODEL_VERSION,
        )

    prefs = request.preferences
    max_budget = prefs.maxBudgetPerTicket if prefs else None
    start_time_pref = prefs.preferredTimeStart if prefs else None
    end_time_pref = prefs.preferredTimeEnd if prefs else None
    seat_pref = prefs.preferredSeatCategory if prefs else None
    max_dist = prefs.maxDistanceKm if prefs else None

    group_size = request.groupSize

    evaluations = []

    for candidate in request.candidates:
        # 1. Hard Constraint Filter: Group Size Availability
        if candidate.availableSeats < group_size:
            continue

        # 2. Hard Constraint Filter: Maximum Budget Exceeded
        if max_budget is not None and candidate.basePrice > max_budget:
            continue

        # 3. Hard Constraint Filter: Distance Exceeded
        if max_dist is not None and candidate.distanceKm is not None and candidate.distanceKm > max_dist:
            continue

        # Multi-Criteria Decision Model (MCDM)
        price_score, price_reason = evaluate_price_fit(candidate, max_budget)
        time_score, time_reason = evaluate_time_proximity(candidate, start_time_pref, end_time_pref)
        seat_score, seat_reason = evaluate_seat_match(candidate, seat_pref)
        dist_score, dist_reason = evaluate_distance(candidate, max_dist)

        composite_score = (
            (WEIGHT_PRICE * price_score)
            + (WEIGHT_TIME * time_score)
            + (WEIGHT_SEAT * seat_score)
            + (WEIGHT_DIST * dist_score)
        )

        suitability = round(composite_score * 100.0, 1)

        breakdown = ScoreBreakdown(
            priceFit=price_score,
            timeProximity=time_score,
            seatCategoryMatch=seat_score,
            distanceProximity=dist_score,
        )

        factors = [
            price_reason,
            time_reason,
            seat_reason,
            dist_reason,
            f"Group capacity requirement met ({candidate.availableSeats} open seats for group size {group_size})",
        ]

        theatre_str = f" at {candidate.theatreName}" if candidate.theatreName else ""
        explanation_summary = f"Suitability score {suitability:.1f}%{theatre_str}. {price_reason}. {time_reason}."

        evaluations.append({
            "showId": candidate.showId,
            "suitabilityScore": suitability,
            "breakdown": breakdown,
            "explanation": explanation_summary,
            "explanationFactors": factors,
        })

    # Sort descending by suitability score, tie-break by showId ascending
    evaluations.sort(key=lambda x: (-x["suitabilityScore"], x["showId"]))

    ranked_results: List[RankedResult] = []
    for idx, item in enumerate(evaluations, start=1):
        alt_notice = None
        if idx > 1:
            alt_notice = f"Alternative Choice (Rank #{idx}, {item['suitabilityScore']:.1f}% suitability match)"

        ranked_results.append(
            RankedResult(
                rank=idx,
                showId=item["showId"],
                suitabilityScore=item["suitabilityScore"],
                scoreBreakdown=item["breakdown"],
                explanation=item["explanation"],
                explanationFactors=item["explanationFactors"],
                isBestMatch=(idx == 1),
                alternativeNotice=alt_notice,
            )
        )

    rec_id = f"rec_{uuid.uuid4().hex[:12]}"

    return RecommendationResponse(
        recommendationId=rec_id,
        evaluatedCount=len(request.candidates),
        rankedResults=ranked_results,
        modelVersion=MODEL_VERSION,
    )
