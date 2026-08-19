# Deployment Architecture Specification

**Document Version:** 2.0  
**Environment:** Local / On-Premise / Staging Containerization  

---

## 1. Deployment Topology

The application is deployed across three primary tiers:

```
┌────────────────────────────────────────────────────────┐
│                   Client Browser                       │
│           (React 18 Single Page App - Port 3000)       │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP / REST APIs
                            ▼
┌────────────────────────────────────────────────────────┐
│               Spring Boot Application                  │
│       (Embedded Tomcat Web Server - Port 8080)         │
│  - Spring Security & JWT Filters                       │
│  - REST Controllers & DTO Mappings                     │
│  - JPA Hibernate / HikariCP Pool                       │
│  - Scheduled Hold Cleanup Engine                       │
└───────────────────────────┬────────────────────────────┘
                            │ JDBC Connection Pool (3306)
                            ▼
┌────────────────────────────────────────────────────────┐
│                   MySQL Database                       │
│               (Relational Store - Port 3306)           │
│  - 15 Relational Tables with FK Constraints            │
│  - Pessimistic Record Locking                          │
└────────────────────────────────────────────────────────┘
```

---

## 2. Environment Configurations & Network Ports

| Layer | Component | Port | Config Variable | Default Value |
|---|---|---|---|---|
| **Frontend** | React / Vite Dev Server | `3000` | `VITE_API_BASE_URL` | `http://localhost:8080/api` |
| **Backend** | Spring Boot / Embedded Tomcat | `8080` | `server.port` | `8080` |
| **Database** | MySQL Server | `3306` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/intelligent_booking_db` |
| **AI Boundary** | Deferred FastAPI Engine | `8000` | `app.ai-service.url` | `http://localhost:8000/api/v1` |

---

## 3. Production Readiness & Build Processes

### Frontend Build Output
* Command: `npm run build`
* Output Directory: `frontend/dist/`
* Production assets compiled into static minified JS/CSS chunks (`index-DrjPZEG6.js 283 kB`).

### Backend Build Output
* Command: `mvn clean package -DskipTests`
* Output Artifact: `backend/target/intelligent-booking-backend-1.0.0.jar`
* Executable standalone Spring Boot FAT JAR with embedded Tomcat server.
