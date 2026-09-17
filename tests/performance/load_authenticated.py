"""
PVK Cinemas — P14-E Authenticated API Concurrency Benchmark
Authority: P14 Baseline §14.2, ARCH §14.

Evaluates representative authenticated endpoints under concurrency:
- /api/v1/auth/me (Customer identity check)
- /api/v1/me/profile (Customer profile lookup)
- /api/v1/manager/theatres/1/shows (Manager theatre schedule read)
- /api/v1/admin/audit-logs (Admin governance read)
"""

import asyncio
import json
import httpx
from tests.config.test_environment import ENV
from tests.config.test_accounts import ACCOUNTS
from tests.performance.performance_utils import run_concurrent_load, format_markdown_table, BenchmarkResult


class AuthenticatedLoadBenchmark:
    def __init__(self):
        self.base_url = ENV.backend_base_url
        self.tokens = {}

    async def authenticate_all(self):
        async with httpx.AsyncClient(timeout=10.0) as client:
            for persona in ["customer", "manager_a", "super_admin"]:
                acc = ACCOUNTS[persona]
                resp = await client.post(f"{self.base_url}/auth/login", json={
                    "email": acc.email,
                    "password": acc.password
                })
                assert resp.status_code == 200, f"Login failed for {persona}: {resp.text}"
                self.tokens[persona] = resp.json()["token"]

    async def request_customer_me(self, client: httpx.AsyncClient, req_idx: int):
        headers = {"Authorization": f"Bearer {self.tokens['customer']}"}
        resp = await client.get(f"{self.base_url}/auth/me", headers=headers)
        return resp, "/auth/me", "GET"

    async def request_manager_shows(self, client: httpx.AsyncClient, req_idx: int):
        headers = {"Authorization": f"Bearer {self.tokens['manager_a']}"}
        resp = await client.get(f"{self.base_url}/manager/theatres/1/shows", headers=headers)
        return resp, "/manager/theatres/1/shows", "GET"

    async def request_admin_audit(self, client: httpx.AsyncClient, req_idx: int):
        headers = {"Authorization": f"Bearer {self.tokens['super_admin']}"}
        resp = await client.get(f"{self.base_url}/admin/audit-logs?page=0&size=20", headers=headers)
        return resp, "/admin/audit-logs", "GET"


async def main():
    bench = AuthenticatedLoadBenchmark()
    await bench.authenticate_all()

    results: list[BenchmarkResult] = []

    print("================================================================================")
    print("PVK Cinemas — P14-E Authenticated API Concurrency Benchmark")
    print(f"Target: {ENV.backend_base_url}")
    print("================================================================================")

    # 1. Customer Auth Me (25 concurrent)
    r1 = await run_concurrent_load(
        scenario_name="Auth Me Customer (25c)",
        request_fn=bench.request_customer_me,
        total_requests=100,
        concurrency=25,
        warmup_requests=5
    )
    results.append(r1)
    print(f"  [Customer /auth/me] P50: {r1.p50_ms:.1f}ms | P95: {r1.p95_ms:.1f}ms | RPS: {r1.throughput_rps:.1f} | Failed: {r1.failed_requests}")

    # 2. Manager Theatre Schedule (25 concurrent)
    r2 = await run_concurrent_load(
        scenario_name="Manager Schedule (25c)",
        request_fn=bench.request_manager_shows,
        total_requests=100,
        concurrency=25,
        warmup_requests=5
    )
    results.append(r2)
    print(f"  [Manager /shows] P50: {r2.p50_ms:.1f}ms | P95: {r2.p95_ms:.1f}ms | RPS: {r2.throughput_rps:.1f} | Failed: {r2.failed_requests}")

    # 3. Super Admin Audit Logs (25 concurrent)
    r3 = await run_concurrent_load(
        scenario_name="Admin Audit Logs (25c)",
        request_fn=bench.request_admin_audit,
        total_requests=100,
        concurrency=25,
        warmup_requests=5
    )
    results.append(r3)
    print(f"  [Admin /audit-logs] P50: {r3.p50_ms:.1f}ms | P95: {r3.p95_ms:.1f}ms | RPS: {r3.throughput_rps:.1f} | Failed: {r3.failed_requests}")

    print("\n" + "="*80)
    print("AUTHENTICATED API EVIDENCE SUMMARY:")
    print("="*80)
    table_md = format_markdown_table(results)
    print(table_md)

    with open("tests/performance/authenticated_benchmark_results.json", "w") as f:
        json.dump([{
            "scenario": r.scenario_name,
            "concurrency": r.concurrency,
            "requests": r.total_requests,
            "rps": r.throughput_rps,
            "p50_ms": r.p50_ms,
            "p95_ms": r.p95_ms,
            "p99_ms": r.p99_ms,
            "failed": r.failed_requests,
            "status_dist": r.status_distribution
        } for r in results], f, indent=2)

    return results


if __name__ == "__main__":
    asyncio.run(main())
