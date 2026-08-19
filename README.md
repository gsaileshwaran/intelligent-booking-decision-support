# AI Decision Engine for Intelligent Booking

## 1. Project Overview
**CineBooking** is a full-stack, transactional movie-theatre ticket reservation platform. Built on Java 21, Spring Boot 3, React 18, and MySQL 8.0, the platform provides end-to-end seat reservation capabilities, pessimistic lock concurrency control for high-demand showtimes, temporary seat hold expiry mechanisms, sandbox payment processing, and comprehensive service provider venue administration.

---

## 2. Problem Statement
Traditional cinema booking portals suffer from race conditions (double-booking when multiple users select identical seats simultaneously), unreleased orphaned seat locks, and poor decoupling between booking status and payment processing state. Furthermore, expanding systems to incorporate future AI recommendations requires a clean, robust transactional foundation that prevents non-deterministic ML algorithms from corrupting ground-truth database state.

---

## 3. Proposed Solution
This system addresses these challenges by:
* Implementing pessimistic write locking (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) on individual show seat inventory records (`ShowSeat`).
* Maintaining decoupled status lifecycles for seat inventory, bookings, and payments.
* Automatically scheduling hold expiration cleanup to return abandoned holds back to available inventory after 10 minutes.
* Providing role-aware user interfaces for both retail customers and theatre service providers.

---

## 4. Current Scope
* **Phase 1**: Database Schema DDL (15 tables) & Seed SQL, Spring Boot backend REST API foundation.
* **Phase 2**: Complete React 18 single-page application with Vite, React Router v6, Axios, Auth & Booking context, interactive seat maps, and operator management dashboards.
* **Phase 3**: Full system integration, JWT authentication flows, and automated JUnit 5 concurrency testing verifying double-booking prevention (`BUILD SUCCESS`).
* **Phase 4**: Complete architecture documentation, 13 Mermaid diagrams, security audit, local setup guide, and Review 3 readiness report.

---

## 5. Deferred AI Scope
* The **FastAPI AI Decision Engine** (machine learning candidate ranker / MCDM recommendation engine) is explicitly **DEFERRED**.
* The core transactional booking engine operates completely independent of AI dependencies. Local Java fallback evaluation (`RecommendationController`) is maintained as an isolated boundary.

---

## 6. Features
* **Authentication**: BCrypt password hashing, JWT token authentication, and role authorization (`ROLE_CUSTOMER`, `ROLE_SERVICE_PROVIDER`, `ROLE_ADMIN`).
* **Customer Portal**: Movie catalogue discovery, genre filtering, showtime selector, interactive seat matrix with live availability, 10-minute hold reservation countdown, sandbox payment confirmation, and booking history/cancellation.
* **Service Provider Portal**: Operator dashboard, theatre venue creation, screen layout config, movie catalogue builder, show scheduling with auto-generated seat inventory, and occupancy analytics.

---

## 7. Architecture
```
React 18 SPA (Port 3000) ──> Spring Boot REST API (Port 8080) ──> MySQL Database (Port 3306)
```

---

## 8. Technology Stack
* **Frontend**: React 18, Vite 5, React Router v6, Axios, Lucide Icons, Vanilla CSS Glassmorphism.
* **Backend**: Java 21, Spring Boot 3.2.5, Spring Security 6, JJWT 0.12.5, Spring Data JPA, HikariCP.
* **Database**: MySQL 8.0 (Ground Truth) / H2 Database (Test Suite execution mode).
* **Testing**: JUnit 5, Spring Boot Test, Surefire.

---

## 9. Repository Structure
```
ai-intelligent-booking/
├── frontend/                 # React 18 Single Page Application
├── backend/                  # Spring Boot REST API (Java 21)
├── database/                 # MySQL DDL Schema & Seed Scripts
│   ├── schema/schema.sql
│   └── seed/seed.sql
├── docs/                     # Comprehensive Project Documentation
│   ├── architecture/        # System, Frontend, Backend, DB, Security & AI specs
│   ├── diagrams/            # 13 Editable Mermaid (.mmd) diagrams
│   ├── api/                 # API Endpoint Specifications
│   └── reports/             # Integration, Audit, Test, Security & Readiness reports
├── tests/                    # System & Integration test resources
├── ai-service/               # Deferred FastAPI boundary placeholder
├── .env.example
├── .gitignore
└── README.md
```

---

## 10. Database
15 relational tables: `role`, `user`, `theatre`, `screen`, `seat`, `movie`, `shows`, `show_seat`, `booking`, `booking_item`, `payment`, `user_preference`, `recommendation`, `recommendation_item`, `recommendation_feedback`.

---

## 11. Backend Setup
1. Prerequisites: Java 21 JDK, Apache Maven 3.8+.
2. Configure database connection in `backend/src/main/resources/application.properties`.
3. Build and test: `cd backend && mvn test`.

---

## 12. Frontend Setup
1. Prerequisites: Node.js 18+ and npm 9+.
2. Install packages: `cd frontend && npm install`.
3. Build for production: `npm run build`.

---

## 13. Environment Variables
* `VITE_API_BASE_URL`: Base URL for REST API (Default: `http://localhost:8080/api`).
* `SPRING_DATASOURCE_URL`: MySQL connection URL.
* `APP_JWT_SECRET`: Secret key for signing JWT tokens.

---

## 14. Seed Data
`database/seed/seed.sql` populates default roles, users, 2 theatres, 3 screens, physical seats, 3 movies, and 3 active showtimes.

---

## 15. Demo Credentials
* **Customer**: `customer@example.com` / `password123`
* **Provider**: `provider@example.com` / `password123`
* **Admin**: `admin@example.com` / `password123`

---

## 16. API Documentation
Detailed API reference available in [`docs/api/api-overview.md`](docs/api/api-overview.md).

---

## 17. Booking Lifecycle
`PENDING` ➔ `HELD` (10-minute hold lock) ➔ `CONFIRMED` (Payment success) or `EXPIRED` (Hold timer runs out) or `CANCELLED` (Refund).

---

## 18. Seat Inventory
`ShowSeat` dynamic states: `AVAILABLE`, `HELD`, `CONFIRMED`. Physical seat layout is static per screen; `ShowSeat` availability is dynamic per show session.

---

## 19. Security
Backend Spring Security filters enforce authentication and role access. Provider resource ownership (`theatre.owner_user_id`) is strictly enforced in the service layer.

---

## 20. Testing
Run the backend test suite:
```bash
cd backend
mvn test
```
Verifies JWT authentication, role guards, and double-booking concurrency locking (`BUILD SUCCESS`).

---

## 21. Running the Application
1. Import `database/schema/schema.sql` and `database/seed/seed.sql` into MySQL.
2. Run backend: `cd backend && mvn spring-boot:run`
3. Run frontend: `cd frontend && npm run dev`
4. Access app at `http://localhost:3000`.

---

## 22. Troubleshooting
* **Database Connection Error**: Verify MySQL server is running on port 3306 and credentials match `application.properties`.
* **CORS Error**: Spring Security is pre-configured with `@CrossOrigin(origins = "*")` on all controllers.

---

## 23. Current Status
Phases 1, 2, 3, and 4 are **100% COMPLETE & VERIFIED**.

---

## 24. Future AI Integration
Refer to [`docs/architecture/future-ai-integration.md`](docs/architecture/future-ai-integration.md) for the deferred FastAPI integration specification.

---

## 25. Known Limitations
* Payment processing uses sandbox simulation. Real payment gateway webhooks (e.g., Stripe/Razorpay) can be plugged into `PaymentService`.
