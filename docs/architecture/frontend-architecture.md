# Frontend Architecture Specification (React.js)

**Document Version:** 2.0  
**Framework:** React 18 + Vite 5 + React Router v6  
**Port:** `3000` (Dev Server)  
**API Target:** `http://localhost:8080/api` (Spring Boot REST API)  

---

## 1. Application Layering & Architecture

The React frontend is structured into clean domain layers:

```
[ Browser UI / Views ]
         │
         ▼
┌─────────────────────────┐
│     Context & State     │  (AuthContext, BookingContext, JWT Persistence)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│     Service Modules     │  (authService, movieService, showService, bookingService, providerService)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│     Axios API Client    │  (Centralized interceptor attaching JWT Bearer headers)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│   Spring Boot Backend   │  (REST API - Port 8080)
└─────────────────────────┘
```

---

## 2. Component Taxonomy & Page Structure

```
frontend/src/
├── components/
│   └── layout/
│       ├── Navbar.jsx              # Role-aware navigation header
│       └── Footer.jsx              # Application footer
├── context/
│   ├── AuthContext.jsx             # User identity, roles (ROLE_CUSTOMER, ROLE_SERVICE_PROVIDER), JWT session
│   └── BookingContext.jsx          # Live seat selection & active hold reservation state
├── pages/
│   ├── customer/
│   │   ├── LoginPage.jsx           # Sign in view
│   │   ├── RegisterPage.jsx        # Account registration
│   │   ├── HomePage.jsx            # Movie catalogue discovery & search filters
│   │   ├── MovieDetailsPage.jsx    # Movie details & showtimes selector
│   │   ├── SeatSelectionPage.jsx  # Interactive auditorium seat map grid (AVAILABLE, HELD, BOOKED)
│   │   ├── BookingConfirmationPage.jsx # Seat hold summary, hold timer & sandbox payment modal
│   │   └── BookingHistoryPage.jsx  # Reservation history & cancellation
│   └── provider/
│       ├── ProviderDashboardPage.jsx # Operator KPI analytics dashboard
│       ├── VenueManagerPage.jsx    # Theatre venue registration modal & list
│       ├── MovieManagerPage.jsx    # Catalogue film addition modal & list
│       └── ShowSchedulerPage.jsx   # Showtime scheduling & seat inventory generator
├── routes/
│   ├── ProtectedRoute.jsx          # Enforces authentication
│   ├── RoleProtectedRoute.jsx      # Enforces role permissions (ROLE_SERVICE_PROVIDER, ROLE_ADMIN)
│   └── AppRoutes.jsx               # Router mapping
├── services/                       # REST API Integration modules
│   ├── api.js                      # Axios client with JWT interceptor
│   ├── authService.js
│   ├── movieService.js
│   ├── showService.js
│   ├── bookingService.js
│   └── providerService.js
└── index.css                       # Modern dark glassmorphic design tokens & seat grid styles
```

---

## 3. Customer & Provider Workflow Sequences

### Customer Booking Sequence
`LoginPage / RegisterPage` ➔ `HomePage` (Movie Search) ➔ `MovieDetailsPage` (Showtime Selection) ➔ `SeatSelectionPage` (Interactive `ShowSeat` Grid) ➔ `BookingConfirmationPage` (Hold Timer & Payment Modal) ➔ `BookingHistoryPage`

### Service Provider Sequence
`ProviderDashboardPage` ➔ `VenueManagerPage` (Create Theatre Venue) ➔ `MovieManagerPage` (Add Movie) ➔ `ShowSchedulerPage` (Schedule Showtime & Auto-Generate `ShowSeat` Inventory)

---

## 4. Security & Role Isolation

* All protected HTTP requests pass through `api.js`, which attaches `Authorization: Bearer <token>`.
* Frontend route authorization (`RoleProtectedRoute`) guards provider dashboard views.
* Backend Spring Security remains the ultimate transactional authority.
