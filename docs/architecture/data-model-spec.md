# Data Model & Relational Schema Specification

**Document Version:** 1.0  
**Classification:** Authoritative Technical Data Specification  
**Domain:** AI Decision Engine for Intelligent Booking (Movie Theatre PoC)  

---

## 1. Domain Modeling Principles

The data architecture separates transactional entity ground truth, inventory state management, preference parameters, and audit historical records.

### Key Modeling Rationale
1. **Service Provider vs. Theatre**: A `service_providers` record represents a business organization or cinema chain operator. A `theatres` record represents an individual physical venue or branch operated by a Service Provider.
2. **Physical Seat vs. Show Seat Inventory**: A `seats` record defines the permanent physical layout of a screen (row, column, category like `BALCONY`, `PREMIUM`, `REGULAR`). A `show_seats` record represents the dynamic availability, price, and hold state of that specific seat for a single specific `shows` session.
3. **Transactional Isolation**: Bookings lock `show_seats` records through transactional status updates (`HELD` -> `CONFIRMED`) to prevent double-booking.

---

## 2. Entity Relationship Overview

```
+------------------+         1:N         +------------------+         1:N         +------------------+
| service_providers| -------------------> |     theatres     | -------------------> |     screens      |
+------------------+                     +------------------+                     +------------------+
                                                                                           |
                                                                                           | 1:N
                                                                                           v
+------------------+                     +------------------+                     +------------------+
|      movies      | -------------------> |      shows       | <------------------- |      seats       |
+------------------+         1:N         +------------------+         1:N         +------------------+
                                                   |                                       |
                                                   | 1:N                                   | 1:N
                                                   v                                       v
                                         +-----------------------------------------------------------+
                                         |                        show_seats                         |
                                         |         (Show-Specific Inventory State & Pricing)         |
                                         +-----------------------------------------------------------+
                                                                       ^
                                                                       | 1:N
                                                                       |
+------------------+         1:N         +------------------+          |
|      users       | -------------------> |     bookings     | ---------+
+------------------+                     +------------------+
         |                                         |
         | 1:N                                     | 1:1
         v                                         v
+------------------+                     +------------------+
| user_preferences |                     |     payments     |
+------------------+                     +------------------+
```

---

## 3. Data Dictionary & Table Definitions

### 3.1 Identity & Access Management
* **`roles`**: System roles (`ROLE_CUSTOMER`, `ROLE_SERVICE_PROVIDER`, `ROLE_ADMIN`).
* **`users`**: Customer and operator credentials, names, emails, passwords (hashed), phone numbers, role references.

### 3.2 Service Provider & Venue Management
* **`service_providers`**: Business entities operating cinema venues.
* **`theatres`**: Physical cinema locations belonging to a service provider (name, city, address, latitude, longitude, contact info).
* **`screens`**: Individual auditoriums/halls within a theatre (name, screen type: 2D, 3D, IMAX, total capacity).
* **`seats`**: Physical seats in a screen (row code, seat number, seat category: VIP, BALCONY, PREMIUM, REGULAR).

### 3.3 Catalogue & Show Scheduling
* **`movies`**: Film catalogue (title, genre, language, duration minutes, rating, release date, poster URL).
* **`shows`**: Scheduled movie sessions (screen reference, movie reference, start time, end time, base price, show status: ACTIVE, CANCELLED, COMPLETED).

### 3.4 Inventory & Transaction Management
* **`show_seats`**: Operational inventory per show (show reference, seat reference, seat price, status: `AVAILABLE`, `HELD`, `BOOKED`, `BLOCKED`, hold expiry timestamp, locking version for optimistic/pessimistic locking).
* **`bookings`**: Booking transactions (user reference, show reference, total price, status: `PENDING`, `HELD`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, `FAILED`, booking time).
* **`booking_items`**: Line items linking a booking to individual `show_seats` reserved.
* **`payments`**: Payment records (booking reference, payment method, transaction reference, amount, payment status: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`, payment time).

### 3.5 Personalization & Decision Support
* **`user_preferences`**: Stated customer preferences (user reference, preferred time window, max budget, seat category preference, max preferred distance, group size).
* **`recommendations`**: Logged recommendation sessions (user reference, show reference, decision request context, generated timestamp, model/engine version).
* **`recommendation_items`**: Ranked candidate items within a recommendation session (candidate show reference, rank position, suitability score, explanation breakdown JSON).
* **`recommendation_feedback`**: User feedback on recommendations (recommendation reference, interaction type: `VIEWED`, `SELECTED`, `BOOKED`, `DISMISSED`, rating 1-5, comment).
* **`audit_logs`**: System event logs for security, booking changes, and administrative actions.

---

## 4. Booking & Inventory State Machines

### 4.1 Show Seat Status Transitions
```
+---------------+      Customer Selects Seat      +------------+
|   AVAILABLE   | ------------------------------> |    HELD    |  (Hold Timer: e.g., 10 mins)
+---------------+                                 +------------+
        ^                                               |
        |                                +--------------+--------------+
        | Hold Expired /                 |                             |
        | Booking Cancelled              v Payment Confirmed           v Payment Failed / Timeout
        |                         +------------+                +------------+
        +------------------------ |   BOOKED   |                |  AVAILABLE |
                                  +------------+                +------------+
```

### 4.2 Booking Transaction Status Transitions
```
+-----------+    Seats Locked    +----------+    Payment Success    +-----------+
|  PENDING  | -----------------> |   HELD   | --------------------> | CONFIRMED |
+-----------+                    +----------+                       +-----------+
      |                               |                                   |
      | Error                         | Timeout / Failure                 | Customer Cancels
      v                               v                                   v
+-----------+                    +----------+                       +-----------+
|  FAILED   |                    | EXPIRED  |                       | CANCELLED |
+-----------+                    +----------+                       +-----------+
```
