package com.pvk.cinemas.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class SeatHoldResponse {

    private String holdToken;
    private Long showId;
    private List<Long> seatIds;
    private Instant expiresAt;
    private long expiresInSeconds;
    private BigDecimal totalAmount;

    public SeatHoldResponse() {}

    public SeatHoldResponse(String holdToken, Long showId, List<Long> seatIds, Instant expiresAt, long expiresInSeconds, BigDecimal totalAmount) {
        this.holdToken = holdToken;
        this.showId = showId;
        this.seatIds = seatIds;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
        this.totalAmount = totalAmount;
    }

    public String getHoldToken() { return holdToken; }
    public void setHoldToken(String holdToken) { this.holdToken = holdToken; }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
