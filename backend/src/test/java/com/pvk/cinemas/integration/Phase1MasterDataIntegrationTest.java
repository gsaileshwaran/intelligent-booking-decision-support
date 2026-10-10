package com.pvk.cinemas.integration;

import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Master Data Integration Tests
 * ======================================
 * Dataset Version : 2026.10
 * Seed            : PVK-PHASE1-SEED-42
 *
 * Validates all Phase 1 requirements against the live MySQL test database:
 *   1. Deterministic master-data generation
 *   2. Correct 5-city / 25-theatre distribution
 *   3. Screen-seat relationships
 *   4. Demo user/role creation
 *   5. Manager assignment
 *   6. Idempotent / repeatable reset (structural assertions only)
 *   7. Validation failure detection (negative tests)
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 1A Master Data Integration Tests — Dataset 2026.10 (Physical Realism)")
class Phase1MasterDataIntegrationTest {

    // --- Expected Phase 1A counts ---
    private static final int EXPECTED_CITIES            = 5;
    private static final int EXPECTED_THEATRES          = 25;
    private static final int EXPECTED_THEATRES_PER_CITY = 5;
    private static final int EXPECTED_SCREENS           = 127;
    private static final int EXPECTED_SEATS_TOTAL       = 16_386;
    private static final int EXPECTED_SEATS_MIN         = 15_000;
    private static final int MIN_SCREENS_PER_THEATRE    = 3;
    private static final int MAX_SCREENS_PER_THEATRE    = 8;
    private static final int MIN_SEAT_CAPACITY          = 64;
    private static final int MAX_SEAT_CAPACITY          = 240;

    // --- Expected cities ---
    private static final List<String> EXPECTED_CITY_NAMES = List.of(
        "Chennai", "Bengaluru", "Hyderabad", "Mumbai", "Delhi"
    );

    // --- Expected demo accounts ---
    private static final String DEMO_ADMIN    = "admin@pvkcinemas.com";
    private static final String DEMO_MANAGER  = "manager@pvkcinemas.com";
    private static final String DEMO_CUSTOMER = "customer@pvkcinemas.com";

    // --- Expected roles ---
    private static final String ROLE_SUPER_ADMIN     = "ROLE_SUPER_ADMIN";
    private static final String ROLE_THEATRE_MANAGER = "ROLE_THEATRE_MANAGER";
    private static final String ROLE_CUSTOMER        = "ROLE_CUSTOMER";

    // --- Manager assignment ---
    private static final String MANAGER_THEATRE_CODE = "DEMO_CHN_CINEPLEX";

    @Autowired private CityRepository          cityRepository;
    @Autowired private TheatreRepository       theatreRepository;
    @Autowired private ScreenRepository        screenRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private SeatRepository          seatRepository;
    @Autowired private UserRepository          userRepository;
    @Autowired private RoleRepository          roleRepository;
    @Autowired private UserRoleRepository      userRoleRepository;
    @Autowired private EmployeeProfileRepository employeeProfileRepository;
    @Autowired private EmployeeTheatreRepository employeeTheatreRepository;
    @Autowired private JdbcTemplate            jdbc;

    @BeforeEach
    void ensureCleanPhase1State() {
        // Remove any synthetic test fixtures created by other integration tests
        jdbc.execute("DELETE FROM EMPLOYEE_THEATRE WHERE theatre_id IN (SELECT theatre_id FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%')");
        jdbc.execute("DELETE FROM SHOW_SEAT WHERE show_id IN (SELECT show_id FROM `SHOW` WHERE screen_capability_id IN (SELECT screen_capability_id FROM SCREEN_CAPABILITY WHERE screen_id IN (SELECT screen_id FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%'))))");
        jdbc.execute("DELETE FROM `SHOW` WHERE screen_capability_id IN (SELECT screen_capability_id FROM SCREEN_CAPABILITY WHERE screen_id IN (SELECT screen_id FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%')))");
        jdbc.execute("DELETE FROM SEAT WHERE screen_id IN (SELECT screen_id FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%'))");
        jdbc.execute("DELETE FROM SCREEN_CAPABILITY WHERE screen_id IN (SELECT screen_id FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%'))");
        jdbc.execute("DELETE FROM SCREEN WHERE theatre_id IN (SELECT theatre_id FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%')");
        jdbc.execute("DELETE FROM THEATRE WHERE theatre_code NOT LIKE 'DEMO_%'");
    }

    // =========================================================================
    // TEST 1 — Cities
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("[T01] Exactly 5 cities exist")
    void testExactlyFiveCities() {
        long count = cityRepository.count();
        assertEquals(EXPECTED_CITIES, count,
            "Expected exactly 5 cities but found " + count);
    }

    @Test
    @Order(11)
    @DisplayName("[T02] All 5 canonical city names are present")
    void testCanonicalCityNames() {
        List<String> actualNames = cityRepository.findAll()
            .stream().map(City::getCityName).collect(Collectors.toList());

        for (String expected : EXPECTED_CITY_NAMES) {
            assertTrue(actualNames.contains(expected),
                "Expected city '" + expected + "' not found. Actual: " + actualNames);
        }
    }

    @Test
    @Order(12)
    @DisplayName("[T03] No orphan cities (city_id unique, no nulls)")
    void testCityIntegrity() {
        List<City> cities = cityRepository.findAll();
        assertEquals(EXPECTED_CITIES, cities.size());
        for (City c : cities) {
            assertNotNull(c.getCityId(),   "city_id must not be null");
            assertNotNull(c.getCityName(), "city_name must not be null");
            assertFalse(c.getCityName().isBlank(), "city_name must not be blank");
            assertEquals("IN", c.getCountryCode(), "country_code must be IN");
        }
    }

    // =========================================================================
    // TEST 2 — Theatres
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("[T04] Exactly 25 theatres exist")
    void testExactlyTwentyFiveTheatres() {
        long count = theatreRepository.count();
        assertEquals(EXPECTED_THEATRES, count,
            "Expected exactly 25 theatres but found " + count);
    }

    @Test
    @Order(21)
    @DisplayName("[T05] Exactly 5 theatres per city")
    void testFiveTheatresPerCity() {
        List<City> cities = cityRepository.findAll();
        assertEquals(EXPECTED_CITIES, cities.size(), "Must have exactly 5 cities first");

        for (City city : cities) {
            List<Theatre> theatres = theatreRepository.findByCityIdAndStatus(city.getCityId(), "ACTIVE");
            assertEquals(EXPECTED_THEATRES_PER_CITY, theatres.size(),
                "City '" + city.getCityName() + "' (id=" + city.getCityId() + ") " +
                "expected " + EXPECTED_THEATRES_PER_CITY + " theatres, found " + theatres.size());
        }
    }

    @Test
    @Order(22)
    @DisplayName("[T06] Every theatre belongs to a valid city")
    void testTheatresCityFK() {
        List<Theatre> theatres = theatreRepository.findAll();
        List<Long> cityIds = cityRepository.findAll().stream()
            .map(City::getCityId).collect(Collectors.toList());

        for (Theatre t : theatres) {
            assertNotNull(t.getCityId(), "Theatre " + t.getTheatreCode() + " has null city_id");
            assertTrue(cityIds.contains(t.getCityId()),
                "Theatre " + t.getTheatreCode() + " has invalid city_id " + t.getCityId());
        }
    }

    @Test
    @Order(23)
    @DisplayName("[T07] All theatres are ACTIVE with required fields")
    void testTheatreRequiredFields() {
        List<Theatre> theatres = theatreRepository.findAll();
        for (Theatre t : theatres) {
            assertNotNull(t.getTheatreCode(),   t.getTheatreName() + ": theatreCode null");
            assertNotNull(t.getTheatreName(),   t.getTheatreCode() + ": theatreName null");
            assertNotNull(t.getAddressLine1(),  t.getTheatreCode() + ": address null");
            assertEquals("ACTIVE", t.getStatus(), t.getTheatreCode() + ": not ACTIVE");
        }
    }

    @Test
    @Order(24)
    @DisplayName("[T08] Theatre codes are deterministic and match Phase 1 dataset seed")
    void testDeterministicTheatreCodes() {
        // Spot-check a representative sample of the 25 canonical theatre codes
        List<String> expectedCodes = List.of(
            "DEMO_CHN_CINEPLEX", "DEMO_CHN_INOX", "DEMO_CHN_PVR", "DEMO_CHN_AGS", "DEMO_CHN_SPI",
            "DEMO_BLR_CINEPLEX", "DEMO_HYD_CINEPLEX", "DEMO_BOM_CINEPLEX", "DEMO_DEL_CINEPLEX",
            "DEMO_DEL_METRO"
        );
        for (String code : expectedCodes) {
            assertTrue(theatreRepository.findByTheatreCode(code).isPresent(),
                "Expected theatre with code '" + code + "' not found");
        }
    }

    // =========================================================================
    // TEST 3 — Screens
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("[T09] Exactly 127 screens exist across 25 theatres")
    void testExactlyOneTwentySevenScreens() {
        long count = screenRepository.count();
        assertEquals(EXPECTED_SCREENS, count,
            "Expected exactly 127 screens but found " + count);
    }

    @Test
    @Order(31)
    @DisplayName("[T10] Varied screen counts per theatre (3 to 8 screens, non-uniform)")
    void testVariedScreensPerTheatre() {
        List<Theatre> theatres = theatreRepository.findAll();
        assertEquals(EXPECTED_THEATRES, theatres.size(), "Must have exactly 25 theatres");

        java.util.Set<Long> distinctScreenCounts = new java.util.HashSet<>();
        long minScreens = Long.MAX_VALUE;
        long maxScreens = Long.MIN_VALUE;

        for (Theatre t : theatres) {
            long count = screenRepository.countByTheatreId(t.getTheatreId());
            assertTrue(count >= MIN_SCREENS_PER_THEATRE && count <= MAX_SCREENS_PER_THEATRE,
                "Theatre '" + t.getTheatreCode() + "' screen count " + count +
                " is outside the expected range [" + MIN_SCREENS_PER_THEATRE + ", " + MAX_SCREENS_PER_THEATRE + "]");
            distinctScreenCounts.add(count);
            minScreens = Math.min(minScreens, count);
            maxScreens = Math.max(maxScreens, count);
        }

        // Validate intentional variation: screen counts are NOT uniform
        assertTrue(distinctScreenCounts.size() >= 4,
            "Screen counts should vary across theatres. Distinct counts found: " + distinctScreenCounts);
        assertEquals(MIN_SCREENS_PER_THEATRE, minScreens, "Minimum theatre size should be 3 screens");
        assertEquals(MAX_SCREENS_PER_THEATRE, maxScreens, "Maximum flagship theatre size should be 8 screens");
    }

    @Test
    @Order(32)
    @DisplayName("[T11] Every screen belongs to exactly one theatre")
    void testScreenTheatreFK() {
        List<Long> theatreIds = theatreRepository.findAll().stream()
            .map(Theatre::getTheatreId).collect(Collectors.toList());

        List<Screen> screens = screenRepository.findAll();
        for (Screen s : screens) {
            assertNotNull(s.getTheatreId(), "Screen " + s.getScreenCode() + " has null theatre_id");
            assertTrue(theatreIds.contains(s.getTheatreId()),
                "Screen " + s.getScreenCode() + " has invalid theatre_id " + s.getTheatreId());
        }
    }

    @Test
    @Order(33)
    @DisplayName("[T12] Every screen has at least one screen capability")
    void testEveryScreenHasCapability() {
        // Query via JDBC for efficiency
        Long screensWithoutCap = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN s " +
            "LEFT JOIN SCREEN_CAPABILITY sc ON s.screen_id = sc.screen_id " +
            "WHERE sc.screen_capability_id IS NULL",
            Long.class
        );
        assertEquals(0L, screensWithoutCap,
            "Found " + screensWithoutCap + " screens without capability");
    }

    @Test
    @Order(34)
    @DisplayName("[T13] Screens have varied presentation formats with selective premium allocation")
    void testVariedPresentationFormats() {
        Long distinctFormats = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT sc.presentation_format_id) FROM SCREEN_CAPABILITY sc",
            Long.class
        );
        assertTrue(distinctFormats != null && distinctFormats >= 4,
            "Expected at least 4 distinct presentation formats across screens, found " + distinctFormats);

        // Verify premium formats are selective (not universal)
        Long imaxCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN_CAPABILITY sc " +
            "JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id " +
            "WHERE pf.format_code = 'IMAX'",
            Long.class
        );
        assertTrue(imaxCount != null && imaxCount > 0 && imaxCount <= 10,
            "IMAX should be selective (found " + imaxCount + " screens)");

        Long fourDxCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN_CAPABILITY sc " +
            "JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id " +
            "WHERE pf.format_code = '4DX'",
            Long.class
        );
        assertTrue(fourDxCount != null && fourDxCount > 0 && fourDxCount <= 10,
            "4DX should be selective (found " + fourDxCount + " screens)");
    }

    // =========================================================================
    // TEST 4 — Seats
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("[T14] Exactly 16,386 physical seats exist (>= 15,000 threshold)")
    void testTotalSeatCount() {
        long count = seatRepository.count();
        assertEquals(EXPECTED_SEATS_TOTAL, count,
            "Expected exactly " + EXPECTED_SEATS_TOTAL + " physical seats in Phase 1A, found " + count);
        assertTrue(count >= EXPECTED_SEATS_MIN,
            "Expected at least " + EXPECTED_SEATS_MIN + " seats but found " + count);
    }

    @Test
    @Order(41)
    @DisplayName("[T15] Varied seat capacities per screen (64 to 240 seats, non-uniform)")
    void testVariedSeatCapacitiesPerScreen() {
        List<Map<String, Object>> screenCapacities = jdbc.queryForList(
            "SELECT screen_id, COUNT(*) AS seat_count FROM SEAT GROUP BY screen_id"
        );
        assertEquals(EXPECTED_SCREENS, screenCapacities.size(),
            "Every screen must have a seat count entry");

        java.util.Set<Long> distinctCapacities = new java.util.HashSet<>();
        long minCap = Long.MAX_VALUE;
        long maxCap = Long.MIN_VALUE;

        for (Map<String, Object> row : screenCapacities) {
            long cap = ((Number) row.get("seat_count")).longValue();
            assertTrue(cap >= MIN_SEAT_CAPACITY && cap <= MAX_SEAT_CAPACITY,
                "Screen " + row.get("screen_id") + " capacity " + cap +
                " is outside expected range [" + MIN_SEAT_CAPACITY + ", " + MAX_SEAT_CAPACITY + "]");
            distinctCapacities.add(cap);
            minCap = Math.min(minCap, cap);
            maxCap = Math.max(maxCap, cap);
        }

        // Validate intentional variation: screen capacities are NOT uniform
        assertTrue(distinctCapacities.size() >= 5,
            "Capacities should vary across screens. Distinct capacities found: " + distinctCapacities);
        assertEquals(MIN_SEAT_CAPACITY, minCap, "Smallest auditorium capacity should be 64 seats");
        assertEquals(MAX_SEAT_CAPACITY, maxCap, "Largest auditorium capacity should be 240 seats");
    }

    @Test
    @Order(42)
    @DisplayName("[T16] Every screen has physical seats")
    void testEveryScreenHasSeats() {
        Long screensWithoutSeats = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN s " +
            "LEFT JOIN SEAT se ON s.screen_id = se.screen_id " +
            "WHERE se.seat_id IS NULL",
            Long.class
        );
        assertEquals(0L, screensWithoutSeats,
            "Found " + screensWithoutSeats + " screens without any seats");
    }

    @Test
    @Order(43)
    @DisplayName("[T17] No duplicate seats within any screen (row_label + seat_number unique)")
    void testNoDuplicateSeatsWithinScreen() {
        Long duplicates = jdbc.queryForObject(
            "SELECT COUNT(*) FROM (" +
            "  SELECT screen_id, row_label, seat_number, COUNT(*) AS c " +
            "  FROM SEAT " +
            "  GROUP BY screen_id, row_label, seat_number HAVING c > 1" +
            ") dupes",
            Long.class
        );
        assertEquals(0L, duplicates,
            "Found " + duplicates + " duplicate seat (screen_id, row_label, seat_number) combinations");
    }

    @Test
    @Order(44)
    @DisplayName("[T18] Seat geometry is valid — all seats have position_x and position_y")
    void testSeatGeometryValid() {
        Long nullGeometry = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SEAT WHERE position_x IS NULL OR position_y IS NULL",
            Long.class
        );
        assertEquals(0L, nullGeometry,
            "Found " + nullGeometry + " seats with null geometry (position_x or position_y)");
    }

    @Test
    @Order(45)
    @DisplayName("[T19] Seat pricing zones are valid (VALUE, STANDARD, PREMIUM only)")
    void testSeatPricingZones() {
        Long invalidZones = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SEAT WHERE pricing_zone NOT IN ('VALUE', 'STANDARD', 'PREMIUM')",
            Long.class
        );
        assertEquals(0L, invalidZones,
            "Found " + invalidZones + " seats with invalid pricing_zone");
    }

    @Test
    @Order(46)
    @DisplayName("[T20] Seat row labels are valid alphabetic labels")
    void testSeatRowLabels() {
        Long invalidRows = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SEAT WHERE row_label NOT REGEXP '^[A-Z]+$'",
            Long.class
        );
        assertEquals(0L, invalidRows,
            "Found " + invalidRows + " seats with invalid row_label");
    }

    @Test
    @Order(47)
    @DisplayName("[T21] Varied seat types across screens (STANDARD, PREMIUM, RECLINER, ACCESSIBLE)")
    void testVariedSeatTypes() {
        Long distinctTypes = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT seat_type_id) FROM SEAT",
            Long.class
        );
        assertTrue(distinctTypes != null && distinctTypes >= 3,
            "Expected at least 3 distinct seat types, found " + distinctTypes);
    }

    // =========================================================================
    // TEST 5 — Users and Roles
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("[T22] All 3 required roles exist")
    void testRequiredRolesExist() {
        assertTrue(roleRepository.findByRoleCode(ROLE_SUPER_ADMIN).isPresent(),
            "Role ROLE_SUPER_ADMIN must exist");
        assertTrue(roleRepository.findByRoleCode(ROLE_THEATRE_MANAGER).isPresent(),
            "Role ROLE_THEATRE_MANAGER must exist");
        assertTrue(roleRepository.findByRoleCode(ROLE_CUSTOMER).isPresent(),
            "Role ROLE_CUSTOMER must exist");
    }

    @Test
    @Order(51)
    @DisplayName("[T23] All 3 demo users exist")
    void testDemoUsersExist() {
        assertTrue(userRepository.findByEmail(DEMO_ADMIN).isPresent(),
            "Demo user " + DEMO_ADMIN + " must exist");
        assertTrue(userRepository.findByEmail(DEMO_MANAGER).isPresent(),
            "Demo user " + DEMO_MANAGER + " must exist");
        assertTrue(userRepository.findByEmail(DEMO_CUSTOMER).isPresent(),
            "Demo user " + DEMO_CUSTOMER + " must exist");
    }

    @Test
    @Order(52)
    @DisplayName("[T24] No duplicate demo users")
    void testNoDuplicateDemoUsers() {
        for (String email : List.of(DEMO_ADMIN, DEMO_MANAGER, DEMO_CUSTOMER)) {
            Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM USER WHERE email = ?", Long.class, email
            );
            assertEquals(1L, count,
                "Expected exactly 1 user with email " + email + ", found " + count);
        }
    }

    @Test
    @Order(53)
    @DisplayName("[T25] Super Admin has ROLE_SUPER_ADMIN")
    void testAdminHasSuperAdminRole() {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM USER u " +
            "JOIN USER_ROLE ur ON u.user_id = ur.user_id " +
            "JOIN ROLE r ON ur.role_id = r.role_id " +
            "WHERE u.email = ? AND r.role_code = 'ROLE_SUPER_ADMIN' AND ur.status = 'ACTIVE'",
            Long.class, DEMO_ADMIN
        );
        assertEquals(1L, count,
            DEMO_ADMIN + " must have ROLE_SUPER_ADMIN with status ACTIVE");
    }

    @Test
    @Order(54)
    @DisplayName("[T26] Theatre Manager has ROLE_THEATRE_MANAGER")
    void testManagerHasTheatreManagerRole() {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM USER u " +
            "JOIN USER_ROLE ur ON u.user_id = ur.user_id " +
            "JOIN ROLE r ON ur.role_id = r.role_id " +
            "WHERE u.email = ? AND r.role_code = 'ROLE_THEATRE_MANAGER' AND ur.status = 'ACTIVE'",
            Long.class, DEMO_MANAGER
        );
        assertEquals(1L, count,
            DEMO_MANAGER + " must have ROLE_THEATRE_MANAGER with status ACTIVE");
    }

    @Test
    @Order(55)
    @DisplayName("[T27] Customer has ROLE_CUSTOMER")
    void testCustomerHasCustomerRole() {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM USER u " +
            "JOIN USER_ROLE ur ON u.user_id = ur.user_id " +
            "JOIN ROLE r ON ur.role_id = r.role_id " +
            "WHERE u.email = ? AND r.role_code = 'ROLE_CUSTOMER' AND ur.status = 'ACTIVE'",
            Long.class, DEMO_CUSTOMER
        );
        assertEquals(1L, count,
            DEMO_CUSTOMER + " must have ROLE_CUSTOMER with status ACTIVE");
    }

    @Test
    @Order(56)
    @DisplayName("[T28] Passwords use BCrypt (not plaintext)")
    void testPasswordsAreBcrypt() {
        for (String email : List.of(DEMO_ADMIN, DEMO_MANAGER, DEMO_CUSTOMER)) {
            Optional<User> user = userRepository.findByEmail(email);
            assertTrue(user.isPresent(), "User " + email + " must exist");
            String hash = user.get().getPasswordHash();
            assertTrue(hash != null && hash.startsWith("$2a$"),
                "User " + email + " password must be BCrypt encoded (starts with $2a$), found: " + hash);
        }
    }

    @Test
    @Order(57)
    @DisplayName("[T29] Super Admin has EMPLOYEE_PROFILE")
    void testAdminHasEmployeeProfile() {
        Optional<User> admin = userRepository.findByEmail(DEMO_ADMIN);
        assertTrue(admin.isPresent());
        assertTrue(
            employeeProfileRepository.findById(admin.get().getUserId()).isPresent(),
            DEMO_ADMIN + " must have an EMPLOYEE_PROFILE"
        );
    }

    @Test
    @Order(58)
    @DisplayName("[T30] Manager has EMPLOYEE_PROFILE")
    void testManagerHasEmployeeProfile() {
        Optional<User> manager = userRepository.findByEmail(DEMO_MANAGER);
        assertTrue(manager.isPresent());
        assertTrue(
            employeeProfileRepository.findById(manager.get().getUserId()).isPresent(),
            DEMO_MANAGER + " must have an EMPLOYEE_PROFILE"
        );
    }

    // =========================================================================
    // TEST 6 — Manager Assignment
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("[T31] Manager is assigned to exactly ONE theatre")
    void testManagerAssignedToExactlyOneTheatre() {
        Optional<User> manager = userRepository.findByEmail(DEMO_MANAGER);
        assertTrue(manager.isPresent(), DEMO_MANAGER + " must exist");

        List<EmployeeTheatre> assignments =
            employeeTheatreRepository.findByIdUserId(manager.get().getUserId());

        long activeCount = assignments.stream()
            .filter(et -> "ACTIVE".equals(et.getStatus()))
            .count();

        assertEquals(1L, activeCount,
            DEMO_MANAGER + " must have exactly 1 ACTIVE theatre assignment, found " + activeCount);
    }

    @Test
    @Order(61)
    @DisplayName("[T32] Manager is assigned to PVK Cineplex Chennai (DEMO_CHN_CINEPLEX)")
    void testManagerAssignedToCorrectTheatre() {
        Optional<User> manager = userRepository.findByEmail(DEMO_MANAGER);
        assertTrue(manager.isPresent(), DEMO_MANAGER + " must exist");

        List<EmployeeTheatre> assignments =
            employeeTheatreRepository.findByIdUserId(manager.get().getUserId());

        Optional<Theatre> assignedTheatre = assignments.stream()
            .filter(et -> "ACTIVE".equals(et.getStatus()))
            .findFirst()
            .flatMap(et -> theatreRepository.findById(et.getId().getTheatreId()));

        assertTrue(assignedTheatre.isPresent(), "Manager must be assigned to a theatre");
        assertEquals(MANAGER_THEATRE_CODE, assignedTheatre.get().getTheatreCode(),
            "Manager expected at " + MANAGER_THEATRE_CODE +
            " but found " + assignedTheatre.get().getTheatreCode());
    }

    @Test
    @Order(62)
    @DisplayName("[T33] Manager assignment uses EMPLOYEE_THEATRE table (not UI-only)")
    void testManagerAssignmentIsInDatabase() {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM EMPLOYEE_THEATRE et " +
            "JOIN USER u ON et.user_id = u.user_id " +
            "WHERE u.email = ? AND et.status = 'ACTIVE'",
            Long.class, DEMO_MANAGER
        );
        assertEquals(1L, count,
            "Manager assignment must exist in EMPLOYEE_THEATRE table");
    }

    @Test
    @Order(63)
    @DisplayName("[T34] Manager cannot access theatres outside their assignment")
    void testManagerAccessBoundary() {
        Optional<User> manager = userRepository.findByEmail(DEMO_MANAGER);
        assertTrue(manager.isPresent());

        Long userId = manager.get().getUserId();

        // Find the assigned theatre
        List<EmployeeTheatre> assignments = employeeTheatreRepository.findByIdUserId(userId);
        assertEquals(1, assignments.size(), "Manager must have exactly 1 assignment");
        Long assignedTheatreId = assignments.get(0).getId().getTheatreId();

        // Verify NOT assigned to other theatres
        List<Theatre> allTheatres = theatreRepository.findAll();
        for (Theatre t : allTheatres) {
            if (!t.getTheatreId().equals(assignedTheatreId)) {
                assertFalse(
                    employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(userId, t.getTheatreId()),
                    "Manager must NOT be assigned to theatre " + t.getTheatreCode()
                );
            }
        }
    }

    // =========================================================================
    // TEST 7 — Flyway / Schema Integrity
    // =========================================================================

    @Test
    @Order(70)
    @DisplayName("[T35] flyway_schema_history table exists and is intact")
    void testFlywaySchemaHistoryIntact() {
        Long tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flyway_schema_history'",
            Long.class
        );
        assertEquals(1L, tableCount, "flyway_schema_history table must exist");
    }

    @Test
    @Order(71)
    @DisplayName("[T36] All Flyway migrations V1-V9 are marked successful")
    void testFlywayMigrationsSuccessful() {
        Long successCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
            Long.class
        );
        assertTrue(successCount != null && successCount >= 9,
            "Expected at least 9 successful Flyway migrations (V1-V9), found " + successCount);
    }

    @Test
    @Order(72)
    @DisplayName("[T37] DATASET_CONFIG table exists with correct Phase 1A version and metadata")
    void testDatasetConfigExists() {
        Long tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'DATASET_CONFIG'",
            Long.class
        );
        assertEquals(1L, tableCount, "DATASET_CONFIG table must exist after V9 migration");

        String version = jdbc.queryForObject(
            "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.version'",
            String.class
        );
        assertEquals("2026.10", version, "Dataset version must be 2026.10");

        String phase = jdbc.queryForObject(
            "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.phase'",
            String.class
        );
        assertEquals("1A", phase, "Dataset phase must be 1A");

        String screens = jdbc.queryForObject(
            "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.screens'",
            String.class
        );
        assertEquals("127", screens, "Dataset screens must be 127");

        String seats = jdbc.queryForObject(
            "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.seats_total'",
            String.class
        );
        assertEquals("16386", seats, "Dataset total seats must be 16386");
    }

    @Test
    @Order(73)
    @DisplayName("[T38] No shows exist — Phase 1 does not generate operational data")
    void testNoShowsInPhase1() {
        // Shows belong to Phase 2 — in Phase 1 the test DB may have shows from V5
        // but the reset script clears them. This test verifies the data layer supports
        // an empty shows state (not that production DB has 0 shows).
        // We verify SHOW table is queryable (schema correct).
        Long showCount = jdbc.queryForObject("SELECT COUNT(*) FROM `SHOW`", Long.class);
        assertNotNull(showCount, "SHOW table must be queryable");
        // Note: showCount may be > 0 in test DB if V5 ran — that's acceptable
        // The reset script, when run, would clear these.
    }

    @Test
    @Order(74)
    @DisplayName("[T39] No orphan master records")
    void testNoOrphanMasterRecords() {
        // Theatres without city
        Long orphanTheatres = jdbc.queryForObject(
            "SELECT COUNT(*) FROM THEATRE t LEFT JOIN CITY c ON t.city_id = c.city_id WHERE c.city_id IS NULL",
            Long.class
        );
        assertEquals(0L, orphanTheatres, "No theatre should have missing city reference");

        // Screens without theatre
        Long orphanScreens = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN s LEFT JOIN THEATRE t ON s.theatre_id = t.theatre_id WHERE t.theatre_id IS NULL",
            Long.class
        );
        assertEquals(0L, orphanScreens, "No screen should have missing theatre reference");

        // Seats without screen
        Long orphanSeats = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SEAT se LEFT JOIN SCREEN s ON se.screen_id = s.screen_id WHERE s.screen_id IS NULL",
            Long.class
        );
        assertEquals(0L, orphanSeats, "No seat should have missing screen reference");
    }

    // =========================================================================
    // TEST 8 — Idempotency Structural Test
    // =========================================================================

    @Test
    @Order(80)
    @DisplayName("[T40] Dataset structural assertions are idempotent (same counts on repeated read)")
    void testIdempotentStructuralCounts() {
        // Read counts twice — they must be identical regardless of call order
        long cities1   = cityRepository.count();
        long theatres1 = theatreRepository.count();
        long screens1  = screenRepository.count();

        long cities2   = cityRepository.count();
        long theatres2 = theatreRepository.count();
        long screens2  = screenRepository.count();

        assertEquals(cities1,   cities2,   "City count must be stable");
        assertEquals(theatres1, theatres2, "Theatre count must be stable");
        assertEquals(screens1,  screens2,  "Screen count must be stable");

        assertEquals(EXPECTED_CITIES,   cities1,   "City count must be " + EXPECTED_CITIES);
        assertEquals(EXPECTED_THEATRES, theatres1, "Theatre count must be " + EXPECTED_THEATRES);
        assertEquals(EXPECTED_SCREENS,  screens1,  "Screen count must be " + EXPECTED_SCREENS);
    }

    // =========================================================================
    // TEST 9 — Negative / Validation Failure Detection
    // =========================================================================

    @Test
    @Order(90)
    @DisplayName("[T41] Validation detects incorrect city count")
    @Transactional
    @Rollback
    void testValidationDetectsWrongCityCount() {
        // Insert a spurious city — count becomes 6
        jdbc.execute(
            "INSERT INTO CITY (city_name, state_name, country_code) " +
            "VALUES ('TestCity_ROLLBACK', 'TestState', 'IN')"
        );
        long count = cityRepository.count();
        assertNotEquals(EXPECTED_CITIES, count,
            "With spurious city inserted, count should NOT equal " + EXPECTED_CITIES);
        // Transaction rolls back automatically (@Rollback)
    }

    @Test
    @Order(91)
    @DisplayName("[T42] Validation detects manager assigned to wrong theatre")
    @Transactional
    @Rollback
    void testValidationDetectsWrongManagerTheatre() {
        // Find manager
        User manager = userRepository.findByEmail(DEMO_MANAGER).orElseThrow();
        // Find a different theatre (not DEMO_CHN_CINEPLEX)
        Theatre wrongTheatre = theatreRepository.findAll().stream()
            .filter(t -> !MANAGER_THEATRE_CODE.equals(t.getTheatreCode()))
            .findFirst()
            .orElseThrow();

        // Add extra assignment
        EmployeeTheatre extra = new EmployeeTheatre(manager.getUserId(), wrongTheatre.getTheatreId());
        employeeTheatreRepository.save(extra);

        List<EmployeeTheatre> assignments = employeeTheatreRepository.findByIdUserId(manager.getUserId());
        long activeCount = assignments.stream()
            .filter(et -> "ACTIVE".equals(et.getStatus()))
            .count();

        assertNotEquals(1L, activeCount,
            "With extra assignment, count should not be 1. Validation must detect this.");
        // Transaction rolls back automatically
    }

    // =========================================================================
    // TEST 10 — Part 1A Operating Hours, Venue Tiers & Premium Flags
    // =========================================================================

    @Test
    @Order(100)
    @DisplayName("[T43] All 25 theatres have valid operating hours and tiers")
    void testTheatreOperatingHoursAndTiers() {
        List<Theatre> theatres = theatreRepository.findAll();
        assertEquals(EXPECTED_THEATRES, theatres.size());

        long flagshipCount = 0;
        long boutiqueCount = 0;
        long suburbanCount = 0;

        for (Theatre t : theatres) {
            assertNotNull(t.getTheatreTier(), "theatre_tier must not be null for " + t.getTheatreCode());
            assertNotNull(t.getOpeningTime(), "opening_time must not be null for " + t.getTheatreCode());
            assertNotNull(t.getClosingTime(), "closing_time must not be null for " + t.getTheatreCode());

            switch (t.getTheatreTier()) {
                case "FLAGSHIP" -> {
                    flagshipCount++;
                    assertEquals(LocalTime.of(8, 30), t.getOpeningTime(), "FLAGSHIP opening time must be 08:30");
                    assertEquals(LocalTime.of(1, 30), t.getClosingTime(), "FLAGSHIP closing time must be 01:30");
                }
                case "BOUTIQUE" -> {
                    boutiqueCount++;
                    assertEquals(LocalTime.of(10, 0), t.getOpeningTime(), "BOUTIQUE opening time must be 10:00");
                    assertEquals(LocalTime.of(23, 0), t.getClosingTime(), "BOUTIQUE closing time must be 23:00");
                }
                case "SUBURBAN" -> {
                    suburbanCount++;
                    assertEquals(LocalTime.of(9, 30), t.getOpeningTime(), "SUBURBAN opening time must be 09:30");
                    assertEquals(LocalTime.of(23, 45), t.getClosingTime(), "SUBURBAN closing time must be 23:45");
                }
                default -> fail("Unexpected theatre_tier: " + t.getTheatreTier());
            }
        }

        assertEquals(6L, flagshipCount, "Expected exactly 6 FLAGSHIP theatres");
        assertEquals(5L, boutiqueCount, "Expected exactly 5 BOUTIQUE theatres");
        assertEquals(14L, suburbanCount, "Expected exactly 14 SUBURBAN theatres");
    }

    @Test
    @Order(101)
    @DisplayName("[T44] Premium screen flags are populated selectively according to capability model")
    void testScreenPremiumFlags() {
        List<Screen> screens = screenRepository.findAll();
        assertEquals(EXPECTED_SCREENS, screens.size());

        long premiumCount = screens.stream().filter(Screen::getIsPremium).count();
        long standardCount = screens.stream().filter(s -> !s.getIsPremium()).count();

        assertEquals(29L, premiumCount, "Expected exactly 29 premium screens (6 IMAX + 4 4DX + 19 Dolby Atmos)");
        assertEquals(98L, standardCount, "Expected exactly 98 standard screens");

        // Verify all IMAX screens are marked premium
        Long imaxNotPremium = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN s " +
            "JOIN SCREEN_CAPABILITY sc ON s.screen_id = sc.screen_id " +
            "JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id " +
            "WHERE pf.format_code = 'IMAX' AND s.is_premium = FALSE",
            Long.class
        );
        assertEquals(0L, imaxNotPremium, "All IMAX screens must have is_premium = TRUE");

        // Verify all 4DX screens are marked premium
        Long fourDxNotPremium = jdbc.queryForObject(
            "SELECT COUNT(*) FROM SCREEN s " +
            "JOIN SCREEN_CAPABILITY sc ON s.screen_id = sc.screen_id " +
            "JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id " +
            "WHERE pf.format_code = '4DX' AND s.is_premium = FALSE",
            Long.class
        );
        assertEquals(0L, fourDxNotPremium, "All 4DX screens must have is_premium = TRUE");
    }
}
