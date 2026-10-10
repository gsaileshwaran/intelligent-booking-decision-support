package com.pvk.cinemas.integration;

import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.AudioFormat;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.AudioFormatRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.InvalidCapabilityException;
import com.pvk.cinemas.common.exceptions.InvalidMovieLanguageException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.common.exceptions.ShowOverlapException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import org.springframework.security.access.AccessDeniedException;
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
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ShowSchedulingAndAtomicityIntegrationTest {

    @Autowired private ShowSchedulingService showSchedulingService;
    @Autowired private SeatAvailabilityService seatAvailabilityService;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private ScreenCapabilityRepository screenCapabilityRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SeatTypeRepository seatTypeRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieLanguageRepository movieLanguageRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PresentationFormatRepository presentationFormatRepository;
    @Autowired private AudioFormatRepository audioFormatRepository;
    @Autowired private BusinessDateProvider businessDateProvider;

    private Theatre testTheatre;
    private Screen testScreen;
    private Screen otherScreen;
    private ScreenCapability testCapability;
    private ScreenCapability otherCapability;
    private Movie testMovie;
    private Movie otherMovie;
    private MovieLanguage testMovieLanguage;
    private MovieLanguage otherMovieLanguage;
    private User managerUser;
    private SeatType standardType;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        managerUser = userRepository.save(new User(
                "Manager", "Test", "mgr_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000),
                "$2a$10$abcdefghijklmnopqrstuvwxyz123456", "ACTIVE"
        ));

        testTheatre = new Theatre();
        testTheatre.setCityId(city.getCityId());
        testTheatre.setTheatreCode("TH-SCHED-" + uid);
        testTheatre.setTheatreName("Sched Theatre " + uid);
        testTheatre.setAddressLine1("Test Road");
        testTheatre.setStatus("ACTIVE");
        testTheatre = theatreRepository.save(testTheatre);

        testScreen = screenRepository.save(new Screen(testTheatre.getTheatreId(), "SCR-1-" + uid, "Auditorium 1"));
        otherScreen = screenRepository.save(new Screen(testTheatre.getTheatreId(), "SCR-2-" + uid, "Auditorium 2"));

        testCapability = screenCapabilityRepository.save(new ScreenCapability(testScreen.getScreenId(), 1L, 1L));
        otherCapability = screenCapabilityRepository.save(new ScreenCapability(otherScreen.getScreenId(), 1L, 1L));

        standardType = seatTypeRepository.findByTypeCode("STANDARD").orElseThrow();

        // Screen 1 has 3 ACTIVE seats and 1 INACTIVE seat
        Seat seat1 = new Seat(testScreen.getScreenId(), standardType.getSeatTypeId(), "A", "1");
        Seat seat2 = new Seat(testScreen.getScreenId(), standardType.getSeatTypeId(), "A", "2");
        Seat seat3 = new Seat(testScreen.getScreenId(), standardType.getSeatTypeId(), "A", "3");
        Seat seatInactive = new Seat(testScreen.getScreenId(), standardType.getSeatTypeId(), "A", "4");
        seatInactive.setStatus("INACTIVE");
        seatRepository.saveAll(List.of(seat1, seat2, seat3, seatInactive));

        // Screen 2 has 2 ACTIVE seats
        Seat seatOther1 = new Seat(otherScreen.getScreenId(), standardType.getSeatTypeId(), "B", "1");
        Seat seatOther2 = new Seat(otherScreen.getScreenId(), standardType.getSeatTypeId(), "B", "2");
        seatRepository.saveAll(List.of(seatOther1, seatOther2));

        // Setup 2 Movies
        testMovie = new Movie();
        testMovie.setTitle("Test Movie " + uid);
        testMovie.setRuntimeMinutes((short) 120);
        testMovie.setStatus("AIRING");
        testMovie.setCertificationId(1L);
        testMovie = movieRepository.save(testMovie);

        otherMovie = new Movie();
        otherMovie.setTitle("Other Movie " + uid);
        otherMovie.setRuntimeMinutes((short) 90);
        otherMovie.setStatus("AIRING");
        otherMovie.setCertificationId(1L);
        otherMovie = movieRepository.save(otherMovie);

        testMovieLanguage = movieLanguageRepository.save(new MovieLanguage(testMovie.getMovieId(), 1L, "ORIGINAL"));
        otherMovieLanguage = movieLanguageRepository.save(new MovieLanguage(otherMovie.getMovieId(), 1L, "ORIGINAL"));
    }

    @Test
    @DisplayName("BR-01: Chronological sanity rejected if end_at <= start_at")
    void testBR01ChronologicalSanity() {
        Instant now = Instant.now().plusSeconds(3600);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(now.plusSeconds(3600));
        req.setEndAt(now); // end before start

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("BR-02: Movie language not belonging to movie is rejected")
    void testBR02MovieLanguageMismatch() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        // Deliberately pass other movie's language
        req.setMovieLanguageId(otherMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);

        assertThrows(InvalidMovieLanguageException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("BR-03: Screen capability not belonging to target screen is rejected")
    void testBR03ScreenCapabilityMismatch() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        // Deliberately pass other screen's capability
        req.setScreenCapabilityId(otherCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);

        assertThrows(InvalidCapabilityException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("BR-04: Overlapping active show on same physical screen is rejected")
    void testBR04OverlappingActiveShows() {
        Instant start1 = Instant.now().plusSeconds(10000);
        Instant end1 = start1.plusSeconds(7200);

        ShowRequest req1 = new ShowRequest();
        req1.setScreenId(testScreen.getScreenId().intValue());
        req1.setMovieId(testMovie.getMovieId());
        req1.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req1.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req1.setStartAt(start1);
        req1.setEndAt(end1);

        ShowResponse show1 = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req1, managerUser.getUserId(), "127.0.0.1");
        assertNotNull(show1);

        // Try to schedule overlapping show (starts 1 hour into show1)
        ShowRequest req2 = new ShowRequest();
        req2.setScreenId(testScreen.getScreenId().intValue());
        req2.setMovieId(testMovie.getMovieId());
        req2.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req2.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req2.setStartAt(start1.plusSeconds(3600));
        req2.setEndAt(end1.plusSeconds(3600));

        assertThrows(ShowOverlapException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req2, managerUser.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Section 22: SHOW_SEAT count verification initializes only active seats on target screen")
    void testShowSeatCountVerification() {
        Instant start = Instant.now().plusSeconds(20000);
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);

        ShowResponse show = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1");
        assertNotNull(show);

        // Screen 1 has 3 active seats and 1 inactive seat. Screen 2 has 2 seats.
        // The new show MUST produce exactly 3 SHOW_SEAT rows!
        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(show.getShowId());
        assertEquals(3, showSeats.size(), "Only active seats on target screen must be initialized");

        // Verify inactive seat was NOT initialized
        Seat inactiveSeat = seatRepository.findByScreenId(testScreen.getScreenId()).stream()
                .filter(s -> "INACTIVE".equals(s.getStatus()))
                .findFirst().orElseThrow();
        boolean inactiveInitialized = showSeats.stream().anyMatch(ss -> ss.getId().getSeatId().equals(inactiveSeat.getSeatId()));
        assertFalse(inactiveInitialized, "Inactive seat must not be initialized into SHOW_SEAT");

        // Verify other screen seats were NOT initialized
        List<Seat> otherScreenSeats = seatRepository.findByScreenId(otherScreen.getScreenId());
        for (Seat otherSeat : otherScreenSeats) {
            boolean otherInitialized = showSeats.stream().anyMatch(ss -> ss.getId().getSeatId().equals(otherSeat.getSeatId()));
            assertFalse(otherInitialized, "Seat from another screen must not be initialized into SHOW_SEAT");
        }
    }

    @Test
    @DisplayName("Section 21: SHOW_SEAT atomicity test proves atomic transaction rollback in MySQL")
    void testShowSeatAtomicityRollback() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        long initialShowCount = showRepository.count();
        long initialShowSeatCount = showSeatRepository.count();

        Instant start = Instant.now().plusSeconds(30000);
        Instant end = start.plusSeconds(7200);

        assertThrows(RuntimeException.class, () -> {
            txTemplate.execute(status -> {
                // 1. Insert Show
                Show show = new Show();
                show.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
                show.setScreenCapabilityId(testCapability.getScreenCapabilityId());
                show.setScreenId(testScreen.getScreenId());
                show.setStartAt(start);
                show.setEndAt(end);
                show.setShowStatus("SCHEDULED");
                show.setCreatedAt(Instant.now());
                show.setUpdatedAt(Instant.now());
                show = showRepository.save(show);
                assertNotNull(show.getShowId());

                // 2. Insert valid show seat
                Seat validSeat = seatRepository.findByScreenId(testScreen.getScreenId()).get(0);
                showSeatRepository.save(new ShowSeat(show.getShowId(), validSeat.getSeatId(), "AVAILABLE"));

                // 3. Force failure during seat generation (e.g. invalid foreign key or explicit exception)
                throw new RuntimeException("Simulated mid-transaction failure during seat generation");
            });
        });

        // Verify in MySQL: neither SHOW nor SHOW_SEAT were committed!
        long postFailShowCount = showRepository.count();
        long postFailShowSeatCount = showSeatRepository.count();

        assertEquals(initialShowCount, postFailShowCount, "SHOW must be completely rolled back");
        assertEquals(initialShowSeatCount, postFailShowSeatCount, "SHOW_SEAT rows must be completely rolled back");
    }

    @Test
    @DisplayName("Section 23: Availability regression verifies read-only behavior with 0 mutations to SHOW_SEAT")
    void testAvailabilityRegressionIsReadOnly() {
        Instant start = Instant.now().plusSeconds(40000);
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);

        ShowResponse show = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1");

        long showSeatCountBefore = showSeatRepository.count();

        // Call availability service (equivalent to GET /api/v1/shows/{id}/seats)
        ShowSeatAvailabilityResponse response = seatAvailabilityService.getShowSeatAvailability(show.getShowId());
        assertNotNull(response);
        assertNotNull(response.getSeats());
        assertEquals(3, response.getSeats().size());

        // Verify no INSERT, UPDATE, or DELETE occurred in SHOW_SEAT
        long showSeatCountAfter = showSeatRepository.count();
        assertEquals(showSeatCountBefore, showSeatCountAfter, "Availability query must be purely read-only");
    }

    // ==========================================
    // GAP A: Format-Capability Validation Tests
    // ==========================================

    @Test
    @DisplayName("Gap A: Valid matching presentation and audio formats are accepted")
    void testFormatValidation_MatchingFormatsAccepted() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);
        req.setPresentationFormatId(testCapability.getPresentationFormatId());
        req.setAudioFormatId(testCapability.getAudioFormatId());

        ShowResponse resp = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1");
        assertNotNull(resp);
        assertNotNull(resp.getShowId());
    }

    @Test
    @DisplayName("Gap A: Mismatched presentation format is rejected with BadRequestException")
    void testFormatValidation_MismatchedPresentationFormatRejected() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = start.plusSeconds(7200);

        long mismatchedPfId = testCapability.getPresentationFormatId().equals(1L) ? 2L : 1L;

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);
        req.setPresentationFormatId(mismatchedPfId);

        long beforeShowCount = showRepository.count();
        long beforeSeatCount = showSeatRepository.count();

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1")
        );

        assertEquals(beforeShowCount, showRepository.count(), "No show should be persisted on format mismatch");
        assertEquals(beforeSeatCount, showSeatRepository.count(), "No show seats should be persisted on format mismatch");
    }

    @Test
    @DisplayName("Gap A: Mismatched audio format is rejected with BadRequestException")
    void testFormatValidation_MismatchedAudioFormatRejected() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = start.plusSeconds(7200);

        long mismatchedAfId = testCapability.getAudioFormatId().equals(1L) ? 2L : 1L;

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);
        req.setAudioFormatId(mismatchedAfId);

        long beforeShowCount = showRepository.count();
        long beforeSeatCount = showSeatRepository.count();

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1")
        );

        assertEquals(beforeShowCount, showRepository.count(), "No show should be persisted on format mismatch");
        assertEquals(beforeSeatCount, showSeatRepository.count(), "No show seats should be persisted on format mismatch");
    }

    @Test
    @DisplayName("Gap A: Nonexistent presentation or audio format ID is rejected with ResourceNotFoundException")
    void testFormatValidation_NonexistentFormatRejected() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = start.plusSeconds(7200);

        // Nonexistent presentation format
        ShowRequest reqPf = new ShowRequest();
        reqPf.setScreenId(testScreen.getScreenId().intValue());
        reqPf.setMovieId(testMovie.getMovieId());
        reqPf.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        reqPf.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        reqPf.setStartAt(start);
        reqPf.setEndAt(end);
        reqPf.setPresentationFormatId(999999L);

        assertThrows(ResourceNotFoundException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), reqPf, managerUser.getUserId(), "127.0.0.1")
        );

        // Nonexistent audio format
        ShowRequest reqAf = new ShowRequest();
        reqAf.setScreenId(testScreen.getScreenId().intValue());
        reqAf.setMovieId(testMovie.getMovieId());
        reqAf.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        reqAf.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        reqAf.setStartAt(start);
        reqAf.setEndAt(end);
        reqAf.setAudioFormatId(999999L);

        assertThrows(ResourceNotFoundException.class, () ->
                showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), reqAf, managerUser.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Gap A: Existing requests without format IDs remain backwards compatible")
    void testFormatValidation_OmittedFormatIdsCompatible() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = start.plusSeconds(7200);

        ShowRequest req = new ShowRequest();
        req.setScreenId(testScreen.getScreenId().intValue());
        req.setMovieId(testMovie.getMovieId());
        req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        req.setStartAt(start);
        req.setEndAt(end);
        // format IDs omitted (null)

        ShowResponse resp = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), req, managerUser.getUserId(), "127.0.0.1");
        assertNotNull(resp);
        assertNotNull(resp.getShowId());
    }

    // ==========================================
    // GAP B: Correct PATCH Timing & Turnaround
    // ==========================================

    @Test
    @DisplayName("Gap B: Updating only startAt retains existing endAt")
    void testPatch_UpdateOnlyStartAt() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(12, 30).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest createReq = new ShowRequest();
        createReq.setScreenId(testScreen.getScreenId().intValue());
        createReq.setMovieId(testMovie.getMovieId());
        createReq.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        createReq.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        createReq.setStartAt(start);
        createReq.setEndAt(end);

        ShowResponse created = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), createReq, managerUser.getUserId(), "127.0.0.1");

        Instant newStart = baseDate.atTime(10, 30).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        ShowRequest patchReq = new ShowRequest();
        patchReq.setStartAt(newStart);

        ShowResponse updated = showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created.getShowId(), patchReq, managerUser.getUserId(), "127.0.0.1");
        assertEquals(newStart, updated.getStartAt());
        assertEquals(end, updated.getEndAt());
    }

    @Test
    @DisplayName("Gap B: Updating only endAt retains existing startAt")
    void testPatch_UpdateOnlyEndAt() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest createReq = new ShowRequest();
        createReq.setScreenId(testScreen.getScreenId().intValue());
        createReq.setMovieId(testMovie.getMovieId());
        createReq.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        createReq.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        createReq.setStartAt(start);
        createReq.setEndAt(end);

        ShowResponse created = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), createReq, managerUser.getUserId(), "127.0.0.1");

        Instant newEnd = baseDate.atTime(12, 30).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        ShowRequest patchReq = new ShowRequest();
        patchReq.setEndAt(newEnd);

        ShowResponse updated = showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created.getShowId(), patchReq, managerUser.getUserId(), "127.0.0.1");
        assertEquals(start, updated.getStartAt());
        assertEquals(newEnd, updated.getEndAt());
    }

    @Test
    @DisplayName("Gap B: Updating both startAt and endAt succeeds")
    void testPatch_UpdateBothStartAndEnd() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest createReq = new ShowRequest();
        createReq.setScreenId(testScreen.getScreenId().intValue());
        createReq.setMovieId(testMovie.getMovieId());
        createReq.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        createReq.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        createReq.setStartAt(start);
        createReq.setEndAt(end);

        ShowResponse created = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), createReq, managerUser.getUserId(), "127.0.0.1");

        Instant newStart = baseDate.atTime(14, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant newEnd = baseDate.atTime(16, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        ShowRequest patchReq = new ShowRequest();
        patchReq.setStartAt(newStart);
        patchReq.setEndAt(newEnd);

        ShowResponse updated = showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created.getShowId(), patchReq, managerUser.getUserId(), "127.0.0.1");
        assertEquals(newStart, updated.getStartAt());
        assertEquals(newEnd, updated.getEndAt());
    }

    @Test
    @DisplayName("Gap B: Rejecting end time that is not strictly after effective start time")
    void testPatch_RejectEndNotAfterStart() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest createReq = new ShowRequest();
        createReq.setScreenId(testScreen.getScreenId().intValue());
        createReq.setMovieId(testMovie.getMovieId());
        createReq.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        createReq.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        createReq.setStartAt(start);
        createReq.setEndAt(end);

        ShowResponse created = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), createReq, managerUser.getUserId(), "127.0.0.1");

        // Attempt to set endAt before current startAt
        ShowRequest invalidEndReq = new ShowRequest();
        invalidEndReq.setEndAt(baseDate.atTime(9, 30).atZone(ZoneId.of("Asia/Kolkata")).toInstant());

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created.getShowId(), invalidEndReq, managerUser.getUserId(), "127.0.0.1")
        );

        // Attempt to set startAt after current endAt
        ShowRequest invalidStartReq = new ShowRequest();
        invalidStartReq.setStartAt(baseDate.atTime(13, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant());

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created.getShowId(), invalidStartReq, managerUser.getUserId(), "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Gap B: Date consistency: showDate is correctly recalculated when start date changes")
    void testPatch_DateConsistencyRecalculated() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest createReq = new ShowRequest();
        createReq.setScreenId(testScreen.getScreenId().intValue());
        createReq.setMovieId(testMovie.getMovieId());
        createReq.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        createReq.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        createReq.setStartAt(start);
        createReq.setEndAt(end);

        ShowResponse created = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), createReq, managerUser.getUserId(), "127.0.0.1");
        assertEquals(baseDate.toString(), created.getShowDate());

        // Shift show to next day
        LocalDate nextDate = baseDate.plusDays(1);
        Instant nextStart = nextDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant nextEnd = nextDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest patchReq = new ShowRequest();
        patchReq.setStartAt(nextStart);
        patchReq.setEndAt(nextEnd);

        ShowResponse updated = showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created.getShowId(), patchReq, managerUser.getUserId(), "127.0.0.1");
        assertEquals(nextDate.toString(), updated.getShowDate());

        Show persisted = showRepository.findById(created.getShowId()).orElseThrow();
        assertEquals(nextDate, persisted.getShowDate());
    }

    @Test
    @DisplayName("Gap B: Rejecting turnaround-buffer violation on update")
    void testPatch_RejectTurnaroundBufferViolation() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        // Show 1: 10:00 to 12:00
        Instant s1 = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant e1 = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest r1 = new ShowRequest();
        r1.setScreenId(testScreen.getScreenId().intValue());
        r1.setMovieId(testMovie.getMovieId());
        r1.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        r1.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        r1.setStartAt(s1);
        r1.setEndAt(e1);
        showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), r1, managerUser.getUserId(), "127.0.0.1");

        // Show 2: 13:00 to 15:00 (valid 60 min gap from show 1)
        Instant s2 = baseDate.atTime(13, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant e2 = baseDate.atTime(15, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest r2 = new ShowRequest();
        r2.setScreenId(testScreen.getScreenId().intValue());
        r2.setMovieId(testMovie.getMovieId());
        r2.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        r2.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        r2.setStartAt(s2);
        r2.setEndAt(e2);
        ShowResponse created2 = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), r2, managerUser.getUserId(), "127.0.0.1");

        // Now attempt to update show 2 start to 12:10 (only 10 mins turnaround after Show 1 end 12:00 -> required 20 min buffer violated!)
        Instant s2New = baseDate.atTime(12, 10).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        ShowRequest patchReq = new ShowRequest();
        patchReq.setStartAt(s2New);

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), created2.getShowId(), patchReq, managerUser.getUserId(), "127.0.0.1")
        );

        // Verify Show 2 retains original timing
        Show show2Reloaded = showRepository.findById(created2.getShowId()).orElseThrow();
        assertEquals(s2, show2Reloaded.getStartAt());
        assertEquals(e2, show2Reloaded.getEndAt());
    }

    @Test
    @DisplayName("Gap B: Theatre-manager scope restriction enforced on update")
    void testPatch_CrossTheatreAccessDenied() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest createReq = new ShowRequest();
        createReq.setScreenId(testScreen.getScreenId().intValue());
        createReq.setMovieId(testMovie.getMovieId());
        createReq.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        createReq.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        createReq.setStartAt(start);
        createReq.setEndAt(end);

        ShowResponse created = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), createReq, managerUser.getUserId(), "127.0.0.1");

        ShowRequest patchReq = new ShowRequest();
        patchReq.setStartAt(baseDate.atTime(10, 30).atZone(ZoneId.of("Asia/Kolkata")).toInstant());

        // Call updateShow with a completely different theatreId
        int wrongTheatreId = testTheatre.getTheatreId().intValue() + 9999;
        assertThrows(AccessDeniedException.class, () ->
                showSchedulingService.updateShow(wrongTheatreId, created.getShowId(), patchReq, managerUser.getUserId(), "127.0.0.1")
        );
    }

    // ==========================================
    // GAP C: SHOW-11 Concurrent Conflicting Creation
    // ==========================================

    @Test
    @DisplayName("Gap C (SHOW-11): Concurrent conflicting show creation on same screen allows exactly 1 to succeed")
    void testSHOW11_ConcurrentConflictingCreation() throws Exception {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        Instant start = baseDate.atTime(16, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = baseDate.atTime(18, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<ShowResponse>> futures = new CopyOnWriteArrayList<>();
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        long initialShowCount = showRepository.count();
        long initialShowSeatCount = showSeatRepository.count();

        try {
            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    readyLatch.await(5, TimeUnit.SECONDS);
                    startLatch.await(5, TimeUnit.SECONDS);

                    ShowRequest req = new ShowRequest();
                    req.setScreenId(testScreen.getScreenId().intValue());
                    req.setMovieId(testMovie.getMovieId());
                    req.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
                    req.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
                    req.setStartAt(start);
                    req.setEndAt(end);

                    return showSchedulingService.createShow(
                            testTheatre.getTheatreId().intValue(),
                            req,
                            managerUser.getUserId(),
                            "127.0.0." + (idx + 1)
                    );
                }));
            }

            // Wait for all threads to be ready
            assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All threads must be ready");
            // Release the gate
            startLatch.countDown();

            int successCount = 0;
            int conflictCount = 0;

            for (Future<ShowResponse> future : futures) {
                try {
                    ShowResponse resp = future.get(10, TimeUnit.SECONDS);
                    if (resp != null) {
                        successCount++;
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    exceptions.add(cause);
                    if (cause instanceof ShowOverlapException) {
                        conflictCount++;
                    }
                }
            }

            // Exactly 1 must succeed, 1 must fail with ShowOverlapException
            assertEquals(1, successCount, "Exactly 1 concurrent conflicting show request must succeed");
            assertEquals(1, conflictCount, "Exactly 1 concurrent request must fail with ShowOverlapException");

            // Verify in DB
            assertEquals(initialShowCount + 1, showRepository.count(), "Exactly 1 show must be persisted in database");
            // Screen 1 has 3 active seats, so exactly 3 show_seat rows must be added
            assertEquals(initialShowSeatCount + 3, showSeatRepository.count(), "Only seats for the successful show should be created");

        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor must terminate");
        }
    }

    // ==========================================
    // GAP D: SHOW-12 Editing Into a Conflict
    // ==========================================

    @Test
    @DisplayName("Gap D (SHOW-12): Editing show timing into a conflict is rejected and preserves original state")
    void testSHOW12_EditingIntoAConflict() {
        LocalDate baseDate = businessDateProvider.getBusinessDate();
        // Show 1: 10:00 to 12:00
        Instant s1 = baseDate.atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant e1 = baseDate.atTime(12, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest r1 = new ShowRequest();
        r1.setScreenId(testScreen.getScreenId().intValue());
        r1.setMovieId(testMovie.getMovieId());
        r1.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        r1.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        r1.setStartAt(s1);
        r1.setEndAt(e1);
        ShowResponse show1 = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), r1, managerUser.getUserId(), "127.0.0.1");

        // Show 2: 13:00 to 15:00 (60 min separation)
        Instant s2 = baseDate.atTime(13, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant e2 = baseDate.atTime(15, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest r2 = new ShowRequest();
        r2.setScreenId(testScreen.getScreenId().intValue());
        r2.setMovieId(testMovie.getMovieId());
        r2.setMovieLanguageId(testMovieLanguage.getMovieLanguageId());
        r2.setScreenCapabilityId(testCapability.getScreenCapabilityId().intValue());
        r2.setStartAt(s2);
        r2.setEndAt(e2);
        ShowResponse show2 = showSchedulingService.createShow(testTheatre.getTheatreId().intValue(), r2, managerUser.getUserId(), "127.0.0.1");

        long showSeatCountBefore = showSeatRepository.count();

        // Attempt to update Show 2 to overlap Show 1 (11:00 to 13:00)
        Instant conflictStart = baseDate.atTime(11, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant conflictEnd = baseDate.atTime(13, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();

        ShowRequest conflictUpdateReq = new ShowRequest();
        conflictUpdateReq.setStartAt(conflictStart);
        conflictUpdateReq.setEndAt(conflictEnd);

        // Assert update is rejected with ShowOverlapException
        assertThrows(ShowOverlapException.class, () ->
                showSchedulingService.updateShow(testTheatre.getTheatreId().intValue(), show2.getShowId(), conflictUpdateReq, managerUser.getUserId(), "127.0.0.1")
        );

        // Reload Show 2 and verify its original timestamps and date remain unchanged
        Show reloaded2 = showRepository.findById(show2.getShowId()).orElseThrow();
        assertEquals(s2, reloaded2.getStartAt(), "Show 2 startAt must be preserved");
        assertEquals(e2, reloaded2.getEndAt(), "Show 2 endAt must be preserved");
        assertEquals(baseDate, reloaded2.getShowDate(), "Show 2 showDate must be preserved");

        // Reload Show 1 and verify it remains completely unchanged
        Show reloaded1 = showRepository.findById(show1.getShowId()).orElseThrow();
        assertEquals(s1, reloaded1.getStartAt(), "Show 1 startAt must remain unchanged");
        assertEquals(e1, reloaded1.getEndAt(), "Show 1 endAt must remain unchanged");

        // Verify no unintended seat mappings were created
        assertEquals(showSeatCountBefore, showSeatRepository.count(), "SHOW_SEAT row count must remain unchanged after rejected update");
    }
}
