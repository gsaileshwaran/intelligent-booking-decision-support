-- =============================================================================
-- PVK CINEMAS — V1__init_schema.sql
-- Relational Schema Initialization (Authoritative 28 Entities)
-- Primary Authority: PVK_Cinemas_Final_Database_Design.docx (Doc #5)
-- Target RDBMS: MySQL 8.0+ | Engine: InnoDB | Charset: utf8mb4_unicode_ci
-- =============================================================================

-- =============================================================================
-- TIER 1: INDEPENDENT MASTER & REFERENCE TABLES
-- =============================================================================

-- 1. USER
CREATE TABLE USER (
    user_id BIGINT AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT pk_user PRIMARY KEY (user_id),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT uq_user_phone UNIQUE (phone),
    CONSTRAINT chk_user_account_status CHECK (account_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. ROLE
CREATE TABLE ROLE (
    role_id BIGINT AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL DEFAULT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'PLATFORM',
    CONSTRAINT pk_role PRIMARY KEY (role_id),
    CONSTRAINT uq_role_code UNIQUE (role_code),
    CONSTRAINT chk_role_scope CHECK (scope IN ('PLATFORM', 'THEATRE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. PERMISSION
CREATE TABLE PERMISSION (
    permission_id BIGINT AUTO_INCREMENT,
    permission_code VARCHAR(80) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL DEFAULT NULL,
    CONSTRAINT pk_permission PRIMARY KEY (permission_id),
    CONSTRAINT uq_permission_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. CITY
CREATE TABLE CITY (
    city_id BIGINT AUTO_INCREMENT,
    city_name VARCHAR(100) NOT NULL,
    state_name VARCHAR(100) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'IN',
    CONSTRAINT pk_city PRIMARY KEY (city_id),
    CONSTRAINT uq_city_state_country UNIQUE (city_name, state_name, country_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. SEAT_TYPE
CREATE TABLE SEAT_TYPE (
    seat_type_id BIGINT AUTO_INCREMENT,
    type_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL DEFAULT NULL,
    CONSTRAINT pk_seat_type PRIMARY KEY (seat_type_id),
    CONSTRAINT uq_seat_type_code UNIQUE (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. GENRE
CREATE TABLE GENRE (
    genre_id BIGINT AUTO_INCREMENT,
    genre_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_genre PRIMARY KEY (genre_id),
    CONSTRAINT uq_genre_code UNIQUE (genre_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. LANGUAGE
CREATE TABLE LANGUAGE (
    language_id BIGINT AUTO_INCREMENT,
    language_code VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_language PRIMARY KEY (language_id),
    CONSTRAINT uq_language_code UNIQUE (language_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. CERTIFICATION
CREATE TABLE CERTIFICATION (
    certification_id BIGINT AUTO_INCREMENT,
    certification_code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL DEFAULT NULL,
    CONSTRAINT pk_certification PRIMARY KEY (certification_id),
    CONSTRAINT uq_certification_code UNIQUE (certification_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. PRESENTATION_FORMAT
CREATE TABLE PRESENTATION_FORMAT (
    presentation_format_id BIGINT AUTO_INCREMENT,
    format_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL DEFAULT NULL,
    CONSTRAINT pk_presentation_format PRIMARY KEY (presentation_format_id),
    CONSTRAINT uq_presentation_format_code UNIQUE (format_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. AUDIO_FORMAT
CREATE TABLE AUDIO_FORMAT (
    audio_format_id BIGINT AUTO_INCREMENT,
    format_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL DEFAULT NULL,
    CONSTRAINT pk_audio_format PRIMARY KEY (audio_format_id),
    CONSTRAINT uq_audio_format_code UNIQUE (format_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TIER 2: DEPENDENT PROFILES, REFERENCE MAPPINGS & MASTER ENTITIES
-- =============================================================================

-- 2. CUSTOMER_PROFILE
CREATE TABLE CUSTOMER_PROFILE (
    user_id BIGINT NOT NULL,
    preferred_language_id BIGINT NULL DEFAULT NULL,
    date_of_birth DATE NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_customer_profile PRIMARY KEY (user_id),
    CONSTRAINT fk_customer_profile_user FOREIGN KEY (user_id) 
        REFERENCES USER(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_customer_profile_pref_lang FOREIGN KEY (preferred_language_id) 
        REFERENCES LANGUAGE(language_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_customer_profile_pref_lang ON CUSTOMER_PROFILE (preferred_language_id);

-- 3. EMPLOYEE_PROFILE
CREATE TABLE EMPLOYEE_PROFILE (
    user_id BIGINT NOT NULL,
    employee_code VARCHAR(50) NOT NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_employee_profile PRIMARY KEY (user_id),
    CONSTRAINT uq_employee_code UNIQUE (employee_code),
    CONSTRAINT fk_employee_profile_user FOREIGN KEY (user_id) 
        REFERENCES USER(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_employee_profile_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. USER_ROLE
CREATE TABLE USER_ROLE (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) 
        REFERENCES USER(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) 
        REFERENCES ROLE(role_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_user_role_status CHECK (status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_role_role ON USER_ROLE (role_id);

-- 7. ROLE_PERMISSION
CREATE TABLE ROLE_PERMISSION (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT pk_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) 
        REFERENCES ROLE(role_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_role_permission_perm FOREIGN KEY (permission_id) 
        REFERENCES PERMISSION(permission_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_role_permission_perm ON ROLE_PERMISSION (permission_id);

-- 9. THEATRE
CREATE TABLE THEATRE (
    theatre_id BIGINT AUTO_INCREMENT,
    city_id BIGINT NOT NULL,
    theatre_code VARCHAR(50) NOT NULL,
    theatre_name VARCHAR(150) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255) NULL DEFAULT NULL,
    postal_code VARCHAR(20) NULL DEFAULT NULL,
    latitude DECIMAL(9,6) NULL DEFAULT NULL,
    longitude DECIMAL(9,6) NULL DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_theatre PRIMARY KEY (theatre_id),
    CONSTRAINT uq_theatre_code UNIQUE (theatre_code),
    CONSTRAINT fk_theatre_city FOREIGN KEY (city_id) 
        REFERENCES CITY(city_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_theatre_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_theatre_city ON THEATRE (city_id);

-- 14. MOVIE
CREATE TABLE MOVIE (
    movie_id BIGINT AUTO_INCREMENT,
    certification_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255) NULL DEFAULT NULL,
    synopsis TEXT NULL DEFAULT NULL,
    runtime_minutes SMALLINT NOT NULL,
    release_date DATE NULL DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    poster_url VARCHAR(1000) NULL DEFAULT NULL,
    trailer_url VARCHAR(1000) NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_movie PRIMARY KEY (movie_id),
    CONSTRAINT fk_movie_certification FOREIGN KEY (certification_id) 
        REFERENCES CERTIFICATION(certification_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_movie_runtime CHECK (runtime_minutes > 0),
    CONSTRAINT chk_movie_status CHECK (status IN ('UPCOMING', 'AIRING', 'ENDED', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_movie_certification ON MOVIE (certification_id);

-- 16. MOVIE_GENRE
CREATE TABLE MOVIE_GENRE (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    CONSTRAINT pk_movie_genre PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genre_movie FOREIGN KEY (movie_id) 
        REFERENCES MOVIE(movie_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_movie_genre_genre FOREIGN KEY (genre_id) 
        REFERENCES GENRE(genre_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_movie_genre_genre ON MOVIE_GENRE (genre_id);

-- 18. MOVIE_LANGUAGE
CREATE TABLE MOVIE_LANGUAGE (
    movie_language_id BIGINT AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    language_id BIGINT NOT NULL,
    language_role VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
    CONSTRAINT pk_movie_language PRIMARY KEY (movie_language_id),
    CONSTRAINT uq_movie_lang_role UNIQUE (movie_id, language_id, language_role),
    CONSTRAINT fk_movie_language_movie FOREIGN KEY (movie_id) 
        REFERENCES MOVIE(movie_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_movie_language_language FOREIGN KEY (language_id) 
        REFERENCES LANGUAGE(language_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_movie_language_role CHECK (language_role IN ('ORIGINAL', 'DUBBED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_movie_lang_movie ON MOVIE_LANGUAGE (movie_id);
CREATE INDEX idx_movie_lang_language ON MOVIE_LANGUAGE (language_id);

-- =============================================================================
-- TIER 3: PHYSICAL INFRASTRUCTURE & MULTIPLEX CONFIGURATION
-- =============================================================================

-- 10. EMPLOYEE_THEATRE
CREATE TABLE EMPLOYEE_THEATRE (
    user_id BIGINT NOT NULL,
    theatre_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_employee_theatre PRIMARY KEY (user_id, theatre_id),
    CONSTRAINT fk_employee_theatre_emp FOREIGN KEY (user_id) 
        REFERENCES EMPLOYEE_PROFILE(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_employee_theatre_th FOREIGN KEY (theatre_id) 
        REFERENCES THEATRE(theatre_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_employee_theatre_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_employee_theatre_theatre ON EMPLOYEE_THEATRE (theatre_id);

-- 11. SCREEN
CREATE TABLE SCREEN (
    screen_id BIGINT AUTO_INCREMENT,
    theatre_id BIGINT NOT NULL,
    screen_code VARCHAR(50) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_screen PRIMARY KEY (screen_id),
    CONSTRAINT uq_theatre_screen_code UNIQUE (theatre_id, screen_code),
    CONSTRAINT fk_screen_theatre FOREIGN KEY (theatre_id) 
        REFERENCES THEATRE(theatre_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_screen_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_screen_theatre ON SCREEN (theatre_id);

-- 22. SCREEN_CAPABILITY
CREATE TABLE SCREEN_CAPABILITY (
    screen_capability_id BIGINT AUTO_INCREMENT,
    screen_id BIGINT NOT NULL,
    presentation_format_id BIGINT NOT NULL,
    audio_format_id BIGINT NOT NULL,
    CONSTRAINT pk_screen_capability PRIMARY KEY (screen_capability_id),
    CONSTRAINT uq_screen_capability UNIQUE (screen_id, presentation_format_id, audio_format_id),
    CONSTRAINT fk_screen_cap_screen FOREIGN KEY (screen_id) 
        REFERENCES SCREEN(screen_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_screen_cap_pres FOREIGN KEY (presentation_format_id) 
        REFERENCES PRESENTATION_FORMAT(presentation_format_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_screen_cap_audio FOREIGN KEY (audio_format_id) 
        REFERENCES AUDIO_FORMAT(audio_format_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_screen_cap_screen ON SCREEN_CAPABILITY (screen_id);
CREATE INDEX idx_screen_cap_format ON SCREEN_CAPABILITY (presentation_format_id);
CREATE INDEX idx_screen_cap_audio ON SCREEN_CAPABILITY (audio_format_id);

-- 13. SEAT
CREATE TABLE SEAT (
    seat_id BIGINT AUTO_INCREMENT,
    screen_id BIGINT NOT NULL,
    seat_type_id BIGINT NOT NULL,
    row_label VARCHAR(20) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    position_x DECIMAL(10,3) NULL DEFAULT NULL,
    position_y DECIMAL(10,3) NULL DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_seat PRIMARY KEY (seat_id),
    CONSTRAINT uq_screen_row_seat UNIQUE (screen_id, row_label, seat_number),
    CONSTRAINT fk_seat_screen FOREIGN KEY (screen_id) 
        REFERENCES SCREEN(screen_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_seat_seat_type FOREIGN KEY (seat_type_id) 
        REFERENCES SEAT_TYPE(seat_type_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_seat_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_seat_screen ON SEAT (screen_id);
CREATE INDEX idx_seat_seat_type ON SEAT (seat_type_id);

-- =============================================================================
-- TIER 4: SCHEDULING & AVAILABILITY
-- =============================================================================

-- 23. SHOW
CREATE TABLE `SHOW` (
    show_id BIGINT AUTO_INCREMENT,
    movie_language_id BIGINT NOT NULL,
    screen_capability_id BIGINT NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_show PRIMARY KEY (show_id),
    CONSTRAINT fk_show_movie_language FOREIGN KEY (movie_language_id) 
        REFERENCES MOVIE_LANGUAGE(movie_language_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_show_screen_capability FOREIGN KEY (screen_capability_id) 
        REFERENCES SCREEN_CAPABILITY(screen_capability_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_show_timing CHECK (end_at > start_at),
    CONSTRAINT chk_show_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_show_screen_cap_start ON `SHOW` (screen_capability_id, start_at);
CREATE INDEX idx_show_movie_lang_start ON `SHOW` (movie_language_id, start_at);
CREATE INDEX idx_show_status_start ON `SHOW` (status, start_at);

-- 24. SHOW_SEAT
CREATE TABLE SHOW_SEAT (
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT pk_show_seat PRIMARY KEY (show_id, seat_id),
    CONSTRAINT fk_show_seat_show FOREIGN KEY (show_id) 
        REFERENCES `SHOW`(show_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_show_seat_seat FOREIGN KEY (seat_id) 
        REFERENCES SEAT(seat_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_show_seat_status CHECK (availability_status IN ('AVAILABLE', 'BOOKED', 'BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_show_seat_status ON SHOW_SEAT (show_id, availability_status);

-- =============================================================================
-- TIER 5: DERIVED PROJECTIONS, SEARCH TELEMETRY & GOVERNANCE
-- =============================================================================

-- 25. SEARCH_INDEX_DOCUMENT
CREATE TABLE SEARCH_INDEX_DOCUMENT (
    document_id BIGINT AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    searchable_text TEXT NULL DEFAULT NULL,
    metadata_json JSON NULL DEFAULT NULL,
    embedding_reference VARCHAR(500) NULL DEFAULT NULL,
    indexed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    index_version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_search_index_document PRIMARY KEY (document_id),
    CONSTRAINT uq_search_index_doc_entity UNIQUE (entity_type, entity_id),
    CONSTRAINT chk_search_index_doc_status CHECK (status IN ('ACTIVE', 'STALE', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_search_index_doc_status ON SEARCH_INDEX_DOCUMENT (status);

-- 26. SEARCH_QUERY
CREATE TABLE SEARCH_QUERY (
    search_query_id BIGINT AUTO_INCREMENT,
    user_id BIGINT NULL DEFAULT NULL,
    query_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_count INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_search_query PRIMARY KEY (search_query_id),
    CONSTRAINT fk_search_query_user FOREIGN KEY (user_id) 
        REFERENCES USER(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_search_query_user_created ON SEARCH_QUERY (user_id, created_at);

-- 27. SEARCH_RESULT
CREATE TABLE SEARCH_RESULT (
    search_result_id BIGINT AUTO_INCREMENT,
    search_query_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    rank_position INT NOT NULL,
    relevance_score DECIMAL(8,6) NULL DEFAULT NULL,
    retrieval_method VARCHAR(50) NOT NULL,
    CONSTRAINT pk_search_result PRIMARY KEY (search_result_id),
    CONSTRAINT fk_search_result_query FOREIGN KEY (search_query_id) 
        REFERENCES SEARCH_QUERY(search_query_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_search_result_method CHECK (retrieval_method IN ('LEXICAL', 'SEMANTIC', 'HYBRID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_search_result_query_rank ON SEARCH_RESULT (search_query_id, rank_position);

-- 28. AUDIT_LOG
CREATE TABLE AUDIT_LOG (
    audit_log_id BIGINT AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value JSON NULL DEFAULT NULL,
    new_value JSON NULL DEFAULT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (audit_log_id),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) 
        REFERENCES USER(user_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_actor ON AUDIT_LOG (actor_user_id);
CREATE INDEX idx_audit_entity ON AUDIT_LOG (entity_type, entity_id);
CREATE INDEX idx_audit_occurred ON AUDIT_LOG (occurred_at);
