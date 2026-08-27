package com.booking.intelligent.service;

import com.booking.intelligent.dto.RecommendationRequestDto;
import com.booking.intelligent.dto.RecommendationResponseDto;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final String AI_SERVICE_URL = "http://localhost:8000/api/v1/recommend";

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationItemRepository recommendationItemRepository;

    public RecommendationResponseDto evaluateRecommendations(RecommendationRequestDto request) {
        return evaluateRecommendations(null, request);
    }

    @Transactional
    public RecommendationResponseDto evaluateRecommendations(Long userId, RecommendationRequestDto request) {
        List<Show> candidateShows;
        if (request.getMovieId() != null) {
            candidateShows = showRepository.findByMovieMovieIdAndStatus(request.getMovieId(), ShowStatus.ACTIVE);
        } else {
            candidateShows = showRepository.findAll().stream()
                    .filter(s -> s.getStatus() == ShowStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        // Filter out past/ended shows
        LocalDate today = LocalDate.now();
        candidateShows = candidateShows.stream()
                .filter(s -> s.getShowDate() != null && !s.getShowDate().isBefore(today))
                .collect(Collectors.toList());

        RecommendationResponseDto responseDto = null;

        // 1. Attempt to call external FastAPI AI Decision Engine
        try {
            responseDto = callAiDecisionEngine(request, candidateShows);
        } catch (Exception ignored) {
            // Logically fall through to local Java MCDM fallback
        }

        // 2. Fallback execution if FastAPI is unreachable or returns null
        if (responseDto == null || responseDto.getRankedResults() == null) {
            responseDto = evaluateLocalFallback(request, candidateShows);
        }

        // 3. Persist recommendation session & items for audit if user is authenticated
        if (userId != null && responseDto != null && responseDto.getRankedResults() != null) {
            persistRecommendationSession(userId, request, responseDto);
        }

        return responseDto;
    }

    @SuppressWarnings("unchecked")
    private RecommendationResponseDto callAiDecisionEngine(RecommendationRequestDto request, List<Show> candidateShows) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(800);
        factory.setReadTimeout(1500);
        RestTemplate restTemplate = new RestTemplate(factory);

        Map<String, Object> payload = new HashMap<>();
        payload.put("groupSize", request.getGroupSize() != null ? request.getGroupSize() : 1);

        Map<String, Object> prefs = new HashMap<>();
        if (request.getMaxBudget() != null) prefs.put("maxBudgetPerTicket", request.getMaxBudget());
        if (request.getPreferredTime() != null) prefs.put("preferredTimeStart", request.getPreferredTime());
        if (request.getPreferredSeatType() != null) prefs.put("preferredSeatCategory", request.getPreferredSeatType());
        payload.put("preferences", prefs);

        List<Map<String, Object>> candidatesList = new ArrayList<>();
        Map<Long, Show> showMap = new HashMap<>();

        for (Show show : candidateShows) {
            showMap.put(show.getShowId(), show);

            List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(show.getShowId());
            LocalDateTime now = LocalDateTime.now();

            // Real availability calculation considering active/expired holds
            long availableCount = showSeats.stream()
                    .filter(ss -> ss.getStatus() == ShowSeatStatus.AVAILABLE ||
                            (ss.getStatus() == ShowSeatStatus.HELD && ss.getHeldUntil() != null && ss.getHeldUntil().isBefore(now)))
                    .count();

            List<String> availCategories = showSeats.stream()
                    .filter(ss -> ss.getStatus() == ShowSeatStatus.AVAILABLE ||
                            (ss.getStatus() == ShowSeatStatus.HELD && ss.getHeldUntil() != null && ss.getHeldUntil().isBefore(now)))
                    .map(ss -> ss.getSeat().getSeatType() != null ? ss.getSeat().getSeatType().name() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (availCategories.isEmpty()) {
                availCategories = List.of("REGULAR", "PREMIUM", "VIP");
            }

            Map<String, Object> cMap = new HashMap<>();
            cMap.put("showId", show.getShowId());
            cMap.put("movieId", show.getMovie().getMovieId());
            cMap.put("theatreId", show.getScreen().getTheatre().getTheatreId());
            cMap.put("theatreName", show.getScreen().getTheatre().getName());
            cMap.put("showTime", show.getStartTime().toString());
            cMap.put("basePrice", show.getTicketPrice());
            cMap.put("availableSeats", availableCount);
            cMap.put("availableCategories", availCategories);
            candidatesList.add(cMap);
        }

        payload.put("candidates", candidatesList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(AI_SERVICE_URL, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map body = response.getBody();
            List<Map> ranked = (List<Map>) body.get("rankedResults");
            List<RecommendationResponseDto.RankedResult> results = new ArrayList<>();

            if (ranked != null) {
                for (Map item : ranked) {
                    Long showId = ((Number) item.get("showId")).longValue();
                    Show show = showMap.get(showId);
                    if (show == null) continue;

                    List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(show.getShowId());
                    LocalDateTime now = LocalDateTime.now();
                    long availableCount = showSeats.stream()
                            .filter(ss -> ss.getStatus() == ShowSeatStatus.AVAILABLE ||
                                    (ss.getStatus() == ShowSeatStatus.HELD && ss.getHeldUntil() != null && ss.getHeldUntil().isBefore(now)))
                            .count();

                    Number scoreNum = (Number) item.get("suitabilityScore");
                    BigDecimal score = scoreNum != null ? BigDecimal.valueOf(scoreNum.doubleValue()) : BigDecimal.ZERO;
                    Boolean isBest = (Boolean) item.get("isBestMatch");
                    String altNotice = (String) item.get("alternativeNotice");

                    List<String> factors = (List<String>) item.get("explanationFactors");
                    if (factors == null) {
                        factors = new ArrayList<>();
                        String exp = (String) item.get("explanation");
                        if (exp != null) factors.add(exp);
                    }

                    results.add(RecommendationResponseDto.RankedResult.builder()
                            .rank(((Number) item.get("rank")).intValue())
                            .showId(showId)
                            .movieTitle(show.getMovie().getTitle())
                            .theatreName(show.getScreen().getTheatre().getName())
                            .showDate(show.getShowDate().toString())
                            .startTime(show.getStartTime().toString())
                            .price(show.getTicketPrice())
                            .availableSeats((int) availableCount)
                            .suitabilityScore(score)
                            .explanationFactors(factors)
                            .isBestMatch(isBest != null && isBest)
                            .alternativeNotice(altNotice)
                            .build());
                }
            }

            return RecommendationResponseDto.builder()
                    .engineVersion((String) body.getOrDefault("modelVersion", "v1.0-fastapi-mcdm"))
                    .totalEvaluated(candidateShows.size())
                    .rankedResults(results)
                    .build();
        }

        return null;
    }

    private RecommendationResponseDto evaluateLocalFallback(RecommendationRequestDto request, List<Show> candidateShows) {
        List<RecommendationResponseDto.RankedResult> rankedResults = new ArrayList<>();

        for (Show show : candidateShows) {
            List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(show.getShowId());
            LocalDateTime now = LocalDateTime.now();
            long availableCount = showSeats.stream()
                    .filter(ss -> ss.getStatus() == ShowSeatStatus.AVAILABLE ||
                            (ss.getStatus() == ShowSeatStatus.HELD && ss.getHeldUntil() != null && ss.getHeldUntil().isBefore(now)))
                    .count();

            int requiredGroup = request.getGroupSize() != null ? request.getGroupSize() : 1;

            // 1. Hard Constraint Filter: Group Size Availability
            if (availableCount < requiredGroup) {
                continue;
            }

            // Hard Constraint Filter: Maximum Budget
            if (request.getMaxBudget() != null && show.getTicketPrice().compareTo(request.getMaxBudget()) > 0) {
                continue;
            }

            // 2. Multi-Criteria Scoring (MCDM) Formula: S_i = Sum(w_j * x_ij)
            double priceScore = 1.0;
            if (request.getMaxBudget() != null && request.getMaxBudget().compareTo(BigDecimal.ZERO) > 0) {
                priceScore = Math.max(0.0, 1.0 - (show.getTicketPrice().doubleValue() / request.getMaxBudget().doubleValue()));
            }

            double timeScore = 0.85; // Base timing score
            double seatScore = 0.90; // Preferred seat availability score

            double compositeScore = (0.40 * priceScore) + (0.35 * timeScore) + (0.25 * seatScore);
            double suitabilityPercentage = Math.round(compositeScore * 1000.0) / 10.0;

            List<String> factors = new ArrayList<>();
            factors.add("Within max budget limit (₹" + show.getTicketPrice() + ")");
            factors.add("Available seats (" + availableCount + " open)");
            factors.add("Auditorium: " + show.getScreen().getName());

            RecommendationResponseDto.RankedResult result = RecommendationResponseDto.RankedResult.builder()
                    .showId(show.getShowId())
                    .movieTitle(show.getMovie().getTitle())
                    .theatreName(show.getScreen().getTheatre().getName())
                    .showDate(show.getShowDate().toString())
                    .startTime(show.getStartTime().toString())
                    .price(show.getTicketPrice())
                    .availableSeats((int) availableCount)
                    .suitabilityScore(BigDecimal.valueOf(suitabilityPercentage))
                    .explanationFactors(factors)
                    .isBestMatch(false)
                    .alternativeNotice(null)
                    .build();

            rankedResults.add(result);
        }

        // Rank descending by suitability score
        rankedResults.sort(Comparator.comparing(RecommendationResponseDto.RankedResult::getSuitabilityScore).reversed());

        for (int i = 0; i < rankedResults.size(); i++) {
            rankedResults.get(i).setRank(i + 1);
            if (i == 0) {
                rankedResults.get(i).setBestMatch(true);
            } else {
                rankedResults.get(i).setAlternativeNotice("Alternative Choice (Rank #" + (i + 1) + ")");
            }
        }

        return RecommendationResponseDto.builder()
                .engineVersion("v1.0-local-mcdm-fallback")
                .totalEvaluated(candidateShows.size())
                .rankedResults(rankedResults)
                .build();
    }

    private void persistRecommendationSession(Long userId, RecommendationRequestDto request, RecommendationResponseDto response) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            String contextJson = String.format("{\"movieId\":%s,\"groupSize\":%d,\"maxBudget\":%s,\"preferredTime\":\"%s\",\"preferredSeatType\":\"%s\"}",
                    request.getMovieId(),
                    request.getGroupSize() != null ? request.getGroupSize() : 1,
                    request.getMaxBudget() != null ? request.getMaxBudget().toString() : "null",
                    request.getPreferredTime() != null ? request.getPreferredTime() : "",
                    request.getPreferredSeatType() != null ? request.getPreferredSeatType() : ""
            );

            Recommendation rec = Recommendation.builder()
                    .user(user)
                    .requestContext(contextJson)
                    .modelVersion(response.getEngineVersion())
                    .createdAt(LocalDateTime.now())
                    .build();

            Recommendation savedRec = recommendationRepository.saveAndFlush(rec);

            if (savedRec != null && savedRec.getRecommendationId() != null && response.getRankedResults() != null) {
                for (RecommendationResponseDto.RankedResult res : response.getRankedResults()) {
                    Show show = showRepository.findById(res.getShowId()).orElse(null);
                    if (show == null) continue;

                    String reasonJson = String.format("{\"factors\":%s,\"isBestMatch\":%b}",
                            res.getExplanationFactors() != null ? res.getExplanationFactors().toString() : "[]",
                            res.isBestMatch()
                    );

                    RecommendationItem item = RecommendationItem.builder()
                            .recommendation(savedRec)
                            .show(show)
                            .rank(res.getRank())
                            .suitabilityScore(res.getSuitabilityScore())
                            .reasonData(reasonJson)
                            .build();

                    recommendationItemRepository.saveAndFlush(item);
                }
            }
        } catch (Exception e) {
            // Auditing persistence errors must never crash recommendation response
        }
    }
}
