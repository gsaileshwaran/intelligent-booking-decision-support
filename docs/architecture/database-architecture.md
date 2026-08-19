# Database Architecture Specification

**Document Version:** 2.0  
**Source of Truth:** Week 2 System Design Document (Section 30)  
**Database System:** MySQL 8.0+  

---

## 1. Domain Entities & Relational Schema

The database model cleanly separates:
1. **User Identity & Roles**: `role`, `user`, `user_preference`.
2. **Venue Ownership**: `theatre` (owned directly by `user.user_id` where `role` = `ROLE_SERVICE_PROVIDER`), `screen`, `seat`.
3. **Show Schedules & Inventory**: `movie`, `shows`, `show_seat`.
4. **Transactions**: `booking`, `booking_item`, `payment`.
5. **Decision Support & Feedback**: `recommendation`, `recommendation_item`, `recommendation_feedback`.

---

## 2. Table Summary & Constraints

| Table Name | Primary Key | Foreign Keys | Key Constraints |
|---|---|---|---|
| `role` | `role_id` | - | `role_name` UNIQUE |
| `user` | `user_id` | `role_id` -> `role` | `email` UNIQUE |
| `theatre` | `theatre_id` | `owner_user_id` -> `user` | Owned directly by User (Service Provider) |
| `screen` | `screen_id` | `theatre_id` -> `theatre` | - |
| `seat` | `seat_id` | `screen_id` -> `screen` | UNIQUE(`screen_id`, `row_label`, `seat_number`) |
| `movie` | `movie_id` | - | - |
| `shows` | `show_id` | `movie_id`, `screen_id` | - |
| `show_seat` | `show_seat_id` | `show_id`, `seat_id` | UNIQUE(`show_id`, `seat_id`), `version` for lock |
| `booking` | `booking_id` | `user_id` -> `user` | `booking_ref` UNIQUE |
| `booking_item` | `booking_item_id` | `booking_id`, `show_seat_id` | Line items linking booking to show seats |
| `payment` | `payment_id` | `booking_id` -> `booking` | Booking & Payment states are separate |
| `user_preference` | `preference_id` | `user_id` -> `user` | `user_id` UNIQUE |

---

## 3. Invariants & Separation of Concerns

* **Physical Seat vs. Show Seat**: Physical seat configuration (`seat`) is permanent per screen. Show availability (`show_seat`) is show-specific.
* **Booking State vs. Payment State**: Booking lifecycle (`PENDING` -> `HELD` -> `CONFIRMED` / `CANCELLED` / `EXPIRED`) is independent of payment status (`PENDING` -> `SUCCESS` -> `FAILED` / `REFUNDED`).
