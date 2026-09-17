package com.pvk.cinemas.infrastructure.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.booking.repository.BookingRepository;
import com.pvk.cinemas.booking.repository.BookingSeatRepository;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.infrastructure.dto.SeatRequest;
import com.pvk.cinemas.infrastructure.dto.SeatResponse;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final ScreenRepository screenRepository;
    private final AuditLogService auditLogService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public SeatService(SeatRepository seatRepository,
                       SeatTypeRepository seatTypeRepository,
                       ScreenRepository screenRepository,
                       AuditLogService auditLogService,
                       BookingRepository bookingRepository,
                       BookingSeatRepository bookingSeatRepository) {
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.screenRepository = screenRepository;
        this.auditLogService = auditLogService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    /**
     * Returns ALL physical seats on a screen (including BLOCKED ones) — used by the manager's
     * screen-wise seat management view. Customers use the show-availability endpoint which
     * applies the ACTIVE-only filter and the physical block override.
     */
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreen(Integer screenId) {
        screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));
        return seatRepository.findByScreenId(screenId.longValue()).stream()
                .sorted((a, b) -> {
                    int rowCmp = a.getRowLabel().compareTo(b.getRowLabel());
                    if (rowCmp != 0) return rowCmp;
                    // Natural seat number sort (numeric-aware)
                    try {
                        return Integer.compare(Integer.parseInt(a.getSeatNumber()), Integer.parseInt(b.getSeatNumber()));
                    } catch (NumberFormatException e) {
                        return a.getSeatNumber().compareTo(b.getSeatNumber());
                    }
                })
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public SeatResponse createSeat(Integer screenId, SeatRequest request, Long actorUserId, String ipAddress) {
        screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));
        seatTypeRepository.findById(request.getSeatTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat type not found: " + request.getSeatTypeId()));

        seatRepository.findByScreenIdAndRowLabelAndSeatNumber(screenId, request.getRowLabel().trim(), request.getSeatNumber().trim())
                .ifPresent(s -> { throw new ConflictException("Seat coordinate already exists: " + request.getRowLabel() + request.getSeatNumber()); });

        Seat seat = new Seat();
        seat.setScreenId(screenId);
        seat.setSeatTypeId(request.getSeatTypeId());
        seat.setRowLabel(request.getRowLabel().trim());
        seat.setSeatNumber(request.getSeatNumber().trim());
        seat.setGridRowIndex(request.getGridRowIndex());
        seat.setGridColIndex(request.getGridColIndex());
        // Resolve physical status: prefer explicit status field, else fall back to isActive
        String resolvedStatus = resolveStatus(request);
        seat.setStatus(resolvedStatus);
        seat = seatRepository.save(seat);

        auditLogService.logAction(actorUserId, "SEAT_CREATE", "SEAT", String.valueOf(seat.getSeatId()), "Created seat " + seat.getRowLabel() + seat.getSeatNumber(), ipAddress);
        return mapToResponse(seat);
    }

    @Transactional
    public SeatResponse updateSeat(Integer screenId, Long seatId, SeatRequest request, Long actorUserId, String ipAddress) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));

        if (screenId != null && !seat.getScreenId().equals(screenId.longValue())) {
            throw new AccessDeniedException("Seat does not belong to specified screen");
        }

        if (request.getSeatTypeId() != null) {
            seatTypeRepository.findById(request.getSeatTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seat type not found: " + request.getSeatTypeId()));
            seat.setSeatTypeId(request.getSeatTypeId());
        }
        if (request.getGridRowIndex() != null) seat.setGridRowIndex(request.getGridRowIndex());
        if (request.getGridColIndex() != null) seat.setGridColIndex(request.getGridColIndex());

        // Resolve physical status: prefer explicit status field, else fall back to isActive
        String prevStatus = seat.getStatus();
        String resolvedStatus = resolveStatus(request);
        if (resolvedStatus != null) {
            seat.setStatus(resolvedStatus);
        }

        seat = seatRepository.save(seat);

        // Compute upcoming bookings count when blocking a seat (for manager warning)
        int upcomingBookingsCount = 0;
        String warningMessage = null;
        if ("BLOCKED".equalsIgnoreCase(seat.getStatus()) && !"BLOCKED".equalsIgnoreCase(prevStatus)) {
            // Count active bookings that reference this seat across all upcoming shows
            upcomingBookingsCount = countUpcomingBookingsForSeat(seatId);
            if (upcomingBookingsCount > 0) {
                warningMessage = String.format(
                    "⚠ This seat has %d upcoming booking(s) that must be manually re-seated or refunded.",
                    upcomingBookingsCount);
            }
        }

        String reason = request.getBlockReason() != null ? request.getBlockReason() : "";
        String auditDetail = String.format("Status changed from %s to %s on seat %s%s%s",
                prevStatus, seat.getStatus(), seat.getRowLabel(), seat.getSeatNumber(),
                reason.isBlank() ? "" : " — Reason: " + reason);
        auditLogService.logAction(actorUserId, "SEAT_UPDATE", "SEAT", String.valueOf(seatId), auditDetail, ipAddress);

        SeatResponse resp = mapToResponse(seat);
        resp.setUpcomingBookingsCount(upcomingBookingsCount);
        resp.setWarningMessage(warningMessage);
        return resp;
    }

    @Transactional
    public SeatResponse updateSeat(Long seatId, SeatRequest request, Long actorUserId, String ipAddress) {
        return updateSeat(null, seatId, request, actorUserId, ipAddress);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String resolveStatus(SeatRequest request) {
        // Prefer explicit status string if supplied
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            return request.getStatus().toUpperCase().trim();
        }
        // Fall back to boolean isActive for backward compat
        if (request.getIsActive() != null) {
            return request.getIsActive() ? "ACTIVE" : "BLOCKED";
        }
        return null;
    }

    private int countUpcomingBookingsForSeat(Long seatId) {
        try {
            // Count bookings that have booking_seats referencing this physical seat
            // We scan all booking_seats for this seatId across CONFIRMED bookings
            return (int) bookingSeatRepository.findAll().stream()
                    .filter(bs -> seatId.equals(bs.getSeatId()))
                    .map(bs -> bookingRepository.findById(bs.getBookingId()).orElse(null))
                    .filter(b -> b != null && ("CONFIRMED".equalsIgnoreCase(b.getBookingStatus())
                            || "PENDING".equalsIgnoreCase(b.getBookingStatus())))
                    .count();
        } catch (Exception e) {
            return 0; // Non-fatal — warning is best-effort
        }
    }

    private SeatResponse mapToResponse(Seat s) {
        SeatResponse resp = new SeatResponse();
        resp.setSeatId(s.getSeatId());
        resp.setScreenId(s.getScreenId());
        resp.setSeatTypeId(s.getSeatTypeId());
        seatTypeRepository.findById(s.getSeatTypeId()).ifPresent(st -> resp.setSeatTypeCode(st.getSeatTypeCode()));
        resp.setRowLabel(s.getRowLabel());
        resp.setSeatNumber(s.getSeatNumber());
        resp.setGridRowIndex(s.getGridRowIndex());
        resp.setGridColIndex(s.getGridColIndex());
        resp.setStatus(s.getStatus());
        resp.setIsActive("ACTIVE".equalsIgnoreCase(s.getStatus()));
        resp.setPricingZone(s.getPricingZone());
        resp.setAisleAfter(s.getAisleAfter());
        return resp;
    }
}
