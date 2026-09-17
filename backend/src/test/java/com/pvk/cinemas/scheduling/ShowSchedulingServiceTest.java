package com.pvk.cinemas.scheduling;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.InvalidCapabilityException;
import com.pvk.cinemas.common.exceptions.InvalidMovieLanguageException;
import com.pvk.cinemas.common.exceptions.ShowOverlapException;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowSchedulingServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ScreenCapabilityRepository screenCapabilityRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieLanguageRepository movieLanguageRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private TheatreRepository theatreRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PresentationFormatRepository presentationFormatRepository;

    @Mock
    private SeatPricingService seatPricingService;

    @InjectMocks
    private ShowSchedulingService showSchedulingService;

    private final Integer theatreId = 1;
    private final Long movieId = 100L;
    private final Long movieLanguageId = 200L;
    private final Long screenId = 10L;
    private final Long screenCapabilityId = 50L;
    private final Instant startAt = Instant.now().plus(2, ChronoUnit.HOURS);
    private final Instant endAt = startAt.plus(150, ChronoUnit.MINUTES);

    private ShowRequest validRequest;
    private Screen screen;
    private Movie movie;
    private MovieLanguage movieLanguage;
    private ScreenCapability capability;

    @BeforeEach
    void setUp() {
        validRequest = new ShowRequest();
        validRequest.setMovieId(movieId);
        validRequest.setMovieLanguageId(movieLanguageId);
        validRequest.setScreenId(screenId.intValue());
        validRequest.setScreenCapabilityId(screenCapabilityId.intValue());
        validRequest.setStartAt(startAt);
        validRequest.setEndAt(endAt);
        validRequest.setShowStatus("SCHEDULED");

        screen = new Screen();
        screen.setScreenId(screenId);
        screen.setScreenName("Screen 1");
        screen.setIsActive(true);
        screen.setTheatreId(theatreId.longValue());

        movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle("Interstellar");
        movie.setRuntimeMinutes(169);

        movieLanguage = new MovieLanguage(movieId, 1L, "ORIGINAL");
        movieLanguage.setMovieLanguageId(movieLanguageId);

        capability = new ScreenCapability(screenId, 1L, 1L);
        capability.setScreenCapabilityId(screenCapabilityId);
    }

    @Test
    @DisplayName("BR-001: Reject show if start_at is in the past or after end_at")
    void testBR001_TimingValidation() {
        ShowRequest req = new ShowRequest();
        req.setMovieId(movieId);
        req.setMovieLanguageId(movieLanguageId);
        req.setScreenId(screenId.intValue());
        req.setScreenCapabilityId(screenCapabilityId.intValue());
        req.setStartAt(Instant.now().plus(2, ChronoUnit.HOURS));
        req.setEndAt(Instant.now().plus(1, ChronoUnit.HOURS)); // end before start

        assertThrows(BadRequestException.class, () ->
                showSchedulingService.createShow(theatreId, req, 1L, "127.0.0.1"));
    }

    @Test
    @DisplayName("BR-002: Reject show if movie language does not belong to selected movie")
    void testBR002_MovieLanguageIntegrity() {
        when(screenRepository.findByIdWithPessimisticLock(screenId)).thenReturn(Optional.of(screen));

        // MovieLanguage belonging to different movie
        MovieLanguage wrongMovieLang = new MovieLanguage(999L, 1L, "DUBBED");
        wrongMovieLang.setMovieLanguageId(movieLanguageId);
        when(movieLanguageRepository.findById(movieLanguageId)).thenReturn(Optional.of(wrongMovieLang));

        assertThrows(InvalidMovieLanguageException.class, () ->
                showSchedulingService.createShow(theatreId, validRequest, 1L, "127.0.0.1"));
    }

    @Test
    @DisplayName("BR-003: Reject show if screen capability does not belong to selected screen")
    void testBR003_ScreenCapabilityIntegrity() {
        when(screenRepository.findByIdWithPessimisticLock(screenId)).thenReturn(Optional.of(screen));
        when(movieLanguageRepository.findById(movieLanguageId)).thenReturn(Optional.of(movieLanguage));

        // Capability belonging to different screen
        ScreenCapability wrongCapability = new ScreenCapability(999L, 1L, 1L);
        wrongCapability.setScreenCapabilityId(screenCapabilityId);
        when(screenCapabilityRepository.findById(screenCapabilityId)).thenReturn(Optional.of(wrongCapability));

        assertThrows(InvalidCapabilityException.class, () ->
                showSchedulingService.createShow(theatreId, validRequest, 1L, "127.0.0.1"));
    }

    @Test
    @DisplayName("BR-004: Reject show if screen has overlapping active show (Pessimistic check)")
    void testBR004_ShowOverlapPrevention() {
        when(screenRepository.findByIdWithPessimisticLock(screenId)).thenReturn(Optional.of(screen));
        when(movieLanguageRepository.findById(movieLanguageId)).thenReturn(Optional.of(movieLanguage));
        when(screenCapabilityRepository.findById(screenCapabilityId)).thenReturn(Optional.of(capability));

        // Overlap found
        when(showRepository.countOverlappingShows(eq(screenId), eq(startAt), eq(endAt)))
                .thenReturn(1L);

        assertThrows(ShowOverlapException.class, () ->
                showSchedulingService.createShow(theatreId, validRequest, 1L, "127.0.0.1"));
    }

    @Test
    @DisplayName("CRITICAL SHOW TEST: Synchronous Show creation + SHOW_SEAT initialization")
    void testCriticalShowCreationAndSeatInitialization() {
        when(screenRepository.findByIdWithPessimisticLock(screenId)).thenReturn(Optional.of(screen));
        when(movieLanguageRepository.findById(movieLanguageId)).thenReturn(Optional.of(movieLanguage));
        when(screenCapabilityRepository.findById(screenCapabilityId)).thenReturn(Optional.of(capability));
        when(showRepository.countOverlappingShows(eq(screenId), eq(startAt), eq(endAt)))
                .thenReturn(0L);

        // Active physical seats on screen
        Seat seat1 = new Seat(screenId, 1L, "A", "1");
        seat1.setSeatId(501L);
        Seat seat2 = new Seat(screenId, 1L, "A", "2");
        seat2.setSeatId(502L);
        when(seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(screenId))
                .thenReturn(List.of(seat1, seat2));

        Show savedShow = new Show();
        savedShow.setShowId(777L);
        savedShow.setMovieLanguageId(movieLanguageId);
        savedShow.setScreenCapabilityId(screenCapabilityId);
        savedShow.setStartAt(startAt);
        savedShow.setEndAt(endAt);
        savedShow.setShowStatus("SCHEDULED");

        when(showRepository.save(any(Show.class))).thenReturn(savedShow);

        ShowResponse response = showSchedulingService.createShow(theatreId, validRequest, 1L, "127.0.0.1");

        assertNotNull(response);
        assertEquals(777L, response.getShowId());

        // Verify SHOW saved
        verify(showRepository).save(any(Show.class));
        // Verify SHOW_SEAT initialized for all active seats
        verify(showSeatRepository).saveAll(argThat(seats -> {
            List<ShowSeat> list = (List<ShowSeat>) seats;
            return list.size() == 2 &&
                    list.stream().allMatch(s -> "AVAILABLE".equals(s.getAvailabilityStatus()) && s.getId().getShowId() == 777L);
        }));
        // Verify audit logged
        verify(auditLogService).logAction(eq(1L), eq("SHOW_CREATE"), eq("SHOW"), eq("777"), anyString(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("CRITICAL ROLLBACK TEST: When SHOW_SEAT initialization fails, exception is thrown")
    void testCriticalShowRollbackOnSeatInitFailure() {
        when(screenRepository.findByIdWithPessimisticLock(screenId)).thenReturn(Optional.of(screen));
        when(movieLanguageRepository.findById(movieLanguageId)).thenReturn(Optional.of(movieLanguage));
        when(screenCapabilityRepository.findById(screenCapabilityId)).thenReturn(Optional.of(capability));
        when(showRepository.countOverlappingShows(eq(screenId), eq(startAt), eq(endAt)))
                .thenReturn(0L);

        Seat seat1 = new Seat(screenId, 1L, "A", "1");
        seat1.setSeatId(501L);
        when(seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(screenId))
                .thenReturn(List.of(seat1));

        Show savedShow = new Show();
        savedShow.setShowId(777L);
        when(showRepository.save(any(Show.class))).thenReturn(savedShow);

        // Simulate database failure during SHOW_SEAT insertion
        doThrow(new RuntimeException("Simulated database failure during bulk insert"))
                .when(showSeatRepository).saveAll(anyList());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                showSchedulingService.createShow(theatreId, validRequest, 1L, "127.0.0.1"));

        assertEquals("Simulated database failure during bulk insert", ex.getMessage());
        // Since method is @Transactional, Spring rollbacks the transaction, preventing SHOW commit
    }
}
