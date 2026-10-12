package com.pvk.cinemas.scheduling.service;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.model.MovieLanguage;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.model.AudioFormat;
import com.pvk.cinemas.catalogue.repository.*;
import com.pvk.cinemas.catalogue.service.MovieLifecycleService;
import com.pvk.cinemas.common.exceptions.OutOfCalendarRangeException;
import com.pvk.cinemas.common.time.BusinessDateProvider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative Year-Long Show Generation and Scheduling Engine (Part 3).
 *
 * Implements the locked scheduling pipeline algorithm (§7) across:
 * - 5 metropolitan regions
 * - 25 multiplexes
 * - 127 physical screens
 * - 16,386 physical seats
 * - 45 catalogue movies
 * - Exactly 365 calendar days [B - 260, B + 104]
 *
 * Enforces:
 * 1. Physical Screen scheduling authority (screen_id)
 * 2. Operating hours with cross-midnight window interpretation
 * 3. Unified turnaround buffers (20m 2D/3D, 30m IMAX/4DX/Atmos)
 * 4. Model A shared-screen programming with dynamic capacity release
 * 5. Deterministic inventory generation (Functions G & F)
 * 6. Hybrid materialization (eager active window [B, B+13], JIT advance schedule [B+14, B+104])
 */
@Service
public class YearLongShowGenerationService {

    private static final Logger log = LoggerFactory.getLogger(YearLongShowGenerationService.class);
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private static final Map<Long, List<String>> CITY_LANGUAGE_PREFERENCES = Map.of(
            1L, List.of("ta", "en", "te", "hi"),
            2L, List.of("kn", "ta", "te", "hi", "en"),
            3L, List.of("te", "hi", "en", "ta"),
            4L, List.of("hi", "en", "ta", "te"),
            5L, List.of("hi", "en")
    );

    private final ShowRepository showRepository;
    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final ScreenRepository screenRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final PresentationFormatRepository presentationFormatRepository;
    private final AudioFormatRepository audioFormatRepository;
    private final MovieRepository movieRepository;
    private final MovieLanguageRepository movieLanguageRepository;
    private final LanguageRepository languageRepository;
    private final SeatRepository seatRepository;
    private final MovieLifecycleService movieLifecycleService;
    private final BusinessDateProvider businessDateProvider;
    private final DeterministicInventoryEngine deterministicInventoryEngine;
    private final JdbcTemplate jdbcTemplate;

    public YearLongShowGenerationService(
            ShowRepository showRepository,
            TheatreRepository theatreRepository,
            CityRepository cityRepository,
            ScreenRepository screenRepository,
            ScreenCapabilityRepository screenCapabilityRepository,
            PresentationFormatRepository presentationFormatRepository,
            AudioFormatRepository audioFormatRepository,
            MovieRepository movieRepository,
            MovieLanguageRepository movieLanguageRepository,
            LanguageRepository languageRepository,
            SeatRepository seatRepository,
            MovieLifecycleService movieLifecycleService,
            BusinessDateProvider businessDateProvider,
            DeterministicInventoryEngine deterministicInventoryEngine,
            JdbcTemplate jdbcTemplate) {
        this.showRepository = showRepository;
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.screenRepository = screenRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.presentationFormatRepository = presentationFormatRepository;
        this.audioFormatRepository = audioFormatRepository;
        this.movieRepository = movieRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.languageRepository = languageRepository;
        this.seatRepository = seatRepository;
        this.movieLifecycleService = movieLifecycleService;
        this.businessDateProvider = businessDateProvider;
        this.deterministicInventoryEngine = deterministicInventoryEngine;
        this.jdbcTemplate = jdbcTemplate;
    }

    public record GenerationSummary(
            int totalShowsGenerated,
            int activeWindowShows,
            int activeWindowShowSeats,
            long executionTimeMs,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    /**
     * Executes deterministic generation for the complete consecutive year of future demo schedules
     * beginning on the current business date and extending approximately one year forward [B, B + 365].
     */
    @Transactional
    public GenerationSummary generateFullYearSchedule() {
        LocalDate anchorDate = businessDateProvider.getBusinessDate();
        LocalDate startDate = businessDateProvider.getBusinessDate();
        LocalDate endDate = businessDateProvider.getCalendarHorizonEnd();
        return generateScheduleRange(startDate, endDate, anchorDate);
    }

    /**
     * Generates shows for a single exhibition date d safely without modifying existing shows.
     */
    @Transactional
    public GenerationSummary generateScheduleForDate(LocalDate date) {
        businessDateProvider.validateCalendarRange(date);
        LocalDate anchorDate = businessDateProvider.getBusinessDate();
        return generateScheduleRange(date, date, anchorDate);
    }

    /**
     * Idempotently extends the schedule horizon to the target end date.
     * Identifies the current maximum exhibition date in the database and only generates
     * shows for forward unpopulated dates, preserving all existing schedules and IDs.
     */
    @Transactional
    public GenerationSummary extendScheduleHorizon(LocalDate targetEndDate) {
        businessDateProvider.validateCalendarRange(targetEndDate);
        LocalDate anchorDate = businessDateProvider.getBusinessDate();
        LocalDate maxDate = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(show_date), ?) FROM `SHOW`",
                (rs, rowNum) -> rs.getDate(1).toLocalDate(),
                Date.valueOf(anchorDate)
        );
        LocalDate startDate = maxDate.isBefore(anchorDate) ? anchorDate : maxDate.plusDays(1);
        if (startDate.isAfter(targetEndDate)) {
            log.info("Schedule horizon is already extended up to {} (requested: {}). Zero shows generated.", maxDate, targetEndDate);
            return new GenerationSummary(0, 0, 0, 0, maxDate, maxDate);
        }
        return generateScheduleRange(startDate, targetEndDate, anchorDate);
    }

    /**
     * Explicit administrative repair for a date range.
     * Strictly fails fast if any show in the target date range has active customer bookings.
     */
    @Transactional
    public GenerationSummary repairScheduleRange(LocalDate startDate, LocalDate endDate, boolean force) {
        if (!force) {
            throw new IllegalArgumentException("Administrative repair requires force=true confirmation.");
        }
        Integer bookingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM booking WHERE show_id IN (SELECT show_id FROM `SHOW` WHERE show_date BETWEEN ? AND ?)",
                Integer.class,
                Date.valueOf(startDate), Date.valueOf(endDate)
        );
        if (bookingCount != null && bookingCount > 0) {
            throw new IllegalStateException("Cannot repair date range containing " + bookingCount +
                    " active customer booking(s). Booked shows and historical records must be preserved.");
        }
        log.warn("Executing explicit administrative repair for range [{} to {}] with zero bookings...", startDate, endDate);
        jdbcTemplate.update("DELETE FROM SHOW_SEAT WHERE show_id IN (SELECT show_id FROM `SHOW` WHERE show_date BETWEEN ? AND ?)",
                Date.valueOf(startDate), Date.valueOf(endDate));
        jdbcTemplate.update("DELETE FROM `SHOW` WHERE show_date BETWEEN ? AND ?",
                Date.valueOf(startDate), Date.valueOf(endDate));

        LocalDate anchorDate = businessDateProvider.getBusinessDate();
        return generateScheduleRange(startDate, endDate, anchorDate);
    }

    /**
     * Core range generator.
     */
    @Transactional
    public GenerationSummary generateScheduleRange(LocalDate startDate, LocalDate endDate, LocalDate anchorDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
        businessDateProvider.validateCalendarRange(startDate);
        businessDateProvider.validateCalendarRange(endDate);

        long t0 = System.currentTimeMillis();
        log.info("Starting Part 3 deterministic show generation for range [{} to {}] (anchor: {})...",
                startDate, endDate, anchorDate);

        // 1. Cache reference datasets in memory
        List<City> cities = cityRepository.findAll().stream()
                .sorted(Comparator.comparing(City::getCityId))
                .toList();

        List<Theatre> allTheatres = theatreRepository.findAll().stream()
                .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .sorted(Comparator.comparing(Theatre::getTheatreId))
                .toList();
        Map<Long, List<Theatre>> theatresByCity = allTheatres.stream()
                .collect(Collectors.groupingBy(Theatre::getCityId));

        List<Screen> allScreens = screenRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .sorted(Comparator.comparing(Screen::getScreenId))
                .toList();
        Map<Long, List<Screen>> screensByTheatre = allScreens.stream()
                .collect(Collectors.groupingBy(Screen::getTheatreId));

        List<ScreenCapability> allCaps = screenCapabilityRepository.findAll().stream()
                .sorted(Comparator.comparing(ScreenCapability::getScreenCapabilityId))
                .toList();
        Map<Long, List<ScreenCapability>> capsByScreen = allCaps.stream()
                .collect(Collectors.groupingBy(ScreenCapability::getScreenId));

        Map<Long, String> presentationFormats = presentationFormatRepository.findAll().stream()
                .collect(Collectors.toMap(PresentationFormat::getPresentationFormatId, PresentationFormat::getFormatCode));
        Map<Long, String> audioFormats = audioFormatRepository.findAll().stream()
                .collect(Collectors.toMap(AudioFormat::getAudioFormatId, AudioFormat::getFormatCode));

        List<Movie> allMovies = movieRepository.findAll().stream()
                .filter(m -> m.getExhibitionStartDate() != null && m.getExhibitionEndDate() != null)
                .sorted(Comparator.comparing(Movie::getMovieId))
                .toList();

        List<MovieLanguage> allMovieLanguages = movieLanguageRepository.findAll();
        Map<Long, List<MovieLanguage>> movieLanguagesByMovie = allMovieLanguages.stream()
                .collect(Collectors.groupingBy(MovieLanguage::getMovieId));

        Map<Long, String> langCodesById = languageRepository.findAll().stream()
                .collect(Collectors.toMap(com.pvk.cinemas.catalogue.model.Language::getLanguageId,
                        com.pvk.cinemas.catalogue.model.Language::getLanguageCode));

        // 2. Prepare show generation buffers
        List<ShowRecord> generatedShows = new ArrayList<>();
        LocalDate activeStart = businessDateProvider.getActiveBookingWindowStart();
        LocalDate activeEnd = businessDateProvider.getActiveBookingWindowEnd();

        // Check which dates in target range already contain scheduled shows to ensure idempotent preservation
        List<LocalDate> existingDates = jdbcTemplate.query(
                "SELECT DISTINCT show_date FROM `SHOW` WHERE show_date BETWEEN ? AND ?",
                (rs, rowNum) -> rs.getDate(1).toLocalDate(),
                Date.valueOf(startDate), Date.valueOf(endDate)
        );
        Set<LocalDate> populatedDates = new HashSet<>(existingDates);

        LocalDate curDate = startDate;
        while (!curDate.isAfter(endDate)) {
            LocalDate d = curDate;
            if (populatedDates.contains(d)) {
                log.info("Schedule for date {} already exists. Preserving existing shows, show IDs, and dependent records without modification.", d);
                curDate = curDate.plusDays(1);
                continue;
            }

            DayOfWeek dow = d.getDayOfWeek();
            String dayType = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
                    ? "WEEKEND" : "WEEKDAY";
            boolean isCompleted = d.isBefore(anchorDate);

            // Filter eligible candidate movies for date d
            List<EligibleMovie> eligibleMovies = new ArrayList<>();
            for (Movie m : allMovies) {
                String stage = movieLifecycleService.evaluateStage(m.getExhibitionStartDate(), m.getExhibitionEndDate(), d);
                if (MovieLifecycleService.ACTIVE_STAGES.contains(stage)) {
                    double rho = movieLifecycleService.calculateRho(m.getExhibitionStartDate(), m.getExhibitionEndDate(), d);
                    eligibleMovies.add(new EligibleMovie(m, stage, rho));
                }
            }

            if (!eligibleMovies.isEmpty()) {
                for (City city : cities) {
                    Long cityId = city.getCityId();
                    List<Theatre> cityTheatres = theatresByCity.getOrDefault(cityId, Collections.emptyList());
                    if (cityTheatres.isEmpty()) continue;

                    // Movie-to-Theatre allocation breadth K with balanced theatre distribution
                    Map<Long, List<Theatre>> theatreAlloc = new HashMap<>();
                    for (EligibleMovie em : eligibleMovies) {
                        int k = calculateTheatreBreadth(em.movie().getDemandTier(), em.stage());
                        int breadth = Math.min(k, cityTheatres.size());
                        int startIdx = (int) ((em.movie().getMovieId() + d.getDayOfMonth()) % cityTheatres.size());
                        List<Theatre> allocTheatres = new ArrayList<>(breadth);
                        for (int i = 0; i < breadth; i++) {
                            allocTheatres.add(cityTheatres.get((startIdx + i) % cityTheatres.size()));
                        }
                        theatreAlloc.put(em.movie().getMovieId(), allocTheatres);
                    }

                    for (Theatre theatre : cityTheatres) {
                        long tid = theatre.getTheatreId();
                        List<EligibleMovie> theatreMovies = eligibleMovies.stream()
                                .filter(em -> theatreAlloc.getOrDefault(em.movie().getMovieId(), Collections.emptyList()).contains(theatre))
                                .toList();
                        if (theatreMovies.isEmpty()) continue;

                        LocalTime opTime = theatre.getOpeningTime() != null ? theatre.getOpeningTime() : LocalTime.of(9, 30);
                        LocalTime clTime = theatre.getClosingTime() != null ? theatre.getClosingTime() : LocalTime.of(23, 30);

                        ZonedDateTime tOpen = d.atTime(opTime).atZone(IST_ZONE);
                        ZonedDateTime tClose = !clTime.isAfter(opTime)
                                ? d.plusDays(1).atTime(clTime).atZone(IST_ZONE)
                                : d.atTime(clTime).atZone(IST_ZONE);

                        List<Screen> screenList = screensByTheatre.getOrDefault(tid, Collections.emptyList());
                        for (int scrIdx = 0; scrIdx < screenList.size(); scrIdx++) {
                            Screen screen = screenList.get(scrIdx);
                            long sid = screen.getScreenId();
                            List<ScreenCapability> screenCaps = capsByScreen.getOrDefault(sid, Collections.emptyList());
                            if (screenCaps.isEmpty()) continue;
                            ScreenCapability cap = screenCaps.get(0);

                            int bufferMinutes = getTurnaroundBufferMinutes(screen, cap, presentationFormats, audioFormats);
                            String pfCode = presentationFormats.getOrDefault(cap.getPresentationFormatId(), "2D");

                            // Compatibility filter
                            List<EligibleMovie> competing = new ArrayList<>();
                            for (EligibleMovie em : theatreMovies) {
                                if (("IMAX".equalsIgnoreCase(pfCode) || "4DX".equalsIgnoreCase(pfCode))
                                        && !"BLOCKBUSTER".equalsIgnoreCase(em.movie().getDemandTier())) {
                                    continue;
                                }
                                competing.add(em);
                            }
                            if (competing.isEmpty()) continue;

                            // Deterministic ranking
                            competing.sort(Comparator
                                    .comparingInt((EligibleMovie em) -> -calculatePriorityScore(em.movie(), em.stage()))
                                    .thenComparing((EligibleMovie em) -> -em.movie().getExhibitionStartDate().toEpochDay())
                                    .thenComparing((EligibleMovie em) -> em.movie().getMovieId())
                            );

                            boolean isDedicated = Boolean.TRUE.equals(screen.getIsPremium())
                                    || competing.size() == 1
                                    || (scrIdx == 0 && "BLOCKBUSTER".equalsIgnoreCase(competing.get(0).movie().getDemandTier())
                                    && ("OPENING".equalsIgnoreCase(competing.get(0).stage()) || "PEAK".equalsIgnoreCase(competing.get(0).stage()))
                                    && "WEEKEND".equalsIgnoreCase(dayType));

                            Map<Long, TargetAllocation> alloc = new LinkedHashMap<>();
                            if (isDedicated) {
                                EligibleMovie m1 = competing.get(0);
                                int target = calculateTargetShows(m1.movie().getDemandTier(), m1.stage(), dayType);
                                alloc.put(m1.movie().getMovieId(), new TargetAllocation(m1, target));
                            } else {
                                int offset = (scrIdx * 2) % competing.size();
                                EligibleMovie ma = competing.get(offset);
                                EligibleMovie mb = competing.get((offset + 1) % competing.size());
                                if (ma.movie().getMovieId().equals(mb.movie().getMovieId()) && competing.size() > 1) {
                                    mb = competing.get((offset + 2) % competing.size());
                                }
                                int targetA = Math.min(3, calculateTargetShows(ma.movie().getDemandTier(), ma.stage(), dayType));
                                int targetB = Math.min(2, calculateTargetShows(mb.movie().getDemandTier(), mb.stage(), dayType));
                                if (targetA < 3) {
                                    targetB = Math.min(5 - targetA, calculateTargetShows(mb.movie().getDemandTier(), mb.stage(), dayType));
                                }
                                alloc.put(ma.movie().getMovieId(), new TargetAllocation(ma, targetA));
                                alloc.put(mb.movie().getMovieId(), new TargetAllocation(mb, targetB));
                            }

                            // Sequential temporal placement (§7)
                            ZonedDateTime cursor = tOpen.plusMinutes(30);
                            String status = isCompleted ? "COMPLETED" : "SCHEDULED";

                            for (TargetAllocation ta : alloc.values()) {
                                int runtime = (ta.em().movie().getRuntimeMinutes() != null)
                                        ? ta.em().movie().getRuntimeMinutes() : 135;
                                Long mlId = resolveBestMovieLanguage(ta.em().movie().getMovieId(), cityId, movieLanguagesByMovie, langCodesById);

                                for (int s = 0; s < ta.targetShows(); s++) {
                                    ZonedDateTime slotStart = cursor;
                                    ZonedDateTime slotEnd = slotStart.plusMinutes(runtime);
                                    if (!slotEnd.isAfter(tClose)) {
                                        generatedShows.add(new ShowRecord(
                                                mlId, sid, d, cap.getScreenCapabilityId(),
                                                slotStart.toInstant(), slotEnd.toInstant(), status
                                        ));
                                        cursor = slotEnd.plusMinutes(bufferMinutes);
                                    } else {
                                        break; // Operating hours boundary reached
                                    }
                                }
                            }

                            // Dynamic Capacity Release to Movie C if auditorium remains available
                            if (competing.size() > alloc.size()) {
                                int offsetC = isDedicated ? 1 : ((scrIdx * 2) + 2) % competing.size();
                                EligibleMovie mc = competing.get(offsetC);
                                int runtimeC = (mc.movie().getRuntimeMinutes() != null) ? mc.movie().getRuntimeMinutes() : 135;
                                Long mlIdC = resolveBestMovieLanguage(mc.movie().getMovieId(), cityId, movieLanguagesByMovie, langCodesById);

                                while (!cursor.plusMinutes(runtimeC).isAfter(tClose)) {
                                    ZonedDateTime slotStart = cursor;
                                    ZonedDateTime slotEnd = slotStart.plusMinutes(runtimeC);
                                    generatedShows.add(new ShowRecord(
                                            mlIdC, sid, d, cap.getScreenCapabilityId(),
                                            slotStart.toInstant(), slotEnd.toInstant(), status
                                    ));
                                    cursor = slotEnd.plusMinutes(bufferMinutes);
                                }
                            }
                        }
                    }
                }
            }
            curDate = curDate.plusDays(1);
        }

        // 3. Non-destructive preservation check & insertion
        if (generatedShows.isEmpty()) {
            log.info("All dates in range [{} to {}] already contain complete schedules. Zero shows modified or recreated.", startDate, endDate);
        } else {
            // 4. Bulk batch insert newly generated shows
            log.info("Batch inserting {} newly generated shows for unpopulated dates...", generatedShows.size());
            String insertShowSql = "INSERT INTO `SHOW` (movie_language_id, screen_id, show_date, screen_capability_id, start_at, end_at, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

            int batchSize = 2000;
            for (int i = 0; i < generatedShows.size(); i += batchSize) {
                List<ShowRecord> batch = generatedShows.subList(i, Math.min(i + batchSize, generatedShows.size()));
                jdbcTemplate.batchUpdate(insertShowSql, batch, batch.size(), (PreparedStatement ps, ShowRecord r) -> {
                    ps.setLong(1, r.movieLanguageId());
                    ps.setLong(2, r.screenId());
                    ps.setDate(3, Date.valueOf(r.showDate()));
                    ps.setLong(4, r.screenCapabilityId());
                    ps.setTimestamp(5, Timestamp.from(r.startAt()));
                    ps.setTimestamp(6, Timestamp.from(r.endAt()));
                    ps.setString(7, r.status());
                });
            }
        }

        // 5. Active Window SHOW_SEAT Eager Materialization (§8.2) - only for shows lacking inventory
        int activeWindowShowCount = 0;
        int activeWindowShowSeatCount = 0;

        LocalDate effectiveActiveStart = startDate.isAfter(activeStart) ? startDate : activeStart;
        LocalDate effectiveActiveEnd = endDate.isBefore(activeEnd) ? endDate : activeEnd;

        if (!effectiveActiveStart.isAfter(effectiveActiveEnd)) {
            log.info("Materializing eager SHOW_SEAT inventory for active window shows lacking inventory [{} to {}]...",
                    effectiveActiveStart, effectiveActiveEnd);

            // Fetch newly inserted active shows lacking show_seat inventory
            String activeShowsQuery = "SELECT s.show_id, s.screen_id, s.show_date, s.movie_language_id " +
                    "FROM `SHOW` s WHERE s.show_date BETWEEN ? AND ? " +
                    "AND s.show_id NOT IN (SELECT DISTINCT show_id FROM SHOW_SEAT) ORDER BY s.show_id ASC";

            List<PersistedShowInfo> activeShows = jdbcTemplate.query(
                    activeShowsQuery,
                    (rs, rowNum) -> new PersistedShowInfo(
                            rs.getLong("show_id"),
                            rs.getLong("screen_id"),
                            rs.getDate("show_date").toLocalDate(),
                            rs.getLong("movie_language_id")
                    ),
                    Date.valueOf(effectiveActiveStart),
                    Date.valueOf(effectiveActiveEnd)
            );
            activeWindowShowCount = activeShows.size();

            // Pre-load physical seats grouped by screen
            List<Seat> allActivePhysicalSeats = seatRepository.findAll().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .sorted(Comparator.comparing(Seat::getSeatId))
                    .toList();
            Map<Long, List<Seat>> physicalSeatsByScreen = allActivePhysicalSeats.stream()
                    .collect(Collectors.groupingBy(Seat::getScreenId));

            Map<Long, Movie> moviesById = allMovies.stream()
                    .collect(Collectors.toMap(Movie::getMovieId, m -> m));
            Map<Long, Long> movieByLangId = allMovieLanguages.stream()
                    .collect(Collectors.toMap(MovieLanguage::getMovieLanguageId, MovieLanguage::getMovieId, (a, b) -> a));
            Map<Long, Theatre> theatresByScreenId = new HashMap<>();
            for (Screen scr : allScreens) {
                theatresByCity.values().stream().flatMap(List::stream)
                        .filter(th -> th.getTheatreId().equals(scr.getTheatreId()))
                        .findFirst()
                        .ifPresent(th -> theatresByScreenId.put(scr.getScreenId(), th));
            }

            String insertSeatSql = "INSERT IGNORE INTO SHOW_SEAT (show_id, seat_id, availability_status) VALUES (?, ?, ?)";
            List<ShowSeatRecord> seatBatch = new ArrayList<>(5000);

            for (PersistedShowInfo psi : activeShows) {
                List<Seat> screenSeats = physicalSeatsByScreen.getOrDefault(psi.screenId(), Collections.emptyList());
                Long mid = movieByLangId.get(psi.movieLanguageId());
                Movie movie = moviesById.get(mid);
                Theatre theatre = theatresByScreenId.get(psi.screenId());
                if (screenSeats.isEmpty() || movie == null || theatre == null) continue;

                DayOfWeek dow = psi.showDate().getDayOfWeek();
                String dayType = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
                        ? "WEEKEND" : "WEEKDAY";
                double rho = movieLifecycleService.calculateRho(movie.getExhibitionStartDate(), movie.getExhibitionEndDate(), psi.showDate());

                for (Seat seat : screenSeats) {
                    double geomWeight = deterministicInventoryEngine.calculateGeometricWeight(seat);
                    double targetOccupancy = deterministicInventoryEngine.calculateTargetOccupancy(
                            movie.getDemandTier(),
                            movie.getPopularityScore(),
                            rho,
                            dayType,
                            theatre.getTheatreTier(),
                            seat.getPricingZone(),
                            geomWeight
                    );
                    String seatState = deterministicInventoryEngine.evaluateSeatState(
                            deterministicInventoryEngine.getDatasetVersion(),
                            deterministicInventoryEngine.getGlobalSeed(),
                            psi.showId(),
                            seat.getSeatId(),
                            psi.showDate(),
                            targetOccupancy,
                            seat.getStatus()
                    );
                    seatBatch.add(new ShowSeatRecord(psi.showId(), seat.getSeatId(), seatState));

                    if (seatBatch.size() >= 5000) {
                        jdbcTemplate.batchUpdate(insertSeatSql, seatBatch, seatBatch.size(), (PreparedStatement ps, ShowSeatRecord ssr) -> {
                            ps.setLong(1, ssr.showId());
                            ps.setLong(2, ssr.seatId());
                            ps.setString(3, ssr.status());
                        });
                        activeWindowShowSeatCount += seatBatch.size();
                        seatBatch.clear();
                    }
                }
            }

            if (!seatBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(insertSeatSql, seatBatch, seatBatch.size(), (PreparedStatement ps, ShowSeatRecord ssr) -> {
                    ps.setLong(1, ssr.showId());
                    ps.setLong(2, ssr.seatId());
                    ps.setString(3, ssr.status());
                });
                activeWindowShowSeatCount += seatBatch.size();
                seatBatch.clear();
            }
        }

        long elapsed = System.currentTimeMillis() - t0;
        log.info("Part 3 generation complete in {}ms! Total shows: {}, Active window shows: {}, Active SHOW_SEAT rows: {}",
                elapsed, generatedShows.size(), activeWindowShowCount, activeWindowShowSeatCount);

        return new GenerationSummary(
                generatedShows.size(),
                activeWindowShowCount,
                activeWindowShowSeatCount,
                elapsed,
                startDate,
                endDate
        );
    }

    private int calculatePriorityScore(Movie movie, String stage) {
        int tierWeight = switch (movie.getDemandTier() != null ? movie.getDemandTier().toUpperCase() : "") {
            case "BLOCKBUSTER" -> 100;
            case "MAJOR_REGIONAL" -> 80;
            case "MID_TIER" -> 60;
            case "RE_RELEASE" -> 50;
            default -> 40;
        };
        int stageWeight = switch (stage != null ? stage.toUpperCase() : "") {
            case "OPENING" -> 50;
            case "PEAK" -> 40;
            case "REGULAR_RUN" -> 30;
            case "REDUCED_RUN" -> 15;
            case "FINAL_DAYS" -> 5;
            default -> 0;
        };
        double popScore = (movie.getPopularityScore() != null) ? movie.getPopularityScore().doubleValue() : 8.0;
        return tierWeight + (int) Math.round(popScore * 2.0) + stageWeight;
    }

    private int calculateTargetShows(String tier, String stage, String dayType) {
        if ("FINAL_DAYS".equalsIgnoreCase(stage)) return 1;
        if ("REDUCED_RUN".equalsIgnoreCase(stage)) return 2;
        if ("REGULAR_RUN".equalsIgnoreCase(stage)) return "WEEKDAY".equalsIgnoreCase(dayType) ? 3 : 4;
        if ("OPENING".equalsIgnoreCase(stage) || "PEAK".equalsIgnoreCase(stage)) return "WEEKDAY".equalsIgnoreCase(dayType) ? 4 : 5;
        return 0;
    }

    private int calculateTheatreBreadth(String tier, String stage) {
        if ("BLOCKBUSTER".equalsIgnoreCase(tier)) {
            if ("OPENING".equalsIgnoreCase(stage) || "PEAK".equalsIgnoreCase(stage)) return 5;
            if ("REGULAR_RUN".equalsIgnoreCase(stage)) return 4;
            if ("REDUCED_RUN".equalsIgnoreCase(stage)) return 3;
            return 1;
        }
        if ("MAJOR_REGIONAL".equalsIgnoreCase(tier)) {
            if ("OPENING".equalsIgnoreCase(stage) || "PEAK".equalsIgnoreCase(stage)) return 4;
            if ("REGULAR_RUN".equalsIgnoreCase(stage)) return 3;
            if ("REDUCED_RUN".equalsIgnoreCase(stage)) return 2;
            return 1;
        }
        if ("MID_TIER".equalsIgnoreCase(tier)) {
            if ("OPENING".equalsIgnoreCase(stage) || "PEAK".equalsIgnoreCase(stage)) return 3;
            if ("REGULAR_RUN".equalsIgnoreCase(stage)) return 2;
            return 1;
        }
        return ("OPENING".equalsIgnoreCase(stage) || "PEAK".equalsIgnoreCase(stage)) ? 2 : 1;
    }

    private int getTurnaroundBufferMinutes(Screen screen, ScreenCapability cap,
                                           Map<Long, String> formats, Map<Long, String> audios) {
        String pfCode = formats.getOrDefault(cap.getPresentationFormatId(), "2D");
        if (Boolean.TRUE.equals(screen.getIsPremium()) || "IMAX".equalsIgnoreCase(pfCode) || "4DX".equalsIgnoreCase(pfCode)) {
            return 30;
        }
        String afCode = audios.getOrDefault(cap.getAudioFormatId(), "DOLBY_5_1");
        if ("DOLBY_ATMOS".equalsIgnoreCase(afCode)) {
            return 30;
        }
        return 20;
    }

    private Long resolveBestMovieLanguage(Long movieId, Long cityId,
                                          Map<Long, List<MovieLanguage>> movieLanguagesByMovie,
                                          Map<Long, String> langCodesById) {
        List<MovieLanguage> mls = movieLanguagesByMovie.getOrDefault(movieId, Collections.emptyList());
        if (mls.isEmpty()) return null;

        List<String> prefLangs = CITY_LANGUAGE_PREFERENCES.getOrDefault(cityId, List.of("en"));
        for (String pref : prefLangs) {
            for (MovieLanguage ml : mls) {
                String code = langCodesById.get(ml.getLanguageId());
                if (pref.equalsIgnoreCase(code)) {
                    return ml.getMovieLanguageId();
                }
            }
        }
        return mls.get(0).getMovieLanguageId();
    }

    private record EligibleMovie(Movie movie, String stage, double rho) {}
    private record TargetAllocation(EligibleMovie em, int targetShows) {}
    private record ShowRecord(Long movieLanguageId, Long screenId, LocalDate showDate, Long screenCapabilityId,
                              Instant startAt, Instant endAt, String status) {}
    private record PersistedShowInfo(Long showId, Long screenId, LocalDate showDate, Long movieLanguageId) {}
    private record ShowSeatRecord(Long showId, Long seatId, String status) {}
}
