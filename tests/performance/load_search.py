"""
PVK Cinemas — P14-E Search Microservice & Proxy Load Benchmark
Authority: P14 Baseline §14.2, ARCH §11, SRS §10 (NFR-001).

Evaluates AI Search Hybrid Retrieval via Spring Boot Proxy across Concurrency:
1 (Baseline), 10, 25, 50, 100 Concurrent Requests.
Engineering Target:
- Search Hybrid Retrieval p95 < 500ms across proxy boundary under concurrency
"""

import asyncio
import json
import random
import httpx
from tests.config.test_environment import ENV
from tests.performance.performance_utils import run_concurrent_load, format_markdown_table, BenchmarkResult


class SearchLoadBenchmark:
    def __init__(self):
        self.base_url = ENV.backend_base_url
        self.queries = [
            ("GET", "/search?query=Inception", None),
            ("GET", "/search?query=Sci-Fi", None),
            ("GET", "/search?query=NonexistentMovieQuery9999", None),  # No-result query
            ("GET", "/search?query=Action+Adventure+Thriller+Cinema", None),  # Multi-term query
            ("POST", "/search", {"query": "Inception IMAX", "limit": 10}),
            ("POST", "/search", {"query": "PVK Cinema Theatre", "entityType": "THEATRE", "limit": 5}),
            ("POST", "/search", {"query": "Christopher Nolan Mind-Bending", "limit": 8}),
        ]

    async def request_search(self, client: httpx.AsyncClient, req_idx: int):
        method, path, body = self.queries[req_idx % len(self.queries)]
        url = f"{self.base_url}{path}"
        if method == "GET":
            resp = await client.get(url)
        else:
            resp = await client.post(url, json=body)
        return resp, path, method


async def main():
    bench = SearchLoadBenchmark()

    concurrency_levels = [1, 10, 25, 50, 100]
    total_requests_map = {1: 20, 10: 40, 25: 75, 50: 100, 100: 150}

    results: list[BenchmarkResult] = []

    print("================================================================================")
    print("PVK Cinemas — P14-E AI Search Hybrid Retrieval Load Benchmark")
    print(f"Target: Spring Boot Search Proxy ({ENV.backend_base_url}/search) -> FastAPI Search Service")
    print("================================================================================")

    for c in concurrency_levels:
        n_req = total_requests_map[c]
        scenario = f"AI Search Hybrid ({c}c)"
        print(f"\n[RUNNING] {scenario}: {n_req} requests, concurrency={c}...")
        res = await run_concurrent_load(
            scenario_name=scenario,
            request_fn=bench.request_search,
            total_requests=n_req,
            concurrency=c,
            warmup_requests=5,
            timeout_seconds=15.0
        )
        results.append(res)
        print(f"  --> Completed in {res.duration_seconds:.2f}s | RPS: {res.throughput_rps:.1f} | P50: {res.p50_ms:.1f}ms | P95: {res.p95_ms:.1f}ms | P99: {res.p99_ms:.1f}ms | Failed: {res.failed_requests}")

    print("\n" + "="*80)
    print("SEARCH PERFORMANCE EVIDENCE SUMMARY:")
    print("="*80)
    table_md = format_markdown_table(results)
    print(table_md)

    with open("tests/performance/search_benchmark_results.json", "w") as f:
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
