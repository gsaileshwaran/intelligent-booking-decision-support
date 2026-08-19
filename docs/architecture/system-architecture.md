# System Architecture Specification

**Document Version:** 1.0  
**Classification:** Authoritative Technical System Architecture  
**Domain:** AI Decision Engine for Intelligent Booking (Movie Theatre PoC)  

---

## 1. System Topology & Architectural Overview

The AI Decision Engine for Intelligent Booking is structured as a multi-tier, decoupled software system. The architecture guarantees a clear separation between client presentation, transactional business logic, relational persistence, and future decision-support intelligence.

```
+-----------------------------------------------------------------------------------+
|                                  PRESENTATION LAYER                               |
|                                                                                   |
|                   React.js Single Page Application (Port 3000)                    |
|       +-----------------------------------+-----------------------------------+   |
|       | Customer Portal                   | Service Provider Portal           |   |
|       | Movie Search, Preferences,        | Theatre/Screen Management,        |   |
|       | Seat Maps, Booking, History       | Shows, Occupancy & Analytics      |   |
|       +-----------------------------------+-----------------------------------+   |
+-----------------------------------------------------------------------------------+
                                          |
                                          | REST / JSON over HTTPS
                                          v
+-----------------------------------------------------------------------------------+
|                              TRANSACTIONAL BACKEND                                |
|                                                                                   |
|                      Spring Boot REST API Service (Port 8080)                     |
|                                                                                   |
|  [ Security & Auth ]        [ Booking Engine ]          [ Inventory Management ]  |
|  Spring Security + JWT      Seat Hold Locks & States    Show Seat Concurrency     |
|                                                                                   |
|  [ Provider Admin ]         [ Analytics Service ]       [ AI Proxy & Fallback ]   |
|  Catalogue & Venues         Occupancy & Trends          MCDM Fallback Engine      |
+-----------------------------------------------------------------------------------+
               |                                                        |
               | JDBC / JPA (Hibernate)                                 | REST / JSON
               v                                                        v
+-----------------------------+                         +---------------------------+
|    PERSISTENCE LAYER        |                         |   DECISION ENGINE LAYER   |
|                             |                         |       (DEFERRED)          |
|    MySQL Database           |                         |                           |
|    (Port 3306)              |                         |   Python FastAPI Service  |
|    • Relational Schema      |                         |   (Port 8000)             |
|    • ACID Ground Truth      |                         |   • Rule-Based MCDM       |
|    • Inventory States       |                         |   • Multi-Factor Scoring  |
+-----------------------------+                         |   • Isolated Boundary     |
                                                        +---------------------------+
```

---

## 2. Core Components & Responsibilities

### 2.1 React Frontend Application (`frontend/`)
* **Role**: Client presentation and user experience layer.
* **Key Responsibilities**:
  * Renders Customer and Service Provider (Admin) workflows.
  * Captures customer booking preferences (budget, time windows, seat types, maximum distance, group size).
  * Displays interactive venue seat maps with live status color-coding (`AVAILABLE`, `HELD`, `OCCUPIED`).
  * Provides service providers with venue configuration tools and analytics visualizers.
  * Communicates **exclusively** with the Spring Boot backend API.

### 2.2 Spring Boot Backend Service (`backend/`)
* **Role**: Transactional authority, business logic, security controller, and system of record.
* **Key Responsibilities**:
  * Handles authentication (`/api/auth/register`, `/api/auth/login`) using Spring Security and JWT.
  * Manages transactional entity lifecycles for Users, Service Providers, Theatres, Screens, Seats, Movies, Shows, Show Seats, Bookings, and Payments.
  * Enforces concurrency safety on seat inventory to prevent double-booking.
  * Controls the booking lifecycle: `PENDING` -> `HELD` -> `CONFIRMED` / `EXPIRED` / `CANCELLED`.
  * Serves as the decision orchestrator: retrieves candidate shows from MySQL, sends decision context to the AI service (when active), or executes a deterministic fallback score calculation.

### 2.3 MySQL Relational Database (`database/`)
* **Role**: ACID-compliant persistence layer storing ground truth.
* **Key Responsibilities**:
  * Stores identity, catalogue, schedule, inventory, transaction, and preference data.
  * Enforces structural relational constraints (foreign keys, uniqueness, check constraints).
  * Executes atomic state updates for show seat holds.

### 2.4 AI Decision Engine Service (`ai-service/` - DEFERRED)
* **Role**: Isolated decision-support boundary.
* **Key Responsibilities**:
  * Evaluates pre-filtered booking candidate shows against customer preferences.
  * Calculates deterministic suitability scores using a multi-criteria scoring algorithm ($S_i = \sum (w_j \times x_{ij})$).
  * Generates explainable score breakdown metrics and identifies next-best alternative options.
  * **Strict Isolation Constraints**:
    * Has **no database access** (does not connect to MySQL).
    * Is **never called directly by the browser client**.
    * Performs **no transactional operations** or state updates.

---

## 3. Communication Protocols & Security Boundaries

1. **Client to Backend**: REST / JSON over HTTPS. Authenticated via `Bearer <JWT_TOKEN>` in HTTP headers.
2. **Backend to Database**: JDBC over standard MySQL Protocol (Port 3306) managed via Spring Data JPA and HikariCP connection pooling.
3. **Backend to AI Service (Future/Deferred)**: Internal REST / JSON over HTTPS. Synchronous HTTP POST requests from Spring Boot to FastAPI.
4. **Browser to AI Service Prohibition**: Direct network connections between browser clients and the AI service are strictly forbidden by architectural policy.

---

## 4. Fallback Architecture & Resilience

To satisfy non-functional availability requirements (**NFR-04**), the system features a **Zero-Downtime Fallback Architecture**:

```
                              [ Spring Boot Backend ]
                                         |
                                         | Retrieve Eligible Candidates from MySQL
                                         v
                         Is AI Service Enabled & Healthy?
                                   /          \
                             YES  /            \  NO (Or Deferred / Timeout)
                                 v              v
               [ Call Python FastAPI Service ]   [ Internal Spring Boot Rule Engine ]
                         |                                 |
                         +-----------------+---------------+
                                           |
                                           v
                          Return Ranked Candidates to React Client
```

If the Python AI service fails, times out (threshold: 3000ms), or is disabled:
1. Spring Boot catches the exception / bypasses the HTTP call.
2. Spring Boot executes an internal, deterministic Java-based candidate sorting algorithm (evaluating show time proximity and ticket price).
3. The customer receives valid booking recommendations seamlessly without experiencing service disruption.
