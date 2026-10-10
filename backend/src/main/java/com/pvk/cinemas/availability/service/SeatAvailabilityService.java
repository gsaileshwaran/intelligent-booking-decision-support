package com.pvk.cinemas.availability.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.dto.UpdateSeatStatusRequest;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SeatAvailabilityService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final AuditLogService auditLogService;
    private final SeatPricingService seatPricingService;
    private final com.pvk.cinemas.catalogue.repository.MovieRepository movieRepository;
    private final com.pvk.cinemas.catalogue.repository.MovieLanguageRepository movieLanguageRepository;
    private final com.pvk.cinemas.catalogue.service.MovieLifecycleService movieLifecycleService;
    private final com.pvk.cinemas.scheduling.service.DeterministicInventoryEngine deterministicInventoryEngine;

    public SeatAvailabilityService(ShowRepository showRepository,
                                   ShowSeatRepository showSeatRepository,
                                   ScreenCapabilityRepository screenCapabilityRepository,
                                   ScreenRepository screenRepository,
                                   TheatreRepository theatreRepository,
                                   SeatRepository seatRepository,
                                   SeatTypeRepository seatTypeRepository,
                                   AuditLogService auditLogService,
                                   SeatPricingService seatPricingService,
                                   com.pvk.cinemas.catalogue.repository.MovieRepository movieRepository,
                                   com.pvk.cinemas.catalogue.repository.MovieLanguageRepository movieLanguageRepository,
                                   com.pvk.cinemas.catalogue.service.MovieLifecycleService movieLifecycleService,
                                   com.pvk.cinemas.scheduling.service.DeterministicInventoryEngine deterministicInventoryEngine) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.auditLogService = auditLogService;
        this.seatPricingService = seatPricingService;
        this.movieRepository = movieRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.movieLifecycleService = movieLifecycleService;
        this.deterministicInventoryEngine = deterministicInventoryEngine;
    }

    /**
     * CHANGE-API-004 / DECISION-007 / PART 3 (§8.2):
     * Seat availability viewer with Just-In-Time (JIT) materialization for advance schedules.
     */
    @Transactional
    public ShowSeatAvailabilityResponse getShowSeatAvailability(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        ScreenCapability sc = screenCapabilityRepository.findById(show.getScreenCapabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen capability not found: " + show.getScreenCapabilityId()));

        Screen screen = screenRepository.findById(sc.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + sc.getScreenId()));

        Theatre theatre = theatreRepository.findById(screen.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + screen.getTheatreId()));

        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(showId);
        if (showSeats.isEmpty()) {
            showSeats = materializeShowSeatsJit(show, screen, theatre);
        }
        List<Long> seatIds = showSeats.stream().map(ss -> ss.getId().getSeatId()).toList();
        Map<Long, Seat> seatMap = seatRepository.findAllById(seatIds).stream()
                .collect(Collectors.toMap(Seat::getSeatId, Function.identity()));

        int availableCount = 0;
        int bookedCount = 0;
        int blockedCount = 0;

        SeatPricingService.ShowPricingContext showCtx = seatPricingService != null 
                ? seatPricingService.getShowPricingContext(showId) : null;

        List<ShowSeatAvailabilityResponse.SeatAvailabilityDetail> details = new ArrayList<>();
        for (ShowSeat ss : showSeats) {
            Seat seat = seatMap.get(ss.getId().getSeatId());
            if (seat == null) continue;

            String status = ss.getAvailabilityStatus();
            // Physical seat maintenance override (§14-15): physical seat status overrides show seat availability
            if (seat.getStatus() != null && !"ACTIVE".equalsIgnoreCase(seat.getStatus())) {
                status = "BLOCKED";
            }

            if ("AVAILABLE".equalsIgnoreCase(status)) availableCount++;
            else if ("BOOKED".equalsIgnoreCase(status)) bookedCount++;
            else if ("BLOCKED".equalsIgnoreCase(status)) blockedCount++;

            String seatTypeName = seatTypeRepository.findById(seat.getSeatTypeId())
                    .map(st -> st.getSeatTypeCode()).orElse("STANDARD");

            BigDecimal price = seatPricingService != null 
                    ? seatPricingService.calculateSeatPrice(seat, showCtx)
                    : new BigDecimal("150.00");

            details.add(new ShowSeatAvailabilityResponse.SeatAvailabilityDetail(
                    seat.getSeatId(),
                    seat.getRowLabel(),
                    seat.getSeatNumber(),
                    seatTypeName,
                    status,
                    seat.getPricingZone(),
                    price,
                    seat.getGridRowIndex(),
                    seat.getGridColIndex(),
                    seat.getAisleAfter()
            ));
        }

        ShowSeatAvailabilityResponse response = new ShowSeatAvailabilityResponse();
        response.setShowId(showId);
        response.setTheatreId(theatre.getTheatreId());
        response.setTheatreName(theatre.getTheatreName());
        response.setScreenId(screen.getScreenId());
        response.setScreenName(screen.getScreenName());
        response.setTotalSeats(details.size());
        response.setAvailableSeats(availableCount);
        response.setBookedSeats(bookedCount);
        response.setBlockedSeats(blockedCount);
        response.setSeats(details);

        return response;
    }

    /**
     * Theatre Manager availability override (e.g. marking seats BLOCKED for maintenance).
     */
    @Transactional
    public ShowSeatAvailabilityResponse overrideSeatStatus(Long showId, UpdateSeatStatusRequest request, Long actorUserId, String ipAddress) {
        showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        String newStatus = request.getAvailabilityStatus().trim().toUpperCase();
        for (Long seatId : request.getSeatIds()) {
            ShowSeat ss = showSeatRepository.findByIdShowIdAndIdSeatId(showId, seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("ShowSeat not found for show " + showId + " and seat " + seatId));
            ss.setAvailabilityStatus(newStatus);
            ss.setUpdatedAt(Instant.now());
            showSeatRepository.save(ss);
        }

        auditLogService.logAction(actorUserId, "SEAT_STATUS_OVERRIDE", "SHOW_SEAT", String.valueOf(showId),
                "Overrode " + request.getSeatIds().size() + " seats to status " + newStatus, ipAddress);

        return getShowSeatAvailability(showId);
    }

    /**
     * Just-In-Time (JIT) materialization for advance schedules (§8.2).
     * Invokes canonical pure functions F and G to generate deterministic SHOW_SEAT rows.
     */
    @Transactional
    public List<ShowSeat> materializeShowSeatsJit(Show show, Screen screen, Theatre theatre) {
        List<Seat> physicalSeats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(screen.getScreenId());
        if (physicalSeats.isEmpty()) {
            physicalSeats = seatRepository.findByScreenId(screen.getScreenId());
        }
        if (physicalSeats.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        if (deterministicInventoryEngine != null && movieLanguageRepository != null && movieRepository != null && movieLifecycleService != null) {
            var mlOpt = movieLanguageRepository.findById(show.getMovieLanguageId());
            if (mlOpt.isPresent()) {
                var movieOpt = movieRepository.findById(mlOpt.get().getMovieId());
                if (movieOpt.isPresent()) {
                    var movie = movieOpt.get();
                    java.time.LocalDate sDate = show.getShowDate();
                    java.time.DayOfWeek dow = sDate != null ? sDate.getDayOfWeek() : java.time.DayOfWeek.FRIDAY;
                    String dayType = (dow == java.time.DayOfWeek.FRIDAY || dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY)
                            ? "WEEKEND" : "WEEKDAY";
                    double rho = movieLifecycleService.calculateRho(movie.getExhibitionStartDate(), movie.getExhibitionEndDate(), sDate);
                    List<ShowSeat> generated = deterministicInventoryEngine.generateShowSeats(
                            show.getShowId(),
                            sDate,
                            physicalSeats,
                            movie.getDemandTier(),
                            movie.getPopularityScore(),
                            rho,
                            dayType,
                            theatre.getTheatreTier()
                    );
                    return showSeatRepository.saveAll(generated);
                }
            }
        }

        List<ShowSeat> fallbackSeats = new ArrayList<>(physicalSeats.size());
        for (Seat seat : physicalSeats) {
            String status = ("BLOCKED".equalsIgnoreCase(seat.getStatus()) || "INACTIVE".equalsIgnoreCase(seat.getStatus()))
                    ? "BLOCKED" : "AVAILABLE";
            fallbackSeats.add(new ShowSeat(show.getShowId(), seat.getSeatId(), status));
        }
        return showSeatRepository.saveAll(fallbackSeats);
    }
}
