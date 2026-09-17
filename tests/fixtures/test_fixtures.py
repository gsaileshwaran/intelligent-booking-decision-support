"""
PVK Cinemas — Authoritative P14 Test Fixture Strategy
Provides deterministic fixture generators, isolated execution contexts,
and non-destructive teardown management.
"""

import uuid
from typing import Any

class FixtureRegistry:
    """
    Tracks all temporary test entities created during test execution
    to guarantee deterministic, isolated cleanup without polluting
    the database or mutating reference seed data.
    """
    def __init__(self, run_id: str | None = None):
        self.run_id = run_id or uuid.randomUUID().hex[:8] if hasattr(uuid, 'randomUUID') else uuid.uuid4().hex[:8]
        self._created_entities: list[dict[str, Any]] = []

    def get_test_prefix(self) -> str:
        return f"pvk_test_{self.run_id}"

    def register(self, entity_type: str, entity_id: Any, metadata: dict[str, Any] | None = None) -> None:
        self._created_entities.append({
            "type": entity_type,
            "id": entity_id,
            "metadata": metadata or {}
        })

    def get_created_entities(self) -> list[dict[str, Any]]:
        return list(self._created_entities)

    def generate_unique_email(self, prefix: str = "user") -> str:
        return f"{prefix}_{self.run_id}_{uuid.uuid4().hex[:6]}@pvkcinemas.test"

    def generate_unique_phone(self) -> str:
        # Generates test phone in format +919XXXXXXXX
        import random
        return f"+919{random.randint(10000000, 99999999)}"

    def generate_movie_payload(self, title_suffix: str = "Feature") -> dict[str, Any]:
        return {
            "title": f"Test Movie {self.run_id} - {title_suffix}",
            "synopsis": "Deterministic test movie synopsis for regression verification.",
            "runtimeMinutes": 120,
            "releaseDate": "2026-10-01",
            "posterUrl": "https://images.example.com/poster.jpg",
            "trailerUrl": "https://videos.example.com/trailer.mp4",
            "certificationId": 1,
            "genreIds": [1, 2],
            "languageIds": [1, 2]
        }

    def generate_show_payload(self, movie_id: int, screen_id: int, start_time: str, end_time: str) -> dict[str, Any]:
        return {
            "movieId": movie_id,
            "movieLanguageId": 1,
            "screenId": screen_id,
            "screenCapabilityId": 1,
            "startTime": start_time,
            "endTime": end_time,
            "basePrice": 250.00
        }

    def clear(self) -> None:
        self._created_entities.clear()

def ensure_canonical_test_accounts():
    """
    Guarantees that Theatres 1 and 2 and all 4 authenticated testing personas
    (Customer, Manager A, Manager B, Super Admin) are reliably provisioned in MySQL.
    """
    import os
    import pymysql
    from tests.config.test_environment import ENV
    conn = pymysql.connect(
        host=ENV.database_host,
        port=ENV.database_port,
        user=os.getenv("PVK_DB_USER", "root"),
        password=os.getenv("PVK_DB_PASSWORD", "root"),
        database=os.getenv("PVK_DB_NAME", "pvk_cinemas_db")
    )
    cur = conn.cursor()
    try:
        # 1. Ensure Theatres 1 and 2 exist
        cur.execute("SELECT city_id FROM city LIMIT 1")
        city_row = cur.fetchone()
        city_id = city_row[0] if city_row else 1

        cur.execute("""
            INSERT IGNORE INTO theatre (theatre_id, city_id, theatre_code, theatre_name, address_line_1, status)
            VALUES (1, %s, 'TH-P14-001', 'PVK Cinema Theatre Alpha', '100 Main Road', 'ACTIVE')
        """, (city_id,))
        cur.execute("""
            INSERT IGNORE INTO theatre (theatre_id, city_id, theatre_code, theatre_name, address_line_1, status)
            VALUES (2, %s, 'TH-P14-002', 'PVK Cinema Theatre Beta', '200 Cross Road', 'ACTIVE')
        """, (city_id,))

        # Standard bcrypt hash for 'TestPassword123!'
        pw_hash = "$2a$12$zY4PqwTPLuCAvrEzkmWZA.EKcE8IAXduTq9u34CpZHR2J80KFF.xm"

        personas = [
            ("Test", "Customer", "test_customer@pvkcinemas.test", "+919000000002", 3, None),
            ("Manager", "Alpha", "manager_a@pvkcinemas.test", "+919000000003", 2, 1),
            ("Manager", "Beta", "manager_b@pvkcinemas.test", "+919000000004", 2, 2),
            ("Super", "Admin", "admin@pvkcinemas.test", "+919000000001", 1, None),
        ]

        for first, last, email, phone, role_id, theatre_id in personas:
            cur.execute("SELECT user_id FROM user WHERE email = %s", (email,))
            row = cur.fetchone()
            if row:
                user_id = row[0]
                cur.execute("UPDATE user SET password_hash = %s, account_status = 'ACTIVE' WHERE user_id = %s", (pw_hash, user_id))
            else:
                cur.execute("""
                    INSERT INTO user (first_name, last_name, email, phone, password_hash, account_status)
                    VALUES (%s, %s, %s, %s, %s, 'ACTIVE')
                """, (first, last, email, phone, pw_hash))
                user_id = cur.lastrowid

            cur.execute("INSERT IGNORE INTO user_role (user_id, role_id, status) VALUES (%s, %s, 'ACTIVE')", (user_id, role_id))

            if role_id == 3:
                cur.execute("INSERT IGNORE INTO customer_profile (user_id) VALUES (%s)", (user_id,))
            elif role_id == 2:
                cur.execute("""
                    INSERT IGNORE INTO employee_profile (user_id, employee_code, joining_date, status)
                    VALUES (%s, %s, CURDATE(), 'ACTIVE')
                """, (user_id, f"EMP-{first[:3].upper()}-{user_id}"))
                cur.execute("DELETE FROM employee_theatre WHERE user_id = %s", (user_id,))
                cur.execute("INSERT INTO employee_theatre (user_id, theatre_id, status) VALUES (%s, %s, 'ACTIVE')", (user_id, theatre_id))
            elif role_id == 1:
                cur.execute("""
                    INSERT IGNORE INTO employee_profile (user_id, employee_code, joining_date, status)
                    VALUES (%s, %s, CURDATE(), 'ACTIVE')
                """, (user_id, f"EMP-ADM-{user_id}"))

        # Ensure valid dates in employee_profile
        cur.execute("UPDATE employee_profile SET joining_date = CURDATE() WHERE joining_date IS NULL")

        # 2. Ensure Screens in Theatre 1 and Theatre 2
        for th_id, scr_code, scr_name in [(1, 'SCR-TH1-01', 'Screen 1 (Alpha)'), (2, 'SCR-TH2-01', 'Screen 1 (Beta)')]:
            cur.execute("SELECT screen_id FROM screen WHERE theatre_id = %s LIMIT 1", (th_id,))
            row = cur.fetchone()
            if not row:
                cur.execute("INSERT INTO screen (theatre_id, screen_code, screen_name, status) VALUES (%s, %s, %s, 'ACTIVE')", (th_id, scr_code, scr_name))
                scr_id = cur.lastrowid
            else:
                scr_id = row[0]

            # Screen capability (2D, Dolby 5.1)
            cur.execute("SELECT screen_capability_id FROM screen_capability WHERE screen_id = %s LIMIT 1", (scr_id,))
            if not cur.fetchone():
                cur.execute("INSERT INTO screen_capability (screen_id, presentation_format_id, audio_format_id) VALUES (%s, 1, 1)", (scr_id,))

            # Seats
            cur.execute("SELECT seat_id FROM seat WHERE screen_id = %s LIMIT 1", (scr_id,))
            if not cur.fetchone():
                cur.execute("INSERT INTO seat (screen_id, seat_type_id, row_label, seat_number, status) VALUES (%s, 1, 'A', '1', 'ACTIVE')", (scr_id,))
                cur.execute("INSERT INTO seat (screen_id, seat_type_id, row_label, seat_number, status) VALUES (%s, 1, 'A', '2', 'ACTIVE')", (scr_id,))

        # 3. Ensure baseline show in Theatre 1
        cur.execute("""
            SELECT s.show_id FROM `show` s
            JOIN screen_capability sc ON s.screen_capability_id = sc.screen_capability_id
            JOIN screen scr ON sc.screen_id = scr.screen_id
            WHERE scr.theatre_id = 1 LIMIT 1
        """)
        if not cur.fetchone():
            cur.execute("SELECT movie_language_id FROM movie_language LIMIT 1")
            ml_id = cur.fetchone()[0]
            cur.execute("""
                SELECT sc.screen_capability_id, sc.screen_id FROM screen_capability sc
                JOIN screen scr ON sc.screen_id = scr.screen_id
                WHERE scr.theatre_id = 1 LIMIT 1
            """)
            sc_row = cur.fetchone()
            sc_id, scr_id = sc_row[0], sc_row[1]

            import datetime
            now = datetime.datetime.utcnow()
            st = now + datetime.timedelta(days=1, hours=2)
            et = now + datetime.timedelta(days=1, hours=4)
            cur.execute("""
                INSERT INTO `show` (screen_capability_id, movie_language_id, start_at, end_at, status)
                VALUES (%s, %s, %s, %s, 'SCHEDULED')
            """, (sc_id, ml_id, st, et))
            show_1_id = cur.lastrowid

            cur.execute("SELECT seat_id FROM seat WHERE screen_id = %s", (scr_id,))
            for seat_row in cur.fetchall():
                cur.execute("INSERT IGNORE INTO show_seat (show_id, seat_id, availability_status) VALUES (%s, %s, 'AVAILABLE')", (show_1_id, seat_row[0]))

        # 4. Ensure baseline show in Theatre 2
        cur.execute("""
            SELECT s.show_id FROM `show` s
            JOIN screen_capability sc ON s.screen_capability_id = sc.screen_capability_id
            JOIN screen scr ON sc.screen_id = scr.screen_id
            WHERE scr.theatre_id = 2 LIMIT 1
        """)
        if not cur.fetchone():
            cur.execute("SELECT movie_language_id FROM movie_language LIMIT 1")
            ml_id = cur.fetchone()[0]
            cur.execute("""
                SELECT sc.screen_capability_id, sc.screen_id FROM screen_capability sc
                JOIN screen scr ON sc.screen_id = scr.screen_id
                WHERE scr.theatre_id = 2 LIMIT 1
            """)
            sc_row = cur.fetchone()
            sc_id, scr_id = sc_row[0], sc_row[1]

            import datetime
            now = datetime.datetime.utcnow()
            st = now + datetime.timedelta(days=1, hours=2)
            et = now + datetime.timedelta(days=1, hours=4)
            cur.execute("""
                INSERT INTO `show` (screen_capability_id, movie_language_id, start_at, end_at, status)
                VALUES (%s, %s, %s, %s, 'SCHEDULED')
            """, (sc_id, ml_id, st, et))
            show_2_id = cur.lastrowid

            cur.execute("SELECT seat_id FROM seat WHERE screen_id = %s", (scr_id,))
            for seat_row in cur.fetchall():
                cur.execute("INSERT IGNORE INTO show_seat (show_id, seat_id, availability_status) VALUES (%s, %s, 'AVAILABLE')", (show_2_id, seat_row[0]))

        conn.commit()
    finally:
        cur.close()
        conn.close()

GLOBAL_FIXTURE_REGISTRY = FixtureRegistry()

