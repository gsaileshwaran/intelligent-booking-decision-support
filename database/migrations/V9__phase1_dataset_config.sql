-- =============================================================================
-- PVK CINEMAS — V9__phase1_dataset_config.sql
-- Phase 1: Dataset Configuration & Versioning Table
-- Dataset Version : 2026.10
-- Deterministic Seed: PVK-PHASE1-SEED-42
-- =============================================================================
-- This migration introduces the DATASET_CONFIG table for tracking the active
-- demo dataset version, seed, and reset metadata.
-- It does NOT regenerate master data — V5/V6 are authoritative for that.
-- =============================================================================

CREATE TABLE IF NOT EXISTS DATASET_CONFIG (
    config_key   VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description  VARCHAR(500) NULL DEFAULT NULL,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_dataset_config PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='PVK Cinemas demo dataset version and seed configuration';

-- Insert Phase 1 dataset configuration (idempotent via ON DUPLICATE KEY UPDATE)
INSERT INTO DATASET_CONFIG (config_key, config_value, description) VALUES
('dataset.version',      '2026.10',                    'PVK Demo Dataset version identifier'),
('dataset.seed',         'PVK-PHASE1-SEED-42',         'Deterministic seed value for reproducible master data generation'),
('dataset.phase',        '1',                          'Current implementation phase (1 = master/reference data only)'),
('dataset.reset_count',  '0',                          'Number of times the demo reset has been executed'),
('dataset.last_reset',   'NEVER',                      'ISO timestamp of the last successful demo reset'),
('dataset.description',  'PVK Cinemas Phase 1 Demo: 5 Cities | 25 Theatres | 125 Screens | 15000 Seats | 3 Demo Users',
                                                       'Human-readable summary of this dataset'),
('dataset.cities',       '5',                          'Expected city count for Phase 1 validation'),
('dataset.theatres',     '25',                         'Expected theatre count for Phase 1 validation'),
('dataset.screens',      '125',                        'Expected screen count for Phase 1 validation'),
('dataset.seats_min',    '15000',                      'Minimum expected seat count for Phase 1 validation'),
('dataset.theatres_per_city', '5',                     'Expected theatres per city for Phase 1 validation'),
('dataset.demo_admin',   'admin@pvkcinemas.com',       'Super Admin demo account'),
('dataset.demo_manager', 'manager@pvkcinemas.com',     'Theatre Manager demo account'),
('dataset.demo_customer','customer@pvkcinemas.com',    'Customer demo account'),
('dataset.manager_theatre_code', 'DEMO_CHN_CINEPLEX',  'Theatre code assigned to demo Theatre Manager')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description  = VALUES(description);

-- =============================================================================
-- END V9
-- =============================================================================
