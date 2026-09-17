package com.pvk.cinemas.booking.service;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.dto.SeatHoldResponse;
import com.pvk.cinemas.booking.model.SeatHold;
import com.pvk.cinemas.booking.repository.SeatHoldRepository;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class SeatHoldService {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldService.class);
    private static final long HOLD_DURATION_SECONDS = 300; // 5 minutes

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatPricingService seatPricingService;

    public SeatHoldService(SeatHoldRepository seatHoldRepository,
                           ShowSeatRepository showSeatRepository,
                           ShowRepository showRepository,
                           SeatRepository seatRepository,
                           SeatTypeRepository seatTypeRepository,
                           SeatPricingService seatPricingService) {
        this.seatHoldRepository = seatHoldRepository;
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.seatPricingService = seatPricingService;
    }

    /**
     * Concurrency-safe seat holding with pessimistic locking.
     */
    @Transactional
    public SeatHoldResponse holdSeats(Long showId, Long userId, List<Long> seatIds) {
        if (showId == null) {
            throw new IllegalArgumentException("Show ID must not be null");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected to hold");
        }
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show not found with ID: " + showId);
        }

        // 1. Release expired holds for this show first
        releaseExpiredHoldsForShow(showId);

        // 2. Acquire pessimistic write lock on target show seats
        List<ShowSeat> lockedShowSeats = showSeatRepository.findByIdShowIdAndIdSeatIdInForUpdate(showId, seatIds);
        Map<Long, ShowSeat> seatMap = new HashMap<>();
        for (ShowSeat ss : lockedShowSeats) {
            seatMap.put(ss.getId().getSeatId(), ss);
        }

        // 3. Verify all seats exist for this show
        for (Long seatId : seatIds) {
            if (!seatMap.containsKey(seatId)) {
                throw new ResourceNotFoundException("Seat ID " + seatId + " does not exist for show " + showId);
            }
        }

        // 4. Verify that each requested seat is AVAILABLE (auto-release if hold expired) and physical seat is ACTIVE (§14-15)
        for (Long seatId : seatIds) {
            Seat physicalSeat = seatRepository.findById(seatId).orElse(null);
            if (physicalSeat != null && physicalSeat.getStatus() != null && !"ACTIVE".equalsIgnoreCase(physicalSeat.getStatus())) {
                String seatLabel = getSeatLabel(seatId);
                throw new ConflictException("Seat " + seatLabel + " is currently blocked for maintenance and cannot be reserved.");
            }

            ShowSeat ss = seatMap.get(seatId);
            if ("HELD".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                Optional<SeatHold> activeHold = seatHoldRepository.findByShowIdAndSeatIdAndStatus(showId, seatId, "ACTIVE");
                if (activeHold.isPresent() && activeHold.get().isExpired()) {
                    SeatHold h = activeHold.get();
                    h.setStatus("RELEASED");
                    seatHoldRepository.save(h);
                    ss.setAvailabilityStatus("AVAILABLE");
                    showSeatRepository.save(ss);
                }
            }

            if (!"AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                String seatLabel = getSeatLabel(seatId);
                throw new ConflictException("Seat " + seatLabel + " is no longer available.");
            }
        }

        // 5. All seats are available -> transition to HELD and record holds
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(HOLD_DURATION_SECONDS);
        String holdToken = UUID.randomUUID().toString();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SeatHold> holdsToSave = new ArrayList<>();

        for (Long seatId : seatIds) {
            ShowSeat ss = seatMap.get(seatId);
            ss.setAvailabilityStatus("HELD");
            showSeatRepository.save(ss);

            SeatHold hold = new SeatHold(holdToken, showId, seatId, userId, expiresAt);
            holdsToSave.add(hold);

            totalAmount = totalAmount.add(determineSeatPrice(showId, seatId));
        }

        seatHoldRepository.saveAll(holdsToSave);
        log.info("Successfully held {} seats for user {} on show {} with token {}", seatIds.size(), userId, showId, holdToken);

        return new SeatHoldResponse(holdToken, showId, seatIds, expiresAt, HOLD_DURATION_SECONDS, totalAmount);
    }

    /**
     * Release active holds explicitly by customer.
     */
    @Transactional
    public void releaseHold(String holdToken, Long userId) {
        if (holdToken == null || holdToken.isBlank()) {
            return;
        }
        List<SeatHold> activeHolds = seatHoldRepository.findByHoldTokenAndStatus(holdToken, "ACTIVE");
        if (activeHolds.isEmpty()) {
            return;
        }

        for (SeatHold hold : activeHolds) {
            hold.setStatus("RELEASED");
            showSeatRepository.findByIdShowIdAndIdSeatId(hold.getShowId(), hold.getSeatId()).ifPresent(ss -> {
                if ("HELD".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                    ss.setAvailabilityStatus("AVAILABLE");
                    showSeatRepository.save(ss);
                }
            });
        }
        seatHoldRepository.saveAll(activeHolds);
        log.info("Released hold with token {} for user {}", holdToken, userId);
    }

    /**
     * Clean up expired holds for a specific show.
     */
    @Transactional
    public void releaseExpiredHoldsForShow(Long showId) {
        List<SeatHold> activeHolds = seatHoldRepository.findByShowIdAndStatus(showId, "ACTIVE");
        Instant now = Instant.now();
        List<SeatHold> expired = new ArrayList<>();

        for (SeatHold hold : activeHolds) {
            if (hold.getExpiresAt().isBefore(now)) {
                hold.setStatus("RELEASED");
                expired.add(hold);
                showSeatRepository.findByIdShowIdAndIdSeatId(hold.getShowId(), hold.getSeatId()).ifPresent(ss -> {
                    if ("HELD".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                        ss.setAvailabilityStatus("AVAILABLE");
                        showSeatRepository.save(ss);
                    }
                });
            }
        }
        if (!expired.isEmpty()) {
            seatHoldRepository.saveAll(expired);
            log.info("Cleaned up {} expired holds for show {}", expired.size(), showId);
        }
    }

    /**
     * Background scheduler to sweep expired holds across the system.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void sweepExpiredHolds() {
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtBefore("ACTIVE", Instant.now());
        if (expiredHolds.isEmpty()) {
            return;
        }
        for (SeatHold hold : expiredHolds) {
            hold.setStatus("RELEASED");
            showSeatRepository.findByIdShowIdAndIdSeatId(hold.getShowId(), hold.getSeatId()).ifPresent(ss -> {
                if ("HELD".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                    ss.setAvailabilityStatus("AVAILABLE");
                    showSeatRepository.save(ss);
                }
            });
        }
        seatHoldRepository.saveAll(expiredHolds);
        log.debug("Swept and released {} globally expired seat holds", expiredHolds.size());
    }

    public BigDecimal determineSeatPrice(Long showId, Long seatId) {
        if (seatPricingService != null) {
            return seatPricingService.determineSeatPrice(showId, seatId);
        }
        return determineSeatPrice(seatId);
    }

    public BigDecimal determineSeatPrice(Long seatId) {
        if (seatPricingService != null) {
            return seatPricingService.determineSeatPrice(seatId);
        }
        return seatRepository.findById(seatId)
                .flatMap(s -> seatTypeRepository.findById(s.getSeatTypeId()))
                .map(SeatType::getTypeCode)
                .map(code -> switch (code.toUpperCase()) {
                    case "RECLINER" -> new BigDecimal("350.00");
                    case "PREMIUM" -> new BigDecimal("220.00");
                    default -> new BigDecimal("150.00");
                })
                .orElse(new BigDecimal("150.00"));
    }

    private String getSeatLabel(Long seatId) {
        return seatRepository.findById(seatId)
                .map(s -> s.getRowLabel() + s.getSeatNumber())
                .orElse("ID " + seatId);
    }
}
