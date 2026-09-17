"""
PVK Cinemas — P14-E Concurrent Show Scheduling & Atomicity Benchmark
Authority: P14 Baseline §14.2, ARCH §14, SRS §7 (BR-004, DECISION-019).

Verifies transactional atomicity and concurrency control during show creation:
- Case A: Two concurrent non-overlapping shows on the same screen (both succeed).
- Case B: Two concurrent overlapping shows on the same screen (pessimistic lock + BR-004: exactly 1 succeeds, 1 receives 409 Conflict).
- Case C: Two concurrent shows on different screens (independent execution: both succeed).
- Atomicity: Verifies SHOW_SEAT rows match physical seats exactly, and zero orphan SHOW_SEAT rows exist.
"""

import asyncio
import json
import time
import httpx
import pymysql
from tests.config.test_environment import ENV
from tests.config.test_accounts import ACCOUNTS


class SchedulingConcurrencyBenchmark:
    def __init__(self):
        self.base_url = ENV.backend_base_url
        self.token_manager_a = None
        self.token_manager_b = None
        self.theatre_1_id = 1
        self.theatre_2_id = 2
        self.screen_theatre_1 = 209
        self.screen_theatre_2 = 210
        self.cap_theatre_1 = 208
        self.cap_theatre_2 = 209
        self.movie_id = 3
        self.movie_language_id = 3
        self.created_show_ids = []

    async def authenticate(self):
        async with httpx.AsyncClient(timeout=10.0) as client:
            # Login Manager A
            resp_a = await client.post(f"{self.base_url}/auth/login", json={
                "email": ACCOUNTS["manager_a"].email,
                "password": ACCOUNTS["manager_a"].password
            })
            assert resp_a.status_code == 200, f"Manager A login failed: {resp_a.text}"
            self.token_manager_a = resp_a.json()["token"]

            # Login Manager B
            resp_b = await client.post(f"{self.base_url}/auth/login", json={
                "email": ACCOUNTS["manager_b"].email,
                "password": ACCOUNTS["manager_b"].password
            })
            assert resp_b.status_code == 200, f"Manager B login failed: {resp_b.text}"
            self.token_manager_b = resp_b.json()["token"]

    def _get_headers(self, token: str) -> dict:
        return {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

    async def run_case_a_non_overlapping(self) -> dict:
        """Case A: Two concurrent non-overlapping shows on Screen 209."""
        print("\n--- Testing Case A: Two Concurrent Non-Overlapping Shows (Same Screen) ---")
        headers = self._get_headers(self.token_manager_a)
        url = f"{self.base_url}/manager/theatres/{self.theatre_1_id}/shows"

        payload1 = {
            "movieId": self.movie_id,
            "movieLanguageId": self.movie_language_id,
            "screenId": self.screen_theatre_1,
            "screenCapabilityId": self.cap_theatre_1,
            "startAt": "2026-11-10T10:00:00Z",
            "endAt": "2026-11-10T12:00:00Z",
            "showStatus": "SCHEDULED"
        }
        payload2 = {
            "movieId": self.movie_id,
            "movieLanguageId": self.movie_language_id,
            "screenId": self.screen_theatre_1,
            "screenCapabilityId": self.cap_theatre_1,
            "startAt": "2026-11-10T13:00:00Z",
            "endAt": "2026-11-10T15:00:00Z",
            "showStatus": "SCHEDULED"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            t0 = time.perf_counter()
            r1, r2 = await asyncio.gather(
                client.post(url, json=payload1, headers=headers),
                client.post(url, json=payload2, headers=headers)
            )
            elapsed = (time.perf_counter() - t0) * 1000.0

        if r1.status_code == 201:
            self.created_show_ids.append(r1.json()["data"]["showId"])
        if r2.status_code == 201:
            self.created_show_ids.append(r2.json()["data"]["showId"])

        success = (r1.status_code == 201 and r2.status_code == 201)
        print(f"  Result: Req 1={r1.status_code}, Req 2={r2.status_code} in {elapsed:.1f}ms -> {'PASS' if success else 'FAIL'}")
        return {"case": "Case A (Non-Overlapping)", "r1_status": r1.status_code, "r2_status": r2.status_code, "elapsed_ms": elapsed, "pass": success}

    async def run_case_b_overlapping(self) -> dict:
        """Case B: Two concurrent overlapping shows on Screen 209 (Pessimistic Lock & BR-004)."""
        print("\n--- Testing Case B: Two Concurrent Overlapping Shows (Same Screen Race Condition) ---")
        headers = self._get_headers(self.token_manager_a)
        url = f"{self.base_url}/manager/theatres/{self.theatre_1_id}/shows"

        # Overlapping time window: 18:00 - 20:30 vs 19:30 - 22:00
        payload1 = {
            "movieId": self.movie_id,
            "movieLanguageId": self.movie_language_id,
            "screenId": self.screen_theatre_1,
            "screenCapabilityId": self.cap_theatre_1,
            "startAt": "2026-11-11T18:00:00Z",
            "endAt": "2026-11-11T20:30:00Z",
            "showStatus": "SCHEDULED"
        }
        payload2 = {
            "movieId": self.movie_id,
            "movieLanguageId": self.movie_language_id,
            "screenId": self.screen_theatre_1,
            "screenCapabilityId": self.cap_theatre_1,
            "startAt": "2026-11-11T19:30:00Z",
            "endAt": "2026-11-11T22:00:00Z",
            "showStatus": "SCHEDULED"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            t0 = time.perf_counter()
            r1, r2 = await asyncio.gather(
                client.post(url, json=payload1, headers=headers),
                client.post(url, json=payload2, headers=headers)
            )
            elapsed = (time.perf_counter() - t0) * 1000.0

        if r1.status_code == 201:
            self.created_show_ids.append(r1.json()["data"]["showId"])
        if r2.status_code == 201:
            self.created_show_ids.append(r2.json()["data"]["showId"])

        # Exactly one must succeed (201) and one must be rejected (409 Conflict)
        statuses = sorted([r1.status_code, r2.status_code])
        success = (statuses == [201, 409])
        print(f"  Result: Statuses={statuses} in {elapsed:.1f}ms -> {'PASS (BR-004 Lock Enforced)' if success else 'FAIL'}")
        return {"case": "Case B (Overlapping Race)", "statuses": statuses, "elapsed_ms": elapsed, "pass": success}

    async def run_case_c_different_screens(self) -> dict:
        """Case C: Two concurrent shows on different screens in different theatres."""
        print("\n--- Testing Case C: Concurrent Shows Across Different Screens (Screen 209 vs 210) ---")
        headers_a = self._get_headers(self.token_manager_a)
        headers_b = self._get_headers(self.token_manager_b)

        url_a = f"{self.base_url}/manager/theatres/{self.theatre_1_id}/shows"
        url_b = f"{self.base_url}/manager/theatres/{self.theatre_2_id}/shows"

        # Identical time window, but different screens in different theatres
        payload_a = {
            "movieId": self.movie_id,
            "movieLanguageId": self.movie_language_id,
            "screenId": self.screen_theatre_1,
            "screenCapabilityId": self.cap_theatre_1,
            "startAt": "2026-11-12T14:00:00Z",
            "endAt": "2026-11-12T16:30:00Z",
            "showStatus": "SCHEDULED"
        }
        payload_b = {
            "movieId": self.movie_id,
            "movieLanguageId": self.movie_language_id,
            "screenId": self.screen_theatre_2,
            "screenCapabilityId": self.cap_theatre_2,
            "startAt": "2026-11-12T14:00:00Z",
            "endAt": "2026-11-12T16:30:00Z",
            "showStatus": "SCHEDULED"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            t0 = time.perf_counter()
            r_a, r_b = await asyncio.gather(
                client.post(url_a, json=payload_a, headers=headers_a),
                client.post(url_b, json=payload_b, headers=headers_b)
            )
            elapsed = (time.perf_counter() - t0) * 1000.0

        if r_a.status_code == 201:
            self.created_show_ids.append(r_a.json()["data"]["showId"])
        if r_b.status_code == 201:
            self.created_show_ids.append(r_b.json()["data"]["showId"])

        success = (r_a.status_code == 201 and r_b.status_code == 201)
        print(f"  Result: Manager A={r_a.status_code}, Manager B={r_b.status_code} in {elapsed:.1f}ms -> {'PASS' if success else 'FAIL'}")
        return {"case": "Case C (Different Screens)", "r_a_status": r_a.status_code, "r_b_status": r_b.status_code, "elapsed_ms": elapsed, "pass": success}

    def verify_atomicity_and_cleanup(self) -> dict:
        """Verifies SHOW_SEAT integrity directly in MySQL and cleans up test rows."""
        print("\n--- Verifying SHOW_SEAT Atomicity & Database Integrity ---")
        conn = pymysql.connect(host=ENV.database_host, user="root", password="root", database="pvk_cinemas_db")
        cursor = conn.cursor()

        # 1. Verify seat counts for each created show match screen capacity (2 seats)
        atomicity_passed = True
        for sid in self.created_show_ids:
            cursor.execute("SELECT count(*) FROM show_seat WHERE show_id = %s", (sid,))
            seat_count = cursor.fetchone()[0]
            print(f"  Show {sid}: {seat_count} SHOW_SEAT records created (Expected 2)")
            if seat_count != 2:
                atomicity_passed = False

        # 2. Verify zero orphan SHOW_SEAT rows
        cursor.execute("SELECT count(*) FROM show_seat ss LEFT JOIN `show` s ON ss.show_id = s.show_id WHERE s.show_id IS NULL")
        orphan_seats = cursor.fetchone()[0]
        print(f"  Orphan SHOW_SEAT rows in database: {orphan_seats} (Expected 0)")
        if orphan_seats != 0:
            atomicity_passed = False

        # 3. Clean up test shows and their show_seat rows
        if self.created_show_ids:
            format_strings = ','.join(['%s'] * len(self.created_show_ids))
            cursor.execute(f"DELETE FROM show_seat WHERE show_id IN ({format_strings})", tuple(self.created_show_ids))
            cursor.execute(f"DELETE FROM `show` WHERE show_id IN ({format_strings})", tuple(self.created_show_ids))
            conn.commit()
            print(f"  Cleaned up {len(self.created_show_ids)} test shows from database.")

        conn.close()
        return {"atomicity_passed": atomicity_passed, "orphan_seats": orphan_seats}


async def main():
    bench = SchedulingConcurrencyBenchmark()
    await bench.authenticate()

    res_a = await bench.run_case_a_non_overlapping()
    res_b = await bench.run_case_b_overlapping()
    res_c = await bench.run_case_c_different_screens()
    integrity = bench.verify_atomicity_and_cleanup()

    print("\n" + "="*80)
    print("SCHEDULING CONCURRENCY & ATOMICITY EVIDENCE SUMMARY:")
    print("="*80)
    print(f"Case A (Non-overlapping): {'PASS' if res_a['pass'] else 'FAIL'} ({res_a['elapsed_ms']:.1f}ms)")
    print(f"Case B (Overlapping race): {'PASS' if res_b['pass'] else 'FAIL'} ({res_b['elapsed_ms']:.1f}ms)")
    print(f"Case C (Different screens): {'PASS' if res_c['pass'] else 'FAIL'} ({res_c['elapsed_ms']:.1f}ms)")
    print(f"SHOW_SEAT Atomicity: {'PASS' if integrity['atomicity_passed'] else 'FAIL'}")

    all_passed = res_a['pass'] and res_b['pass'] and res_c['pass'] and integrity['atomicity_passed']
    print(f"\nOVERALL SCHEDULING CONCURRENCY RESULT: {'PASS' if all_passed else 'FAIL'}")

    with open("tests/performance/scheduling_concurrency_results.json", "w") as f:
        json.dump({
            "case_a": res_a,
            "case_b": res_b,
            "case_c": res_c,
            "integrity": integrity,
            "overall_pass": all_passed
        }, f, indent=2)

    return all_passed


if __name__ == "__main__":
    asyncio.run(main())
