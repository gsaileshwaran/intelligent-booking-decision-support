package com.pvk.cinemas.decision.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for show recommendation with hard constraints and priority modes.
 *
 * Hard Constraints (applied before ranking — must all be satisfied):
 *   - cityId: shows must belong to theatres in this city (authoritative DB relationship)
 *   - languageCode: shows must match this language (e.g., "en", "ta", "hi")
 *   - dateFrom / dateTo: show date must be within this range
 *   - partySize: show must have >= partySize available seats
 *   - budgetMaxTotal: partySize × ticketPrice must not exceed this total budget
 *
 * Soft Preferences (influence ranking but do not exclude):
 *   - formatPreference: preferred format name (e.g., "IMAX", "4DX")
 *   - timePreference: preferred time slot ("MORNING", "AFTERNOON", "EVENING", "NIGHT" or "18:30")
 *
 * Priority Mode (controls ranking weight distribution):
 *   - BEST_PRICE: price 50%, seats 25%, time 25%
 *   - BEST_SEATS: seats 50%, price 25%, time 25%
 *   - BEST_TIME: time 50%, seats 30%, price 20%
 *   - BALANCED (default): seats 40%, price 30%, time 30%
 */
public class ShowRecommendationRequest {

    // Scope
    private Long cityId;
    private Long theatreId;

    // Hard constraints
    private String languageCode;       // e.g. "en", "ta", "hi" — null means no language constraint
    private LocalDate dateFrom;        // inclusive — null means no lower bound
    private LocalDate dateTo;          // inclusive — null means no upper bound
    private int partySize = 2;
    private BigDecimal budgetMaxTotal; // total budget for all partySize seats

    // Soft preferences
    private String formatPreference;   // e.g. "IMAX", "4DX", "2D"
    private String timePreference;     // e.g. "EVENING", "AFTERNOON", "MORNING", "NIGHT", or "18:30"

    // Priority mode — drives weight distribution
    private String priority = "BALANCED"; // BEST_PRICE | BEST_SEATS | BEST_TIME | BALANCED

    // Manual weight overrides (optional — if all non-null, bypass priority mode)
    private Double weightFormat;
    private Double weightTime;
    private Double weightAvailability;
    private Double weightPrice;

    // Legacy field for backward compat
    private BigDecimal budgetMax; // per-ticket budget (deprecated; use budgetMaxTotal)

    public ShowRecommendationRequest() {}

    public ShowRecommendationRequest(Long cityId, String formatPreference, String timePreference, int partySize) {
        this.cityId = cityId;
        this.formatPreference = formatPreference;
        this.timePreference = timePreference;
        this.partySize = partySize;
    }

    // ---- Getters / Setters ----

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }

    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }

    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }

    public BigDecimal getBudgetMaxTotal() { return budgetMaxTotal; }
    public void setBudgetMaxTotal(BigDecimal budgetMaxTotal) { this.budgetMaxTotal = budgetMaxTotal; }

    /** Legacy per-ticket budget field — mapped to budgetMaxTotal if set. */
    public BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }

    public String getFormatPreference() { return formatPreference; }
    public void setFormatPreference(String formatPreference) { this.formatPreference = formatPreference; }

    public String getTimePreference() { return timePreference; }
    public void setTimePreference(String timePreference) { this.timePreference = timePreference; }

    public String getPriority() { return priority != null ? priority : "BALANCED"; }
    public void setPriority(String priority) { this.priority = priority; }

    // ---- Priority-driven weight resolution ----

    /**
     * Returns the effective format-match weight for ranking, based on priority mode.
     * Manual overrides take precedence if all four weights are explicitly set.
     */
    public double getWeightAvailability() {
        if (hasManualWeights()) return weightAvailability;
        return switch (getPriority()) {
            case "BEST_SEATS" -> 0.55;
            case "BEST_PRICE" -> 0.25;
            case "BEST_TIME"  -> 0.25;
            default           -> 0.40; // BALANCED: Seat Fit = 40%
        };
    }
    public void setWeightAvailability(Double weightAvailability) { this.weightAvailability = weightAvailability; }

    public double getWeightPrice() {
        if (hasManualWeights()) return weightPrice;
        return switch (getPriority()) {
            case "BEST_PRICE" -> 0.55;
            case "BEST_SEATS" -> 0.20;
            case "BEST_TIME"  -> 0.20;
            default           -> 0.30; // BALANCED: Price/Value = 30%
        };
    }
    public void setWeightPrice(Double weightPrice) { this.weightPrice = weightPrice; }

    public double getWeightTime() {
        if (hasManualWeights()) return weightTime;
        return switch (getPriority()) {
            case "BEST_TIME"  -> 0.55;
            case "BEST_PRICE" -> 0.20;
            case "BEST_SEATS" -> 0.25;
            default           -> 0.30; // BALANCED: Time Fit = 30%
        };
    }
    public void setWeightTime(Double weightTime) { this.weightTime = weightTime; }

    public double getWeightFormat() {
        if (hasManualWeights()) return weightFormat;
        return 0.0; // Format is a hard constraint when specified
    }
    public void setWeightFormat(Double weightFormat) { this.weightFormat = weightFormat; }

    private boolean hasManualWeights() {
        return weightTime != null && weightAvailability != null && weightPrice != null;
    }
}
