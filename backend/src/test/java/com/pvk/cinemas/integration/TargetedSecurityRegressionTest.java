package com.pvk.cinemas.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.infrastructure.dto.ScreenRequest;
import com.pvk.cinemas.infrastructure.dto.SeatRequest;
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
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.search.dto.SearchCardResponse;
import com.pvk.cinemas.search.service.SearchOrchestrationService;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.dto.UpdateUserStatusRequest;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TargetedSecurityRegressionTest {

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
    @Autowired private PresentationFormatRepository presentationFormatRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private LanguageRepository languageRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SearchOrchestrationService searchOrchestrationService;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatTypeRepository seatTypeRepository;

    private User managerA;
    private User superAdmin;
    private User normalUser;
    private String managerAToken;
    private String superAdminToken;

    private Theatre theatreA;
    private Theatre theatreB;
    private Screen screenA;
    private Screen screenA2;
    private Screen screenB;
    private Seat seatA1;
    private Seat seatA2;
    private Seat seatB;
    private Show showA;
    private Show showB;

    @BeforeEach
    void setUp() throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        Role managerRole = roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").orElseThrow();
        Role superAdminRole = roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").orElseThrow();
        Role customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER").orElseThrow();

        // 1. Create Theatre A and Theatre B
        theatreA = new Theatre();
        theatreA.setCityId(city.getCityId());
        theatreA.setTheatreCode("TH-A-" + uid);
        theatreA.setTheatreName("PVR Grand " + uid);
        theatreA.setAddressLine1("100 Alpha Road");
        theatreA.setStatus("ACTIVE");
        theatreA = theatreRepository.save(theatreA);

        theatreB = new Theatre();
        theatreB.setCityId(city.getCityId());
        theatreB.setTheatreCode("TH-B-" + uid);
        theatreB.setTheatreName("INOX Cinema " + uid);
        theatreB.setAddressLine1("200 Beta Road");
        theatreB.setStatus("ACTIVE");
        theatreB = theatreRepository.save(theatreB);

        // 2. Create Screens
        screenA = new Screen();
        screenA.setTheatreId(theatreA.getTheatreId());
        screenA.setScreenCode("SCR-A-" + uid);
        screenA.setScreenName("Screen 1 - Atmos Alpha");
        screenA.setStatus("ACTIVE");
        screenA = screenRepository.save(screenA);

        screenB = new Screen();
        screenB.setTheatreId(theatreB.getTheatreId());
        screenB.setScreenCode("SCR-B-" + uid);
        screenB.setScreenName("Screen 1 - IMAX Beta");
        screenB.setStatus("ACTIVE");
        screenB = screenRepository.save(screenB);

        screenA2 = new Screen();
        screenA2.setTheatreId(theatreA.getTheatreId());
        screenA2.setScreenCode("SCR-A2-" + uid);
        screenA2.setScreenName("Screen 2 - Atmos Alpha");
        screenA2.setStatus("ACTIVE");
        screenA2 = screenRepository.save(screenA2);

        SeatType st = seatTypeRepository.findAll().get(0);
        seatA1 = new Seat(screenA.getScreenId(), st.getSeatTypeId(), "A", "1");
        seatA1.setStatus("ACTIVE");
        seatA1 = seatRepository.save(seatA1);

        seatA2 = new Seat(screenA2.getScreenId(), st.getSeatTypeId(), "A", "2");
        seatA2.setStatus("ACTIVE");
        seatA2 = seatRepository.save(seatA2);

        seatB = new Seat(screenB.getScreenId(), st.getSeatTypeId(), "B", "1");
        seatB.setStatus("ACTIVE");
        seatB = seatRepository.save(seatB);

        // 3. Capabilities
        PresentationFormat pf = presentationFormatRepository.findAll().get(0);
        ScreenCapability capA = new ScreenCapability(screenA.getScreenId(), pf.getPresentationFormatId(), 1L);
        capA = screenCapabilityRepository.save(capA);

        ScreenCapability capB = new ScreenCapability(screenB.getScreenId(), pf.getPresentationFormatId(), 1L);
        capB = screenCapabilityRepository.save(capB);

        // 4. Movie & Language
        Movie movie = movieRepository.findAll().get(0);
        Language lang = languageRepository.findAll().get(0);
        MovieLanguage ml = movieLanguageRepository.findByMovieId(movie.getMovieId()).stream()
                .findFirst()
                .orElseGet(() -> movieLanguageRepository.save(new MovieLanguage(movie.getMovieId(), lang.getLanguageId(), "ORIGINAL")));

        // 5. Shows
        Instant now = Instant.now().plus(2, ChronoUnit.HOURS);
        showA = new Show();
        showA.setScreenCapabilityId(capA.getScreenCapabilityId());
        showA.setMovieLanguageId(ml.getMovieLanguageId());
        showA.setStartAt(now);
        showA.setEndAt(now.plus(120, ChronoUnit.MINUTES));
        showA.setShowStatus("SCHEDULED");
        showA = showRepository.save(showA);

        Instant nowB = Instant.now().plus(5, ChronoUnit.HOURS);
        showB = new Show();
        showB.setScreenCapabilityId(capB.getScreenCapabilityId());
        showB.setMovieLanguageId(ml.getMovieLanguageId());
        showB.setStartAt(nowB);
        showB.setEndAt(nowB.plus(120, ChronoUnit.MINUTES));
        showB.setShowStatus("SCHEDULED");
        showB = showRepository.save(showB);

        // 6. Users: Manager A assigned ONLY to Theatre A
        managerA = userRepository.save(new User("Manager", "Alpha", "mgr_sec_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), passwordEncoder.encode("Pass123!"), "ACTIVE"));
        employeeProfileRepository.save(new EmployeeProfile(managerA.getUserId(), "EMP-" + uid));
        userRoleRepository.save(new UserRole(managerA.getUserId(), managerRole.getRoleId()));
        employeeTheatreRepository.save(new EmployeeTheatre(managerA.getUserId(), theatreA.getTheatreId()));

        superAdmin = userRepository.save(new User("Super", "Admin", "admin_sec_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), passwordEncoder.encode("Pass123!"), "ACTIVE"));
        userRoleRepository.save(new UserRole(superAdmin.getUserId(), superAdminRole.getRoleId()));

        normalUser = userRepository.save(new User("Normal", "User", "user_sec_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), passwordEncoder.encode("Pass123!"), "ACTIVE"));
        userRoleRepository.save(new UserRole(normalUser.getUserId(), customerRole.getRoleId()));

        // Login to get tokens
        managerAToken = loginAndGetToken(managerA.getEmail(), "Pass123!");
        superAdminToken = loginAndGetToken(superAdmin.getEmail(), "Pass123!");
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
    // P0: MANAGER SHOW UPDATE BOLA / IDOR TESTS
    // =========================================================================

    @Test
    @DisplayName("P0-SHOW-1: Manager updates own-theatre show -> SUCCESS 200")
    void testManagerUpdateOwnShowSuccess() throws Exception {
        ShowRequest updateReq = new ShowRequest();
        updateReq.setShowStatus("CANCELLED");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/shows/" + showA.getShowId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        Show updated = showRepository.findById(showA.getShowId()).orElseThrow();
        assertEquals("CANCELLED", updated.getShowStatus());
    }

    @Test
    @DisplayName("P0-SHOW-2: Manager updates another theatre's show with target theatreId -> FORBIDDEN 403")
    void testManagerUpdateOtherTheatreShowForbidden() throws Exception {
        ShowRequest updateReq = new ShowRequest();
        updateReq.setShowStatus("CANCELLED");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/shows/" + showB.getShowId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        Show unchanged = showRepository.findById(showB.getShowId()).orElseThrow();
        assertEquals("SCHEDULED", unchanged.getShowStatus(), "DB status must not change on rejected request");
    }

    @Test
    @DisplayName("P0-SHOW-3: Manager ID tampering (own theatreId in URL, showId of other theatre) -> FORBIDDEN 403")
    void testManagerShowIdTamperingForbidden() throws Exception {
        ShowRequest updateReq = new ShowRequest();
        updateReq.setShowStatus("CANCELLED");

        // Manager A passes theatreAId (which they own), but showBId (which belongs to theatre B)
        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/shows/" + showB.getShowId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        Show unchanged = showRepository.findById(showB.getShowId()).orElseThrow();
        assertEquals("SCHEDULED", unchanged.getShowStatus(), "Show B must not be modified by Manager A");
    }

    @Test
    @DisplayName("P0-SHOW-4: Super Admin can update show platform-wide -> SUCCESS 200")
    void testAdminCanUpdateShowPlatformWide() throws Exception {
        ShowRequest updateReq = new ShowRequest();
        updateReq.setShowStatus("CANCELLED");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/shows/" + showB.getShowId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        Show updated = showRepository.findById(showB.getShowId()).orElseThrow();
        assertEquals("CANCELLED", updated.getShowStatus());
    }

    // =========================================================================
    // P0: MANAGER SCREEN UPDATE BOLA / IDOR TESTS
    // =========================================================================

    @Test
    @DisplayName("P0-SCREEN-1: Manager updates own-theatre screen -> SUCCESS 200")
    void testManagerUpdateOwnScreenSuccess() throws Exception {
        ScreenRequest updateReq = new ScreenRequest();
        updateReq.setScreenName("Screen 1 - Renamed Alpha");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens/" + screenA.getScreenId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        Screen updated = screenRepository.findById(screenA.getScreenId()).orElseThrow();
        assertEquals("Screen 1 - Renamed Alpha", updated.getScreenName());
    }

    @Test
    @DisplayName("P0-SCREEN-2: Manager updates another theatre's screen -> FORBIDDEN 403")
    void testManagerUpdateOtherTheatreScreenForbidden() throws Exception {
        ScreenRequest updateReq = new ScreenRequest();
        updateReq.setScreenName("Screen Tampered");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens/" + screenB.getScreenId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        Screen unchanged = screenRepository.findById(screenB.getScreenId()).orElseThrow();
        assertEquals("Screen 1 - IMAX Beta", unchanged.getScreenName(), "DB row must remain unchanged after rejected request");
    }

    @Test
    @DisplayName("P0-SCREEN-3: Manager screen ID tampering (own theatreId, other screenId) -> FORBIDDEN 403")
    void testManagerScreenIdTamperingForbidden() throws Exception {
        ScreenRequest updateReq = new ScreenRequest();
        updateReq.setScreenName("Screen Tampered");

        // Manager A passes theatreAId, but screenBId
        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens/" + screenB.getScreenId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        Screen unchanged = screenRepository.findById(screenB.getScreenId()).orElseThrow();
        assertEquals("Screen 1 - IMAX Beta", unchanged.getScreenName(), "Screen B must not be modified by Manager A");
    }

    @Test
    @DisplayName("P0-SCREEN-4: Super Admin can update screen platform-wide -> SUCCESS 200")
    void testAdminCanUpdateScreenPlatformWide() throws Exception {
        ScreenRequest updateReq = new ScreenRequest();
        updateReq.setScreenName("Screen 1 - Admin Updated");

        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens/" + screenB.getScreenId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        Screen updated = screenRepository.findById(screenB.getScreenId()).orElseThrow();
        assertEquals("Screen 1 - Admin Updated", updated.getScreenName());
    }

    // =========================================================================
    // P1: ADMIN SELF-SUSPENSION TESTS
    // =========================================================================

    @Test
    @DisplayName("P1-ADMIN-1: Admin suspends another user -> SUCCESS 200")
    void testAdminCanSuspendOtherUser() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setAccountStatus("SUSPENDED");

        mockMvc.perform(patch("/api/v1/admin/users/" + normalUser.getUserId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        User updated = userRepository.findById(normalUser.getUserId()).orElseThrow();
        assertEquals("SUSPENDED", updated.getAccountStatus());
    }

    @Test
    @DisplayName("P1-ADMIN-2: Admin attempts to suspend self -> BAD REQUEST 400 (rejected)")
    void testAdminCannotSuspendSelf() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setAccountStatus("SUSPENDED");

        mockMvc.perform(patch("/api/v1/admin/users/" + superAdmin.getUserId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        User unchanged = userRepository.findById(superAdmin.getUserId()).orElseThrow();
        assertEquals("ACTIVE", unchanged.getAccountStatus(), "Self-suspension attempt must not alter database status");
    }

    // =========================================================================
    // P1: SQL SEARCH FALLBACK (MOVIES & THEATRES)
    // =========================================================================

    @Test
    @DisplayName("P1-SQL-1: SQL fallback returns movie results and theatre results")
    void testSqlFallbackReturnsMoviesAndTheatres() {
        // 1. Direct invocation of searchViaSqlFallback with theatre query
        List<SearchCardResponse> theatreResults = searchOrchestrationService.searchViaSqlFallback("PVR Grand");
        assertNotNull(theatreResults);
        boolean hasTheatre = theatreResults.stream()
                .anyMatch(r -> "THEATRE".equals(r.getEntityType()) && r.getTitle().contains("PVR Grand"));
        assertTrue(hasTheatre, "Fallback must return theatre results for theatre queries");

        // 2. Direct invocation of searchViaSqlFallback with movie query
        Movie sampleMovie = movieRepository.findAll().get(0);
        List<SearchCardResponse> movieResults = searchOrchestrationService.searchViaSqlFallback(sampleMovie.getTitle());
        assertNotNull(movieResults);
        boolean hasMovie = movieResults.stream()
                .anyMatch(r -> "MOVIE".equals(r.getEntityType()) && r.getTitle().equals(sampleMovie.getTitle()));
        assertTrue(hasMovie, "Fallback must return movie results for movie queries");

        // 3. Fallback with empty/all query should return both movies and theatres
        List<SearchCardResponse> allResults = searchOrchestrationService.searchViaSqlFallback("");
        assertNotNull(allResults);
        boolean containsMovie = allResults.stream().anyMatch(r -> "MOVIE".equals(r.getEntityType()));
        boolean containsTheatre = allResults.stream().anyMatch(r -> "THEATRE".equals(r.getEntityType()));
        assertTrue(containsMovie, "Search fallback must include movies");
        assertTrue(containsTheatre, "Search fallback must include theatres");

        // 4. Browsing mode search("") delegates to SQL fallback
        List<SearchCardResponse> browseResults = searchOrchestrationService.search("", null, null);
        assertNotNull(browseResults);
        assertTrue(browseResults.stream().anyMatch(r -> "MOVIE".equals(r.getEntityType())), "Browse must return movies");
        assertTrue(browseResults.stream().anyMatch(r -> "THEATRE".equals(r.getEntityType())), "Browse must return theatres");
    }

    // =========================================================================
    // P1: SEAT UPDATE BOLA / IDOR HORIZONTAL AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("P1-SEAT-1: Manager + own screen + own seat -> SUCCESS 200")
    void testManagerOwnScreenOwnSeatAllowed() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatA1.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        Seat updated = seatRepository.findById(seatA1.getSeatId()).orElseThrow();
        assertFalse(updated.getIsActive(), "Seat status should be updated by authorized manager");
    }

    @Test
    @DisplayName("P1-SEAT-2: Manager + authorized screen + seat belonging to another screen -> FORBIDDEN 403")
    void testManagerAuthorizedScreenForeignSeatForbidden() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatA2.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P1-SEAT-3: Manager + another theatre's screen + another theatre's seat -> FORBIDDEN 403")
    void testManagerForeignScreenForeignSeatForbidden() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenB.getScreenId() + "/seats/" + seatB.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P1-SEAT-4: Manager + own screen + another theatre's seat -> FORBIDDEN 403")
    void testManagerOwnScreenForeignTheatreSeatForbidden() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatB.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P1-SEAT-5: Admin + valid screen/seat combination -> SUCCESS 200")
    void testAdminValidScreenSeatAllowed() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setIsActive(false);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatA1.getSeatId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        Seat updated = seatRepository.findById(seatA1.getSeatId()).orElseThrow();
        assertFalse(updated.getIsActive(), "Seat status should be updated by super admin");
    }

    @Test
    @DisplayName("P1-SEAT-6: Rejected cross-screen mutation does NOT modify database")
    void testRejectedCrossScreenMutationLeavesDatabaseUnchanged() throws Exception {
        Seat before = seatRepository.findById(seatA2.getSeatId()).orElseThrow();
        Boolean initialStatus = before.getIsActive();
        String initialRow = before.getRowLabel();
        String initialNumber = before.getSeatNumber();

        SeatRequest req = new SeatRequest();
        req.setIsActive(false);
        req.setGridRowIndex(99);
        req.setGridColIndex(99);

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatA2.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Seat after = seatRepository.findById(seatA2.getSeatId()).orElseThrow();
        assertEquals(initialStatus, after.getIsActive(), "Seat isActive must remain unchanged after rejected attack");
        assertEquals(initialRow, after.getRowLabel(), "Seat rowLabel must remain unchanged after rejected attack");
        assertEquals(initialNumber, after.getSeatNumber(), "Seat seatNumber must remain unchanged after rejected attack");
        assertNotEquals(99, after.getGridRowIndex(), "Seat grid coordinates must not be mutated");
    }
}
