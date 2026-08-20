-- ===================================================================
-- AI DECISION ENGINE FOR INTELLIGENT BOOKING
-- Relational Database DDL Schema (MySQL 8.0+)
-- Source of Truth: Week 2 System Design Document (Section 30)
-- ===================================================================

CREATE DATABASE IF NOT EXISTS `intelligent_booking_db` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `intelligent_booking_db`;

-- Drop tables in reverse foreign key order
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `recommendation_feedback`;
DROP TABLE IF EXISTS `recommendation_item`;
DROP TABLE IF EXISTS `recommendation`;
DROP TABLE IF EXISTS `user_preference`;
DROP TABLE IF EXISTS `payment`;
DROP TABLE IF EXISTS `booking_item`;
DROP TABLE IF EXISTS `booking`;
DROP TABLE IF EXISTS `show_seat`;
DROP TABLE IF EXISTS `show_table`; -- handles name variations
DROP TABLE IF EXISTS `shows`;
DROP TABLE IF EXISTS `movie`;
DROP TABLE IF EXISTS `seat`;
DROP TABLE IF EXISTS `screen`;
DROP TABLE IF EXISTS `theatre`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `role`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. ROLE
CREATE TABLE `role` (
    `role_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_name` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. USER
CREATE TABLE `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(120) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. THEATRE
-- Theatre ownership is represented directly through owner_user_id referencing user.user_id
CREATE TABLE `theatre` (
    `theatre_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `owner_user_id` BIGINT NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `location` VARCHAR(150) NOT NULL,
    `address` VARCHAR(255) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT `fk_theatre_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. SCREEN
CREATE TABLE `screen` (
    `screen_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `theatre_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `capacity` INT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_screen_theatre` FOREIGN KEY (`theatre_id`) REFERENCES `theatre` (`theatre_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. SEAT (Physical Seat Matrix per Screen)
CREATE TABLE `seat` (
    `seat_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `screen_id` BIGINT NOT NULL,
    `row_label` VARCHAR(10) NOT NULL,
    `seat_number` INT NOT NULL,
    `seat_type` VARCHAR(30) NOT NULL DEFAULT 'REGULAR', -- e.g. REGULAR, PREMIUM, BALCONY, VIP
    CONSTRAINT `fk_seat_screen` FOREIGN KEY (`screen_id`) REFERENCES `screen` (`screen_id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_seat_position` (`screen_id`, `row_label`, `seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. MOVIE
CREATE TABLE `movie` (
    `movie_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `genre` VARCHAR(100) NULL,
    `language` VARCHAR(50) NULL,
    `duration` INT NULL, -- duration in minutes
    `release_date` DATE NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. SHOW (Scheduled Sessions)
CREATE TABLE `shows` (
    `show_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `movie_id` BIGINT NOT NULL,
    `screen_id` BIGINT NOT NULL,
    `show_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `ticket_price` DECIMAL(10, 2) NOT NULL, -- base/default show price
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT `fk_show_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie` (`movie_id`),
    CONSTRAINT `fk_show_screen` FOREIGN KEY (`screen_id`) REFERENCES `screen` (`screen_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. SHOW_SEAT (Show-Specific Inventory State & Holds)
CREATE TABLE `show_seat` (
    `show_seat_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `show_id` BIGINT NOT NULL,
    `seat_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, HELD, CONFIRMED/BOOKED
    `price` DECIMAL(10, 2) NOT NULL,
    `held_until` TIMESTAMP NULL,
    `version` INT DEFAULT 0,
    CONSTRAINT `fk_show_seat_show` FOREIGN KEY (`show_id`) REFERENCES `shows` (`show_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_show_seat_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`seat_id`),
    UNIQUE KEY `uk_show_seat_instance` (`show_id`, `seat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. BOOKING (Booking Lifecycle: PENDING, HELD, CONFIRMED, CANCELLED, EXPIRED)
CREATE TABLE `booking` (
    `booking_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `booking_ref` VARCHAR(50) NOT NULL UNIQUE,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. BOOKING_ITEM (Line Items linking Booking to ShowSeat)
CREATE TABLE `booking_item` (
    `booking_item_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` BIGINT NOT NULL,
    `show_seat_id` BIGINT NOT NULL,
    `price` DECIMAL(10, 2) NOT NULL,
    CONSTRAINT `fk_booking_item_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_booking_item_show_seat` FOREIGN KEY (`show_seat_id`) REFERENCES `show_seat` (`show_seat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. PAYMENT (Payment Lifecycle: PENDING, SUCCESS, FAILED, REFUNDED)
CREATE TABLE `payment` (
    `payment_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` BIGINT NOT NULL,
    `amount` DECIMAL(10, 2) NOT NULL,
    `payment_method` VARCHAR(50) NOT NULL,
    `transaction_ref` VARCHAR(100) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT `fk_payment_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. USER_PREFERENCE
CREATE TABLE `user_preference` (
    `preference_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `budget_limit` DECIMAL(10, 2) NULL,
    `preferred_time` VARCHAR(50) NULL,
    `preferred_seat_type` VARCHAR(50) NULL,
    `group_size` INT NULL DEFAULT 1,
    CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. RECOMMENDATION
CREATE TABLE `recommendation` (
    `recommendation_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `request_context` JSON NULL,
    `model_version` VARCHAR(50) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_recommendation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. RECOMMENDATION_ITEM
CREATE TABLE `recommendation_item` (
    `recommendation_item_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recommendation_id` BIGINT NOT NULL,
    `show_id` BIGINT NOT NULL,
    `rank` INT NOT NULL,
    `suitability_score` DECIMAL(5, 4) NOT NULL,
    `reason_data` JSON NULL,
    CONSTRAINT `fk_rec_item_recommendation` FOREIGN KEY (`recommendation_id`) REFERENCES `recommendation` (`recommendation_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rec_item_show` FOREIGN KEY (`show_id`) REFERENCES `shows` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. RECOMMENDATION_FEEDBACK
CREATE TABLE `recommendation_feedback` (
    `feedback_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recommendation_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `selected_show_id` BIGINT NULL,
    `feedback_type` VARCHAR(50) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_feedback_rec` FOREIGN KEY (`recommendation_id`) REFERENCES `recommendation` (`recommendation_id`),
    CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_feedback_show` FOREIGN KEY (`selected_show_id`) REFERENCES `shows` (`show_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
