-- =============================================================================
-- PVK CINEMAS — V12__part2_movie_catalogue_and_lifecycle.sql
-- Part 2: Curated 45-Movie Catalogue, Active Languages & Lifecycle Windows
-- Dataset Version : 2026.10
-- Authoritative Scope: 45 Titles | 5 Active Languages (ta, te, hi, kn, en)
-- 5 Demand Tiers: BLOCKBUSTER, MAJOR_REGIONAL, MID_TIER, NICHE, RE_RELEASE
-- Populates: demand_tier, popularity_score, exhibition_start_date, exhibition_end_date
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. RESOLVE REFERENCE DATA IDENTIFIERS
-- -----------------------------------------------------------------------------
SET @cert_u  = (SELECT certification_id FROM CERTIFICATION WHERE certification_code = 'U');
SET @cert_ua = (SELECT certification_id FROM CERTIFICATION WHERE certification_code = 'UA');
SET @cert_a  = (SELECT certification_id FROM CERTIFICATION WHERE certification_code = 'A');

SET @gen_action    = (SELECT genre_id FROM GENRE WHERE genre_code = 'ACTION');
SET @gen_comedy    = (SELECT genre_id FROM GENRE WHERE genre_code = 'COMEDY');
SET @gen_drama     = (SELECT genre_id FROM GENRE WHERE genre_code = 'DRAMA');
SET @gen_scifi     = (SELECT genre_id FROM GENRE WHERE genre_code = 'SCI_FI');
SET @gen_thriller  = (SELECT genre_id FROM GENRE WHERE genre_code = 'THRILLER');
SET @gen_romance   = (SELECT genre_id FROM GENRE WHERE genre_code = 'ROMANCE');
SET @gen_animation = (SELECT genre_id FROM GENRE WHERE genre_code = 'ANIMATION');
SET @gen_horror    = (SELECT genre_id FROM GENRE WHERE genre_code = 'HORROR');

SET @lang_ta = (SELECT language_id FROM LANGUAGE WHERE language_code = 'ta');
SET @lang_en = (SELECT language_id FROM LANGUAGE WHERE language_code = 'en');
SET @lang_te = (SELECT language_id FROM LANGUAGE WHERE language_code = 'te');
SET @lang_hi = (SELECT language_id FROM LANGUAGE WHERE language_code = 'hi');
SET @lang_kn = (SELECT language_id FROM LANGUAGE WHERE language_code = 'kn');

-- -----------------------------------------------------------------------------
-- 2. UPDATE EXISTING MOVIES (IDs 1–15) WITH PART 2 LIFECYCLE & DEMAND METADATA
-- -----------------------------------------------------------------------------
UPDATE MOVIE
SET demand_tier = 'RE_RELEASE',
    popularity_score = 9.40,
    exhibition_start_date = '2026-09-01',
    exhibition_end_date = '2026-09-30',
    status = 'AIRING'
WHERE movie_id = 1;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.80,
    exhibition_start_date = '2026-09-04',
    exhibition_end_date = '2026-10-15',
    status = 'AIRING'
WHERE movie_id = 2;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.20,
    exhibition_start_date = '2026-08-15',
    exhibition_end_date = '2026-09-25',
    status = 'AIRING'
WHERE movie_id = 3;

UPDATE MOVIE
SET demand_tier = 'RE_RELEASE',
    popularity_score = 9.10,
    exhibition_start_date = '2026-09-11',
    exhibition_end_date = '2026-09-24',
    status = 'AIRING'
WHERE movie_id = 4;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.50,
    exhibition_start_date = '2026-09-15',
    exhibition_end_date = '2026-10-31',
    status = 'AIRING'
WHERE movie_id = 5;

UPDATE MOVIE
SET demand_tier = 'MAJOR_REGIONAL',
    popularity_score = 8.90,
    exhibition_start_date = '2026-08-01',
    exhibition_end_date = '2026-09-19',
    status = 'AIRING'
WHERE movie_id = 6;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.30,
    exhibition_start_date = '2026-09-18',
    exhibition_end_date = '2026-10-30',
    status = 'AIRING'
WHERE movie_id = 7;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.70,
    exhibition_start_date = '2026-09-08',
    exhibition_end_date = '2026-10-20',
    status = 'AIRING'
WHERE movie_id = 8;

UPDATE MOVIE
SET demand_tier = 'MAJOR_REGIONAL',
    popularity_score = 8.80,
    exhibition_start_date = '2026-08-20',
    exhibition_end_date = '2026-09-30',
    status = 'AIRING'
WHERE movie_id = 9;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.90,
    exhibition_start_date = '2026-09-05',
    exhibition_end_date = '2026-10-25',
    status = 'AIRING'
WHERE movie_id = 10;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.85,
    exhibition_start_date = '2026-10-02',
    exhibition_end_date = '2026-11-20',
    status = 'UPCOMING'
WHERE movie_id = 11;

UPDATE MOVIE
SET demand_tier = 'BLOCKBUSTER',
    popularity_score = 9.40,
    exhibition_start_date = '2026-08-10',
    exhibition_end_date = '2026-09-22',
    status = 'AIRING'
WHERE movie_id = 12;

UPDATE MOVIE
SET demand_tier = 'MID_TIER',
    popularity_score = 8.50,
    exhibition_start_date = '2026-09-01',
    exhibition_end_date = '2026-10-10',
    status = 'AIRING'
WHERE movie_id = 13;

UPDATE MOVIE
SET demand_tier = 'NICHE',
    popularity_score = 8.10,
    exhibition_start_date = '2026-09-12',
    exhibition_end_date = '2026-10-05',
    status = 'AIRING'
WHERE movie_id = 14;

UPDATE MOVIE
SET demand_tier = 'MAJOR_REGIONAL',
    popularity_score = 8.60,
    exhibition_start_date = '2026-10-15',
    exhibition_end_date = '2026-11-25',
    status = 'UPCOMING'
WHERE movie_id = 15;

-- -----------------------------------------------------------------------------
-- 3. ENSURE CLEAN SLATE & INSERT ADDITIONAL 30 CURATED MOVIES (IDs 16–45)
-- -----------------------------------------------------------------------------
-- Remove any transient test fixtures with movie_id > 15 before inserting master catalogue
DELETE FROM BOOKING_SEAT WHERE booking_id IN (
    SELECT booking_id FROM BOOKING WHERE show_id IN (
        SELECT show_id FROM `SHOW` WHERE movie_language_id IN (
            SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id > 15
        )
    )
);
DELETE FROM PAYMENT WHERE booking_id IN (
    SELECT booking_id FROM BOOKING WHERE show_id IN (
        SELECT show_id FROM `SHOW` WHERE movie_language_id IN (
            SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id > 15
        )
    )
);
DELETE FROM BOOKING WHERE show_id IN (
    SELECT show_id FROM `SHOW` WHERE movie_language_id IN (
        SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id > 15
    )
);
DELETE FROM SEAT_HOLD WHERE show_id IN (
    SELECT show_id FROM `SHOW` WHERE movie_language_id IN (
        SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id > 15
    )
);
DELETE FROM SHOW_SEAT WHERE show_id IN (
    SELECT show_id FROM `SHOW` WHERE movie_language_id IN (
        SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id > 15
    )
);
DELETE FROM `SHOW` WHERE movie_language_id IN (
    SELECT movie_language_id FROM MOVIE_LANGUAGE WHERE movie_id > 15
);
DELETE FROM MOVIE_LANGUAGE WHERE movie_id > 15;
DELETE FROM MOVIE_GENRE WHERE movie_id > 15;
DELETE FROM MOVIE WHERE movie_id > 15;

INSERT INTO MOVIE (movie_id, certification_id, title, original_title, synopsis, runtime_minutes, release_date, status, poster_url, trailer_url, demand_tier, popularity_score, exhibition_start_date, exhibition_end_date) VALUES
(16, @cert_ua, 'Jailer', 'Jailer', 'A retired prison warden goes on a manhunt to avenge the presumed murder of his police officer son, revealing his past syndicate identity.', 168, '2023-08-10', 'ENDED', 'https://image.tmdb.org/t/p/w500/q2i0iB8bZ8sM3u5k2o1uX6pQ0vE.jpg', 'https://www.youtube.com/watch?v=Y5BeWdODPqo', 'BLOCKBUSTER', 9.15, '2026-05-01', '2026-06-15'),
(17, @cert_ua, 'Ponniyin Selvan: Part 1', 'Ponniyin Selvan: 1', 'Vandiyathevan sets out to cross the Chola land to deliver a message from Crown Prince Aditha Karikalan amidst brewing palace mutiny.', 167, '2022-09-30', 'ENDED', 'https://image.tmdb.org/t/p/w500/1X7vQ4z1kL5n6u7t8e9w0xY1zA.jpg', 'https://www.youtube.com/watch?v=D4qAQYLGZVM', 'MAJOR_REGIONAL', 8.75, '2026-03-01', '2026-04-15'),
(18, @cert_ua, 'Maharaja', 'Maharaja', 'A barber seeks vengeance and reports a missing dustbin to police, setting off a gripping, non-linear chain of dark retribution.', 142, '2024-06-14', 'AIRING', 'https://image.tmdb.org/t/p/w500/mH7e6r8t1y2u3i4o5p6a7s8d9f.jpg', 'https://www.youtube.com/watch?v=uK8f6fR5sBw', 'MID_TIER', 8.65, '2026-09-10', '2026-10-10'),
(19, @cert_u, 'Kadaisi Vivasayi', 'Kadaisi Vivasayi', 'An elderly farmer in a rural Tamil hamlet stubbornly continues traditional farming, standing as the sole agricultural guardian of ancestral seeds.', 144, '2022-02-11', 'AIRING', 'https://image.tmdb.org/t/p/w500/k8v9w0x1y2z3a4b5c6d7e8f9g0.jpg', 'https://www.youtube.com/watch?v=m7z5eXvK2p8', 'NICHE', 7.80, '2026-09-05', '2026-09-25'),
(20, @cert_u, 'Ghilli', 'Ghilli', 'A state-level kabaddi player in Madurai rescues an innocent young woman from a tyrannical faction leader in an iconic crowd-pleasing action saga.', 160, '2004-04-17', 'AIRING', 'https://image.tmdb.org/t/p/w500/g8h9i0j1k2l3m4n5o6p7q8r9s0.jpg', 'https://www.youtube.com/watch?v=h9g8f7e6d5c', 'RE_RELEASE', 8.70, '2026-09-16', '2026-09-22'),
(21, @cert_ua, 'Thangalaan', 'Thangalaan', 'A tribal leader during British colonial rule aids in the search for gold in the Kolar Gold Fields, confronting mythical guardian sorcery.', 156, '2024-08-15', 'UPCOMING', 'https://image.tmdb.org/t/p/w500/t9h8a7b6c5d4e3f2g1h0i9j8k7.jpg', 'https://www.youtube.com/watch?v=t7h8g9f0e1d', 'MAJOR_REGIONAL', 8.40, '2026-10-10', '2026-11-15'),
(22, @cert_a, 'Viduthalai Part 1', 'Viduthalai Part 1', 'A newly recruited police constable is conflicted between duty and conscience during a brutal crackdown on a rebel hill insurgent leader.', 150, '2023-03-31', 'ENDED', 'https://image.tmdb.org/t/p/w500/v1d8u7t6h5a4l3a2i1p0a9r8t7.jpg', 'https://www.youtube.com/watch?v=v8d7t6h5a4l', 'NICHE', 8.20, '2026-02-15', '2026-03-25'),
(23, @cert_ua, 'Kalki 2898 AD', 'Kalki 2898 AD', 'A modern avatar of Vishnu descends to Earth to protect humanity from tyrannical dystopian forces in the futuristic city of Kasi.', 181, '2024-06-27', 'AIRING', 'https://image.tmdb.org/t/p/w500/k8a9l0k1i2j3a4b5c6d7e8f9g0.jpg', 'https://www.youtube.com/watch?v=y16w0q5gA4k', 'BLOCKBUSTER', 9.75, '2026-09-17', '2026-10-31'),
(24, @cert_a, 'Salaar: Part 1 – Ceasefire', 'Salaar: Part 1 – Ceasefire', 'A gang leader makes a solemn promise to a dying friend and takes on other criminal clans in the fortified dystopian city of Khansaar.', 175, '2023-12-22', 'ENDED', 'https://image.tmdb.org/t/p/w500/s8a9l0k1i2j3a4b5c6d7e8f9g0.jpg', 'https://www.youtube.com/watch?v=4GPvYMKrytI', 'BLOCKBUSTER', 9.30, '2026-06-01', '2026-07-15'),
(25, @cert_ua, 'Devara: Part 1', 'Devara: Part 1', 'A brave seafaring chieftain safeguards coastal waters against ruthless smuggling operations, leading to generational vengeance.', 170, '2024-09-27', 'AIRING', 'https://image.tmdb.org/t/p/w500/d8e9v0a1r2a3p4a5r6t7o8n9e0.jpg', 'https://www.youtube.com/watch?v=k9a8b7c6d5e', 'MAJOR_REGIONAL', 8.95, '2026-09-18', '2026-10-25'),
(26, @cert_ua, 'Hanu-Man', 'Hanu-Man', 'An ordinary youth in an imaginary ancient village gains the legendary powers of Lord Hanuman and fights against corporate evil.', 158, '2024-01-12', 'ENDED', 'https://image.tmdb.org/t/p/w500/h8a9n0u1m2a3n4p5q6r7s8t9u0.jpg', 'https://www.youtube.com/watch?v=OqvLY_n3g9M', 'MAJOR_REGIONAL', 8.60, '2026-04-10', '2026-05-25'),
(27, @cert_u, 'Hi Nanna', 'Hi Nanna', 'A doting single father and his precocious young daughter find their lives transformed when an empathetic woman enters their world.', 155, '2023-12-07', 'AIRING', 'https://image.tmdb.org/t/p/w500/h8i9n0a1n2n3a4r5o6m7a8n9c0.jpg', 'https://www.youtube.com/watch?v=b8c7d6e5f4g', 'MID_TIER', 8.30, '2026-09-08', '2026-10-05'),
(28, @cert_u, 'Balagam', 'Balagam', 'Set in a Telangana village, family fissures and ancestral traditions come to light during the traditional mourning rites for a patriarch.', 131, '2023-03-03', 'ENDED', 'https://image.tmdb.org/t/p/w500/b8a9l0a1g2a3m4f5a6m7i8l9y0.jpg', 'https://www.youtube.com/watch?v=a7b6c5d4e3f', 'NICHE', 7.90, '2026-01-15', '2026-02-20'),
(29, @cert_ua, 'Pokiri', 'Pokiri', 'A street-smart mercenary operating inside the Hyderabad underworld conceals a startling undercover identity.', 160, '2006-04-28', 'AIRING', 'https://image.tmdb.org/t/p/w500/p8o9k0i1r2i3a4c5t6i7o8n9s0.jpg', 'https://www.youtube.com/watch?v=z8y7x6w5v4u', 'RE_RELEASE', 8.65, '2026-09-14', '2026-09-20'),
(30, @cert_u, '777 Charlie', '777 Charlie', 'A lonely factory worker stuck in a monotonous life finds renewed purpose and joy when an energetic Labrador pup arrives on his doorstep.', 164, '2022-06-10', 'ENDED', 'https://image.tmdb.org/t/p/w500/c7h7a7r8l9i0e1d2o3g4f5i6l7.jpg', 'https://www.youtube.com/watch?v=k8l7m6n5o4p', 'MAJOR_REGIONAL', 8.70, '2026-07-01', '2026-08-15'),
(31, @cert_ua, 'Sapta Sagaradaache Ello – Side A', 'Sapta Sagaradaache Ello – Side A', 'A pair of star-crossed young lovers dream of a serene seaside life, but a fateful decision spirals their fragile romance into jeopardy.', 142, '2023-09-01', 'AIRING', 'https://image.tmdb.org/t/p/w500/s8a9p0t1a2s3a4g5a6r7a8d9a0.jpg', 'https://www.youtube.com/watch?v=j8k7l6m5n4o', 'MID_TIER', 8.40, '2026-09-12', '2026-10-10'),
(32, @cert_ua, 'Sapta Sagaradaache Ello – Side B', 'Sapta Sagaradaache Ello – Side B', 'Ten years after his tragic separation, Manu searches for Priya to ensure her happiness, navigating unresolved pain and forgiveness.', 146, '2023-11-17', 'UPCOMING', 'https://image.tmdb.org/t/p/w500/s8b9p0t1a2s3a4g5a6r7a8d9b0.jpg', 'https://www.youtube.com/watch?v=i8j7k6l5m4n', 'MID_TIER', 8.35, '2026-10-15', '2026-11-15'),
(33, @cert_a, 'Garuda Gamana Vrishabha Vahana', 'Garuda Gamana Vrishabha Vahana', 'In coastal Mangalore, two childhood allies rise through gangland ranks until ruthless ambition tears their brotherhood asunder.', 151, '2021-11-19', 'ENDED', 'https://image.tmdb.org/t/p/w500/g8a9r0u1d2a3g4a5m6a7n8a9v0.jpg', 'https://www.youtube.com/watch?v=g8f7e6d5c4b', 'NICHE', 8.25, '2026-03-10', '2026-04-20'),
(34, @cert_ua, 'Ulidavaru Kandanthe', 'Ulidavaru Kandanthe', 'A Rashomon-style neo-noir mystery exploring a murder incident during the Krishna Janmashtami festival in coastal Karnataka.', 154, '2014-03-28', 'AIRING', 'https://image.tmdb.org/t/p/w500/u8l9i0d1a2v3a4r5u6k7a8n9d0.jpg', 'https://www.youtube.com/watch?v=f8e7d6c5b4a', 'RE_RELEASE', 8.50, '2026-09-15', '2026-09-22'),
(35, @cert_ua, 'Martin', 'Martin', 'An army officer embarks on a journey to discover his true identity while combating ruthless terror networks across the borders.', 148, '2024-10-11', 'UPCOMING', 'https://image.tmdb.org/t/p/w500/m8a9r0t1i2n3a4c5t6i7o8n9s0.jpg', 'https://www.youtube.com/watch?v=e8d7c6b5a4z', 'MAJOR_REGIONAL', 8.10, '2026-10-20', '2026-11-25'),
(36, @cert_ua, 'Kantara: Chapter 1', 'Kantara: Chapter 1', 'The grand prequel unfolding the ancient origin story of the divine Daiva protector in the coastal forests of Karnataka.', 160, '2025-10-02', 'UPCOMING', 'https://image.tmdb.org/t/p/w500/k8a9n0t1a2r3a4c5h6a7p8t9e0.jpg', 'https://www.youtube.com/watch?v=d8c7b6a5z4y', 'BLOCKBUSTER', 9.80, '2026-11-15', '2026-12-31'),
(37, @cert_a, 'Animal', 'Animal', 'A son driven by obsessive, unconditioned love for his distant father embarks on an unapologetically violent bloodbath of revenge.', 201, '2023-12-01', 'ENDED', 'https://image.tmdb.org/t/p/w500/a8n9i0m1a2l3a4c5t6i7o8n9s0.jpg', 'https://www.youtube.com/watch?v=Dydmpfo68DA', 'BLOCKBUSTER', 9.60, '2026-05-15', '2026-07-01'),
(38, @cert_ua, 'Pathaan', 'Pathaan', 'An exiled RAW field operative is called back to active duty to neutralize a rogue private mercenary outfit plotting an attack on India.', 146, '2023-01-25', 'ENDED', 'https://image.tmdb.org/t/p/w500/p8a9t0h1a2a3n4s5p6y7v8e9r0.jpg', 'https://www.youtube.com/watch?v=vqu4z34wENw', 'BLOCKBUSTER', 9.20, '2026-02-01', '2026-03-15'),
(39, @cert_ua, 'Shaitaan', 'Shaitaan', 'A family weekend getaway at a secluded farmhouse turns sinister when a mysterious stranger hypnotizes their teenage daughter.', 132, '2024-03-08', 'AIRING', 'https://image.tmdb.org/t/p/w500/s8h9a0i1t2a3a4n5h6o7r8r9o0.jpg', 'https://www.youtube.com/watch?v=b7c6d5e4f3a', 'MID_TIER', 8.15, '2026-08-25', '2026-09-25'),
(40, @cert_u, 'Laapataa Ladies', 'Laapataa Ladies', 'Two newly-wed brides get accidentally swapped on a crowded passenger train in rural India, sparking a heartwarming voyage of self-discovery.', 122, '2024-03-01', 'AIRING', 'https://image.tmdb.org/t/p/w500/l8a9a0p1a2t3a4a5l6a7d8i9e0.jpg', 'https://www.youtube.com/watch?v=a6b5c4d3e2f', 'NICHE', 8.45, '2026-09-02', '2026-09-28'),
(41, @cert_u, 'Dilwale Dulhania Le Jayenge', 'Dilwale Dulhania Le Jayenge', 'Raj and Simran meet on a backpacking trip across Europe and fall in love; Raj travels to Punjab to win over Simran traditional family.', 189, '1995-10-20', 'AIRING', 'https://image.tmdb.org/t/p/w500/d8d9l0j1r2o3m4a5n6c7e8s9h0.jpg', 'https://www.youtube.com/watch?v=c25GbrZ2m3k', 'RE_RELEASE', 8.90, '2026-09-17', '2026-09-24'),
(42, @cert_a, 'Deadpool & Wolverine', 'Deadpool & Wolverine', 'Wade Wilson is pulled from quiet retirement by the Time Variance Authority and recruits a sullen Wolverine to rescue his universe.', 128, '2024-07-26', 'AIRING', 'https://image.tmdb.org/t/p/w500/d8p9w0o1l2v3e4r5i6n7e8m9a0.jpg', 'https://www.youtube.com/watch?v=73_1biulkYk', 'BLOCKBUSTER', 9.65, '2026-08-28', '2026-10-15'),
(43, @cert_u, 'Spider-Man: Across the Spider-Verse', 'Spider-Man: Across the Spider-Verse', 'Miles Morales is catapulted across the Multiverse where he encounters a team of Spider-People charged with protecting its existence.', 140, '2023-06-02', 'ENDED', 'https://image.tmdb.org/t/p/w500/s8p9i0d1e2r3m4a5n6s7p8i9d0.jpg', 'https://www.youtube.com/watch?v=cqGjhVJWtEg', 'MAJOR_REGIONAL', 9.05, '2026-04-15', '2026-05-31'),
(44, @cert_u, 'Past Lives', 'Past Lives', 'Nora and Hae Sung, two deeply connected childhood friends, are wrested apart after Nora family emigrates from South Korea; they reunite two decades later.', 106, '2023-06-02', 'AIRING', 'https://image.tmdb.org/t/p/w500/p8a9s0t1l2i3v4e5s6r7o8m9a0.jpg', 'https://www.youtube.com/watch?v=kA244xewjcI', 'NICHE', 8.10, '2026-09-15', '2026-09-23'),
(45, @cert_ua, 'The Dark Knight', 'The Dark Knight', 'When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest tests.', 152, '2008-07-18', 'AIRING', 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', 'https://www.youtube.com/watch?v=EXeTwQWrcwY', 'RE_RELEASE', 9.55, '2026-09-14', '2026-09-21');

-- -----------------------------------------------------------------------------
-- 4. INSERT MOVIE GENRES FOR MOVIES 16–45
-- -----------------------------------------------------------------------------
INSERT INTO MOVIE_GENRE (movie_id, genre_id) VALUES
(16, @gen_action),
(16, @gen_comedy),
(17, @gen_drama),
(17, @gen_action),
(18, @gen_thriller),
(18, @gen_drama),
(19, @gen_drama),
(20, @gen_action),
(20, @gen_romance),
(21, @gen_action),
(21, @gen_drama),
(22, @gen_drama),
(22, @gen_thriller),
(23, @gen_action),
(23, @gen_scifi),
(24, @gen_action),
(24, @gen_thriller),
(25, @gen_action),
(25, @gen_drama),
(26, @gen_action),
(26, @gen_scifi),
(27, @gen_drama),
(27, @gen_romance),
(28, @gen_drama),
(29, @gen_action),
(29, @gen_thriller),
(30, @gen_drama),
(30, @gen_comedy),
(31, @gen_romance),
(31, @gen_drama),
(32, @gen_romance),
(32, @gen_drama),
(33, @gen_drama),
(33, @gen_thriller),
(34, @gen_drama),
(34, @gen_thriller),
(35, @gen_action),
(35, @gen_thriller),
(36, @gen_action),
(36, @gen_drama),
(37, @gen_action),
(37, @gen_drama),
(38, @gen_action),
(38, @gen_thriller),
(39, @gen_horror),
(39, @gen_thriller),
(40, @gen_comedy),
(40, @gen_drama),
(41, @gen_romance),
(41, @gen_drama),
(42, @gen_action),
(42, @gen_comedy),
(43, @gen_animation),
(43, @gen_action),
(44, @gen_drama),
(44, @gen_romance),
(45, @gen_action),
(45, @gen_thriller);

-- -----------------------------------------------------------------------------
-- 5. INSERT MOVIE LANGUAGES FOR MOVIES 16–45 (Canonical Languages: ta, te, hi, kn, en)
-- -----------------------------------------------------------------------------
INSERT INTO MOVIE_LANGUAGE (movie_id, language_id, language_role) VALUES
(16, @lang_ta, 'ORIGINAL'),
(16, @lang_te, 'DUBBED'),
(16, @lang_hi, 'DUBBED'),
(16, @lang_kn, 'DUBBED'),
(17, @lang_ta, 'ORIGINAL'),
(17, @lang_te, 'DUBBED'),
(17, @lang_hi, 'DUBBED'),
(17, @lang_kn, 'DUBBED'),
(18, @lang_ta, 'ORIGINAL'),
(18, @lang_te, 'DUBBED'),
(18, @lang_hi, 'DUBBED'),
(19, @lang_ta, 'ORIGINAL'),
(20, @lang_ta, 'ORIGINAL'),
(21, @lang_ta, 'ORIGINAL'),
(21, @lang_te, 'DUBBED'),
(21, @lang_hi, 'DUBBED'),
(22, @lang_ta, 'ORIGINAL'),
(22, @lang_te, 'DUBBED'),
(23, @lang_te, 'ORIGINAL'),
(23, @lang_hi, 'DUBBED'),
(23, @lang_ta, 'DUBBED'),
(23, @lang_kn, 'DUBBED'),
(23, @lang_en, 'DUBBED'),
(24, @lang_te, 'ORIGINAL'),
(24, @lang_hi, 'DUBBED'),
(24, @lang_ta, 'DUBBED'),
(24, @lang_kn, 'DUBBED'),
(25, @lang_te, 'ORIGINAL'),
(25, @lang_hi, 'DUBBED'),
(25, @lang_ta, 'DUBBED'),
(25, @lang_kn, 'DUBBED'),
(26, @lang_te, 'ORIGINAL'),
(26, @lang_hi, 'DUBBED'),
(26, @lang_ta, 'DUBBED'),
(26, @lang_kn, 'DUBBED'),
(27, @lang_te, 'ORIGINAL'),
(27, @lang_hi, 'DUBBED'),
(27, @lang_ta, 'DUBBED'),
(28, @lang_te, 'ORIGINAL'),
(29, @lang_te, 'ORIGINAL'),
(30, @lang_kn, 'ORIGINAL'),
(30, @lang_hi, 'DUBBED'),
(30, @lang_ta, 'DUBBED'),
(30, @lang_te, 'DUBBED'),
(31, @lang_kn, 'ORIGINAL'),
(31, @lang_te, 'DUBBED'),
(31, @lang_ta, 'DUBBED'),
(32, @lang_kn, 'ORIGINAL'),
(32, @lang_te, 'DUBBED'),
(32, @lang_ta, 'DUBBED'),
(33, @lang_kn, 'ORIGINAL'),
(33, @lang_hi, 'DUBBED'),
(34, @lang_kn, 'ORIGINAL'),
(35, @lang_kn, 'ORIGINAL'),
(35, @lang_hi, 'DUBBED'),
(35, @lang_te, 'DUBBED'),
(35, @lang_ta, 'DUBBED'),
(36, @lang_kn, 'ORIGINAL'),
(36, @lang_hi, 'DUBBED'),
(36, @lang_te, 'DUBBED'),
(36, @lang_ta, 'DUBBED'),
(36, @lang_en, 'DUBBED'),
(37, @lang_hi, 'ORIGINAL'),
(37, @lang_te, 'DUBBED'),
(37, @lang_ta, 'DUBBED'),
(37, @lang_kn, 'DUBBED'),
(38, @lang_hi, 'ORIGINAL'),
(38, @lang_te, 'DUBBED'),
(38, @lang_ta, 'DUBBED'),
(39, @lang_hi, 'ORIGINAL'),
(40, @lang_hi, 'ORIGINAL'),
(41, @lang_hi, 'ORIGINAL'),
(42, @lang_en, 'ORIGINAL'),
(42, @lang_hi, 'DUBBED'),
(42, @lang_ta, 'DUBBED'),
(42, @lang_te, 'DUBBED'),
(43, @lang_en, 'ORIGINAL'),
(43, @lang_hi, 'DUBBED'),
(43, @lang_ta, 'DUBBED'),
(43, @lang_te, 'DUBBED'),
(44, @lang_en, 'ORIGINAL'),
(45, @lang_en, 'ORIGINAL'),
(45, @lang_hi, 'DUBBED');

-- -----------------------------------------------------------------------------
-- 6. UPDATE DATASET_CONFIG METADATA
-- -----------------------------------------------------------------------------
INSERT INTO DATASET_CONFIG (config_key, config_value, description) VALUES
('dataset.movies',           '45',                  'Total curated movie titles in Part 2 catalogue'),
('dataset.active_languages', 'ta,te,hi,kn,en',      'Canonical active language codes supported in Part 2'),
('dataset.part2_status',     'COMPLETED',           'Part 2 Movie Catalogue & Lifecycle status'),
('dataset.v12_applied_at',   NOW(),                 'Timestamp when V12 catalogue and lifecycle migration was applied')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description  = VALUES(description);

-- =============================================================================
-- END V12 — Part 2 Movie Catalogue & Lifecycle
-- =============================================================================
