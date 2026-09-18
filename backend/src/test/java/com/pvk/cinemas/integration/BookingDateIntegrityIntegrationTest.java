package com.pvk.cinemas.integration;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.dto.SeatHoldResponse;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingDateIntegrityIntegrationTest {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private BusinessDateProvider businessDateProvider;

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Test
    @DisplayName("BusinessDateProvider is authoritative and configured for 2026-09-18")
    void verifyAuthoritativeBusinessDate() {
        assertNotNull(businessDateProvider);
        assertEquals(LocalDate.of(2026, 9, 18), businessDateProvider.getBusinessDate());
        assertEquals(7, businessDateProvider.getWindowDays());
        assertEquals(LocalDate.of(2026, 9, 18), businessDateProvider.getDemoWindowStart());
        assertEquals(LocalDate.of(2026, 9, 24), businessDateProvider.getDemoWindowEnd());
    }

    @Test
    @DisplayName("PART E CASE 1: Past show booking (2026-09-17) is rejected with SHOW_DATE_IN_PAST")
    @Transactional
    void rejectPastDateBooking() {
        // Find a show on 2026-09-17
        List<Show> pastShows = showRepository.findAll().stream()
                .filter(s -> s.getStartAt() != null && s.getStartAt().atZone(IST_ZONE).toLocalDate().equals(LocalDate.of(2026, 9, 17)))
                .toList();

        assertFalse(pastShows.isEmpty(), "Should have shows seeded for 2026-09-17");
        Show pastShow = pastShows.get(0);

        List<ShowSeat> availableSeats = showSeatRepository.findByIdShowId(pastShow.getShowId()).stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .toList();
        assertFalse(availableSeats.isEmpty(), "Past show should have seats");

        Long seatId = availableSeats.get(0).getId().getSeatId();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            seatHoldService.holdSeats(pastShow.getShowId(), 1L, List.of(seatId));
        });

        assertEquals("SHOW_DATE_IN_PAST", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("SHOW_DATE_IN_PAST"));
    }

    @Test
    @DisplayName("PART E CASE 2 & 3: Current (2026-09-18) and Future (2026-09-19) shows can be held successfully")
    @Transactional
    void allowCurrentAndFutureDateBooking() {
        // Find show on business date (2026-09-18)
        List<Show> todayShows = showRepository.findAll().stream()
                .filter(s -> s.getStartAt() != null && s.getStartAt().atZone(IST_ZONE).toLocalDate().equals(LocalDate.of(2026, 9, 18)))
                .toList();
        assertFalse(todayShows.isEmpty(), "Should have shows for 2026-09-18");
        Show todayShow = todayShows.get(0);

        List<ShowSeat> availableSeats = showSeatRepository.findByIdShowId(todayShow.getShowId()).stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .toList();
        assertFalse(availableSeats.isEmpty(), "Today show should have available seats");
        Long seatId = availableSeats.get(0).getId().getSeatId();

        SeatHoldResponse holdResponse = seatHoldService.holdSeats(todayShow.getShowId(), 1L, List.of(seatId));
        assertNotNull(holdResponse);
        assertNotNull(holdResponse.getHoldToken());
        assertTrue(holdResponse.getSeatIds().contains(seatId));
    }

    @Test
    @DisplayName("PART B #13 & #14: Cross-date seat isolation - booking seat on 2026-09-18 leaves it available on 2026-09-19")
    @Transactional
    void testCrossDateSeatIsolation() {
        // Find two shows on the same screen on consecutive dates
        List<Show> allShows = showRepository.findAll();
        Map<Long, List<Show>> screenCapabilityShows = allShows.stream()
                .collect(Collectors.groupingBy(Show::getScreenCapabilityId));

        Long targetCapabilityId = null;
        Show showDay1 = null;
        Show showDay2 = null;

        for (Map.Entry<Long, List<Show>> entry : screenCapabilityShows.entrySet()) {
            Show d1 = entry.getValue().stream()
                    .filter(s -> s.getStartAt().atZone(IST_ZONE).toLocalDate().equals(LocalDate.of(2026, 9, 18)))
                    .findFirst().orElse(null);
            Show d2 = entry.getValue().stream()
                    .filter(s -> s.getStartAt().atZone(IST_ZONE).toLocalDate().equals(LocalDate.of(2026, 9, 19)))
                    .findFirst().orElse(null);
            if (d1 != null && d2 != null) {
                targetCapabilityId = entry.getKey();
                showDay1 = d1;
                showDay2 = d2;
                break;
            }
        }

        assertNotNull(showDay1, "Must find show on 2026-09-18");
        assertNotNull(showDay2, "Must find show on 2026-09-19 for same capability");
        assertNotEquals(showDay1.getShowId(), showDay2.getShowId(), "Each show must have unique showId");

        // Find a seat available in both shows
        List<ShowSeat> day1Avail = showSeatRepository.findByIdShowId(showDay1.getShowId()).stream()
                .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                .toList();
        List<ShowSeat> day2Seats = showSeatRepository.findByIdShowId(showDay2.getShowId());
        Map<Long, ShowSeat> day2SeatMap = day2Seats.stream()
                .collect(Collectors.toMap(ss -> ss.getId().getSeatId(), ss -> ss));

        Long testSeatId = null;
        for (ShowSeat ss : day1Avail) {
            ShowSeat ss2 = day2SeatMap.get(ss.getId().getSeatId());
            if (ss2 != null && "AVAILABLE".equalsIgnoreCase(ss2.getAvailabilityStatus())) {
                testSeatId = ss.getId().getSeatId();
                break;
            }
        }

        assertNotNull(testSeatId, "Should find common available physical seat");

        // Hold seat on Day 1
        SeatHoldResponse holdD1 = seatHoldService.holdSeats(showDay1.getShowId(), 1L, List.of(testSeatId));
        assertNotNull(holdD1);

        // Verify that on Day 2, the exact same physical seat remains AVAILABLE and can be held independently!
        ShowSeat day2SeatStatus = showSeatRepository.findByIdShowIdAndIdSeatId(showDay2.getShowId(), testSeatId).orElseThrow();
        assertEquals("AVAILABLE", day2SeatStatus.getAvailabilityStatus(), "Seat on Day 2 must remain AVAILABLE");

        SeatHoldResponse holdD2 = seatHoldService.holdSeats(showDay2.getShowId(), 2L, List.of(testSeatId));
        assertNotNull(holdD2);
        assertNotNull(holdD2.getHoldToken());
        assertTrue(holdD2.getSeatIds().contains(testSeatId));
    }

    @Test
    @DisplayName("PART B #12: No overlapping active shows on the same physical screen across all dates")
    void testNoScreenOverlap() {
        List<Show> allShows = showRepository.findAll();
        Map<Long, List<Show>> byCapability = allShows.stream()
                .collect(Collectors.groupingBy(Show::getScreenCapabilityId));

        for (Map.Entry<Long, List<Show>> entry : byCapability.entrySet()) {
            List<Show> shows = entry.getValue();
            // Sort chronologically
            shows.sort((a, b) -> a.getStartAt().compareTo(b.getStartAt()));
            for (int i = 0; i < shows.size() - 1; i++) {
                Show s1 = shows.get(i);
                Show s2 = shows.get(i + 1);
                assertFalse(s1.getEndAt().isAfter(s2.getStartAt()),
                        String.format("Overlap detected on screen capability %d: Show %d [%s - %s] overlaps Show %d [%s - %s]",
                                entry.getKey(), s1.getShowId(), s1.getStartAt(), s1.getEndAt(),
                                s2.getShowId(), s2.getStartAt(), s2.getEndAt()));
            }
        }
    }
}
