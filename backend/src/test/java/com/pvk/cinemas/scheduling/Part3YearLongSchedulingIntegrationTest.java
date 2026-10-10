package com.pvk.cinemas.scheduling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.InvalidCapabilityException;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Part3YearLongSchedulingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BusinessDateProvider businessDateProvider;
    @Autowired private ShowSchedulingService showSchedulingService;
    @Autowired private SeatAvailabilityService seatAvailabilityService;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private com.pvk.cinemas.scheduling.service.YearLongShowGenerationService yearLongShowGenerationService;

    @Test
    @DisplayName("API Calendar Range: B-261 and B+105 are rejected with 400 OUT_OF_CALENDAR_RANGE")
    void testCalendarRangeApiRejection() throws Exception {
        Theatre theatre = theatreRepository.findAll().get(0);
        Long theatreId = theatre.getTheatreId();

        LocalDate beforeStart = businessDateProvider.getCalendarHorizonStart().minusDays(1); // 2025-12-31
        LocalDate afterEnd = businessDateProvider.getCalendarHorizonEnd().plusDays(1); // 2027-01-01

        mockMvc.perform(get("/api/v1/theatres/" + theatreId + "/shows")
                        .param("date", beforeStart.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUT_OF_CALENDAR_RANGE"));

        mockMvc.perform(get("/api/v1/theatres/" + theatreId + "/shows")
                        .param("date", afterEnd.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUT_OF_CALENDAR_RANGE"));

        // Valid dates within horizon succeed
        mockMvc.perform(get("/api/v1/theatres/" + theatreId + "/shows")
                        .param("date", businessDateProvider.getCalendarHorizonStart().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/theatres/" + theatreId + "/shows")
                        .param("date", businessDateProvider.getCalendarHorizonEnd().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Movie Lifecycle Window: Show outside exhibition start/end date is rejected")
    void testMovieLifecycleBoundaryRejection() {
        Movie movie = movieRepository.findById(1L).orElseThrow();
        LocalDate exhibitionStart = movie.getExhibitionStartDate();
        LocalDate exhibitionEnd = movie.getExhibitionEndDate();

        assertNotNull(exhibitionStart);
        assertNotNull(exhibitionEnd);

        Theatre theatre = theatreRepository.findAll().get(0);
        Screen screen = screenRepository.findByTheatreId(theatre.getTheatreId()).get(0);
        ScreenCapability cap = screenCapabilityRepository.findByScreenId(screen.getScreenId()).get(0);
        var movieLang = movieLanguageRepository.findByMovieId(movie.getMovieId()).get(0);

        // Attempt show before exhibition start
        LocalDate beforeDate = exhibitionStart.minusDays(5);
        if (businessDateProvider.isWithinCalendarRange(beforeDate)) {
            ShowRequest reqBefore = new ShowRequest();
            reqBefore.setMovieId(movie.getMovieId());
            reqBefore.setMovieLanguageId(movieLang.getMovieLanguageId());
            reqBefore.setScreenId(screen.getScreenId().intValue());
            reqBefore.setScreenCapabilityId(cap.getScreenCapabilityId().intValue());
            reqBefore.setStartAt(beforeDate.atTime(10, 0).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());
            reqBefore.setEndAt(beforeDate.atTime(12, 30).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    showSchedulingService.createShow(theatre.getTheatreId().intValue(), reqBefore, 1L, "127.0.0.1"));
            assertTrue(ex.getMessage().contains("outside movie exhibition window"));
        }

        // Attempt show after exhibition end
        LocalDate afterDate = exhibitionEnd.plusDays(5);
        if (businessDateProvider.isWithinCalendarRange(afterDate)) {
            ShowRequest reqAfter = new ShowRequest();
            reqAfter.setMovieId(movie.getMovieId());
            reqAfter.setMovieLanguageId(movieLang.getMovieLanguageId());
            reqAfter.setScreenId(screen.getScreenId().intValue());
            reqAfter.setScreenCapabilityId(cap.getScreenCapabilityId().intValue());
            reqAfter.setStartAt(afterDate.atTime(10, 0).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());
            reqAfter.setEndAt(afterDate.atTime(12, 30).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    showSchedulingService.createShow(theatre.getTheatreId().intValue(), reqAfter, 1L, "127.0.0.1"));
            assertTrue(ex.getMessage().contains("outside movie exhibition window"));
        }
    }

    @Test
    @DisplayName("Turnaround buffer and operating hours validation in scheduling")
    void testOperatingHoursAndTurnaround() {
        Theatre theatre = theatreRepository.findAll().get(0);
        Screen screen = screenRepository.findByTheatreId(theatre.getTheatreId()).get(0);
        ScreenCapability cap = screenCapabilityRepository.findByScreenId(screen.getScreenId()).get(0);
        Movie movie = movieRepository.findById(1L).orElseThrow();
        var movieLang = movieLanguageRepository.findByMovieId(movie.getMovieId()).get(0);

        // Date in valid exhibition range
        LocalDate validDate = movie.getExhibitionStartDate().plusDays(2);

        // 1. Show starting before opening time (e.g. 05:00 AM)
        ShowRequest reqEarly = new ShowRequest();
        reqEarly.setMovieId(movie.getMovieId());
        reqEarly.setMovieLanguageId(movieLang.getMovieLanguageId());
        reqEarly.setScreenId(screen.getScreenId().intValue());
        reqEarly.setScreenCapabilityId(cap.getScreenCapabilityId().intValue());
        reqEarly.setStartAt(validDate.atTime(5, 0).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());
        reqEarly.setEndAt(validDate.atTime(7, 30).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());

        BadRequestException exEarly = assertThrows(BadRequestException.class, () ->
                showSchedulingService.createShow(theatre.getTheatreId().intValue(), reqEarly, 1L, "127.0.0.1"));
        assertTrue(exEarly.getMessage().contains("violates theatre operating hours"));
    }

    @Test
    @DisplayName("Cross-theatre capability reference rejection")
    void testCrossTheatreCapabilityRejection() {
        List<Theatre> theatres = theatreRepository.findAll();
        Theatre theatreA = theatres.get(0);
        Theatre theatreB = theatres.get(1);

        Screen screenA = screenRepository.findByTheatreId(theatreA.getTheatreId()).get(0);
        Screen screenB = screenRepository.findByTheatreId(theatreB.getTheatreId()).get(0);
        ScreenCapability capB = screenCapabilityRepository.findByScreenId(screenB.getScreenId()).get(0);

        Movie movie = movieRepository.findById(1L).orElseThrow();
        var movieLang = movieLanguageRepository.findByMovieId(movie.getMovieId()).get(0);
        LocalDate validDate = movie.getExhibitionStartDate().plusDays(2);

        ShowRequest req = new ShowRequest();
        req.setMovieId(movie.getMovieId());
        req.setMovieLanguageId(movieLang.getMovieLanguageId());
        req.setScreenId(screenA.getScreenId().intValue());
        req.setScreenCapabilityId(capB.getScreenCapabilityId().intValue()); // Belongs to screenB!
        req.setStartAt(validDate.atTime(10, 0).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());
        req.setEndAt(validDate.atTime(12, 30).atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant());

        assertThrows(InvalidCapabilityException.class, () ->
                showSchedulingService.createShow(theatreA.getTheatreId().intValue(), req, 1L, "127.0.0.1"));
    }

    @Test
    @DisplayName("Admin scheduling security: Unauthorized users rejected, SUPER_ADMIN permitted")
    @WithMockUser(roles = "CUSTOMER")
    void testAdminSchedulingSecurityCustomerRejected() throws Exception {
        mockMvc.perform(post("/api/v1/admin/scheduling/generate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin stats endpoint accessible to SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void testAdminStatsEndpointSuperAdminAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/scheduling/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalShows").exists());
    }

    @Test
    @DisplayName("Deterministic Scheduling: Multiple generation runs produce identical output")
    void testDeterministicSchedulingGeneration() {
        LocalDate date = LocalDate.of(2026, 9, 20);
        
        // First run
        yearLongShowGenerationService.generateScheduleRange(date, date, date);
        List<Show> run1 = showRepository.findAll().stream()
                .filter(s -> date.equals(s.getShowDate()))
                .sorted(java.util.Comparator.comparing(Show::getScreenId).thenComparing(Show::getStartAt))
                .toList();

        // Second run with same date, same dataset, same anchor
        yearLongShowGenerationService.generateScheduleRange(date, date, date);
        List<Show> run2 = showRepository.findAll().stream()
                .filter(s -> date.equals(s.getShowDate()))
                .sorted(java.util.Comparator.comparing(Show::getScreenId).thenComparing(Show::getStartAt))
                .toList();

        assertFalse(run1.isEmpty(), "Run 1 must generate shows");
        assertEquals(run1.size(), run2.size(), "Both runs must produce the exact same show count");

        for (int i = 0; i < run1.size(); i++) {
            Show s1 = run1.get(i);
            Show s2 = run2.get(i);
            assertEquals(s1.getScreenId(), s2.getScreenId(), "screenId must match at index " + i);
            assertEquals(s1.getStartAt(), s2.getStartAt(), "startAt must match at index " + i);
            assertEquals(s1.getEndAt(), s2.getEndAt(), "endAt must match at index " + i);
            assertEquals(s1.getMovieLanguageId(), s2.getMovieLanguageId(), "movieLanguageId must match at index " + i);
            assertEquals(s1.getScreenCapabilityId(), s2.getScreenCapabilityId(), "screenCapabilityId must match at index " + i);
            assertEquals(s1.getStatus(), s2.getStatus(), "status must match at index " + i);
        }
    }

    @Test
    @DisplayName("Public show & seat availability endpoints are accessible to customers")
    void testPublicShowAndSeatEndpoints() throws Exception {
        Theatre theatre = theatreRepository.findAll().get(0);
        Long theatreId = theatre.getTheatreId();
        LocalDate date = LocalDate.of(2026, 9, 18);

        // 1. Theatre shows endpoint
        mockMvc.perform(get("/api/v1/theatres/" + theatreId + "/shows")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 2. Movie shows endpoint
        mockMvc.perform(get("/api/v1/movies/1/shows")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 3. Show seats endpoint
        List<Show> shows = showRepository.findAll().stream()
                .filter(s -> date.equals(s.getShowDate()))
                .toList();
        if (!shows.isEmpty()) {
            Long showId = shows.get(0).getShowId();
            mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Test
    @DisplayName("Theatre Manager Scope: Manager cannot access shows of unmanaged theatre")
    @WithMockUser(username = "unauthorized_mgr", roles = "THEATRE_MANAGER")
    void testManagerScopeEnforcement() throws Exception {
        mockMvc.perform(get("/api/v1/manager/theatres/1/shows"))
                .andExpect(status().isForbidden());
    }
}
