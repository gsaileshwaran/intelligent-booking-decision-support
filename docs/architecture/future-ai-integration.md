# Deferred AI Decision Engine Integration Specification

**Status:** DEFERRED (Future Architecture Specification Only)  
**Target Framework:** Python 3.11 + FastAPI + NumPy / Scikit-learn  

> [!IMPORTANT]
> The AI Decision Engine is **DEFERRED** per system design directives. The core transactional movie booking application functions completely without AI integration. This document defines the architectural boundary and contract for future extension.

---

## 1. Architectural Boundary Principles

1. **Spring Boot is Transactional Authority**: The Java Spring Boot application owns database transactions, seat reservations, user authentication, and payment state transitions.
2. **REST-Only Interface**: Spring Boot communicates with the FastAPI AI Decision Service exclusively via HTTP REST APIs.
3. **No Direct Database Access**: The FastAPI AI service **never** connects directly to MySQL. It operates strictly on request payloads passed by Spring Boot.
4. **No Transaction Ownership**: The AI service provides recommendations and decision scores; it does **not** create, modify, or lock bookings or seats.
5. **Circuit-Breaker Safety**: If the AI Decision Service is offline, timing out, or throwing errors, Spring Boot falls back gracefully to local heuristic/MCDM candidate evaluation (`RecommendationController`). Booking transactions are never interrupted or corrupted by AI service failures.

---

## 2. Proposed Architecture Sequence

```
┌──────────────┐         REST / JSON          ┌───────────────────────────┐
│              │ ── 1. Evaluate Candidates ──>│                           │
│ Spring Boot  │                              │  FastAPI AI Service       │
│ Backend      │                              │  (ML Candidate Scorer)    │
│              │ <── 2. Return Ranked List ───│                           │
└──────────────┘                              └───────────────────────────┘
```

---

## 3. Proposed Candidate Evaluation DTO Contract

### Request Payload (`POST /api/v1/recommendations/rank`)
```json
{
  "userId": 1,
  "userGenrePreferences": ["Sci-Fi", "Action"],
  "candidateMovieIds": [101, 102, 103],
  "contextTime": "2026-08-20T19:00:00Z"
}
```

### Response Payload
```json
{
  "status": "SUCCESS",
  "rankedCandidates": [
    { "movieId": 101, "score": 0.95, "reason": "Matches genre preference Sci-Fi" },
    { "movieId": 103, "score": 0.72, "reason": "Trending in current venue" }
  ]
}
```
