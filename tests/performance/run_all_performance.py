"""
PVK Cinemas — P14-E Master Performance & Concurrency Verification Suite
Authority: P14 Baseline §14, SRS §10 (NFR-001), Test Strategy §18, §26 (Gate G7).

Orchestrates the execution of the entire P14-E performance matrix:
1. Catalog Load Benchmark (1c, 10c, 25c, 50c, 100c)
2. AI Search Hybrid Load Benchmark (1c, 10c, 25c, 50c, 100c)
3. Mixed Discovery 100-Concurrent Load Benchmark
4. Show Scheduling Concurrency & Atomicity Benchmark
5. Authenticated API Load Benchmark
"""

import asyncio
import json
import time
from tests.performance.load_catalog import main as run_catalog
from tests.performance.load_search import main as run_search
from tests.performance.load_mixed_discovery import main as run_mixed
from tests.performance.load_scheduling import main as run_scheduling
from tests.performance.load_authenticated import main as run_authenticated
from tests.performance.performance_utils import format_markdown_table, BenchmarkResult


async def master():
    print("\n" + "#"*80)
    print("# STARTING PVK CINEMAS P14-E PERFORMANCE, LOAD & CONCURRENCY VERIFICATION")
    print("#"*80)

    start_all = time.perf_counter()

    # 1. Catalog Load
    print("\n>>> Phase 1/5: Catalog & Public Discovery Load (1c, 10c, 25c, 50c, 100c) <<<")
    cat_results: list[BenchmarkResult] = await run_catalog()

    # 2. Search Load
    print("\n>>> Phase 2/5: AI Search Hybrid Retrieval Load (1c, 10c, 25c, 50c, 100c) <<<")
    search_results: list[BenchmarkResult] = await run_search()

    # 3. Mixed Discovery Load
    print("\n>>> Phase 3/5: Mixed Customer Discovery 100-Concurrent Load <<<")
    mixed_result: BenchmarkResult = await run_mixed()

    # 4. Scheduling Concurrency & Atomicity
    print("\n>>> Phase 4/5: Show Scheduling Concurrency, BR-004 Lock & Atomicity <<<")
    sched_passed: bool = await run_scheduling()

    # 5. Authenticated API Load
    print("\n>>> Phase 5/5: Authenticated Operations Concurrency Load <<<")
    auth_results: list[BenchmarkResult] = await run_authenticated()

    total_time = time.perf_counter() - start_all

    all_benchmarks = cat_results + search_results + [mixed_result] + auth_results

    print("\n" + "="*80)
    print("MASTER PERFORMANCE EVIDENCE MATRIX (P14-E):")
    print("="*80)
    full_table = format_markdown_table(all_benchmarks)
    print(full_table)
    print(f"Show Scheduling Concurrency & SHOW_SEAT Atomicity: {'PASS' if sched_passed else 'FAIL'}")
    print(f"Total Benchmark Suite Duration: {total_time:.2f} seconds")

    with open("tests/performance/master_performance_matrix.json", "w") as f:
        json.dump({
            "total_duration_sec": total_time,
            "scheduling_atomicity_pass": sched_passed,
            "benchmarks": [{
                "scenario": b.scenario_name,
                "concurrency": b.concurrency,
                "requests": b.total_requests,
                "throughput_rps": b.throughput_rps,
                "p50_ms": b.p50_ms,
                "p90_ms": b.p90_ms,
                "p95_ms": b.p95_ms,
                "p99_ms": b.p99_ms,
                "max_ms": b.max_ms,
                "failed": b.failed_requests,
                "timeouts": b.timeouts,
                "status_dist": b.status_distribution
            } for b in all_benchmarks]
        }, f, indent=2)

    return all_benchmarks, sched_passed


if __name__ == "__main__":
    asyncio.run(master())
