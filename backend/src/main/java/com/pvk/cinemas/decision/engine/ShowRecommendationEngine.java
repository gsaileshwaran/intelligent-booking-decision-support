package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.model.SeatHold;
import com.pvk.cinemas.booking.repository.SeatHoldRepository;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.booking.service.SeatPricingService;
import com.pvk.cinemas.catalogue.model.AudioFormat;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.AudioFormatRepository;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.catalogue.service.MovieLifecycleService;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import com.pvk.cinemas.decision.dto.SeatFitResult;
import com.pvk.cinemas.decision.dto.ShowCandidateDTO;
import com.pvk.cinemas.decision.dto.ShowRecommendationRequest;
import com.pvk.cinemas.decision.dto.ShowRecommendationResponse;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.scheduling.service.DeterministicInventoryEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Show Recommendation Engine — PVK Cinemas Decision Support.
 *
 * Implements authoritative multi-criteria decision ranking across candidate shows for a movie.
 *
 * Requirements:
 * 1. Hard constraints determine eligibility (city, date window, language, format/capability, time category, party size, budget).
 * 2. Multi-select filters use OR semantics within category, combined with AND across categories.
 * 3. Exact seat inventory evaluated using existing deterministic inventory engine when show_seat is unmaterialized.
 * 4. Active holds excluded from bookable availability.
 * 5. Budget enforced per-person and party total using exact BigDecimal currency.
 * 6. Ranking priorities (BEST_PRICE, BEST_SEATS, BEST_TIME, BALANCED) apply normalized weights and deterministic tie-breakers.
 * 7. Explainable reasons and accurate diagnostic exclusion counts returned.
 */
@Component
public class ShowRecommendationEngine {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a").withZone(IST_ZONE);

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MovieRepository movieRepository;
    private final MovieLanguageRepository movieLanguageRepository;
    private final LanguageRepository languageRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final PresentationFormatRepository presentationFormatRepository;
    private final AudioFormatRepository audioFormatRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final SeatHoldService seatHoldService;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatScoringEngine seatScoringEngine;
    private final SeatPricingService seatPricingService;
    private final SeatGroupPlanner seatGroupPlanner;
    private final BusinessDateProvider businessDateProvider;
    private final DeterministicInventoryEngine deterministicInventoryEngine;
    private final MovieLifecycleService movieLifecycleService;

    @Autowired
    public ShowRecommendationEngine(ShowRepository showRepository,
                                    ShowSeatRepository showSeatRepository,
                                    MovieRepository movieRepository,
                                    MovieLanguageRepository movieLanguageRepository,
                                    LanguageRepository languageRepository,
                                    ScreenCapabilityRepository screenCapabilityRepository,
                                    PresentationFormatRepository presentationFormatRepository,
                                    @Autowired(required = false) AudioFormatRepository audioFormatRepository,
                                    ScreenRepository screenRepository,
                                    SeatRepository seatRepository,
                                    TheatreRepository theatreRepository,
                                    CityRepository cityRepository,
                                    SeatHoldService seatHoldService,
                                    @Autowired(required = false) SeatHoldRepository seatHoldRepository,
                                    SeatScoringEngine seatScoringEngine,
                                    SeatPricingService seatPricingService,
                                    SeatGroupPlanner seatGroupPlanner,
                                    BusinessDateProvider businessDateProvider,
                                    @Autowired(required = false) DeterministicInventoryEngine deterministicInventoryEngine,
                                    @Autowired(required = false) MovieLifecycleService movieLifecycleService) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.movieRepository = movieRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.languageRepository = languageRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.presentationFormatRepository = presentationFormatRepository;
        this.audioFormatRepository = audioFormatRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.seatHoldService = seatHoldService;
        this.seatHoldRepository = seatHoldRepository;
        this.seatScoringEngine = seatScoringEngine;
        this.seatPricingService = seatPricingService;
        this.seatGroupPlanner = seatGroupPlanner;
        this.businessDateProvider = businessDateProvider;
        this.deterministicInventoryEngine = deterministicInventoryEngine;
        this.movieLifecycleService = movieLifecycleService;
    }

    /**
     * Backward-compatible constructor for existing tests.
     */
    public ShowRecommendationEngine(ShowRepository showRepository,
                                    ShowSeatRepository showSeatRepository,
                                    MovieRepository movieRepository,
                                    MovieLanguageRepository movieLanguageRepository,
                                    LanguageRepository languageRepository,
                                    ScreenCapabilityRepository screenCapabilityRepository,
                                    PresentationFormatRepository presentationFormatRepository,
                                    ScreenRepository screenRepository,
                                    SeatRepository seatRepository,
                                    TheatreRepository theatreRepository,
                                    CityRepository cityRepository,
                                    SeatHoldService seatHoldService,
                                    SeatScoringEngine seatScoringEngine,
                                    SeatPricingService seatPricingService,
                                    SeatGroupPlanner seatGroupPlanner,
                                    BusinessDateProvider businessDateProvider) {
        this(showRepository, showSeatRepository, movieRepository, movieLanguageRepository,
                languageRepository, screenCapabilityRepository, presentationFormatRepository,
                null, screenRepository, seatRepository, theatreRepository, cityRepository,
                seatHoldService, null, seatScoringEngine, seatPricingService, seatGroupPlanner,
                businessDateProvider, null, null);
    }

    public ShowRecommendationResponse recommendShows(Long movieId, ShowRecommendationRequest request) {
        if (request == null) {
            request = new ShowRecommendationRequest();
        }

        Movie movie = movieRepository.findById(movieId).orElse(null);
        String movieTitle = movie != null ? movie.getTitle() : "Unknown Movie";

        Long cityId = request.getCityId();
        String cityName = "All Cities";
        if (cityId != null) {
            cityName = cityRepository.findById(cityId.intValue())
                    .map(City::getCityName)
                    .orElse("City #" + cityId);
        }

        // =====================================================================
        // DATE RANGE & 7-DAY BOOKING WINDOW VALIDATION
        // =====================================================================
        LocalDate windowStart = businessDateProvider != null
                ? businessDateProvider.getBookingWindowStart()
                : LocalDate.now(IST_ZONE);
        LocalDate windowEnd = businessDateProvider != null
                ? businessDateProvider.getBookingWindowEnd()
                : windowStart.plusDays(6);

        LocalDate dateFrom = request.getDateFrom();
        LocalDate dateTo = request.getDateTo();

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                    Collections.emptyList(), "Invalid date range: 'from' date cannot be after 'to' date.");
        }

        if (dateTo != null && dateTo.isBefore(windowStart)) {
            return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                    Collections.emptyList(), String.format("The requested date range (%s to %s) is in the past. Shows can only be booked within the rolling 7-day window (%s to %s).",
                            dateFrom != null ? dateFrom : "start", dateTo, windowStart, windowEnd));
        }

        if (dateFrom != null && dateFrom.isAfter(windowEnd)) {
            return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                    Collections.emptyList(), String.format("The requested date range (%s to %s) is beyond the rolling 7-day booking window (%s to %s).",
                            dateFrom, dateTo != null ? dateTo : "end", windowStart, windowEnd));
        }

        LocalDate effectiveDateFrom = (dateFrom != null && dateFrom.isAfter(windowStart)) ? dateFrom : windowStart;
        LocalDate effectiveDateTo = (dateTo != null && dateTo.isBefore(windowEnd)) ? dateTo : windowEnd;

        // =====================================================================
        // HARD CONSTRAINT: Language Filter (Multi-Select OR semantics)
        // =====================================================================
        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(movieId);
        Map<Long, Language> languageMap = new HashMap<>();
        if (languageRepository != null) {
            try {
                languageRepository.findAll().forEach(l -> languageMap.put(l.getLanguageId(), l));
            } catch (Exception ignored) {}
        }

        Map<Long, String> mlIdToLangName = new HashMap<>();
        for (MovieLanguage ml : mls) {
            Language lang = languageMap.get(ml.getLanguageId());
            if (lang == null && languageRepository != null && ml.getLanguageId() != null) {
                lang = languageRepository.findById(ml.getLanguageId()).orElse(null);
                if (lang != null) languageMap.put(lang.getLanguageId(), lang);
            }
            if (lang != null) mlIdToLangName.put(ml.getMovieLanguageId(), lang.getName());
        }

        List<String> reqLanguages = request.getEffectiveLanguages();
        if (!reqLanguages.isEmpty()) {
            Set<Long> matchedLanguageIds = new HashSet<>();
            for (String reqL : reqLanguages) {
                String norm = reqL.trim().toLowerCase(Locale.ENGLISH);
                boolean matched = false;
                for (Language l : languageMap.values()) {
                    if (l.getLanguageCode().equalsIgnoreCase(norm) || l.getName().toLowerCase(Locale.ENGLISH).contains(norm)) {
                        matchedLanguageIds.add(l.getLanguageId());
                        matched = true;
                    }
                }
                if (!matched && languageRepository != null) {
                    Language l = languageRepository.findByLanguageCode(norm).orElse(null);
                    if (l != null) {
                        languageMap.put(l.getLanguageId(), l);
                        matchedLanguageIds.add(l.getLanguageId());
                    }
                }
            }

            List<MovieLanguage> filteredMls = mls.stream()
                    .filter(ml -> matchedLanguageIds.contains(ml.getLanguageId()))
                    .toList();

            if (filteredMls.isEmpty()) {
                String availableLangs = mls.stream()
                        .map(ml -> {
                            Language l = languageMap.get(ml.getLanguageId());
                            return l != null ? l.getName() : "Standard";
                        })
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining(", "));
                String msg = String.format("No shows available for '%s' in requested language(s): %s. Available languages for this movie: %s.",
                        movieTitle, String.join(", ", reqLanguages), availableLangs.isEmpty() ? "None" : availableLangs);
                ShowRecommendationResponse resp = new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null, Collections.emptyList(), msg);
                resp.setExcludedLanguage(mls.size());
                return resp;
            }
            mls = filteredMls;
        }

        List<Long> mlIds = mls.stream().map(MovieLanguage::getMovieLanguageId).toList();
        if (mlIds.isEmpty()) {
            return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                    Collections.emptyList(), "No scheduled shows found for this movie.");
        }

        // =====================================================================
        // CANDIDATE SHOW RETRIEVAL
        // =====================================================================
        List<Show> candidateShows = showRepository.findByMovieLanguageIdIn(mlIds).stream()
                .filter(s -> "SCHEDULED".equalsIgnoreCase(s.getShowStatus())
                        || "OPEN".equalsIgnoreCase(s.getShowStatus()))
                .toList();

        Map<Long, Show> distinctShowsMap = new LinkedHashMap<>();
        for (Show s : candidateShows) {
            distinctShowsMap.putIfAbsent(s.getShowId(), s);
        }
        candidateShows = new ArrayList<>(distinctShowsMap.values());

        int initialCandidatesCount = candidateShows.size();

        // Filter by effective date window
        candidateShows = candidateShows.stream().filter(s -> {
            LocalDate showDate = s.getShowDate() != null ? s.getShowDate() : s.getStartAt().atZone(IST_ZONE).toLocalDate();
            return !showDate.isBefore(effectiveDateFrom) && !showDate.isAfter(effectiveDateTo);
        }).toList();

        int excludedDate = initialCandidatesCount - candidateShows.size();

        if (candidateShows.isEmpty()) {
            String msg = String.format("No shows found for '%s' in the selected date range (%s – %s).",
                    movieTitle, effectiveDateFrom, effectiveDateTo);
            ShowRecommendationResponse resp = new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null, Collections.emptyList(), msg);
            resp.setTotalShowsEvaluated(initialCandidatesCount);
            resp.setExcludedDate(excludedDate);
            return resp;
        }

        // Preload metadata
        Map<Long, ScreenCapability> scMap = screenCapabilityRepository.findAll().stream()
                .collect(Collectors.toMap(ScreenCapability::getScreenCapabilityId, sc -> sc, (a, b) -> a));
        Map<Long, Screen> screenMap = screenRepository.findAll().stream()
                .collect(Collectors.toMap(Screen::getScreenId, s -> s, (a, b) -> a));
        Map<Long, Theatre> theatreMap = theatreRepository.findAll().stream()
                .collect(Collectors.toMap(Theatre::getTheatreId, t -> t, (a, b) -> a));
        Map<Long, PresentationFormat> formatMap = presentationFormatRepository.findAll().stream()
                .collect(Collectors.toMap(PresentationFormat::getPresentationFormatId, f -> f, (a, b) -> a));
        Map<Long, AudioFormat> audioFormatMap = audioFormatRepository != null
                ? audioFormatRepository.findAll().stream().collect(Collectors.toMap(AudioFormat::getAudioFormatId, a -> a, (a, b) -> a))
                : Collections.emptyMap();
        Map<Long, List<Seat>> screenSeatsCache = new java.util.concurrent.ConcurrentHashMap<>();

        List<ShowCandidateDTO> candidates = new ArrayList<>();
        int excludedCity = 0;
        int excludedFormat = 0;
        int excludedTime = 0;
        int excludedSeats = 0;
        int excludedBudget = 0;

        List<String> reqFormats = request.getEffectiveFormats();
        List<String> reqTimes = request.getEffectiveTimes();
        BigDecimal partyBudget = request.getEffectivePartyBudget(request.getPartySize());

        for (Show show : candidateShows) {
            ScreenCapability sc = scMap.get(show.getScreenCapabilityId());
            if (sc == null) sc = screenCapabilityRepository.findById(show.getScreenCapabilityId()).orElse(null);
            if (sc == null) continue;

            Screen screen = screenMap.get(sc.getScreenId());
            if (screen == null) screen = screenRepository.findById(sc.getScreenId()).orElse(null);
            if (screen == null) continue;

            Theatre theatre = theatreMap.get(screen.getTheatreId());
            if (theatre == null) theatre = theatreRepository.findById(screen.getTheatreId()).orElse(null);
            if (theatre == null) continue;

            // 1. City / Multiplex Scope
            if (cityId != null && (theatre.getCityId() == null || !theatre.getCityId().equals(cityId))) {
                excludedCity++;
                continue;
            }
            if (request.getTheatreId() != null && !theatre.getTheatreId().equals(request.getTheatreId())) {
                excludedCity++;
                continue;
            }

            // 2. Format & Audio Capability Filter (Multi-Select OR semantics)
            PresentationFormat pf = formatMap.get(sc.getPresentationFormatId());
            if (pf == null && sc.getPresentationFormatId() != null && presentationFormatRepository != null) {
                pf = presentationFormatRepository.findById(sc.getPresentationFormatId()).orElse(null);
            }
            AudioFormat af = audioFormatMap.get(sc.getAudioFormatId());
            if (af == null && sc.getAudioFormatId() != null && audioFormatRepository != null) {
                af = audioFormatRepository.findById(sc.getAudioFormatId()).orElse(null);
            }
            String formatDisplay = resolveFormatDisplay(pf, af);

            if (!reqFormats.isEmpty() && !matchesFormat(pf, af, reqFormats)) {
                excludedFormat++;
                continue;
            }

            // 3. Preferred Time Filter (Multi-Select OR semantics)
            ZonedDateTime showZdt = show.getStartAt().atZone(IST_ZONE);
            String showTimeCategory = getTimeCategory(showZdt.getHour());
            if (!reqTimes.isEmpty() && !matchesTime(showTimeCategory, reqTimes)) {
                excludedTime++;
                continue;
            }

            // 4. Exact Seat Inventory & Availability Check (JIT Deterministic Fallback)
            List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(show.getShowId());
            if (showSeats.isEmpty() && deterministicInventoryEngine != null && movie != null) {
                List<Seat> physicalSeats = screenSeatsCache.computeIfAbsent(screen.getScreenId(),
                        id -> seatRepository.findByScreenIdAndIsActiveTrueOrderByRowLabelAscSeatNumberAsc(id));
                if (physicalSeats.isEmpty()) physicalSeats = seatRepository.findByScreenId(screen.getScreenId());

                if (!physicalSeats.isEmpty()) {
                    LocalDate sDate = show.getShowDate() != null ? show.getShowDate() : showZdt.toLocalDate();
                    DayOfWeek dow = sDate.getDayOfWeek();
                    String dayType = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
                            ? "WEEKEND" : "WEEKDAY";
                    double rho = movieLifecycleService != null
                            ? movieLifecycleService.calculateRho(movie.getExhibitionStartDate(), movie.getExhibitionEndDate(), sDate)
                            : 0.5;
                    showSeats = deterministicInventoryEngine.generateShowSeats(
                            show.getShowId(), sDate, physicalSeats,
                            movie.getDemandTier(), movie.getPopularityScore(), rho, dayType, theatre.getTheatreTier());
                }
            }

            // Exclude active holds
            Set<Long> heldSeatIds = Collections.emptySet();
            if (seatHoldRepository != null) {
                try {
                    List<SeatHold> activeHolds = seatHoldRepository.findByShowIdAndStatus(show.getShowId(), "ACTIVE");
                    Instant now = Instant.now();
                    heldSeatIds = activeHolds.stream()
                            .filter(h -> h.getExpiresAt() != null && h.getExpiresAt().isAfter(now))
                            .map(SeatHold::getSeatId)
                            .collect(Collectors.toSet());
                } catch (Exception ignored) {}
            }

            final Set<Long> activeHeldIds = heldSeatIds;
            List<ShowSeat> availableSeats = showSeats.stream()
                    .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()) && !activeHeldIds.contains(ss.getId().getSeatId()))
                    .toList();
            int availableCount = availableSeats.size();
            int totalSeats = showSeats.size();

            if (availableCount < request.getPartySize()) {
                excludedSeats++;
                continue;
            }

            // Group seating planning
            List<Seat> allScreenSeats = screenSeatsCache.computeIfAbsent(screen.getScreenId(),
                    id -> seatRepository.findByScreenId(id));
            if (allScreenSeats == null || allScreenSeats.isEmpty()) {
                allScreenSeats = seatRepository.findByScreenId(screen.getScreenId());
            }

            List<ShowSeat> plannerSeats = showSeats;
            if (!activeHeldIds.isEmpty()) {
                plannerSeats = showSeats.stream().map(ss -> {
                    if (activeHeldIds.contains(ss.getId().getSeatId())) {
                        return new ShowSeat(ss.getId().getShowId(), ss.getId().getSeatId(), "HELD");
                    }
                    return ss;
                }).toList();
            }

            SeatFitResult seatFit = seatGroupPlanner.planBestGroup(
                    show.getShowId(), plannerSeats, allScreenSeats, request.getPartySize(), request.getPriority());

            // 5. Pricing & Budget Evaluation
            BigDecimal totalCost;
            BigDecimal ticketPrice;
            if (seatFit != null && seatFit.getTotalPrice() != null && seatFit.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                totalCost = seatFit.getTotalPrice();
                ticketPrice = totalCost.divide(BigDecimal.valueOf(request.getPartySize()), 2, RoundingMode.HALF_UP);
            } else {
                ticketPrice = determineFormatPrice(formatDisplay);
                totalCost = ticketPrice.multiply(BigDecimal.valueOf(request.getPartySize()));
            }

            if (partyBudget != null && partyBudget.compareTo(BigDecimal.ZERO) > 0) {
                if (totalCost.compareTo(partyBudget) > 0) {
                    excludedBudget++;
                    continue;
                }
            }

            double availabilityRatio = totalSeats > 0 ? (double) availableCount / totalSeats : 0.0;
            String showLangName = mlIdToLangName.getOrDefault(show.getMovieLanguageId(), "Standard");

            ShowCandidateDTO candidate = evaluateCandidate(
                    show, theatre, screen, formatDisplay, showTimeCategory,
                    totalSeats, availableCount, availabilityRatio,
                    ticketPrice, totalCost, partyBudget,
                    seatFit, showLangName, request);

            candidates.add(candidate);
        }

        // Guarantee 1 Show = 1 Card
        Map<Long, ShowCandidateDTO> uniqueCandidateMap = new LinkedHashMap<>();
        for (ShowCandidateDTO cand : candidates) {
            uniqueCandidateMap.putIfAbsent(cand.getShowId(), cand);
        }
        candidates = new ArrayList<>(uniqueCandidateMap.values());

        // Deterministic sorting: matchScore desc -> availableSeats desc -> showId asc
        candidates.sort(Comparator.comparingInt(ShowCandidateDTO::getMatchScore).reversed()
                .thenComparing(Comparator.comparingInt(ShowCandidateDTO::getAvailableSeats).reversed()
                .thenComparing(ShowCandidateDTO::getShowId)));

        // Cross-candidate trade-offs
        BigDecimal minCost = candidates.stream()
                .map(ShowCandidateDTO::getTotalCost)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        if (minCost != null) {
            for (ShowCandidateDTO c : candidates) {
                if (c.getTotalCost() != null && c.getTotalCost().compareTo(minCost) > 0) {
                    BigDecimal diff = c.getTotalCost().subtract(minCost);
                    c.getTradeOffs().add(String.format("₹%.0f more than cheapest valid option for %d seats",
                            diff.doubleValue(), request.getPartySize()));
                }
            }
        }

        ShowCandidateDTO preferredOption = candidates.isEmpty() ? null : candidates.get(0);

        String conflictAnalysis = buildConflictAnalysis(
                preferredOption, candidates, request,
                candidateShows.size(), excludedCity, excludedFormat, excludedTime, excludedSeats, excludedBudget,
                effectiveDateFrom, effectiveDateTo);

        ShowRecommendationResponse response = new ShowRecommendationResponse(
                movieId, movieTitle, cityId, cityName, preferredOption, candidates, conflictAnalysis);

        response.setTotalShowsEvaluated(initialCandidatesCount);
        response.setEligibleCandidatesCount(candidates.size());
        response.setExcludedCity(excludedCity);
        response.setExcludedDate(excludedDate);
        response.setExcludedFormat(excludedFormat);
        response.setExcludedTime(excludedTime);
        response.setExcludedSeats(excludedSeats);
        response.setExcludedBudget(excludedBudget);

        return response;
    }

    private String resolveFormatDisplay(PresentationFormat pf, AudioFormat af) {
        String pfName = pf != null ? pf.getName() : "Standard 2D";
        if (af != null && af.getFormatCode() != null && af.getFormatCode().contains("ATMOS")) {
            return pfName + " (" + af.getName() + ")";
        }
        return pfName;
    }

    private boolean matchesFormat(PresentationFormat pf, AudioFormat af, List<String> reqFormats) {
        if (reqFormats == null || reqFormats.isEmpty()) return true;
        String pfCode = pf != null && pf.getFormatCode() != null ? pf.getFormatCode().toUpperCase() : "2D";
        String pfName = pf != null && pf.getName() != null ? pf.getName().toUpperCase() : "STANDARD 2D";
        String afCode = af != null && af.getFormatCode() != null ? af.getFormatCode().toUpperCase() : "";
        String afName = af != null && af.getName() != null ? af.getName().toUpperCase() : "";
        String combined = pfCode + " " + pfName + " " + afCode + " " + afName;

        for (String rf : reqFormats) {
            String norm = rf.trim().toUpperCase();
            if ("ALL".equals(norm)) return true;
            if (norm.contains("2D") && !norm.contains("3D")) {
                if (pfCode.contains("2D") || (combined.contains("2D") && !combined.contains("3D") && !combined.contains("IMAX") && !combined.contains("4DX"))) {
                    return true;
                }
            }
            if (norm.contains("3D")) {
                if (pfCode.contains("3D") || combined.contains("3D")) {
                    return true;
                }
            }
            if (norm.contains("IMAX")) {
                if (pfCode.contains("IMAX") || afCode.contains("IMAX") || combined.contains("IMAX")) {
                    return true;
                }
            }
            if (norm.contains("4DX")) {
                if (pfCode.contains("4DX") || combined.contains("4DX")) {
                    return true;
                }
            }
            if (norm.contains("DOLBY") || norm.contains("ATMOS")) {
                if (afCode.contains("ATMOS") || combined.contains("ATMOS") || afCode.contains("DOLBY")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getTimeCategory(int hour) {
        if (hour >= 6 && hour < 12) return "MORNING";
        if (hour >= 12 && hour < 16) return "AFTERNOON";
        if (hour >= 16 && hour < 20) return "EVENING";
        return "NIGHT";
    }

    private boolean matchesTime(String showCategory, List<String> reqTimes) {
        if (reqTimes == null || reqTimes.isEmpty()) return true;
        for (String rt : reqTimes) {
            if (rt.equalsIgnoreCase(showCategory)) return true;
        }
        return false;
    }

    private double getSlotTargetHour(String slot) {
        if (slot == null) return 18.0;
        return switch (slot.toUpperCase().trim()) {
            case "MORNING"   -> 10.0;
            case "AFTERNOON" -> 14.0;
            case "EVENING"   -> 18.0;
            case "NIGHT"     -> 21.5;
            default -> {
                try {
                    String[] parts = slot.split("[:.\\-]");
                    if (parts.length >= 2) {
                        yield Double.parseDouble(parts[0]) + (Double.parseDouble(parts[1]) / 60.0);
                    }
                    yield Double.parseDouble(parts[0]);
                } catch (Exception e) {
                    yield 18.0;
                }
            }
        };
    }

    private BigDecimal determineFormatPrice(String formatDisplay) {
        if (formatDisplay == null) return new BigDecimal("180.00");
        String norm = formatDisplay.toUpperCase();
        if (norm.contains("4DX")) return new BigDecimal("450.00");
        if (norm.contains("IMAX")) return new BigDecimal("350.00");
        if (norm.contains("DOLBY") || norm.contains("ATMOS")) return new BigDecimal("280.00");
        if (norm.contains("3D")) return new BigDecimal("220.00");
        return new BigDecimal("180.00");
    }

    private ShowCandidateDTO evaluateCandidate(Show show, Theatre theatre, Screen screen,
                                               String formatDisplay, String showTimeCategory,
                                               int totalSeats, int availableSeats,
                                               double availabilityRatio, BigDecimal ticketPrice,
                                               BigDecimal totalCost, BigDecimal partyBudget,
                                               SeatFitResult seatFit, String showLangName,
                                               ShowRecommendationRequest req) {
        ShowCandidateDTO dto = new ShowCandidateDTO();
        dto.setShowId(show.getShowId());
        dto.setTheatreId(theatre.getTheatreId());
        dto.setTheatreName(theatre.getTheatreName());
        dto.setScreenId(screen.getScreenId());
        dto.setScreenName(screen.getScreenName());
        dto.setPresentationFormat(formatDisplay);
        dto.setStartAt(show.getStartAt());

        ZonedDateTime showZdt = show.getStartAt().atZone(IST_ZONE);
        dto.setShowDate(showZdt.toLocalDate().toString());
        dto.setFormattedTime(TIME_FORMATTER.format(show.getStartAt()).toUpperCase(Locale.ENGLISH));
        dto.setLanguage(showLangName != null ? showLangName : "Standard");
        dto.setTotalSeats(totalSeats);
        dto.setAvailableSeats(availableSeats);
        dto.setAvailabilityRatio(Math.round(availabilityRatio * 100.0) / 100.0);
        dto.setTicketPrice(ticketPrice);
        dto.setTotalCost(totalCost);

        List<String> reasons = new ArrayList<>();
        List<String> tradeOffs = new ArrayList<>();

        // Explanations
        if (showLangName != null && !showLangName.equalsIgnoreCase("Standard")) {
            reasons.add("Available in requested language: " + showLangName);
        }

        reasons.add("Supported format: " + formatDisplay);

        // Seating details
        int seatFitScore = seatFit != null ? seatFit.getGroupScore() : 70;
        if (seatFit != null) {
            dto.setRecommendedSeatIds(seatFit.getSeatIds());
            dto.setRecommendedSeatLabels(seatFit.getSeatLabels());
            dto.setSeatFitDescription(seatFit.getSplitDescription());
            dto.setViewingQualityScore(seatFitScore);
            dto.setSeatingTradeoff(seatFit.getSeatingTradeoff());

            if (seatFit.isAllTogether()) {
                if (seatFit.getBestRow() != null) {
                    reasons.add(String.format("Has %d adjacent seats together in Row %s (%s)",
                            req.getPartySize(), seatFit.getBestRow(), seatFit.getRationale()));
                } else {
                    reasons.add(seatFit.getRationale());
                }
            } else {
                reasons.add(String.format("Seating arrangement: %s (viewing quality: %d/100)",
                        seatFit.getSplitDescription(), seatFitScore));
                if (seatFit.getSeatingTradeoff() != null) {
                    tradeOffs.add(seatFit.getSeatingTradeoff());
                }
            }
        }

        // Timing fit score
        int timeScore;
        List<String> reqTimes = req.getEffectiveTimes();
        List<String> scoringTimes = !reqTimes.isEmpty() ? reqTimes :
                (req.getTimePreference() != null && !req.getTimePreference().isBlank() && !"ALL".equalsIgnoreCase(req.getTimePreference())
                        ? List.of(req.getTimePreference())
                        : Collections.emptyList());

        if (scoringTimes.isEmpty()) {
            timeScore = 100;
        } else {
            double showHour = showZdt.getHour() + (showZdt.getMinute() / 60.0);
            double minDiff = Double.MAX_VALUE;
            for (String t : scoringTimes) {
                double target = getSlotTargetHour(t);
                minDiff = Math.min(minDiff, Math.abs(showHour - target));
            }
            timeScore = Math.max(30, (int) Math.round(100 - (minDiff * 8)));
            if (!reqTimes.isEmpty()) {
                reasons.add(String.format("Showtime %s matches preferred %s slot", dto.getFormattedTime(), showTimeCategory));
            } else {
                reasons.add(String.format("Showtime %s is close to preferred time %s", dto.getFormattedTime(), req.getTimePreference()));
            }
        }

        // Pricing fit score
        int priceScore;
        if (partyBudget != null && partyBudget.compareTo(BigDecimal.ZERO) > 0) {
            double savings = partyBudget.subtract(totalCost).doubleValue();
            double budgetUsed = totalCost.doubleValue() / partyBudget.doubleValue();
            priceScore = Math.max(30, (int) Math.round(100 - (budgetUsed * 40)));
            reasons.add(String.format("Total party cost ₹%.0f for %d seats (₹%.0f/person) within budget of ₹%.0f (saves ₹%.0f)",
                    totalCost.doubleValue(), req.getPartySize(), ticketPrice.doubleValue(), partyBudget.doubleValue(), savings));
        } else {
            double over = Math.max(0, ticketPrice.doubleValue() - 150.0) / 30.0;
            priceScore = Math.max(40, (int) Math.round(100 - (over * 10)));
            reasons.add(String.format("₹%.0f/ticket × %d seats = ₹%.0f total (inclusive of seat tier adjustment)",
                    ticketPrice.doubleValue(), req.getPartySize(), totalCost.doubleValue()));
        }

        // Composite weighted score
        double wSeats = req.getWeightAvailability();
        double wPrice = req.getWeightPrice();
        double wTime  = req.getWeightTime();

        double composite = (wSeats * seatFitScore) + (wPrice * priceScore) + (wTime * timeScore);
        int finalScore = (int) Math.round(Math.min(100, Math.max(30, composite)));

        dto.setMatchScore(finalScore);
        dto.setReasons(reasons);
        dto.setTradeOffs(tradeOffs);

        return dto;
    }

    private String buildConflictAnalysis(ShowCandidateDTO preferred, List<ShowCandidateDTO> candidates,
                                         ShowRecommendationRequest req, int totalInWindow,
                                         int excludedCity, int excludedFormat, int excludedTime,
                                         int excludedSeats, int excludedBudget,
                                         LocalDate dateFrom, LocalDate dateTo) {
        if (preferred == null) {
            List<String> breakdowns = new ArrayList<>();
            if (excludedCity > 0) breakdowns.add(excludedCity + " outside selected city/multiplex");
            if (excludedFormat > 0) breakdowns.add(excludedFormat + " did not match format preference");
            if (excludedTime > 0) breakdowns.add(excludedTime + " did not match preferred time slot");
            if (excludedSeats > 0) breakdowns.add(excludedSeats + " had insufficient seats for party of " + req.getPartySize());
            if (excludedBudget > 0) breakdowns.add(excludedBudget + " exceeded total party budget");

            String breakdownText = breakdowns.isEmpty() ? "No shows scheduled for these dates."
                    : String.format("Evaluated %d shows (%s – %s): %s.", totalInWindow, dateFrom, dateTo, String.join("; ", breakdowns));

            List<String> suggestions = new ArrayList<>();
            if (excludedFormat > 0) suggestions.add("select 'Any Format'");
            if (excludedTime > 0) suggestions.add("select additional time slots");
            if (excludedBudget > 0) suggestions.add("increase budget per person");
            if (excludedSeats > 0) suggestions.add("try a smaller party size");
            if (excludedCity > 0) suggestions.add("select 'All Cities'");

            String suggestionText = suggestions.isEmpty() ? "Try adjusting your filters above."
                    : "To find available shows, try to: " + String.join(" or ", suggestions) + ".";

            return breakdownText + " " + suggestionText;
        }

        StringBuilder sb = new StringBuilder();
        String priority = req.getPriority();
        sb.append(switch (priority) {
            case "BEST_PRICE" -> "Ranked by BEST PRICE — lower total party cost prioritised. ";
            case "BEST_SEATS" -> "Ranked by BEST SEATS — contiguous adjacent seat block prioritised. ";
            case "BEST_TIME"  -> "Ranked by BEST TIME — proximity to preferred time slot prioritised. ";
            default           -> "Balanced ranking (40% seat quality, 30% price, 30% time). ";
        });

        sb.append(String.format("%d show%s match all hard constraints.",
                candidates.size(), candidates.size() == 1 ? "" : "s"));

        if (candidates.size() > 1) {
            ShowCandidateDTO second = candidates.get(1);
            if (preferred.getTotalCost() != null && second.getTotalCost() != null) {
                BigDecimal diff = preferred.getTotalCost().subtract(second.getTotalCost()).abs();
                if (preferred.getTotalCost().compareTo(second.getTotalCost()) < 0 && diff.compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(String.format(" Top pick is ₹%.0f cheaper than %s.", diff.doubleValue(), second.getTheatreName()));
                } else if (preferred.getTotalCost().compareTo(second.getTotalCost()) > 0) {
                    sb.append(String.format(" Top pick costs ₹%.0f more than %s but ranked higher by %s priority.",
                            diff.doubleValue(), second.getTheatreName(), priority));
                }
            }
        }

        return sb.toString();
    }
}
