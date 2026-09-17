package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.decision.dto.*;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DecisionNegotiationEngine {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("Asia/Kolkata"));

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    private final SeatScoringEngine seatScoringEngine;
    private final SeatRecommendationEngine recommendationEngine;

    public DecisionNegotiationEngine(ShowRepository showRepository,
                                     ShowSeatRepository showSeatRepository,
                                     ScreenCapabilityRepository screenCapabilityRepository,
                                     ScreenRepository screenRepository,
                                     TheatreRepository theatreRepository,
                                     SeatRepository seatRepository,
                                     SeatScoringEngine seatScoringEngine,
                                     SeatRecommendationEngine recommendationEngine) {
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
        this.seatScoringEngine = seatScoringEngine;
        this.recommendationEngine = recommendationEngine;
    }

    public NegotiationResponse negotiate(Long currentShowId, NegotiationRequest request) {
        int partySize = Math.max(1, request.getPartySize());

        Show currentShow = showRepository.findById(currentShowId).orElse(null);
        if (currentShow == null) {
            return new NegotiationResponse(false, partySize, List.of(), List.of(), List.of());
        }

        // 1. Check if contiguous block exists in current show
        SeatRecommendationResponse currentRecs = recommendationEngine.recommend(
                currentShowId,
                new SeatRecommendationRequest(partySize, "BEST_VIEW")
        );
        boolean contiguousFound = !currentRecs.getRecommendations().isEmpty();

        List<RecommendedBlock> adjacentSplitOptions = new ArrayList<>();
        // If party >= 4, evaluate 2-row adjacent block (e.g. 2 in Row C + 2 in Row D)
        if (partySize >= 4) {
            int half = partySize / 2;
            SeatRecommendationResponse rowC = recommendationEngine.recommend(currentShowId, new SeatRecommendationRequest(half, "BEST_VIEW"));
            if (!rowC.getRecommendations().isEmpty()) {
                RecommendedBlock b1 = rowC.getRecommendations().get(0);
                List<Long> combinedIds = new ArrayList<>(b1.getSeatIds());
                List<String> combinedLabels = new ArrayList<>(b1.getSeatLabels());

                // Dynamically find half seats in adjacent row
                String firstRow = !combinedLabels.isEmpty() ? combinedLabels.get(0).replaceAll("[0-9]", "").trim() : "F";
                char firstChar = !firstRow.isEmpty() ? firstRow.charAt(0) : 'F';
                String nextRow = String.valueOf((char) (firstChar + 1));
                String prevRow = String.valueOf((char) (firstChar - 1));

                List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(currentShowId);
                Set<Long> availableIds = showSeats.stream()
                        .filter(ss -> "AVAILABLE".equalsIgnoreCase(ss.getAvailabilityStatus()))
                        .map(ss -> ss.getId().getSeatId())
                        .collect(Collectors.toSet());

                // Try next row first, fallback to previous row
                List<Seat> candidateSeats = seatRepository.findAllById(availableIds).stream()
                        .filter(s -> !combinedIds.contains(s.getSeatId()))
                        .filter(s -> nextRow.equalsIgnoreCase(s.getRowLabel()))
                        .limit(half)
                        .toList();

                String matchedAdjRow = nextRow;
                if (candidateSeats.size() < half) {
                    candidateSeats = seatRepository.findAllById(availableIds).stream()
                            .filter(s -> !combinedIds.contains(s.getSeatId()))
                            .filter(s -> prevRow.equalsIgnoreCase(s.getRowLabel()))
                            .limit(half)
                            .toList();
                    matchedAdjRow = prevRow;
                }

                if (candidateSeats.size() == half) {
                    for (Seat s : candidateSeats) {
                        combinedIds.add(s.getSeatId());
                        combinedLabels.add(s.getRowLabel() + s.getSeatNumber());
                    }
                    adjacentSplitOptions.add(new RecommendedBlock(
                            "Adjacent 2-Row Split (" + half + " + " + half + ")",
                            "ADJACENT_SPLIT",
                            combinedIds,
                            combinedLabels,
                            84,
                            new BigDecimal("150.00").multiply(BigDecimal.valueOf(partySize)),
                            String.format("Sit directly in front/behind each other across Rows %s and %s.", firstRow, matchedAdjRow)
                    ));
                }
            }
        }

        // 2. Alternative Showtimes in the same theatre for this movie
        List<ShowtimeAlternative> alternativeShowtimes = new ArrayList<>();
        Long currentMovieLanguageId = currentShow.getMovieLanguageId();
        Long currentScreenCapId = currentShow.getScreenCapabilityId();

        ScreenCapability screenCap = screenCapabilityRepository.findById(currentScreenCapId).orElse(null);
        if (screenCap != null) {
            Screen screen = screenRepository.findById(screenCap.getScreenId()).orElse(null);
            if (screen != null) {
                Long currentTheatreId = screen.getTheatreId();
                // Find other shows on screens in this theatre with same movie language
                List<Show> sameTheatreShows = showRepository.findAll().stream()
                        .filter(s -> !s.getShowId().equals(currentShowId))
                        .filter(s -> s.getMovieLanguageId().equals(currentMovieLanguageId))
                        .filter(s -> "SCHEDULED".equalsIgnoreCase(s.getShowStatus()))
                        .filter(s -> {
                            return screenCapabilityRepository.findById(s.getScreenCapabilityId())
                                    .flatMap(sc -> screenRepository.findById(sc.getScreenId()))
                                    .map(scr -> scr.getTheatreId().equals(currentTheatreId))
                                    .orElse(false);
                        })
                        .toList();

                for (Show s : sameTheatreShows) {
                    long availableCount = showSeatRepository.countByIdShowIdAndAvailabilityStatus(s.getShowId(), "AVAILABLE");
                    if (availableCount >= partySize) {
                        SeatRecommendationResponse sRec = recommendationEngine.recommend(s.getShowId(), new SeatRecommendationRequest(partySize, "BEST_VIEW"));
                        int topScore = sRec.getRecommendations().isEmpty() ? 75 : sRec.getRecommendations().get(0).getAverageScore();
                        alternativeShowtimes.add(new ShowtimeAlternative(
                                s.getShowId(),
                                TIME_FMT.format(s.getStartAt()),
                                (int) availableCount,
                                partySize,
                                topScore
                        ));
                    }
                }
            }
        }

        // 3. Alternative Theatres in the same City
        List<TheatreAlternative> alternativeTheatres = new ArrayList<>();
        if (screenCap != null) {
            screenRepository.findById(screenCap.getScreenId()).ifPresent(scr -> {
                theatreRepository.findById(scr.getTheatreId()).ifPresent(th -> {
                    Long cityId = th.getCityId();
                    if (cityId != null) {
                        // Find other theatres in the same city
                        List<Theatre> cityTheatres = theatreRepository.findByCityIdAndStatus(cityId, "ACTIVE").stream()
                                .filter(t -> !t.getTheatreId().equals(th.getTheatreId()))
                                .toList();

                        for (Theatre otherTh : cityTheatres) {
                            // Find a show for this movie in other theatre
                            List<Show> otherShows = showRepository.findAll().stream()
                                    .filter(s -> s.getMovieLanguageId().equals(currentMovieLanguageId))
                                    .filter(s -> "SCHEDULED".equalsIgnoreCase(s.getShowStatus()))
                                    .filter(s -> {
                                        return screenCapabilityRepository.findById(s.getScreenCapabilityId())
                                                .flatMap(sc -> screenRepository.findById(sc.getScreenId()))
                                                .map(oscr -> oscr.getTheatreId().equals(otherTh.getTheatreId()))
                                                .orElse(false);
                                    })
                                    .toList();

                            for (Show os : otherShows) {
                                long avail = showSeatRepository.countByIdShowIdAndAvailabilityStatus(os.getShowId(), "AVAILABLE");
                                if (avail >= partySize) {
                                    SeatRecommendationResponse osRec = recommendationEngine.recommend(os.getShowId(), new SeatRecommendationRequest(partySize, "BEST_VIEW"));
                                    int qScore = osRec.getRecommendations().isEmpty() ? 78 : osRec.getRecommendations().get(0).getAverageScore();
                                    alternativeTheatres.add(new TheatreAlternative(
                                            otherTh.getTheatreId(),
                                            otherTh.getTheatreName(),
                                            os.getShowId(),
                                            TIME_FMT.format(os.getStartAt()),
                                            (int) avail,
                                            qScore
                                    ));
                                    break; // one show per other theatre
                                }
                            }
                        }
                    }
                });
            });
        }

        return new NegotiationResponse(contiguousFound, partySize, adjacentSplitOptions, alternativeShowtimes, alternativeTheatres);
    }
}
