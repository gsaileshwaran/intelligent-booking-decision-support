package com.pvk.cinemas.scheduling;

import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.common.exceptions.OutOfCalendarRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CalendarHorizonUnitTest {

    private BusinessDateProvider businessDateProvider;

    @BeforeEach
    void setUp() {
        businessDateProvider = new BusinessDateProvider("2026-09-18", 7);
    }

    @Test
    @DisplayName("Anchor date defaults to 2026-09-18 and spans exactly 365 days")
    void testDefaultAnchorAndHorizonBoundaries() {
        LocalDate anchor = businessDateProvider.getBusinessDate();
        assertEquals(LocalDate.of(2026, 9, 18), anchor);

        LocalDate start = businessDateProvider.getCalendarHorizonStart();
        LocalDate end = businessDateProvider.getCalendarHorizonEnd();

        // B - 260 days = 2026-01-01
        assertEquals(LocalDate.of(2026, 1, 1), start);
        // B + 104 days = 2026-12-31
        assertEquals(LocalDate.of(2026, 12, 31), end);
    }

    @Test
    @DisplayName("Boundary acceptance: B-260 and B+104 are within calendar range")
    void testCalendarBoundariesAccepted() {
        LocalDate start = businessDateProvider.getCalendarHorizonStart();
        LocalDate end = businessDateProvider.getCalendarHorizonEnd();

        assertTrue(businessDateProvider.isWithinCalendarRange(start));
        assertTrue(businessDateProvider.isWithinCalendarRange(end));
        assertTrue(businessDateProvider.isWithinCalendarRange(businessDateProvider.getBusinessDate()));

        assertDoesNotThrow(() -> businessDateProvider.validateCalendarRange(start));
        assertDoesNotThrow(() -> businessDateProvider.validateCalendarRange(end));
    }

    @Test
    @DisplayName("Boundary rejection: B-261 and B+105 throw OutOfCalendarRangeException")
    void testCalendarBoundariesRejected() {
        LocalDate beforeStart = businessDateProvider.getCalendarHorizonStart().minusDays(1); // 2025-12-31
        LocalDate afterEnd = businessDateProvider.getCalendarHorizonEnd().plusDays(1); // 2027-01-01

        assertFalse(businessDateProvider.isWithinCalendarRange(beforeStart));
        assertFalse(businessDateProvider.isWithinCalendarRange(afterEnd));

        OutOfCalendarRangeException exBefore = assertThrows(
                OutOfCalendarRangeException.class,
                () -> businessDateProvider.validateCalendarRange(beforeStart)
        );
        assertEquals("OUT_OF_CALENDAR_RANGE", exBefore.getErrorCode());
        assertTrue(exBefore.getMessage().contains("outside the allowed year-long calendar horizon"));

        OutOfCalendarRangeException exAfter = assertThrows(
                OutOfCalendarRangeException.class,
                () -> businessDateProvider.validateCalendarRange(afterEnd)
        );
        assertEquals("OUT_OF_CALENDAR_RANGE", exAfter.getErrorCode());
        assertTrue(exAfter.getMessage().contains("outside the allowed year-long calendar horizon"));
    }

    @Test
    @DisplayName("Deterministic business-date behavior when custom anchor is configured")
    void testCustomAnchorDeterminism() {
        BusinessDateProvider customProvider = new BusinessDateProvider("2026-06-01", 7);
        LocalDate customAnchor = customProvider.getBusinessDate();
        assertEquals(LocalDate.of(2026, 6, 1), customAnchor);

        LocalDate expectedStart = customAnchor.minusDays(260);
        LocalDate expectedEnd = customAnchor.plusDays(104);

        assertEquals(expectedStart, customProvider.getCalendarHorizonStart());
        assertEquals(expectedEnd, customProvider.getCalendarHorizonEnd());

        assertTrue(customProvider.isWithinCalendarRange(expectedStart));
        assertTrue(customProvider.isWithinCalendarRange(expectedEnd));
        assertFalse(customProvider.isWithinCalendarRange(expectedStart.minusDays(1)));
        assertFalse(customProvider.isWithinCalendarRange(expectedEnd.plusDays(1)));
    }
}
