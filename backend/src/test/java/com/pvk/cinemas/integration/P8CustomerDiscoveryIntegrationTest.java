package com.pvk.cinemas.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.catalogue.model.AudioFormat;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.*;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class P8CustomerDiscoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieLanguageRepository movieLanguageRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ScreenCapabilityRepository screenCapabilityRepository;

    @Autowired
    private PresentationFormatRepository presentationFormatRepository;

    @Autowired
    private AudioFormatRepository audioFormatRepository;

    @Test
    @DisplayName("P8-01: Complete Customer Discovery Journey (City -> Theatre -> Movie -> Shows -> Show Details -> Seat Availability)")
    void testCompleteCustomerDiscoveryJourney() throws Exception {
        // Step 1: Customer discovers discoverable cities
        MvcResult citiesResult = mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode citiesNode = objectMapper.readTree(citiesResult.getResponse().getContentAsString()).path("data");
        assertTrue(citiesNode.size() > 0, "Cities list must not be empty");
        Long cityId = citiesNode.get(0).path("cityId").asLong();

        // Step 2: Customer discovers active theatres in that city
        MvcResult theatresResult = mockMvc.perform(get("/api/v1/cities/{cityId}/theatres", cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode theatresNode = objectMapper.readTree(theatresResult.getResponse().getContentAsString()).path("data");
        assertTrue(theatresNode.size() > 0, "Theatres list in selected city must not be empty");
        Long theatreId = theatresNode.get(0).path("theatreId").asLong();

        // Step 3: Customer discovers active catalogue movies
        MvcResult moviesResult = mockMvc.perform(get("/api/v1/movies?size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        JsonNode moviesNode = objectMapper.readTree(moviesResult.getResponse().getContentAsString()).path("data").path("content");
        assertTrue(moviesNode.size() > 0, "Movies list must not be empty");
        Long movieId = moviesNode.get(0).path("movieId").asLong();

        // Step 4: Customer views single movie details
        mockMvc.perform(get("/api/v1/movies/{movieId}", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movieId").value(movieId))
                .andExpect(jsonPath("$.data.title").isNotEmpty())
                .andExpect(jsonPath("$.data.genres").isArray())
                .andExpect(jsonPath("$.data.languages").isArray());

        // Step 5: Customer discovers shows for the movie filtered by city
        MvcResult showsResult = mockMvc.perform(get("/api/v1/movies/{movieId}/shows?cityId={cityId}", movieId, cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode showsNode = objectMapper.readTree(showsResult.getResponse().getContentAsString()).path("data");
        if (showsNode.size() > 0) {
            Long showId = showsNode.get(0).path("showId").asLong();

            // Step 6: Customer views single show details
            mockMvc.perform(get("/api/v1/shows/{showId}", showId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.showId").value(showId))
                    .andExpect(jsonPath("$.data.movieTitle").isNotEmpty())
                    .andExpect(jsonPath("$.data.theatreName").isNotEmpty())
                    .andExpect(jsonPath("$.data.screenName").isNotEmpty())
                    .andExpect(jsonPath("$.data.startAt").isNotEmpty());

            // Step 7: Customer checks seat availability for the show
            mockMvc.perform(get("/api/v1/shows/{showId}/seats", showId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.showId").value(showId))
                    .andExpect(jsonPath("$.data.seats").isArray())
                    .andExpect(jsonPath("$.data.totalSeats").isNumber())
                    .andExpect(jsonPath("$.data.availableSeats").isNumber());
        }
    }

    @Test
    @DisplayName("P8-02: Movie Shows Filtered by City, Theatre, Date, Language, Presentation Format, and Audio Format")
    void testMovieShowsIndividualAndCombinedFilters() throws Exception {
        List<Show> allShows = showRepository.findAll();
        assertFalse(allShows.isEmpty(), "Show repository must have seeded shows");
        Show sampleShow = allShows.get(0);

        MovieLanguage ml = movieLanguageRepository.findById(sampleShow.getMovieLanguageId()).orElseThrow();
        Long movieId = ml.getMovieId();
        Long languageId = ml.getLanguageId();
        ScreenCapability cap = screenCapabilityRepository.findById(sampleShow.getScreenCapabilityId()).orElseThrow();
        Long presFormatId = cap.getPresentationFormatId();
        Long audioFormatId = cap.getAudioFormatId();

        LocalDate showDate = sampleShow.getShowDate() != null
                ? sampleShow.getShowDate()
                : sampleShow.getStartAt().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();

        Screen screen = screenRepository.findById(cap.getScreenId()).orElseThrow();
        Long theatreId = screen.getTheatreId();
        Theatre theatre = theatreRepository.findById(theatreId).orElseThrow();
        Long cityId = theatre.getCityId();

        // 1. Filter by languageId
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?languageId={langId}", movieId, languageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 2. Filter by date
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?date={date}", movieId, showDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 3. Filter by presentationFormatId
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?presentationFormatId={pfId}", movieId, presFormatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 4. Filter by audioFormatId
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?audioFormatId={afId}", movieId, audioFormatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 5. Dedicated filter by cityId (verifying actual returned records match cityId)
        MvcResult cityResult = mockMvc.perform(get("/api/v1/movies/{movieId}/shows?cityId={cityId}", movieId, cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        JsonNode cityShows = objectMapper.readTree(cityResult.getResponse().getContentAsString()).path("data");
        assertFalse(cityShows.isEmpty(), "Shows for movie in city must not be empty");
        for (JsonNode s : cityShows) {
            assertEquals(cityId, s.path("cityId").asLong(), "Every returned show must match filtered cityId");
        }

        // 6. Dedicated filter by theatreId (verifying actual returned records match theatreId)
        MvcResult theatreResult = mockMvc.perform(get("/api/v1/movies/{movieId}/shows?theatreId={theatreId}", movieId, theatreId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        JsonNode theatreShows = objectMapper.readTree(theatreResult.getResponse().getContentAsString()).path("data");
        assertFalse(theatreShows.isEmpty(), "Shows for movie in theatre must not be empty");
        for (JsonNode s : theatreShows) {
            assertEquals(theatreId, s.path("theatreId").asLong(), "Every returned show must match filtered theatreId");
        }

        // 7. Combined cityId + theatreId filter
        MvcResult combinedResult = mockMvc.perform(get("/api/v1/movies/{movieId}/shows?cityId={cityId}&theatreId={theatreId}", movieId, cityId, theatreId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        JsonNode combinedShows = objectMapper.readTree(combinedResult.getResponse().getContentAsString()).path("data");
        assertFalse(combinedShows.isEmpty(), "Shows for movie in city and theatre must not be empty");
        for (JsonNode s : combinedShows) {
            assertEquals(cityId, s.path("cityId").asLong(), "Combined filter: cityId must match");
            assertEquals(theatreId, s.path("theatreId").asLong(), "Combined filter: theatreId must match");
        }

        // 8. Chronological ordering verification for movie-shows discovery endpoint
        Instant prevMovieShowTime = null;
        for (JsonNode s : cityShows) {
            Instant curr = Instant.parse(s.path("startAt").asText());
            if (prevMovieShowTime != null) {
                assertFalse(curr.isBefore(prevMovieShowTime), "Movie shows must be returned in chronological order (startAt ASC)");
            }
            prevMovieShowTime = curr;
        }

        // 9. Combined multi-parameter filter (date + languageId + presentationFormatId + audioFormatId)
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?date={date}&languageId={langId}&presentationFormatId={pfId}&audioFormatId={afId}",
                        movieId, showDate, languageId, presFormatId, audioFormatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("P8-02b: Invalid Filter Parameter Semantics (404 for invalid cityId/theatreId/movieId vs 200 empty array for catalogue filters)")
    void testInvalidFilterAndReferenceSemantics() throws Exception {
        List<Movie> movies = movieRepository.findAll();
        assertFalse(movies.isEmpty());
        Long movieId = movies.get(0).getMovieId();

        List<Theatre> theatres = theatreRepository.findAll();
        assertFalse(theatres.isEmpty());
        Long theatreId = theatres.get(0).getTheatreId();

        // 1. Invalid cityId filter on /movies/{movieId}/shows -> 404 RESOURCE_NOT_FOUND
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?cityId=999999", movieId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // 2. Invalid theatreId filter on /movies/{movieId}/shows -> 404 RESOURCE_NOT_FOUND
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?theatreId=999999", movieId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // 3. Invalid movieId filter on /theatres/{theatreId}/shows -> 404 RESOURCE_NOT_FOUND
        mockMvc.perform(get("/api/v1/theatres/{theatreId}/shows?movieId=999999", theatreId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // 4. Invalid languageId filter on /movies/{movieId}/shows -> 200 OK with empty array []
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?languageId=999999", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        // 5. Invalid presentationFormatId filter on /movies/{movieId}/shows -> 200 OK with empty array []
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?presentationFormatId=999999", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        // 6. Invalid audioFormatId filter on /movies/{movieId}/shows -> 200 OK with empty array []
        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?audioFormatId=999999", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("P8-03: Theatre Shows Filtered by Date and MovieId with Chronological Order")
    void testTheatreShowsFilteredByDateAndMovieId() throws Exception {
        List<Show> allShows = showRepository.findAll();
        assertFalse(allShows.isEmpty());
        Show sampleShow = allShows.get(0);
        MovieLanguage ml = movieLanguageRepository.findById(sampleShow.getMovieLanguageId()).orElseThrow();
        Long movieId = ml.getMovieId();

        // Resolve theatre for this show
        ScreenCapability cap = screenCapabilityRepository.findById(sampleShow.getScreenCapabilityId()).orElseThrow();
        Long screenId = cap.getScreenId();
        Theatre theatre = theatreRepository.findAll().stream()
                .filter(t -> t.getTheatreId() != null)
                .findFirst().orElseThrow();
        Long theatreId = theatre.getTheatreId();

        LocalDate showDate = sampleShow.getShowDate() != null
                ? sampleShow.getShowDate()
                : sampleShow.getStartAt().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();

        // Filter theatre shows by date and movieId
        MvcResult result = mockMvc.perform(get("/api/v1/theatres/{theatreId}/shows?date={date}&movieId={movieId}",
                        theatreId, showDate, movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode showsArray = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        // Verify chronological order
        Instant prev = null;
        for (JsonNode sNode : showsArray) {
            Instant curr = Instant.parse(sNode.path("startAt").asText());
            if (prev != null) {
                assertFalse(curr.isBefore(prev), "Shows must be chronologically ordered");
            }
            prev = curr;
        }
    }

    @Test
    @DisplayName("P8-04: Non-existent Resource Handling returns 404 with RESOURCE_NOT_FOUND")
    void testNonExistentResourcesReturn404() throws Exception {
        // Non-existent movie on GET /movies/{id}
        mockMvc.perform(get("/api/v1/movies/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Non-existent movie on GET /movies/{id}/shows
        mockMvc.perform(get("/api/v1/movies/999999/shows"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Non-existent theatre on GET /theatres/{id}
        mockMvc.perform(get("/api/v1/theatres/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Non-existent theatre on GET /theatres/{id}/shows
        mockMvc.perform(get("/api/v1/theatres/999999/shows"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Non-existent show on GET /shows/{id}
        mockMvc.perform(get("/api/v1/shows/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Non-existent show on GET /shows/{id}/seats
        mockMvc.perform(get("/api/v1/shows/999999/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Non-existent city on GET /cities/{id}
        mockMvc.perform(get("/api/v1/cities/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("P8-05: Direct Show Details Verification (Positive and Negative)")
    void testShowDetailsDirectVerification() throws Exception {
        List<Show> allShows = showRepository.findAll();
        assertFalse(allShows.isEmpty());
        Show sample = allShows.get(0);

        mockMvc.perform(get("/api/v1/shows/{showId}", sample.getShowId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.showId").value(sample.getShowId()))
                .andExpect(jsonPath("$.data.movieTitle").isNotEmpty())
                .andExpect(jsonPath("$.data.theatreName").isNotEmpty())
                .andExpect(jsonPath("$.data.screenName").isNotEmpty())
                .andExpect(jsonPath("$.data.startAt").isNotEmpty())
                .andExpect(jsonPath("$.data.endAt").isNotEmpty())
                .andExpect(jsonPath("$.data.totalSeats").isNumber())
                .andExpect(jsonPath("$.data.availableSeats").isNumber());

        mockMvc.perform(get("/api/v1/shows/8888888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("P8-06: Calendar Horizon Validation Rejection (400 OUT_OF_CALENDAR_RANGE)")
    void testCalendarHorizonValidationRejection() throws Exception {
        List<Movie> movies = movieRepository.findAll();
        assertFalse(movies.isEmpty());
        Long movieId = movies.get(0).getMovieId();

        // 500 days in the future is outside the 365-day calendar horizon
        LocalDate outOfRangeDate = LocalDate.now().plusDays(500);

        mockMvc.perform(get("/api/v1/movies/{movieId}/shows?date={date}", movieId, outOfRangeDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUT_OF_CALENDAR_RANGE"));
    }

    @Test
    @DisplayName("P8-07: Catalogue Text Query and Sorting on GET /api/v1/movies")
    void testCatalogueTextQueryAndSorting() throws Exception {
        List<Movie> movies = movieRepository.findAll();
        assertFalse(movies.isEmpty());
        String keyword = movies.get(0).getTitle().substring(0, Math.min(4, movies.get(0).getTitle().length()));

        // Query by q
        mockMvc.perform(get("/api/v1/movies?q={q}", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        // Sort by title asc
        mockMvc.perform(get("/api/v1/movies?sort=title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        // Sort by releaseDate desc
        mockMvc.perform(get("/api/v1/movies?sort=releaseDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("P8-08: Public Browsing Unauthenticated Accessibility (Zero Trust PermitAll)")
    void testPublicBrowsingPermitAll() throws Exception {
        // Confirm all discovery endpoints respond without Authorization header
        mockMvc.perform(get("/api/v1/movies")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/cities")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/theatres")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/seat-types")).andExpect(status().isOk());
    }
}
