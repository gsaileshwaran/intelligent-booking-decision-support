# Local Setup & Execution Guide

**Document Version:** 1.0  
**Target OS:** Windows / Linux / macOS  

---

## 1. Prerequisites

Before setting up the project, ensure the following software is installed on your local machine:
* **Java Development Kit (JDK)**: Version 21 (`java -version`)
* **Apache Maven**: Version 3.8+ (`mvn -version`)
* **Node.js**: Version 18 or 20+ (`node -version`)
* **npm**: Version 9 or 10+ (`npm -version`)
* **MySQL Server**: Version 8.0 running on port 3306

---

## 2. Database Initialization (MySQL)

Open your MySQL command-line client or administration tool (Workbench / DBeaver) and execute:

```sql
-- Step 1: Create Database
CREATE DATABASE IF NOT EXISTS intelligent_booking_db;
USE intelligent_booking_db;

-- Step 2: Execute Schema DDL
SOURCE database/schema/schema.sql;

-- Step 3: Populate Seed Data
SOURCE database/seed/seed.sql;
```

---

## 3. Backend Configuration & Startup (Spring Boot)

1. Navigate to the `backend/` directory:
   ```cmd
   cd backend
   ```

2. Open `src/main/resources/application.properties` and verify your MySQL database credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/intelligent_booking_db?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```

3. Run Maven tests to verify compilation and backend integration:
   ```cmd
   mvn test
   ```

4. Launch the Spring Boot backend application:
   ```cmd
   mvn spring-boot:run
   ```
   The backend server will start on `http://localhost:8080`.

---

## 4. Frontend Installation & Startup (React + Vite)

1. Open a new terminal window and navigate to the `frontend/` directory:
   ```cmd
   cd frontend
   ```

2. Install Node dependencies:
   ```cmd
   npm install
   ```

3. Verify environment configuration in `.env`:
   ```properties
   VITE_API_BASE_URL=http://localhost:8080/api
   ```

4. Launch the Vite development server:
   ```cmd
   npm run dev
   ```
   The React application will be available at `http://localhost:3000`.

---

## 5. Demo Logins & Access

Access `http://localhost:3000` in your web browser:

| Account Type | Email | Password | Role / Access |
|---|---|---|---|
| **Customer** | `customer@example.com` | `password123` | Ticket booking, seat map, hold timer, payment, history |
| **Service Provider** | `provider@example.com` | `password123` | Operator dashboard, venue manager, catalogue, show scheduler |
| **Admin** | `admin@example.com` | `password123` | Full administration privileges |
