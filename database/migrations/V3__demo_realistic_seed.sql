-- =============================================================================
-- PVK CINEMAS — V3__demo_realistic_seed.sql
-- Demo-Quality Realistic Seed Data
-- Conforms strictly to the locked 28-table database schema
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- CLEANUP PREVIOUS DEMO & SYNTHETIC TEST FIXTURES (Safe, Idempotent, Controlled)
-- Removes synthetic test records while leaving reference data untouched
-- -----------------------------------------------------------------------------

CREATE TEMPORARY TABLE IF NOT EXISTS _ttid AS
SELECT theatre_id FROM THEATRE
WHERE theatre_code NOT LIKE 'DEMO_%'
   OR theatre_code LIKE 'DEMO_%';

CREATE TEMPORARY TABLE IF NOT EXISTS _tsid AS
SELECT screen_id FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM _ttid);

CREATE TEMPORARY TABLE IF NOT EXISTS _tscpid AS
SELECT screen_capability_id FROM SCREEN_CAPABILITY WHERE screen_id IN (SELECT screen_id FROM _tsid);

CREATE TEMPORARY TABLE IF NOT EXISTS _tshid AS
SELECT show_id FROM `SHOW` WHERE screen_capability_id IN (SELECT screen_capability_id FROM _tscpid);

DELETE FROM SHOW_SEAT WHERE show_id IN (SELECT show_id FROM _tshid);
DELETE FROM `SHOW` WHERE show_id IN (SELECT show_id FROM _tshid);
DELETE FROM SEAT WHERE screen_id IN (SELECT screen_id FROM _tsid);
DELETE FROM SCREEN_CAPABILITY WHERE screen_id IN (SELECT screen_id FROM _tsid);
DELETE FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM _ttid);
DELETE FROM EMPLOYEE_THEATRE WHERE theatre_id IN (SELECT theatre_id FROM _ttid);
DELETE FROM THEATRE WHERE theatre_id IN (SELECT theatre_id FROM _ttid);

DROP TEMPORARY TABLE IF EXISTS _tshid;
DROP TEMPORARY TABLE IF EXISTS _tscpid;
DROP TEMPORARY TABLE IF EXISTS _tsid;
DROP TEMPORARY TABLE IF EXISTS _ttid;

CREATE TEMPORARY TABLE IF NOT EXISTS _tmid AS
SELECT movie_id FROM MOVIE;

CREATE TEMPORARY TABLE IF NOT EXISTS _tmlid AS
SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id IN (SELECT movie_id FROM _tmid);

CREATE TEMPORARY TABLE IF NOT EXISTS _tmshid AS
SELECT show_id FROM `SHOW` WHERE movie_language_id IN (SELECT movie_language_id FROM _tmlid);

DELETE FROM SHOW_SEAT WHERE show_id IN (SELECT show_id FROM _tmshid);
DELETE FROM `SHOW` WHERE show_id IN (SELECT show_id FROM _tmshid);
DELETE FROM MOVIE_GENRE WHERE movie_id IN (SELECT movie_id FROM _tmid);
DELETE FROM MOVIE_LANGUAGE WHERE movie_id IN (SELECT movie_id FROM _tmid);
DELETE FROM MOVIE WHERE movie_id IN (SELECT movie_id FROM _tmid);

DROP TEMPORARY TABLE IF EXISTS _tmshid;
DROP TEMPORARY TABLE IF EXISTS _tmlid;
DROP TEMPORARY TABLE IF EXISTS _tmid;

CREATE TEMPORARY TABLE IF NOT EXISTS _tuid AS
SELECT user_id FROM USER
WHERE email LIKE '%@test%'
   OR email LIKE '%@example.com'
   OR email IN ('admin@pvkcinemas.com','manager@pvkcinemas.com','customer@pvkcinemas.com');

DELETE FROM EMPLOYEE_THEATRE WHERE user_id IN (SELECT user_id FROM _tuid);
DELETE FROM USER_ROLE WHERE user_id IN (SELECT user_id FROM _tuid);
DELETE FROM EMPLOYEE_PROFILE WHERE user_id IN (SELECT user_id FROM _tuid);
DELETE FROM CUSTOMER_PROFILE WHERE user_id IN (SELECT user_id FROM _tuid);
DELETE FROM USER WHERE user_id IN (SELECT user_id FROM _tuid);

DROP TEMPORARY TABLE IF EXISTS _tuid;

DELETE FROM SEARCH_RESULT WHERE 1=1;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- SECTION 1: DEMO USER ACCOUNTS (BCrypt of 'Password123!')
-- =============================================================================
INSERT INTO USER (first_name, last_name, email, phone, password_hash, account_status) VALUES
('Arjun',  'Sharma',   'admin@pvkcinemas.com',    '+91-9800000001', '$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be', 'ACTIVE'),
('Priya',  'Krishnan', 'manager@pvkcinemas.com',  '+91-9800000002', '$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be', 'ACTIVE'),
('Kavya',  'Reddy',    'customer@pvkcinemas.com', '+91-9800000003', '$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be', 'ACTIVE');

-- Super Admin Role & Profile
INSERT INTO USER_ROLE (user_id, role_id, status)
SELECT u.user_id, r.role_id, 'ACTIVE'
FROM USER u, ROLE r
WHERE u.email = 'admin@pvkcinemas.com' AND r.role_code = 'ROLE_SUPER_ADMIN';

INSERT INTO EMPLOYEE_PROFILE (user_id, employee_code, joining_date, status)
SELECT u.user_id, 'EMP-ADMIN-001', '2024-01-15', 'ACTIVE'
FROM USER u WHERE u.email = 'admin@pvkcinemas.com';

-- Theatre Manager Role & Profile
INSERT INTO USER_ROLE (user_id, role_id, status)
SELECT u.user_id, r.role_id, 'ACTIVE'
FROM USER u, ROLE r
WHERE u.email = 'manager@pvkcinemas.com' AND r.role_code = 'ROLE_THEATRE_MANAGER';

INSERT INTO EMPLOYEE_PROFILE (user_id, employee_code, joining_date, status)
SELECT u.user_id, 'EMP-MGR-001', '2024-03-01', 'ACTIVE'
FROM USER u WHERE u.email = 'manager@pvkcinemas.com';

-- Customer Role & Profile
INSERT INTO USER_ROLE (user_id, role_id, status)
SELECT u.user_id, r.role_id, 'ACTIVE'
FROM USER u, ROLE r
WHERE u.email = 'customer@pvkcinemas.com' AND r.role_code = 'ROLE_CUSTOMER';

INSERT INTO CUSTOMER_PROFILE (user_id, preferred_language_id, date_of_birth)
SELECT u.user_id, l.language_id, '1998-07-22'
FROM USER u, LANGUAGE l
WHERE u.email = 'customer@pvkcinemas.com' AND l.language_code = 'ta';

-- =============================================================================
-- SECTION 2: THEATRES (6 Realistic Multiplexes across 4 Cities)
-- =============================================================================
INSERT INTO THEATRE (city_id, theatre_code, theatre_name, address_line_1, address_line_2, postal_code, latitude, longitude, status) VALUES
((SELECT city_id FROM CITY WHERE city_name='Chennai'),    'DEMO_PVK_INOX_CHN', 'PVK INOX Luxe Chennai',       'Express Avenue Mall, Whites Road, Royapettah', 'Level 3, Theatre Block', '600014', 13.064220, 80.268080, 'ACTIVE'),
((SELECT city_id FROM CITY WHERE city_name='Chennai'),    'DEMO_PVK_SPI_CHN',  'PVK SPI Cinemas Phoenix',     'Phoenix MarketCity, Velachery Main Road',       'Velachery, Chennai',     '600042', 12.980310, 80.218780, 'ACTIVE'),
((SELECT city_id FROM CITY WHERE city_name='Bengaluru'),  'DEMO_PVK_PVR_BLR',  'PVK PVR Forum Koramangala',   'Forum Mall, Hosur Road, Koramangala',           '4th Block, Koramangala', '560034', 12.935010, 77.616850, 'ACTIVE'),
((SELECT city_id FROM CITY WHERE city_name='Bengaluru'),  'DEMO_PVK_INOX_BLR', 'PVK INOX Garuda Bengaluru',   'Garuda Mall, Magrath Road, Ashok Nagar',        'Near Residency Road',    '560025', 12.972910, 77.603550, 'ACTIVE'),
((SELECT city_id FROM CITY WHERE city_name='Hyderabad'),  'DEMO_PVK_AMB_HYD',  'PVK Cineplex Hyderabad',      'Sarath City Capital Mall, Kondapur',           'Hi-Tech City, Madhapur', '500081', 17.450210, 78.373610, 'ACTIVE'),
((SELECT city_id FROM CITY WHERE city_name='Coimbatore'), 'DEMO_PVK_CBE',      'PVK Cineplex Coimbatore',     'Brookefields Mall, Race Course Road',           'Coimbatore',             '641005', 11.007790, 76.971090, 'ACTIVE');

-- Assign Theatre Manager to DEMO_PVK_INOX_CHN
INSERT INTO EMPLOYEE_THEATRE (user_id, theatre_id, status)
SELECT u.user_id, t.theatre_id, 'ACTIVE'
FROM USER u, THEATRE t
WHERE u.email = 'manager@pvkcinemas.com' AND t.theatre_code = 'DEMO_PVK_INOX_CHN';

-- =============================================================================
-- SECTION 3: SCREENS (13 Auditoriums across 6 Multiplexes)
-- =============================================================================
INSERT INTO SCREEN (theatre_id, screen_code, screen_name, status) VALUES
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_INOX_CHN'), 'D_CHN1_IMAX', 'Screen 1 — IMAX Atmos',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_INOX_CHN'), 'D_CHN1_4DX',  'Screen 2 — 4DX Motion',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_INOX_CHN'), 'D_CHN1_2D',   'Screen 3 — Standard 2D',    'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_SPI_CHN'),  'D_CHN2_3D',   'Screen 1 — Digital 3D',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_SPI_CHN'),  'D_CHN2_2D',   'Screen 2 — Standard 2D',    'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_PVR_BLR'),  'D_BLR1_IMAX', 'Screen 1 — IMAX Atmos',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_PVR_BLR'),  'D_BLR1_2D',   'Screen 2 — 2D Premium',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_INOX_BLR'), 'D_BLR2_4DX',  'Screen 1 — 4DX Experience', 'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_INOX_BLR'), 'D_BLR2_2D',   'Screen 2 — Standard 2D',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_AMB_HYD'),  'D_HYD1_IMAX', 'Screen 1 — IMAX Premium',   'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_AMB_HYD'),  'D_HYD1_2D',   'Screen 2 — Standard 2D',    'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_CBE'),      'D_CBE1_3D',   'Screen 1 — Digital 3D',     'ACTIVE'),
((SELECT theatre_id FROM THEATRE WHERE theatre_code='DEMO_PVK_CBE'),      'D_CBE1_2D',   'Screen 2 — Standard 2D',     'ACTIVE');

-- SCREEN CAPABILITIES (Links Screen + Presentation Format + Audio Format)
INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code IN ('D_CHN1_IMAX','D_BLR1_IMAX','D_HYD1_IMAX')
  AND pf.format_code='IMAX' AND af.format_code='DOLBY_ATMOS';

INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code IN ('D_CHN1_4DX','D_BLR2_4DX')
  AND pf.format_code='4DX' AND af.format_code='DOLBY_7_1';

INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code IN ('D_CHN2_3D','D_CBE1_3D')
  AND pf.format_code='3D' AND af.format_code='DOLBY_5_1';

INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code IN ('D_CHN1_2D','D_CHN2_2D','D_BLR1_2D','D_BLR2_2D','D_HYD1_2D','D_CBE1_2D')
  AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- =============================================================================
-- SECTION 4: SEATS (122 Seats Per Auditorium = 1,586 Realistic Seats)
-- Standard: Rows A-F (12/row = 72)
-- Premium:  Rows G-J (10/row = 40)
-- Recliner: Row K (1-8 = 8)
-- Accessible: Row K (9-10 = 2)
-- =============================================================================
INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, status)
SELECT sc.screen_id,
       (SELECT seat_type_id FROM SEAT_TYPE WHERE type_code='STANDARD'),
       r.rl,
       CAST(n.num AS CHAR),
       'ACTIVE'
FROM SCREEN sc
CROSS JOIN (SELECT 'A' AS rl UNION SELECT 'B' UNION SELECT 'C' UNION SELECT 'D' UNION SELECT 'E' UNION SELECT 'F') r
CROSS JOIN (SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
            UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) n
WHERE sc.screen_code IN ('D_CHN1_IMAX','D_CHN1_4DX','D_CHN1_2D','D_CHN2_3D','D_CHN2_2D','D_BLR1_IMAX',
                         'D_BLR1_2D','D_BLR2_4DX','D_BLR2_2D','D_HYD1_IMAX','D_HYD1_2D','D_CBE1_3D','D_CBE1_2D');

INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, status)
SELECT sc.screen_id,
       (SELECT seat_type_id FROM SEAT_TYPE WHERE type_code='PREMIUM'),
       r.rl,
       CAST(n.num AS CHAR),
       'ACTIVE'
FROM SCREEN sc
CROSS JOIN (SELECT 'G' AS rl UNION SELECT 'H' UNION SELECT 'I' UNION SELECT 'J') r
CROSS JOIN (SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
            UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) n
WHERE sc.screen_code IN ('D_CHN1_IMAX','D_CHN1_4DX','D_CHN1_2D','D_CHN2_3D','D_CHN2_2D','D_BLR1_IMAX',
                         'D_BLR1_2D','D_BLR2_4DX','D_BLR2_2D','D_HYD1_IMAX','D_HYD1_2D','D_CBE1_3D','D_CBE1_2D');

INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, status)
SELECT sc.screen_id,
       (SELECT seat_type_id FROM SEAT_TYPE WHERE type_code='RECLINER'),
       'K',
       CAST(n.num AS CHAR),
       'ACTIVE'
FROM SCREEN sc
CROSS JOIN (SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
            UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) n
WHERE sc.screen_code IN ('D_CHN1_IMAX','D_CHN1_4DX','D_CHN1_2D','D_CHN2_3D','D_CHN2_2D','D_BLR1_IMAX',
                         'D_BLR1_2D','D_BLR2_4DX','D_BLR2_2D','D_HYD1_IMAX','D_HYD1_2D','D_CBE1_3D','D_CBE1_2D');

INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, status)
SELECT sc.screen_id,
       (SELECT seat_type_id FROM SEAT_TYPE WHERE type_code='ACCESSIBLE'),
       'K',
       CAST(n.num AS CHAR),
       'ACTIVE'
FROM SCREEN sc
CROSS JOIN (SELECT 9 AS num UNION SELECT 10) n
WHERE sc.screen_code IN ('D_CHN1_IMAX','D_CHN1_4DX','D_CHN1_2D','D_CHN2_3D','D_CHN2_2D','D_BLR1_IMAX',
                         'D_BLR1_2D','D_BLR2_4DX','D_BLR2_2D','D_HYD1_IMAX','D_HYD1_2D','D_CBE1_3D','D_CBE1_2D');

-- =============================================================================
-- SECTION 5: MOVIES (10 Blockbuster Titles with TMDB Poster URLs)
-- =============================================================================
INSERT INTO MOVIE (certification_id, title, original_title, synopsis, runtime_minutes, release_date, status, poster_url, trailer_url) VALUES
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'Interstellar','Interstellar','When Earth becomes uninhabitable, astronaut Cooper leads a desperate mission through a wormhole near Saturn to find humanity new home. Leaving his daughter behind, he journeys through black holes and time dilation, discovering that love itself may be the key to saving the species.',169,'2014-11-07','AIRING','https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIE.jpg','https://www.youtube.com/watch?v=zSWdZVtXT7E'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'Avengers: Endgame','Avengers: Endgame','After Thanos catastrophic Snap erases half of all life, the surviving Avengers devise a desperate time-heist to reverse the decimation. The final battle brings Earth mightiest heroes together for cinema greatest superhero spectacle.',181,'2019-04-26','AIRING','https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg','https://www.youtube.com/watch?v=TcMBFSGVi1c'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'Inception','Inception','Dom Cobb is a master thief who infiltrates the subconscious to steal secrets from dreams. Tasked with planting an idea — inception — into a target mind, he assembles a crew to dive through layered dream realities in Christopher Nolan labyrinthine masterpiece.',148,'2010-07-16','AIRING','https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg','https://www.youtube.com/watch?v=YoHD9XEInc0'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'The Dark Knight','The Dark Knight','Gotham City faces its gravest threat as the anarchic Joker rises to sow chaos. Batman must confront the limits of his own morality. Heath Ledger iconic Oscar-winning performance defines one of cinema greatest villain portrayals.',152,'2008-07-18','AIRING','https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg','https://www.youtube.com/watch?v=kmJLuwP3MbY'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'RRR','RRR — Rise Roar Revolt','An epic fictional saga of two legendary Indian revolutionaries — Alluri Sitarama Raju and Komaram Bheem — who forge an unexpected friendship before their destinies diverge. A visual marvel of scale, emotion, and action by S.S. Rajamouli.',187,'2022-03-25','AIRING','https://image.tmdb.org/t/p/w500/nEufeZlyAOLqO6larF8kGaTbGOy.jpg','https://www.youtube.com/watch?v=f_vbAtFSEc0'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='A'),'KGF: Chapter 2','Kolar Gold Fields: Chapter 2','Rocky reigns supreme over the Kolar Gold Fields after defeating Garuda. But the combined might of Adheera army and government Operation Rocky puts him on a collision course with destiny in Prashanth Neel thundering sequel.',168,'2022-04-14','AIRING','https://image.tmdb.org/t/p/w500/4tFHUDQPAkEd1dIF1vkA49ej7t8.jpg','https://www.youtube.com/watch?v=McjLhH-f9oE'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='A'),'Pushpa 2: The Rule','Pushpa 2: The Rule','Pushpa Raj expands his red sandalwood empire to unprecedented scale. As his legend grows, SP Bhanwar Singh Shekhawat vendetta intensifies. The conclusion of a saga that redefined mass cinema — louder, fiercer, and more relentless than its predecessor.',220,'2024-12-05','AIRING','https://image.tmdb.org/t/p/w500/soHLOxQxdEwl0cNRWr9h4Oaiz3Q.jpg','https://www.youtube.com/watch?v=mrnVEd-CUU0'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'Kalki 2898 AD','Kalki 2898 AD','In a dystopian 29th-century world, ancient Vedic prophecy resurfaces through a reluctant bounty hunter named Bhairava. A grand mythological sci-fi spectacle blending legend with futuristic warfare on an unprecedented canvas, directed by Nag Ashwin.',181,'2024-06-27','AIRING','https://image.tmdb.org/t/p/w500/eKRmEy0JhBj8Q6iKb1D2nfNjHjR.jpg','https://www.youtube.com/watch?v=k37-oO4jT-A'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'Dune: Part Two','Dune: Part Two','Paul Atreides unites with the Fremen of Arrakis to wage war against House Harkonnen. Forced to choose between love and the fate of the universe, he embraces a destiny he fears most in Denis Villeneuve monumental sequel.',166,'2024-03-01','AIRING','https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg','https://www.youtube.com/watch?v=Way9Dexny3w'),
((SELECT certification_id FROM CERTIFICATION WHERE certification_code='UA'),'Oppenheimer','Oppenheimer','The story of J. Robert Oppenheimer and his pivotal role in the Manhattan Project. Through Christopher Nolan searing non-linear narrative, we witness the devastating moral weight of scientific achievement that forever altered human history.',180,'2023-07-21','AIRING','https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg','https://www.youtube.com/watch?v=uYPbbksJxIg');

-- MOVIE GENRES
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Interstellar' AND g.genre_code IN ('SCI_FI','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Avengers: Endgame' AND g.genre_code IN ('ACTION','SCI_FI');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Inception' AND g.genre_code IN ('THRILLER','SCI_FI');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='The Dark Knight' AND g.genre_code IN ('ACTION','THRILLER');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='RRR' AND g.genre_code IN ('ACTION','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='KGF: Chapter 2' AND g.genre_code IN ('ACTION','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Pushpa 2: The Rule' AND g.genre_code IN ('ACTION','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Kalki 2898 AD' AND g.genre_code IN ('SCI_FI','ACTION');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Dune: Part Two' AND g.genre_code IN ('SCI_FI','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Oppenheimer' AND g.genre_code IN ('DRAMA','THRILLER');

-- MOVIE LANGUAGES
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'ORIGINAL'
FROM MOVIE m, LANGUAGE l
WHERE m.title IN ('Interstellar','Avengers: Endgame','Inception','The Dark Knight','Dune: Part Two','Oppenheimer') AND l.language_code='en';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'DUBBED'
FROM MOVIE m, LANGUAGE l
WHERE m.title IN ('Interstellar','Avengers: Endgame','Inception','Dune: Part Two','Oppenheimer') AND l.language_code='hi';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'DUBBED'
FROM MOVIE m, LANGUAGE l
WHERE m.title IN ('Avengers: Endgame') AND l.language_code='ta';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'ORIGINAL'
FROM MOVIE m, LANGUAGE l
WHERE m.title='RRR' AND l.language_code='te';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'DUBBED'
FROM MOVIE m, LANGUAGE l
WHERE m.title='RRR' AND l.language_code IN ('hi','ta');

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'ORIGINAL'
FROM MOVIE m, LANGUAGE l
WHERE m.title='KGF: Chapter 2' AND l.language_code='kn';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'DUBBED'
FROM MOVIE m, LANGUAGE l
WHERE m.title='KGF: Chapter 2' AND l.language_code='hi';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'ORIGINAL'
FROM MOVIE m, LANGUAGE l
WHERE m.title='Pushpa 2: The Rule' AND l.language_code='te';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'DUBBED'
FROM MOVIE m, LANGUAGE l
WHERE m.title='Pushpa 2: The Rule' AND l.language_code IN ('hi','ta');

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'ORIGINAL'
FROM MOVIE m, LANGUAGE l
WHERE m.title='Kalki 2898 AD' AND l.language_code='te';

INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role)
SELECT m.movie_id, l.language_id, 'DUBBED'
FROM MOVIE m, LANGUAGE l
WHERE m.title='Kalki 2898 AD' AND l.language_code IN ('hi','ta');

-- =============================================================================
-- SECTION 6: SHOWS (Locked schema: movie_language_id, screen_capability_id)
-- Multi-day schedules with ZERO overlap guaranteed by non-overlapping time windows
-- =============================================================================

-- 1. Chennai INOX IMAX: Interstellar (English Original, IMAX Atmos)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Interstellar'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='en'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:00:00' AS st UNION SELECT '14:00:00' UNION SELECT '18:00:00' UNION SELECT '22:00:00') t
WHERE sc.screen_code='D_CHN1_IMAX' AND pf.format_code='IMAX' AND af.format_code='DOLBY_ATMOS';

-- 2. Chennai INOX 4DX: Avengers Endgame (English Original, 4DX Dolby 7.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Avengers: Endgame'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='en'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '09:30:00' AS st UNION SELECT '13:45:00' UNION SELECT '18:00:00' UNION SELECT '22:15:00') t
WHERE sc.screen_code='D_CHN1_4DX' AND pf.format_code='4DX' AND af.format_code='DOLBY_7_1';

-- 3. Chennai INOX 2D: RRR (Tamil Dubbed, 2D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='RRR'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='DUBBED'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='ta'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:00:00' AS st UNION SELECT '14:30:00' UNION SELECT '19:00:00') t
WHERE sc.screen_code='D_CHN1_2D' AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- 4. Chennai SPI 3D: Inception (English Original, 3D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Inception'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='en'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:30:00' AS st UNION SELECT '14:15:00' UNION SELECT '18:00:00' UNION SELECT '21:45:00') t
WHERE sc.screen_code='D_CHN2_3D' AND pf.format_code='3D' AND af.format_code='DOLBY_5_1';

-- 5. Chennai SPI 2D: KGF Chapter 2 (Hindi Dubbed, 2D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='KGF: Chapter 2'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='DUBBED'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='hi'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:15:00' AS st UNION SELECT '14:15:00' UNION SELECT '18:15:00' UNION SELECT '22:15:00') t
WHERE sc.screen_code='D_CHN2_2D' AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- 6. Bengaluru PVR IMAX: Oppenheimer (English Original, IMAX Atmos)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Oppenheimer'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='en'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '09:45:00' AS st UNION SELECT '14:00:00' UNION SELECT '18:15:00' UNION SELECT '22:30:00') t
WHERE sc.screen_code='D_BLR1_IMAX' AND pf.format_code='IMAX' AND af.format_code='DOLBY_ATMOS';

-- 7. Bengaluru PVR 2D: Dune Part Two (English Original, 2D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Dune: Part Two'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='en'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:00:00' AS st UNION SELECT '14:00:00' UNION SELECT '18:00:00' UNION SELECT '22:00:00') t
WHERE sc.screen_code='D_BLR1_2D' AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- 8. Bengaluru INOX 4DX: Kalki 2898 AD (Telugu Original, 4DX Dolby 7.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Kalki 2898 AD'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='te'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '09:30:00' AS st UNION SELECT '13:45:00' UNION SELECT '18:00:00' UNION SELECT '22:15:00') t
WHERE sc.screen_code='D_BLR2_4DX' AND pf.format_code='4DX' AND af.format_code='DOLBY_7_1';

-- 9. Bengaluru INOX 2D: Pushpa 2 (Telugu Original, 2D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Pushpa 2: The Rule'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='te'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '09:30:00' AS st UNION SELECT '14:15:00' UNION SELECT '19:00:00') t
WHERE sc.screen_code='D_BLR2_2D' AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- 10. Hyderabad IMAX: The Dark Knight (English Original, IMAX Atmos)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='The Dark Knight'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='ORIGINAL'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='en'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:00:00' AS st UNION SELECT '14:00:00' UNION SELECT '18:00:00' UNION SELECT '22:00:00') t
WHERE sc.screen_code='D_HYD1_IMAX' AND pf.format_code='IMAX' AND af.format_code='DOLBY_ATMOS';

-- 11. Hyderabad 2D: RRR (Hindi Dubbed, 2D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='RRR'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='DUBBED'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='hi'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '10:00:00' AS st UNION SELECT '14:30:00' UNION SELECT '19:00:00') t
WHERE sc.screen_code='D_HYD1_2D' AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- 12. Coimbatore 3D: Kalki 2898 AD (Tamil Dubbed, 3D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Kalki 2898 AD'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='DUBBED'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='ta'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '09:30:00' AS st UNION SELECT '13:45:00' UNION SELECT '18:00:00' UNION SELECT '22:15:00') t
WHERE sc.screen_code='D_CBE1_3D' AND pf.format_code='3D' AND af.format_code='DOLBY_5_1';

-- 13. Coimbatore 2D: Pushpa 2 (Tamil Dubbed, 2D Dolby 5.1)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st),
       DATE_ADD(CONCAT(DATE_ADD(CURDATE(), INTERVAL d.days DAY),' ',t.st), INTERVAL (m.runtime_minutes+30) MINUTE),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN PRESENTATION_FORMAT pf ON scp.presentation_format_id = pf.presentation_format_id
JOIN AUDIO_FORMAT af ON scp.audio_format_id = af.audio_format_id
JOIN MOVIE m ON m.title='Pushpa 2: The Rule'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id=m.movie_id AND ml.language_role='DUBBED'
JOIN LANGUAGE l ON ml.language_id=l.language_id AND l.language_code='ta'
CROSS JOIN (SELECT 0 AS days UNION SELECT 1 UNION SELECT 2) d
CROSS JOIN (SELECT '09:30:00' AS st UNION SELECT '14:15:00' UNION SELECT '19:00:00') t
WHERE sc.screen_code='D_CBE1_2D' AND pf.format_code='2D' AND af.format_code='DOLBY_5_1';

-- =============================================================================
-- SECTION 7: SHOW_SEAT AVAILABILITY (~65% AVAILABLE, ~25% BOOKED, ~10% BLOCKED)
-- Correlates exact physical seats of the auditorium with each scheduled show
-- =============================================================================
INSERT INTO SHOW_SEAT (show_id, seat_id, availability_status)
SELECT sh.show_id, s.seat_id,
       CASE WHEN MOD(s.seat_id + sh.show_id, 10) < 2 THEN 'BLOCKED'
            WHEN MOD(s.seat_id + sh.show_id, 10) < 5 THEN 'BOOKED'
            ELSE 'AVAILABLE' END
FROM `SHOW` sh
JOIN SCREEN_CAPABILITY scp ON sh.screen_capability_id = scp.screen_capability_id
JOIN SCREEN sc ON scp.screen_id = sc.screen_id
JOIN THEATRE t ON sc.theatre_id = t.theatre_id
JOIN SEAT s ON s.screen_id = sc.screen_id
WHERE t.theatre_code LIKE 'DEMO_%';

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- END V3 DEMO SEED: 3 ACCOUNTS | 6 THEATRES | 13 SCREENS | 1,586 SEATS | 10 MOVIES | 144 SHOWS
-- All Demo Accounts Password: Password123!
-- =============================================================================
