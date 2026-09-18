# PVK Cinemas — AI-Powered Cinema Search & Decision Support Platform

## 1. Project Title
**PVK Cinemas: Intelligent Multiplex Scheduling, Movie Discovery, and Decision Support Platform**

---

## 2. Short Description
PVK Cinemas is a full-stack cinema management and customer discovery platform that integrates hybrid lexical/semantic movie search with an AI-driven multi-factor decision engine. It evaluates screening schedules, spatial seat layouts, aisle geometries, party cohesion, and pricing tiers to recommend optimal shows and seat groupings, backed by temporary seat reservations and booking flows.

---

## 3. Core Problem
Modern cinema patrons encounter significant friction when planning movie outings:
1. **Decision Fatigue:** Customers must manually inspect multiple multiplexes, show timings, projection formats (IMAX, 4DX, 2D), and audio setups (Dolby Atmos) to find suitable options.
2. **Fragmented Group Seating:** Booking for groups (pairs, families, parties of 4 to 15+) often results in accidental aisle splits, undesirable front-row neck strain, or disjointed seating blocks.
3. **Keyword-Only Search Inadequacies:** Standard cinema search bars fail to interpret semantic queries such as *"action sci-fi in Tamil with Dolby Atmos"* or natural-language intent.

PVK Cinemas solves these problems by providing:
- Hybrid lexical (BM25) and semantic vector search (MiniLM) for films and multiplexes.
- An automated multi-candidate decision support engine ranking show options according to user preferences (`BEST_VIEW`, `BEST_SEATS`, `BALANCED`, `BUDGET`, `TIME`).
- A geometry-aware `SeatGroupPlanner` evaluating contiguous runs, physical aisles, neck-tilt distance penalties, horizontal sightlines, and cohesive multi-row splits.
- Real-time temporary seat reservations with automatic hold expirations and simulated payments.

---

## 4. Key Capabilities
- **Hybrid Lexical & Semantic Retrieval:** Combines BM25 keyword matching with dense MiniLM sentence embeddings over synopsis, cast, crew, and technical format attributes.
- **Multi-Factor Show Ranking:** Evaluates time suitability, screen format, audio quality, venue pricing variation, and available seat cluster scores across multiplexes.
- **Spatial Seat Group Planning:** Mathematically scores auditorium seat blocks across rows and aisles, avoiding split parties across aisle boundaries and computing honest sightline metrics.
- **Seat Hold & Booking Lifecycle:** Atomically manages `AVAILABLE` → `HELD` (10-minute lease) → `BOOKED` state transitions with concurrency protection and simulated checkout.
- **Role-Based Access Control (RBAC):** Strict separation across `ROLE_SUPER_ADMIN` (platform-wide management, audit log inspection), `ROLE_THEATRE_MANAGER` (scoped strictly to assigned theatres), and `ROLE_CUSTOMER` (public discovery and personal bookings).
- **Immutable Security Auditing:** Append-only system audit log (`AUDIT_LOG`) recording all security, operational, and scheduling events.

---

## 5. Architecture Overview

```
                         +-----------------------------+
                         |      React 19 Frontend      |
                         |   (Vite + TypeScript + UI)  |
                         |     http://localhost:3000   |
                         +--------------+--------------+
                                        | (REST / JSON)
                                        v
                         +-----------------------------+
                         |    Spring Boot 3.3.4 API    |
                         |  (Java 21 / Spring Security)|
                         |   http://127.0.0.1:8080     |
                         +-------+--------------+------+
                                 |              |
         +-----------------------+              +-----------------------+
         |                                                              |
         v (TCP / SQL)                                                  v (HTTP REST)
+-----------------------------+                                +-----------------------------+
|    MySQL 8.0 Database       | <----------------------------+ |    FastAPI Search Service   |
|   (28 Entities in 3NF)      |    (Read-Only Indexing Sync)   |  (Python 3.13 / MiniLM)     |
|   127.0.0.1:3306            |                                |    http://127.0.0.1:8001    |
+-----------------------------+                                +-----------------------------+
```

---

## 6. Technology Stack

| Layer | Technologies | Primary Responsibilities |
| :--- | :--- | :--- |
| **Frontend** | React 19, TypeScript, Vite, Tailwind/Vanilla CSS, Lucide Icons | Responsive UI, interactive seat map, decision support panel, booking checkout |
| **Backend API** | Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Security, Flyway | Core REST endpoints, JWT authentication, RBAC authorization, seat hold orchestration |
| **AI Decision Engine** | Java 21 (`SeatGroupPlanner`, `ShowRecommendationEngine`, `AdaptiveGuidanceEngine`) | Multi-factor show candidate scoring, spatial seat block optimization, aisle checking |
| **Search Microservice** | Python 3.13, FastAPI, PyTorch, `sentence-transformers` (`all-MiniLM-L6-v2`), BM25 | Hybrid lexical + semantic search, text preprocessing, tokenized candidate ranking |
| **Database** | MySQL Server 8.0 (InnoDB, UTF-8 MB4) | 28 normalized entities, foreign key constraints, Flyway forward migrations (V1–V7) |

---

## 7. Repository Structure

```
intelligent-booking-system/
├── backend/                       # Spring Boot 3.3.4 Backend API (Java 21)
│   ├── src/main/java/             # Domain controllers, services, repositories, decision engine
│   ├── src/main/resources/        # application.yml, db/migration (Flyway V1 to V7)
│   ├── src/test/java/             # Comprehensive unit & integration test suites
│   └── pom.xml                    # Maven build specification
├── database/                      # Authoritative Database Documentation & Migrations
│   ├── migrations/                # Standalone SQL migrations matching Flyway baseline (V1 to V7)
│   └── README.md                  # Database schema specifications & privilege definitions
├── frontend/                      # React 19 + TypeScript + Vite Application
│   ├── src/                       # Components, views (Customer, Manager, Admin), API clients
│   ├── public/                    # Static assets & TMDB movie posters
│   ├── package.json               # Node dependencies & scripts
│   └── vite.config.ts             # Vite configuration with API reverse proxy
├── search-service/                # FastAPI Hybrid Search Microservice
│   ├── app/                       # BM25 lexical engine, vector store, pipeline, API endpoints
│   ├── tests/                     # Search microservice test suite (81 tests)
│   └── requirements.txt           # Python dependencies
├── docs/                          # Architecture, SRS, and regression documentation
├── tests/                         # End-to-end, accessibility, security, and performance test suites
├── start-dev.ps1                  # Clean, automated development startup script
├── reset-demo.ps1                 # Optional demo database reset script
├── .env.example                   # Environment configuration template
└── README.md                      # Primary project documentation
```

---

## 8. Prerequisites

Ensure the following runtimes and tools are installed:
1. **Java JDK:** Java 21 LTS (`java -version`)
2. **Node.js:** Node.js v18 or higher with `npm` (`node -v`)
3. **Python:** Python 3.10 to 3.13 (`python --version`)
4. **Apache Maven:** Maven 3.9 or higher (`mvn -version`)
5. **MySQL Server:** MySQL Community Server 8.0 listening on port `3306`

---

## 9. Environment Configuration

Copy the root configuration template to `.env`:
```powershell
Copy-Item .env.example .env
```

Default local evaluation parameters:
- `DB_HOST=127.0.0.1`, `DB_PORT=3306`, `DB_NAME=pvk_cinemas_db`, `DB_USER=root`, `DB_PASSWORD=root`
- `PORT=8080`, `JWT_SECRET=your-256-bit-secret-key-change-this-for-production-use-only-2026!`
- `SEARCH_SERVICE_URL=http://127.0.0.1:8001`, `SEARCH_FALLBACK_ENABLED=true`
- `VITE_PORT=3000`

---

## 10. Database Setup

1. Verify MySQL Server 8.0 is running:
   ```powershell
   Get-Service -Name *mysql*
   ```
2. Create the target schema if it does not already exist:
   ```sql
   CREATE DATABASE IF NOT EXISTS pvk_cinemas_db
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_unicode_ci;
   ```
3. Flyway migrations (`V1` through `V7`) are executed automatically by the Spring Boot backend on initial startup.

---

## 11. Backend Startup

In PowerShell / Terminal:
```powershell
cd backend
mvn spring-boot:run
```
- API Base URL: `http://127.0.0.1:8080/api/v1`
- Flyway executes schema creation, reference seed data, demo seed data, seat hold tables, pricing zones, and constraint updates.

---

## 12. Search Service Startup

In PowerShell / Terminal:
```powershell
cd search-service
pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload
```
- OpenAPI Documentation: `http://127.0.0.1:8001/docs`
- Health Endpoint: `http://127.0.0.1:8001/health`

---

## 13. Frontend Startup

In PowerShell / Terminal:
```powershell
cd frontend
npm install
npm run dev
```
- Web Application URL: `http://localhost:3000`

---

## 14. Automated One-Click Launch

To launch all components concurrently with automated database prerequisite verification:
```powershell
.\start-dev.ps1
```

---

## 15. Verification & Test Commands

### Backend Automated Test Suite
```powershell
cd backend
mvn clean test
```
- Executes 116 tests covering JWT authentication, RBAC authorization, show scheduling, atomicity, pessimistic locking, and seat scoring.

### Search Service Automated Test Suite
```powershell
cd search-service
python -m pytest tests
```
- Executes 81 tests verifying query tokenization, BM25 indexing, MiniLM embeddings, and hybrid rankers.

### Frontend TypeScript Verification & Build
```powershell
cd frontend
npx tsc --noEmit
npm run build
```
- Verifies type integrity and builds the client bundle (`dist/`).

---

## 16. Demo Accounts & Personas

| Role | Email | Password | Scope / Permissions |
| :--- | :--- | :--- | :--- |
| **Super Admin** | `admin@pvkcinemas.com` | `Password123!` | Full platform administration, audit logs, user management |
| **Theatre Manager** | `manager@pvkcinemas.com` | `Password123!` | Scoped to assigned multiplex (Chennai Grand Multiplex) |
| **Customer** | `customer@pvkcinemas.com` | `Password123!` | Movie catalogue discovery, seat selection, booking flow |

---

## 17. Core Customer Flow

```
1. Select City (e.g., Chennai, Bengaluru)
   ↓
2. Browse Catalog / AI Hybrid Search
   ↓
3. View Movie Details & Decision Support Panel
   ↓
4. Select Party Size & Optimization Priority (Best View / Best Seats / Budget / Time)
   ↓
5. Inspect Ranked Show Candidates with Honest Explanations
   ↓
6. Navigate to Seat Availability Grid
   ↓
7. Trigger AI Seat Recommendation ("Use These Seats")
   ↓
8. Reserve Seats (10-minute temporary lease)
   ↓
9. Complete Simulated Checkout
   ↓
10. View Booking Confirmation & QR Reference in Profile
```

---

## 18. AI Decision Pipeline & Seat Grouping

The AI decision support system operates across two cooperative levels:

1. **Show Recommendation Engine (`ShowRecommendationEngine`):**
   - Ranks all scheduled shows across multiplexes for a chosen movie, city, date, and party size.
   - Evaluates weighted candidate attributes: viewing geometry fit, screen technology capability, audio setup, time suitability, and venue price offset.
   - Generates exact candidate seat previews for each show before the user navigates to the auditorium seat map.

2. **Seat Group Planner (`SeatGroupPlanner`):**
   - **Aisle & Boundary Respect:** Partitions physical rows into contiguous banks based on `aisle_after` markers and seat number gaps.
   - **Continuous Immersion & Neck-Tilt Penalty:** Applies progressive sightline penalties to extreme front rows (Row A/B) while favoring center mid-auditorium sweet spots.
   - **Cohesive Group Splits:** When a contiguous block of `N` seats is unavailable in a single row, the planner identifies adjacent rows with aligned lateral centers (e.g. 5+5 or 4+4+4) and truthful rationale explanations (`Paired group` / `Clustered group`).

---

## 19. Booking & Seat Hold Behavior

- **State Transition:** Seats transition between `AVAILABLE`, `HELD`, and `BOOKED`.
- **Atomic Hold:** `POST /api/v1/shows/{id}/hold` acquires a pessimistic lease on the selected seats. A unique `holdToken` with a 10-minute expiration is returned.
- **Concurrent Collision Protection:** Any attempt by another session to hold or book an already `HELD` or `BOOKED` seat immediately fails with HTTP 409 Conflict.
- **Simulated Payment:** Simulated checkout (`POST /api/v1/bookings/checkout`) accepts the `holdToken`, generates a unique booking reference, records payment details, and finalizes the seat status to `BOOKED`.

---

## 20. Role Separation & Security Boundaries

- **Super Admin:** Can manage cities, theatres, screens, movies, and inspect platform-wide audit trails.
- **Theatre Manager:** Restricted via `TheatreScopeService` strictly to assigned multiplexes. Any attempt by a manager to create shows or alter seats in an unassigned theatre returns HTTP 403 Forbidden.
- **Customer:** Has read-only access to catalogue, schedules, and public seat maps. Customer attempts to access manager or admin endpoints return HTTP 403 Forbidden.

---

## 21. Troubleshooting & Support

- **Port 3306 Inactive:** Ensure MySQL Server 8.0 is running (`Start-Service MySQL80`).
- **Port 8080 In Use:** Check and terminate conflicting processes (`Get-NetTCPConnection -LocalPort 8080`).
- **Missing Python Packages:** Run `pip install -r search-service/requirements.txt`.
- **Stale Frontend Token:** If API calls return 401 Unauthorized, click Logout in the top-right navigation bar or clear browser `localStorage`.

---

## 22. Project Documentation References
Detailed design specifications are located in the repository:
- `database/README.md`: Authoritative physical schema & database privileges
- `docs/DECISION_REGISTER.md`: Architectural decision records (ADRs)
- `docs/FINAL_REGRESSION_AND_ACCEPTANCE_REPORT.md`: Comprehensive verification matrix
- `PVK_Cinemas_System_and_Software_Architecture.docx`: System architecture specification
- `PVK_Cinemas_Software_Requirements_Specification.docx`: SRS documentation
