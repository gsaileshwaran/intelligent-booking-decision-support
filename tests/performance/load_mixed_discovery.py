"""
PVK Cinemas — P14-E Mixed Discovery Concurrency Benchmark
Authority: P14 Baseline §14.2, SRS §10 (NFR-001).

Simulates a realistic mixed customer discovery traffic pattern:
- 85-90% Catalogue and Availability Reads (/movies, /theatres, /shows/seats)
- 10-15% Hybrid Search Queries (/search)
Target: 100 Concurrent Virtual Users.
"""

import asyncio
import json
import random
import httpx
from tests.config.test_environment import ENV
from tests.performance.performance_utils import run_concurrent_load, format_markdown_table, BenchmarkResult


class MixedDiscoveryBenchmark:
    def __init__(self):
        self.base_url = ENV.backend_base_url
        self.sample_show_id = 1
        self.catalog_endpoints = []
        self.search_queries = [
            "/search?query=Inception",
            "/search?query=Sci-Fi+IMAX",
            "/search?query=Action+Thriller",
            "/search?query=Bengaluru"
        ]

    async def initialize(self):
        async with httpx.AsyncClient(timeout=5.0) as client:
            try:
                r_shows = await client.get(f"{self.base_url}/theatres/1/shows")
                if r_shows.status_code == 200:
                    shows = r_shows.json().get("data", [])
                    if shows:
                        self.sample_show_id = shows[0]["showId"]
            except Exception:
                pass

        self.catalog_endpoints = [
            "/movies",
            "/movies/3",
            "/cities",
            "/cities/2/theatres",
            "/theatres/1",
            "/theatres/1/shows",
            "/movies/3/shows",
            f"/shows/{self.sample_show_id}/seats"
        ]

    async def request_mixed(self, client: httpx.AsyncClient, req_idx: int):
        # 87.5% Catalog reads, 12.5% Search queries (7:1 ratio)
        if req_idx % 8 == 0:
            query = self.search_queries[(req_idx // 8) % len(self.search_queries)]
            url = f"{self.base_url}{query}"
            resp = await client.get(url)
            return resp, "/search", "GET"
        else:
            path = self.catalog_endpoints[req_idx % len(self.catalog_endpoints)]
            url = f"{self.base_url}{path}"
            resp = await client.get(url)
            return resp, path, "GET"


async def main():
    bench = MixedDiscoveryBenchmark()
    await bench.initialize()

    concurrency = 100
    total_requests = 300

    print("================================================================================")
    print("PVK Cinemas — P14-E Mixed Discovery 100-Concurrent Benchmark")
    print(f"Target: {ENV.backend_base_url} (87.5% Catalog, 12.5% Search)")
    print("================================================================================")

    print(f"\n[RUNNING] Mixed Discovery Workload: {total_requests} requests, concurrency={concurrency}...")
    res = await run_concurrent_load(
        scenario_name="Mixed Discovery (100c)",
        request_fn=bench.request_mixed,
        total_requests=total_requests,
        concurrency=concurrency,
        warmup_requests=10,
        timeout_seconds=15.0
    )

    print(f"\n  --> Completed in {res.duration_seconds:.2f}s | RPS: {res.throughput_rps:.1f} | P50: {res.p50_ms:.1f}ms | P95: {res.p95_ms:.1f}ms | P99: {res.p99_ms:.1f}ms | Failed: {res.failed_requests}")

    print("\n" + "="*80)
    print("MIXED DISCOVERY 100-CONCURRENT EVIDENCE SUMMARY:")
    print("="*80)
    table_md = format_markdown_table([res])
    print(table_md)

    with open("tests/performance/mixed_discovery_results.json", "w") as f:
        json.dump([{
            "scenario": res.scenario_name,
            "concurrency": res.concurrency,
            "requests": res.total_requests,
            "rps": res.throughput_rps,
            "p50_ms": res.p50_ms,
            "p95_ms": res.p95_ms,
            "p99_ms": res.p99_ms,
            "failed": res.failed_requests,
            "status_dist": res.status_distribution
        }], f, indent=2)

    return res


if __name__ == "__main__":
    asyncio.run(main())
