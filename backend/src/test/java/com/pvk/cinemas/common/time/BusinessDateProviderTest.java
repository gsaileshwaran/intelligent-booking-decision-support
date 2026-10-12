package com.pvk.cinemas.common.time;

import com.pvk.cinemas.common.exceptions.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessDateProviderTest {

    @Test
    @DisplayName("Default business date is 2026-09-18 and window is 7 days when fixed date configured")
    void defaultConfiguration() {
        BusinessDateProvider provider = new BusinessDateProvider("2026-09-18", 7);

        assertEquals(LocalDate.of(2026, 9, 18), provider.getBusinessDate());
        assertEquals(7, provider.getWindowDays());
        assertEquals(LocalDate.of(2026, 9, 18), provider.getDemoWindowStart());
        assertEquals(LocalDate.of(2026, 9, 24), provider.getDemoWindowEnd());

        List<LocalDate> window = provider.getDemoBookingWindow();
        assertEquals(7, window.size());
        assertEquals(LocalDate.of(2026, 9, 18), window.get(0));
        assertEquals(LocalDate.of(2026, 9, 24), window.get(6));
    }

    @Test
    @DisplayName("Runtime business date defaults to current date in Asia/Kolkata timezone when auto configured")
    void testRuntimeDynamicAutoCalculation() {
        // Fix clock to October 11, 2026 at 12:00:00 UTC (17:30:00 IST)
        Instant instant = Instant.parse("2026-10-11T12:00:00Z");
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        Clock fixedClock = Clock.fixed(instant, ist);

        BusinessDateProvider provider = new BusinessDateProvider("auto", 7);
        provider.setClock(fixedClock);

        assertEquals(ZoneId.of("Asia/Kolkata"), provider.getZone());
        assertEquals(LocalDate.of(2026, 10, 11), provider.getBusinessDate());
        assertEquals(LocalDate.of(2026, 10, 11), provider.getBookingWindowStart());
        assertEquals(LocalDate.of(2026, 10, 17), provider.getBookingWindowEnd());

        List<LocalDate> window = provider.getBookingWindow();
        assertEquals(7, window.size());
        assertEquals(LocalDate.of(2026, 10, 11), window.get(0)); // 1st calendar day
        assertEquals(LocalDate.of(2026, 10, 17), window.get(6)); // 7th calendar day
    }

    @Test
    @DisplayName("isPast correctly identifies dates before business date")
    void testIsPast() {
        BusinessDateProvider provider = new BusinessDateProvider("2026-10-11", 7);

        assertTrue(provider.isPast(LocalDate.of(2026, 10, 10)));
        assertTrue(provider.isPast(LocalDate.of(2026, 9, 18)));
        assertTrue(provider.isPast(null));

        assertFalse(provider.isPast(LocalDate.of(2026, 10, 11)));
        assertFalse(provider.isPast(LocalDate.of(2026, 10, 12)));
        assertFalse(provider.isPast(LocalDate.of(2026, 10, 17)));
    }

    @Test
    @DisplayName("Acceptance: Day 1 (businessDate) and Day 7 (businessDate + 6) are valid and bookable")
    void testFirstAndSeventhPermittedDatesAccepted() {
        BusinessDateProvider provider = new BusinessDateProvider("2026-10-11", 7);

        LocalDate day1 = LocalDate.of(2026, 10, 11);
        LocalDate day7 = LocalDate.of(2026, 10, 17);

        assertTrue(provider.isBookable(day1));
        assertTrue(provider.isBookable(day7));

        assertDoesNotThrow(() -> provider.validateBookingWindow(day1));
        assertDoesNotThrow(() -> provider.validateBookingWindow(day7));

        // All intermediate days inside window are also bookable
        for (int i = 0; i < 7; i++) {
            LocalDate d = day1.plusDays(i);
            assertTrue(provider.isBookable(d), "Day " + d + " must be bookable");
            assertDoesNotThrow(() -> provider.validateBookingWindow(d));
        }
    }

    @Test
    @DisplayName("Rejection: Yesterday (businessDate - 1) and Day 8 (businessDate + 7) are rejected")
    void testYesterdayAndDayEightRejected() {
        BusinessDateProvider provider = new BusinessDateProvider("2026-10-11", 7);

        LocalDate yesterday = LocalDate.of(2026, 10, 10);
        LocalDate day8 = LocalDate.of(2026, 10, 18);

        assertFalse(provider.isBookable(yesterday));
        assertFalse(provider.isBookable(day8));
        assertFalse(provider.isBookable(null));

        BadRequestException exYesterday = assertThrows(BadRequestException.class,
                () -> provider.validateBookingWindow(yesterday));
        assertEquals("SHOW_DATE_IN_PAST", exYesterday.getErrorCode());
        assertTrue(exYesterday.getMessage().contains("before authoritative business date"));

        BadRequestException exDay8 = assertThrows(BadRequestException.class,
                () -> provider.validateBookingWindow(day8));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", exDay8.getErrorCode());
        assertTrue(exDay8.getMessage().contains("beyond the rolling 7-day customer booking window"));

        BadRequestException exNull = assertThrows(BadRequestException.class,
                () -> provider.validateBookingWindow(null));
        assertEquals("SHOW_DATE_INVALID", exNull.getErrorCode());
    }

    @Test
    @DisplayName("Window advances automatically when clock advances without restart or regeneration")
    void testWindowAdvancesAutomaticallyWithClock() {
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        BusinessDateProvider provider = new BusinessDateProvider("auto", 7);

        // Day T: Oct 11, 2026
        provider.setClock(Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ist));
        assertEquals(LocalDate.of(2026, 10, 11), provider.getBusinessDate());
        assertEquals(LocalDate.of(2026, 10, 11), provider.getBookingWindowStart());
        assertEquals(LocalDate.of(2026, 10, 17), provider.getBookingWindowEnd());
        assertTrue(provider.isBookable(LocalDate.of(2026, 10, 11)));
        assertFalse(provider.isBookable(LocalDate.of(2026, 10, 18)));

        // Advance Clock to Day T + 1: Oct 12, 2026 (e.g. crossing midnight in IST)
        provider.setClock(Clock.fixed(Instant.parse("2026-10-12T01:00:00Z"), ist));
        assertEquals(LocalDate.of(2026, 10, 12), provider.getBusinessDate());
        assertEquals(LocalDate.of(2026, 10, 12), provider.getBookingWindowStart());
        assertEquals(LocalDate.of(2026, 10, 18), provider.getBookingWindowEnd());

        // Oct 11 is now past and rejected!
        assertFalse(provider.isBookable(LocalDate.of(2026, 10, 11)));
        BadRequestException exPast = assertThrows(BadRequestException.class,
                () -> provider.validateBookingWindow(LocalDate.of(2026, 10, 11)));
        assertEquals("SHOW_DATE_IN_PAST", exPast.getErrorCode());

        // Oct 18 (formerly day 8) is now inside the window and accepted!
        assertTrue(provider.isBookable(LocalDate.of(2026, 10, 18)));
        assertDoesNotThrow(() -> provider.validateBookingWindow(LocalDate.of(2026, 10, 18)));

        // Oct 19 is the new day 8 and rejected
        assertFalse(provider.isBookable(LocalDate.of(2026, 10, 19)));
        BadRequestException exFuture = assertThrows(BadRequestException.class,
                () -> provider.validateBookingWindow(LocalDate.of(2026, 10, 19)));
        assertEquals("SHOW_DATE_OUT_OF_WINDOW", exFuture.getErrorCode());
    }
}
