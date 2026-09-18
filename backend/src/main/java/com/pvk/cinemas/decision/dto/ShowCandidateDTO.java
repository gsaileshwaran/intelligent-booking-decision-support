package com.pvk.cinemas.decision.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ShowCandidateDTO {

    private Long showId;
    private Long theatreId;
    private String theatreName;
    private Long screenId;
    private String screenName;
    private String presentationFormat; // e.g. IMAX Experience, Standard 2D, etc.
    private Instant startAt;
    private String formattedTime; // e.g. "06:30 PM"
    private int availableSeats;
    private int totalSeats;
    private double availabilityRatio; // 0.0 - 1.0
    private BigDecimal ticketPrice;
    private String showDate; // e.g. "2026-09-20"
    private String language; // e.g. "English"
    private BigDecimal totalCost; // partySize * ticketPrice
    private int matchScore; // 0 - 100
    private List<String> reasons = new ArrayList<>();
    private List<String> tradeOffs = new ArrayList<>();
    private List<Long> recommendedSeatIds = new ArrayList<>();
    private List<String> recommendedSeatLabels = new ArrayList<>();
    private List<String> alternativeSeatLabels = new ArrayList<>();
    private String seatFitDescription;
    private Integer viewingQualityScore;
    private String seatingTradeoff;

    public ShowCandidateDTO() {}

    public String getSeatFitDescription() { return seatFitDescription; }
    public void setSeatFitDescription(String seatFitDescription) { this.seatFitDescription = seatFitDescription; }

    public Integer getViewingQualityScore() { return viewingQualityScore; }
    public void setViewingQualityScore(Integer viewingQualityScore) { this.viewingQualityScore = viewingQualityScore; }

    public String getSeatingTradeoff() { return seatingTradeoff; }
    public void setSeatingTradeoff(String seatingTradeoff) { this.seatingTradeoff = seatingTradeoff; }

    public List<String> getAlternativeSeatLabels() { return alternativeSeatLabels; }
    public void setAlternativeSeatLabels(List<String> alternativeSeatLabels) { this.alternativeSeatLabels = alternativeSeatLabels; }

    public List<Long> getRecommendedSeatIds() { return recommendedSeatIds; }
    public void setRecommendedSeatIds(List<Long> recommendedSeatIds) { this.recommendedSeatIds = recommendedSeatIds; }

    public List<String> getRecommendedSeatLabels() { return recommendedSeatLabels; }
    public void setRecommendedSeatLabels(List<String> recommendedSeatLabels) { this.recommendedSeatLabels = recommendedSeatLabels; }

    public String getStartTime() { return formattedTime; }
    public Integer getAvailableSeatCount() { return availableSeats; }
    public BigDecimal getStartingPrice() { return ticketPrice; }

    public String getShowDate() { return showDate; }
    public void setShowDate(String showDate) { this.showDate = showDate; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public String getPresentationFormat() { return presentationFormat; }
    public void setPresentationFormat(String presentationFormat) { this.presentationFormat = presentationFormat; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public double getAvailabilityRatio() { return availabilityRatio; }
    public void setAvailabilityRatio(double availabilityRatio) { this.availabilityRatio = availabilityRatio; }

    public BigDecimal getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(BigDecimal ticketPrice) { this.ticketPrice = ticketPrice; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public List<String> getTradeOffs() { return tradeOffs; }
    public void setTradeOffs(List<String> tradeOffs) { this.tradeOffs = tradeOffs; }
}
