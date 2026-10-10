package com.pvk.cinemas.catalogue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.catalogue.dto.MovieRequest;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.User;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Part 2 — Movie API Endpoints, Filtering, RBAC Authorization & Validation Integration Tests")
class MovieApiSecurityAndEndpointsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MovieRepository movieRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String customerToken;
    private String managerToken;
    private String adminToken;
    private Movie sampleMovie;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM show_seat WHERE show_id IN (SELECT show_id FROM `show` WHERE movie_language_id IN (SELECT movie_language_id FROM movie_language WHERE movie_id > 45))");
        jdbcTemplate.execute("DELETE FROM `show` WHERE movie_language_id IN (SELECT movie_language_id FROM movie_language WHERE movie_id > 45)");
        jdbcTemplate.execute("DELETE FROM movie_language WHERE movie_id IN (SELECT movie_id FROM movie WHERE movie_id > 45)");
        jdbcTemplate.execute("DELETE FROM movie_genre WHERE movie_id IN (SELECT movie_id FROM movie WHERE movie_id > 45)");
        jdbcTemplate.execute("DELETE FROM movie WHERE movie_id > 45");
    }

    @BeforeEach
    void setUp() throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        Role superAdminRole = roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").orElseThrow();
        Role managerRole = roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").orElseThrow();
        Role customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER").orElseThrow();

        // 1. Create Customer
        User cust = userRepository.save(new User("Test", "Customer", "p2_cust_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), passwordEncoder.encode("Secret123!"), "ACTIVE"));
        userRoleRepository.save(new UserRole(cust.getUserId(), customerRole.getRoleId()));

        // 2. Create Theatre Manager
        User mgr = userRepository.save(new User("Test", "Manager", "p2_mgr_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), passwordEncoder.encode("Secret123!"), "ACTIVE"));
        userRoleRepository.save(new UserRole(mgr.getUserId(), managerRole.getRoleId()));

        // 3. Create Super Admin
        User adm = userRepository.save(new User("Test", "Admin", "p2_adm_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), passwordEncoder.encode("Secret123!"), "ACTIVE"));
        userRoleRepository.save(new UserRole(adm.getUserId(), superAdminRole.getRoleId()));

        customerToken = loginAndGetToken(cust.getEmail(), "Secret123!");
        managerToken = loginAndGetToken(mgr.getEmail(), "Secret123!");
        adminToken = loginAndGetToken(adm.getEmail(), "Secret123!");

        sampleMovie = movieRepository.findAll().get(0);
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
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        return node.path("data").path("token").asText(node.path("token").asText());
    }

    // =========================================================================
    // SECTION 1: PUBLIC CUSTOMER API
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/movies: publicly accessible, returns 200 with catalogue")
    void testPublicGetMovies() throws Exception {
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[0].demandTier").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].lifecycleStage").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/movies: filtering by language and demandTier")
    void testPublicGetMoviesFiltering() throws Exception {
        // Demand tier filter
        mockMvc.perform(get("/api/v1/movies")
                        .param("demandTier", "BLOCKBUSTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].demandTier", everyItem(is("BLOCKBUSTER"))));

        // Language filter (ta)
        mockMvc.perform(get("/api/v1/movies")
                        .param("language", "ta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].languages", everyItem(hasItem("Tamil"))));
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id}: publicly accessible, returns movie with lifecycle stage")
    void testPublicGetMovieById() throws Exception {
        mockMvc.perform(get("/api/v1/movies/{movieId}", sampleMovie.getMovieId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movieId").value(sampleMovie.getMovieId()))
                .andExpect(jsonPath("$.data.title").value(sampleMovie.getTitle()))
                .andExpect(jsonPath("$.data.demandTier").value(sampleMovie.getDemandTier()))
                .andExpect(jsonPath("$.data.lifecycleStage").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id}/languages: returns language list")
    void testPublicGetMovieLanguages() throws Exception {
        mockMvc.perform(get("/api/v1/movies/{movieId}/languages", sampleMovie.getMovieId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id}/genres: returns genres list")
    void testPublicGetMovieGenres() throws Exception {
        mockMvc.perform(get("/api/v1/movies/{movieId}/genres", sampleMovie.getMovieId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
    }

    // =========================================================================
    // SECTION 2: THEATRE MANAGER RBAC (READ-ONLY)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/manager/movies: 401 without auth, 403 for Customer, 200 for Manager")
    void testManagerMovieCatalogueAccess() throws Exception {
        // Without auth -> 401
        mockMvc.perform(get("/api/v1/manager/movies"))
                .andExpect(status().isUnauthorized());

        // With Customer token -> 403
        mockMvc.perform(get("/api/v1/manager/movies")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // With Manager token -> 200
        mockMvc.perform(get("/api/v1/manager/movies")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // SECTION 3: SUPER ADMIN RBAC & VALIDATION (MUTATION)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/admin/movies: 401 without auth, 403 for Manager/Customer, 201 for Admin")
    void testAdminCreateMovieRBACAndSuccess() throws Exception {
        MovieRequest req = new MovieRequest();
        req.setTitle("RBAC Test Movie " + UUID.randomUUID().toString().substring(0, 5));
        req.setCertificationId(sampleMovie.getCertificationId().intValue());
        req.setRuntimeMinutes(130);
        req.setReleaseDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionEndDate(LocalDate.of(2026, 10, 15));
        req.setDemandTier("MID_TIER");
        req.setPopularityScore(new BigDecimal("7.50"));
        req.setLanguageIds(List.of(1));
        req.setGenreIds(List.of(1));

        String body = objectMapper.writeValueAsString(req);

        // 1. Without auth -> 401
        mockMvc.perform(post("/api/v1/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        // 2. Customer -> 403
        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // 3. Manager -> 403
        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // 4. Admin -> 201 Created
        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.demandTier").value("MID_TIER"))
                .andExpect(jsonPath("$.data.popularityScore").value(7.50));
    }

    @Test
    @DisplayName("POST /api/v1/admin/movies: validation rejects invalid demand tier and negative score")
    void testAdminCreateMovieValidationFailures() throws Exception {
        // Invalid demand tier
        MovieRequest invalidTier = new MovieRequest();
        invalidTier.setTitle("Bad Tier Movie");
        invalidTier.setCertificationId(sampleMovie.getCertificationId().intValue());
        invalidTier.setRuntimeMinutes(120);
        invalidTier.setDemandTier("NON_EXISTENT_TIER");
        invalidTier.setPopularityScore(new BigDecimal("5.00"));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTier)))
                .andExpect(status().isBadRequest());

        // Invalid popularity score (> 10.00)
        MovieRequest invalidScore = new MovieRequest();
        invalidScore.setTitle("Bad Score Movie");
        invalidScore.setCertificationId(sampleMovie.getCertificationId().intValue());
        invalidScore.setRuntimeMinutes(120);
        invalidScore.setDemandTier("MID_TIER");
        invalidScore.setPopularityScore(new BigDecimal("15.00"));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidScore)))
                .andExpect(status().isBadRequest());

        // Invalid date order: end before start
        MovieRequest invalidDates = new MovieRequest();
        invalidDates.setTitle("Bad Dates Movie");
        invalidDates.setCertificationId(sampleMovie.getCertificationId().intValue());
        invalidDates.setRuntimeMinutes(120);
        invalidDates.setExhibitionStartDate(LocalDate.of(2026, 9, 20));
        invalidDates.setExhibitionEndDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDates)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/movies/{id}: updates movie metadata successfully with Admin token")
    void testAdminPatchMovie() throws Exception {
        MovieRequest patchReq = new MovieRequest();
        patchReq.setPopularityScore(new BigDecimal("9.95"));
        patchReq.setDemandTier("BLOCKBUSTER");

        mockMvc.perform(patch("/api/v1/admin/movies/{movieId}", sampleMovie.getMovieId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.popularityScore").value(9.95))
                .andExpect(jsonPath("$.data.demandTier").value("BLOCKBUSTER"));
    }

    // =========================================================================
    // SECTION 4: P6 GAP A — COMPLETE CATALOGUE FILTERING TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/movies?genreId=1: returns only movies with Action genre")
    void testFilterByGenreIdReturnsMatchingMovies() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                        .param("genreId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[*].genres", everyItem(hasItem("Action"))));
    }

    @Test
    @DisplayName("GET /api/v1/movies?certificationId=1: returns only movies with Certification ID 1")
    void testFilterByCertificationIdReturnsMatchingMovies() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                        .param("certificationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[*].certificationId", everyItem(is(1))));
    }

    @Test
    @DisplayName("GET /api/v1/movies?languageId=1: returns only movies with Tamil language (ID 1)")
    void testFilterByLanguageIdReturnsMatchingMovies() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                        .param("languageId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[*].languages", everyItem(hasItem("Tamil"))));
    }

    @Test
    @DisplayName("GET /api/v1/movies: combined cumulative AND filtering across genre, certification, language, demandTier")
    void testFilterCombinedCumulativeAndConditions() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                        .param("genreId", "1")
                        .param("languageId", "1")
                        .param("demandTier", "BLOCKBUSTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[*].genres", everyItem(hasItem("Action"))))
                .andExpect(jsonPath("$.data.content[*].languages", everyItem(hasItem("Tamil"))))
                .andExpect(jsonPath("$.data.content[*].demandTier", everyItem(is("BLOCKBUSTER"))));
    }

    @Test
    @DisplayName("GET /api/v1/movies: pagination correctly slices and total count reflects filtered total")
    void testFilterPaginationAndTotalCountBeforeSlicing() throws Exception {
        // Query all Action movies first to get exact total
        MvcResult allResult = mockMvc.perform(get("/api/v1/movies")
                        .param("genreId", "1")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();
        int totalActionMovies = objectMapper.readTree(allResult.getResponse().getContentAsString())
                .path("data").path("totalElements").asInt();
        org.junit.jupiter.api.Assertions.assertTrue(totalActionMovies > 2);

        // Now request page 0 with size 2
        mockMvc.perform(get("/api/v1/movies")
                        .param("genreId", "1")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(totalActionMovies))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/movies: non-existent filter ID returns 200 with empty list")
    void testFilterNonExistentReferenceIdReturnsEmpty200() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                        .param("genreId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/movies")
                        .param("certificationId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/movies")
                        .param("languageId", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /admin/movies and /manager/movies: new filters work consistently across roles")
    void testFilterConsistentAcrossAdminAndManagerEndpoints() throws Exception {
        // Super Admin accessing admin endpoint with genreId filter
        mockMvc.perform(get("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("genreId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[*].genres", everyItem(hasItem("Action"))));

        // Theatre Manager accessing manager endpoint with genreId filter
        mockMvc.perform(get("/api/v1/manager/movies")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("genreId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.content[*].genres", everyItem(hasItem("Action"))));

        // Customer trying to access manager endpoint with filter -> 403 Forbidden
        mockMvc.perform(get("/api/v1/manager/movies")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("genreId", "1"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 5: P6 GAP B — STRICT GENRE & LANGUAGE RELATIONSHIP VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/admin/movies: non-existent languageId is rejected with 404 and rolls back")
    void testCreateMovieNonExistentLanguageIdFails404AndRollsBack() throws Exception {
        String uniqueTitle = "NonExistentLangMovie_" + UUID.randomUUID().toString().substring(0, 8);
        MovieRequest req = new MovieRequest();
        req.setTitle(uniqueTitle);
        req.setCertificationId(sampleMovie.getCertificationId().intValue());
        req.setRuntimeMinutes(120);
        req.setReleaseDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionEndDate(LocalDate.of(2026, 10, 15));
        req.setDemandTier("MID_TIER");
        req.setPopularityScore(new BigDecimal("7.00"));
        req.setGenreIds(List.of(1));
        req.setLanguageIds(List.of(99999)); // Non-existent language ID

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Language not found: 99999")));

        // Verify database state: no movie was created with that title
        org.junit.jupiter.api.Assertions.assertFalse(movieRepository.findByTitle(uniqueTitle).isPresent(),
                "Movie must not be persisted when language validation fails");
    }

    @Test
    @DisplayName("POST /api/v1/admin/movies: non-existent genreId is rejected with 404 and rolls back")
    void testCreateMovieNonExistentGenreIdFails404AndRollsBack() throws Exception {
        String uniqueTitle = "NonExistentGenreMovie_" + UUID.randomUUID().toString().substring(0, 8);
        MovieRequest req = new MovieRequest();
        req.setTitle(uniqueTitle);
        req.setCertificationId(sampleMovie.getCertificationId().intValue());
        req.setRuntimeMinutes(120);
        req.setReleaseDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionEndDate(LocalDate.of(2026, 10, 15));
        req.setDemandTier("MID_TIER");
        req.setPopularityScore(new BigDecimal("7.00"));
        req.setGenreIds(List.of(99999)); // Non-existent genre ID
        req.setLanguageIds(List.of(1));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Genre not found: 99999")));

        // Verify database state: no movie was created
        org.junit.jupiter.api.Assertions.assertFalse(movieRepository.findByTitle(uniqueTitle).isPresent(),
                "Movie must not be persisted when genre validation fails");
    }

    @Test
    @DisplayName("POST /api/v1/admin/movies: mixture of valid and invalid IDs is rejected and rolls back")
    void testCreateMovieMixedValidAndInvalidRelationshipIdsFails404AndRollsBack() throws Exception {
        String uniqueTitle = "MixedGenreMovie_" + UUID.randomUUID().toString().substring(0, 8);
        MovieRequest req = new MovieRequest();
        req.setTitle(uniqueTitle);
        req.setCertificationId(sampleMovie.getCertificationId().intValue());
        req.setRuntimeMinutes(120);
        req.setReleaseDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionEndDate(LocalDate.of(2026, 10, 15));
        req.setDemandTier("MID_TIER");
        req.setPopularityScore(new BigDecimal("7.00"));
        req.setGenreIds(List.of(1, 99999)); // Mix of valid (1) and invalid (99999)
        req.setLanguageIds(List.of(1));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Genre not found: 99999")));

        org.junit.jupiter.api.Assertions.assertFalse(movieRepository.findByTitle(uniqueTitle).isPresent(),
                "Movie must not be persisted when mixed validation fails");
    }

    @Test
    @DisplayName("POST /api/v1/admin/movies: valid movie creation persists movie, genres, and languages")
    void testCreateMovieValidGenreAndLanguageSucceeds() throws Exception {
        String uniqueTitle = "ValidP6Movie_" + UUID.randomUUID().toString().substring(0, 8);
        MovieRequest req = new MovieRequest();
        req.setTitle(uniqueTitle);
        req.setCertificationId(sampleMovie.getCertificationId().intValue());
        req.setRuntimeMinutes(135);
        req.setReleaseDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        req.setExhibitionEndDate(LocalDate.of(2026, 10, 15));
        req.setDemandTier("MID_TIER");
        req.setPopularityScore(new BigDecimal("8.00"));
        req.setGenreIds(List.of(1, 2)); // Action, Comedy
        req.setLanguageIds(List.of(1, 2)); // Tamil, English

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(uniqueTitle))
                .andExpect(jsonPath("$.data.genres", hasItems("Action", "Comedy")))
                .andExpect(jsonPath("$.data.languages", hasItems("Tamil", "English")));

        // Verify entity persisted in database
        Movie created = movieRepository.findByTitle(uniqueTitle).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(uniqueTitle, created.getTitle());
    }
}
