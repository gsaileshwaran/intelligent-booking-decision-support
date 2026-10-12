package com.pvk.cinemas.common.time;

import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.OutOfCalendarRangeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative Centralized Business Date Provider for PVK Cinemas.
 * 
 * Prevents reliance on client clocks or arbitrary machine dates.
 * In production/runtime, calculates current business date dynamically in Asia/Kolkata timezone.
 * Supports configurable fixed demo date or clock overrides for testing.
 *
 * Defines:
 * - "Today" for booking rules and scheduling
 * - Server-side validation against booking shows in the past (SHOW_DATE_IN_PAST)
 * - Strict rolling 7-calendar-day booking window [businessDate, businessDate + 6] (SHOW_DATE_OUT_OF_WINDOW)
 * - Automatic window advancement at midnight IST without application restart or schedule regeneration
 */
@Component
public class BusinessDateProvider {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LocalDate fixedDate;
    private final int windowDays;
    private volatile Clock clock;
    private volatile LocalDate businessDateOverride;

    public BusinessDateProvider(
            @Value("${app.business-date:auto}") String configuredBusinessDate,
            @Value("${app.demo-window-days:7}") int windowDays) {
        this.windowDays = Math.max(1, windowDays);
        this.clock = Clock.system(IST_ZONE);

        if (configuredBusinessDate != null
                && !configuredBusinessDate.isBlank()
                && !"auto".equalsIgnoreCase(configuredBusinessDate.trim())
                && !"today".equalsIgnoreCase(configuredBusinessDate.trim())
                && !"current".equalsIgnoreCase(configuredBusinessDate.trim())) {
            this.fixedDate = LocalDate.parse(configuredBusinessDate.trim(), ISO_FORMATTER);
        } else {
            this.fixedDate = null;
        }
    }

    /**
     * Set a custom Clock for time-advancement testing without restarts.
     */
    public void setClock(Clock clock) {
        this.clock = clock;
        this.businessDateOverride = null;
    }

    /**
     * Set an explicit business date override for focused test cases.
     */
    public void setBusinessDateOverride(LocalDate date) {
        this.businessDateOverride = date;
    }

    /**
     * Clear any explicit business date override.
     */
    public void clearBusinessDateOverride() {
        this.businessDateOverride = null;
    }

    /**
     * The authoritative current business date calculated in Asia/Kolkata timezone.
     */
    public LocalDate getBusinessDate() {
        if (businessDateOverride != null) {
            return businessDateOverride;
        }
        if (fixedDate != null) {
            return fixedDate;
        }
        return LocalDate.now(clock.withZone(IST_ZONE));
    }

    public ZoneId getZone() {
        return IST_ZONE;
    }

    public int getWindowDays() {
        return windowDays;
    }

    /**
     * Start of the rolling customer booking window (businessDate).
     */
    public LocalDate getBookingWindowStart() {
        return getBusinessDate();
    }

    /**
     * Inclusive end of the rolling customer booking window (businessDate + windowDays - 1).
     * For 7 calendar days, this is businessDate + 6 calendar days.
     */
    public LocalDate getBookingWindowEnd() {
        return getBusinessDate().plusDays(windowDays - 1);
    }

    /**
     * Returns the continuous list of all permitted calendar dates in the rolling booking window.
     */
    public List<LocalDate> getBookingWindow() {
        LocalDate start = getBookingWindowStart();
        List<LocalDate> window = new ArrayList<>();
        for (int i = 0; i < windowDays; i++) {
            window.add(start.plusDays(i));
        }
        return window;
    }

    // Backwards-compatible aliases for demo window methods
    public LocalDate getDemoWindowStart() {
        return getBookingWindowStart();
    }

    public LocalDate getDemoWindowEnd() {
        return getBookingWindowEnd();
    }

    public List<LocalDate> getDemoBookingWindow() {
        return getBookingWindow();
    }

    /**
     * Checks if a show date is strictly in the past relative to the business date.
     */
    public boolean isPast(LocalDate date) {
        if (date == null) return true;
        return date.isBefore(getBusinessDate());
    }

    /**
     * Checks if a show date is eligible for customer booking.
     * Must be on or after business date, and within the rolling 7-day booking window.
     */
    public boolean isBookable(LocalDate date) {
        if (date == null) return false;
        if (isPast(date)) return false;
        return !date.isAfter(getBookingWindowEnd());
    }

    /**
     * Validates that a date falls strictly within the customer booking window [businessDate, businessDate + 6].
     * Throws SHOW_DATE_INVALID if null, SHOW_DATE_IN_PAST if past, or SHOW_DATE_OUT_OF_WINDOW if beyond 7 days.
     */
    public void validateBookingWindow(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("SHOW_DATE_INVALID", "Show date cannot be null");
        }
        if (isPast(date)) {
            throw new BadRequestException("SHOW_DATE_IN_PAST",
                    String.format("SHOW_DATE_IN_PAST: Show date %s is before authoritative business date %s and cannot be booked.",
                            date, getBusinessDate()));
        }
        if (date.isAfter(getBookingWindowEnd())) {
            throw new BadRequestException("SHOW_DATE_OUT_OF_WINDOW",
                    String.format("SHOW_DATE_OUT_OF_WINDOW: Show date %s is beyond the rolling %d-day customer booking window [%s to %s].",
                            date, windowDays, getBookingWindowStart(), getBookingWindowEnd()));
        }
    }

    // =========================================================================
    // Part 3: Year-Long Calendar Horizon Architecture (§3)
    // Total Calendar Horizon: Exactly 365 calendar days [B - 260, B + 365]
    // =========================================================================

    public static final int HORIZON_PAST_DAYS = 260;
    public static final int HORIZON_FUTURE_DAYS = 365;
    public static final int ACTIVE_BOOKING_WINDOW_DAYS = 14;

    public LocalDate getCalendarHorizonStart() {
        return getBusinessDate().minusDays(HORIZON_PAST_DAYS);
    }

    public LocalDate getCalendarHorizonEnd() {
        return getBusinessDate().plusDays(HORIZON_FUTURE_DAYS);
    }

    public boolean isWithinCalendarRange(LocalDate date) {
        if (date == null) return false;
        return !date.isBefore(getCalendarHorizonStart()) && !date.isAfter(getCalendarHorizonEnd());
    }

    public void validateCalendarRange(LocalDate date) {
        if (date != null && !isWithinCalendarRange(date)) {
            throw new OutOfCalendarRangeException(
                    "Date " + date + " is outside the allowed year-long calendar horizon ["
                            + getCalendarHorizonStart() + " to " + getCalendarHorizonEnd() + "]");
        }
    }

    public LocalDate getHistoricalArchiveStart() {
        return getBusinessDate().minusDays(HORIZON_PAST_DAYS);
    }

    public LocalDate getHistoricalArchiveEnd() {
        return getBusinessDate().minusDays(1);
    }

    public LocalDate getActiveBookingWindowStart() {
        return getBusinessDate();
    }

    public LocalDate getActiveBookingWindowEnd() {
        return getBusinessDate().plusDays(ACTIVE_BOOKING_WINDOW_DAYS - 1);
    }

    public LocalDate getAdvanceScheduleStart() {
        return getBusinessDate().plusDays(ACTIVE_BOOKING_WINDOW_DAYS);
    }

    public LocalDate getAdvanceScheduleEnd() {
        return getBusinessDate().plusDays(HORIZON_FUTURE_DAYS);
    }
}
