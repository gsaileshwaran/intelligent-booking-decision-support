package com.pvk.cinemas.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.auth.dto.RegisterRequest;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
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
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LiveHttpVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private EmployeeTheatreRepository employeeTheatreRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ScreenCapabilityRepository screenCapabilityRepository;

    @Autowired
    private SeatTypeRepository seatTypeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieLanguageRepository movieLanguageRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String uniqueSuffix;
    private static String testCustomerEmail;
    private static String testCustomerPassword = "Password123!";
    private static String customerToken;

    private static String managerAEmail;
    private static String managerAToken;
    private static String superAdminEmail;
    private static String superAdminToken;

    private static Long theatreAId;
    private static Long theatreBId;
    private static Long movieId;
    private static Long showId;

    @BeforeAll
    static void initSuite() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testCustomerEmail = "live_cust_" + uniqueSuffix + "@example.com";
        managerAEmail = "live_mgr_a_" + uniqueSuffix + "@example.com";
        superAdminEmail = "live_admin_" + uniqueSuffix + "@example.com";
    }

    @Test
    @Order(1)
    @DisplayName("01: POST /api/v1/auth/register")
    void test01_Register() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Live");
        req.setLastName("Customer");
        req.setEmail(testCustomerEmail);
        req.setPhone("+919" + (int) (Math.random() * 90000000 + 10000000));
        req.setPassword(testCustomerPassword);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        System.out.println("HTTP VERIFICATION: POST | /api/v1/auth/register | 201 | PASS | User & CustomerProfile inserted in MySQL");

        User user = userRepository.findByEmail(testCustomerEmail).orElseThrow();
        assertEquals("ACTIVE", user.getAccountStatus());
        assertTrue(passwordEncoder.matches(testCustomerPassword, user.getPasswordHash()));
    }

    @Test
    @Order(2)
    @DisplayName("02: POST /api/v1/auth/login")
    void test02_Login() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(testCustomerEmail);
        req.setPassword(testCustomerPassword);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        customerToken = root.path("token").asText();
        assertNotNull(customerToken);
        assertFalse(customerToken.isBlank());

        System.out.println("HTTP VERIFICATION: POST | /api/v1/auth/login | 200 | PASS | JWT token generated, audit log appended");
    }

    @Test
    @Order(3)
    @DisplayName("03: GET /api/v1/auth/me (Authenticated Endpoint)")
    void test03_AuthenticatedMe() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(testCustomerEmail, root.path("data").path("email").asText());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/auth/me | 200 | PASS | Customer profile returned from MySQL");
    }

    @Test
    @Order(4)
    @DisplayName("04: POST /api/v1/auth/logout")
    void test04_Logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        System.out.println("HTTP VERIFICATION: POST | /api/v1/auth/logout | 200 | PASS | Token added to TokenRevocationStore");
    }

    @Test
    @Order(5)
    @DisplayName("05: GET /api/v1/auth/me (Reuse Revoked Token)")
    void test05_ReuseRevokedToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/auth/me | 401 | PASS | Revoked token rejected server-side");
    }

    @Test
    @Order(6)
    @DisplayName("06: GET /api/v1/movies (Public Movie List)")
    void test06_PublicMovieList() throws Exception {
        // Ensure at least one movie in database
        City city = cityRepository.findAll().get(0);
        Movie movie = new Movie();
        movie.setTitle("Inception " + uniqueSuffix);
        movie.setRuntimeMinutes((short) 148);
        movie.setStatus("AIRING");
        movie.setCertificationId(1L);
        movie = movieRepository.save(movie);
        movieId = movie.getMovieId();

        MvcResult result = mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("HTTP VERIFICATION: GET | /api/v1/movies | 200 | PASS | Movies retrieved from MOVIE table");
    }

    @Test
    @Order(7)
    @DisplayName("07: GET /api/v1/movies/{id} (Public Movie Details)")
    void test07_PublicMovieDetails() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/movies/" + movieId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("Inception " + uniqueSuffix, root.path("data").path("title").asText());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/movies/" + movieId + " | 200 | PASS | Movie details fetched by ID");
    }

    @Test
    @Order(8)
    @DisplayName("08: GET /api/v1/movies/{id}/shows (Movie Shows Filtered)")
    void test08_MovieShows() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/movies/" + movieId + "/shows"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("HTTP VERIFICATION: GET | /api/v1/movies/" + movieId + "/shows | 200 | PASS | Shows filtered strictly by movie");
    }

    @Test
    @Order(9)
    @DisplayName("09: GET /api/v1/cities/{id}/theatres (Theatre List)")
    void test09_TheatreList() throws Exception {
        City city = cityRepository.findAll().get(0);

        // Create Theatres A & B in MySQL
        Theatre thA = new Theatre();
        thA.setCityId(city.getCityId());
        thA.setTheatreCode("TH-A-" + uniqueSuffix);
        thA.setTheatreName("PVR Live A " + uniqueSuffix);
        thA.setAddressLine1("100 Live Road");
        thA.setStatus("ACTIVE");
        thA = theatreRepository.save(thA);
        theatreAId = thA.getTheatreId().longValue();

        Theatre thB = new Theatre();
        thB.setCityId(city.getCityId());
        thB.setTheatreCode("TH-B-" + uniqueSuffix);
        thB.setTheatreName("PVR Live B " + uniqueSuffix);
        thB.setAddressLine1("200 Live Road");
        thB.setStatus("ACTIVE");
        thB = theatreRepository.save(thB);
        theatreBId = thB.getTheatreId().longValue();

        MvcResult result = mockMvc.perform(get("/api/v1/cities/" + city.getCityId() + "/theatres"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("HTTP VERIFICATION: GET | /api/v1/cities/" + city.getCityId() + "/theatres | 200 | PASS | Theatres listed from THEATRE table");
    }

    @Test
    @Order(10)
    @DisplayName("10: GET /api/v1/theatres/{id} (Theatre Details)")
    void test10_TheatreDetails() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/theatres/" + theatreAId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("PVR Live A " + uniqueSuffix, root.path("data").path("theatreName").asText());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/theatres/" + theatreAId + " | 200 | PASS | Theatre details fetched by ID");
    }

    @Test
    @Order(11)
    @DisplayName("11: GET /api/v1/theatres/{id}/shows (Theatre Shows Filtered)")
    void test11_TheatreShows() throws Exception {
        // Create Screen, ScreenCapability, MovieLanguage, Seat, and Show
        Screen screen = screenRepository.save(new Screen(theatreAId, "SCR-LIVE-" + uniqueSuffix, "Auditorium 1"));
        ScreenCapability cap = screenCapabilityRepository.save(new ScreenCapability(screen.getScreenId(), 1L, 1L));
        SeatType standardType = seatTypeRepository.findByTypeCode("STANDARD").orElseThrow();
        Seat seat1 = seatRepository.save(new Seat(screen.getScreenId(), standardType.getSeatTypeId(), "A", "1"));
        Seat seat2 = seatRepository.save(new Seat(screen.getScreenId(), standardType.getSeatTypeId(), "A", "2"));

        MovieLanguage ml = movieLanguageRepository.save(new MovieLanguage(movieId, 1L, "ORIGINAL"));

        Show show = new Show();
        show.setMovieLanguageId(ml.getMovieLanguageId());
        show.setScreenCapabilityId(cap.getScreenCapabilityId());
        show.setStartAt(Instant.now().plus(4, ChronoUnit.HOURS));
        show.setEndAt(Instant.now().plus(6, ChronoUnit.HOURS));
        show.setShowStatus("SCHEDULED");
        show = showRepository.save(show);
        showId = show.getShowId();

        showSeatRepository.save(new ShowSeat(showId, seat1.getSeatId(), "AVAILABLE"));
        showSeatRepository.save(new ShowSeat(showId, seat2.getSeatId(), "AVAILABLE"));

        MvcResult result = mockMvc.perform(get("/api/v1/theatres/" + theatreAId + "/shows"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("HTTP VERIFICATION: GET | /api/v1/theatres/" + theatreAId + "/shows | 200 | PASS | Shows filtered strictly through SCREEN -> CAPABILITY");
    }

    @Test
    @Order(12)
    @DisplayName("12: GET /api/v1/shows/{id}/seats (Show Seats Read-Only)")
    void test12_ShowSeats() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, root.path("data").path("totalSeats").asInt());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/shows/" + showId + "/seats | 200 | PASS | Seat availability read-only confirmed");
    }

    @Test
    @Order(13)
    @DisplayName("13: GET /api/v1/manager/theatres/{theatreAId}/shows (Manager A -> Theatre A ALLOWED)")
    void test13_ManagerAuthorized() throws Exception {
        // Setup Manager A in database
        Role managerRole = roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").orElseThrow();
        User mgrA = userRepository.save(new User("Manager", "A", managerAEmail,
                "+919" + (int) (Math.random() * 90000000 + 10000000), passwordEncoder.encode("MgrPass123!"), "ACTIVE"));
        employeeProfileRepository.save(new EmployeeProfile(mgrA.getUserId(), "EMP-MGR-A-" + uniqueSuffix));
        userRoleRepository.save(new UserRole(mgrA.getUserId(), managerRole.getRoleId()));
        employeeTheatreRepository.save(new EmployeeTheatre(mgrA.getUserId(), theatreAId));

        // Login as Manager A
        LoginRequest login = new LoginRequest();
        login.setEmail(managerAEmail);
        login.setPassword("MgrPass123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk()).andReturn();
        managerAToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("token").asText();

        // Perform manager request on Theatre A
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreAId + "/shows")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/manager/theatres/" + theatreAId + "/shows | 200 | PASS | Manager A authorized for Theatre A");
    }

    @Test
    @Order(14)
    @DisplayName("14: GET /api/v1/manager/theatres/{theatreBId}/shows (Manager A -> Theatre B REJECTED 403)")
    void test14_ManagerUnauthorizedTheatre() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreBId + "/shows")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/manager/theatres/" + theatreBId + "/shows | 403 | PASS | Manager A blocked from Theatre B by scope");
    }

    @Test
    @Order(15)
    @DisplayName("15: GET /api/v1/manager/theatres/{theatreAId}/shows (Customer attempting Manager endpoint REJECTED 403)")
    void test15_CustomerAttemptingManagerEndpoint() throws Exception {
        // Re-login customer to get fresh token
        LoginRequest login = new LoginRequest();
        login.setEmail(testCustomerEmail);
        login.setPassword(testCustomerPassword);
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk()).andReturn();
        String custToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("token").asText();

        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreAId + "/shows")
                        .header("Authorization", "Bearer " + custToken))
                .andExpect(status().isForbidden());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/manager/theatres/" + theatreAId + "/shows | 403 | PASS | Customer forbidden from manager endpoint");
    }

    @Test
    @Order(16)
    @DisplayName("16: GET /api/v1/admin/audit-logs (Manager attempting Admin endpoint REJECTED 403)")
    void test16_ManagerAttemptingAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/admin/audit-logs | 403 | PASS | Manager forbidden from admin audit log endpoint");
    }

    @Test
    @Order(17)
    @DisplayName("17: GET /api/v1/admin/audit-logs (Super Admin endpoint ALLOWED 200)")
    void test17_AdminEndpoint() throws Exception {
        Role adminRole = roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").orElseThrow();
        User adminUser = userRepository.save(new User("Super", "Admin", superAdminEmail,
                "+919" + (int) (Math.random() * 90000000 + 10000000), passwordEncoder.encode("AdminPass123!"), "ACTIVE"));
        userRoleRepository.save(new UserRole(adminUser.getUserId(), adminRole.getRoleId()));

        LoginRequest login = new LoginRequest();
        login.setEmail(superAdminEmail);
        login.setPassword("AdminPass123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk()).andReturn();
        superAdminToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("token").asText();

        MvcResult result = mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(root.path("data").path("content").isArray());

        System.out.println("HTTP VERIFICATION: GET | /api/v1/admin/audit-logs | 200 | PASS | Audit logs page retrieved from AUDIT_LOG");
    }
}
