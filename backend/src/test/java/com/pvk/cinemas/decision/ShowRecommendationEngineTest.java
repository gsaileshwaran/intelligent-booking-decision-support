package com.pvk.cinemas.decision;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.decision.dto.SeatScoreDTO;
import com.pvk.cinemas.decision.dto.ShowCandidateDTO;
import com.pvk.cinemas.decision.dto.ShowRecommendationRequest;
import com.pvk.cinemas.decision.dto.ShowRecommendationResponse;
import com.pvk.cinemas.decision.engine.SeatGroupPlanner;
import com.pvk.cinemas.decision.engine.SeatScoringEngine;
import com.pvk.cinemas.decision.engine.ShowRecommendationEngine;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ShowRecommendationEngineTest {

    private ShowRepository showRepository;
    private ShowSeatRepository showSeatRepository;
    private MovieRepository movieRepository;
    private MovieLanguageRepository movieLanguageRepository;
    private LanguageRepository languageRepository;
    private ScreenCapabilityRepository screenCapabilityRepository;
    private PresentationFormatRepository presentationFormatRepository;
    private ScreenRepository screenRepository;
    private SeatRepository seatRepository;
    private TheatreRepository theatreRepository;
    private CityRepository cityRepository;
    private SeatHoldService seatHoldService;
    private SeatScoringEngine seatScoringEngine;
    private com.pvk.cinemas.booking.service.SeatPricingService seatPricingService;
    private ShowRecommendationEngine engine;

    // Test Show A: Evening 18:30 IST, ₹250/ticket, 10 contiguous seats in Row C
    private Show showA;
    // Test Show B: Afternoon 14:00 IST, ₹150/ticket, 4 scattered seats across rows
    private Show showB;

    @BeforeEach
    void setUp() {
        showRepository = Mockito.mock(ShowRepository.class);
        showSeatRepository = Mockito.mock(ShowSeatRepository.class);
        movieRepository = Mockito.mock(MovieRepository.class);
        movieLanguageRepository = Mockito.mock(MovieLanguageRepository.class);
        languageRepository = Mockito.mock(LanguageRepository.class);
        screenCapabilityRepository = Mockito.mock(ScreenCapabilityRepository.class);
        presentationFormatRepository = Mockito.mock(PresentationFormatRepository.class);
        screenRepository = Mockito.mock(ScreenRepository.class);
        seatRepository = Mockito.mock(SeatRepository.class);
        theatreRepository = Mockito.mock(TheatreRepository.class);
        cityRepository = Mockito.mock(CityRepository.class);
        seatHoldService = Mockito.mock(SeatHoldService.class);
        seatScoringEngine = Mockito.mock(SeatScoringEngine.class);
        seatPricingService = Mockito.mock(com.pvk.cinemas.booking.service.SeatPricingService.class);
        SeatGroupPlanner seatGroupPlanner = new SeatGroupPlanner(seatScoringEngine, seatPricingService);

        engine = new ShowRecommendationEngine(
                showRepository, showSeatRepository, movieRepository, movieLanguageRepository,
                languageRepository, screenCapabilityRepository, presentationFormatRepository,
                screenRepository, seatRepository, theatreRepository, cityRepository,
                seatHoldService, seatScoringEngine, seatPricingService, seatGroupPlanner
        );

        // Movie
        Movie movie = new Movie();
        movie.setMovieId(680L);
        movie.setTitle("Interstellar");
        when(movieRepository.findById(680L)).thenReturn(Optional.of(movie));

        // Languages
        Language langEn = new Language(1L, "en", "English");
        Language langTa = new Language(2L, "ta", "Tamil");
        when(languageRepository.findByLanguageCode("en")).thenReturn(Optional.of(langEn));
        when(languageRepository.findByLanguageCode("ta")).thenReturn(Optional.of(langTa));

        MovieLanguage mlEn = new MovieLanguage(680L, 1L, "ORIGINAL");
        mlEn.setMovieLanguageId(10L);
        when(movieLanguageRepository.findByMovieId(680L)).thenReturn(List.of(mlEn));

        // City
        when(cityRepository.findById(1)).thenReturn(Optional.of(new City(1L, "Chennai", "Tamil Nadu", "IN")));

        // Theatre in Chennai
        Theatre theatreChennai = new Theatre();
        theatreChennai.setTheatreId(791L);
        theatreChennai.setCityId(1L);
        theatreChennai.setTheatreName("PVK INOX Chennai");
        when(theatreRepository.findById(791L)).thenReturn(Optional.of(theatreChennai));

        // Screen & Capability
        Screen screen1 = new Screen(791L, "SCR-1", "Screen 1 IMAX");
        screen1.setScreenId(501L);
        when(screenRepository.findById(501L)).thenReturn(Optional.of(screen1));

        ScreenCapability capImax = new ScreenCapability(501L, 3L, 1L);
        capImax.setScreenCapabilityId(901L);
        when(screenCapabilityRepository.findById(901L)).thenReturn(Optional.of(capImax));

        PresentationFormat imaxFormat = new PresentationFormat(3L, "IMAX", "IMAX Experience", null);
        when(presentationFormatRepository.findById(3L)).thenReturn(Optional.of(imaxFormat));

        // Shows (Date: 2026-09-20)
        // 18:30 IST is 13:00 UTC
        Instant showATime = Instant.parse("2026-09-20T13:00:00Z");
        showA = new Show();
        showA.setShowId(3001L);
        showA.setMovieLanguageId(10L);
        showA.setScreenCapabilityId(901L);
        showA.setStartAt(showATime);
        showA.setEndAt(showATime.plus(180, ChronoUnit.MINUTES));
        showA.setShowStatus("SCHEDULED");

        // 14:00 IST is 08:30 UTC
        Instant showBTime = Instant.parse("2026-09-20T08:30:00Z");
        showB = new Show();
        showB.setShowId(3002L);
        showB.setMovieLanguageId(10L);
        showB.setScreenCapabilityId(901L);
        showB.setStartAt(showBTime);
        showB.setEndAt(showBTime.plus(180, ChronoUnit.MINUTES));
        showB.setShowStatus("SCHEDULED");

        when(showRepository.findByMovieLanguageIdIn(anyList())).thenReturn(List.of(showA, showB));

        // Show A Seats: 10 seats in Row C (C1 to C10), consecutive IDs 1-10
        List<ShowSeat> showASeats = new ArrayList<>();
        List<Seat> seatEntitiesA = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            showASeats.add(new ShowSeat(3001L, (long) i, "AVAILABLE"));
            Seat s = new Seat(501L, 1L, "C", String.valueOf(i));
            s.setSeatId((long) i);
            seatEntitiesA.add(s);
        }
        when(showSeatRepository.findByIdShowId(3001L)).thenReturn(showASeats);

        // Show B Seats: 4 scattered seats in different rows (A1, B2, D5, E8)
        List<ShowSeat> showBSeats = List.of(
                new ShowSeat(3002L, 101L, "AVAILABLE"),
                new ShowSeat(3002L, 102L, "AVAILABLE"),
                new ShowSeat(3002L, 103L, "AVAILABLE"),
                new ShowSeat(3002L, 104L, "AVAILABLE")
        );
        List<Seat> seatEntitiesB = List.of(
                new Seat(501L, 1L, "A", "1"),
                new Seat(501L, 1L, "B", "2"),
                new Seat(501L, 1L, "D", "5"),
                new Seat(501L, 1L, "E", "8")
        );
        seatEntitiesB.get(0).setSeatId(101L);
        seatEntitiesB.get(1).setSeatId(102L);
        seatEntitiesB.get(2).setSeatId(103L);
        seatEntitiesB.get(3).setSeatId(104L);
        when(showSeatRepository.findByIdShowId(3002L)).thenReturn(showBSeats);

        when(seatRepository.findAllById(anyCollection())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Long> ids = new ArrayList<>(inv.getArgument(0));
            if (ids.contains(1L)) return seatEntitiesA;
            return seatEntitiesB;
        });

        // Prices: Show A = ₹250/ticket, Show B = ₹150/ticket
        when(seatHoldService.determineSeatPrice(argThat(id -> id != null && id < 100))).thenReturn(BigDecimal.valueOf(250.00));
        when(seatHoldService.determineSeatPrice(argThat(id -> id != null && id >= 100))).thenReturn(BigDecimal.valueOf(150.00));
        when(seatPricingService.determineSeatPrice(eq(3001L), anyLong())).thenReturn(BigDecimal.valueOf(250.00));
        when(seatPricingService.determineSeatPrice(eq(3002L), anyLong())).thenReturn(BigDecimal.valueOf(150.00));
        when(seatPricingService.determineSeatPrice(any(), argThat(id -> id != null && id < 100))).thenReturn(BigDecimal.valueOf(250.00));
        when(seatPricingService.determineSeatPrice(any(), argThat(id -> id != null && id >= 100))).thenReturn(BigDecimal.valueOf(150.00));

        // Seat quality scoring
        SeatScoreDTO highQuality = new SeatScoreDTO();
        highQuality.setScore(95);
        when(seatScoringEngine.scoreSeat(argThat(s -> s != null && "C".equals(s.getRowLabel())))).thenReturn(highQuality);

        SeatScoreDTO standardQuality = new SeatScoreDTO();
        standardQuality.setScore(60);
        when(seatScoringEngine.scoreSeat(argThat(s -> s != null && !"C".equals(s.getRowLabel())))).thenReturn(standardQuality);
    }

    @Test
    @DisplayName("Language hard constraint: excludes shows when requested language does not match")
    void testLanguageHardConstraint_excludesWrongLanguage() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setLanguageCode("ta"); // Tamil requested, but movie only has English scheduled
        req.setPartySize(2);

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertTrue(res.getCandidates().isEmpty(), "Shows for wrong language must be excluded");
    }

    @Test
    @DisplayName("Format hard constraint: excludes shows when format does not match")
    void testFormatHardConstraint_excludesWrongFormat() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setFormatPreference("4DX"); // 4DX requested, but shows are IMAX
        req.setPartySize(2);

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertTrue(res.getCandidates().isEmpty(), "Non-matching format must be hard excluded");
    }

    @Test
    @DisplayName("Date range hard constraint: excludes shows outside selected dates")
    void testDateRangeHardConstraint_excludesOutOfRangeShows() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setDateFrom(LocalDate.of(2026, 9, 25)); // Shows are on 2026-09-20
        req.setDateTo(LocalDate.of(2026, 9, 30));
        req.setPartySize(2);

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertTrue(res.getCandidates().isEmpty(), "Shows outside date range must be excluded");
    }

    @Test
    @DisplayName("Party size hard constraint: excludes shows without enough available seats")
    void testPartySizeHardConstraint_excludesInsufficientSeats() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(15); // Show A has 10, Show B has 4

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertTrue(res.getCandidates().isEmpty(), "Shows with insufficient seats must be excluded");
    }

    @Test
    @DisplayName("Party budget hard constraint: excludes shows where partySize × price > budgetMaxTotal")
    void testBudgetHardConstraint_excludesOverPartyBudget() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(4);
        // Budget ₹800: Show A costs 4 × ₹250 = ₹1000 (EXCLUDED)
        // Show B costs 4 × ₹150 = ₹600 (VALID)
        req.setBudgetMaxTotal(BigDecimal.valueOf(800.00));

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertEquals(1, res.getCandidates().size(), "Only within-budget show must qualify");
        assertEquals(3002L, res.getCandidates().get(0).getShowId(), "Show B (₹600 total) must qualify");
    }

    @Test
    @DisplayName("No budget specified: price remains soft ranking factor (both qualify)")
    void testNoBudget_priceRemainsSoftFactor() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(4);
        req.setBudgetMaxTotal(null);

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertEquals(2, res.getCandidates().size(), "Both shows must qualify when no budget limit");
    }

    @Test
    @DisplayName("No preferred time: no artificial bias toward any time slot")
    void testNoPreferredTime_noArtificialBias() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(2);
        req.setTimePreference(null); // No preferred time

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertFalse(res.getCandidates().isEmpty());
        // Neither candidate should have a "time trade-off" penalty when user didn't give a preferred time
        for (ShowCandidateDTO candidate : res.getCandidates()) {
            boolean hasTimePenalty = candidate.getTradeOffs().stream().anyMatch(t -> t.contains("from your preferred time"));
            assertFalse(hasTimePenalty, "No time penalty should exist when user provided no preferred time");
        }
    }

    @Test
    @DisplayName("BEST_PRICE priority: favors cheaper Show B (₹150/ticket) over Show A (₹250/ticket)")
    void testPriorityMode_bestPriceFavorsCheaperShow() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(4);
        req.setPriority("BEST_PRICE");

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertEquals(2, res.getCandidates().size());
        assertEquals(3002L, res.getCandidates().get(0).getShowId(),
                "BEST_PRICE must rank cheaper Show B (₹600 total) ahead of Show A (₹1000 total)");
    }

    @Test
    @DisplayName("BEST_SEATS priority: favors Show A with 10 adjacent seats in Row C over scattered Show B")
    void testPriorityMode_bestSeatsFavorsContiguousSeats() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(4);
        req.setPriority("BEST_SEATS");

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertEquals(2, res.getCandidates().size());
        assertEquals(3001L, res.getCandidates().get(0).getShowId(),
                "BEST_SEATS must rank Show A (contiguous Row C seats) ahead of Show B (scattered seats)");
        assertTrue(res.getCandidates().get(0).getReasons().stream().anyMatch(r -> r.contains("adjacent seats together in Row C")));
    }

    @Test
    @DisplayName("BEST_TIME priority: favors show closest to preferred slot")
    void testPriorityMode_bestTimeFavorsCloserShow() {
        // Target: 18:30 (Evening). Show A is 18:30 IST (0h diff), Show B is 14:00 IST (4.5h diff)
        ShowRecommendationRequest reqEvening = new ShowRecommendationRequest();
        reqEvening.setCityId(1L);
        reqEvening.setPartySize(4);
        reqEvening.setTimePreference("18:30");
        reqEvening.setPriority("BEST_TIME");

        ShowRecommendationResponse resEvening = engine.recommendShows(680L, reqEvening);
        assertEquals(3001L, resEvening.getCandidates().get(0).getShowId(),
                "BEST_TIME with 18:30 target must favor 18:30 Show A");

        // Target: 14:00 (Afternoon). Show B is 14:00 IST (0h diff), Show A is 18:30 IST (4.5h diff)
        ShowRecommendationRequest reqAfternoon = new ShowRecommendationRequest();
        reqAfternoon.setCityId(1L);
        reqAfternoon.setPartySize(4);
        reqAfternoon.setTimePreference("14:00");
        reqAfternoon.setPriority("BEST_TIME");

        ShowRecommendationResponse resAfternoon = engine.recommendShows(680L, reqAfternoon);
        assertEquals(3002L, resAfternoon.getCandidates().get(0).getShowId(),
                "BEST_TIME with 14:00 target must favor 14:00 Show B");
    }

    @Test
    @DisplayName("BALANCED priority: seat quality, price, and timing all contribute")
    void testBalancedMode_weightedContribution() {
        ShowRecommendationRequest req = new ShowRecommendationRequest();
        req.setCityId(1L);
        req.setPartySize(4);
        req.setPriority("BALANCED");
        req.setTimePreference("EVENING"); // 18:00 target

        ShowRecommendationResponse res = engine.recommendShows(680L, req);
        assertNotNull(res);
        assertEquals(2, res.getCandidates().size());
        // Both candidates have computed match scores derived from the 40/30/30 formula
        assertTrue(res.getCandidates().get(0).getMatchScore() > 0);
        assertTrue(res.getCandidates().get(1).getMatchScore() > 0);
        assertNotNull(res.getConflictAnalysis());
        assertTrue(res.getConflictAnalysis().contains("Balanced ranking"));
    }
}
