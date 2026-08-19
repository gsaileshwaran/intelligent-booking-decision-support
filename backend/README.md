# Backend Architecture (Spring Boot 3.x / Java 21)

**Application Type:** RESTful Microservice API  
**Port:** `8080`  
**Database Connection:** MySQL 8.0 (`jdbc:mysql://localhost:3306/intelligent_booking_db`)  

---

## 📌 Overview

The Spring Boot backend serves as the **system of record** and **transactional authority**. It handles authentication, authorization, domain business logic, seat inventory concurrency control, booking state lifecycles, and database interaction.

---

## 🏗 Planned Package Structure

```
backend/src/main/java/com/booking/intelligent/
├── config/                 # Spring Security, CORS, JWT, OpenAPI Swagger Config
├── controller/             # REST Endpoints
│   ├── AuthController.java        # User registration & JWT login
│   ├── MovieController.java       # Catalogue & Show browsing
│   ├── BookingController.java     # Concurrency-safe seat hold & booking
│   ├── RecommendationController.java # Decision proxy & fallback engine
│   └── ProviderController.java    # Service Provider management & analytics
├── dto/                    # Data Transfer Objects (Requests & Responses)
├── entity/                 # JPA Relational Entities (User, Theatre, Show, Seat, Booking...)
├── repository/             # Spring Data JPA Repositories
├── security/               # Custom UserDetailsService, JWT Filter, AuthEntryPoint
├── service/                # Domain Business Logic
│   ├── AuthService.java
│   ├── BookingEngineService.java  # Concurrency lock & seat hold management
│   ├── RecommendationProxyService.java # AI Proxy + Local MCDM Fallback
│   └── AnalyticsService.java      # Occupancy & revenue analytics queries
└── exception/              # Global Exception Handler (@ControllerAdvice)
```

---

## 🔒 Transactional Authority & Concurrency Locks
1. **Seat Inventory Locks**: `BookingEngineService` uses database lock strategies (`PESSIMISTIC_WRITE` or `version` optimistic locking) on `show_seats` to guarantee zero double-booking during concurrent seat holds.
2. **Hold Timer Expiry**: Background scheduled task cleans up expired `HELD` seats (hold duration: 10 minutes) and resets seat status to `AVAILABLE`.
