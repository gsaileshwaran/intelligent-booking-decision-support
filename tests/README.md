# PVK Cinemas — Authoritative P14 Test Infrastructure

This directory contains the testing infrastructure, shared fixtures, configuration catalogs, and harnesses for **Phase P14 (Test and Hardening)**.

## 1. Directory Layout

```
tests/
├── README.md                 # Test infrastructure overview & execution guide
├── run_infrastructure_check.py # Unified infrastructure validation script
├── config/
│   ├── test_environment.py   # Service host/port configuration (ports 8080, 8001, 3000, 3306)
│   ├── test_accounts.py      # 5 canonical personas with secure credential resolution
│   └── endpoints_catalog.py  # Frozen 53 discrete HTTP operations catalog
├── fixtures/
│   └── test_fixtures.py      # Isolated entity generators & non-destructive registry
├── api/
│   ├── conftest.py           # Pytest fixtures injecting PvkApiClient
│   ├── api_client.py         # HTTP harness with token injection & envelope assertions
│   └── test_harness_smoke.py # Verification of the API harness and catalog invariants
├── e2e/                      # Playwright E2E configuration & smoke infrastructure
└── evidence/                 # Git-ignored directory for execution artifacts and traces
```

## 2. Test Personas (5 Canonical Identities)

1. **Anonymous:** Unauthenticated public customer (`/movies`, `/cities`, `/shows`, `/search`).
2. **Customer:** Registered patron for profile verification and authenticated search history.
3. **Theatre Manager A:** Assigned strictly to Theatre 1 (`EMPLOYEE_THEATRE`).
4. **Theatre Manager B:** Assigned strictly to Theatre 2 for horizontal scope isolation testing.
5. **Super Admin:** Global administrator for catalogue management, role assignment, and audit logs.

*Note:* Passwords are dynamically resolved from environment variables (`PVK_TEST_CUSTOMER_PASSWORD`, etc.) with safe local fallback.

## 3. How to Execute Tests

### 3.1 Run Full Infrastructure Health Check
```powershell
python tests/run_infrastructure_check.py
```

### 3.2 Run API Test Harness Smoke Tests
```powershell
python -m pytest tests/api/test_harness_smoke.py
```

### 3.3 Run Playwright Browser Infrastructure Smoke Test
```powershell
cd frontend
npm run test:e2e:infra
```

### 3.4 Run Backend Unit & Integration Tests
```powershell
cd backend
mvn test
```

### 3.5 Run Search Service Unit Tests
```powershell
cd search-service
python -m pytest
```

## 4. Architectural Rules
- **No Scope Creep:** Zero tests for booking, payments, reservations, or checkout.
- **Display-Only Seats:** `GET /api/v1/shows/{id}/seats` is strictly read-only.
- **Port 8001:** Search service runs on port 8001 (not 8000).
- **Exact 53 Operations:** All API regression testing targets the frozen 53-operation catalog.
