"""
PVK Cinemas — P14-E Catalog & Public Discovery Load Benchmark
Authority: P14 Baseline §14, SRS §10 (NFR-001), Test Strategy §18.

Evaluates Public Catalogue & Availability Endpoints across Concurrency:
1 (Baseline), 10, 25, 50, 100 Concurrent Requests.
Engineering Targets:
- Catalog Read p95 < 200ms
- Availability Read p95 < 200ms
"""

import asyncio
import json
import random
import httpx
from tests.config.test_environment import ENV
from tests.performance.performance_utils import run_concurrent_load, format_markdown_table, BenchmarkResult


class CatalogLoadBenchmark:
    def __init__(self):
        self.base_url = ENV.backend_base_url
        self.sample_movie_id = 3
        self.sample_city_id = 2
        self.sample_theatre_id = 1
        self.sample_show_id = 1

    async def initialize(self):
        """Fetch sample active IDs from live database via HTTP."""
        async with httpx.AsyncClient(timeout=5.0) as client:
            try:
                r_movies = await client.get(f"{self.base_url}/movies")
                if r_movies.status_code == 200:
                    data = r_movies.json().get("data", {})
                    items = data.get("content", [])
                    if items:
                        self.sample_movie_id = items[0]["movieId"]

                r_shows = await client.get(f"{self.base_url}/theatres/{self.sample_theatre_id}/shows")
                if r_shows.status_code == 200:
                    shows = r_shows.json().get("data", [])
                    if shows:
                        self.sample_show_id = shows[0]["showId"]
            except Exception as e:
                print(f"[WARN] Initialization failed to fetch dynamic IDs: {e}. Using defaults.")

    def get_catalog_endpoints(self) -> list[str]:
        return [
            f"/movies",
            f"/movies/{self.sample_movie_id}",
            f"/cities",
            f"/cities/{self.sample_city_id}/theatres",
            f"/theatres/{self.sample_theatre_id}",
            f"/theatres/{self.sample_theatre_id}/shows",
            f"/movies/{self.sample_movie_id}/shows",
            f"/shows/{self.sample_show_id}/seats"
        ]

    async def request_catalog(self, client: httpx.AsyncClient, req_idx: int):
        endpoints = self.get_catalog_endpoints()
        # Round-robin or uniform selection across catalog endpoints
        endpoint = endpoints[req_idx % len(endpoints)]
        url = f"{self.base_url}{endpoint}"
        resp = await client.get(url)
        return resp, endpoint, "GET"


async def main():
    bench = CatalogLoadBenchmark()
    await bench.initialize()

    concurrency_levels = [1, 10, 25, 50, 100]
    total_requests_map = {1: 30, 10: 50, 25: 100, 50: 150, 100: 200}

    results: list[BenchmarkResult] = []

    print("================================================================================")
    print("PVK Cinemas — P14-E Catalog & Discovery Load Benchmark")
    print(f"Backend Target: {ENV.backend_base_url}")
    print("================================================================================")

    for c in concurrency_levels:
        n_req = total_requests_map[c]
        scenario = f"Catalog Discovery ({c}c)"
        print(f"\n[RUNNING] {scenario}: {n_req} requests, concurrency={c}...")
        res = await run_concurrent_load(
            scenario_name=scenario,
            request_fn=bench.request_catalog,
            total_requests=n_req,
            concurrency=c,
            warmup_requests=10
        )
        results.append(res)
        print(f"  --> Completed in {res.duration_seconds:.2f}s | RPS: {res.throughput_rps:.1f} | P50: {res.p50_ms:.1f}ms | P95: {res.p95_ms:.1f}ms | P99: {res.p99_ms:.1f}ms | Failed: {res.failed_requests}")

    print("\n" + "="*80)
    print("CATALOG PERFORMANCE EVIDENCE SUMMARY:")
    print("="*80)
    table_md = format_markdown_table(results)
    print(table_md)

    with open("tests/performance/catalog_benchmark_results.json", "w") as f:
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
