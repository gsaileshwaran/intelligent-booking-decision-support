# PVK Cinemas — P14-E Performance, Load & Concurrency Benchmark Suite

**Authority:** `docs/FINAL_REGRESSION_AND_ACCEPTANCE_REPORT.md`, SRS §10 (`NFR-001`), System Architecture §14/§18, Test Strategy §18.

---

## 1. Overview

This suite conducts high-precision, asynchronous HTTP concurrency and load testing against the integrated PVK Cinemas system:
- **Backend:** Spring Boot 3.3.4 (`http://localhost:8080/api/v1`)
- **Search Service:** FastAPI Hybrid Retrieval (`http://localhost:8001`)
- **Database:** MySQL 8.0 (`pvk_cinemas_db`)

---

## 2. Benchmark Scenarios

| Script | Purpose | Concurrency Range | Target Endpoints |
|---|---|---|---|
| `load_catalog.py` | Measures Public Catalogue & Availability retrieval | 1c, 10c, 25c, 50c, 100c | `/movies`, `/movies/{id}`, `/cities`, `/theatres`, `/shows/{id}/seats` |
| `load_search.py` | Measures AI Hybrid Search via Spring Boot proxy | 1c, 10c, 25c, 50c, 100c | `GET /search`, `POST /search` |
| `load_mixed_discovery.py` | Realistic customer traffic (87.5% Catalog, 12.5% Search) | 100c | Mixed public operations |
| `load_scheduling.py` | Pessimistic screen locking & BR-004 race condition check | Concurrent transactions | `POST /manager/theatres/{id}/shows` |
| `load_authenticated.py` | Authenticated operations under concurrency | 25c | `/auth/me`, `/manager/...`, `/admin/audit-logs` |
| `run_all_performance.py` | Master runner orchestrating all benchmarks | Full matrix | All endpoints |

---

## 3. Execution Commands

To run the complete performance verification suite:
```powershell
python tests/performance/run_all_performance.py
```

To run individual scenario benchmarks:
```powershell
python tests/performance/load_catalog.py
python tests/performance/load_search.py
python tests/performance/load_mixed_discovery.py
python tests/performance/load_scheduling.py
python tests/performance/load_authenticated.py
```

---

## 4. Key Metrics Captured

- **Request Count & Concurrency ($C$)**
- **Throughput (Requests per Second / RPS)**
- **Latency Percentiles:** Minimum, Average, P50 (Median), P90, P95, P99, Maximum
- **Status Code Distribution**
- **Timeouts & Failure Count**
- **SHOW_SEAT Atomicity & Orphan Seat Verification**
