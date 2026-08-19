# REST API Specification

**Document Version:** 1.0  
**Base URL:** `/api`  
**Authentication:** HTTP Headers `Authorization: Bearer <JWT_TOKEN>`  

---

## 1. Authentication & User Management (`/api/auth`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new customer or service provider account. |
| `POST` | `/api/auth/login` | Public | Authenticate user credentials and return JWT token. |
| `GET` | `/api/auth/profile` | Authenticated | Retrieve profile details of currently authenticated user. |

---

## 2. Customer Movie & Show Browsing (`/api/movies`, `/api/shows`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/movies` | Public | List now-showing and upcoming movies with optional search/filter. |
| `GET` | `/api/movies/{id}` | Public | Get detailed movie metadata, synopsis, duration, rating. |
| `GET` | `/api/shows` | Public | Query available shows filtered by movie, date, city, or theatre. |
| `GET` | `/api/shows/{id}/seat-map` | Public / Customer | Retrieve real-time seat layout and status (`AVAILABLE`, `HELD`, `BOOKED`) for a show. |

---

## 3. Preference Management & Recommendation (`/api/preferences`, `/api/recommendations`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/preferences` | Customer | Create or update customer booking preference profile. |
| `GET` | `/api/preferences` | Customer | Retrieve customer's saved booking preferences. |
| `POST` | `/api/recommendations/evaluate` | Customer | Submit contextual search constraints and receive ranked show recommendations with explanations. |
| `POST` | `/api/recommendations/{id}/feedback` | Customer | Submit feedback rating/comments for a recommendation result. |

---

## 4. Seat Reservation, Booking & Payment (`/api/bookings`, `/api/payments`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/bookings/hold` | Customer | Reserve selected show seats with temporary hold lock (`HELD`). |
| `POST` | `/api/bookings/{id}/confirm` | Customer | Confirm held booking and initiate payment. |
| `POST` | `/api/payments/process` | Customer | Process payment simulation (Sandbox/Mock provider integration). |
| `GET` | `/api/bookings/my-history` | Customer | Retrieve user's past and active booking history. |
| `POST` | `/api/bookings/{id}/cancel` | Customer | Cancel booking and release held/reserved show seats back to `AVAILABLE`. |

---

## 5. Service Provider Administration (`/api/provider`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/provider/theatres` | Service Provider | List all theatres operated by authenticated provider. |
| `POST` | `/api/provider/theatres` | Service Provider | Add a new theatre venue. |
| `POST` | `/api/provider/theatres/{id}/screens` | Service Provider | Add screen auditorium to a theatre. |
| `POST` | `/api/provider/screens/{id}/seats` | Service Provider | Configure screen physical seat matrix layout. |
| `POST` | `/api/provider/shows` | Service Provider | Schedule a new movie showtime session and generate show-seat inventory. |
| `GET` | `/api/provider/analytics/occupancy` | Service Provider | Retrieve venue/screen show occupancy metrics. |
| `GET` | `/api/provider/analytics/revenue` | Service Provider | Retrieve booking revenue and ticket sales analytics. |

---

## 6. Deferred AI Service Internal Contract (`/api/v1/recommend`)

*Internal system-to-system REST API exposed by Python FastAPI service (Port 8000), called strictly by Spring Boot.*

* `POST` `/api/v1/recommend`: Accepts candidate show structures and customer preferences; returns scored and ranked items with score factors.
