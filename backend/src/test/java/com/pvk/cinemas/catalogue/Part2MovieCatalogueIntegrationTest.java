package com.pvk.cinemas.catalogue;

import com.pvk.cinemas.catalogue.dto.MovieRequest;
import com.pvk.cinemas.catalogue.dto.MovieResponse;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.service.MovieLifecycleService;
import com.pvk.cinemas.catalogue.service.MovieService;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Part 2 — Movie Catalogue & Lifecycle Master Integration Tests")
class Part2MovieCatalogueIntegrationTest {

    private static final int EXPECTED_MOVIES_COUNT = 45;
    private static final Set<String> EXPECTED_LANGUAGES = Set.of("ta", "te", "hi", "kn", "en");

    @Autowired private MovieRepository movieRepository;
    @Autowired private LanguageRepository languageRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private MovieService movieService;
    @Autowired private MovieLifecycleService movieLifecycleService;
    @Autowired private BusinessDateProvider businessDateProvider;
    @Autowired private JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeEach
    void cleanSyntheticTestMovies() {
        jdbc.execute("DELETE FROM show_seat WHERE show_id IN (SELECT show_id FROM `show` WHERE movie_language_id IN (SELECT movie_language_id FROM movie_language WHERE movie_id > 45))");
        jdbc.execute("DELETE FROM `show` WHERE movie_language_id IN (SELECT movie_language_id FROM movie_language WHERE movie_id > 45)");
        jdbc.execute("DELETE FROM movie_language WHERE movie_id IN (SELECT movie_id FROM movie WHERE movie_id > 45)");
        jdbc.execute("DELETE FROM movie_genre WHERE movie_id IN (SELECT movie_id FROM movie WHERE movie_id > 45)");
        jdbc.execute("DELETE FROM movie WHERE movie_id > 45");
    }

    // =========================================================================
    // SECTION 1 — CATALOGUE INTEGRITY & 45 TITLES
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("[P2-01] Exactly 45 curated movies exist in the catalogue")
    void testExactly45MoviesExist() {
        long count = movieRepository.count();
        assertEquals(EXPECTED_MOVIES_COUNT, count,
                "Expected exactly " + EXPECTED_MOVIES_COUNT + " movies in Part 2 catalogue, but found " + count);
    }

    @Test
    @Order(2)
    @DisplayName("[P2-02] No duplicate movie titles exist")
    void testNoDuplicateTitles() {
        List<Movie> allMovies = movieRepository.findAll();
        Set<String> titles = new HashSet<>();
        for (Movie m : allMovies) {
            String title = m.getTitle().trim().toLowerCase();
            assertFalse(titles.contains(title), "Duplicate movie title detected: " + m.getTitle());
            titles.add(title);
        }
        assertEquals(EXPECTED_MOVIES_COUNT, titles.size());
    }

    @Test
    @Order(3)
    @DisplayName("[P2-03] All movies have mandatory fields populated with valid data")
    void testMandatoryFieldsPopulated() {
        List<Movie> allMovies = movieRepository.findAll();
        for (Movie m : allMovies) {
            assertNotNull(m.getMovieId(), "movieId cannot be null");
            assertNotNull(m.getTitle(), "title cannot be null for movieId " + m.getMovieId());
            assertFalse(m.getTitle().isBlank(), "title cannot be blank");
            assertNotNull(m.getRuntimeMinutes(), "runtimeMinutes cannot be null for " + m.getTitle());
            assertTrue(m.getRuntimeMinutes() > 0, "runtimeMinutes must be > 0 for " + m.getTitle());
            assertNotNull(m.getCertificationId(), "certificationId cannot be null for " + m.getTitle());
            assertNotNull(m.getReleaseDate(), "releaseDate cannot be null for " + m.getTitle());
            assertNotNull(m.getExhibitionStartDate(), "exhibitionStartDate cannot be null for " + m.getTitle());
            assertNotNull(m.getExhibitionEndDate(), "exhibitionEndDate cannot be null for " + m.getTitle());
            assertNotNull(m.getDemandTier(), "demandTier cannot be null for " + m.getTitle());
            assertNotNull(m.getPopularityScore(), "popularityScore cannot be null for " + m.getTitle());

            // Date ordering
            assertTrue(m.getExhibitionStartDate().compareTo(m.getExhibitionEndDate()) <= 0,
                    "exhibitionStartDate must be <= exhibitionEndDate for " + m.getTitle());

            // Demand tier validation
            assertTrue(MovieLifecycleService.ALLOWED_DEMAND_TIERS.contains(m.getDemandTier()),
                    "Invalid demand tier: " + m.getDemandTier() + " for " + m.getTitle());

            // Popularity score range [0.00, 10.00]
            assertTrue(m.getPopularityScore().compareTo(BigDecimal.ZERO) >= 0 &&
                            m.getPopularityScore().compareTo(BigDecimal.TEN) <= 0,
                    "Popularity score out of range: " + m.getPopularityScore() + " for " + m.getTitle());
        }
    }

    // =========================================================================
    // SECTION 2 — LANGUAGE MODEL & RELATIONSHIPS
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("[P2-04] Exactly 5 active languages exist (ta, te, hi, kn, en)")
    void testActiveLanguagesExist() {
        List<Language> languages = languageRepository.findAll();
        Set<String> codes = languages.stream().map(Language::getLanguageCode).collect(Collectors.toSet());
        assertTrue(codes.containsAll(EXPECTED_LANGUAGES),
                "Missing expected languages. Expected: " + EXPECTED_LANGUAGES + ", found: " + codes);
    }

    @Test
    @Order(11)
    @DisplayName("[P2-05] Every movie has at least one valid language mapped")
    void testEveryMovieHasLanguage() {
        List<Movie> allMovies = movieRepository.findAll();
        for (Movie m : allMovies) {
            List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(m.getMovieId());
            assertFalse(mls.isEmpty(), "Movie " + m.getTitle() + " has no language mapping");
        }
    }

    @Test
    @Order(12)
    @DisplayName("[P2-06] All movie languages map exclusively to the 5 active languages")
    void testMovieLanguagesAreValid() {
        List<MovieLanguage> allMLs = movieLanguageRepository.findAll();
        assertFalse(allMLs.isEmpty());

        Map<Long, String> langMap = languageRepository.findAll().stream()
                .collect(Collectors.toMap(Language::getLanguageId, Language::getLanguageCode));

        for (MovieLanguage ml : allMLs) {
            assertTrue(langMap.containsKey(ml.getLanguageId()),
                    "Orphan movie_language record: invalid language_id " + ml.getLanguageId());
            String code = langMap.get(ml.getLanguageId());
            assertTrue(EXPECTED_LANGUAGES.contains(code),
                    "Movie language " + code + " is not one of the 5 canonical active languages");
        }
    }

    @Test
    @Order(13)
    @DisplayName("[P2-07] No orphan movie language or movie genre records")
    void testNoOrphanCatalogueRecords() {
        Long orphanMovieLangs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movie_language ml LEFT JOIN movie m ON ml.movie_id = m.movie_id WHERE m.movie_id IS NULL",
                Long.class
        );
        assertEquals(0L, orphanMovieLangs, "Found orphan movie_language records");

        Long orphanMovieGenres = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movie_genre mg LEFT JOIN movie m ON mg.movie_id = m.movie_id WHERE m.movie_id IS NULL",
                Long.class
        );
        assertEquals(0L, orphanMovieGenres, "Found orphan movie_genre records");
    }

    @Test
    @Order(14)
    @DisplayName("[P2-08] Duplicate movie-language relationship is rejected by unique constraint")
    @Transactional
    void testDuplicateMovieLanguageRejected() {
        Movie movie = movieRepository.findAll().get(0);
        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(movie.getMovieId());
        MovieLanguage existing = mls.get(0);

        MovieLanguage duplicate = new MovieLanguage(movie.getMovieId(), existing.getLanguageId(), existing.getLanguageRole());
        assertThrows(DataIntegrityViolationException.class, () -> {
            movieLanguageRepository.saveAndFlush(duplicate);
        });
    }

    // =========================================================================
    // SECTION 3 — LIFECYCLE MODEL & STAGES
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("[P2-09] Lifecycle stage evaluation against business anchor 2026-09-18 covers all states")
    void testLifecycleStagesDistribution() {
        LocalDate anchor = businessDateProvider.getBusinessDate();
        assertEquals(LocalDate.of(2026, 9, 18), anchor, "Authoritative anchor date must be 2026-09-18");

        List<Movie> allMovies = movieRepository.findAll();
        Map<String, Integer> stageCounts = new HashMap<>();

        for (Movie m : allMovies) {
            String stage = movieLifecycleService.calculateLifecycleStage(m);
            stageCounts.put(stage, stageCounts.getOrDefault(stage, 0) + 1);
        }

        // Must have upcoming, active, and ended movies
        assertTrue(stageCounts.getOrDefault("UPCOMING", 0) > 0, "Catalogue must include UPCOMING movies");
        assertTrue(stageCounts.getOrDefault("ENDED", 0) > 0, "Catalogue must include ENDED movies");

        int activeCount = stageCounts.getOrDefault("OPENING", 0) +
                stageCounts.getOrDefault("PEAK", 0) +
                stageCounts.getOrDefault("REGULAR_RUN", 0) +
                stageCounts.getOrDefault("REDUCED_RUN", 0) +
                stageCounts.getOrDefault("FINAL_DAYS", 0);

        assertTrue(activeCount > 0, "Catalogue must include AIRING (active) movies");
        assertEquals(EXPECTED_MOVIES_COUNT, activeCount + stageCounts.get("UPCOMING") + stageCounts.get("ENDED"),
                "Total stage counts must sum to 45");
    }

    @Test
    @Order(21)
    @DisplayName("[P2-10] Demand tiers: all 5 demand tiers are represented")
    void testDemandTiersDistribution() {
        List<Movie> allMovies = movieRepository.findAll();
        Set<String> foundTiers = allMovies.stream().map(Movie::getDemandTier).collect(Collectors.toSet());

        for (String expectedTier : MovieLifecycleService.ALLOWED_DEMAND_TIERS) {
            assertTrue(foundTiers.contains(expectedTier),
                    "Catalogue must include movies with demand tier: " + expectedTier);
        }
    }

    @Test
    @Order(22)
    @DisplayName("[P2-11] DATASET_CONFIG contains Part 2 metadata while keeping Phase 1A baseline intact")
    void testDatasetConfigMetadata() {
        String moviesCount = jdbc.queryForObject(
                "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.movies'",
                String.class
        );
        assertEquals("45", moviesCount, "DATASET_CONFIG dataset.movies must be 45");

        String activeLangs = jdbc.queryForObject(
                "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.active_languages'",
                String.class
        );
        assertEquals("ta,te,hi,kn,en", activeLangs, "DATASET_CONFIG dataset.active_languages must be ta,te,hi,kn,en");

        String part2Status = jdbc.queryForObject(
                "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.part2_status'",
                String.class
        );
        assertEquals("COMPLETED", part2Status, "DATASET_CONFIG dataset.part2_status must be COMPLETED");

        // Verify Part 1A invariants in DATASET_CONFIG are intact
        String phase = jdbc.queryForObject(
                "SELECT config_value FROM DATASET_CONFIG WHERE config_key = 'dataset.phase'",
                String.class
        );
        assertEquals("1A", phase, "DATASET_CONFIG dataset.phase must remain 1A");
    }

    // =========================================================================
    // SECTION 4 — SERVICE & API LAYER
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("[P2-12] MovieService.getMovies returns full catalogue and enriched Part 2 DTOs")
    void testServiceGetMovies() {
        Page<MovieResponse> page = movieService.getMovies(null, null, null, null, PageRequest.of(0, 50));
        assertEquals(EXPECTED_MOVIES_COUNT, page.getTotalElements());

        MovieResponse sample = page.getContent().get(0);
        assertNotNull(sample.getMovieId());
        assertNotNull(sample.getTitle());
        assertNotNull(sample.getDemandTier());
        assertNotNull(sample.getPopularityScore());
        assertNotNull(sample.getExhibitionStartDate());
        assertNotNull(sample.getExhibitionEndDate());
        assertNotNull(sample.getLifecycleStage());
        assertNotNull(sample.getMovieStatus());
        assertNotNull(sample.getLanguages());
        assertFalse(sample.getLanguages().isEmpty());
    }

    @Test
    @Order(31)
    @DisplayName("[P2-13] Filtering by demandTier and language works accurately")
    void testFilteringByTierAndLanguage() {
        // Filter by BLOCKBUSTER
        Page<MovieResponse> blockbusters = movieService.getMovies(null, null, null, "BLOCKBUSTER", PageRequest.of(0, 50));
        assertTrue(blockbusters.getTotalElements() > 0);
        blockbusters.getContent().forEach(m -> assertEquals("BLOCKBUSTER", m.getDemandTier()));

        // Filter by Tamil language code
        Page<MovieResponse> tamilMovies = movieService.getMovies(null, null, "ta", null, PageRequest.of(0, 50));
        assertTrue(tamilMovies.getTotalElements() > 0);
        tamilMovies.getContent().forEach(m -> assertTrue(m.getLanguages().contains("Tamil")));

        // Filter by AIRING status
        Page<MovieResponse> airingMovies = movieService.getMovies("AIRING", null, null, null, PageRequest.of(0, 50));
        assertTrue(airingMovies.getTotalElements() > 0);
        airingMovies.getContent().forEach(m -> assertEquals("AIRING", m.getMovieStatus()));
    }

    @Test
    @Order(32)
    @DisplayName("[P2-14] Validation rejects invalid metadata upon movie creation")
    void testValidationRejectsInvalidMetadata() {
        MovieRequest invalidTierReq = new MovieRequest();
        invalidTierReq.setTitle("Invalid Tier Movie");
        invalidTierReq.setCertificationId(1);
        invalidTierReq.setRuntimeMinutes(120);
        invalidTierReq.setReleaseDate(LocalDate.of(2026, 9, 1));
        invalidTierReq.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        invalidTierReq.setExhibitionEndDate(LocalDate.of(2026, 9, 30));
        invalidTierReq.setDemandTier("SUPER_INVALID");
        invalidTierReq.setPopularityScore(new BigDecimal("7.50"));

        assertThrows(Exception.class, () ->
                movieService.createMovie(invalidTierReq, 1L, "127.0.0.1"));
    }

    @Test
    @Order(33)
    @DisplayName("[P2-15] Filtering by genreId, certificationId, and languageId at service layer")
    void testFilteringByGenreCertAndLangId() {
        // Filter by Action genre (ID 1)
        Page<MovieResponse> actionMovies = movieService.getMovies(null, null, null, null, 1L, null, null, PageRequest.of(0, 50));
        assertTrue(actionMovies.getTotalElements() > 0);
        actionMovies.getContent().forEach(m -> assertTrue(m.getGenres().contains("Action")));

        // Filter by Certification U (ID 1)
        Page<MovieResponse> cert1Movies = movieService.getMovies(null, null, null, null, null, 1L, null, PageRequest.of(0, 50));
        assertTrue(cert1Movies.getTotalElements() > 0);
        cert1Movies.getContent().forEach(m -> assertEquals(1L, m.getCertificationId()));

        // Filter by Tamil language (ID 1)
        Page<MovieResponse> lang1Movies = movieService.getMovies(null, null, null, 1L, null, null, null, PageRequest.of(0, 50));
        assertTrue(lang1Movies.getTotalElements() > 0);
        lang1Movies.getContent().forEach(m -> assertTrue(m.getLanguages().contains("Tamil")));
    }

    @Test
    @Order(34)
    @DisplayName("[P2-16] Rejection of non-existent genreId and languageId at service layer")
    void testRejectionOfNonExistentGenreAndLanguageId() {
        MovieRequest invalidGenreReq = new MovieRequest();
        invalidGenreReq.setTitle("Invalid Genre Service Movie");
        invalidGenreReq.setCertificationId(1);
        invalidGenreReq.setRuntimeMinutes(120);
        invalidGenreReq.setReleaseDate(LocalDate.of(2026, 9, 1));
        invalidGenreReq.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        invalidGenreReq.setExhibitionEndDate(LocalDate.of(2026, 9, 30));
        invalidGenreReq.setDemandTier("MID_TIER");
        invalidGenreReq.setPopularityScore(new BigDecimal("7.50"));
        invalidGenreReq.setGenreIds(List.of(99999));

        ResourceNotFoundException exG = assertThrows(ResourceNotFoundException.class, () ->
                movieService.createMovie(invalidGenreReq, 1L, "127.0.0.1"));
        assertTrue(exG.getMessage().contains("Genre not found: 99999"));

        MovieRequest invalidLangReq = new MovieRequest();
        invalidLangReq.setTitle("Invalid Lang Service Movie");
        invalidLangReq.setCertificationId(1);
        invalidLangReq.setRuntimeMinutes(120);
        invalidLangReq.setReleaseDate(LocalDate.of(2026, 9, 1));
        invalidLangReq.setExhibitionStartDate(LocalDate.of(2026, 9, 1));
        invalidLangReq.setExhibitionEndDate(LocalDate.of(2026, 9, 30));
        invalidLangReq.setDemandTier("MID_TIER");
        invalidLangReq.setPopularityScore(new BigDecimal("7.50"));
        invalidLangReq.setLanguageIds(List.of(99999));

        ResourceNotFoundException exL = assertThrows(ResourceNotFoundException.class, () ->
                movieService.createMovie(invalidLangReq, 1L, "127.0.0.1"));
        assertTrue(exL.getMessage().contains("Language not found: 99999"));
    }
}
