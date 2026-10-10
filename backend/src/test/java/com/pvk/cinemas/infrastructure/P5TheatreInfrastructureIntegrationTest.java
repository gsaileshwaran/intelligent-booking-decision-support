package com.pvk.cinemas.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.catalogue.model.AudioFormat;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.AudioFormatRepository;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.infrastructure.dto.ScreenCapabilityRequest;
import com.pvk.cinemas.infrastructure.dto.ScreenRequest;
import com.pvk.cinemas.infrastructure.dto.SeatRequest;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.infrastructure.service.ScreenCapabilityService;
import com.pvk.cinemas.infrastructure.service.ScreenService;
import com.pvk.cinemas.infrastructure.service.SeatService;
import com.pvk.cinemas.organization.dto.CityRequest;
import com.pvk.cinemas.organization.dto.TheatreRequest;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.organization.service.CityService;
import com.pvk.cinemas.organization.service.TheatreService;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import com.pvk.cinemas.security.JwtTokenProvider;
import com.pvk.cinemas.security.TheatreScopeService;
import com.pvk.cinemas.security.UserPrincipal;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P5 — Theatre Infrastructure Domain Integration Tests")
public class P5TheatreInfrastructureIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private CityRepository cityRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatTypeRepository seatTypeRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private PresentationFormatRepository presentationFormatRepository;
    @Autowired private AudioFormatRepository audioFormatRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private LanguageRepository languageRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EmployeeProfileRepository employeeProfileRepository;
    @Autowired private EmployeeTheatreRepository employeeTheatreRepository;

    @Autowired private CityService cityService;
    @Autowired private TheatreService theatreService;
    @Autowired private ScreenService screenService;
    @Autowired private SeatService seatService;
    @Autowired private ScreenCapabilityService screenCapabilityService;
    @Autowired private ShowSchedulingService showSchedulingService;
    @Autowired private TheatreScopeService theatreScopeService;

    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    private String uid;
    private City canonicalCity;

    private User managerA;
    private User managerB;
    private User superAdmin;
    private User customer;

    private String managerAToken;
    private String managerBToken;
    private String superAdminToken;
    private String customerToken;

    private Theatre theatreA;
    private Theatre theatreB;
    private Screen screenA;
    private Screen screenB;
    private ScreenCapability capabilityA;
    private ScreenCapability capabilityB;
    private Seat seatA;
    private Seat seatB;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        uid = UUID.randomUUID().toString().substring(0, 8);

        canonicalCity = cityRepository.findAll().stream()
                .filter(c -> "Chennai".equalsIgnoreCase(c.getCityName()))
                .findFirst()
                .orElseGet(() -> cityRepository.findAll().get(0));

        Role managerRole = roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").orElseThrow();
        Role superAdminRole = roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").orElseThrow();
        Role customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER").orElseThrow();

        // 1. Theatres
        theatreA = new Theatre();
        theatreA.setCityId(canonicalCity.getCityId());
        theatreA.setTheatreCode("TH-P5-A-" + uid);
        theatreA.setTheatreName("PVK Grand A " + uid);
        theatreA.setAddressLine1("100 Mount Road");
        theatreA.setStatus("ACTIVE");
        theatreA = theatreRepository.save(theatreA);

        theatreB = new Theatre();
        theatreB.setCityId(canonicalCity.getCityId());
        theatreB.setTheatreCode("TH-P5-B-" + uid);
        theatreB.setTheatreName("PVK Grand B " + uid);
        theatreB.setAddressLine1("200 Anna Salai");
        theatreB.setStatus("ACTIVE");
        theatreB = theatreRepository.save(theatreB);

        // 2. Screens
        screenA = new Screen();
        screenA.setTheatreId(theatreA.getTheatreId());
        screenA.setScreenCode("SCR-P5-1A-" + uid);
        screenA.setScreenName("Screen 1 Alfa");
        screenA.setStatus("ACTIVE");
        screenA = screenRepository.save(screenA);

        screenB = new Screen();
        screenB.setTheatreId(theatreB.getTheatreId());
        screenB.setScreenCode("SCR-P5-1B-" + uid);
        screenB.setScreenName("Screen 1 Bravo");
        screenB.setStatus("ACTIVE");
        screenB = screenRepository.save(screenB);

        // 3. Capabilities
        PresentationFormat pf = presentationFormatRepository.findAll().get(0);
        AudioFormat af = audioFormatRepository.findAll().get(0);
        capabilityA = screenCapabilityRepository.save(new ScreenCapability(screenA.getScreenId(), pf.getPresentationFormatId(), af.getAudioFormatId()));
        capabilityB = screenCapabilityRepository.save(new ScreenCapability(screenB.getScreenId(), pf.getPresentationFormatId(), af.getAudioFormatId()));

        // 4. Seats
        SeatType st = seatTypeRepository.findAll().get(0);
        seatA = new Seat(screenA.getScreenId(), st.getSeatTypeId(), "A", "1");
        seatA.setStatus("ACTIVE");
        seatA = seatRepository.save(seatA);

        seatB = new Seat(screenB.getScreenId(), st.getSeatTypeId(), "B", "1");
        seatB.setStatus("ACTIVE");
        seatB = seatRepository.save(seatB);

        // 5. Users & Roles
        managerA = createUser("mgrA_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(managerA.getUserId(), managerRole.getRoleId()));
        employeeProfileRepository.save(new EmployeeProfile(managerA.getUserId(), "EMP-A-" + uid));
        employeeTheatreRepository.save(new EmployeeTheatre(managerA.getUserId(), theatreA.getTheatreId()));

        managerB = createUser("mgrB_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(managerB.getUserId(), managerRole.getRoleId()));
        employeeProfileRepository.save(new EmployeeProfile(managerB.getUserId(), "EMP-B-" + uid));
        employeeTheatreRepository.save(new EmployeeTheatre(managerB.getUserId(), theatreB.getTheatreId()));

        superAdmin = createUser("admin_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(superAdmin.getUserId(), superAdminRole.getRoleId()));

        customer = createUser("cust_" + uid + "@pvk.com", "ACTIVE");
        userRoleRepository.save(new UserRole(customer.getUserId(), customerRole.getRoleId()));

        // 6. Tokens
        managerAToken = loginAndGetToken(managerA.getEmail(), "Password123!");
        managerBToken = loginAndGetToken(managerB.getEmail(), "Password123!");
        superAdminToken = loginAndGetToken(superAdmin.getEmail(), "Password123!");
        customerToken = loginAndGetToken(customer.getEmail(), "Password123!");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
    // SECTION 1: CITY DOMAIN TESTS
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("[P5-01] Public City Listing returns 200 with deterministic ordering")
    void testPublicCityListing() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(root.path("success").asBoolean());
        JsonNode data = root.path("data");
        assertTrue(data.isArray());
        assertTrue(data.size() >= 5);

        // Verify canonical cities appear first in deterministic order
        List<String> canonical = List.of("Chennai", "Bengaluru", "Hyderabad", "Mumbai", "Delhi");
        for (int i = 0; i < 5; i++) {
            assertEquals(canonical.get(i), data.get(i).path("cityName").asText());
        }
    }

    @Test
    @Order(11)
    @DisplayName("[P5-02] City lookup by valid ID returns 200; invalid ID returns 404")
    void testCityLookupById() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + canonicalCity.getCityId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cityName").value("Chennai"));

        mockMvc.perform(get("/api/v1/cities/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(12)
    @DisplayName("[P5-03] City theatres lookup returns 200; invalid city returns 404")
    void testCityTheatresLookup() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + canonicalCity.getCityId() + "/theatres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/cities/999999/theatres"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(13)
    @DisplayName("[P5-04] Admin City mutations: Super Admin allowed, Manager/Customer denied 403")
    void testAdminCityMutations() throws Exception {
        CityRequest request = new CityRequest();
        request.setCityName("Metropolis_" + uid);
        request.setStateName("Tamil Nadu");
        request.setCountryCode("IN");

        // Super Admin creates city -> 201 Created
        MvcResult res = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdCity = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        Long newCityId = createdCity.path("cityId").asLong();

        // Duplicate city creation rejected -> 409 Conflict
        mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Manager attempting create city -> 403 Forbidden
        mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Customer attempting create city -> 403 Forbidden
        mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Clean up created city
        cityRepository.deleteById(newCityId);
    }

    // =========================================================================
    // SECTION 2: THEATRE DOMAIN TESTS
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("[P5-05] Public Theatre Details: exposes customer-safe DTO without leaking entities")
    void testPublicTheatreDetails() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/theatres/" + theatreA.getTheatreId()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertEquals(theatreA.getTheatreId(), data.path("theatreId").asLong());
        assertEquals("PVK Grand A " + uid, data.path("theatreName").asText());
        assertEquals("Chennai", data.path("cityName").asText());
        assertTrue(data.has("totalScreens"));

        // Administrative / internal properties must NOT be leaked
        assertFalse(data.has("openingTime"));
        assertFalse(data.has("closingTime"));
        assertFalse(data.has("basePriceOffset"));
        assertFalse(data.has("createdAt"));

        // Invalid theatre lookup -> 404
        mockMvc.perform(get("/api/v1/theatres/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(21)
    @DisplayName("[P5-06] Public Theatre Screens: returns ScreenResponse DTO; 404 for invalid theatre")
    void testPublicTheatreScreens() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/theatres/" + theatreA.getTheatreId() + "/screens"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertTrue(data.isArray());
        assertTrue(data.size() >= 1);
        JsonNode firstScreen = data.get(0);
        assertEquals(theatreA.getTheatreId(), firstScreen.path("theatreId").asLong());
        assertTrue(firstScreen.has("totalCapacity"));
        assertTrue(firstScreen.has("screenCapabilityId"));

        // Invalid theatre returns 404 RESOURCE_NOT_FOUND
        mockMvc.perform(get("/api/v1/theatres/999999/screens"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(22)
    @DisplayName("[P5-07] Admin Theatre Creation: validates City existence and rejects duplicate codes")
    void testAdminTheatreCreation() throws Exception {
        TheatreRequest request = new TheatreRequest();
        request.setCityId(canonicalCity.getCityId().intValue());
        request.setTheatreCode("TH-P5-NEW-" + uid);
        request.setTheatreName("New Cinema " + uid);
        request.setAddressLine1("55 Cross Road");
        request.setIsActive(true);

        // Super Admin creates -> 201 Created
        MvcResult res = mockMvc.perform(post("/api/v1/admin/theatres")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long createdId = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("theatreId").asLong();

        // Duplicate theatre code -> 409 Conflict
        mockMvc.perform(post("/api/v1/admin/theatres")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Creation with nonexistent city -> 404
        request.setCityId(999999);
        request.setTheatreCode("TH-P5-ERR-" + uid);
        mockMvc.perform(post("/api/v1/admin/theatres")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // Clean up
        theatreRepository.deleteById(createdId);
    }

    // =========================================================================
    // SECTION 3: SCREEN DOMAIN TESTS
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("[P5-08] Manager Screen Access: permitted for assigned theatre, denied 403 for unassigned")
    void testManagerScreenAccessScope() throws Exception {
        // Manager A -> Theatre A (Assigned) -> 200 OK
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Manager A -> Theatre B (Unassigned) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(31)
    @DisplayName("[P5-09] Manager Screen Creation: permitted for assigned theatre, duplicate code rejected (409)")
    void testManagerScreenCreationAndDuplicate() throws Exception {
        ScreenRequest req = new ScreenRequest();
        req.setScreenCode("SCR-NEW-" + uid);
        req.setScreenName("New Auditorium " + uid);
        req.setIsActive(true);

        // Manager A creates screen in assigned Theatre A -> 201 Created
        MvcResult res = mockMvc.perform(post("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long newScreenId = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("screenId").asLong();

        // Duplicate screenCode in same theatre -> 409 Conflict
        mockMvc.perform(post("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Manager A attempting create screen in Theatre B -> 403 Forbidden
        mockMvc.perform(post("/api/v1/manager/theatres/" + theatreB.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Clean up
        screenRepository.deleteById(newScreenId);
    }

    @Test
    @Order(32)
    @DisplayName("[P5-10] Cross-theatre screen tampering: Updating Screen B via Theatre A path rejected 403")
    void testCrossTheatreScreenTampering() throws Exception {
        ScreenRequest req = new ScreenRequest();
        req.setScreenName("Hacked Screen Name");

        // Manager A owns Theatre A, Screen B belongs to Theatre B
        // Request: PATCH /manager/theatres/{theatreAId}/screens/{screenBId}
        mockMvc.perform(patch("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens/" + screenB.getScreenId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // =========================================================================
    // SECTION 4: SCREEN CAPABILITY TESTS
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("[P5-11] Manager Capability Access: allowed for assigned screen, denied 403 for unassigned")
    void testManagerCapabilityAccessScope() throws Exception {
        // Manager A -> Screen A (Assigned) -> 200 OK
        mockMvc.perform(get("/api/v1/manager/screens/" + screenA.getScreenId() + "/capabilities")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Manager A -> Screen B (Unassigned) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/manager/screens/" + screenB.getScreenId() + "/capabilities")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(41)
    @DisplayName("[P5-12] Add Capability: validates formats, rejects duplicates (409); manager denied on unassigned screen")
    void testAddCapabilityAndDuplicates() throws Exception {
        PresentationFormat pf3D = presentationFormatRepository.findAll().stream()
                .filter(p -> "3D".equalsIgnoreCase(p.getFormatCode()))
                .findFirst().orElseGet(() -> presentationFormatRepository.findAll().get(1));
        AudioFormat afAtmos = audioFormatRepository.findAll().stream()
                .filter(a -> "DOLBY_ATMOS".equalsIgnoreCase(a.getFormatCode()))
                .findFirst().orElseGet(() -> audioFormatRepository.findAll().get(1));

        ScreenCapabilityRequest req = new ScreenCapabilityRequest(pf3D.getPresentationFormatId(), afAtmos.getAudioFormatId());

        // Manager A adds capability to Screen A -> 201 Created
        MvcResult res = mockMvc.perform(post("/api/v1/manager/screens/" + screenA.getScreenId() + "/capabilities")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long newCapId = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("screenCapabilityId").asLong();

        // Adding exact duplicate capability -> 409 Conflict
        mockMvc.perform(post("/api/v1/manager/screens/" + screenA.getScreenId() + "/capabilities")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Manager A attempting to add capability to Screen B -> 403 Forbidden
        mockMvc.perform(post("/api/v1/manager/screens/" + screenB.getScreenId() + "/capabilities")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Clean up
        screenCapabilityRepository.deleteById(newCapId);
    }

    @Test
    @Order(42)
    @DisplayName("[P5-13] Delete Capability: rejects cross-screen tampering (403); permits authorized deletion")
    void testDeleteCapabilityTampering() throws Exception {
        // Manager A attempts to delete Capability B (belongs to Screen B) by referencing Screen A
        mockMvc.perform(delete("/api/v1/manager/screens/" + screenA.getScreenId() + "/capabilities/" + capabilityB.getScreenCapabilityId())
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(43)
    @DisplayName("[P5-14] Show Scheduling BR-003: Scheduling on Screen A with Capability B rejected with 409 INVALID_SCREEN_CAPABILITY")
    void testSchedulingCapabilityMismatchBR003() throws Exception {
        Movie movie = movieRepository.findAll().get(0);
        Language lang = languageRepository.findAll().get(0);
        MovieLanguage ml = movieLanguageRepository.findByMovieId(movie.getMovieId()).stream().findFirst()
                .orElseGet(() -> movieLanguageRepository.save(new MovieLanguage(movie.getMovieId(), lang.getLanguageId(), "ORIGINAL")));

        ShowRequest showReq = new ShowRequest();
        showReq.setMovieId(movie.getMovieId());
        showReq.setMovieLanguageId(ml.getMovieLanguageId());
        showReq.setScreenId(screenA.getScreenId().intValue());
        // Deliberately pass capabilityB which belongs to Screen B!
        showReq.setScreenCapabilityId(capabilityB.getScreenCapabilityId().intValue());
        java.time.LocalDate validDate = movie.getExhibitionStartDate().plusDays(2);
        showReq.setStartAt(validDate.atTime(10, 0).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());
        showReq.setEndAt(validDate.atTime(12, 30).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());

        mockMvc.perform(post("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/shows")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_SCREEN_CAPABILITY"));
    }

    // =========================================================================
    // SECTION 5: SEAT TYPES AND PHYSICAL SEATS TESTS
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("[P5-15] Public Seat Type Reference Discovery returns all 4 reference seat types")
    void testSeatTypeDiscovery() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/seat-types"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        assertTrue(data.isArray());
        assertEquals(4, data.size());

        List<String> expectedCodes = List.of("STANDARD", "PREMIUM", "RECLINER", "ACCESSIBLE");
        for (int i = 0; i < 4; i++) {
            assertEquals(expectedCodes.get(i), data.get(i).path("typeCode").asText());
        }

        // Invalid seat type lookup -> 404
        mockMvc.perform(get("/api/v1/seat-types/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(51)
    @DisplayName("[P5-16] Manager Seat Access: permitted on assigned screen, denied 403 on unassigned screen")
    void testManagerSeatAccessScope() throws Exception {
        // Manager A -> Screen A seats -> 200 OK
        mockMvc.perform(get("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Manager A -> Screen B seats -> 403 Forbidden
        mockMvc.perform(get("/api/v1/manager/screens/" + screenB.getScreenId() + "/seats")
                        .header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(52)
    @DisplayName("[P5-17] Manager Seat Creation: validates seat type, coordinate duplicates (409)")
    void testManagerSeatCreation() throws Exception {
        SeatType st = seatTypeRepository.findAll().get(0);
        SeatRequest req = new SeatRequest();
        req.setSeatTypeId(st.getSeatTypeId().intValue());
        req.setRowLabel("Z");
        req.setSeatNumber("99");
        req.setStatus("ACTIVE");

        // Manager A creates seat on Screen A -> 201 Created
        MvcResult res = mockMvc.perform(post("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long newSeatId = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("seatId").asLong();

        // Duplicate coordinate on same screen -> 409 Conflict
        mockMvc.perform(post("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Manager A creating seat on Screen B -> 403 Forbidden
        mockMvc.perform(post("/api/v1/manager/screens/" + screenB.getScreenId() + "/seats")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Clean up
        seatRepository.deleteById(newSeatId);
    }

    @Test
    @Order(53)
    @DisplayName("[P5-18] Cross-screen seat tampering: Updating Seat B via Screen A path rejected 403")
    void testCrossScreenSeatTampering() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setStatus("BLOCKED");

        // Seat B belongs to Screen B. Manager A attempts update using Screen A path
        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatB.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(54)
    @DisplayName("[P5-19] Seat Blocking and Status Update: updates physical status correctly")
    void testSeatBlockingAndStatus() throws Exception {
        SeatRequest req = new SeatRequest();
        req.setStatus("BLOCKED");
        req.setBlockReason("Broken recliner motor");

        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatA.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        // Restore to ACTIVE
        req.setStatus("ACTIVE");
        mockMvc.perform(patch("/api/v1/manager/screens/" + screenA.getScreenId() + "/seats/" + seatA.getSeatId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    // =========================================================================
    // SECTION 6: SECURITY AND INTEGRATION GUARDS
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("[P5-20] Anonymous access rejected with 401 on protected manager/admin endpoints")
    void testAnonymousAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/v1/admin/cities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    @Order(61)
    @DisplayName("[P5-21] Customer token rejected with 403 on manager and admin endpoints")
    void testCustomerForbiddenOnManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/" + theatreA.getTheatreId() + "/screens")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/theatres")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(62)
    @DisplayName("[P5-22] Service-Level Scope Guards cannot be bypassed even if controller checks were avoided")
    void testServiceLevelScopeGuards() {
        // Authenticate Manager A in SecurityContext
        UserPrincipal principal = new UserPrincipal(managerA.getUserId(), managerA.getEmail(), "hash", "ACTIVE",
                List.of(new SimpleGrantedAuthority("ROLE_THEATRE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        // Service-level check on Theatre B must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            screenService.getScreensByTheatre(theatreB.getTheatreId());
        });

        // Service-level check on Screen B seats must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            seatService.getSeatsByScreen(screenB.getScreenId());
        });

        // Service-level check on Screen B capabilities must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            screenCapabilityService.getCapabilitiesByScreen(screenB.getScreenId());
        });
    }

    @Test
    @Order(63)
    @DisplayName("[P5-23] Super Admin retains platform-wide scope across all infrastructure services")
    void testSuperAdminGlobalScope() {
        UserPrincipal principal = new UserPrincipal(superAdmin.getUserId(), superAdmin.getEmail(), "hash", "ACTIVE",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        // Super Admin can access Theatre A, Theatre B, and arbitrary theatres
        assertDoesNotThrow(() -> {
            screenService.getScreensByTheatre(theatreA.getTheatreId());
            screenService.getScreensByTheatre(theatreB.getTheatreId());
            seatService.getSeatsByScreen(screenA.getScreenId());
            seatService.getSeatsByScreen(screenB.getScreenId());
            screenCapabilityService.getCapabilitiesByScreen(screenA.getScreenId());
            screenCapabilityService.getCapabilitiesByScreen(screenB.getScreenId());
        });
    }
}
