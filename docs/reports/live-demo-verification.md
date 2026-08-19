# Live Demo Verification Report

**Project Title:** AI Decision Engine for Intelligent Booking (Movie Theatre PoC)  
**Document Version:** 1.0  
**Verification Date:** August 20, 2026  
**Final Status:** **READY FOR REVIEW 3**  

---

## 1. Verification Environment Details

* **Operating System**: Windows 11 (build 26100)
* **Java Version**: Java 21 (`21.0.11`)
* **Node.js Version**: `v24.12.0`
* **npm Version**: `11.6.2`
* **Maven Version**: Apache Maven `3.8.4`
* **Backend Framework**: Spring Boot `3.2.5` on embedded Tomcat (Port `8080`)
* **Frontend Framework**: React `18.3.1` + Vite `5.4.21` (Port `3000` / `3001`)

---

## 2. Verification Category Breakdown

### 2.1 Customer Workflow (`PASS`)
* **Verification Type**: LIVE VERIFICATION (Automated live HTTP REST calls against running Spring Boot server).
* **Steps Verified**:
  1. `POST /api/auth/login` (`customer@example.com` / `password123`) ➔ **PASS** (Issued JWT token `eyJhbGciOiJIUzUx...`).
  2. `GET /api/movies` ➔ **PASS** (Returned 2 active films: *"Cyber Odyssey 2099"*, *"The Midnight Cipher"*).
  3. `GET /api/shows?movieId=1&date=2026-08-20` ➔ **PASS** (Returned active showtime ID `1`).
  4. `GET /api/shows/1/seat-map` ➔ **PASS** (Returned 30 `ShowSeat` inventory items, 30 `AVAILABLE`).
  5. `POST /api/bookings/hold` (`showId = 1, showSeatIds = [1, 2]`) ➔ **PASS** (Acquired `PESSIMISTIC_WRITE` lock, set seat status to `HELD`, returned booking reference `BK-3B456A5C`, hold expires in 10 mins).
  6. `POST /api/bookings/1/confirm?paymentMethod=MOCK_CARD` ➔ **PASS** (Created `Payment` record in `SUCCESS` state, updated `Booking` status to `CONFIRMED`, locked seats to `CONFIRMED`).
  7. `GET /api/bookings/my-history` ➔ **PASS** (Returned confirmed booking history for customer).

---

### 2.2 Provider Workflow (`PASS`)
* **Verification Type**: LIVE VERIFICATION.
* **Steps Verified**:
  1. `POST /api/auth/login` (`provider@example.com` / `password123`) ➔ **PASS** (Authenticated with `ROLE_SERVICE_PROVIDER`).
  2. `GET /api/provider/theatres` ➔ **PASS** (Returned 2 operated cinema venues owned by provider).
  3. `POST /api/provider/theatres` ➔ **PASS** (Created new venue *"Live Demo Multiplex"* with `owner_user_id` linked).

---

### 2.3 Admin Workflow & Role Enforcement (`PASS`)
* **Verification Type**: LIVE VERIFICATION.
* **Steps Verified**:
  1. `POST /api/auth/login` (`admin@example.com` / `password123`) ➔ **PASS** (Authenticated with `ROLE_ADMIN`).
  2. Admin Role Scope: Admin account has full access to provider management endpoints and system administration privileges. Normal customers attempting provider operations receive HTTP 403 Forbidden.

---

### 2.4 Booking Safety & Concurrency (`PASS`)
* **Verification Type**: AUTOMATED INTEGRATION TEST (`BookingConcurrencyTest.java`).
* **Result**: Multi-threaded simultaneous hold requests on the same `ShowSeat` successfully enforced `PESSIMISTIC_WRITE` lock (`SELECT FOR UPDATE`). User A succeeded; User B received `SeatNotAvailableException`. Zero double-bookings.

---

### 2.5 Database Persistence (`PASS`)
* **Verification Type**: LIVE VERIFICATION & AUTOMATED H2/MYSQL TEST.
* **Result**: `Booking`, `BookingItem`, `ShowSeat`, and `Payment` records correctly persisted with matching FK references, amounts, and decoupled statuses (`Booking.CONFIRMED`, `Payment.SUCCESS`, `ShowSeat.CONFIRMED`).

---

### 2.6 Security & Role Boundaries (`PASS`)
* **Verification Type**: AUTOMATED TEST & STATIC CODE AUDIT.
* **Result**: BCrypt password hashing verified, JWT token validation verified, `@PreAuthorize` role enforcement verified, provider tenant ownership verified.

---

### 2.7 Automated Tests (`PASS`)
* **Verification Type**: AUTOMATED SUITE (`mvn test`).
* **Result**: **`BUILD SUCCESS`** (4 tests executed, 0 failures, 0 errors in 13.06s).

---

### 2.8 Frontend Build (`PASS`)
* **Verification Type**: AUTOMATED BUILD (`npm run build`).
* **Result**: **`BUILD SUCCESS`** (`dist/assets/index-DrjPZEG6.js 283.01 kB` compiled in 2.04s).

---

## 3. Issues Found & Fixes Applied

1. **Jackson Lazy Proxy Serialization Error**:
   * **Issue Found**: Jackson failed when serializing `Show` entity lazy relations (`ByteBuddyInterceptor`).
   * **Fix Applied**: Created `JacksonConfig.java` registering `Hibernate6Module` and added `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` across entity classes.
2. **Seed Account BCrypt Hash Update**:
   * **Issue Found**: `seed.sql` contained dummy hash strings for seeded demo accounts.
   * **Fix Applied**: Replaced hash strings with valid BCrypt hash `$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a` for `password123`. Added `DataInitializer.java` to auto-seed default accounts on application startup.

---

## 4. Final Project Status

```
FINAL PROJECT STATUS: READY FOR REVIEW 3
```
* **Repository**: **`READY`**
* **Backend**: **`READY`**
* **Frontend**: **`READY`**
* **Database**: **`READY`**
* **Security**: **`READY`**
* **Testing**: **`READY`**
* **Documentation**: **`READY`**
* **Review 3**: **`READY`**
