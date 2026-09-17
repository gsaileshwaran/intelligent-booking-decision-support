# PVK Cinemas — P14 Test Evidence Repository

This directory archives test execution evidence, logs, and artifacts generated during Phase P14 (Test and Hardening).

## 1. Directory Structure

```
tests/evidence/
├── api/             # API regression test run results (JSON, JUnit XML)
├── e2e/             # Playwright browser test traces, screenshots, video
├── security/        # Security hardening and penetration audit logs
├── performance/     # Load test profiles (p50/p95/p99 latency charts, stats)
└── accessibility/   # Axe-core / Lighthouse WCAG audit reports
```

## 2. Evidence Naming Convention

All generated artifact filenames must adhere to the standard format:
`{test_tier}_{suite_name}_{environment}_{timestamp}.{ext}`

Examples:
- `api_53ops_local_20260905_210000.json`
- `e2e_j001_discovery_chromium_20260905_210000.webm`
- `perf_catalogue_load_20260905_210000.json`
- `a11y_wcag22aa_ui08_20260905_210000.json`

## 3. Required Evidence Attributes

Every formal test report must document:
- **Test Type:** (API, E2E, Security, Performance, Accessibility)
- **Run Identifier:** Unique execution run ID (e.g., `pvk_p14_run_<uuid>`)
- **Timestamp:** ISO-8601 UTC timestamp
- **Environment:** Service base URLs, database commit/schema version, browser version
- **Result:** Status (PASS, FAIL, BLOCKED, WAIVED)
- **Log / Trace URI:** Relative path to attached trace, video, or execution log

## 4. Git Policy

Raw test execution dumps, trace ZIP files, screenshots, and logs are excluded from version control via `.gitignore`. Only formal summary audit reports in `docs/` are committed.
