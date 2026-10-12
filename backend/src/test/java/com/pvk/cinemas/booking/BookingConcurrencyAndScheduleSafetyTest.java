package com.pvk.cinemas.booking;

import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
import com.pvk.cinemas.booking.dto.BookingResponse;
import com.pvk.cinemas.booking.dto.CheckoutRequest;
import com.pvk.cinemas.booking.dto.SeatHoldResponse;
import com.pvk.cinemas.booking.model.Booking;
import com.pvk.cinemas.booking.model.SeatHold;
import com.pvk.cinemas.booking.repository.BookingRepository;
import com.pvk.cinemas.booking.repository.SeatHoldRepository;
import com.pvk.cinemas.booking.service.BookingService;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.YearLongShowGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingConcurrencyAndScheduleSafetyTest {

    @Autowired private BookingService bookingService;
    @Autowired private SeatHoldService seatHoldService;
    @Autowired private SeatAvailabilityService seatAvailabilityService;
    @Autowired private YearLongShowGenerationService yearLongShowGenerationService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private SeatHoldRepository seatHoldRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private BusinessDateProvider businessDateProvider;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Priority 1: Repeated checkout with the same hold token is idempotent and returns the original booking")
    void testRepeatedCheckoutReturnsOriginalBooking() {
        LocalDate busDate = businessDateProvider.getBusinessDate();
        List<Show> shows = showRepository.findByShowDate(busDate);
        assertFalse(shows.isEmpty(), "Expected shows for business date");
        Show show = shows.get(0);

        List<ShowSeat> availableSeats = showSeatRepository.findByIdShowId(show.getShowId()).stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .toList();
        assertFalse(availableSeats.isEmpty(), "Expected available seats");
        Long seatId = availableSeats.get(0).getId().getSeatId();

        // 1. Hold seat for user 1
        Long userId = 1L;
        SeatHoldResponse holdResp = seatHoldService.holdSeats(show.getShowId(), userId, List.of(seatId));
        assertNotNull(holdResp.getHoldToken());

        // 2. First checkout
        CheckoutRequest req = new CheckoutRequest();
        req.setHoldToken(holdResp.getHoldToken());
        req.setPaymentMethod("TEST_CARD");
        req.setSimulateOutcome("SUCCESS");

        BookingResponse firstResp = bookingService.checkout(req, userId);
        assertNotNull(firstResp);
        assertNotNull(firstResp.getBookingReference());
        assertEquals("CONFIRMED", firstResp.getBookingStatus());

        // 3. Repeated checkout with same hold token
        BookingResponse secondResp = bookingService.checkout(req, userId);
        assertNotNull(secondResp);
        assertEquals(firstResp.getBookingId(), secondResp.getBookingId(), "Repeated checkout must return original booking ID");
        assertEquals(firstResp.getBookingReference(), secondResp.getBookingReference(), "Repeated checkout must return identical booking reference");

        // Verify database contains exactly one booking for this hold token
        Optional<Booking> persisted = bookingRepository.findByHoldToken(holdResp.getHoldToken());
        assertTrue(persisted.isPresent());
        assertEquals(firstResp.getBookingReference(), persisted.get().getBookingReference());
    }

    @Test
    @DisplayName("Priority 1: Concurrent checkout attempts for the same hold token serialize safely without duplicate bookings")
    void testConcurrentCheckoutSerialization() throws Exception {
        LocalDate busDate = businessDateProvider.getBusinessDate();
        List<Show> shows = showRepository.findByShowDate(busDate);
        Show show = shows.get(0);

        List<ShowSeat> availableSeats = showSeatRepository.findByIdShowId(show.getShowId()).stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .toList();
        assertTrue(availableSeats.size() >= 2);
        Long seatId = availableSeats.get(1).getId().getSeatId();

        Long userId = 1L;
        SeatHoldResponse holdResp = seatHoldService.holdSeats(show.getShowId(), userId, List.of(seatId));

        CheckoutRequest req = new CheckoutRequest();
        req.setHoldToken(holdResp.getHoldToken());
        req.setPaymentMethod("TEST_CARD");
        req.setSimulateOutcome("SUCCESS");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        List<Future<BookingResponse>> futures = new ArrayList<>();
        AtomicInteger conflicts = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await();
                try {
                    return bookingService.checkout(req, userId);
                } catch (ConflictException ce) {
                    conflicts.incrementAndGet();
                    return null;
                }
            }));
        }

        List<BookingResponse> responses = new ArrayList<>();
        for (Future<BookingResponse> f : futures) {
            BookingResponse r = f.get();
            if (r != null) responses.add(r);
        }
        executor.shutdown();

        // At least one succeeded, and any successful responses point to the EXACT same booking reference
        assertFalse(responses.isEmpty());
        String expectedRef = responses.get(0).getBookingReference();
        for (BookingResponse r : responses) {
            assertEquals(expectedRef, r.getBookingReference(), "All returned bookings must be the exact same booking reference");
        }

        // Database must have strictly 1 booking row for this hold token
        long count = bookingRepository.findAll().stream()
                .filter(b -> holdResp.getHoldToken().equals(b.getHoldToken()))
                .count();
        assertEquals(1, count, "Database must strictly contain exactly 1 booking for this hold token");
    }

    @Test
    @DisplayName("Priority 3: Concurrent attempts to hold the same show-seat result in exactly one winner and one conflict")
    void testConcurrentSeatHoldExclusivity() throws Exception {
        LocalDate busDate = businessDateProvider.getBusinessDate();
        List<Show> shows = showRepository.findByShowDate(busDate);
        Show show = shows.get(0);

        List<ShowSeat> availableSeats = showSeatRepository.findByIdShowId(show.getShowId()).stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .toList();
        assertTrue(availableSeats.size() >= 3);
        Long seatId = availableSeats.get(2).getId().getSeatId();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final long user = 10L + i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    SeatHoldResponse resp = seatHoldService.holdSeats(show.getShowId(), user, List.of(seatId));
                    if (resp != null && resp.getHoldToken() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (ConflictException ce) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one user must acquire the hold");
        assertEquals(1, conflictCount.get(), "The competing user must receive ConflictException");
    }

    @Test
    @DisplayName("Priority 4: Concurrent first-time seat map requests for unmaterialized show succeed without HTTP 500 duplicate key error")
    void testConcurrentJitSeatMaterialization() throws Exception {
        LocalDate futureDate = businessDateProvider.getCalendarHorizonEnd().minusDays(5);
        List<Show> shows = showRepository.findByShowDate(futureDate);
        if (shows.isEmpty()) {
            return; // advance show test guard
        }
        Show advanceShow = shows.get(0);

        // Remove any materialized seats for this advance show to test concurrent JIT
        jdbcTemplate.update("DELETE FROM show_seat WHERE show_id = ?", advanceShow.getShowId());

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        List<Future<ShowSeatAvailabilityResponse>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await();
                return seatAvailabilityService.getShowSeatAvailability(advanceShow.getShowId(), true);
            }));
        }

        for (Future<ShowSeatAvailabilityResponse> f : futures) {
            ShowSeatAvailabilityResponse resp = f.get();
            assertNotNull(resp);
            assertEquals(advanceShow.getShowId(), resp.getShowId());
            assertTrue(resp.getTotalSeats() > 0);
        }
        executor.shutdown();
    }

    @Test
    @DisplayName("Priority 2: Schedule extension executed twice preserves existing show IDs and does not duplicate shows")
    void testScheduleExtensionIdempotency() {
        LocalDate anchorDate = businessDateProvider.getBusinessDate();
        LocalDate testDate = anchorDate.plusDays(2);

        // Get initial count of shows for testDate
        List<Show> initialShows = showRepository.findByShowDate(testDate);
        assertFalse(initialShows.isEmpty());
        int initialCount = initialShows.size();
        Set<Long> initialShowIds = new HashSet<>(initialShows.stream().map(Show::getShowId).toList());

        // First execution of generateScheduleForDate on existing date
        YearLongShowGenerationService.GenerationSummary summary1 =
                yearLongShowGenerationService.generateScheduleForDate(testDate);
        assertEquals(0, summary1.totalShowsGenerated(), "Idempotent generator must not regenerate existing date");

        // Second execution
        YearLongShowGenerationService.GenerationSummary summary2 =
                yearLongShowGenerationService.generateScheduleForDate(testDate);
        assertEquals(0, summary2.totalShowsGenerated(), "Second run must not regenerate existing date");

        // Verify show count and show IDs are 100% identical and preserved
        List<Show> finalShows = showRepository.findByShowDate(testDate);
        assertEquals(initialCount, finalShows.size(), "Show count must be strictly preserved");
        Set<Long> finalShowIds = new HashSet<>(finalShows.stream().map(Show::getShowId).toList());
        assertEquals(initialShowIds, finalShowIds, "Show IDs must remain strictly stable and identical");
    }

    @Test
    @DisplayName("Priority 2: Administrative repair fails fast when bookings exist, protecting customer data")
    void testRepairScheduleRangeFailsWhenBookingsExist() {
        LocalDate busDate = businessDateProvider.getBusinessDate();
        // There is at least 1 booking on the business date from our earlier test
        assertThrows(IllegalStateException.class, () -> {
            yearLongShowGenerationService.repairScheduleRange(busDate, busDate, true);
        }, "Administrative repair must reject date ranges containing customer bookings");
    }
}
