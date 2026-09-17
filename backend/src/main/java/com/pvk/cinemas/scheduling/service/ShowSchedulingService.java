package com.pvk.cinemas.scheduling.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.InvalidCapabilityException;
import com.pvk.cinemas.common.exceptions.InvalidMovieLanguageException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.common.exceptions.ShowOverlapException;
import org.springframework.security.access.AccessDeniedException;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.dto.ShowRequest;
import com.pvk.cinemas.scheduling.dto.ShowResponse;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowSchedulingService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ScreenRepository screenRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final MovieRepository movieRepository;
    private final MovieLanguageRepository movieLanguageRepository;
    private final LanguageRepository languageRepository;
    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final SeatRepository seatRepository;
    private final AuditLogService auditLogService;
    private final PresentationFormatRepository presentationFormatRepository;
    private final SeatPricingService seatPricingService;

    public ShowSchedulingService(ShowRepository showRepository,
                                 ShowSeatRepository showSeatRepository,
                                 ScreenRepository screenRepository,
                                 ScreenCapabilityRepository screenCapabilityRepository,
                                 MovieRepository movieRepository,
                                 MovieLanguageRepository movieLanguageRepository,
                                 LanguageRepository languageRepository,
                                 TheatreRepository theatreRepository,
                                 CityRepository cityRepository,
                                 SeatRepository seatRepository,
                                 AuditLogService auditLogService,
                                 PresentationFormatRepository presentationFormatRepository,
                                 SeatPricingService seatPricingService) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.screenRepository = screenRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.movieRepository = movieRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.languageRepository = languageRepository;
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.seatRepository = seatRepository;
        this.auditLogService = auditLogService;
        this.presentationFormatRepository = presentationFormatRepository;
        this.seatPricingService = seatPricingService;
    }

    /**
     * DECISION-019 / BR-001, BR-002, BR-003, BR-004:
     * Atomically validates, acquires pessimistic write lock on target Screen,
     * verifies non-overlap, inserts SHOW, and bulk initializes all physical screen seats in SHOW_SEAT
     * within the SAME transaction.
     */
    @Transactional
    public ShowResponse createShow(Integer theatreId, ShowRequest request, Long actorUserId, String ipAddress) {
        // 1. Chronological sanity (BR-001)
        if (request.getStartAt() == null || request.getEndAt() == null || !request.getEndAt().isAfter(request.getStartAt())) {
            throw new BadRequestException("Show end time must be strictly greater than start time (BR-001)");
        }

        // 2. Screen existence, theatre association, and Pessimistic Write Lock (BR-004)
        Long screenId = request.getScreenId() != null ? request.getScreenId().longValue() : null;
        Screen screen = screenRepository.findByIdWithPessimisticLock(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + request.getScreenId()));
        if (theatreId == null || screen.getTheatreId() == null || screen.getTheatreId().longValue() != theatreId.longValue()) {
            throw new BadRequestException("Screen " + request.getScreenId() + " does not belong to theatre " + theatreId);
        }

        // 4. Movie Language verification (BR-002)
        MovieLanguage movieLanguage = movieLanguageRepository.findById(request.getMovieLanguageId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie Language not found: " + request.getMovieLanguageId()));
        if (request.getMovieId() == null || movieLanguage.getMovieId() == null || movieLanguage.getMovieId().longValue() != request.getMovieId().longValue()) {
            throw new InvalidMovieLanguageException("Movie language track does not belong to selected movie (BR-002)");
        }

        // 5. Screen Capability verification (BR-003)
        Long screenCapId = request.getScreenCapabilityId() != null ? request.getScreenCapabilityId().longValue() : null;
        ScreenCapability capability = screenCapabilityRepository.findById(screenCapId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen Capability not found: " + request.getScreenCapabilityId()));
        if (screenId == null || capability.getScreenId() == null || capability.getScreenId().longValue() != screenId.longValue()) {
            throw new InvalidCapabilityException("Screen capability does not belong to selected screen (BR-003)");
        }

        // 6. Non-overlapping active show verification (BR-004)
        long overlappingCount = showRepository.countOverlappingShows(screenId, request.getStartAt(), request.getEndAt());
        if (overlappingCount > 0) {
            throw new ShowOverlapException("Conflicting active show exists on screen " + request.getScreenId() + " for requested timing (BR-004)");
        }

        // 7. Insert SHOW
        Show show = new Show();
        show.setMovieLanguageId(movieLanguage.getMovieLanguageId());
        show.setScreenCapabilityId(screenCapId);
        show.setStartAt(request.getStartAt());
        show.setEndAt(request.getEndAt());
        show.setShowStatus(request.getShowStatus() != null ? request.getShowStatus().toUpperCase() : "SCHEDULED");
        show.setCreatedAt(Instant.now());
        show.setUpdatedAt(Instant.now());
        show = showRepository.save(show);

        // 8. Bulk Initialize SHOW_SEAT for all active physical seats on screen (DECISION-019)
        List<Seat> physicalSeats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(screenId);
        if (physicalSeats.isEmpty()) {
            // Screen has no seats yet; check all seats or generate for any available
            physicalSeats = seatRepository.findByScreenId(screenId);
        }

        List<ShowSeat> showSeats = new ArrayList<>();
        for (Seat seat : physicalSeats) {
            showSeats.add(new ShowSeat(show.getShowId(), seat.getSeatId(), "AVAILABLE"));
        }
        showSeatRepository.saveAll(showSeats);

        // 9. Audit log
        auditLogService.logAction(actorUserId, "SHOW_CREATE", "SHOW", String.valueOf(show.getShowId()), "Scheduled show with " + showSeats.size() + " seats initialized", ipAddress);

        return mapToResponse(show);
    }

    @Transactional
    public ShowResponse updateShow(Integer theatreId, Long showId, ShowRequest request, Long actorUserId, String ipAddress) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        ScreenCapability sc = screenCapabilityRepository.findById(show.getScreenCapabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Capability not found"));
        Screen screen = screenRepository.findById(sc.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        if (theatreId != null && !screen.getTheatreId().equals(theatreId.longValue())) {
            throw new AccessDeniedException("Show does not belong to specified theatre");
        }

        if (request.getStartAt() != null && request.getEndAt() != null) {
            if (!request.getEndAt().isAfter(request.getStartAt())) {
                throw new BadRequestException("Show end time must be after start time (BR-001)");
            }
            show.setStartAt(request.getStartAt());
            show.setEndAt(request.getEndAt());
        }

        if (request.getShowStatus() != null) {
            show.setShowStatus(request.getShowStatus().toUpperCase());
        }

        // Re-check overlap if timing changed
        if (request.getStartAt() != null && request.getEndAt() != null) {
            long overlaps = showRepository.countOverlappingShowsExcluding(sc.getScreenId(), showId, show.getStartAt(), show.getEndAt());
            if (overlaps > 0) {
                throw new ShowOverlapException("Conflicting show exists for updated timing (BR-004)");
            }
        }

        show.setUpdatedAt(Instant.now());
        show = showRepository.save(show);
        auditLogService.logAction(actorUserId, "SHOW_UPDATE", "SHOW", String.valueOf(showId), "Updated show timing/status", ipAddress);
        return mapToResponse(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForTheatre(Long theatreId) {
        if (theatreId == null) {
            return java.util.Collections.emptyList();
        }
        List<Screen> screens = screenRepository.findByTheatreIdAndIsActiveTrue(theatreId);
        List<Long> screenIds = screens.stream().map(Screen::getScreenId).toList();
        if (screenIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<ScreenCapability> capabilities = screenCapabilityRepository.findByScreenIdIn(screenIds);
        List<Long> capabilityIds = capabilities.stream().map(ScreenCapability::getScreenCapabilityId).toList();
        if (capabilityIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return showRepository.findByScreenCapabilityIdIn(capabilityIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForTheatre(Integer theatreId) {
        return theatreId != null ? getShowsForTheatre(theatreId.longValue()) : java.util.Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForMovie(Long movieId, Integer cityId) {
        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(movieId);
        List<Long> mlIds = mls.stream().map(MovieLanguage::getMovieLanguageId).toList();
        if (mlIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return showRepository.findByMovieLanguageIdIn(mlIds).stream()
                .map(this::mapToResponse)
                .filter(resp -> cityId == null || (resp.getCityId() != null && resp.getCityId().equals(cityId.longValue())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForMovie(Long movieId) {
        return getShowsForMovie(movieId, null);
    }

    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long showId) {
        return showRepository.findById(showId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
    }

    private ShowResponse mapToResponse(Show s) {
        ShowResponse resp = new ShowResponse();
        resp.setShowId(s.getShowId());
        resp.setMovieLanguageId(s.getMovieLanguageId());
        resp.setScreenCapabilityId(s.getScreenCapabilityId());
        resp.setStartAt(s.getStartAt());
        resp.setEndAt(s.getEndAt());
        resp.setShowStatus(s.getShowStatus());

        movieLanguageRepository.findById(s.getMovieLanguageId()).ifPresent(ml -> {
            resp.setMovieId(ml.getMovieId());
            movieRepository.findById(ml.getMovieId()).ifPresent(m -> {
                resp.setMovieTitle(m.getTitle());
                resp.setPosterUrl(m.getPosterUrl());
            });
            languageRepository.findById(ml.getLanguageId()).ifPresent(l -> resp.setLanguageName(l.getLanguageName()));
        });

        screenCapabilityRepository.findById(s.getScreenCapabilityId()).ifPresent(sc -> {
            resp.setScreenId(sc.getScreenId());
            if (presentationFormatRepository != null && sc.getPresentationFormatId() != null) {
                presentationFormatRepository.findById(sc.getPresentationFormatId()).ifPresent(pf -> {
                    resp.setFormatCode(pf.getFormatCode());
                    resp.setPresentationFormat(pf.getName());
                });
            }
            screenRepository.findById(sc.getScreenId()).ifPresent(scr -> {
                resp.setScreenName(scr.getScreenName());
                resp.setTheatreId(scr.getTheatreId());
                theatreRepository.findById(scr.getTheatreId()).ifPresent(th -> {
                    resp.setTheatreName(th.getTheatreName());
                    resp.setCityId(th.getCityId());
                    if (th.getCityId() != null) {
                        cityRepository.findById(th.getCityId().intValue()).ifPresent(c -> resp.setCityName(c.getCityName()));
                    }
                });
            });
        });

        // Availability calculation
        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(s.getShowId());
        resp.setTotalSeats(showSeats.size());
        int availCount = (int) showSeats.stream().filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus())).count();
        resp.setAvailableSeats(availCount);

        // Price calculation
        if (seatPricingService != null) {
            try {
                SeatPricingService.ShowPricingContext showCtx = seatPricingService.getShowPricingContext(s.getShowId());
                java.math.BigDecimal base = showCtx.venueBasePrice.add(showCtx.formatDelta);
                // Min starting ticket price (value zone offset)
                resp.setMinPrice(base.subtract(new java.math.BigDecimal("40.00")));
            } catch (Exception e) {
                resp.setMinPrice(new java.math.BigDecimal("160.00"));
            }
        } else {
            resp.setMinPrice(new java.math.BigDecimal("160.00"));
        }

        return resp;
    }
}
