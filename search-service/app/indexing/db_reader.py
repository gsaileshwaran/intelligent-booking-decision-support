"""
PVK Cinemas Search Service — Database Reader
Read-only SQLAlchemy connection to MySQL for the indexing pipeline.

IMPORTANT BOUNDARY RULES (Authority: ARCH §11, AP-03, BR-009):
- This module is READ-ONLY. It NEVER writes to operational tables.
- It queries: MOVIE, GENRE, MOVIE_GENRE, LANGUAGE, MOVIE_LANGUAGE,
               THEATRE, CITY, CERTIFICATION
- It NEVER queries: USER, EMPLOYEE_PROFILE, SHOW, SHOW_SEAT, AUDIT_LOG
- It NEVER executes INSERT, UPDATE, DELETE on any operational table
- It MAY write to SEARCH_INDEX_DOCUMENT (derived, non-operational)
"""

import logging
from typing import Any
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

logger = logging.getLogger(__name__)


class DbReader:
    """
    Provides read-only access to the MySQL operational database.
    Used exclusively by the indexing pipeline to build search documents.
    Authority: AI-001 (consume authoritative data via controlled indexing pipeline).
    """

    def __init__(self, db_url: str) -> None:
        self._engine: Engine = create_engine(
            db_url,
            pool_pre_ping=True,
            pool_recycle=3600,
            echo=False,
        )

    def test_connection(self) -> bool:
        """Verify database connectivity. Returns True on success."""
        try:
            with self._engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            return True
        except Exception as exc:
            logger.error("Database connection test failed: %s", exc)
            return False

    def fetch_movies(self) -> list[dict[str, Any]]:
        """
        Fetch all movies with their genres and languages for indexing.
        Returns a list of dicts keyed by column name.
        Authority: FR-096, AI-001 — derive from authoritative operational data.
        """
        sql = text("""
            SELECT
                m.movie_id,
                m.title,
                m.original_title,
                m.synopsis,
                m.runtime_minutes,
                m.status,
                m.poster_url,
                c.certification_code,
                GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ' ') AS genre_names,
                GROUP_CONCAT(DISTINCT l.name ORDER BY l.name SEPARATOR ' ') AS language_names
            FROM MOVIE m
            JOIN CERTIFICATION c ON m.certification_id = c.certification_id
            LEFT JOIN MOVIE_GENRE mg ON m.movie_id = mg.movie_id
            LEFT JOIN GENRE g ON mg.genre_id = g.genre_id
            LEFT JOIN MOVIE_LANGUAGE ml ON m.movie_id = ml.movie_id
            LEFT JOIN LANGUAGE l ON ml.language_id = l.language_id
            WHERE m.status NOT IN ('INACTIVE', 'DELETED')
            GROUP BY m.movie_id, m.title, m.original_title, m.synopsis,
                     m.runtime_minutes, m.status, m.poster_url, c.certification_code
            ORDER BY m.movie_id
        """)
        with self._engine.connect() as conn:
            result = conn.execute(sql)
            rows = result.mappings().all()
        return [dict(row) for row in rows]

    def fetch_theatres(self) -> list[dict[str, Any]]:
        """
        Fetch all active theatres with city info for indexing.
        Authority: FR-096, AI-001.
        """
        sql = text("""
            SELECT
                t.theatre_id,
                t.theatre_code,
                t.theatre_name,
                t.address_line_1,
                t.address_line_2,
                t.postal_code,
                t.status,
                c.city_name,
                c.state_name,
                c.country_code,
                GROUP_CONCAT(DISTINCT pf.name ORDER BY pf.name SEPARATOR ' ') AS format_names
            FROM THEATRE t
            JOIN CITY c ON t.city_id = c.city_id
            LEFT JOIN SCREEN scr ON t.theatre_id = scr.theatre_id
            LEFT JOIN SCREEN_CAPABILITY sc ON scr.screen_id = sc.screen_id
            LEFT JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id
            WHERE t.status = 'ACTIVE'
            GROUP BY t.theatre_id, t.theatre_code, t.theatre_name, t.address_line_1,
                     t.address_line_2, t.postal_code, t.status, c.city_name, c.state_name, c.country_code
            ORDER BY t.theatre_id
        """)
        with self._engine.connect() as conn:
            result = conn.execute(sql)
            rows = result.mappings().all()
        return [dict(row) for row in rows]

    def upsert_search_index_document(
        self,
        entity_type: str,
        entity_id: int,
        searchable_text: str,
        metadata_json: str,
        pipeline_version: str,
    ) -> None:
        """
        Upsert a SEARCH_INDEX_DOCUMENT record in MySQL.
        This is the ONLY write operation permitted by the search service.
        Authority: FR-096, FR-097, FR-098, AI-003.

        Note: SEARCH_INDEX_DOCUMENT is a derived projection table, not an
        authoritative operational table (BR-009, SPEC-BASE §F §25).
        """
        upsert_sql = text("""
            INSERT INTO SEARCH_INDEX_DOCUMENT
                (entity_type, entity_id, searchable_text, metadata_json, index_version, status, indexed_at)
            VALUES
                (:entity_type, :entity_id, :searchable_text, :metadata_json, :version, 'ACTIVE', NOW())
            ON DUPLICATE KEY UPDATE
                searchable_text = VALUES(searchable_text),
                metadata_json   = VALUES(metadata_json),
                index_version   = VALUES(index_version),
                status          = 'ACTIVE',
                indexed_at      = NOW()
        """)
        with self._engine.begin() as conn:
            conn.execute(upsert_sql, {
                "entity_type": entity_type,
                "entity_id": entity_id,
                "searchable_text": searchable_text,
                "metadata_json": metadata_json,
                "version": pipeline_version,
            })

    def get_index_stats(self) -> dict[str, int]:
        """
        Return aggregate counts from SEARCH_INDEX_DOCUMENT.
        Used by /internal/v1/index/status.
        """
        sql = text("""
            SELECT status, COUNT(*) AS cnt
            FROM SEARCH_INDEX_DOCUMENT
            GROUP BY status
        """)
        with self._engine.connect() as conn:
            result = conn.execute(sql)
            rows = result.mappings().all()
        counts = {"ACTIVE": 0, "STALE": 0, "DELETED": 0}
        for row in rows:
            status = row["status"]
            if status in counts:
                counts[status] = int(row["cnt"])
        return counts

    def get_last_indexed_at(self) -> str | None:
        """Return the most recent indexed_at timestamp as ISO string."""
        sql = text("SELECT MAX(indexed_at) AS last_indexed FROM SEARCH_INDEX_DOCUMENT")
        with self._engine.connect() as conn:
            result = conn.execute(sql)
            row = result.fetchone()
        if row and row[0]:
            return str(row[0])
        return None
