# Frontend Component Architecture (React.js)

**Application Type:** Single Page Application (SPA)  
**Port:** `3000` (Dev Server)  
**API Target:** `http://localhost:8080/api` (Spring Boot REST API)  

---

## 📌 Overview

The frontend layer is built with React.js. It delivers two decoupled user portals:
1. **Customer Portal**: Movie catalogue discovery, interactive booking preference sliders, ranked recommendation listings with score factor tooltips, real-time seat matrix grid, hold timer, and payment simulation.
2. **Service Provider Portal**: Theatre venue manager, screen auditorium setup, physical seat layout designer, showtime scheduler, real-time seat inventory monitor, and operational analytics dashboard.

---

## 🏗 Planned Directory Architecture

```
frontend/
├── public/                 # Static assets, favicon, index.html
├── src/
│   ├── assets/             # Images, icons, CSS design tokens
│   ├── components/         # Shared UI components (Modals, Navbars, Buttons, Badges)
│   │   ├── common/
│   │   ├── customer/       # Customer-specific UI (SeatGrid, PreferenceBar, RecCard)
│   │   └── provider/       # Provider UI (AnalyticsChart, ShowScheduler, VenueForm)
│   ├── pages/              # Top-level page views
│   │   ├── customer/       # HomePage, MovieDetailsPage, BookingPage, HistoryPage
│   │   └── provider/       # DashboardPage, VenueManagerPage, ShowManagerPage
│   ├── services/           # Axios REST API client modules
│   │   ├── api.js          # Base Axios instance with JWT Interceptors
│   │   ├── authService.js  # Registration and Login APIs
│   │   ├── bookingService.js # Seat hold & booking confirmation APIs
│   │   └── providerService.js # Admin catalogue & analytics APIs
│   ├── context/            # React Context (AuthContext, BookingContext)
│   ├── App.jsx             # React Router routing topology
│   └── main.jsx            # Application entry point
├── package.json
└── README.md
```

---

## 🔒 Security Rule
The client application **only communicates with Spring Boot (`http://localhost:8080/api`)**. It never sends requests directly to the deferred Python AI service.
