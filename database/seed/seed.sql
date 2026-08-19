-- ===================================================================
-- AI DECISION ENGINE FOR INTELLIGENT BOOKING
-- Initial Development Seed Script (MySQL 8.0+)
-- Source of Truth: Week 2 System Design Document
-- ===================================================================

USE `intelligent_booking_db`;

-- 1. Roles
INSERT INTO `role` (`role_id`, `role_name`) VALUES 
(1, 'ROLE_CUSTOMER'),
(2, 'ROLE_SERVICE_PROVIDER'),
(3, 'ROLE_ADMIN');

-- 2. Users
-- BCrypt hash for 'password123': $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a
INSERT INTO `user` (`user_id`, `role_id`, `name`, `email`, `password_hash`, `status`) VALUES 
(1, 1, 'John Customer', 'customer@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ACTIVE'),
(2, 2, 'Cineplex Operator', 'provider@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ACTIVE'),
(3, 3, 'System Administrator', 'admin@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ACTIVE');

-- 3. Theatres (Owned directly by User 2 - Service Provider)
INSERT INTO `theatre` (`theatre_id`, `owner_user_id`, `name`, `location`, `address`, `status`) VALUES 
(1, 2, 'Grand Cinema Downtown', 'Metropolis Central', '123 Main Boulevard, Downtown', 'ACTIVE'),
(2, 2, 'Grand Cinema Westside IMAX', 'Westside Mall', '456 West Avenue, Shopping District', 'ACTIVE');

-- 4. Screens
INSERT INTO `screen` (`screen_id`, `theatre_id`, `name`, `capacity`) VALUES 
(1, 1, 'Auditorium 1', 30),
(2, 1, 'Auditorium 2 (VIP)', 20),
(3, 2, 'IMAX Hall A', 40);

-- 5. Physical Seats Layout for Screen 1 (30 Seats: Rows A, B, C)
INSERT INTO `seat` (`screen_id`, `row_label`, `seat_number`, `seat_type`) VALUES
-- Row A (Regular)
(1, 'A', 1, 'REGULAR'), (1, 'A', 2, 'REGULAR'), (1, 'A', 3, 'REGULAR'), (1, 'A', 4, 'REGULAR'), (1, 'A', 5, 'REGULAR'),
(1, 'A', 6, 'REGULAR'), (1, 'A', 7, 'REGULAR'), (1, 'A', 8, 'REGULAR'), (1, 'A', 9, 'REGULAR'), (1, 'A', 10, 'REGULAR'),
-- Row B (Premium)
(1, 'B', 1, 'PREMIUM'), (1, 'B', 2, 'PREMIUM'), (1, 'B', 3, 'PREMIUM'), (1, 'B', 4, 'PREMIUM'), (1, 'B', 5, 'PREMIUM'),
(1, 'B', 6, 'PREMIUM'), (1, 'B', 7, 'PREMIUM'), (1, 'B', 8, 'PREMIUM'), (1, 'B', 9, 'PREMIUM'), (1, 'B', 10, 'PREMIUM'),
-- Row C (Balcony)
(1, 'C', 1, 'BALCONY'), (1, 'C', 2, 'BALCONY'), (1, 'C', 3, 'BALCONY'), (1, 'C', 4, 'BALCONY'), (1, 'C', 5, 'BALCONY'),
(1, 'C', 6, 'BALCONY'), (1, 'C', 7, 'BALCONY'), (1, 'C', 8, 'BALCONY'), (1, 'C', 9, 'BALCONY'), (1, 'C', 10, 'BALCONY');

-- 6. Movies
INSERT INTO `movie` (`movie_id`, `title`, `genre`, `language`, `duration`, `release_date`, `status`) VALUES 
(1, 'Cyber Odyssey 2099', 'Sci-Fi / Thriller', 'English', 145, '2026-08-01', 'ACTIVE'),
(2, 'The Midnight Cipher', 'Mystery / Drama', 'English', 120, '2026-08-10', 'ACTIVE');

-- 7. Shows (Scheduled Sessions)
INSERT INTO `shows` (`show_id`, `movie_id`, `screen_id`, `show_date`, `start_time`, `end_time`, `ticket_price`, `status`) VALUES 
(1, 1, 1, CURRENT_DATE(), '18:00:00', '20:25:00', 15.00, 'ACTIVE'),
(2, 1, 1, CURRENT_DATE(), '21:00:00', '23:25:00', 18.00, 'ACTIVE'),
(3, 2, 2, CURRENT_DATE(), '19:00:00', '21:00:00', 22.00, 'ACTIVE');

-- 8. Show Seats Inventory for Show 1
INSERT INTO `show_seat` (`show_id`, `seat_id`, `status`, `price`)
SELECT 1, s.seat_id, 'AVAILABLE', 
       CASE 
           WHEN s.seat_type = 'BALCONY' THEN 20.00 
           WHEN s.seat_type = 'PREMIUM' THEN 18.00 
           ELSE 15.00 
       END
FROM `seat` s WHERE s.screen_id = 1;

-- 9. User Preferences for Customer (User ID 1)
INSERT INTO `user_preference` (`user_id`, `budget_limit`, `preferred_time`, `preferred_seat_type`, `group_size`) VALUES 
(1, 20.00, 'evening', 'PREMIUM', 2);
