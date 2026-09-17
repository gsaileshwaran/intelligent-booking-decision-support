# PVK Cinemas — Complete System Startup & Access Guide

Welcome to **PVK Cinemas**, an intelligent cinema decision support, multiplex scheduling, and movie discovery platform.

This guide provides complete instructions on system architecture, pre-requisites, step-by-step startup procedures, UI access details, and troubleshooting for future reference.

---

## 1. System Architecture Overview

The system consists of **4 core subsystems**:

| Subsystem | Technology Stack | Default URL / Port | Description |
| :--- | :--- | :--- | :--- |
| **Database** | MySQL 8.0 + Flyway | `127.0.0.1:3306` (`pvk_cinemas_db`) | Relational database (28 entities in 3NF), Flyway automated migrations |
| **Search Service** | Python 3.13 + FastAPI + PyTorch / Sentence-Transformers | `http://127.0.0.1:8001` | Lexical (BM25) + Semantic (Vector similarity) Hybrid Search Engine |
| **Backend API** | Java 21 + Spring Boot 3.3.4 + Spring Security | `http://127.0.0.1:8080/api/v1` | Core REST API, JWT auth, RBAC, show scheduling & audit logs |
| **Frontend UI** | React 19 + TypeScript + Vite + Lucide | `http://localhost:3000` | Modern, responsive web application for patrons, managers & admins |

---

## 2. Prerequisites & Environment Setup

Ensure the following tools are installed on your machine:

1. **Java JDK:** Java 21 LTS (`java -version`)
2. **Node.js:** Node.js v18 or higher (`node -v`)
3. **Python:** Python 3.10 or higher (`python --version`)
4. **Apache Maven:** Maven 3.9+ (`mvn -version`)
5. **MySQL Server 8.0:** Running as a system service on port `3306` (`MySQL80` service)

---

## 3. Quick Start (Services Currently Active)

> [!NOTE]
> **All services are ALREADY RUNNING** in your current session:
> - **Frontend UI:** [`http://localhost:3000`](http://localhost:3000)
> - **Backend REST API:** [`http://127.0.0.1:8080/api/v1/movies`](http://127.0.0.1:8080/api/v1/movies)
> - **Search Microservice:** [`http://127.0.0.1:8001/docs`](http://127.0.0.1:8001/docs)

---

## 4. Step-by-Step Instructions to Start in the Future

If you restart your computer or close all background tasks, follow these steps to launch the project:

### Step 1: Verify MySQL Database is Running

Ensure MySQL Server 8.0 is running on port 3306.
In PowerShell (as Administrator if needed):
```powershell
Get-Service -Name *mysql*
```
If not running, start it using:
```powershell
Start-Service MySQL80
```

> **Database Credentials** (Default in `application.yml` and `.env`):
> - **Host:** `127.0.0.1:3306`
> - **Database Name:** `pvk_cinemas_db`
> - **User:** `root`
> - **Password:** `root`

---

### Step 2: Start Python Hybrid Search Service

Open Terminal / PowerShell #1:
```powershell
cd c:\Users\admin\Desktop\intelligent-booking-system\search-service
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --reload
```
- **Swagger Documentation:** [`http://127.0.0.1:8001/docs`](http://127.0.0.1:8001/docs)
- **Health Check:** [`http://127.0.0.1:8001/health`](http://127.0.0.1:8001/health)

---

### Step 3: Start Spring Boot Backend API

Open Terminal / PowerShell #2:
```powershell
cd c:\Users\admin\Desktop\intelligent-booking-system\backend
mvn spring-boot:run
```
- Flyway will automatically execute database migrations (`V1__init_schema.sql` and `V2__seed_reference_data.sql`).
- **Base Endpoint:** [`http://127.0.0.1:8080/api/v1`](http://127.0.0.1:8080/api/v1)

---

### Step 4: Start Frontend Vite Web UI

Open Terminal / PowerShell #3:
```powershell
cd c:\Users\admin\Desktop\intelligent-booking-system\frontend
npm run dev
```
- **Access URL:** [`http://localhost:3000`](http://localhost:3000) (or `http://localhost:5173`)

---

## 5. Automated One-Click Startup Script

For convenience, a PowerShell startup script `start-services.ps1` is located in the root folder. You can double-click or run:

```powershell
.\start-services.ps1
```

This will automatically start all 3 services in separate background windows.

---

## 6. How to Access & Navigate the Web UI

Open your web browser (Chrome, Edge, Firefox) and navigate to:

👉 **`http://localhost:3000`**

### Available UI Features & Roles

1. **Movie Catalogue & Discovery (Public / Customer)**
   - Browse now-airing and upcoming films.
   - Filter by City (Chennai, Bengaluru, Hyderabad, Coimbatore), Language (Tamil, English, Hindi, etc.), and Genre.
   - Click on any movie card to view detailed synopsis, runtime, certification (U, UA, A), and scheduled shows.

2. **AI-Powered Hybrid Search Bar**
   - Type queries like *"action movies in Tamil with Dolby Atmos"* or *"Inception"*.
   - Uses BM25 lexical matching combined with MiniLM semantic embeddings.

3. **Showtimes & Seat Layout Viewer**
   - Select a theatre multiplex and view scheduled shows across screens (IMAX, 4DX, 2D).
   - Click on a showtime to interactively inspect seat grid availability (Standard, Premium, Recliner, Accessible).

4. **User Authentication & Profiles**
   - Click **Login** / **Register** in the header.
   - Register a new Customer account or log in with existing user credentials.
   - Access user profile details and active JWT session management.

5. **Manager & Admin Operations**
   - **Theatre Manager Access:** Multiplex show scheduling, screen capability overrides, and auditorium status.
   - **Super Admin Access:** Immutable platform-wide audit log inspection (`AUDIT_LOG`).

---

## 7. Verification & Automated Testing Commands

To run the complete system verification test suite:

### Backend Integration Tests (61 Tests)
```powershell
cd backend
mvn clean test
```

### Search Service Unit & Pipeline Tests (75 Tests)
```powershell
cd search-service
python -m pytest
```

### Frontend Production Build Test
```powershell
cd frontend
npm run build
```

---

## 8. Summary of Ports & API Endpoints

| Resource | URL |
| :--- | :--- |
| **Web UI Interface** | `http://localhost:3000` |
| **Backend API Health Check** | `http://127.0.0.1:8080/api/v1/movies` |
| **Search Microservice Docs** | `http://127.0.0.1:8001/docs` |
| **MySQL Database Port** | `127.0.0.1:3306` |

---
*Created on 2026-09-15 — PVK Cinemas Platform*
