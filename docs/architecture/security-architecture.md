# Security Architecture Specification

**Document Version:** 2.0  
**Framework:** Spring Security 6 + JJWT + BCrypt + React Auth Context  

---

## 1. Security Architecture Overview

The system implements a defense-in-depth security model where:
1. **Frontend Role Checks (`RoleProtectedRoute`)**: Provide user interface/navigation controls to prevent customers from navigating to operator screens.
2. **Backend Spring Security**: Formulates the **authoritative security boundary**. Every incoming REST API request is intercepted, validated, and authorized by Spring Security filters before hitting business logic.

```
Incoming Request
      │
      ▼
┌─────────────────────────┐
│ JwtAuthenticationFilter │  (Extracts & verifies 'Authorization: Bearer <token>')
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Spring Security Filter  │  (Populates SecurityContext with UserPrincipal & GrantedAuthorities)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  @PreAuthorize Guards   │  (Enforces ROLE_CUSTOMER, ROLE_SERVICE_PROVIDER, ROLE_ADMIN)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ Business Service Layer │  (Enforces provider resource ownership, e.g., theatre.owner_user_id)
└─────────────────────────┘
```

---

## 2. Authentication Flow

### 2.1 User Registration & Password Hashing
* User registers via `POST /api/auth/register` with `name`, `email`, `password`, and `role`.
* Passwords are never stored in plaintext. They are hashed using **BCrypt** (`BCryptPasswordEncoder` with strength factor 10) before persistence to MySQL `user.password_hash`.

### 2.2 Login & Token Generation
* User submits credentials to `POST /api/auth/login`.
* `AuthenticationManager.authenticate()` validates credentials against database records via `CustomUserDetailsService`.
* Upon success, `JwtTokenProvider` generates a signed JWT token containing:
  * `Subject`: `user_id`
  * `IssuedAt`: Current timestamp
  * `Expiration`: `now + 86400000ms` (24 hours)
  * `Signature`: HMAC-SHA512 using `app.jwt.secret` key.

### 2.3 Token Interception & Request Validation
* React frontend includes `Authorization: Bearer <jwt_token>` header in Axios API requests.
* `JwtAuthenticationFilter` intercepts the request:
  1. Parses JWT token from header.
  2. Validates signature and expiration timestamp.
  3. Extracts `user_id` and loads `UserPrincipal`.
  4. Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.

---

## 3. Role-Based Authorization & Resource Ownership

### 3.1 Role Hierarchy
* `ROLE_CUSTOMER`: Access to movie discovery, showtimes, seat map, hold booking, confirmation, payment simulation, personal booking history, and cancellation.
* `ROLE_SERVICE_PROVIDER`: Access to provider dashboard, theatre registration, screen layout config, movie catalogue builder, show scheduling, and operator analytics.
* `ROLE_ADMIN`: Superuser administrative privileges.

### 3.2 Service Provider Ownership Guard
* When a provider attempts to view or schedule shows for a theatre:
  ```java
  // TheatreService.java
  Theatre theatre = theatreRepository.findById(theatreId).orElseThrow(...);
  if (!theatre.getOwnerUser().getUserId().equals(currentUserId)) {
      throw new AccessDeniedException("Access denied: You do not own this theatre venue.");
  }
  ```
