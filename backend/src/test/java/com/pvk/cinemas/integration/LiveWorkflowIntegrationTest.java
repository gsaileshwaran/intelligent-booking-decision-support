package com.pvk.cinemas.integration;

import com.pvk.cinemas.audit.model.AuditLog;
import com.pvk.cinemas.audit.repository.AuditLogRepository;
import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.auth.dto.AuthResponse;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.auth.dto.RegisterRequest;
import com.pvk.cinemas.auth.service.AuthService;
import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import com.pvk.cinemas.security.JwtTokenProvider;
import com.pvk.cinemas.security.TokenRevocationStore;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.CustomerProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class LiveWorkflowIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private SeatAvailabilityService seatAvailabilityService;
    @Autowired private ShowSchedulingService showSchedulingService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerProfileRepository customerProfileRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatTypeRepository seatTypeRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TokenRevocationStore tokenRevocationStore;

    @Test
    @DisplayName("End-to-end registration, login, logout, and token revocation against live MySQL")
    void testLiveRegistrationWorkflow() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "testuser_" + uniqueSuffix + "@example.com";
        String phone = "+919" + (int)(Math.random() * 90000000 + 10000000);

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Integration");
        request.setLastName("Tester");
        request.setEmail(email);
        request.setPassword("SecurePassword123!");
        request.setPhone(phone);
        request.setPreferredLanguageId(1); // Seed language (Tamil)

        // 1. Execute Registration
        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.getUserId());
        assertNotNull(response.getToken());
        assertEquals(email, response.getEmail());
        assertEquals("CUSTOMER", response.getRole());

        // 2. Verify Database Persistence in MySQL
        User savedUser = userRepository.findById(response.getUserId()).orElse(null);
        assertNotNull(savedUser);
        assertEquals("ACTIVE", savedUser.getAccountStatus());
        assertTrue(customerProfileRepository.findById(response.getUserId()).isPresent());
        assertFalse(userRoleRepository.findByIdUserId(response.getUserId()).isEmpty());

        // 3. Test Live Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("SecurePassword123!");

        AuthResponse loginResponse = authService.login(loginRequest);
        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getToken());
        assertEquals(response.getUserId(), loginResponse.getUserId());

        // 4. Test Live Logout
        String rawToken = loginResponse.getToken();
        assertDoesNotThrow(() -> authService.logout("Bearer " + rawToken));

        // 5. Test Revoked Token Rejection
        String jti = jwtTokenProvider.getJti(rawToken);
        assertTrue(tokenRevocationStore.isRevoked(jti), "Token must be marked revoked in TokenRevocationStore after logout");
    }

    @Test
    @DisplayName("Live Show and Seat Availability retrieval against live database")
    void testLiveShowAndSeatAvailability() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        // 1. Create Theatre & Screen
        Theatre theatre = new Theatre();
        theatre.setCityId(city.getCityId());
        theatre.setTheatreCode("TH-LVE-" + uid);
        theatre.setTheatreName("Live Theatre " + uid);
        theatre.setAddressLine1("Test Avenue");
        theatre.setStatus("ACTIVE");
        theatre = theatreRepository.save(theatre);

        Screen screen = screenRepository.save(new Screen(theatre.getTheatreId(), "SCR-LVE-" + uid, "Auditorium Live"));
        ScreenCapability cap = screenCapabilityRepository.save(new ScreenCapability(screen.getScreenId(), 1L, 1L));

        SeatType standardType = seatTypeRepository.findByTypeCode("STANDARD").orElseThrow();
        Seat seat1 = seatRepository.save(new Seat(screen.getScreenId(), standardType.getSeatTypeId(), "A", "1"));
        Seat seat2 = seatRepository.save(new Seat(screen.getScreenId(), standardType.getSeatTypeId(), "A", "2"));

        // 2. Create Movie & Language
        Movie movie = new Movie();
        movie.setTitle("Live Movie " + uid);
        movie.setSynopsis("Live movie synopsis");
        movie.setRuntimeMinutes((short) 120);
        movie.setReleaseDate(LocalDate.now());
        movie.setStatus("AIRING");
        movie.setCertificationId(1L);
        movie = movieRepository.save(movie);

        MovieLanguage ml = movieLanguageRepository.save(new MovieLanguage(movie.getMovieId(), 1L, "ORIGINAL"));

        // 3. Create Manager User for Audit Actor
        User managerUser = userRepository.save(new User(
                "Manager", "User", "mgr_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000),
                "$2a$10$abcdefghijklmnopqrstuvwxyz123456", "ACTIVE"
        ));

        // 4. Schedule Show
        Instant start = Instant.now().plusSeconds(7200);
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(screen.getScreenId().intValue());
        req.setMovieId(movie.getMovieId());
        req.setMovieLanguageId(ml.getMovieLanguageId());
        req.setScreenCapabilityId(cap.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);
        req.setShowStatus("SCHEDULED");

        ShowResponse createdShow = showSchedulingService.createShow(theatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1");
        assertNotNull(createdShow);
        assertNotNull(createdShow.getShowId());

        // 4. Retrieve Seat Availability for scheduled show
        ShowSeatAvailabilityResponse layout = seatAvailabilityService.getShowSeatAvailability(createdShow.getShowId());
        assertNotNull(layout);
        assertEquals(createdShow.getShowId(), layout.getShowId());
        assertNotNull(layout.getSeats());
        assertEquals(2, layout.getSeats().size(), "Show must have initialized 2 seats from physical screen");

        // Verify Seat DTO fields
        layout.getSeats().forEach(s -> {
            assertNotNull(s.getSeatId());
            assertNotNull(s.getRowLabel());
            assertNotNull(s.getSeatNumber());
            assertEquals("AVAILABLE", s.getAvailabilityStatus());
        });

        // 5. Retrieve Shows for Theatre
        List<ShowResponse> theatreShows = showSchedulingService.getShowsForTheatre(theatre.getTheatreId().intValue());
        assertNotNull(theatreShows);
        assertFalse(theatreShows.isEmpty());
        assertTrue(theatreShows.stream().anyMatch(s -> s.getShowId().equals(createdShow.getShowId())));

        // 6. Retrieve Shows for Movie
        List<ShowResponse> movieShows = showSchedulingService.getShowsForMovie(movie.getMovieId());
        assertNotNull(movieShows);
        assertFalse(movieShows.isEmpty());
        assertTrue(movieShows.stream().anyMatch(s -> s.getShowId().equals(createdShow.getShowId())));
    }

    @Test
    @DisplayName("Live Audit Log creation and retrieval against AUDIT_LOG table")
    void testLiveAuditLogging() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // Create actor user
        User actor = new User(
                "Audit", "Actor", "audit_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000),
                "$2a$10$abcdefghijklmnopqrstuvwxyz123456", "ACTIVE"
        );
        actor = userRepository.save(actor);

        long entityId = System.currentTimeMillis();
        long initialCount = auditLogRepository.count();

        auditLogService.logAction(
                actor.getUserId(),
                "TEST_ACTION",
                "TEST_ENTITY",
                entityId,
                "{\"before\":\"val1\"}",
                "{\"after\":\"val2\"}"
        );

        long newCount = auditLogRepository.count();
        assertEquals(initialCount + 1, newCount, "Audit log record must be inserted into AUDIT_LOG table");

        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("TEST_ENTITY", entityId);
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals("TEST_ACTION", log.getAction());
        assertEquals(actor.getUserId(), log.getActorUserId());
        assertNotNull(log.getOccurredAt());
    }
}
