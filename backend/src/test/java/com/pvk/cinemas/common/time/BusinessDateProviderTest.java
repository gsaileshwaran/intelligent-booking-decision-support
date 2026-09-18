package com.pvk.cinemas.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessDateProviderTest {

    @Test
    @DisplayName("Default business date is 2026-09-18 and window is 7 days")
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
    @DisplayName("isPast correctly identifies dates before 2026-09-18")
    void testIsPast() {
        BusinessDateProvider provider = new BusinessDateProvider("2026-09-18", 7);

        assertTrue(provider.isPast(LocalDate.of(2026, 9, 17)));
        assertTrue(provider.isPast(LocalDate.of(2025, 1, 1)));
        assertTrue(provider.isPast(null));

        assertFalse(provider.isPast(LocalDate.of(2026, 9, 18)));
        assertFalse(provider.isPast(LocalDate.of(2026, 9, 19)));
        assertFalse(provider.isPast(LocalDate.of(2026, 9, 24)));
    }

    @Test
    @DisplayName("isBookable correctly limits to the 7-day demo window [2026-09-18, 2026-09-24]")
    void testIsBookable() {
        BusinessDateProvider provider = new BusinessDateProvider("2026-09-18", 7);

        // Before window
        assertFalse(provider.isBookable(LocalDate.of(2026, 9, 17)));
        assertFalse(provider.isBookable(null));

        // Inside 7-day window
        for (int i = 0; i < 7; i++) {
            LocalDate d = LocalDate.of(2026, 9, 18).plusDays(i);
            assertTrue(provider.isBookable(d), "Date " + d + " should be bookable");
        }

        // After window
        assertFalse(provider.isBookable(LocalDate.of(2026, 9, 25)));
        assertFalse(provider.isBookable(LocalDate.of(2026, 10, 1)));
    }
}
