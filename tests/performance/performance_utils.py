"""
PVK Cinemas — Authoritative P14-E Performance & Concurrency Testing Utilities
Provides asynchronous load generators, high-precision latency percentile computation,
concurrency governors, and benchmark reporting.
"""

import asyncio
import time
from dataclasses import dataclass, field
from typing import Callable, Any, Optional
import httpx
from tests.config.test_environment import ENV
from tests.config.test_accounts import ACCOUNTS


@dataclass
class RequestMetric:
    endpoint: str
    method: str
    status_code: int
    latency_ms: float
    is_success: bool
    error: Optional[str] = None


@dataclass
class BenchmarkResult:
    scenario_name: str
    concurrency: int
    total_requests: int
    successful_requests: int
    failed_requests: int
    duration_seconds: float
    throughput_rps: float
    p50_ms: float
    p90_ms: float
    p95_ms: float
    p99_ms: float
    min_ms: float
    max_ms: float
    avg_ms: float
    status_distribution: dict[int, int] = field(default_factory=dict)
    error_summary: dict[str, int] = field(default_factory=dict)
    timeouts: int = 0

    def to_markdown_row(self) -> str:
        return (
            f"| {self.scenario_name} "
            f"| {self.concurrency} "
            f"| {self.total_requests} "
            f"| {self.throughput_rps:.1f} "
            f"| {self.p50_ms:.1f} "
            f"| {self.p90_ms:.1f} "
            f"| {self.p95_ms:.1f} "
            f"| {self.p99_ms:.1f} "
            f"| {self.max_ms:.1f} "
            f"| {self.failed_requests} ({self.timeouts} to) |"
        )


def calculate_percentiles(latencies: list[float]) -> dict[str, float]:
    """Calculates exact percentiles from recorded latencies in milliseconds."""
    if not latencies:
        return {"p50": 0.0, "p90": 0.0, "p95": 0.0, "p99": 0.0, "min": 0.0, "max": 0.0, "avg": 0.0}

    sorted_lats = sorted(latencies)
    n = len(sorted_lats)

    def get_p(pct: float) -> float:
        idx = int(pct * n / 100.0)
        idx = min(idx, n - 1)
        return sorted_lats[idx]

    return {
        "p50": get_p(50),
        "p90": get_p(90),
        "p95": get_p(95),
        "p99": get_p(99),
        "min": sorted_lats[0],
        "max": sorted_lats[-1],
        "avg": sum(sorted_lats) / n
    }


async def run_concurrent_load(
    scenario_name: str,
    request_fn: Callable[[httpx.AsyncClient, int], Any],
    total_requests: int,
    concurrency: int,
    timeout_seconds: float = 10.0,
    warmup_requests: int = 5
) -> BenchmarkResult:
    """
    Executes a high-precision asynchronous concurrent load test against the designated target.
    Uses asyncio.Semaphore to enforce strictly bounded concurrent in-flight requests.
    """
    limits = httpx.Limits(max_connections=max(concurrency * 2, 200), max_keepalive_connections=concurrency)
    timeout = httpx.Timeout(timeout_seconds, connect=5.0)

    async with httpx.AsyncClient(limits=limits, timeout=timeout) as client:
        # 1. Warm-up requests (discarded from metrics to avoid cold-start skew)
        for w in range(warmup_requests):
            try:
                await request_fn(client, w)
            except Exception:
                pass

        # 2. Benchmark execution
        semaphore = asyncio.Semaphore(concurrency)
        metrics: list[RequestMetric] = []
        status_counts: dict[int, int] = {}
        error_counts: dict[str, int] = {}
        timeouts = 0

        async def worker(req_idx: int):
            nonlocal timeouts
            async with semaphore:
                t0 = time.perf_counter()
                try:
                    resp, endpoint, method = await request_fn(client, req_idx)
                    elapsed = (time.perf_counter() - t0) * 1000.0
                    status = resp.status_code
                    status_counts[status] = status_counts.get(status, 0) + 1
                    is_ok = 200 <= status < 400
                    metric = RequestMetric(endpoint, method, status, elapsed, is_ok)
                    metrics.append(metric)
                except httpx.TimeoutException:
                    elapsed = (time.perf_counter() - t0) * 1000.0
                    timeouts += 1
                    error_counts["Timeout"] = error_counts.get("Timeout", 0) + 1
                    metrics.append(RequestMetric("unknown", "GET", 0, elapsed, False, "Timeout"))
                except Exception as exc:
                    elapsed = (time.perf_counter() - t0) * 1000.0
                    err_msg = type(exc).__name__
                    error_counts[err_msg] = error_counts.get(err_msg, 0) + 1
                    metrics.append(RequestMetric("unknown", "GET", 0, elapsed, False, err_msg))

        start_time = time.perf_counter()
        tasks = [asyncio.create_task(worker(i)) for i in range(total_requests)]
        await asyncio.gather(*tasks)
        total_time = time.perf_counter() - start_time

        latencies = [m.latency_ms for m in metrics if m.is_success]
        all_latencies = [m.latency_ms for m in metrics]
        pcts = calculate_percentiles(latencies if latencies else all_latencies)

        successful = sum(1 for m in metrics if m.is_success)
        failed = len(metrics) - successful
        rps = total_requests / total_time if total_time > 0 else 0.0

        return BenchmarkResult(
            scenario_name=scenario_name,
            concurrency=concurrency,
            total_requests=total_requests,
            successful_requests=successful,
            failed_requests=failed,
            duration_seconds=total_time,
            throughput_rps=rps,
            p50_ms=pcts["p50"],
            p90_ms=pcts["p90"],
            p95_ms=pcts["p95"],
            p99_ms=pcts["p99"],
            min_ms=pcts["min"],
            max_ms=pcts["max"],
            avg_ms=pcts["avg"],
            status_distribution=status_counts,
            error_summary=error_counts,
            timeouts=timeouts
        )


def format_markdown_table(results: list[BenchmarkResult]) -> str:
    """Renders a formatted markdown evidence table from a list of BenchmarkResults."""
    header = (
        "| Scenario | Concurrency | Requests | RPS | P50 (ms) | P90 (ms) | P95 (ms) | P99 (ms) | Max (ms) | Failures |\n"
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n"
    )
    rows = [r.to_markdown_row() for r in results]
    return header + "\n".join(rows) + "\n"
