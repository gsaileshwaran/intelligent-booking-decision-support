# PVK CINEMAS — FINAL TARGETED REMEDIATION LOG

**Date:** September 16, 2026  
**Status:** IMPLEMENTED & EMPIRICALLY VERIFIED  
**Governing Phase:** Post-Audit Targeted Remediation Pass  

---

## 1. P1 — Seat Update BOLA / IDOR Horizontal Authorization

### 1.1 Root Cause Analysis
The controller endpoint `PATCH /api/v1/manager/screens/{screenId}/seats/{seatId}` in `ManagerInfrastructureController.java` applied a `@PreAuthorize("@theatreScopeService.hasAccessToScreen(#screenId)")` security check on the path variable `#screenId`. However, it invoked `seatService.updateSeat(seatId, request, ...)`, discarding `screenId`. Inside `SeatService.java`, the seat was fetched via `seatRepository.findById(seatId)` and mutated without verifying that `seat.getScreenId()` matched `screenId`. An authenticated theatre manager could provide an authorized `screenId` belonging to their own theatre alongside an arbitrary `seatId` belonging to another screen or theatre, causing unauthorized cross-theatre seat mutations.

### 1.2 Remediation
1. **Controller Layer:** Updated `ManagerInfrastructureController.java` to pass `screenId` as the primary scoping parameter:
   ```java
   seatService.updateSeat(screenId, seatId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
   ```
2. **Service Layer:** Updated `SeatService.java` to enforce defense-in-depth screen ownership:
   ```java
   if (screenId != null && !seat.getScreenId().equals(screenId.longValue())) {
       throw new AccessDeniedException("Seat does not belong to specified screen");
   }
   ```
   Retained the original `updateSeat(Long seatId, ...)` method as an overload for backward compatibility with existing internal callers.
3. **Automated Verification:** Added 6 security regression test cases (`P1-SEAT-1` through `P1-SEAT-6`) in `TargetedSecurityRegressionTest.java`. Verified that cross-screen mutation attempts return HTTP 403 Forbidden and leave the database completely unaltered.

---

## 2. P2 — Single-Candidate Hybrid Search Score Zeroing

### 2.1 Root Cause Analysis
In `search-service/app/search/hybrid_ranker.py`, min-max normalization was calculated as:
```python
rng = max_val - min_val
if rng == 0.0:
    return 0.0
return (value - min_val) / rng
```
Whenever an exact query matched exactly one candidate (e.g., `Inception`, `Avengers`, `Interstellar`, `PVR`), `min_val == max_val`, so `rng == 0.0`. The ranker returned `[0.0]` for both lexical and semantic components, zeroing the final hybrid relevance score (`0.4 * 0.0 + 0.6 * 0.0 = 0.000`) despite being an exact match.

### 2.2 Remediation
In `search-service/app/search/hybrid_ranker.py`, updated `_normalize` to preserve perfect candidate confidence when the score range is zero:
```python
rng = max_val - min_val
if rng == 0.0:
    return [1.0 if v > 0.0 else 0.0 for v in values]
return [(v - min_val) / rng for v in values]
```
For single positive hits, this normalizes to `1.0`, yielding a proper positive relevance score (`1.000` for exact hybrid hits) while preserving multi-candidate normalization and zeroing true `0.0` scores. Added comprehensive unit tests in `test_hybrid_ranker.py` (81/81 passed).

---

## 3. P1 — Backend Integration Test Database Isolation

### 3.1 Root Cause Analysis
Backend Maven integration tests previously executed against the demo database `pvk_cinemas_db` without rollback or isolation fixtures. During test runs, synthetic theatres, screens, and shows without seats were injected into `pvk_cinemas_db`, polluting the demo environment and causing subsequent Playwright E2E tests (or manual testing) to fail upon loading seat-less shows.

### 3.2 Remediation
1. Created dedicated test configuration in `backend/src/test/resources/application.yml` targeting `pvk_cinemas_test_db`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/${DB_TEST_NAME:pvk_cinemas_test_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
     flyway:
       enabled: true
       baseline-on-migrate: true
       locations: classpath:db/migration
   ```
2. During test suite startup, Spring Boot automatically connects to `pvk_cinemas_test_db`, creates the database if it doesn't exist, executes Flyway migrations (V1, V2, V3) on the isolated test schema, and runs all 78 tests entirely isolated from `pvk_cinemas_db`.
3. Verified zero delta in `pvk_cinemas_db` before and after running `mvn test`. Playwright test suite passes 12/12 immediately following `mvn test` without resetting the database.

---

## 4. Test & Verification Summary

| Suite | Status | Execution Time | Notes |
|---|---|---|---|
| `mvn test` | PASS (78/78) | ~3m 25s | Executed against `pvk_cinemas_test_db`, 0 failures |
| `pytest` | PASS (81/81) | ~7.62s | Tests normalization, cutoff, BM25, semantic ranker |
| `npm run build` | PASS | 1.27s | Frontend TypeScript compilation and asset bundling |
| `npx playwright test` | PASS (12/12) | 1.0m | Run immediately post-Maven without DB reset |
| Demo Reset Determinism | PASS | 2 runs | Exact count matching across Run 1 and Run 2 |