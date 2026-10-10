-- =============================================================================
-- PVK CINEMAS — V10__schema_extensions.sql
-- Schema Extensions for Part 1A, Part 2 (Movie Catalogue Lifecycle) and
-- Part 3 (Year-Long Show Generation + Scheduling Engine) Prerequisites
--
-- Authority:
--   PART_3_YEAR_LONG_SHOW_SCHEDULING_ARCHITECTURE.md (locked)
--   PVK_Cinemas_Final_Database_Design.docx
--
-- Design Rules:
--   1. Additive-only. No existing column modified. No existing constraint dropped.
--   2. New nullable columns for data not yet seeded (Part 1A/2 data comes in V11/V12).
--   3. SHOW.screen_id and SHOW.show_date backfilled from existing rows before
--      NOT NULL and FK constraints are applied.
--   4. UNIQUE(screen_id, start_at) enforces Part 3 ShowIdentity. It does NOT
--      replace interval-overlap application guard (BR-004 in ShowSchedulingService).
--   5. Historical Flyway migrations V1–V9 are not modified.
-- =============================================================================

-- =============================================================================
-- SECTION 1: MOVIE — Lifecycle and Demand Classification Columns
-- =============================================================================
-- These columns support the Part 2 catalogue lifecycle model:
--   demand_tier      → drives theatre breadth, TargetShows, occupancy G()
--   popularity_score → fine-grained demand weight within tier (for G())
--   exhibition_start_date → lifecycle start anchor (elapsedDays = D_ref - startDate)
--   exhibition_end_date   → lifecycle end anchor   (durationDays = endDate - startDate + 1)
--
-- All four columns are NULL until V12 (Part 2 catalogue migration) populates them.
-- The application does not read these columns until Part 2 services are implemented.
-- =============================================================================

ALTER TABLE MOVIE
    ADD COLUMN demand_tier VARCHAR(20) NULL DEFAULT NULL
        COMMENT 'Part 2 demand tier: BLOCKBUSTER, MAJOR_REGIONAL, MID_TIER, NICHE, RE_RELEASE';

ALTER TABLE MOVIE
    ADD COLUMN popularity_score DECIMAL(5,2) NULL DEFAULT NULL
        COMMENT 'Part 2 demand weight within tier [0.00 - 10.00]. Input to occupancy function G().';

ALTER TABLE MOVIE
    ADD COLUMN exhibition_start_date DATE NULL DEFAULT NULL
        COMMENT 'Part 2 lifecycle start anchor. elapsedDays = scheduleDate - exhibition_start_date.';

ALTER TABLE MOVIE
    ADD COLUMN exhibition_end_date DATE NULL DEFAULT NULL
        COMMENT 'Part 2 lifecycle end anchor. durationDays = exhibition_end_date - exhibition_start_date + 1.';

ALTER TABLE MOVIE
    ADD CONSTRAINT chk_movie_demand_tier
        CHECK (demand_tier IN ('BLOCKBUSTER', 'MAJOR_REGIONAL', 'MID_TIER', 'NICHE', 'RE_RELEASE') OR demand_tier IS NULL);

ALTER TABLE MOVIE
    ADD CONSTRAINT chk_movie_popularity_score
        CHECK (popularity_score IS NULL OR (popularity_score >= 0.00 AND popularity_score <= 10.00));

ALTER TABLE MOVIE
    ADD CONSTRAINT chk_movie_exhibition_dates
        CHECK (
            exhibition_start_date IS NULL
            OR exhibition_end_date IS NULL
            OR exhibition_end_date >= exhibition_start_date
        );

-- =============================================================================
-- SECTION 2: THEATRE — Operating Hours and Tier Classification
-- =============================================================================
-- These columns support Part 3 scheduling:
--   opening_time  → earliest show start time (TIME in Asia/Kolkata)
--   closing_time  → latest show end time (TIME in Asia/Kolkata)
--                   closing_time < opening_time means overnight (closes next calendar day)
--   theatre_tier  → venue classification driving operating windows:
--                   FLAGSHIP  → 08:30 to 01:30 (+1 day) = 17.0 hours
--                   SUBURBAN  → 09:30 to 23:45         = 14.25 hours
--                   BOUTIQUE  → 10:00 to 23:00         = 13.0 hours
--
-- All three columns are NULL until V11 (Part 1A dataset migration) populates them.
-- =============================================================================

ALTER TABLE THEATRE
    ADD COLUMN theatre_tier VARCHAR(20) NULL DEFAULT NULL
        COMMENT 'Part 3 venue classification: FLAGSHIP, SUBURBAN, BOUTIQUE';

ALTER TABLE THEATRE
    ADD COLUMN opening_time TIME NULL DEFAULT NULL
        COMMENT 'Part 3 operating window start in Asia/Kolkata. Null until V11 populates.';

ALTER TABLE THEATRE
    ADD COLUMN closing_time TIME NULL DEFAULT NULL
        COMMENT 'Part 3 operating window end in Asia/Kolkata. If closing_time < opening_time, window extends to next calendar day.';

ALTER TABLE THEATRE
    ADD CONSTRAINT chk_theatre_tier
        CHECK (theatre_tier IN ('FLAGSHIP', 'SUBURBAN', 'BOUTIQUE') OR theatre_tier IS NULL);

-- =============================================================================
-- SECTION 3: SCREEN — Premium Screen Flag
-- =============================================================================
-- is_premium drives Part 3 dedicated-screen logic:
--   A screen is dedicated to Movie A if:
--     screen.is_premium = TRUE, OR
--     only 1 movie competes on this screen, OR
--     (Movie A is BLOCKBUSTER AND lifecycle IN [OPENING, PEAK] AND dayType = WEEKEND)
--
-- All existing 125 screens receive is_premium = FALSE (correct — V5 screens are standard).
-- V11 (Part 1A) will mark premium screens (IMAX, 4DX, Dolby) as is_premium = TRUE.
-- =============================================================================

ALTER TABLE SCREEN
    ADD COLUMN is_premium BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Part 3 dedicated-screen indicator. TRUE for IMAX, 4DX, Dolby Cinema screens.';

-- =============================================================================
-- SECTION 4: SHOW — Physical Screen Reference + Exhibition Date
-- =============================================================================
-- Step 4A: Add screen_id and show_date as nullable first (required for backfill)
-- Step 4B: Backfill screen_id from existing SCREEN_CAPABILITY join
-- Step 4C: Backfill show_date from start_at with explicit IST timezone translation
-- Step 4D: Apply NOT NULL constraints
-- Step 4E: Apply FK constraint (fk_show_screen)
-- Step 4F: Apply UNIQUE constraint (uq_show_screen_start)
-- Step 4G: Apply performance index (idx_show_screen_date)
--
-- DESIGN NOTE on UNIQUE(screen_id, start_at):
--   This constraint enforces Part 3 ShowIdentity = (screen_id, start_at).
--   It prevents duplicate SHOW rows for the same physical screen at the same
--   exact start moment. It does NOT prevent interval overlap (a 180-minute show
--   starting at 14:00 can still conflict with a 120-minute show starting at
--   15:30 on the same screen). Interval-overlap prevention remains the
--   responsibility of ShowSchedulingService.countOverlappingShows() (BR-004).
-- =============================================================================

-- 4A: Add nullable columns
ALTER TABLE `SHOW`
    ADD COLUMN screen_id BIGINT NULL DEFAULT NULL
        COMMENT 'Direct FK to physical SCREEN. Backfilled from SCREEN_CAPABILITY. Part 3 physical identity.'
        AFTER movie_language_id;

ALTER TABLE `SHOW`
    ADD COLUMN show_date DATE NULL DEFAULT NULL
        COMMENT 'Exhibition date in Asia/Kolkata (IST). A show starting 23:30 UTC+5:30 on Friday belongs to Friday. Backfilled from start_at.'
        AFTER screen_id;

-- 4B: Backfill screen_id — deterministic 1:1 join with SCREEN_CAPABILITY
--     V5 has screen_capability_id == screen_id for all 125 capabilities.
--     All 5,000+ existing SHOW rows have a valid screen_capability_id with a known screen_id.
UPDATE `SHOW` s
    JOIN SCREEN_CAPABILITY sc ON s.screen_capability_id = sc.screen_capability_id
    SET s.screen_id = sc.screen_id;

-- 4C: Backfill show_date — translate UTC start_at to IST exhibition date
--     CONVERT_TZ(start_at, '+00:00', '+05:30') shifts from UTC to IST.
--     DATE(...) extracts the calendar date portion in IST.
--     Late-night shows (e.g. start_at = 21:25 UTC = 02:55 IST next day)
--     correctly get the next calendar date, matching the locked architecture
--     principle that the showing date is the IST calendar date of start_at.
UPDATE `SHOW`
    SET show_date = DATE(CONVERT_TZ(start_at, '+00:00', '+05:30'));

-- 4D: Apply NOT NULL after backfill is complete
ALTER TABLE `SHOW`
    MODIFY COLUMN screen_id BIGINT NOT NULL;

ALTER TABLE `SHOW`
    MODIFY COLUMN show_date DATE NOT NULL;

-- 4E: Apply FK constraint (screen_id → SCREEN)
ALTER TABLE `SHOW`
    ADD CONSTRAINT fk_show_screen
        FOREIGN KEY (screen_id) REFERENCES SCREEN (screen_id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- 4F: Apply UNIQUE(screen_id, start_at)
--     Guarantees no two SHOW rows share the same physical screen at the same exact UTC instant.
--     Applied after backfill so existing data is validated during constraint creation.
ALTER TABLE `SHOW`
    ADD CONSTRAINT uq_show_screen_start UNIQUE (screen_id, start_at);

-- 4G: Performance index for screen + exhibition date queries
--     Supports: "Find all shows for screen S on date d"
--     Required by Part 3 scheduler inner loop and availability APIs.
CREATE INDEX idx_show_screen_date ON `SHOW` (screen_id, show_date);

-- =============================================================================
-- SECTION 5: DATASET_CONFIG — Update metadata to reflect V10 schema state
-- =============================================================================
-- The existing V9 config describes the V5 physical data (125 screens / 15,000 seats).
-- V10 adds schema-level columns only. Physical data is still 125 screens / 15,000 seats.
-- Dataset phase is advanced to '1A_PENDING' to indicate schema is ready but
-- physical realism data has not yet been loaded (that is V11's responsibility).
-- =============================================================================

INSERT INTO DATASET_CONFIG (config_key, config_value, description) VALUES
('dataset.schema_version',   'V10',          'Flyway schema version that introduced Part 1A/2/3 column prerequisites'),
('dataset.phase',            '1A_PENDING',   'Schema ready for Part 1A data; physical data (127 screens/16386 seats) loaded in V11'),
('dataset.v10_applied_at',   NOW(),          'Timestamp when V10 schema extensions were applied')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description  = VALUES(description);

-- =============================================================================
-- END V10 — Schema Extensions
-- =============================================================================
