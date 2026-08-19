# Code-Level Security Audit Report

**Document Version:** 1.0  
**Audit Date:** August 20, 2026  

---

## 1. Security Audit Findings

| Category | Security Control | Verification Status | Severity / Note |
|---|---|---|---|
| **Password Hashing** | BCrypt encoder (`factor 10`) used for all user accounts. Passwords never stored in plaintext. | **PASS** | `BCryptPasswordEncoder` configured in `SecurityConfig`. |
| **Authentication** | JwtTokenProvider issues HMAC-SHA512 signed tokens. `JwtAuthenticationFilter` validates token signature & expiration on every protected request. | **PASS** | Expiration set to 24 hours (`86400000ms`). |
| **Authorization** | `@PreAuthorize("hasRole('SERVICE_PROVIDER') or hasRole('ADMIN')")` enforced on provider controllers. | **PASS** | Role enforcement handled by Spring Security. |
| **Provider Ownership** | `TheatreService` verifies `theatre.owner_user_id` matches authenticated user ID before allowing modification. | **PASS** | Prevents cross-tenant venue tampering. |
| **Concurrency Lock** | `ShowSeatRepository` uses `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) during seat hold requests. | **PASS** | Prevents race condition double-booking. |
| **SQL Injection** | Spring Data JPA parameterized queries and ORM mappings used exclusively. Zero raw concatenated SQL strings. | **PASS** | SQL injection vectors eliminated. |
| **CORS** | `@CrossOrigin(origins = "*")` configured on controllers for local dev integration. | **WARNING** | Restrict origins to specific domain in production. |
| **Secrets Management** | Secret key provided in `application.properties` and `.env.example`. Real production secrets must use environment overrides. | **REQUIRES LOCAL VERIFICATION** | Environment overrides recommended for prod. |
