package com.booking.intelligent.config;

import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.*;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Override
    public void run(String... args) throws Exception {
        // 0a. Automatic MySQL Schema Migration: Add missing 'version' column to show_seat
        try {
            jdbcTemplate.execute("ALTER TABLE `show_seat` ADD COLUMN `version` INT NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // Column already exists
        }

        // 0b. Clean up legacy / incorrect theatre brand names to standard PVK brand
        try {
            jdbcTemplate.execute("UPDATE `theatre` SET `name` = 'PVK — Anna Nagar' WHERE `name` LIKE '%Downtown%' OR `name` LIKE '%Central%' OR `name` LIKE '%PVR%'");
            jdbcTemplate.execute("UPDATE `theatre` SET `name` = 'PVK — OMR' WHERE `name` LIKE '%Grand Mall%' OR `name` LIKE '%INOX%'");
            jdbcTemplate.execute("UPDATE `theatre` SET `name` = 'PVK — Velachery' WHERE `name` LIKE '%AGS%'");
            jdbcTemplate.execute("UPDATE `theatre` SET `name` = 'PVK — T. Nagar' WHERE `name` LIKE '%SPI%'");
        } catch (Exception ignored) {
        }

        // 1. Seed Roles
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_ADMIN).build()));

        // 2. Seed / Synchronize Default Demo Users (Password: password123)
        User customer = createOrUpdateDemoUser("John Customer", "customer@example.com", "password123", customerRole);
        User provider = createOrUpdateDemoUser("PVK Cinema Operator", "provider@example.com", "password123", providerRole);
        User admin = createOrUpdateDemoUser("System Administrator", "admin@example.com", "password123", adminRole);

        // 3. Seed PVK Theatre Branches owned by single provider (provider@example.com)
        Theatre b1 = getOrCreateTheatre("PVK — Anna Nagar", "Chennai", "2nd Avenue, Anna Nagar, Chennai", provider);
        Theatre b2 = getOrCreateTheatre("PVK — OMR", "Chennai", "Rajiv Gandhi Salai, OMR, Chennai", provider);
        Theatre b3 = getOrCreateTheatre("PVK — Velachery", "Chennai", "Phoenix Marketcity, Velachery, Chennai", provider);
        Theatre b4 = getOrCreateTheatre("PVK — T. Nagar", "Chennai", "GN Chetty Road, T. Nagar, Chennai", provider);

        // Ensure provider user ID is 100% synchronized on all PVK branches
        for (Theatre t : new Theatre[]{b1, b2, b3, b4}) {
            if (t.getOwnerUser() == null || !t.getOwnerUser().getUserId().equals(provider.getUserId())) {
                t.setOwnerUser(provider);
                theatreRepository.save(t);
            }
        }

        // 4. Seed Screens per Branch (Idempotent)
        // PVK — Anna Nagar Screens
        Screen s1_1 = getOrCreateScreen(b1, "Screen 1 — Standard", 60);
        Screen s1_2 = getOrCreateScreen(b1, "Screen 2 — Standard", 60);
        Screen s1_3 = getOrCreateScreen(b1, "IMAX Screen", 100);

        // PVK — OMR Screens
        Screen s2_1 = getOrCreateScreen(b2, "Screen 1 — Standard", 60);
        Screen s2_2 = getOrCreateScreen(b2, "Screen 2 — Standard", 60);
        Screen s2_3 = getOrCreateScreen(b2, "Premium Screen", 80);

        // PVK — Velachery Screens
        Screen s3_1 = getOrCreateScreen(b3, "Screen 1 — Standard", 60);
        Screen s3_2 = getOrCreateScreen(b3, "Screen 2 — Premium", 80);
        Screen s3_3 = getOrCreateScreen(b3, "Screen 3 — Standard", 60);

        // PVK — T. Nagar Screens
        Screen s4_1 = getOrCreateScreen(b4, "Screen 1 — Standard", 60);
        Screen s4_2 = getOrCreateScreen(b4, "Screen 2 — Premium", 80);

        // 5. Seed Physical Seat Layouts per Screen (Idempotent)
        // Standard Screens (Rows A-F: 60 seats)
        for (Screen sc : new Screen[]{s1_1, s1_2, s2_1, s2_2, s3_1, s3_3, s4_1}) {
            seedPhysicalSeats(sc, new String[]{"A", "B", "C", "D", "E", "F"}, 10);
        }
        // Premium Screens (Rows A-H: 80 seats)
        for (Screen sc : new Screen[]{s2_3, s3_2, s4_2}) {
            seedPhysicalSeats(sc, new String[]{"A", "B", "C", "D", "E", "F", "G", "H"}, 10);
        }
        // IMAX Screens (Rows A-J: 100 seats)
        seedPhysicalSeats(s1_3, new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"}, 10);

        // 6. Seed Movies (Idempotent)
        Movie m1 = getOrCreateMovie("Cyber Odyssey 2099", "Sci-Fi / Thriller", "English", 145);
        Movie m2 = getOrCreateMovie("The Last Signal", "Mystery / Drama", "English", 120);
        Movie m3 = getOrCreateMovie("Neon Horizon", "Action / Cyberpunk", "Tamil", 135);
        Movie m4 = getOrCreateMovie("Shadow Protocol", "Spy / Thriller", "Hindi", 150);
        Movie m5 = getOrCreateMovie("Beyond the Stars", "Space / Adventure", "English", 160);

        // 7. Seed Show Schedules across Branches (Multi-day distribution, non-overlapping)
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = today.plusDays(2);
        LocalDate day3 = today.plusDays(3);

        // PVK — Anna Nagar (Today, Tomorrow, DayAfter)
        getOrCreateShow(m1, s1_1, today, LocalTime.of(18, 0), LocalTime.of(20, 25), new BigDecimal("180.00"));
        getOrCreateShow(m1, s1_3, today, LocalTime.of(21, 0), LocalTime.of(23, 25), new BigDecimal("380.00"));

        getOrCreateShow(m1, s1_1, tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 25), new BigDecimal("180.00"));
        getOrCreateShow(m2, s1_1, tomorrow, LocalTime.of(13, 0), LocalTime.of(15, 0), new BigDecimal("180.00"));
        getOrCreateShow(m1, s1_2, tomorrow, LocalTime.of(16, 0), LocalTime.of(18, 25), new BigDecimal("180.00"));
        getOrCreateShow(m5, s1_3, tomorrow, LocalTime.of(19, 0), LocalTime.of(21, 40), new BigDecimal("350.00"));

        getOrCreateShow(m3, s1_2, dayAfter, LocalTime.of(11, 0), LocalTime.of(13, 15), new BigDecimal("200.00"));
        getOrCreateShow(m4, s1_3, dayAfter, LocalTime.of(18, 0), LocalTime.of(20, 30), new BigDecimal("360.00"));

        // PVK — OMR (Today, Tomorrow, DayAfter)
        getOrCreateShow(m1, s2_1, today, LocalTime.of(19, 0), LocalTime.of(21, 25), new BigDecimal("200.00"));
        getOrCreateShow(m4, s2_3, today, LocalTime.of(20, 0), LocalTime.of(22, 30), new BigDecimal("260.00"));

        getOrCreateShow(m1, s2_1, tomorrow, LocalTime.of(11, 0), LocalTime.of(13, 25), new BigDecimal("200.00"));
        getOrCreateShow(m3, s2_2, tomorrow, LocalTime.of(14, 30), LocalTime.of(16, 45), new BigDecimal("200.00"));
        getOrCreateShow(m1, s2_3, tomorrow, LocalTime.of(17, 30), LocalTime.of(19, 55), new BigDecimal("250.00"));
        getOrCreateShow(m5, s2_3, tomorrow, LocalTime.of(20, 30), LocalTime.of(23, 10), new BigDecimal("270.00"));

        // PVK — Velachery (Today, Tomorrow, DayAfter)
        getOrCreateShow(m3, s3_1, today, LocalTime.of(18, 30), LocalTime.of(20, 45), new BigDecimal("180.00"));
        getOrCreateShow(m1, s3_2, today, LocalTime.of(20, 0), LocalTime.of(22, 25), new BigDecimal("240.00"));

        getOrCreateShow(m3, s3_1, tomorrow, LocalTime.of(10, 30), LocalTime.of(12, 45), new BigDecimal("180.00"));
        getOrCreateShow(m1, s3_2, tomorrow, LocalTime.of(14, 0), LocalTime.of(16, 25), new BigDecimal("240.00"));
        getOrCreateShow(m4, s3_3, tomorrow, LocalTime.of(17, 30), LocalTime.of(20, 0), new BigDecimal("190.00"));
        getOrCreateShow(m5, s3_2, tomorrow, LocalTime.of(20, 30), LocalTime.of(23, 10), new BigDecimal("280.00"));

        // PVK — T. Nagar (Today, Tomorrow, Day3)
        getOrCreateShow(m2, s4_1, today, LocalTime.of(19, 0), LocalTime.of(21, 0), new BigDecimal("180.00"));
        getOrCreateShow(m1, s4_2, today, LocalTime.of(20, 30), LocalTime.of(22, 55), new BigDecimal("240.00"));

        getOrCreateShow(m2, s4_1, tomorrow, LocalTime.of(11, 30), LocalTime.of(13, 30), new BigDecimal("180.00"));
        getOrCreateShow(m4, s4_2, tomorrow, LocalTime.of(15, 0), LocalTime.of(17, 30), new BigDecimal("220.00"));
        getOrCreateShow(m1, s4_2, tomorrow, LocalTime.of(18, 30), LocalTime.of(20, 55), new BigDecimal("240.00"));

        // 8. Generate ShowSeat Inventory for ALL shows missing inventory using exact multipliers
        try {
            jdbcTemplate.execute(
                "INSERT INTO `show_seat` (`show_id`, `seat_id`, `status`, `price`, `version`) " +
                "SELECT sh.show_id, s.seat_id, 'AVAILABLE', " +
                "CASE WHEN s.seat_type = 'BALCONY' THEN sh.ticket_price * 1.50 WHEN s.seat_type = 'PREMIUM' THEN sh.ticket_price * 1.25 ELSE sh.ticket_price END, 0 " +
                "FROM `shows` sh " +
                "JOIN `seat` s ON s.screen_id = sh.screen_id " +
                "WHERE NOT EXISTS (SELECT 1 FROM `show_seat` ss WHERE ss.show_id = sh.show_id AND ss.seat_id = s.seat_id)"
            );
        } catch (Exception ignored) {
        }
    }

    private User createOrUpdateDemoUser(String name, String email, String rawPassword, Role role) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return userRepository.save(User.builder()
                    .name(name)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .build());
        } else {
            user.setName(name);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        }
    }

    private Theatre getOrCreateTheatre(String name, String location, String address, User owner) {
        return theatreRepository.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> theatreRepository.save(Theatre.builder()
                        .name(name)
                        .location(location)
                        .address(address)
                        .ownerUser(owner)
                        .status(TheatreStatus.ACTIVE)
                        .build()));
    }

    private Screen getOrCreateScreen(Theatre theatre, String name, int capacity) {
        return screenRepository.findByTheatreTheatreId(theatre.getTheatreId()).stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> screenRepository.save(Screen.builder()
                        .theatre(theatre)
                        .name(name)
                        .capacity(capacity)
                        .build()));
    }

    private void seedPhysicalSeats(Screen screen, String[] rows, int seatsPerRow) {
        List<Seat> existing = seatRepository.findByScreenScreenId(screen.getScreenId());
        if (existing.isEmpty()) {
            for (String r : rows) {
                SeatType type = (r.equals("E") || r.equals("F") || r.equals("G") || r.equals("H") || r.equals("I") || r.equals("J")) 
                        ? SeatType.BALCONY 
                        : ((r.equals("C") || r.equals("D")) ? SeatType.PREMIUM : SeatType.REGULAR);
                for (int i = 1; i <= seatsPerRow; i++) {
                    seatRepository.save(Seat.builder()
                            .screen(screen)
                            .rowLabel(r)
                            .seatNumber(i)
                            .seatType(type)
                            .build());
                }
            }
        }
    }

    private Movie getOrCreateMovie(String title, String genre, String language, int duration) {
        return movieRepository.findAll().stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElseGet(() -> movieRepository.save(Movie.builder()
                        .title(title)
                        .genre(genre)
                        .language(language)
                        .duration(duration)
                        .releaseDate(LocalDate.now().minusDays(10))
                        .status(MovieStatus.ACTIVE)
                        .build()));
    }

    private Show getOrCreateShow(Movie movie, Screen screen, LocalDate date, LocalTime start, LocalTime end, BigDecimal price) {
        List<Show> existing = showRepository.findByMovieMovieIdAndShowDate(movie.getMovieId(), date);
        for (Show s : existing) {
            if (s.getScreen().getScreenId().equals(screen.getScreenId()) && s.getStartTime().equals(start)) {
                return s;
            }
        }
        return showRepository.save(Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(date)
                .startTime(start)
                .endTime(end)
                .ticketPrice(price)
                .status(ShowStatus.ACTIVE)
                .build());
    }
}
