package com.pvk.cinemas.integration;

import com.pvk.cinemas.audit.model.AuditLog;
import com.pvk.cinemas.audit.repository.AuditLogRepository;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.CertificationRepository;
import com.pvk.cinemas.catalogue.repository.GenreRepository;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.search.model.SearchIndexDocument;
import com.pvk.cinemas.search.model.SearchQuery;
import com.pvk.cinemas.search.model.SearchResult;
import com.pvk.cinemas.search.repository.SearchIndexDocumentRepository;
import com.pvk.cinemas.search.repository.SearchQueryRepository;
import com.pvk.cinemas.search.repository.SearchResultRepository;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.PermissionRepository;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.CustomerProfile;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.CustomerProfileRepository;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class DatabaseSchemaIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CustomerProfileRepository customerProfileRepository;
    @Autowired private EmployeeProfileRepository employeeProfileRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EmployeeTheatreRepository employeeTheatreRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatTypeRepository seatTypeRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private LanguageRepository languageRepository;
    @Autowired private CertificationRepository certificationRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private SearchIndexDocumentRepository searchIndexDocumentRepository;
    @Autowired private SearchQueryRepository searchQueryRepository;
    @Autowired private SearchResultRepository searchResultRepository;

    private User createTestUser(String prefix) {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setFirstName("First" + uid);
        user.setLastName("Last" + uid);
        user.setEmail(prefix + "_" + uid + "@test.com");
        user.setPhone("+919" + (int)(Math.random() * 90000000 + 10000000));
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        user.setAccountStatus("ACTIVE");
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Hibernate ddl-auto=validate verifies all 28 JPA entity classes exactly match MySQL tables")
    void testSchemaValidationSucceeds() {
        assertNotNull(userRepository);
        assertNotNull(roleRepository);
        assertNotNull(theatreRepository);
        assertNotNull(screenRepository);
        assertNotNull(seatRepository);
        assertNotNull(showRepository);
        assertNotNull(showSeatRepository);
        assertNotNull(auditLogRepository);
        assertNotNull(searchIndexDocumentRepository);
    }

    @Test
    @DisplayName("Verify seed reference data in MySQL (Roles, Permissions, Cities, Languages, Genres, Certifications, SeatTypes)")
    void testReferenceDataSeedIntegrity() {
        // 1. Roles & Permissions
        assertTrue(roleRepository.count() >= 3, "Seed roles must exist");
        assertTrue(roleRepository.findByRoleCode("ROLE_CUSTOMER").isPresent(), "ROLE_CUSTOMER must exist");
        assertTrue(roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").isPresent(), "ROLE_SUPER_ADMIN must exist");
        assertTrue(roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").isPresent(), "ROLE_THEATRE_MANAGER must exist");
        assertTrue(permissionRepository.count() >= 10, "Seed permissions must exist");

        // 2. Reference Data
        assertTrue(cityRepository.count() >= 4, "Seed cities must exist");
        assertTrue(languageRepository.count() >= 6, "Seed languages must exist");
        assertTrue(genreRepository.count() >= 8, "Seed genres must exist");
        assertTrue(certificationRepository.count() >= 4, "Seed certifications must exist");
        assertTrue(seatTypeRepository.count() >= 4, "Seed seat types must exist");

        SeatType standard = seatTypeRepository.findByTypeCode("STANDARD").orElse(null);
        assertNotNull(standard);
        assertEquals("Standard", standard.getName());
    }

    @Test
    @DisplayName("Verify USER, CUSTOMER_PROFILE, EMPLOYEE_PROFILE, USER_ROLE, and EMPLOYEE_THEATRE persistence")
    void testUserAndProfilePersistence() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // 1. User
        User user = createTestUser("cust");
        assertNotNull(user.getUserId());

        // 2. Customer Profile
        CustomerProfile cp = new CustomerProfile(user.getUserId(), 1L, LocalDate.of(1995, 5, 20));
        cp = customerProfileRepository.save(cp);
        assertEquals(user.getUserId(), cp.getUserId());

        // 3. User Role
        Role customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER").orElseThrow();
        UserRole ur = new UserRole(user.getUserId(), customerRole.getRoleId());
        ur = userRoleRepository.save(ur);
        assertNotNull(ur.getId());

        // 4. Employee Profile & Employee Theatre
        User empUser = createTestUser("emp");

        EmployeeProfile ep = new EmployeeProfile(empUser.getUserId(), "EMP-" + uid);
        ep = employeeProfileRepository.save(ep);
        assertEquals(empUser.getUserId(), ep.getUserId());

        City city = cityRepository.findAll().get(0);
        Theatre theatre = new Theatre();
        theatre.setCityId(city.getCityId());
        theatre.setTheatreCode("TH-" + uid);
        theatre.setTheatreName("Test Theatre " + uid);
        theatre.setAddressLine1("123 Main St");
        theatre.setStatus("ACTIVE");
        theatre = theatreRepository.save(theatre);

        EmployeeTheatre et = new EmployeeTheatre(empUser.getUserId(), theatre.getTheatreId());
        et = employeeTheatreRepository.save(et);
        assertNotNull(et.getId());
        assertTrue(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(empUser.getUserId(), theatre.getTheatreId()));
    }

    @Test
    @DisplayName("Verify THEATRE, SCREEN, SCREEN_CAPABILITY, and SEAT persistence and query execution")
    void testTheatreScreenSeatPersistenceAndQueries() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        Theatre theatre = new Theatre();
        theatre.setCityId(city.getCityId());
        theatre.setTheatreCode("TH-SCR-" + uid);
        theatre.setTheatreName("Multiplex " + uid);
        theatre.setAddressLine1("Address " + uid);
        theatre.setStatus("ACTIVE");
        theatre = theatreRepository.save(theatre);
        assertNotNull(theatre.getTheatreId());
        assertEquals("ACTIVE", theatre.getStatus());

        Screen screen = new Screen(theatre.getTheatreId(), "SCR-1-" + uid, "Screen 1");
        screen = screenRepository.save(screen);
        assertNotNull(screen.getScreenId());
        assertEquals("ACTIVE", screen.getStatus());

        ScreenCapability sc = new ScreenCapability(screen.getScreenId(), 1L, 1L);
        sc = screenCapabilityRepository.save(sc);
        assertNotNull(sc.getScreenCapabilityId());

        SeatType standardType = seatTypeRepository.findByTypeCode("STANDARD").orElseThrow();
        Seat seat1 = new Seat(screen.getScreenId(), standardType.getSeatTypeId(), "A", "1");
        Seat seat2 = new Seat(screen.getScreenId(), standardType.getSeatTypeId(), "A", "2");
        seatRepository.saveAll(List.of(seat1, seat2));

        // Test custom query methods
        List<Screen> activeScreens = screenRepository.findByTheatreIdAndIsActiveTrue(theatre.getTheatreId());
        assertEquals(1, activeScreens.size());

        List<Seat> activeSeats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(screen.getScreenId());
        assertEquals(2, activeSeats.size());
        assertEquals("A", activeSeats.get(0).getRowLabel());
        assertEquals("1", activeSeats.get(0).getSeatNumber());
    }

    @Test
    @DisplayName("Verify MOVIE, MOVIE_LANGUAGE, SHOW, and SHOW_SEAT persistence and queries")
    void testShowAndShowSeatPersistenceAndQueries() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        // 1. Setup Movie (status must be in UPCOMING, AIRING, ENDED, INACTIVE per chk_movie_status)
        Movie movie = new Movie();
        movie.setTitle("Movie " + uid);
        movie.setSynopsis("Test movie synopsis");
        movie.setRuntimeMinutes((short) 120);
        movie.setReleaseDate(LocalDate.now());
        movie.setStatus("AIRING");
        movie.setCertificationId(1L);
        movie = movieRepository.save(movie);
        assertNotNull(movie.getMovieId());
        assertEquals((short) 120, movie.getRuntimeMinutes());
        assertEquals("AIRING", movie.getStatus());

        MovieLanguage ml = new MovieLanguage(movie.getMovieId(), 1L, "ORIGINAL");
        ml = movieLanguageRepository.save(ml);
        assertNotNull(ml.getMovieLanguageId());

        // 2. Setup Theatre, Screen, Capability, Seat
        Theatre theatre = new Theatre();
        theatre.setCityId(city.getCityId());
        theatre.setTheatreCode("TH-SHW-" + uid);
        theatre.setTheatreName("Th " + uid);
        theatre.setAddressLine1("Addr");
        theatre.setStatus("ACTIVE");
        theatre = theatreRepository.save(theatre);

        Screen screen = screenRepository.save(new Screen(theatre.getTheatreId(), "SCR-" + uid, "Auditorium 1"));
        ScreenCapability cap = screenCapabilityRepository.save(new ScreenCapability(screen.getScreenId(), 1L, 1L));

        SeatType st = seatTypeRepository.findByTypeCode("STANDARD").orElseThrow();
        Seat seat = seatRepository.save(new Seat(screen.getScreenId(), st.getSeatTypeId(), "A", "1"));

        // 3. Setup Show
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Show show = new Show(ml.getMovieLanguageId(), cap.getScreenCapabilityId(), start, end, "SCHEDULED");
        show = showRepository.save(show);
        assertNotNull(show.getShowId());
        assertEquals("SCHEDULED", show.getShowStatus());

        // 4. Overlap query verification
        long overlaps = showRepository.countOverlappingShows(screen.getScreenId(), start.minus(30, ChronoUnit.MINUTES), end.plus(30, ChronoUnit.MINUTES));
        assertEquals(1, overlaps, "Should detect 1 overlapping show");

        // 5. Setup ShowSeat
        ShowSeat showSeat = new ShowSeat(show.getShowId(), seat.getSeatId(), "AVAILABLE");
        showSeat = showSeatRepository.save(showSeat);
        assertNotNull(showSeat.getId());
        assertEquals("AVAILABLE", showSeat.getAvailabilityStatus());

        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(show.getShowId());
        assertEquals(1, showSeats.size());
    }

    @Test
    @DisplayName("Verify AUDIT_LOG persistence with authoritative column mapping")
    void testAuditLogPersistence() {
        User actor = createTestUser("audit_actor");
        AuditLog log = new AuditLog(actor.getUserId(), "TEST_ACTION", "THEATRE", 100L, "{}", "{\"action\":\"test\"}");
        log = auditLogRepository.save(log);
        assertNotNull(log.getAuditLogId());
        assertNotNull(log.getOccurredAt());
        assertEquals("TEST_ACTION", log.getAction());
        assertEquals("{\"action\":\"test\"}", log.getNewValue());
    }

    @Test
    @DisplayName("Verify SEARCH_INDEX_DOCUMENT, SEARCH_QUERY, and SEARCH_RESULT persistence")
    void testSearchPersistence() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        User searchUser = createTestUser("search_user");

        SearchIndexDocument doc = new SearchIndexDocument("MOVIE", 1L, "{\"title\":\"Action Movie\"}");
        doc = searchIndexDocumentRepository.save(doc);
        assertNotNull(doc.getDocumentId());
        assertEquals("MOVIE", doc.getEntityType());
        assertEquals(1L, doc.getEntityId());

        SearchQuery query = new SearchQuery(searchUser.getUserId(), "action movie " + uid, 5);
        query = searchQueryRepository.save(query);
        assertNotNull(query.getSearchQueryId());
        assertEquals(5, query.getResultCount());

        SearchResult result = new SearchResult(query.getSearchQueryId(), "MOVIE", 1L, 1, "HYBRID");
        result = searchResultRepository.save(result);
        assertNotNull(result.getSearchResultId());
        assertEquals(1, result.getRankPosition());
    }
}
