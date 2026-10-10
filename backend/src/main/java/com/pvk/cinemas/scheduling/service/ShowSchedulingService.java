package com.pvk.cinemas.scheduling.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.AudioFormatRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.common.exceptions.InvalidCapabilityException;
import com.pvk.cinemas.common.exceptions.InvalidMovieLanguageException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.common.exceptions.ShowOverlapException;
import org.springframework.security.access.AccessDeniedException;
import com.pvk.cinemas.security.TheatreScopeService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ShowSchedulingService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(IST_ZONE);

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
    private final AudioFormatRepository audioFormatRepository;
    private final SeatPricingService seatPricingService;
    private final BusinessDateProvider businessDateProvider;

    @Autowired
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
                                 AudioFormatRepository audioFormatRepository,
                                 SeatPricingService seatPricingService,
                                 BusinessDateProvider businessDateProvider) {
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
        this.audioFormatRepository = audioFormatRepository;
        this.seatPricingService = seatPricingService;
        this.businessDateProvider = businessDateProvider;
    }

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
                                 SeatPricingService seatPricingService,
                                 BusinessDateProvider businessDateProvider) {
        this(showRepository, showSeatRepository, screenRepository, screenCapabilityRepository,
             movieRepository, movieLanguageRepository, languageRepository, theatreRepository,
             cityRepository, seatRepository, auditLogService, presentationFormatRepository,
             null, seatPricingService, businessDateProvider);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TheatreScopeService theatreScopeService;

    public void setTheatreScopeService(TheatreScopeService theatreScopeService) {
        this.theatreScopeService = theatreScopeService;
    }

    /**
     * DECISION-019 / BR-001, BR-002, BR-003, BR-004:
     * Atomically validates, acquires pessimistic write lock on target Screen,
     * verifies non-overlap, inserts SHOW, and bulk initializes all physical screen seats in SHOW_SEAT
     * within the SAME transaction.
     */
    @Transactional
    public ShowResponse createShow(Integer theatreId, ShowRequest request, Long actorUserId, String ipAddress) {
        if (theatreScopeService != null) {
            theatreScopeService.checkTheatreScope(theatreId);
        }

        // 0. Calendar horizon validation (§3)
        if (request.getStartAt() != null && businessDateProvider != null) {
            LocalDate showDate = request.getStartAt().atZone(IST_ZONE).toLocalDate();
            businessDateProvider.validateCalendarRange(showDate);
        }

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

        // 3. Operating hours validation (§5.3)
        if (theatreRepository != null && screen.getTheatreId() != null) {
            theatreRepository.findById(screen.getTheatreId()).ifPresent(theatre -> {
                if (theatre.getOpeningTime() != null && theatre.getClosingTime() != null) {
                    LocalDate showDate = request.getStartAt().atZone(IST_ZONE).toLocalDate();
                    ZonedDateTime tOpen = showDate.atTime(theatre.getOpeningTime()).atZone(IST_ZONE);
                    ZonedDateTime tClose;
                    if (!theatre.getClosingTime().isAfter(theatre.getOpeningTime())) {
                        tClose = showDate.plusDays(1).atTime(theatre.getClosingTime()).atZone(IST_ZONE);
                    } else {
                        tClose = showDate.atTime(theatre.getClosingTime()).atZone(IST_ZONE);
                    }
                    if (request.getStartAt().isBefore(tOpen.toInstant()) || request.getEndAt().isAfter(tClose.toInstant())) {
                        throw new BadRequestException("Show timing violates theatre operating hours ("
                                + theatre.getOpeningTime() + " to " + theatre.getClosingTime() + ")");
                    }
                }
            });
        }

        // 4. Movie Language verification (BR-002) and Movie Lifecycle exhibition window (§5.1)
        MovieLanguage movieLanguage = movieLanguageRepository.findById(request.getMovieLanguageId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie Language not found: " + request.getMovieLanguageId()));
        if (request.getMovieId() == null || movieLanguage.getMovieId() == null || movieLanguage.getMovieId().longValue() != request.getMovieId().longValue()) {
            throw new InvalidMovieLanguageException("Movie language track does not belong to selected movie (BR-002)");
        }
        if (movieRepository != null && movieLanguage.getMovieId() != null) {
            movieRepository.findById(movieLanguage.getMovieId()).ifPresent(movie -> {
                if (movie.getExhibitionStartDate() != null && movie.getExhibitionEndDate() != null) {
                    LocalDate showDate = request.getStartAt().atZone(IST_ZONE).toLocalDate();
                    if (showDate.isBefore(movie.getExhibitionStartDate()) || showDate.isAfter(movie.getExhibitionEndDate())) {
                        throw new BadRequestException("Show date " + showDate + " is outside movie exhibition window ["
                                + movie.getExhibitionStartDate() + " to " + movie.getExhibitionEndDate() + "]");
                    }
                }
            });
        }

        // 5. Screen Capability verification (BR-003)
        Long screenCapId = request.getScreenCapabilityId() != null ? request.getScreenCapabilityId().longValue() : null;
        ScreenCapability capability = screenCapabilityRepository.findById(screenCapId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen Capability not found: " + request.getScreenCapabilityId()));
        if (screenId == null || capability.getScreenId() == null || capability.getScreenId().longValue() != screenId.longValue()) {
            throw new InvalidCapabilityException("Screen capability does not belong to selected screen (BR-003)");
        }

        // 5b. Presentation & Audio format compatibility validation (§11.1)
        if (request.getPresentationFormatId() != null) {
            if (presentationFormatRepository != null) {
                presentationFormatRepository.findById(request.getPresentationFormatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Presentation format not found: " + request.getPresentationFormatId()));
            }
            if (capability.getPresentationFormatId() == null || !capability.getPresentationFormatId().equals(request.getPresentationFormatId())) {
                throw new BadRequestException("Presentation format does not match selected screen capability");
            }
        }

        if (request.getAudioFormatId() != null) {
            if (audioFormatRepository != null) {
                audioFormatRepository.findById(request.getAudioFormatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Audio format not found: " + request.getAudioFormatId()));
            }
            if (capability.getAudioFormatId() == null || !capability.getAudioFormatId().equals(request.getAudioFormatId())) {
                throw new BadRequestException("Audio format does not match selected screen capability");
            }
        }

        // 6. Non-overlapping active show verification (BR-004)
        long overlappingCount = showRepository.countOverlappingShows(screenId, request.getStartAt(), request.getEndAt());
        if (overlappingCount > 0) {
            throw new ShowOverlapException("Conflicting active show exists on screen " + request.getScreenId() + " for requested timing (BR-004)");
        }

        // 6b. Turnaround buffer verification (§5.2)
        int turnaroundMinutes = 20;
        if (screen.getIsPremium() != null && screen.getIsPremium()) {
            turnaroundMinutes = 30;
        } else if (presentationFormatRepository != null && capability.getPresentationFormatId() != null) {
            var pfOpt = presentationFormatRepository.findById(capability.getPresentationFormatId());
            if (pfOpt.isPresent()) {
                String code = pfOpt.get().getFormatCode();
                if ("IMAX".equalsIgnoreCase(code) || "4DX".equalsIgnoreCase(code)) {
                    turnaroundMinutes = 30;
                }
            }
        }

        List<ScreenCapability> screenCaps = screenCapabilityRepository.findByScreenId(screenId);
        List<Long> allCapIds = screenCaps.isEmpty() ? List.of(capability.getScreenCapabilityId()) : screenCaps.stream().map(ScreenCapability::getScreenCapabilityId).toList();
        List<Show> activeShowsOnScreen = showRepository.findByScreenCapabilityIdInAndStatusIn(
                allCapIds,
                List.of("SCHEDULED", "OPEN")
        );
        for (Show existing : activeShowsOnScreen) {
            if (existing.getEndAt() != null && existing.getEndAt().isBefore(request.getStartAt())
                    && existing.getEndAt().plus(java.time.Duration.ofMinutes(turnaroundMinutes)).isAfter(request.getStartAt())) {
                throw new BadRequestException("Insufficient turnaround buffer of " + turnaroundMinutes + " minutes required between shows");
            }
            if (existing.getStartAt() != null && existing.getStartAt().isAfter(request.getEndAt())
                    && request.getEndAt().plus(java.time.Duration.ofMinutes(turnaroundMinutes)).isAfter(existing.getStartAt())) {
                throw new BadRequestException("Insufficient turnaround buffer of " + turnaroundMinutes + " minutes required between shows");
            }
        }

        // 7. Insert SHOW
        Show show = new Show();
        show.setMovieLanguageId(movieLanguage.getMovieLanguageId());
        show.setScreenCapabilityId(screenCapId);
        show.setScreenId(screenId);  // Part 3: physical screen identity (V10)
        show.setStartAt(request.getStartAt());
        show.setEndAt(request.getEndAt());
        // showDate is set via constructor derivation from startAt (IST) — V10 persisted column
        if (request.getStartAt() != null) {
            show.setShowDate(request.getStartAt().atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate());
        }
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
        if (theatreScopeService != null) {
            theatreScopeService.checkTheatreScope(theatreId);
            theatreScopeService.checkShowScope(showId, theatreId);
        }

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        ScreenCapability sc = screenCapabilityRepository.findById(show.getScreenCapabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Capability not found"));
        Screen screen = screenRepository.findById(sc.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        if (theatreId != null && !screen.getTheatreId().equals(theatreId.longValue())) {
            throw new AccessDeniedException("Show does not belong to specified theatre");
        }

        // Format compatibility validation (if provided in update request)
        if (request.getPresentationFormatId() != null) {
            if (presentationFormatRepository != null) {
                presentationFormatRepository.findById(request.getPresentationFormatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Presentation format not found: " + request.getPresentationFormatId()));
            }
            if (sc.getPresentationFormatId() == null || !sc.getPresentationFormatId().equals(request.getPresentationFormatId())) {
                throw new BadRequestException("Presentation format does not match selected screen capability");
            }
        }

        if (request.getAudioFormatId() != null) {
            if (audioFormatRepository != null) {
                audioFormatRepository.findById(request.getAudioFormatId())
                        .orElseThrow(() -> new ResourceNotFoundException("Audio format not found: " + request.getAudioFormatId()));
            }
            if (sc.getAudioFormatId() == null || !sc.getAudioFormatId().equals(request.getAudioFormatId())) {
                throw new BadRequestException("Audio format does not match selected screen capability");
            }
        }

        boolean timingChanged = (request.getStartAt() != null || request.getEndAt() != null);
        Instant effectiveStart = request.getStartAt() != null ? request.getStartAt() : show.getStartAt();
        Instant effectiveEnd = request.getEndAt() != null ? request.getEndAt() : show.getEndAt();

        if (timingChanged) {
            if (effectiveStart == null || effectiveEnd == null || !effectiveEnd.isAfter(effectiveStart)) {
                throw new BadRequestException("Show end time must be after start time (BR-001)");
            }

            LocalDate newShowDate = effectiveStart.atZone(IST_ZONE).toLocalDate();
            if (businessDateProvider != null) {
                businessDateProvider.validateCalendarRange(newShowDate);
            }

            // Movie Lifecycle exhibition window
            if (movieRepository != null && show.getMovieLanguageId() != null) {
                movieLanguageRepository.findById(show.getMovieLanguageId()).ifPresent(ml -> {
                    movieRepository.findById(ml.getMovieId()).ifPresent(movie -> {
                        if (movie.getExhibitionStartDate() != null && movie.getExhibitionEndDate() != null) {
                            if (newShowDate.isBefore(movie.getExhibitionStartDate()) || newShowDate.isAfter(movie.getExhibitionEndDate())) {
                                throw new BadRequestException("Show date " + newShowDate + " is outside movie exhibition window ["
                                        + movie.getExhibitionStartDate() + " to " + movie.getExhibitionEndDate() + "]");
                            }
                        }
                    });
                });
            }

            // Operating hours validation
            if (theatreRepository != null && screen.getTheatreId() != null) {
                theatreRepository.findById(screen.getTheatreId()).ifPresent(theatre -> {
                    if (theatre.getOpeningTime() != null && theatre.getClosingTime() != null) {
                        ZonedDateTime tOpen = newShowDate.atTime(theatre.getOpeningTime()).atZone(IST_ZONE);
                        ZonedDateTime tClose;
                        if (!theatre.getClosingTime().isAfter(theatre.getOpeningTime())) {
                            tClose = newShowDate.plusDays(1).atTime(theatre.getClosingTime()).atZone(IST_ZONE);
                        } else {
                            tClose = newShowDate.atTime(theatre.getClosingTime()).atZone(IST_ZONE);
                        }
                        if (effectiveStart.isBefore(tOpen.toInstant()) || effectiveEnd.isAfter(tClose.toInstant())) {
                            throw new BadRequestException("Show timing violates theatre operating hours ("
                                    + theatre.getOpeningTime() + " to " + theatre.getClosingTime() + ")");
                        }
                    }
                });
            }

            // Re-check overlap excluding current show
            long overlaps = showRepository.countOverlappingShowsExcluding(screen.getScreenId(), showId, effectiveStart, effectiveEnd);
            if (overlaps > 0) {
                throw new ShowOverlapException("Conflicting show exists for updated timing (BR-004)");
            }

            // Turnaround buffer verification
            int turnaroundMinutes = 20;
            if (screen.getIsPremium() != null && screen.getIsPremium()) {
                turnaroundMinutes = 30;
            } else if (presentationFormatRepository != null && sc.getPresentationFormatId() != null) {
                var pfOpt = presentationFormatRepository.findById(sc.getPresentationFormatId());
                if (pfOpt.isPresent()) {
                    String code = pfOpt.get().getFormatCode();
                    if ("IMAX".equalsIgnoreCase(code) || "4DX".equalsIgnoreCase(code)) {
                        turnaroundMinutes = 30;
                    }
                }
            }

            List<ScreenCapability> screenCaps = screenCapabilityRepository.findByScreenId(screen.getScreenId());
            List<Long> allCapIds = screenCaps.isEmpty() ? List.of(sc.getScreenCapabilityId()) : screenCaps.stream().map(ScreenCapability::getScreenCapabilityId).toList();
            List<Show> activeShowsOnScreen = showRepository.findByScreenCapabilityIdInAndStatusIn(
                    allCapIds,
                    List.of("SCHEDULED", "OPEN")
            );
            for (Show existing : activeShowsOnScreen) {
                if (existing.getShowId().equals(showId)) {
                    continue; // Exclude current show
                }
                if (existing.getEndAt() != null && existing.getEndAt().isBefore(effectiveStart)
                        && existing.getEndAt().plus(java.time.Duration.ofMinutes(turnaroundMinutes)).isAfter(effectiveStart)) {
                    throw new BadRequestException("Insufficient turnaround buffer of " + turnaroundMinutes + " minutes required between shows");
                }
                if (existing.getStartAt() != null && existing.getStartAt().isAfter(effectiveEnd)
                        && effectiveEnd.plus(java.time.Duration.ofMinutes(turnaroundMinutes)).isAfter(existing.getStartAt())) {
                    throw new BadRequestException("Insufficient turnaround buffer of " + turnaroundMinutes + " minutes required between shows");
                }
            }

            show.setStartAt(effectiveStart);
            show.setEndAt(effectiveEnd);
            show.setShowDate(newShowDate);
        }

        if (request.getShowStatus() != null) {
            show.setShowStatus(request.getShowStatus().toUpperCase());
        }

        show.setUpdatedAt(Instant.now());
        show = showRepository.save(show);
        auditLogService.logAction(actorUserId, "SHOW_UPDATE", "SHOW", String.valueOf(showId), "Updated show timing/status", ipAddress);
        return mapToResponse(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForTheatre(Long theatreId, LocalDate date) {
        if (date != null && businessDateProvider != null) {
            businessDateProvider.validateCalendarRange(date);
        }
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
                .filter(s -> {
                    if (date == null) return true;
                    if (s.getShowDate() != null) return s.getShowDate().equals(date);
                    if (s.getStartAt() == null) return false;
                    return s.getStartAt().atZone(IST_ZONE).toLocalDate().equals(date);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForTheatre(Long theatreId) {
        return getShowsForTheatre(theatreId, (LocalDate) null);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForTheatre(Integer theatreId, LocalDate date) {
        return theatreId != null ? getShowsForTheatre(theatreId.longValue(), date) : java.util.Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForTheatre(Integer theatreId) {
        return getShowsForTheatre(theatreId, (LocalDate) null);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForMovie(Long movieId, Integer cityId, LocalDate date) {
        if (date != null && businessDateProvider != null) {
            businessDateProvider.validateCalendarRange(date);
        }
        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(movieId);
        List<Long> mlIds = mls.stream().map(MovieLanguage::getMovieLanguageId).toList();
        if (mlIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return showRepository.findByMovieLanguageIdIn(mlIds).stream()
                .filter(s -> {
                    if (date == null) return true;
                    if (s.getShowDate() != null) return s.getShowDate().equals(date);
                    if (s.getStartAt() == null) return false;
                    return s.getStartAt().atZone(IST_ZONE).toLocalDate().equals(date);
                })
                .map(this::mapToResponse)
                .filter(resp -> cityId == null || (resp.getCityId() != null && resp.getCityId().equals(cityId.longValue())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForMovie(Long movieId, Integer cityId) {
        return getShowsForMovie(movieId, cityId, null);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsForMovie(Long movieId) {
        return getShowsForMovie(movieId, null, null);
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
            languageRepository.findById(ml.getLanguageId()).ifPresent(l -> {
                resp.setLanguageName(l.getLanguageName());
                resp.setLanguage(l.getLanguageName());
            });
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
        resp.setAvailableSeatCount(availCount);

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
        resp.setStartingPrice(resp.getMinPrice());

        if (s.getStartAt() != null) {
            ZonedDateTime zdt = s.getStartAt().atZone(IST_ZONE);
            LocalDate showDate = zdt.toLocalDate();
            resp.setShowDate(showDate.toString());
            resp.setStartTime(TIME_FORMATTER.format(s.getStartAt()).toUpperCase(Locale.ENGLISH));

            boolean isPast = businessDateProvider != null && businessDateProvider.isPast(showDate);
            boolean isBookable = businessDateProvider == null || businessDateProvider.isBookable(showDate);
            resp.setBookable(isBookable);
            if (isPast) {
                resp.setBookingEligibility("SHOW_DATE_IN_PAST");
            } else if (isBookable) {
                resp.setBookingEligibility("BOOKABLE");
            } else {
                resp.setBookingEligibility("OUTSIDE_DEMO_WINDOW");
            }
        }

        return resp;
    }
}
