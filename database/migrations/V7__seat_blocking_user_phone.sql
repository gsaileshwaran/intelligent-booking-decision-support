-- ============================================================================
-- V7: Constraint adjustments for seat blocking and user provisioning
-- ============================================================================
-- 1. Allow 'BLOCKED' in seat.status constraint (for manager seat blocking)
-- 2. Make user.phone nullable (for admin-provisioned users without phone)
-- ============================================================================

-- 1. Seat status: allow ACTIVE, INACTIVE, BLOCKED
ALTER TABLE seat DROP CONSTRAINT chk_seat_status;
ALTER TABLE seat ADD CONSTRAINT chk_seat_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

-- 2. User phone: make nullable and drop NOT NULL constraint
ALTER TABLE `user` MODIFY COLUMN phone VARCHAR(30) NULL DEFAULT NULL;
