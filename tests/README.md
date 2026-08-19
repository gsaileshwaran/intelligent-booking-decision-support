# Test Strategy & Quality Assurance Architecture

This directory contains test plans, integration specifications, and test data for verifying the AI Decision Engine for Intelligent Booking application.

---

## 🧪 Testing Pyramid & Layers

### 1. Backend Unit & Service Tests (`backend/src/test/`)
* **Tooling**: JUnit 5, Mockito, Spring Boot Test.
* **Scope**:
  * Unit testing `BookingEngineService` state transitions (`AVAILABLE` -> `HELD` -> `CONFIRMED`).
  * Concurrency safety test for simultaneous booking requests on the same `show_seat`.
  * Unit testing recommendation fallback logic when AI service is unavailable.
  * JWT token validation and role-based endpoint authorization tests.

### 2. Database Integration Tests
* **Tooling**: H2 In-Memory Database / Testcontainers MySQL.
* **Scope**:
  * Verifying DDL foreign key constraints and transactional rollback rules.
  * Testing complex JPA queries for service provider occupancy analytics.

### 3. Frontend Unit & Integration Tests (`frontend/src/`)
* **Tooling**: Vitest / Jest, React Testing Library.
* **Scope**:
  * Seat map state rendering (`AVAILABLE` blue, `HELD` orange, `BOOKED` gray).
  * Customer preference form payload generation.

### 4. End-to-End API Workflow Verification (`tests/`)
* **Tooling**: Postman Collections / REST-Assured scripts.
* **Scope**:
  * Full customer journey: Register -> Login -> Browse Movies -> Set Preferences -> Receive Recommendation -> Hold Seats -> Confirm Payment -> View Booking History.
