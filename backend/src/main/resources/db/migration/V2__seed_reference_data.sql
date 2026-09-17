-- =============================================================================
-- PVK CINEMAS — V2__seed_reference_data.sql
-- Authoritative Static Reference Data
-- Primary Authority: PVK_Cinemas_Final_Database_Design.docx, Phase 0.6 Freeze, PVK-ARCH-001
-- =============================================================================

-- =============================================================================
-- 1. SECURITY ROLES
-- =============================================================================
INSERT INTO ROLE (role_code, name, description, scope) VALUES
('ROLE_SUPER_ADMIN', 'Super Administrator', 'Platform-wide administrator with full authority across all modules and multiplexes', 'PLATFORM'),
('ROLE_THEATRE_MANAGER', 'Theatre Manager', 'Operational manager authorized for assigned multiplexes via EMPLOYEE_THEATRE', 'THEATRE'),
('ROLE_CUSTOMER', 'Customer', 'Standard registered cinema patron', 'PLATFORM');

-- =============================================================================
-- 2. OPERATIONAL PERMISSIONS
-- =============================================================================
INSERT INTO PERMISSION (permission_code, name, description) VALUES
('THEATRE_READ', 'View Theatres', 'View multiplex information, facilities, and contact details'),
('THEATRE_WRITE', 'Manage Theatres', 'Create, update, or modify theatre multiplex metadata'),
('SCREEN_READ', 'View Screens', 'View auditorium layout, formats, and screen configurations'),
('SCREEN_WRITE', 'Manage Screens', 'Create, update, or configure auditoriums and screen capabilities'),
('SEAT_READ', 'View Seats', 'View physical seat layouts and real-time show availability'),
('SEAT_WRITE', 'Override Seat Availability', 'Update seat availability status (AVAILABLE, BLOCKED, BOOKED) for operational needs'),
('MOVIE_READ', 'View Catalogue', 'Browse movies, genres, languages, certifications, and details'),
('MOVIE_WRITE', 'Manage Catalogue', 'Create and modify movie metadata, certifications, and media links'),
('SHOW_READ', 'View Showtimes', 'Browse scheduled shows, presentation formats, and timings'),
('SHOW_WRITE', 'Schedule Shows', 'Create, update, reschedule, or cancel auditorium shows'),
('SEARCH_EXECUTE', 'Execute Search', 'Execute AI hybrid and lexical cinema search queries'),
('AUDIT_READ', 'View Audit Logs', 'Inspect immutable platform security and operational audit trails'),
('USER_MANAGE', 'Manage Users', 'Manage employee profiles, theatre assignments, and administrative access');

-- =============================================================================
-- 3. ROLE-PERMISSION MAPPINGS
-- =============================================================================

-- Super Admin: Full Permissions
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_code = 'ROLE_SUPER_ADMIN';

-- Theatre Manager: Operational Multiplex Permissions
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_code = 'ROLE_THEATRE_MANAGER'
  AND p.permission_code IN (
    'THEATRE_READ',
    'SCREEN_READ', 'SCREEN_WRITE',
    'SEAT_READ', 'SEAT_WRITE',
    'MOVIE_READ',
    'SHOW_READ', 'SHOW_WRITE',
    'SEARCH_EXECUTE'
  );

-- Customer: Public / Patron Permissions
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_code = 'ROLE_CUSTOMER'
  AND p.permission_code IN (
    'THEATRE_READ',
    'SCREEN_READ',
    'SEAT_READ',
    'MOVIE_READ',
    'SHOW_READ',
    'SEARCH_EXECUTE'
  );

-- =============================================================================
-- 4. OPERATIONAL CITIES
-- =============================================================================
INSERT INTO CITY (city_name, state_name, country_code) VALUES
('Chennai', 'Tamil Nadu', 'IN'),
('Bengaluru', 'Karnataka', 'IN'),
('Hyderabad', 'Telangana', 'IN'),
('Coimbatore', 'Tamil Nadu', 'IN');

-- =============================================================================
-- 5. PHYSICAL SEAT TYPES
-- =============================================================================
INSERT INTO SEAT_TYPE (type_code, name, description) VALUES
('STANDARD', 'Standard', 'Standard cinema auditorium seat'),
('PREMIUM', 'Premium', 'Premium comfort seat with enhanced legroom'),
('RECLINER', 'Recliner', 'Luxury motorized recliner chair'),
('ACCESSIBLE', 'Accessible', 'Wheelchair and accessibility designated seat');

-- =============================================================================
-- 6. FILM GENRES
-- =============================================================================
INSERT INTO GENRE (genre_code, name) VALUES
('ACTION', 'Action'),
('COMEDY', 'Comedy'),
('DRAMA', 'Drama'),
('SCI_FI', 'Sci-Fi'),
('THRILLER', 'Thriller'),
('ROMANCE', 'Romance'),
('ANIMATION', 'Animation'),
('HORROR', 'Horror');

-- =============================================================================
-- 7. LANGUAGES
-- =============================================================================
INSERT INTO LANGUAGE (language_code, name) VALUES
('ta', 'Tamil'),
('en', 'English'),
('te', 'Telugu'),
('hi', 'Hindi'),
('ml', 'Malayalam'),
('kn', 'Kannada');

-- =============================================================================
-- 8. STATUTORY CERTIFICATIONS
-- =============================================================================
INSERT INTO CERTIFICATION (certification_code, name, description) VALUES
('U', 'Unrestricted Public Exhibition', 'Universal public exhibition without age restriction'),
('UA', 'Parental Guidance', 'Unrestricted public exhibition with parental discretion for children below 12 years'),
('A', 'Adults Only', 'Exhibition strictly restricted to adults aged 18 and above'),
('S', 'Specialized Audience', 'Exhibition restricted to specialized professions or audiences');

-- =============================================================================
-- 9. PRESENTATION FORMATS
-- =============================================================================
INSERT INTO PRESENTATION_FORMAT (format_code, name, description) VALUES
('2D', 'Standard 2D', 'Standard 2-Dimensional digital projection'),
('3D', 'Digital 3D', 'Stereoscopic digital 3D projection'),
('IMAX', 'IMAX Experience', 'Large-format immersive high-resolution projection'),
('4DX', '4DX Motion', 'Dynamic motion seating with synchronized atmospheric sensory effects');

-- =============================================================================
-- 10. AUDIO FORMATS
-- =============================================================================
INSERT INTO AUDIO_FORMAT (format_code, name, description) VALUES
('DOLBY_5_1', 'Dolby Digital 5.1', 'Standard 5.1 multi-channel discrete surround sound'),
('DOLBY_7_1', 'Dolby Surround 7.1', 'Enhanced 7.1 multi-channel discrete surround sound'),
('DOLBY_ATMOS', 'Dolby Atmos', 'Object-based multidimensional immersive spatial audio'),
('IMAX_12CH', 'IMAX 12-Channel', 'Next-generation 12.0 channel immersive audio system');
