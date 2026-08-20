-- ===================================================================
-- AI DECISION ENGINE FOR INTELLIGENT BOOKING
-- Initial Development Seed Script (MySQL 8.0+)
-- Source of Truth: System Design & Multi-Branch Architecture
-- Brand: PVK Cinemas
-- ===================================================================

USE `intelligent_booking_db`;

-- 1. Roles
INSERT INTO `role` (`role_id`, `role_name`) VALUES 
(1, 'ROLE_CUSTOMER'),
(2, 'ROLE_SERVICE_PROVIDER'),
(3, 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- 2. Users
-- BCrypt hash for 'password123': $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a
INSERT INTO `user` (`user_id`, `role_id`, `name`, `email`, `password_hash`, `status`) VALUES 
(1, 1, 'John Customer', 'customer@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ACTIVE'),
(2, 2, 'PVK Cinema Operator', 'provider@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ACTIVE'),
(3, 3, 'System Administrator', 'admin@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `password_hash` = VALUES(`password_hash`);

-- 3. PVK Theatre Branches (ALL owned directly by User 2: provider@example.com)
INSERT INTO `theatre` (`theatre_id`, `owner_user_id`, `name`, `location`, `address`, `status`) VALUES 
(1, 2, 'PVK — Anna Nagar', 'Chennai', '2nd Avenue, Anna Nagar, Chennai', 'ACTIVE'),
(2, 2, 'PVK — OMR', 'Chennai', 'Rajiv Gandhi Salai, OMR, Chennai', 'ACTIVE'),
(3, 2, 'PVK — Velachery', 'Chennai', 'Phoenix Marketcity, Velachery, Chennai', 'ACTIVE'),
(4, 2, 'PVK — T. Nagar', 'Chennai', 'GN Chetty Road, T. Nagar, Chennai', 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `location` = VALUES(`location`), `owner_user_id` = VALUES(`owner_user_id`);

-- 4. Screens per PVK Branch
INSERT INTO `screen` (`screen_id`, `theatre_id`, `name`, `capacity`) VALUES 
(1, 1, 'Screen 1 — Standard', 60),
(2, 1, 'Screen 2 — Standard', 60),
(3, 1, 'IMAX Screen', 100),
(4, 2, 'Screen 1 — Standard', 60),
(5, 2, 'Screen 2 — Standard', 60),
(6, 2, 'Premium Screen', 80),
(7, 3, 'Screen 1 — Standard', 60),
(8, 3, 'Screen 2 — Premium', 80),
(9, 3, 'Screen 3 — Standard', 60),
(10, 4, 'Screen 1 — Standard', 60),
(11, 4, 'Screen 2 — Premium', 80)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `capacity` = VALUES(`capacity`);

-- 5. Physical Seats Layout for Screen 1 (Rows A-F, 10 Seats per Row)
INSERT IGNORE INTO `seat` (`screen_id`, `row_label`, `seat_number`, `seat_type`) VALUES
-- PVK Anna Nagar Screen 1
(1, 'A', 1, 'REGULAR'), (1, 'A', 2, 'REGULAR'), (1, 'A', 3, 'REGULAR'), (1, 'A', 4, 'REGULAR'), (1, 'A', 5, 'REGULAR'),
(1, 'A', 6, 'REGULAR'), (1, 'A', 7, 'REGULAR'), (1, 'A', 8, 'REGULAR'), (1, 'A', 9, 'REGULAR'), (1, 'A', 10, 'REGULAR'),
(1, 'B', 1, 'REGULAR'), (1, 'B', 2, 'REGULAR'), (1, 'B', 3, 'REGULAR'), (1, 'B', 4, 'REGULAR'), (1, 'B', 5, 'REGULAR'),
(1, 'B', 6, 'REGULAR'), (1, 'B', 7, 'REGULAR'), (1, 'B', 8, 'REGULAR'), (1, 'B', 9, 'REGULAR'), (1, 'B', 10, 'REGULAR'),
(1, 'C', 1, 'PREMIUM'), (1, 'C', 2, 'PREMIUM'), (1, 'C', 3, 'PREMIUM'), (1, 'C', 4, 'PREMIUM'), (1, 'C', 5, 'PREMIUM'),
(1, 'C', 6, 'PREMIUM'), (1, 'C', 7, 'PREMIUM'), (1, 'C', 8, 'PREMIUM'), (1, 'C', 9, 'PREMIUM'), (1, 'C', 10, 'PREMIUM'),
(1, 'D', 1, 'PREMIUM'), (1, 'D', 2, 'PREMIUM'), (1, 'D', 3, 'PREMIUM'), (1, 'D', 4, 'PREMIUM'), (1, 'D', 5, 'PREMIUM'),
(1, 'D', 6, 'PREMIUM'), (1, 'D', 7, 'PREMIUM'), (1, 'D', 8, 'PREMIUM'), (1, 'D', 9, 'PREMIUM'), (1, 'D', 10, 'PREMIUM'),
(1, 'E', 1, 'BALCONY'), (1, 'E', 2, 'BALCONY'), (1, 'E', 3, 'BALCONY'), (1, 'E', 4, 'BALCONY'), (1, 'E', 5, 'BALCONY'),
(1, 'E', 6, 'BALCONY'), (1, 'E', 7, 'BALCONY'), (1, 'E', 8, 'BALCONY'), (1, 'E', 9, 'BALCONY'), (1, 'E', 10, 'BALCONY'),
(1, 'F', 1, 'BALCONY'), (1, 'F', 2, 'BALCONY'), (1, 'F', 3, 'BALCONY'), (1, 'F', 4, 'BALCONY'), (1, 'F', 5, 'BALCONY'),
(1, 'F', 6, 'BALCONY'), (1, 'F', 7, 'BALCONY'), (1, 'F', 8, 'BALCONY'), (1, 'F', 9, 'BALCONY'), (1, 'F', 10, 'BALCONY');

-- 6. Movies
INSERT INTO `movie` (`movie_id`, `title`, `genre`, `language`, `duration`, `release_date`, `status`) VALUES 
(1, 'Cyber Odyssey 2099', 'Sci-Fi / Thriller', 'English', 145, '2026-08-01', 'ACTIVE'),
(2, 'The Midnight Cipher', 'Mystery / Drama', 'English', 120, '2026-08-10', 'ACTIVE'),
(3, 'Neon Horizon', 'Action / Cyberpunk', 'Tamil', 135, '2026-08-12', 'ACTIVE'),
(4, 'Shadow Protocol', 'Spy / Thriller', 'Hindi', 150, '2026-08-15', 'ACTIVE'),
(5, 'Beyond the Stars', 'Space / Adventure', 'English', 160, '2026-08-18', 'ACTIVE')
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `genre` = VALUES(`genre`);

-- 7. Shows (Scheduled Sessions across PVK Branches in INR ₹)
INSERT INTO `shows` (`show_id`, `movie_id`, `screen_id`, `show_date`, `start_time`, `end_time`, `ticket_price`, `status`) VALUES 
-- PVK Anna Nagar
(1, 1, 1, CURRENT_DATE(), '10:00:00', '12:25:00', 180.00, 'ACTIVE'),
(2, 1, 2, CURRENT_DATE(), '14:00:00', '16:25:00', 180.00, 'ACTIVE'),
(3, 1, 3, CURRENT_DATE(), '19:30:00', '21:55:00', 380.00, 'ACTIVE'),
-- PVK OMR
(4, 1, 4, CURRENT_DATE(), '11:00:00', '13:25:00', 200.00, 'ACTIVE'),
(5, 1, 6, CURRENT_DATE(), '17:00:00', '19:25:00', 250.00, 'ACTIVE'),
-- PVK Velachery
(6, 1, 8, CURRENT_DATE(), '15:30:00', '17:55:00', 240.00, 'ACTIVE'),
(7, 3, 7, CURRENT_DATE(), '10:30:00', '12:45:00', 180.00, 'ACTIVE'),
-- PVK T. Nagar
(8, 2, 10, CURRENT_DATE(), '11:30:00', '13:30:00', 180.00, 'ACTIVE'),
(9, 1, 11, CURRENT_DATE(), '18:30:00', '20:55:00', 240.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE `ticket_price` = VALUES(`ticket_price`), `show_date` = VALUES(`show_date`);

-- 8. Show Seats Inventory for All Shows
INSERT INTO `show_seat` (`show_id`, `seat_id`, `status`, `price`, `version`)
SELECT sh.show_id, s.seat_id, 'AVAILABLE', 
       CASE 
           WHEN s.seat_type = 'BALCONY' THEN sh.ticket_price + 70.00 
           WHEN s.seat_type = 'PREMIUM' THEN sh.ticket_price + 40.00 
           ELSE sh.ticket_price 
       END, 0
FROM `shows` sh
JOIN `seat` s ON s.screen_id = sh.screen_id
WHERE NOT EXISTS (
    SELECT 1 FROM `show_seat` ss WHERE ss.show_id = sh.show_id AND ss.seat_id = s.seat_id
);

-- 9. User Preferences for Customer (User ID 1)
INSERT INTO `user_preference` (`user_id`, `budget_limit`, `preferred_time`, `preferred_seat_type`, `group_size`) VALUES 
(1, 250.00, 'evening', 'PREMIUM', 2)
ON DUPLICATE KEY UPDATE `budget_limit` = VALUES(`budget_limit`);
