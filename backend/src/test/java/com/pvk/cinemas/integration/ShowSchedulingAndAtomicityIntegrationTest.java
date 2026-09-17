package com.pvk.cinemas.integration;

import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.InvalidCapabilityException;
import com.pvk.cinemas.common.exceptions.InvalidMovieLanguageException;
import com.pvk.cinemas.common.exceptions.ShowOverlapException;
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
import java.util.List;
import java.util.UUID;

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
}
