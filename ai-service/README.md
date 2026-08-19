# Future AI Decision Engine Boundary Specification

> ⚠️ **IMPORTANT ARCHITECTURAL RULE: THIS COMPONENT IS DEFERRED**  
> Machine learning, recommendation algorithm scripts, scoring pipelines, model training, and FastAPI implementations are **EXPLICITLY DEFERRED** during the current development phase.  
> This directory serves strictly as a **documented future integration boundary**.

---

## 📌 Component Purpose & Domain Boundary

When implemented in Phase 4, the AI Decision Service will operate as an **isolated, explainable decision-support engine**. It provides multi-criteria scoring and candidate ranking to assist customers in choosing among feasible booking options.

### Architectural Rules & Invariants
1. **Decision Support Only**: The AI service provides advisory scoring and explanations. It has **no transactional authority**.
2. **Zero Database Access**: The AI service does **not connect to the MySQL database**. All ground truth data (shows, seats, prices) is retrieved and supplied by Spring Boot.
3. **No Direct Browser Client Access**: The browser React application **never communicates directly with the AI service**. All request routing flows through Spring Boot (`POST /api/recommendations/evaluate`).
4. **Deterministic Fallback**: If this service is offline or deferred, Spring Boot executes an internal Java-based fallback scoring algorithm.

---

## 🧮 Decision & Scoring Framework (Planned Rule-Based MCDM)

The decision engine applies a Multi-Criteria Decision Model (MCDM):

1. **Hard Constraints (Filtering)**: Ineligible candidate shows are eliminated before scoring (e.g., $SeatsAvailable < GroupSize$, or $ShowPrice > MaxBudget$).
2. **Multi-Criteria Scoring Formula**:
   For each candidate show $i$, suitability score $S_i$ is computed:

   $$S_i = \sum_{j=1}^{M} (w_j \times x_{ij})$$

   * $w_j$: Configured weight for criterion $j$ (where $\sum w_j = 1.0$)
   * $x_{ij}$: Normalized score metric $\in [0, 1]$ for criterion $j$

3. **Core Decision Criteria**:
   * Ticket Price Fit ($w_{price}$)
   * Showtime Proximity ($w_{time}$)
   * Seat Category Match ($w_{seat}$)
   * Venue Distance Proximity ($w_{dist}$)
   * Screen Occupancy Level ($w_{occupancy}$)

---

## 🔌 API Boundary Contract (Spring Boot <-> FastAPI)

* **HTTP Method**: `POST`
* **Internal Endpoint**: `http://localhost:8000/api/v1/recommend`
* **Content-Type**: `application/json`

### Expected Input Payload
```json
{
  "customerId": 1,
  "groupSize": 2,
  "preferences": {
    "maxBudgetPerTicket": 20.00,
    "preferredTimeStart": "18:00:00",
    "preferredTimeEnd": "22:00:00",
    "preferredSeatCategory": "PREMIUM",
    "maxDistanceKm": 10.0
  },
  "candidates": [
    {
      "showId": 1,
      "movieId": 1,
      "theatreId": 1,
      "showTime": "2026-08-20T18:00:00",
      "basePrice": 15.00,
      "availableSeats": 30,
      "distanceKm": 4.2
    }
  ]
}
```

### Expected Output Payload
```json
{
  "recommendationId": "rec_uuid_109283",
  "rankedResults": [
    {
      "rank": 1,
      "showId": 1,
      "suitabilityScore": 95.0,
      "explanation": "Exact match for budget ($15 vs $20 max), prime time 18:00, Premium seats available.",
      "scoreBreakdown": {
        "priceFit": 1.0,
        "timeProximity": 1.0,
        "seatCategoryMatch": 1.0,
        "distanceProximity": 0.85
      }
    }
  ]
}
```
