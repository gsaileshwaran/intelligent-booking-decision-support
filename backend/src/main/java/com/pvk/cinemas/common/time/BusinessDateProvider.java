package com.pvk.cinemas.common.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative Centralized Business Date Provider for PVK Cinemas.
 * 
 * Prevents reliance on client clocks or arbitrary machine dates.
 * In production/demo, the authoritative business date defines:
 * - "Today" for booking rules and scheduling
 * - Server-side validation against booking shows in the past
 * - Deterministic 7-day demo booking window (2026-09-18 through 2026-09-24)
 */
@Component
public class BusinessDateProvider {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LocalDate businessDate;
    private final int windowDays;

    public BusinessDateProvider(
            @Value("${app.business-date:2026-09-18}") String configuredBusinessDate,
            @Value("${app.demo-window-days:7}") int windowDays) {
        this.businessDate = LocalDate.parse(configuredBusinessDate.trim(), ISO_FORMATTER);
        this.windowDays = Math.max(1, windowDays);
    }

    /**
     * The authoritative current business date (default: 2026-09-18).
     */
    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public ZoneId getZone() {
        return IST_ZONE;
    }

    public int getWindowDays() {
        return windowDays;
    }

    /**
     * Checks if a show date is strictly in the past relative to the business date.
     */
    public boolean isPast(LocalDate date) {
        if (date == null) return true;
        return date.isBefore(businessDate);
    }

    /**
     * Checks if a show date is eligible for customer booking.
     * Must be on or after business date, and within the demo window.
     */
    public boolean isBookable(LocalDate date) {
        if (date == null) return false;
        if (isPast(date)) return false;
        return !date.isAfter(businessDate.plusDays(windowDays - 1));
    }

    /**
     * Start of the 7-day demo window (2026-09-18).
     */
    public LocalDate getDemoWindowStart() {
        return businessDate;
    }

    /**
     * End of the 7-day demo window inclusive (2026-09-24).
     */
    public LocalDate getDemoWindowEnd() {
        return businessDate.plusDays(windowDays - 1);
    }

    /**
     * Returns the continuous list of all dates in the 7-day demo booking window.
     */
    public List<LocalDate> getDemoBookingWindow() {
        List<LocalDate> window = new ArrayList<>();
        for (int i = 0; i < windowDays; i++) {
            window.add(businessDate.plusDays(i));
        }
        return window;
    }
}
