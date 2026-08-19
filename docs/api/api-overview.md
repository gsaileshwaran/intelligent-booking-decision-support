# REST API Overview & Integration Specification

**Document Version:** 2.0  
**Base Path:** `/api`  
**Authentication Header:** `Authorization: Bearer <JWT_TOKEN>`  
**Response Wrapper:** All API endpoints return a standardized `ApiResponse<T>` wrapper object.

---

## 1. Standard Response Structure

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-08-20T19:00:00.000"
}
```

### Error Response Payload
```json
{
  "success": false,
  "message": "Seat A1 is no longer available.",
  "data": null,
  "timestamp": "2026-08-20T19:00:00.000"
}
```

---

## 2. Implemented API Endpoints

### 2.1 Authentication & User Access (`/api/auth`)
* `POST /api/auth/register`: Register new account (`ROLE_CUSTOMER` or `ROLE_SERVICE_PROVIDER`). Returns JWT token and user info.
* `POST /api/auth/login`: Authenticate user credentials (`email`, `password`). Returns JWT token.

### 2.2 Customer Movies & Shows Discovery (`/api/movies`, `/api/shows`)
* `GET /api/movies`: List active catalogue movies.
* `GET /api/movies/{id}`: Get movie metadata, duration, genre, ratings.
* `GET /api/shows?movieId={id}&date={yyyy-MM-dd}`: Query scheduled showtimes.
* `GET /api/shows/{id}`: Get show details.
* `GET /api/shows/{id}/seat-map`: Retrieve real-time show seat inventory (`ShowSeat` status: `AVAILABLE`, `HELD`, `CONFIRMED`, `BOOKED`).

### 2.3 Seat Reservation & Booking Transactions (`/api/bookings`)
* `POST /api/bookings/hold`: Reserve temporary seat hold locks (`HELD`) for 10 minutes. Requires `{ showId, showSeatIds }`.
* `POST /api/bookings/{id}/confirm?paymentMethod=MOCK_CARD`: Confirm held booking and execute sandbox payment simulation (`Payment.SUCCESS`).
* `POST /api/bookings/{id}/cancel`: Cancel confirmed or held booking and release reserved seats back to `AVAILABLE`.
* `GET /api/bookings/my-history`: Retrieve authenticated customer's booking history.

### 2.4 Service Provider Administration (`/api/provider`)
* `GET /api/provider/theatres`: Retrieve theatres owned by authenticated provider (`owner_user_id`).
* `POST /api/provider/theatres`: Register new cinema branch venue.
* `POST /api/provider/movies`: Add new film release to global catalogue.
* `POST /api/provider/shows?movieId={id}&screenId={id}`: Schedule new showtime and automatically populate `ShowSeat` inventory.

### 2.5 Deferred AI Decision Service Boundary (`/api/recommendations`)
* `POST /api/recommendations/evaluate`: Local Java MCDM fallback candidate evaluation engine.
