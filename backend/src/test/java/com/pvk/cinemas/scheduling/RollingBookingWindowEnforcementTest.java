package com.pvk.cinemas.scheduling;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.booking.model.Booking;
import com.pvk.cinemas.booking.repository.BookingRepository;
import com.pvk.cinemas.booking.repository.BookingSeatRepository;
import com.pvk.cinemas.booking.repository.PaymentRepository;
import com.pvk.cinemas.booking.repository.SeatHoldRepository;
import com.pvk.cinemas.booking.service.BookingService;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.ShowSchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RollingBookingWindowEnforcementTest {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate ANCHOR_DATE = LocalDate.of(2026, 10, 11);

    private BusinessDateProvider businessDateProvider;

    @Mock private ShowRepository showRepository;
    @Mock private ShowSeatRepository showSeatRepository;
    @Mock private ScreenRepository screenRepository;
    @Mock private ScreenCapabilityRepository screenCapabilityRepository;
    @Mock private TheatreRepository theatreRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private MovieLanguageRepository movieLanguageRepository;
    @Mock private SeatHoldRepository seatHoldRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingSeatRepository bookingSeatRepository;
    @Mock private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        businessDateProvider = new BusinessDateProvider("auto", 7);
        businessDateProvider.setClock(Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), IST_ZONE));
    }

    @Test
    @DisplayName("Enforce 7-day rolling window bounds: Acceptance of Day 1 and Day 7, rejection of Yesterday and Day 8")
    void testBookingWindowBoundaries() {
        assertEquals(ANCHOR_DATE, businessDateProvider.getBusinessDate());
        assertEquals(ANCHOR_DATE, businessDateProvider.getBookingWindowStart());
        assertEquals(LocalDate.of(2026, 10, 17), businessDateProvider.getBookingWindowEnd());

        // First permitted date (2026-10-11) is accepted
        assertDoesNotThrow(() -> businessDateProvider.validateBookingWindow(LocalDate.of(2026, 10, 11)));
        assertTrue(businessDateProvider.isBookable(LocalDate.of(2026, 10, 11)));

        // Seventh permitted date (2026-10-17) is accepted
        assertDoesNotThrow(() -> businessDateProvider.validateBookingWindow(LocalDate.of(2026, 10, 17)));
        assertTrue(businessDateProvider.isBookable(LocalDate.of(2026, 10, 17)));

        // Yesterday (2026-10-10) is rejected
        assertFalse(businessDateProvider.isBookable(LocalDate.of(2026, 10, 10)));
        BadRequestException exPast = assertThrows(BadRequestException.class,
                () -> businessDateProvider.validateBookingWindow(LocalDate.of(2026, 10, 10)));
        assertEquals("SHOW_DATE_IN_PAST", exPast.getErrorCode());

        // Eighth calendar day (2026-10-18) is rejected
        assertFalse(businessDateProvider.isBookable(LocalDate.of(2026, 10, 18)));
        BadRequestException exFuture = assertThrows(BadRequestException.class,
                () -> businessDateProvider.validateBookingWindow(LocalDate.of(2026, 10, 18)));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", exFuture.getErrorCode());
    }

    @Test
    @DisplayName("Window advances automatically at runtime when business date advances without restart")
    void testWindowAdvancementWithoutRestart() {
        // Oct 11: Day 1 = Oct 11, Day 7 = Oct 17
        assertTrue(businessDateProvider.isBookable(LocalDate.of(2026, 10, 11)));
        assertFalse(businessDateProvider.isBookable(LocalDate.of(2026, 10, 18)));

        // Advance Clock to Oct 12 without restarting application
        businessDateProvider.setClock(Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), IST_ZONE));

        assertEquals(LocalDate.of(2026, 10, 12), businessDateProvider.getBusinessDate());
        assertEquals(LocalDate.of(2026, 10, 12), businessDateProvider.getBookingWindowStart());
        assertEquals(LocalDate.of(2026, 10, 18), businessDateProvider.getBookingWindowEnd());

        // Oct 11 is now past and rejected
        assertFalse(businessDateProvider.isBookable(LocalDate.of(2026, 10, 11)));
        // Oct 18 is now valid and accepted
        assertTrue(businessDateProvider.isBookable(LocalDate.of(2026, 10, 18)));
        assertDoesNotThrow(() -> businessDateProvider.validateBookingWindow(LocalDate.of(2026, 10, 18)));
    }

    @Test
    @DisplayName("Show discovery API: Excludes out-of-window shows and validates date parameter")
    void testDiscoveryExcludesOutOfWindowShows() {
        ShowSchedulingService service = new ShowSchedulingService(
                showRepository, showSeatRepository, screenRepository, screenCapabilityRepository,
                movieRepository, movieLanguageRepository, null, theatreRepository,
                null, null, null, null,
                null, null, businessDateProvider
        );

        Long movieId = 101L;
        Movie movie = new Movie();
        movie.setMovieId(movieId);
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        // Attempting to query shows on yesterday throws SHOW_DATE_IN_PAST
        BadRequestException exPast = assertThrows(BadRequestException.class,
                () -> service.getShowsForMovie(movieId, null, null, LocalDate.of(2026, 10, 10), null, null, null));
        assertEquals("SHOW_DATE_IN_PAST", exPast.getErrorCode());

        // Attempting to query shows on day 8 throws SHOW_DATE_OUT_OF_WINDOW
        BadRequestException exFuture = assertThrows(BadRequestException.class,
                () -> service.getShowsForMovie(movieId, null, null, LocalDate.of(2026, 10, 18), null, null, null));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", exFuture.getErrorCode());

        // When date is null, service must filter candidate shows to only those within the 7-day window
        MovieLanguage ml = new MovieLanguage();
        ml.setMovieLanguageId(1L);
        ml.setMovieId(movieId);
        when(movieLanguageRepository.findByMovieId(movieId)).thenReturn(List.of(ml));

        Show pastShow = new Show();
        pastShow.setShowId(1L);
        pastShow.setMovieLanguageId(1L);
        pastShow.setShowDate(LocalDate.of(2026, 10, 9)); // Past

        Show inWindowShow1 = new Show();
        inWindowShow1.setShowId(2L);
        inWindowShow1.setMovieLanguageId(1L);
        inWindowShow1.setShowDate(LocalDate.of(2026, 10, 11)); // Day 1

        Show inWindowShow7 = new Show();
        inWindowShow7.setShowId(3L);
        inWindowShow7.setMovieLanguageId(1L);
        inWindowShow7.setShowDate(LocalDate.of(2026, 10, 17)); // Day 7

        Show futureAdvanceShow = new Show();
        futureAdvanceShow.setShowId(4L);
        futureAdvanceShow.setMovieLanguageId(1L);
        futureAdvanceShow.setShowDate(LocalDate.of(2026, 12, 25)); // Advance show

        when(showRepository.findByMovieLanguageIdIn(anyList()))
                .thenReturn(List.of(pastShow, inWindowShow1, inWindowShow7, futureAdvanceShow));

        List<ShowResponse> responses = service.getShowsForMovie(movieId, null, null, null, null, null, null);
        assertEquals(2, responses.size(), "Only the 2 in-window shows should be returned");
        assertTrue(responses.stream().anyMatch(r -> r.getShowId().equals(2L)));
        assertTrue(responses.stream().anyMatch(r -> r.getShowId().equals(3L)));
    }

    @Test
    @DisplayName("Direct show details request: Rejected if show date is outside rolling window")
    void testDirectShowDetailsRejection() {
        ShowSchedulingService service = new ShowSchedulingService(
                showRepository, null, null, screenCapabilityRepository,
                movieRepository, movieLanguageRepository, null, null,
                null, null, null, null,
                null, null, businessDateProvider
        );

        Show outOfWindowShow = new Show();
        outOfWindowShow.setShowId(999L);
        outOfWindowShow.setShowDate(LocalDate.of(2026, 10, 18)); // Day 8

        when(showRepository.findById(999L)).thenReturn(Optional.of(outOfWindowShow));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.getShowById(999L));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", ex.getErrorCode());
    }

    @Test
    @DisplayName("Direct seat map request: Customer rejected for out-of-window show")
    void testDirectSeatMapRequestValidation() {
        SeatAvailabilityService seatService = new SeatAvailabilityService(
                showRepository, showSeatRepository, null, null,
                theatreRepository, null, null, null, null,
                movieRepository, movieLanguageRepository, null, null,
                businessDateProvider
        );

        Show outOfWindowShow = new Show();
        outOfWindowShow.setShowId(888L);
        outOfWindowShow.setShowDate(LocalDate.of(2026, 10, 19)); // Out of window

        when(showRepository.findById(888L)).thenReturn(Optional.of(outOfWindowShow));

        // Customer request throws SHOW_DATE_OUT_OF_WINDOW
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> seatService.getShowSeatAvailability(888L));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", ex.getErrorCode());
    }

    @Test
    @DisplayName("Direct seat hold request: Rejected for out-of-window show")
    void testDirectSeatHoldRejection() {
        SeatHoldService holdService = new SeatHoldService(
                seatHoldRepository, showSeatRepository, showRepository,
                null, null, null,
                businessDateProvider
        );

        Show outOfWindowShow = new Show();
        outOfWindowShow.setShowId(777L);
        outOfWindowShow.setShowDate(LocalDate.of(2026, 10, 20));

        when(showRepository.findById(777L)).thenReturn(Optional.of(outOfWindowShow));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> holdService.holdSeats(777L, 1L, List.of(10L, 11L)));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", ex.getErrorCode());
    }

    @Test
    @DisplayName("Historical booking retrieval: Preserved and accessible without date rejection")
    void testHistoricalBookingRetrievalPreserved() {
        BookingService bookingService = new BookingService(
                bookingRepository, bookingSeatRepository, paymentRepository, seatHoldRepository,
                showSeatRepository, showRepository, movieLanguageRepository, movieRepository,
                null, null, null, screenRepository,
                theatreRepository, null, null, null,
                null, businessDateProvider
        );

        Booking historicalBooking = new Booking();
        historicalBooking.setBookingId(1L);
        historicalBooking.setBookingReference("PVK-HIST-001");
        historicalBooking.setUserId(42L);
        historicalBooking.setShowId(10L);
        historicalBooking.setBookingStatus("CONFIRMED");

        when(bookingRepository.findByBookingReference("PVK-HIST-001"))
                .thenReturn(Optional.of(historicalBooking));
        when(bookingSeatRepository.findByBookingId(1L))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.findByBookingId(1L))
                .thenReturn(Optional.empty());

        // Historical lookup must NOT throw date validation exceptions
        assertDoesNotThrow(() -> bookingService.getBookingByReference("PVK-HIST-001", 42L, false));
    }
}
