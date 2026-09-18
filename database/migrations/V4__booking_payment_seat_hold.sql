-- =============================================================================
-- PVK CINEMAS SCHEMA MIGRATION V4: BOOKING, SEAT HOLD & SIMULATED PAYMENT
-- =============================================================================

-- Update demo personas to canonical names
UPDATE USER SET first_name = 'Arun', last_name = 'Sharma' WHERE email = 'admin@pvkcinemas.com';
UPDATE USER SET first_name = 'Ravi', last_name = 'Kumar' WHERE email = 'manager@pvkcinemas.com';
UPDATE USER SET first_name = 'Kavya', last_name = 'Reddy' WHERE email = 'customer@pvkcinemas.com';

-- Update SHOW_SEAT constraint to support HELD
ALTER TABLE SHOW_SEAT DROP CHECK chk_show_seat_status;
ALTER TABLE SHOW_SEAT ADD CONSTRAINT chk_show_seat_status CHECK (availability_status IN ('AVAILABLE', 'BOOKED', 'BLOCKED', 'HELD'));

-- 1. SEAT_HOLD
CREATE TABLE IF NOT EXISTS SEAT_HOLD (
    hold_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hold_token VARCHAR(64) NOT NULL,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_seat_hold_show FOREIGN KEY (show_id) REFERENCES `SHOW` (show_id) ON DELETE CASCADE,
    CONSTRAINT fk_seat_hold_seat FOREIGN KEY (seat_id) REFERENCES SEAT (seat_id) ON DELETE RESTRICT,
    CONSTRAINT fk_seat_hold_user FOREIGN KEY (user_id) REFERENCES USER (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_seat_hold_token_seat UNIQUE (hold_token, seat_id),
    INDEX idx_seat_hold_show_seat (show_id, seat_id),
    INDEX idx_seat_hold_token (hold_token),
    INDEX idx_seat_hold_expires (expires_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BOOKING
CREATE TABLE IF NOT EXISTS BOOKING (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES USER (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_show FOREIGN KEY (show_id) REFERENCES `SHOW` (show_id) ON DELETE RESTRICT,
    INDEX idx_booking_user (user_id),
    INDEX idx_booking_show (show_id),
    INDEX idx_booking_status (booking_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BOOKING_SEAT
CREATE TABLE IF NOT EXISTS BOOKING_SEAT (
    booking_seat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_booking_seat_booking FOREIGN KEY (booking_id) REFERENCES BOOKING (booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seat_seat FOREIGN KEY (seat_id) REFERENCES SEAT (seat_id) ON DELETE RESTRICT,
    CONSTRAINT uq_booking_seat UNIQUE (booking_id, seat_id),
    INDEX idx_booking_seat_seat (seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. PAYMENT (Simulated dummy payment record)
CREATE TABLE IF NOT EXISTS PAYMENT (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(64) NOT NULL UNIQUE,
    booking_id BIGINT NULL DEFAULT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    simulated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES BOOKING (booking_id) ON DELETE CASCADE,
    INDEX idx_payment_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. NEW PERMISSIONS & ROLE-PERMISSION MAPPINGS
INSERT INTO PERMISSION (permission_code, name, description) VALUES
('BOOKING_CREATE', 'Create Booking', 'Hold seats and create simulated customer bookings'),
('BOOKING_READ', 'View Bookings', 'View personal or platform booking history'),
('PAYMENT_PROCESS', 'Process Dummy Payment', 'Execute simulated payment transactions')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Map to ROLE_SUPER_ADMIN
INSERT IGNORE INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_code = 'ROLE_SUPER_ADMIN'
  AND p.permission_code IN ('BOOKING_CREATE', 'BOOKING_READ', 'PAYMENT_PROCESS');

-- Map to ROLE_CUSTOMER
INSERT IGNORE INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_code = 'ROLE_CUSTOMER'
  AND p.permission_code IN ('BOOKING_CREATE', 'BOOKING_READ', 'PAYMENT_PROCESS');
