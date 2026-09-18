-- =============================================================================
-- PVK CINEMAS — 7-DAY DEMO INVENTORY EXPANSION (2026-09-18 TO 2026-09-24)
-- =============================================================================
-- Extends the deterministic mock show inventory across all 5 cities, 25 theatres,
-- and 125 screens for dates:
--   - 2026-09-20 (Day +2, Show IDs 1876-2500)
--   - 2026-09-21 (Day +3, Show IDs 2501-3125)
--   - 2026-09-22 (Day +4, Show IDs 3126-3750)
--   - 2026-09-23 (Day +5, Show IDs 3751-4375)
--   - 2026-09-24 (Day +6, Show IDs 4376-5000)
-- Base schedule is derived from 2026-09-18 (Show IDs 626-1250).
-- Guarantees:
--   1. Zero screen overlap on each physical screen on any date.
--   2. Unique showId for every single screening.
--   3. Dedicated show-specific SHOW_SEAT records (no cross-date contamination).
--   4. Deterministic seat availability (~75% AVAILABLE, ~25% BOOKED).
-- =============================================================================

-- Day +2: 2026-09-20 (IDs 1876 - 2500)
INSERT INTO `SHOW` (show_id, movie_language_id, screen_capability_id, start_at, end_at, status, created_at, updated_at)
SELECT show_id + 1250, movie_language_id, screen_capability_id,
       DATE_ADD(start_at, INTERVAL 2 DAY),
       DATE_ADD(end_at, INTERVAL 2 DAY),
       status, NOW(), NOW()
FROM `SHOW`
WHERE show_id BETWEEN 626 AND 1250;

-- Day +3: 2026-09-21 (IDs 2501 - 3125)
INSERT INTO `SHOW` (show_id, movie_language_id, screen_capability_id, start_at, end_at, status, created_at, updated_at)
SELECT show_id + 1875, movie_language_id, screen_capability_id,
       DATE_ADD(start_at, INTERVAL 3 DAY),
       DATE_ADD(end_at, INTERVAL 3 DAY),
       status, NOW(), NOW()
FROM `SHOW`
WHERE show_id BETWEEN 626 AND 1250;

-- Day +4: 2026-09-22 (IDs 3126 - 3750)
INSERT INTO `SHOW` (show_id, movie_language_id, screen_capability_id, start_at, end_at, status, created_at, updated_at)
SELECT show_id + 2500, movie_language_id, screen_capability_id,
       DATE_ADD(start_at, INTERVAL 4 DAY),
       DATE_ADD(end_at, INTERVAL 4 DAY),
       status, NOW(), NOW()
FROM `SHOW`
WHERE show_id BETWEEN 626 AND 1250;

-- Day +5: 2026-09-23 (IDs 3751 - 4375)
INSERT INTO `SHOW` (show_id, movie_language_id, screen_capability_id, start_at, end_at, status, created_at, updated_at)
SELECT show_id + 3125, movie_language_id, screen_capability_id,
       DATE_ADD(start_at, INTERVAL 5 DAY),
       DATE_ADD(end_at, INTERVAL 5 DAY),
       status, NOW(), NOW()
FROM `SHOW`
WHERE show_id BETWEEN 626 AND 1250;

-- Day +6: 2026-09-24 (IDs 4376 - 5000)
INSERT INTO `SHOW` (show_id, movie_language_id, screen_capability_id, start_at, end_at, status, created_at, updated_at)
SELECT show_id + 3750, movie_language_id, screen_capability_id,
       DATE_ADD(start_at, INTERVAL 6 DAY),
       DATE_ADD(end_at, INTERVAL 6 DAY),
       status, NOW(), NOW()
FROM `SHOW`
WHERE show_id BETWEEN 626 AND 1250;

-- Dedicated SHOW_SEAT records for new shows (show_id >= 1876)
INSERT INTO SHOW_SEAT (show_id, seat_id, availability_status)
SELECT sh.show_id, s.seat_id,
       CASE WHEN MOD(s.seat_id * 37 + sh.show_id * 73 + CAST(s.seat_number AS UNSIGNED) * 19, 100) < 25 THEN 'BOOKED'
            ELSE 'AVAILABLE' END
FROM `SHOW` sh
JOIN SCREEN_CAPABILITY scp ON sh.screen_capability_id = scp.screen_capability_id
JOIN SCREEN sc ON scp.screen_id = sc.screen_id
JOIN SEAT s ON s.screen_id = sc.screen_id
WHERE sh.show_id >= 1876;
