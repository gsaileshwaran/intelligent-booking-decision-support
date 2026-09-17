package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.service.SeatHoldService;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Show Recommendation Engine — PVK Cinemas Decision Support.
 *
 * Hard Constraint Pipeline (applied BEFORE ranking — violators are excluded, not penalised):
 *   1. City scope (authoritative DB relationship: theatre.city_id)
 *   2. Date range (dateFrom / dateTo — show date must be within range)
 *   3. Language (languageCode — show's movie_language must match; hard-excluded if not)
 *   4. Format (formatPreference — hard-excluded if customer selected a specific format)
 *   5. Party size (available seats >= partySize)
 *   6. Budget (partySize × ticketPrice <= budgetMaxTotal)
 *
 * Soft Preference + Ranking:
 *   Surviving candidates are ranked by a weighted composite score:
 *   - BALANCED: Seat Fit = 40%, Price/Value = 30%, Time Fit = 30%
 *   - BEST_PRICE: Price/Value = 55%, Seat Fit = 25%, Time Fit = 20%
 *   - BEST_SEATS: Seat Fit = 55%, Price/Value = 20%, Time Fit = 25%
 *   - BEST_TIME: Time Fit = 55%, Seat Fit = 25%, Price/Value = 20%
 *
 * Seat Fit Scoring:
 *   Queries actual Seat records from the database.
 *   Groups available seats by row_label, sorts by numeric seat_number,
 *   and identifies the longest contiguous run of adjacent seats.
 *   Scores optical/acoustic sweet-spot quality via SeatScoringEngine.
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
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final SeatHoldService seatHoldService;
    private final SeatScoringEngine seatScoringEngine;
    private final com.pvk.cinemas.booking.service.SeatPricingService seatPricingService;

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
                                    com.pvk.cinemas.booking.service.SeatPricingService seatPricingService) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.movieRepository = movieRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.languageRepository = languageRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.presentationFormatRepository = presentationFormatRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.seatHoldService = seatHoldService;
        this.seatScoringEngine = seatScoringEngine;
        this.seatPricingService = seatPricingService;
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
        // HARD CONSTRAINT 2: Language Filter
        // =====================================================================
        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(movieId);
        List<Long> mlIds;

        String langCode = request.getLanguageCode();
        String resolvedLanguageName = null;
        if (langCode != null && !langCode.trim().isEmpty()) {
            Optional<Language> langOpt = languageRepository.findByLanguageCode(langCode.toLowerCase().trim());
            if (langOpt.isPresent()) {
                Long langId = langOpt.get().getLanguageId();
                resolvedLanguageName = langOpt.get().getName();
                mlIds = mls.stream()
                        .filter(ml -> langId.equals(ml.getLanguageId()))
                        .map(MovieLanguage::getMovieLanguageId)
                        .toList();
                if (mlIds.isEmpty()) {
                    String noLangMsg = String.format(
                            "No shows available for '%s' in %s.",
                            resolvedLanguageName, movieTitle);
                    return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                            Collections.emptyList(), noLangMsg);
                }
            } else {
                mlIds = mls.stream().map(MovieLanguage::getMovieLanguageId).toList();
            }
        } else {
            mlIds = mls.stream().map(MovieLanguage::getMovieLanguageId).toList();
        }

        if (mlIds.isEmpty()) {
            return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                    Collections.emptyList(), "No scheduled shows found for this movie.");
        }

        // Fetch all scheduled shows for matching language(s)
        List<Show> candidateShows = showRepository.findByMovieLanguageIdIn(mlIds).stream()
                .filter(s -> "SCHEDULED".equalsIgnoreCase(s.getShowStatus())
                        || "OPEN".equalsIgnoreCase(s.getShowStatus()))
                .toList();

        // =====================================================================
        // HARD CONSTRAINT 1: Date Range Filter
        // =====================================================================
        LocalDate dateFrom = request.getDateFrom();
        LocalDate dateTo = request.getDateTo();
        if (dateFrom != null || dateTo != null) {
            candidateShows = candidateShows.stream().filter(s -> {
                LocalDate showDate = s.getStartAt().atZone(IST_ZONE).toLocalDate();
                if (dateFrom != null && showDate.isBefore(dateFrom)) return false;
                if (dateTo != null && showDate.isAfter(dateTo)) return false;
                return true;
            }).toList();
        }

        if (candidateShows.isEmpty()) {
            String noDateMsg = dateFrom != null || dateTo != null
                    ? String.format("No shows found for '%s' in the selected date range (%s – %s).",
                        movieTitle,
                        dateFrom != null ? dateFrom.toString() : "any",
                        dateTo != null ? dateTo.toString() : "any")
                    : "No shows currently scheduled for this movie.";
            return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName, null,
                    Collections.emptyList(), noDateMsg);
        }

        Map<Long, ScreenCapability> scMap = screenCapabilityRepository.findAll().stream()
                .collect(Collectors.toMap(ScreenCapability::getScreenCapabilityId, sc -> sc, (a, b) -> a));
        Map<Long, Screen> screenMap = screenRepository.findAll().stream()
                .collect(Collectors.toMap(Screen::getScreenId, s -> s, (a, b) -> a));
        Map<Long, Theatre> theatreMap = theatreRepository.findAll().stream()
                .collect(Collectors.toMap(Theatre::getTheatreId, t -> t, (a, b) -> a));
        Map<Long, PresentationFormat> formatMap = presentationFormatRepository.findAll().stream()
                .collect(Collectors.toMap(PresentationFormat::getPresentationFormatId, f -> f, (a, b) -> a));
        Map<Long, List<Seat>> screenSeatsCache = new java.util.concurrent.ConcurrentHashMap<>();

        List<ShowCandidateDTO> candidates = new ArrayList<>();
        int excludedCity = 0, excludedFormat = 0, excludedSeats = 0, excludedBudget = 0;

        Map<Long, BigDecimal> seatPriceCache = new java.util.concurrent.ConcurrentHashMap<>();

        for (Show show : candidateShows) {
            ScreenCapability sc = scMap.get(show.getScreenCapabilityId());
            if (sc == null) {
                sc = screenCapabilityRepository.findById(show.getScreenCapabilityId()).orElse(null);
            }
            if (sc == null) continue;

            Screen screen = screenMap.get(sc.getScreenId());
            if (screen == null) {
                screen = screenRepository.findById(sc.getScreenId()).orElse(null);
            }
            if (screen == null) continue;

            Theatre theatre = theatreMap.get(screen.getTheatreId());
            if (theatre == null) {
                theatre = theatreRepository.findById(screen.getTheatreId()).orElse(null);
            }
            if (theatre == null) continue;

            // HARD CONSTRAINT 1a: City Scope (authoritative relational join: theatre.city_id)
            if (cityId != null && (theatre.getCityId() == null || !theatre.getCityId().equals(cityId))) {
                excludedCity++;
                continue;
            }

            // Optional theatre filter
            if (request.getTheatreId() != null && !theatre.getTheatreId().equals(request.getTheatreId())) {
                continue;
            }

            PresentationFormat format = formatMap.get(sc.getPresentationFormatId());
            if (format == null) {
                format = presentationFormatRepository.findById(sc.getPresentationFormatId()).orElse(null);
            }
            String formatName = format != null ? format.getName() : "Standard 2D";

            // HARD CONSTRAINT 3: Format (hard-excluded when specific format requested)
            String reqFormat = request.getFormatPreference();
            if (reqFormat != null && !reqFormat.trim().isEmpty() && !"ALL".equalsIgnoreCase(reqFormat)) {
                String normReq = reqFormat.toUpperCase();
                String normActual = formatName.toUpperCase();
                boolean formatMatches = normActual.contains(normReq)
                        || (normReq.contains("IMAX") && normActual.contains("IMAX"))
                        || (normReq.contains("4DX") && normActual.contains("4DX"))
                        || (normReq.contains("DOLBY") && (normActual.contains("DOLBY") || normActual.contains("ATMOS")))
                        || (normReq.contains("2D") && !normActual.contains("IMAX")
                                && !normActual.contains("4DX") && !normActual.contains("3D"));
                if (!formatMatches) {
                    excludedFormat++;
                    continue; // HARD exclude
                }
            }

            List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(show.getShowId());
            int totalSeats = showSeats.size();
            List<ShowSeat> availableSeats = showSeats.stream()
                    .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                    .toList();
            int availableCount = availableSeats.size();

            // HARD CONSTRAINT 4: Party Size (enough available seats)
            if (availableCount < request.getPartySize()) {
                excludedSeats++;
                continue;
            }

            // Determine ticket price with caching
            BigDecimal ticketPrice = null;
            if (!showSeats.isEmpty()) {
                Long firstSeatId = showSeats.get(0).getId().getSeatId();
                ticketPrice = seatPriceCache.computeIfAbsent(firstSeatId, id -> {
                    if (seatPricingService != null) {
                        try {
                            BigDecimal p = seatPricingService.determineSeatPrice(show.getShowId(), id);
                            if (p != null) return p;
                        } catch (Exception ignored) {}
                    }
                    if (seatHoldService != null) {
                        try {
                            BigDecimal p = seatHoldService.determineSeatPrice(show.getShowId(), id);
                            if (p != null) return p;
                        } catch (Exception ignored) {}
                    }
                    return determineFormatPrice(formatName);
                });
            }
            if (ticketPrice == null) {
                ticketPrice = determineFormatPrice(formatName);
            }

            // HARD CONSTRAINT 5: Total Party Budget (partySize × ticketPrice <= budgetMaxTotal)
            BigDecimal totalCost = ticketPrice.multiply(BigDecimal.valueOf(request.getPartySize()));
            BigDecimal budgetMaxTotal = request.getBudgetMaxTotal();
            if (budgetMaxTotal == null && request.getBudgetMax() != null) {
                budgetMaxTotal = request.getBudgetMax().multiply(BigDecimal.valueOf(request.getPartySize()));
            }
            if (budgetMaxTotal != null && budgetMaxTotal.compareTo(BigDecimal.ZERO) > 0) {
                if (totalCost.compareTo(budgetMaxTotal) > 0) {
                    excludedBudget++;
                    continue; // HARD exclude over-budget
                }
            }

            double availabilityRatio = totalSeats > 0 ? (double) availableCount / totalSeats : 0.0;

            // REAL SEAT FIT SCORING: Evaluate actual contiguous adjacent seats
            List<Seat> allScreenSeats = screenSeatsCache.computeIfAbsent(screen.getScreenId().longValue(),
                    id -> seatRepository.findByScreenId(id));
            SeatFitInfo seatFitInfo = evaluateSeatFit(availableSeats, allScreenSeats, request.getPartySize());

            ShowCandidateDTO candidate = evaluateCandidate(
                    show, theatre, screen, formatName,
                    totalSeats, availableCount, availabilityRatio,
                    ticketPrice, totalCost, budgetMaxTotal,
                    seatFitInfo, resolvedLanguageName, request);
            candidates.add(candidate);
        }

        // Sort by matchScore descending, then availableSeats descending as tiebreaker
        candidates.sort(Comparator.comparingInt(ShowCandidateDTO::getMatchScore).reversed()
                .thenComparing(Comparator.comparingInt(ShowCandidateDTO::getAvailableSeats).reversed()));

        // Cross-candidate trade-offs: compare against cheapest valid option
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
                excludedCity, excludedFormat, excludedSeats, excludedBudget,
                dateFrom, dateTo, langCode);

        return new ShowRecommendationResponse(movieId, movieTitle, cityId, cityName,
                preferredOption, candidates, conflictAnalysis);
    }

    private BigDecimal determineFormatPrice(String formatName) {
        if (formatName == null) return new BigDecimal("180.00");
        String norm = formatName.toUpperCase();
        if (norm.contains("4DX")) return new BigDecimal("450.00");
        if (norm.contains("IMAX")) return new BigDecimal("350.00");
        if (norm.contains("DOLBY") || norm.contains("ATMOS")) return new BigDecimal("280.00");
        return new BigDecimal("180.00");
    }

    /**
     * Evaluates actual seat allocation quality using database Seat records.
     * Groups available seats by row_label, orders by integer seat_number,
     * and calculates contiguous block size and acoustic/optical quality.
     */
    static class SeatFitInfo {
        int maxContiguousBlock = 0;
        String bestRow = null;
        List<String> bestSeatLabels = new ArrayList<>();
        List<Long> bestSeatIds = new ArrayList<>();
        int seatFitScore = 50;
        boolean allTogether = false;
    }

    private SeatFitInfo evaluateSeatFit(List<ShowSeat> availableSeats, List<Seat> allScreenSeats, int partySize) {
        SeatFitInfo info = new SeatFitInfo();
        if (availableSeats == null || availableSeats.isEmpty() || partySize <= 0) {
            return info;
        }

        Set<Long> availIds = availableSeats.stream()
                .map(ss -> ss.getId().getSeatId())
                .collect(Collectors.toSet());

        // Filter: must be in available show-seat set AND physically ACTIVE (not maintenance-blocked)
        List<Seat> seats = (allScreenSeats != null && !allScreenSeats.isEmpty())
                ? allScreenSeats.stream()
                    .filter(s -> availIds.contains(s.getSeatId()))
                    .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                    .toList()
                : seatRepository.findAllById(availIds).stream()
                    .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                    .toList();

        if (seats == null || seats.isEmpty()) {
            info.maxContiguousBlock = availableSeats.size();
            info.seatFitScore = Math.min(100, Math.max(40, (int) Math.round(((double) availableSeats.size() / partySize) * 100)));
            return info;
        }

        Map<String, List<Seat>> seatsByRow = seats.stream()
                .filter(s -> s.getRowLabel() != null)
                .collect(Collectors.groupingBy(Seat::getRowLabel));

        int globalMaxContiguous = 0;
        String topRow = null;
        List<String> topLabels = new ArrayList<>();
        List<Long> topIds = new ArrayList<>();
        int topQualityScore = 70;

        for (Map.Entry<String, List<Seat>> entry : seatsByRow.entrySet()) {
            String row = entry.getKey();
            List<Seat> rowSeats = entry.getValue();

            rowSeats.sort(Comparator.comparingInt(s -> {
                try {
                    return Integer.parseInt(s.getSeatNumber().replaceAll("[^0-9]", ""));
                } catch (Exception e) {
                    return 0;
                }
            }));

            int currentRun = 0;
            List<Seat> currentRunSeats = new ArrayList<>();
            int prevNum = -999;
            Seat prevSeat = null;

            for (Seat seat : rowSeats) {
                int seatNum;
                try {
                    seatNum = Integer.parseInt(seat.getSeatNumber().replaceAll("[^0-9]", ""));
                } catch (Exception e) {
                    seatNum = prevNum + 2;
                }

                boolean aisleBreak = (prevSeat != null && prevSeat.getAisleAfter() != null && prevSeat.getAisleAfter());

                if (seatNum == prevNum + 1 && !aisleBreak) {
                    currentRun++;
                    currentRunSeats.add(seat);
                } else {
                    currentRun = 1;
                    currentRunSeats = new ArrayList<>();
                    currentRunSeats.add(seat);
                }
                prevNum = seatNum;
                prevSeat = seat;

                if (currentRun > globalMaxContiguous) {
                    globalMaxContiguous = currentRun;
                    topRow = row;
                    topLabels = currentRunSeats.stream()
                            .map(s -> s.getRowLabel() + s.getSeatNumber())
                            .toList();
                    topIds = currentRunSeats.stream()
                            .map(Seat::getSeatId)
                            .toList();
                }

                if (currentRun >= partySize) {
                    List<Seat> partySeats = currentRunSeats.subList(currentRun - partySize, currentRun);
                    int quality = (int) partySeats.stream()
                            .mapToInt(s -> {
                                if (seatScoringEngine != null) {
                                    return seatScoringEngine.scoreSeat(s).getScore();
                                }
                                return 85;
                            })
                            .average()
                            .orElse(80);

                    if (quality > topQualityScore || !info.allTogether) {
                        topQualityScore = quality;
                        topRow = row;
                        topLabels = partySeats.stream()
                                .map(s -> s.getRowLabel() + s.getSeatNumber())
                                .toList();
                        topIds = partySeats.stream()
                                .map(Seat::getSeatId)
                                .toList();
                        info.allTogether = true;
                    }
                }
            }
        }

        // If contiguous block of full partySize was not found, assemble best cluster of partySize seats
        if (topIds.size() < partySize && seats.size() >= partySize) {
            List<Seat> candidatePool = new ArrayList<>(seats);
            candidatePool.sort(Comparator.comparingInt((Seat s) -> {
                if (seatScoringEngine != null) {
                    return seatScoringEngine.scoreSeat(s).getScore();
                }
                return 80;
            }).reversed());

            Set<Long> collected = new LinkedHashSet<>(topIds);
            for (Seat s : candidatePool) {
                if (collected.size() >= partySize) break;
                collected.add(s.getSeatId());
            }
            topIds = new ArrayList<>(collected);
            Map<Long, Seat> byId = seats.stream().collect(Collectors.toMap(Seat::getSeatId, s -> s));
            topLabels = topIds.stream()
                    .map(id -> {
                        Seat s = byId.get(id);
                        return s != null ? (s.getRowLabel() + s.getSeatNumber()) : String.valueOf(id);
                    })
                    .toList();
            topRow = "Mixed";
        }

        info.maxContiguousBlock = globalMaxContiguous;
        info.bestRow = topRow;
        info.bestSeatLabels = topLabels;
        info.bestSeatIds = topIds;

        if (globalMaxContiguous >= partySize) {
            info.allTogether = true;
            info.seatFitScore = Math.min(100, Math.max(70, topQualityScore));
        } else {
            info.allTogether = false;
            double ratio = (double) globalMaxContiguous / partySize;
            info.seatFitScore = Math.max(30, (int) Math.round(ratio * 65));
        }

        return info;
    }

    private ShowCandidateDTO evaluateCandidate(Show show, Theatre theatre, Screen screen,
                                               String formatName, int totalSeats, int availableSeats,
                                               double availabilityRatio, BigDecimal ticketPrice,
                                               BigDecimal totalCost, BigDecimal budgetMaxTotal,
                                               SeatFitInfo seatFitInfo,
                                               String resolvedLanguageName,
                                               ShowRecommendationRequest req) {
        ShowCandidateDTO dto = new ShowCandidateDTO();
        dto.setShowId(show.getShowId());
        dto.setTheatreId(theatre.getTheatreId());
        dto.setTheatreName(theatre.getTheatreName());
        dto.setScreenId(screen.getScreenId());
        dto.setScreenName(screen.getScreenName());
        dto.setPresentationFormat(formatName);
        dto.setStartAt(show.getStartAt());

        ZonedDateTime showZdt = show.getStartAt().atZone(IST_ZONE);
        dto.setShowDate(showZdt.toLocalDate().toString());
        dto.setFormattedTime(TIME_FORMATTER.format(show.getStartAt()).toUpperCase(Locale.ENGLISH));
        dto.setLanguage(resolvedLanguageName != null ? resolvedLanguageName : "Standard");
        dto.setTotalSeats(totalSeats);
        dto.setAvailableSeats(availableSeats);
        dto.setAvailabilityRatio(Math.round(availabilityRatio * 100.0) / 100.0);
        dto.setTicketPrice(ticketPrice);
        dto.setTotalCost(totalCost);

        List<String> reasons = new ArrayList<>();
        List<String> tradeOffs = new ArrayList<>();

        // ---- Language explanation ----
        if (resolvedLanguageName != null) {
            reasons.add("Available in requested language: " + resolvedLanguageName);
        }

        // ---- Format explanation ----
        String reqFormat = req.getFormatPreference();
        if (reqFormat != null && !reqFormat.trim().isEmpty() && !"ALL".equalsIgnoreCase(reqFormat)) {
            reasons.add("Exact match for requested presentation format (" + formatName + ")");
        } else {
            reasons.add("Format: " + formatName);
        }

        // ---- SEAT FIT SCORE [0-100] ----
        int seatFitScore = seatFitInfo.seatFitScore;
        if (seatFitInfo.allTogether) {
            if (seatFitInfo.bestSeatIds != null && !seatFitInfo.bestSeatIds.isEmpty()) {
                dto.setRecommendedSeatIds(seatFitInfo.bestSeatIds);
                dto.setRecommendedSeatLabels(seatFitInfo.bestSeatLabels);
            }
            if (seatFitInfo.bestRow != null && !seatFitInfo.bestSeatLabels.isEmpty()) {
                reasons.add(String.format("%d adjacent seats together in Row %s (%s)",
                        req.getPartySize(), seatFitInfo.bestRow, String.join(", ", seatFitInfo.bestSeatLabels)));
            } else {
                reasons.add(String.format("%d adjacent seats available together in a single row", req.getPartySize()));
            }
        } else {
            tradeOffs.add(String.format("Seats not all together: largest adjacent block is %d seats in Row %s (need %d)",
                    seatFitInfo.maxContiguousBlock, seatFitInfo.bestRow != null ? seatFitInfo.bestRow : "?", req.getPartySize()));
        }

        // ---- TIME FIT SCORE [0-100] ----
        int timeScore;
        boolean hasTimePref = req.getTimePreference() != null
                && !req.getTimePreference().trim().isEmpty()
                && !"ALL".equalsIgnoreCase(req.getTimePreference());

        if (!hasTimePref) {
            // Neutral score when no time preference provided — no artificial bias
            timeScore = 100;
        } else {
            double showHour = showZdt.getHour() + (showZdt.getMinute() / 60.0);
            double targetHour = parseTargetHour(req.getTimePreference());
            double diffHours = Math.abs(showHour - targetHour);

            timeScore = Math.max(30, (int) Math.round(100 - (diffHours * 12)));
            if (diffHours <= 1.0) {
                reasons.add("Optimal time: " + dto.getFormattedTime() + " — closely matches your preferred slot");
            } else if (diffHours <= 2.5) {
                reasons.add("Convenient time: " + dto.getFormattedTime() + " (~" + Math.round(diffHours) + "h from preferred)");
            } else {
                tradeOffs.add("Show at " + dto.getFormattedTime() + " is " + String.format("%.1f", diffHours) + "h from your preferred time");
            }
        }

        // ---- PRICE SCORE [0-100] ----
        int priceScore;
        if (budgetMaxTotal != null && budgetMaxTotal.compareTo(BigDecimal.ZERO) > 0) {
            double savings = budgetMaxTotal.subtract(totalCost).doubleValue();
            double budgetUsed = totalCost.doubleValue() / budgetMaxTotal.doubleValue();
            priceScore = Math.max(40, (int) Math.round(100 - (budgetUsed * 50)));
            reasons.add(String.format("Total cost ₹%.0f for %d seats — within budget of ₹%.0f (saves ₹%.0f)",
                    totalCost.doubleValue(), req.getPartySize(), budgetMaxTotal.doubleValue(), savings));
        } else {
            // Cheaper shows score higher
            double over = Math.max(0, ticketPrice.doubleValue() - 150.0) / 25.0;
            priceScore = Math.max(40, (int) Math.round(100 - (over * 10)));
            reasons.add(String.format("₹%.0f/ticket × %d seats = ₹%.0f total",
                    ticketPrice.doubleValue(), req.getPartySize(), totalCost.doubleValue()));
        }

        // ---- COMPOSITE WEIGHTED SCORE ----
        double wSeats = req.getWeightAvailability();
        double wPrice = req.getWeightPrice();
        double wTime  = req.getWeightTime();

        double composite = (wSeats * seatFitScore)
                + (wPrice * priceScore)
                + (wTime * timeScore);

        int finalScore = (int) Math.round(Math.min(100, Math.max(30, composite)));
        dto.setMatchScore(finalScore);
        dto.setReasons(reasons);
        dto.setTradeOffs(tradeOffs);

        return dto;
    }

    private double parseTargetHour(String timePref) {
        if (timePref == null || timePref.trim().isEmpty()) {
            return 18.5;
        }
        String norm = timePref.toUpperCase().trim();
        return switch (norm) {
            case "MORNING"   -> 10.5;
            case "AFTERNOON" -> 14.0;
            case "EVENING"   -> 18.0;
            case "NIGHT"     -> 21.5;
            default -> {
                try {
                    String[] parts = norm.split("[:.\\-]");
                    if (parts.length >= 2) {
                        double h = Double.parseDouble(parts[0]);
                        double m = Double.parseDouble(parts[1]) / 60.0;
                        yield h + m;
                    }
                    yield Double.parseDouble(parts[0]);
                } catch (Exception e) {
                    yield 18.5;
                }
            }
        };
    }

    private String buildConflictAnalysis(ShowCandidateDTO preferred, List<ShowCandidateDTO> candidates,
                                         ShowRecommendationRequest req,
                                         int excludedCity, int excludedFormat, int excludedSeats, int excludedBudget,
                                         LocalDate dateFrom, LocalDate dateTo, String langCode) {
        if (preferred == null) {
            List<String> reasons = new ArrayList<>();
            if (excludedCity > 0) reasons.add(excludedCity + " shows excluded (outside selected city)");
            if (excludedFormat > 0) reasons.add(excludedFormat + " shows excluded (did not match format: " + req.getFormatPreference() + ")");
            if (excludedSeats > 0) reasons.add(excludedSeats + " shows excluded (insufficient seats for party of " + req.getPartySize() + ")");
            if (excludedBudget > 0) reasons.add(excludedBudget + " shows excluded (total cost exceeded party budget)");
            if (dateFrom != null || dateTo != null)
                reasons.add("Date range: " + (dateFrom != null ? dateFrom : "any") + " – " + (dateTo != null ? dateTo : "any"));
            return reasons.isEmpty()
                    ? "No shows found matching your criteria. Try relaxing some constraints."
                    : "No matching shows. Constraints applied: " + String.join("; ", reasons) + ". Try relaxing one constraint.";
        }

        StringBuilder sb = new StringBuilder();
        String priority = req.getPriority();
        sb.append(switch (priority) {
            case "BEST_PRICE" -> "Ranked by BEST PRICE — lowest party cost prioritised. ";
            case "BEST_SEATS" -> "Ranked by BEST SEATS — contiguous adjacent seat block prioritised. ";
            case "BEST_TIME"  -> "Ranked by BEST TIME — closest match to preferred time prioritised. ";
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

        if (!preferred.getTradeOffs().isEmpty()) {
            sb.append(" Note: ").append(preferred.getTradeOffs().get(0));
        }

        return sb.toString();
    }
}
