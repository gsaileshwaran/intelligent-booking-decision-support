package com.pvk.cinemas.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.auth.dto.RegisterRequest;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.infrastructure.dto.ScreenRequest;
import com.pvk.cinemas.infrastructure.dto.SeatRequest;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.infrastructure.service.ScreenService;
import com.pvk.cinemas.infrastructure.service.SeatService;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class P4AuthenticationAndAuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EmployeeProfileRepository employeeProfileRepository;
    @Autowired private EmployeeTheatreRepository employeeTheatreRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatTypeRepository seatTypeRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private LanguageRepository languageRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private PresentationFormatRepository presentationFormatRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TokenRevocationStore tokenRevocationStore;
    @Autowired private ScreenService screenService;
    @Autowired private SeatService seatService;
    @Autowired private ShowSchedulingService showSchedulingService;
    @Autowired private TheatreScopeService theatreScopeService;

    private User customerUser;
    private User managerA;
    private User managerMulti;
    private User managerZero;
    private User superAdmin;
    private User suspendedUser;

    private String customerToken;
    private String managerAToken;
    private String managerMultiToken;
    private String managerZeroToken;
    private String superAdminToken;

    private Theatre theatreA;
    private Theatre theatreB;
    private Theatre theatreC;
    private Screen screenA;
    private Screen screenB;
    private Seat seatA;
    private Seat seatB;
    private Show showA;
    private Show showB;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        Role managerRole = roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").orElseThrow();
        Role superAdminRole = roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").orElseThrow();
        Role customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER").orElseThrow();

        // 1. Theatres
        theatreA = new Theatre();
        theatreA.setCityId(city.getCityId());
        theatreA.setTheatreCode("TH-A-" + uid);
        theatreA.setTheatreName("PVR A " + uid);
        theatreA.setAddressLine1("Street A");
        theatreA.setStatus("ACTIVE");
        theatreA = theatreRepository.save(theatreA);

        theatreB = new Theatre();
        theatreB.setCityId(city.getCityId());
        theatreB.setTheatreCode("TH-B-" + uid);
        theatreB.setTheatreName("PVR B " + uid);
        theatreB.setAddressLine1("Street B");
        theatreB.setStatus("ACTIVE");
        theatreB = theatreRepository.save(theatreB);

        theatreC = new Theatre();
        theatreC.setCityId(city.getCityId());
        theatreC.setTheatreCode("TH-C-" + uid);
        theatreC.setTheatreName("PVR C " + uid);
        theatreC.setAddressLine1("Street C");
        theatreC.setStatus("ACTIVE");
        theatreC = theatreRepository.save(theatreC);

        // 2. Screens
        screenA = new Screen();
        screenA.setTheatreId(theatreA.getTheatreId());
        screenA.setScreenCode("SCR-A-" + uid);
        screenA.setScreenName("Screen 1 Alpha");
        screenA.setStatus("ACTIVE");
        screenA = screenRepository.save(screenA);

        screenB = new Screen();
        screenB.setTheatreId(theatreB.getTheatreId());
        screenB.setScreenCode("SCR-B-" + uid);
        screenB.setScreenName("Screen 1 Beta");
        screenB.setStatus("ACTIVE");
        screenB = screenRepository.save(screenB);

        // 3. Seats
        SeatType seatType = seatTypeRepository.findAll().get(0);
        seatA = new Seat(screenA.getScreenId(), seatType.getSeatTypeId(), "A", "1");
        seatA.setStatus("ACTIVE");
        seatA = seatRepository.save(seatA);

        seatB = new Seat(screenB.getScreenId(), seatType.getSeatTypeId(), "B", "1");
        seatB.setStatus("ACTIVE");
        seatB = seatRepository.save(seatB);

        // 4. Movies & Shows
        Movie movie = movieRepository.findAll().get(0);
        Language lang = languageRepository.findAll().get(0);
        MovieLanguage ml = movieLanguageRepository.findByMovieId(movie.getMovieId()).stream().findFirst()
                .orElseGet(() -> movieLanguageRepository.save(new MovieLanguage(movie.getMovieId(), lang.getLanguageId(), "ORIGINAL")));

        PresentationFormat pf = presentationFormatRepository.findAll().get(0);
        ScreenCapability capA = screenCapabilityRepository.save(new ScreenCapability(screenA.getScreenId(), pf.getPresentationFormatId(), 1L));
        ScreenCapability capB = screenCapabilityRepository.save(new ScreenCapability(screenB.getScreenId(), pf.getPresentationFormatId(), 1L));

        Instant start1 = Instant.now().plus(48, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        Instant end1 = start1.plus(120, ChronoUnit.MINUTES);

        showA = new Show();
        showA.setMovieLanguageId(ml.getMovieLanguageId());
        showA.setScreenCapabilityId(capA.getScreenCapabilityId());
        showA.setScreenId(screenA.getScreenId());
        showA.setStartAt(start1);
        showA.setEndAt(end1);
        showA.setShowStatus("SCHEDULED");
        showA = showRepository.save(showA);

        showB = new Show();
        showB.setMovieLanguageId(ml.getMovieLanguageId());
        showB.setScreenCapabilityId(capB.getScreenCapabilityId());
        showB.setScreenId(screenB.getScreenId());
        showB.setStartAt(start1);
        showB.setEndAt(end1);
        showB.setShowStatus("SCHEDULED");
        showB = showRepository.save(showB);

        // 5. Users & Roles
        customerUser = createUser("cust_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(customerUser.getUserId(), customerRole.getRoleId()));

        managerA = createUser("mgrA_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(managerA.getUserId(), managerRole.getRoleId()));
        EmployeeProfile empA = new EmployeeProfile(managerA.getUserId(), "EMP-A-" + uid);
        employeeProfileRepository.save(empA);
        employeeTheatreRepository.save(new EmployeeTheatre(managerA.getUserId(), theatreA.getTheatreId()));

        managerMulti = createUser("mgrMulti_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(managerMulti.getUserId(), managerRole.getRoleId()));
        EmployeeProfile empMulti = new EmployeeProfile(managerMulti.getUserId(), "EMP-M-" + uid);
        employeeProfileRepository.save(empMulti);
        employeeTheatreRepository.save(new EmployeeTheatre(managerMulti.getUserId(), theatreA.getTheatreId()));
        employeeTheatreRepository.save(new EmployeeTheatre(managerMulti.getUserId(), theatreB.getTheatreId()));

        managerZero = createUser("mgrZero_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(managerZero.getUserId(), managerRole.getRoleId()));
        EmployeeProfile empZero = new EmployeeProfile(managerZero.getUserId(), "EMP-Z-" + uid);
        employeeProfileRepository.save(empZero);
        // zero theatre assignments

        superAdmin = createUser("admin_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(superAdmin.getUserId(), superAdminRole.getRoleId()));

        suspendedUser = createUser("susp_" + uid + "@pvk.com", "SUSPENDED");
        userRoleRepository.save(new UserRole(suspendedUser.getUserId(), customerRole.getRoleId()));

        // Obtain tokens
        customerToken = loginAndGetToken(customerUser.getEmail(), "Password123!");
        managerAToken = loginAndGetToken(managerA.getEmail(), "Password123!");
        managerMultiToken = loginAndGetToken(managerMulti.getEmail(), "Password123!");
        managerZeroToken = loginAndGetToken(managerZero.getEmail(), "Password123!");
        superAdminToken = loginAndGetToken(superAdmin.getEmail(), "Password123!");
    }

    private User createUser(String email, String status) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
        u.setFirstName("First");
        u.setLastName("Last");
        String phone;
        do {
            phone = "+919" + (long) (Math.random() * 900000000L + 100000000L);
        } while (userRepository.existsByPhone(phone));
        u.setPhone(phone);
        u.setAccountStatus(status);
        return userRepository.save(u);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("token").asText();
    }

    // =========================================================================
    // SECTION 14.1 — AUTHENTICATION VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("14.1.1: Valid credentials -> successful authentication (200 + token + cookie)")
    void testValidCredentialsAuthentication() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(customerUser.getEmail());
        req.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(body.path("token").asText());
        assertTrue(body.path("role").asText().contains("CUSTOMER"));
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("pvk_token"));
    }

    @Test
    @DisplayName("14.1.2: Invalid password -> authentication rejected (401)")
    void testInvalidPasswordRejected() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(customerUser.getEmail());
        req.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("14.1.3: Unknown user -> authentication rejected (401)")
    void testUnknownUserRejected() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("nonexistent_" + UUID.randomUUID() + "@pvk.com");
        req.setPassword("AnyPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("14.1.4: Unauthenticated protected request -> 401")
    void testUnauthenticatedProtectedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("14.1.5: Revoked or invalid token -> rejected (401)")
    void testRevokedTokenRejected() throws Exception {
        // Log out to revoke the token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Subsequent call with revoked token must return 401
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("14.1.6: Password hash is never exposed through API DTOs")
    void testPasswordNeverExposedInDto() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn();

        String content = res.getResponse().getContentAsString();
        assertFalse(content.contains("passwordHash"), "passwordHash must not appear in DTO response");
        assertFalse(content.contains("Password123!"), "Plaintext password must not appear in DTO response");
    }

    @Test
    @DisplayName("14.1.7: Inactive / suspended account rejected at login and token filter")
    void testSuspendedUserRejected() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(suspendedUser.getEmail());
        req.setPassword("Password123!");

        // Login fails with 403 Forbidden
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14.1.8: HttpOnly cookie authentication works identically to Bearer header")
    void testHttpOnlyCookieAuthentication() throws Exception {
        Cookie authCookie = new Cookie("pvk_token", customerToken);
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(customerUser.getEmail()));
    }

    @Test
    @DisplayName("14.1.9: Account disabled after token issuance -> subsequent requests return 401")
    void testAccountDisabledAfterTokenIssuanceRejected() throws Exception {
        // Initially active token works
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Account is deactivated in DB
        customerUser.setAccountStatus("SUSPENDED");
        userRepository.save(customerUser);

        // Next request with same token is rejected
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("14.1.10: Role revoked after token issuance -> subsequent manager requests return 403")
    void testRoleRevokedAfterTokenIssuanceForbidden() throws Exception {
        // Manager can initially access assigned theatre
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk());

        // Remove manager role from database
        userRoleRepository.deleteAll(userRoleRepository.findByIdUserId(managerA.getUserId()));

        // Next request with same token must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14.1.11: Theatre assignment revoked after token issuance -> subsequent requests return 403")
    void testTheatreAssignmentRevokedAfterTokenIssuanceForbidden() throws Exception {
        // Manager can initially access assigned theatre
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk());

        // Revoke theatre assignment in database
        employeeTheatreRepository.deleteAll(employeeTheatreRepository.findByIdUserId(managerA.getUserId()));

        // Next request with same token must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 14.2 & 14.3 — IDENTITY & ROLE AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("14.2.1: Authenticated request resolves correct identity and roles")
    void testIdentityAndRoleResolution() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertEquals(managerA.getUserId(), data.path("userId").asLong());
        assertEquals(managerA.getEmail(), data.path("email").asText());
        assertTrue(data.path("roles").toString().contains("THEATRE_MANAGER") || data.path("roles").toString().contains("ROLE_THEATRE_MANAGER"));
        assertEquals("EMP-A-", data.path("employeeCode").asText().substring(0, 6));
        assertTrue(data.path("assignedTheatreIds").toString().contains(String.valueOf(theatreA.getTheatreId())));
    }

    @Test
    @DisplayName("14.3.1: CUSTOMER cannot access manager-only operation (403)")
    void testCustomerCannotAccessManagerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14.3.2: CUSTOMER cannot access admin-only operation (403)")
    void testCustomerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14.3.3: THEATRE_MANAGER cannot access SUPER_ADMIN operation (403)")
    void testManagerCannotAccessSuperAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14.3.4: SUPER_ADMIN can access administrative operations platform-wide")
    void testSuperAdminGlobalAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        // Super Admin can access any theatre's manager endpoints
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // SECTION 14.4 & SECTION 15 — THEATRE SCOPE & CROSS-THEATRE ATTACK DENIAL
    // =========================================================================

    @Test
    @DisplayName("14.4.1: Manager can access assigned theatre (200)")
    void testManagerCanAccessAssignedTheatre() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("14.4.2: Manager cannot access unassigned theatre (403)")
    void testManagerCannotAccessUnassignedTheatre() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15.1: Cross-theatre attack: Manager A attempts to update Screen B in Theatre B (403)")
    void testCrossTheatreScreenUpdateForbidden() throws Exception {
        ScreenRequest req = new ScreenRequest();
        req.setScreenName("Hacked Screen B");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens/" + screenB.getScreenId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Screen B must remain untouched
        Screen unchanged = screenRepository.findById(screenB.getScreenId()).orElseThrow();
        assertEquals("Screen 1 Beta", unchanged.getScreenName());
    }

    @Test
    @DisplayName("15.2: Cross-theatre attack: Manager A tampers path (Theatre A path, Screen B ID) (403)")
    void testCrossTheatreScreenTamperingForbidden() throws Exception {
        ScreenRequest req = new ScreenRequest();
        req.setScreenName("Hacked Screen B");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens/" + screenB.getScreenId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Screen unchanged = screenRepository.findById(screenB.getScreenId()).orElseThrow();
        assertEquals("Screen 1 Beta", unchanged.getScreenName());
    }

    @Test
    @DisplayName("15.3: Cross-theatre attack: Manager A attempts to update Seat B in Screen B (403)")
    void testCrossTheatreSeatUpdateForbidden() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenB.getScreenId() + "/seats/" + seatB.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Seat unchanged = seatRepository.findById(seatB.getSeatId()).orElseThrow();
        assertTrue(unchanged.getIsActive());
    }

    @Test
    @DisplayName("15.4: Cross-theatre attack: Manager A tampers path with Screen A and Seat B (403)")
    void testCrossTheatreSeatTamperingForbidden() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatB.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Seat unchanged = seatRepository.findById(seatB.getSeatId()).orElseThrow();
        assertTrue(unchanged.getIsActive());
    }

    @Test
    @DisplayName("15.5: Cross-theatre attack: Manager A attempts to update Show B in Theatre B (403)")
    void testCrossTheatreShowUpdateForbidden() throws Exception {
        ShowRequest req = new ShowRequest();
        req.setShowStatus("CANCELLED");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/shows/" + showB.getShowId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Show unchanged = showRepository.findById(showB.getShowId()).orElseThrow();
        assertEquals("SCHEDULED", unchanged.getShowStatus());
    }

    @Test
    @DisplayName("15.6: Cross-theatre attack: Manager A tampers path with Theatre A and Show B ID (403)")
    void testCrossTheatreShowTamperingForbidden() throws Exception {
        ShowRequest req = new ShowRequest();
        req.setShowStatus("CANCELLED");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/shows/" + showB.getShowId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Show unchanged = showRepository.findById(showB.getShowId()).orElseThrow();
        assertEquals("SCHEDULED", unchanged.getShowStatus());
    }

    @Test
    @DisplayName("14.4.3: Manager with multiple assignments can access only assigned theatres")
    void testMultiTheatreManagerScope() throws Exception {
        // Manager Multi is assigned Theatre A and Theatre B
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerMultiToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerMultiToken))
                .andExpect(status().isOk());

        // But is strictly DENIED Theatre C
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreC.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerMultiToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14.4.4: Manager with no theatre assignment is denied all manager operations (403)")
    void testZeroAssignmentManagerDenied() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerZeroToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerZeroToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 14.5 — SERVICE-LEVEL AUTHORIZATION ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("14.5.1: Direct invocation of ScreenService below controller enforces scope")
    void testServiceLevelScreenAuthorization() {
        // Set SecurityContext for Manager A (assigned to Theatre A)
        UserPrincipal principal = new UserPrincipal(
                managerA.getUserId(),
                managerA.getEmail(),
                "Password123!",
                "ACTIVE",
                List.of(new SimpleGrantedAuthority("ROLE_THEATRE_MANAGER")),
                "EMP-A",
                List.of("ROLE_THEATRE_MANAGER"),
                List.of("SCREEN_READ", "SCREEN_WRITE"),
                List.of(theatreA.getTheatreId())
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Accessing Theatre A succeeds
        assertDoesNotThrow(() -> screenService.getScreensByTheatre(theatreA.getTheatreId().intValue()));

        // Directly invoking getScreensByTheatre for Theatre B throws AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                screenService.getScreensByTheatre(theatreB.getTheatreId().intValue())
        );

        // Directly invoking createScreen for Theatre B throws AccessDeniedException
        ScreenRequest req = new ScreenRequest();
        req.setScreenName("Test Screen");
        req.setScreenCode("SC-TEST");
        req.setIsActive(true);
        assertThrows(AccessDeniedException.class, () ->
                screenService.createScreen(theatreB.getTheatreId().intValue(), req, managerA.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("14.5.2: Direct invocation of SeatService below controller enforces scope")
    void testServiceLevelSeatAuthorization() {
        UserPrincipal principal = new UserPrincipal(
                managerA.getUserId(),
                managerA.getEmail(),
                "Password123!",
                "ACTIVE",
                List.of(new SimpleGrantedAuthority("ROLE_THEATRE_MANAGER")),
                "EMP-A",
                List.of("ROLE_THEATRE_MANAGER"),
                List.of("SEAT_READ", "SEAT_WRITE"),
                List.of(theatreA.getTheatreId())
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Accessing Screen A seats succeeds
        assertDoesNotThrow(() -> seatService.getSeatsByScreen(screenA.getScreenId().intValue()));

        // Directly invoking getSeatsByScreen for Screen B throws AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                seatService.getSeatsByScreen(screenB.getScreenId().intValue())
        );

        // Directly invoking updateSeat for Seat B throws AccessDeniedException
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);
        assertThrows(AccessDeniedException.class, () ->
                seatService.updateSeat(screenB.getScreenId().intValue(), seatB.getSeatId(), req, managerA.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("14.5.3: Direct invocation of ShowSchedulingService below controller enforces scope")
    void testServiceLevelShowAuthorization() {
        UserPrincipal principal = new UserPrincipal(
                managerA.getUserId(),
                managerA.getEmail(),
                "Password123!",
                "ACTIVE",
                List.of(new SimpleGrantedAuthority("ROLE_THEATRE_MANAGER")),
                "EMP-A",
                List.of("ROLE_THEATRE_MANAGER"),
                List.of("SHOW_READ", "SHOW_WRITE"),
                List.of(theatreA.getTheatreId())
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ShowRequest req = new ShowRequest();
        req.setShowStatus("CANCELLED");

        // Directly invoking updateShow for Theatre B throws AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                showSchedulingService.updateShow(theatreB.getTheatreId().intValue(), showB.getShowId(), req, managerA.getUserId(), "127.0.0.1")
        );
    }

    // =========================================================================
    // SECTION 14.6 — PUBLIC ENDPOINT REGRESSION
    // =========================================================================

    @Test
    @DisplayName("14.6.1: Public discovery endpoints remain accessible anonymously")
    void testPublicDiscoveryAccessibleAnonymously() throws Exception {
        // Theatres list
        mockMvc.perform(get("/api/v1/theatres"))
                .andExpect(status().isOk());

        // Theatres by city
        mockMvc.perform(get("/api/v1/theatres?cityId=" + theatreA.getCityId()))
                .andExpect(status().isOk());

        // Shows for theatre
        mockMvc.perform(get("/api/v1/theatres/" + theatreA.getTheatreId() + "/shows"))
                .andExpect(status().isOk());

        // Movies list
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk());

        // Unified search
        mockMvc.perform(get("/api/v1/search?query=PVR"))
                .andExpect(status().isOk());
    }
}
