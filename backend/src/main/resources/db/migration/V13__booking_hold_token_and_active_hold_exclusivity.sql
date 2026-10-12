-- V13: Booking hold token association and active hold exclusivity
-- Purpose:
-- 1. Add hold_token column and unique constraint to booking table for checkout idempotency.
-- 2. Create active_seat_hold table to enforce database-level single active hold per show-seat.

-- 1. Add hold_token to booking
ALTER TABLE booking ADD COLUMN hold_token VARCHAR(64) NULL AFTER booking_reference;
ALTER TABLE booking ADD CONSTRAINT uq_booking_hold_token UNIQUE (hold_token);

-- 2. Dedicated active_seat_hold table
CREATE TABLE IF NOT EXISTS active_seat_hold (
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    hold_id BIGINT NOT NULL,
    hold_token VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (show_id, seat_id),
    KEY idx_active_hold_token (hold_token),
    KEY idx_active_hold_expires (expires_at),
    CONSTRAINT fk_active_hold_show FOREIGN KEY (show_id) REFERENCES `show` (show_id) ON DELETE CASCADE,
    CONSTRAINT fk_active_hold_seat FOREIGN KEY (seat_id) REFERENCES seat (seat_id) ON DELETE RESTRICT,
    CONSTRAINT fk_active_hold_hold FOREIGN KEY (hold_id) REFERENCES seat_hold (hold_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
