# Backend Architecture Specification

**Document Version:** 2.0  
**Framework Baseline:** Java 21 + Spring Boot 3.2.5  
**Domain:** Transactional Booking Engine  

---

## 1. Architectural Overview & Layering

The backend application follows a strict layered architecture pattern:

```
[ HTTP Client / Frontend ]
          │
          ▼
┌─────────────────────────┐
│     Controller Layer    │  (Handles HTTP request routing, DTO validation, security context)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│      Service Layer      │  (Implements business rules, state transitions, concurrency locks)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│    Repository Layer     │  (Spring Data JPA data access interfaces)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│   MySQL Database (RDBMS)│  (ACID relational ground truth)
└─────────────────────────┘
```

---

## 2. Package Structure

```
com.booking.intelligent
├── IntelligentBookingApplication.java
├── config/             # Spring Security, Web MVC CORS, JWT configuration
├── controller/         # REST Controllers (Auth, Movie, Show, Booking, Provider, Recommendation)
├── dto/                # Request & Response Data Transfer Objects
├── entity/             # JPA Entities (Role, User, Theatre, Screen, Seat, Movie, Show, ShowSeat, Booking...)
├── enums/              # Domain Value Enums (RoleName, BookingStatus, PaymentStatus, ShowSeatStatus...)
├── exception/          # Custom Domain Exceptions & Global Exception Handler
├── repository/         # Spring Data JPA Repositories
├── security/           # Custom UserDetailsService, UserPrincipal, JwtTokenProvider, JwtFilter
└── service/            # Domain Services (AuthService, BookingService, ShowService, RecommendationService...)
```

---

## 3. Transactional & Concurrency Guarantees

### 3.1 Pessimistic Lock for Seat Inventory
To guarantee zero double-booking under concurrent hold attempts:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT ss FROM ShowSeat ss WHERE ss.showSeatId IN :ids")
List<ShowSeat> findAllByIdForUpdate(@Param("ids") List<Long> ids);
```
`BookingService.holdSeats()` acquires a database write lock on requested `ShowSeat` records, verifies that status is `AVAILABLE` (or expired), and transitions the status to `HELD` with `heldUntil = now + 10 mins`.

### 3.2 Hold Timer Expiry Scheduled Cleaning
A background task runs periodically (`@Scheduled(fixedDelay = 60000)`):
1. Queries `ShowSeat` records where `status = 'HELD'` and `heldUntil < now`.
2. Resets status to `AVAILABLE` and clears `heldUntil`.
3. Marks unconfirmed `Booking` records as `EXPIRED`.
