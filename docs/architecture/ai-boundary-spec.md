# AI Decision Engine Boundary Specification

**Document Version:** 1.0  
**Classification:** Authoritative Decision Boundary Specification  
**Status:** Architecture Baseline / Implementation Deferred  

---

## 1. Boundary & Governance Rules

The AI Decision Engine (`ai-service/`) is an **isolated decision-support component**. To maintain system security, operational stability, and data integrity, the following rules govern its execution:

### Mandatory Rules
1. **DEFERRED IMPLEMENTATION**: Machine learning training, complex recommendation pipelines, FastAPI endpoints, and ranking algorithms are explicitly deferred during the current development phase.
2. **ZERO DATABASE ACCESS**: The AI Decision Service has **no database credentials** and **no connection** to the MySQL database. All domain ground truth remains inside Spring Boot.
3. **NO DIRECT BROWSER INTERACTION**: The frontend React client must **never call the AI service directly**. All recommendation requests flow through Spring Boot (`POST /api/recommendations/evaluate`).
4. **NO TRANSACTIONAL AUTHORITY**: The AI service cannot modify seat availability, lock inventory, create bookings, or process payments. It returns purely advisory ranked candidate lists.
5. **DETERMINISTIC FALLBACK**: Spring Boot maintains a local Java fallback scoring algorithm that triggers automatically if the AI service is disabled, unreachable, or responds with latency exceeding 3000ms.

---

## 2. Decision Pipeline & Scoring Model

When activated, the decision engine evaluates eligible candidate shows using a two-stage filter and score approach:

```
[ Eligible Shows from Spring Boot ] -> [ 1. Hard Constraint Filter ] -> [ 2. Multi-Criteria Scoring ] -> [ 3. Ranking & Explanation ]
```

### Stage 1: Hard Constraint Filtering
Non-negotiable criteria filter out ineligible candidate shows before scoring:
* **Availability**: $SeatsAvailable \ge GroupSize$
* **Max Budget**: $ShowPrice \le CustomerMaxBudget$
* **Time Boundary**: $ShowStartTime \ge PreferredTimeStart$ and $ShowEndTime \le PreferredTimeEnd$
* **Max Distance**: $TheatreDistance \le CustomerMaxDistance$

### Stage 2: Multi-Criteria Decision Model (MCDM) Scoring
For each feasible candidate show $i$, normalized scores $x_{ij} \in [0, 1]$ are calculated for criteria $j$, weighted by importance $w_j$:

$$S_i = \sum_{j=1}^{M} (w_j \times x_{ij}) \quad \text{where} \quad \sum_{j=1}^{M} w_j = 1.0$$

#### Core Criteria & Normalization Metrics
1. **Price Fit ($x_{i,price}$)**: Measures how favourably the ticket price compares to the user's maximum budget.
2. **Time Proximity ($x_{i,time}$)**: Measures proximity of show start time to preferred target time.
3. **Seat Quality Match ($x_{i,seat}$)**: Alignment between available seat categories and preferred category (`BALCONY`, `PREMIUM`, `REGULAR`).
4. **Distance Proximity ($x_{i,dist}$)**: Proximity of theatre location to customer's location.
5. **Occupancy Ratio ($x_{i,occupancy}$)**: Optimal filling level (penalizing completely empty or nearly full shows based on user preference).

### Stage 3: Explainable Ranking Output
Each evaluated candidate returns:
* `rank`: Position in ranked list (1, 2, 3...).
* `suitabilityScore`: Composite score $S_i \in [0, 100]$.
* `explanationFactors`: Key factors driving the rank (e.g., `"Matches budget (95% fit), exact seat category match"`).
* `alternativeNotice`: Next-best alternative tag when top preference is unavailable.

---

## 3. Spring Boot AI Proxy Interface (Deferred Contract)

### Request Payload (Spring Boot -> FastAPI)
```json
{
  "customerId": 101,
  "groupSize": 2,
  "preferences": {
    "maxBudgetPerTicket": 15.00,
    "preferredTimeStart": "2026-08-20T18:00:00",
    "preferredTimeEnd": "2026-08-20T22:00:00",
    "preferredSeatCategory": "PREMIUM",
    "maxDistanceKm": 15.0
  },
  "candidates": [
    {
      "showId": 501,
      "movieId": 12,
      "theatreId": 3,
      "theatreName": "Grand Cinema Hub",
      "distanceKm": 4.2,
      "showTime": "2026-08-20T19:30:00",
      "basePrice": 12.50,
      "availableSeats": 45,
      "totalSeats": 120,
      "availableCategories": ["PREMIUM", "REGULAR"]
    }
  ]
}
```

### Response Payload (FastAPI -> Spring Boot)
```json
{
  "recommendationId": "rec_98234712",
  "evaluatedCount": 1,
  "rankedResults": [
    {
      "rank": 1,
      "showId": 501,
      "suitabilityScore": 92.5,
      "scoreBreakdown": {
        "priceScore": 1.0,
        "timeScore": 0.95,
        "seatScore": 1.0,
        "distanceScore": 0.85
      },
      "explanation": "Optimal price fit, prime evening showtime, preferred Premium seats available.",
      "isBestMatch": true
    }
  ]
}
```
