-- =============================================================================
-- PVK CINEMAS — FLYWAY MIGRATION V6: SEAT GEOMETRY, ZONES & PRICING ENHANCEMENT
-- =============================================================================

-- 1. Extend SEAT table with pricing_zone and aisle_after columns
ALTER TABLE SEAT ADD COLUMN pricing_zone VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE SEAT ADD COLUMN aisle_after BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_seat_zone ON SEAT (pricing_zone);

-- 2. Extend THEATRE table with venue price variation offset
ALTER TABLE THEATRE ADD COLUMN base_price_offset DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- 3. Populate realistic pricing zones across all 15,000 physical seats
-- Rows A-C: Front screen rows -> VALUE zone
UPDATE SEAT SET pricing_zone = 'VALUE' WHERE row_label IN ('A', 'B', 'C');

-- Rows D-F: Standard viewing rows -> STANDARD zone
UPDATE SEAT SET pricing_zone = 'STANDARD' WHERE row_label IN ('D', 'E', 'F');

-- Rows G-J: Prime visual & acoustic sweet spot -> PREMIUM zone
UPDATE SEAT SET pricing_zone = 'PREMIUM' WHERE row_label IN ('G', 'H', 'I', 'J');

-- Rows K-L: Luxury recliner and accessible rear rows -> PREMIUM zone
UPDATE SEAT SET pricing_zone = 'PREMIUM' WHERE row_label IN ('K', 'L');

-- 4. Populate realistic physical aisle boundaries
-- In 10-seat rows: Aisle after seat 3 (left bank: 1-3, center bank: 4-7)
-- and Aisle after seat 7 (right bank: 8-10)
UPDATE SEAT SET aisle_after = TRUE WHERE seat_number = '3';
UPDATE SEAT SET aisle_after = TRUE WHERE seat_number = '7' AND row_label != 'L';

-- In 12-seat row L: Aisle after seat 3 and seat 9
UPDATE SEAT SET aisle_after = TRUE WHERE seat_number = '9' AND row_label = 'L';

-- 5. Introduce realistic venue-to-venue base price variation across 25 multiplexes
UPDATE THEATRE SET base_price_offset = -10.00 WHERE MOD(theatre_id, 5) = 1;
UPDATE THEATRE SET base_price_offset = 0.00   WHERE MOD(theatre_id, 5) = 2;
UPDATE THEATRE SET base_price_offset = 10.00  WHERE MOD(theatre_id, 5) = 3;
UPDATE THEATRE SET base_price_offset = 20.00  WHERE MOD(theatre_id, 5) = 4;
UPDATE THEATRE SET base_price_offset = 30.00  WHERE MOD(theatre_id, 5) = 0;

-- 6. Audit and repair all 11 broken movie poster URLs with verified 200 OK TMDB assets
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/yQvGrMoipbRoddT0ZR8tPoR7NfX.jpg' WHERE movie_id = 1;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/idT5mnqPcJgSkvpDX7pJffBzdVH.jpg' WHERE movie_id = 6;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/5qHoazZiaLe7oFBok7XlUhg96f2.jpg' WHERE movie_id = 7;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/7oZA31r9NzVGBZOKs5qmKwZyraZ.jpg' WHERE movie_id = 8;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/eyr8okTqsFTUVW3DD1edTMsWc3u.jpg' WHERE movie_id = 9;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/tjpiEnZBUAA8pdNPRKa5vP2Zpqw.jpg' WHERE movie_id = 10;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/jFt1gS4BGHlK8xt76Y81Alp4dbt.jpg' WHERE movie_id = 11;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/51tqzRtKMMZEYUpSYkrUE7v9ehm.jpg' WHERE movie_id = 12;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/1F5BPNbhxaWAA83YTnPjcswt7Nc.jpg' WHERE movie_id = 13;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/n726fdyL1dGwt15bY7Nj3XOXc4Q.jpg' WHERE movie_id = 14;
UPDATE MOVIE SET poster_url = 'https://image.tmdb.org/t/p/w500/6I7zs05TdLXP5hCbFoRc2REGqfC.jpg' WHERE movie_id = 15;
