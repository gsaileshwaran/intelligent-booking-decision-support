-- =============================================================================
-- PVK CINEMAS — V3__demo_realistic_seed.sql
-- Authoritative Demo-Quality Deterministic Seed Data
-- Conforms strictly to the locked 28-table database schema
-- 5 Cities | 25 Theatres | 250 Screens | 12,500 Seats | 10 Movies | 1,250 Shows
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- PURGE PREVIOUS DEMO & SYNTHETIC TEST FIXTURES
-- -----------------------------------------------------------------------------
DELETE FROM SHOW_SEAT WHERE 1=1;
DELETE FROM `SHOW` WHERE 1=1;
DELETE FROM SEAT WHERE 1=1;
DELETE FROM SCREEN_CAPABILITY WHERE 1=1;
DELETE FROM SCREEN WHERE 1=1;
DELETE FROM EMPLOYEE_THEATRE WHERE 1=1;
DELETE FROM THEATRE WHERE 1=1;
DELETE FROM CITY WHERE 1=1;
ALTER TABLE CITY AUTO_INCREMENT = 1;

DELETE FROM MOVIE_GENRE WHERE 1=1;
DELETE FROM MOVIE_LANGUAGE WHERE 1=1;
DELETE FROM MOVIE WHERE 1=1;

DELETE FROM SEARCH_RESULT WHERE 1=1;
DELETE FROM SEARCH_QUERY WHERE 1=1;

DELETE FROM EMPLOYEE_PROFILE WHERE user_id IN (SELECT user_id FROM USER WHERE email LIKE '%@test%' OR email LIKE '%@example.com' OR email IN ('admin@pvkcinemas.com','manager@pvkcinemas.com','customer@pvkcinemas.com'));
DELETE FROM CUSTOMER_PROFILE WHERE user_id IN (SELECT user_id FROM USER WHERE email LIKE '%@test%' OR email LIKE '%@example.com' OR email IN ('admin@pvkcinemas.com','manager@pvkcinemas.com','customer@pvkcinemas.com'));
DELETE FROM USER_ROLE WHERE user_id IN (SELECT user_id FROM USER WHERE email LIKE '%@test%' OR email LIKE '%@example.com' OR email IN ('admin@pvkcinemas.com','manager@pvkcinemas.com','customer@pvkcinemas.com'));
DELETE FROM USER WHERE email LIKE '%@test%' OR email LIKE '%@example.com' OR email IN ('admin@pvkcinemas.com','manager@pvkcinemas.com','customer@pvkcinemas.com');

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- SECTION 1: CANONICAL OPERATIONAL CITIES (Exactly 5)
-- =============================================================================
INSERT INTO CITY (city_id, city_name, state_name, country_code) VALUES
(1, 'Chennai',   'Tamil Nadu',   'IN'),
(2, 'Bengaluru', 'Karnataka',    'IN'),
(3, 'Hyderabad', 'Telangana',    'IN'),
(4, 'Mumbai',    'Maharashtra',  'IN'),
(5, 'Delhi',     'Delhi',        'IN');

-- =============================================================================
-- SECTION 2: DEMO USER ACCOUNTS (Password: Password123!)
-- =============================================================================
INSERT INTO USER (first_name, last_name, email, phone, password_hash, account_status) VALUES
('Arun',  'Sharma',   'admin@pvkcinemas.com',    '+91-9800000001', '$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be', 'ACTIVE'),
('Ravi',  'Kumar',    'manager@pvkcinemas.com',  '+91-9800000002', '$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be', 'ACTIVE'),
('Kavya',  'Reddy',    'customer@pvkcinemas.com', '+91-9800000003', '$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be', 'ACTIVE');

INSERT INTO USER_ROLE (user_id, role_id, status)
SELECT u.user_id, r.role_id, 'ACTIVE' FROM USER u, ROLE r WHERE u.email = 'admin@pvkcinemas.com' AND r.role_code = 'ROLE_SUPER_ADMIN';
INSERT INTO EMPLOYEE_PROFILE (user_id, employee_code, joining_date, status)
SELECT u.user_id, 'EMP-ADMIN-001', '2024-01-15', 'ACTIVE' FROM USER u WHERE u.email = 'admin@pvkcinemas.com';

INSERT INTO USER_ROLE (user_id, role_id, status)
SELECT u.user_id, r.role_id, 'ACTIVE' FROM USER u, ROLE r WHERE u.email = 'manager@pvkcinemas.com' AND r.role_code = 'ROLE_THEATRE_MANAGER';
INSERT INTO EMPLOYEE_PROFILE (user_id, employee_code, joining_date, status)
SELECT u.user_id, 'EMP-MGR-001', '2024-03-01', 'ACTIVE' FROM USER u WHERE u.email = 'manager@pvkcinemas.com';

INSERT INTO USER_ROLE (user_id, role_id, status)
SELECT u.user_id, r.role_id, 'ACTIVE' FROM USER u, ROLE r WHERE u.email = 'customer@pvkcinemas.com' AND r.role_code = 'ROLE_CUSTOMER';
INSERT INTO CUSTOMER_PROFILE (user_id, preferred_language_id, date_of_birth)
SELECT u.user_id, l.language_id, '1998-07-22' FROM USER u, LANGUAGE l WHERE u.email = 'customer@pvkcinemas.com' AND l.language_code = 'ta';

-- =============================================================================
-- SECTION 3: THEATRES (Exactly 5 Theatres per City = 25 Theatres Total)
-- =============================================================================
INSERT INTO THEATRE (city_id, theatre_code, theatre_name, address_line_1, address_line_2, postal_code, latitude, longitude, status) VALUES
(1, 'DEMO_CHN_CINEPLEX', 'PVK Cineplex Chennai', 'Express Avenue Mall, Whites Road, Royapettah', 'Level 3, Theatre Block', '600014', 13.064220, 80.268080, 'ACTIVE'),
(1, 'DEMO_CHN_INOX', 'PVK INOX Chennai', 'Phoenix MarketCity, Velachery Main Road', 'Velachery, Chennai', '600042', 12.980310, 80.218780, 'ACTIVE'),
(1, 'DEMO_CHN_PVR', 'PVK PVR Chennai', 'VR Chennai Mall, Inner Ring Road, Anna Nagar', 'Jawaharlal Nehru Road', '600040', 13.085430, 80.201240, 'ACTIVE'),
(1, 'DEMO_CHN_AGS', 'PVK AGS Chennai', 'AGS Cinemas, OMR Navalur', 'Rajiv Gandhi Salai', '603103', 12.845620, 80.228910, 'ACTIVE'),
(1, 'DEMO_CHN_SPI', 'PVK SPI Chennai', 'Sathyam Complex, Royapettah', '8 Thiruvika Road', '600014', 13.058320, 80.264170, 'ACTIVE'),
(2, 'DEMO_BLR_CINEPLEX', 'PVK Cineplex Bengaluru', 'Forum Mall, Hosur Road, Koramangala', '4th Block, Koramangala', '560034', 12.935010, 77.616850, 'ACTIVE'),
(2, 'DEMO_BLR_INOX', 'PVK INOX Bengaluru', 'Garuda Mall, Magrath Road, Ashok Nagar', 'Near Residency Road', '560025', 12.972910, 77.603550, 'ACTIVE'),
(2, 'DEMO_BLR_PVR', 'PVK PVR Bengaluru', 'Orion Mall, Dr Rajkumar Road, Rajajinagar', 'Brigade Gateway', '560055', 13.011240, 77.555230, 'ACTIVE'),
(2, 'DEMO_BLR_AGS', 'PVK AGS Bengaluru', 'Nexus Shantiniketan, Whitefield Main Road', 'Hoodi, Whitefield', '560048', 12.989210, 77.728910, 'ACTIVE'),
(2, 'DEMO_BLR_SPI', 'PVK SPI Bengaluru', 'Lulu Global Malls, Gopalapura, Binnypet', 'Magadi Road', '560023', 12.978930, 77.564120, 'ACTIVE'),
(3, 'DEMO_HYD_CINEPLEX', 'PVK Cineplex Hyderabad', 'Sarath City Capital Mall, Kondapur', 'Gachibowli-Miyapur Rd', '500084', 17.456810, 78.364210, 'ACTIVE'),
(3, 'DEMO_HYD_INOX', 'PVK INOX Hyderabad', 'GVK One Mall, Rd Number 1, Banjara Hills', 'Balapur Basheerbagh Rd', '500034', 17.419240, 78.448510, 'ACTIVE'),
(3, 'DEMO_HYD_PVR', 'PVK PVR Hyderabad', 'Next Galleria Mall, Hitec City', 'Madhapur', '500081', 17.449830, 78.381240, 'ACTIVE'),
(3, 'DEMO_HYD_AMB', 'PVK AMB Hyderabad', 'AMB Cinemas, Botanical Garden Road, Kondapur', 'Hi-Tech City', '500084', 17.450210, 78.373610, 'ACTIVE'),
(3, 'DEMO_HYD_PRASADS', 'PVK Prasads Hyderabad', 'Prasads Multiplex, NTR Marg, Central', 'Khairatabad', '500063', 17.413210, 78.468240, 'ACTIVE'),
(4, 'DEMO_BOM_CINEPLEX', 'PVK Cineplex Mumbai', 'Phoenix Palladium, Senapati Bapat Marg, Lower Parel', 'South Mumbai', '400013', 18.995320, 72.824810, 'ACTIVE'),
(4, 'DEMO_BOM_INOX', 'PVK INOX Mumbai', 'Inorbit Mall, Link Road, Malad West', 'Suburban Mumbai', '400064', 19.173820, 72.835120, 'ACTIVE'),
(4, 'DEMO_BOM_PVR', 'PVK PVR Mumbai', 'PVR Dynamix Mall, JVPD Scheme, Juhu', 'Vile Parle West', '400049', 19.107410, 72.826720, 'ACTIVE'),
(4, 'DEMO_BOM_AGS', 'PVK AGS Mumbai', 'R City Mall, LBS Road, Ghatkopar West', 'Central Suburbs', '400086', 19.099430, 72.916820, 'ACTIVE'),
(4, 'DEMO_BOM_METRO', 'PVK Metro Mumbai', 'Metro INOX Cinemas, MG Road, Dhobi Talao', 'Marine Lines', '400020', 18.941210, 72.829140, 'ACTIVE'),
(5, 'DEMO_DEL_CINEPLEX', 'PVK Cineplex Delhi', 'Select CITYWALK, Saket District Centre', 'Saket, South Delhi', '110017', 28.528410, 77.218920, 'ACTIVE'),
(5, 'DEMO_DEL_INOX', 'PVK INOX Delhi', 'Nehru Place Epicuria, Metro Station', 'Nehru Place', '110019', 28.551230, 77.251410, 'ACTIVE'),
(5, 'DEMO_DEL_PVR', 'PVK PVR Delhi', 'PVR Plaza, Connaught Circus, H-Block', 'Connaught Place', '110001', 28.632940, 77.219810, 'ACTIVE'),
(5, 'DEMO_DEL_AGS', 'PVK AGS Delhi', 'Pacific Mall, Tagore Garden, Najafgarh Road', 'West Delhi', '110027', 28.642130, 77.108420, 'ACTIVE'),
(5, 'DEMO_DEL_METRO', 'PVK Metro Delhi', 'V3S East Centre Mall, Vikas Marg, Nirman Vihar', 'Laxmi Nagar', '110092', 28.636410, 77.284520, 'ACTIVE');

-- Assign Manager to PVK Cineplex Chennai
INSERT INTO EMPLOYEE_THEATRE (user_id, theatre_id, status)
SELECT u.user_id, t.theatre_id, 'ACTIVE' FROM USER u, THEATRE t WHERE u.email = 'manager@pvkcinemas.com' AND t.theatre_code = 'DEMO_CHN_CINEPLEX';

-- =============================================================================
-- SECTION 4: SCREENS (Exactly 10 Screens per Theatre = 250 Screens Total)
-- Screen k is dedicated to Movie k, guaranteeing zero screen overlap violations
-- =============================================================================
INSERT INTO SCREEN (theatre_id, screen_code, screen_name, status)
SELECT t.theatre_id, CONCAT(t.theatre_code, '_S', sn.s_num), sn.s_name, 'ACTIVE'
FROM THEATRE t
CROSS JOIN (
  SELECT 1 AS s_num, 'Screen 1 - IMAX Laser' AS s_name UNION
  SELECT 2, 'Screen 2 - 4DX Motion' UNION
  SELECT 3, 'Screen 3 - RealD 3D' UNION
  SELECT 4, 'Screen 4 - Dolby Cinema' UNION
  SELECT 5, 'Screen 5 - 2D Premium' UNION
  SELECT 6, 'Screen 6 - Classic Cinema' UNION
  SELECT 7, 'Screen 7 - Classic Cinema' UNION
  SELECT 8, 'Screen 8 - Classic Cinema' UNION
  SELECT 9, 'Screen 9 - Classic Cinema' UNION
  SELECT 10, 'Screen 10 - VIP Lounge'
) sn;

-- SCREEN CAPABILITIES
-- Screen 1: IMAX + DOLBY_ATMOS
INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code LIKE '%_S1' AND pf.format_code = 'IMAX' AND af.format_code = 'DOLBY_ATMOS';

-- Screen 2: 4DX + DOLBY_7_1
INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code LIKE '%_S2' AND pf.format_code = '4DX' AND af.format_code = 'DOLBY_7_1';

-- Screen 3: 3D + DOLBY_5_1
INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE sc.screen_code LIKE '%_S3' AND pf.format_code = '3D' AND af.format_code = 'DOLBY_5_1';

-- Screens 4-10: 2D + DOLBY_5_1
INSERT INTO SCREEN_CAPABILITY (screen_id, presentation_format_id, audio_format_id)
SELECT sc.screen_id, pf.presentation_format_id, af.audio_format_id
FROM SCREEN sc, PRESENTATION_FORMAT pf, AUDIO_FORMAT af
WHERE (sc.screen_code LIKE '%_S4' OR sc.screen_code LIKE '%_S5' OR sc.screen_code LIKE '%_S6' OR
       sc.screen_code LIKE '%_S7' OR sc.screen_code LIKE '%_S8' OR sc.screen_code LIKE '%_S9' OR sc.screen_code LIKE '%_S10')
  AND pf.format_code = '2D' AND af.format_code = 'DOLBY_5_1';

-- =============================================================================
-- SECTION 5: PHYSICAL SEATS (50 Seats per Screen × 250 Screens = 12,500 Seats)
-- Rows A, B, C (1-10): Standard (30 seats)
-- Row D (1-10): Premium (10 seats)
-- Row E (1-8): Recliner (8 seats)
-- Row E (9-10): Accessible (2 seats)
-- =============================================================================
INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, position_x, position_y, status)
SELECT sc.screen_id, st.seat_type_id, r.rl, CAST(n.num AS CHAR), n.num, r.ry, 'ACTIVE'
FROM SCREEN sc
CROSS JOIN (
  SELECT 'A' AS rl, 1 AS ry UNION SELECT 'B', 2 UNION SELECT 'C', 3
) r
CROSS JOIN (
  SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
  SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) n
CROSS JOIN SEAT_TYPE st WHERE st.type_code = 'STANDARD';

INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, position_x, position_y, status)
SELECT sc.screen_id, st.seat_type_id, 'D', CAST(n.num AS CHAR), n.num, 4, 'ACTIVE'
FROM SCREEN sc
CROSS JOIN (
  SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
  SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) n
CROSS JOIN SEAT_TYPE st WHERE st.type_code = 'PREMIUM';

INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, position_x, position_y, status)
SELECT sc.screen_id, st.seat_type_id, 'E', CAST(n.num AS CHAR), n.num, 5, 'ACTIVE'
FROM SCREEN sc
CROSS JOIN (
  SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION
  SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8
) n
CROSS JOIN SEAT_TYPE st WHERE st.type_code = 'RECLINER';

INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, position_x, position_y, status)
SELECT sc.screen_id, st.seat_type_id, 'E', CAST(n.num AS CHAR), n.num, 5, 'ACTIVE'
FROM SCREEN sc
CROSS JOIN (SELECT 9 AS num UNION SELECT 10) n
CROSS JOIN SEAT_TYPE st WHERE st.type_code = 'ACCESSIBLE';

-- =============================================================================
-- SECTION 6: MOVIES (10 Canonical Blockbuster Titles)
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
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Inception' AND g.genre_code IN ('SCI_FI','THRILLER');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='The Dark Knight' AND g.genre_code IN ('ACTION','THRILLER');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='RRR' AND g.genre_code IN ('ACTION','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='KGF: Chapter 2' AND g.genre_code IN ('ACTION','THRILLER');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Pushpa 2: The Rule' AND g.genre_code IN ('ACTION','THRILLER');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Kalki 2898 AD' AND g.genre_code IN ('ACTION','SCI_FI');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Dune: Part Two' AND g.genre_code IN ('SCI_FI','DRAMA');
INSERT INTO MOVIE_GENRE (movie_id, genre_id) SELECT m.movie_id, g.genre_id FROM MOVIE m CROSS JOIN GENRE g WHERE m.title='Oppenheimer' AND g.genre_code IN ('DRAMA','THRILLER');

-- MOVIE LANGUAGES
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Interstellar' AND l.language_code='en';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='Interstellar' AND l.language_code IN ('hi','ta');
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Avengers: Endgame' AND l.language_code='en';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='Avengers: Endgame' AND l.language_code IN ('hi','ta','te');
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Inception' AND l.language_code='en';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='Inception' AND l.language_code='hi';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='The Dark Knight' AND l.language_code='en';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='The Dark Knight' AND l.language_code='hi';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='RRR' AND l.language_code='te';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='RRR' AND l.language_code IN ('hi','ta');
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='KGF: Chapter 2' AND l.language_code='kn';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='KGF: Chapter 2' AND l.language_code='hi';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Pushpa 2: The Rule' AND l.language_code='te';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='Pushpa 2: The Rule' AND l.language_code IN ('hi','ta');
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Kalki 2898 AD' AND l.language_code='te';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'DUBBED' FROM MOVIE m, LANGUAGE l WHERE m.title='Kalki 2898 AD' AND l.language_code IN ('hi','ta');
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Dune: Part Two' AND l.language_code='en';
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) SELECT m.movie_id, l.language_id, 'ORIGINAL' FROM MOVIE m, LANGUAGE l WHERE m.title='Oppenheimer' AND l.language_code='en';

-- =============================================================================
-- SECTION 7: SHOWS (Exactly 5 Showtimes per Theatre per Movie = 1,250 Shows)
-- Screen k in every theatre is dedicated to Movie k
-- Showtimes: 09:30 AM, 12:30 PM, 03:30 PM, 06:30 PM, 09:30 PM
-- ZERO screen overlaps guaranteed (150 min duration + 30 min turnover)
-- =============================================================================
-- Movie 1: Interstellar -> Screen 1 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Interstellar'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S1';

-- Movie 2: Avengers: Endgame -> Screen 2 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Avengers: Endgame'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S2';

-- Movie 3: Inception -> Screen 3 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Inception'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S3';

-- Movie 4: The Dark Knight -> Screen 4 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'The Dark Knight'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S4';

-- Movie 5: RRR -> Screen 5 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'RRR'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S5';

-- Movie 6: KGF: Chapter 2 -> Screen 6 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'KGF: Chapter 2'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S6';

-- Movie 7: Pushpa 2: The Rule -> Screen 7 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Pushpa 2: The Rule'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S7';

-- Movie 8: Kalki 2898 AD -> Screen 8 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Kalki 2898 AD'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S8';

-- Movie 9: Dune: Part Two -> Screen 9 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Dune: Part Two'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S9';

-- Movie 10: Oppenheimer -> Screen 10 in all 25 Theatres (5 shows each)
INSERT INTO `SHOW` (movie_language_id, screen_capability_id, start_at, end_at, status)
SELECT ml.movie_language_id, scp.screen_capability_id,
       CONCAT(CURDATE(), ' ', slot.st),
       CONCAT(CURDATE(), ' ', slot.et),
       'SCHEDULED'
FROM SCREEN sc
JOIN SCREEN_CAPABILITY scp ON sc.screen_id = scp.screen_id
JOIN MOVIE m ON m.title = 'Oppenheimer'
JOIN MOVIE_LANGUAGE ml ON ml.movie_id = m.movie_id AND ml.language_role = 'ORIGINAL'
CROSS JOIN (
  SELECT '09:30:00' AS st, '12:00:00' AS et UNION
  SELECT '12:30:00', '15:00:00' UNION
  SELECT '15:30:00', '18:00:00' UNION
  SELECT '18:30:00', '21:00:00' UNION
  SELECT '21:30:00', '23:59:00'
) slot
WHERE sc.screen_code LIKE '%_S10';

-- =============================================================================
-- SECTION 8: SHOW_SEAT AVAILABILITY (62,500 Rows: ~75% AVAILABLE, ~25% BOOKED, 0 BLOCKED)
-- Deterministic pseudo-random seed distribution using prime hashing
-- ZERO BLOCKED seats in fresh demo seed (management can block at runtime)
-- =============================================================================
INSERT INTO SHOW_SEAT (show_id, seat_id, availability_status)
SELECT sh.show_id, s.seat_id,
       CASE WHEN MOD(s.seat_id * 37 + sh.show_id * 73 + CAST(s.seat_number AS UNSIGNED) * 19, 100) < 25 THEN 'BOOKED'
            ELSE 'AVAILABLE' END
FROM `SHOW` sh
JOIN SCREEN_CAPABILITY scp ON sh.screen_capability_id = scp.screen_capability_id
JOIN SCREEN sc ON scp.screen_id = sc.screen_id
JOIN SEAT s ON s.screen_id = sc.screen_id;

-- =============================================================================
-- END V3 DEMO SEED
-- Summary: 5 Cities | 25 Theatres | 250 Screens | 12,500 Seats | 10 Movies | 1,250 Shows | 62,500 Show Seats
-- 0 Default BLOCKED Seats | Realistic ~75% AVAILABLE / ~25% BOOKED
-- =============================================================================