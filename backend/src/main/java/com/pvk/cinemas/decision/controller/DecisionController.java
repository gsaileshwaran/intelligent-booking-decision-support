package com.pvk.cinemas.decision.controller;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.decision.dto.*;
import com.pvk.cinemas.decision.engine.*;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/decision")
public class DecisionController {

    private final SeatScoringEngine seatScoringEngine;
    private final AdaptiveGuidanceEngine adaptiveGuidanceEngine;
    private final DecisionGuidanceEngine decisionGuidanceEngine;
    private final DecisionNegotiationEngine decisionNegotiationEngine;
    private final ShowRecommendationEngine showRecommendationEngine;
    private final InteractionFrictionDetector interactionFrictionDetector;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;

    public DecisionController(SeatScoringEngine seatScoringEngine,
                              AdaptiveGuidanceEngine adaptiveGuidanceEngine,
                              DecisionGuidanceEngine decisionGuidanceEngine,
                              DecisionNegotiationEngine decisionNegotiationEngine,
                              ShowRecommendationEngine showRecommendationEngine,
                              InteractionFrictionDetector interactionFrictionDetector,
                              ShowSeatRepository showSeatRepository,
                              SeatRepository seatRepository) {
        this.seatScoringEngine = seatScoringEngine;
        this.adaptiveGuidanceEngine = adaptiveGuidanceEngine;
        this.decisionGuidanceEngine = decisionGuidanceEngine;
        this.decisionNegotiationEngine = decisionNegotiationEngine;
        this.showRecommendationEngine = showRecommendationEngine;
        this.interactionFrictionDetector = interactionFrictionDetector;
        this.showSeatRepository = showSeatRepository;
        this.seatRepository = seatRepository;
    }

    @GetMapping("/shows/{showId}/seat-scores")
    public ResponseEntity<ApiResponse<List<SeatScoreDTO>>> getSeatScores(@PathVariable Long showId) {
        List<ShowSeat> showSeats = showSeatRepository.findByIdShowId(showId);
        List<Long> seatIds = showSeats.stream().map(ss -> ss.getId().getSeatId()).toList();
        List<Seat> seats = seatRepository.findAllById(seatIds);

        List<SeatScoreDTO> scores = seats.stream().map(seatScoringEngine::scoreSeat).toList();
        return ResponseEntity.ok(ApiResponse.ok(scores));
    }

    @RequestMapping(value = "/shows/{showId}/recommend", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SeatRecommendationResponse>> getRecommendations(
            @PathVariable Long showId,
            @RequestParam(name = "partySize", required = false, defaultValue = "2") Integer partySize,
            @RequestParam(name = "preference", required = false, defaultValue = "BEST_VIEW") String preference,
            @RequestBody(required = false) SeatRecommendationRequest request) {
        int size = (request != null && request.getPartySize() > 0) ? request.getPartySize() : (partySize != null ? partySize : 2);
        String pref = (request != null && request.getPreference() != null) ? request.getPreference() : (preference != null ? preference : "BEST_VIEW");
        SeatRecommendationResponse response = adaptiveGuidanceEngine.adaptForParty(showId, size, pref);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/shows/{showId}/guidance")
    public ResponseEntity<ApiResponse<DecisionGuidanceResponse>> getGuidance(
            @PathVariable Long showId,
            @RequestBody DecisionGuidanceRequest request) {
        DecisionGuidanceResponse response = decisionGuidanceEngine.evaluateGuidance(showId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @RequestMapping(value = "/shows/{showId}/negotiate", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<NegotiationResponse>> negotiate(
            @PathVariable Long showId,
            @RequestParam(name = "partySize", required = false, defaultValue = "2") Integer partySize,
            @RequestBody(required = false) NegotiationRequest request) {
        int size = (request != null && request.getPartySize() > 0) ? request.getPartySize() : (partySize != null ? partySize : 2);
        NegotiationResponse response = decisionNegotiationEngine.negotiate(showId, new NegotiationRequest(size, null));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Movie Show Recommendation — with full hard constraint and priority mode support.
     *
     * Hard constraints (shows not satisfying these are EXCLUDED, not penalised):
     *   cityId      — theatre must be in this city (DB relationship)
     *   languageCode — show must be in this language (e.g. "en", "ta")
     *   dateFrom    — show date >= dateFrom (ISO date: 2026-09-20)
     *   dateTo      — show date <= dateTo
     *   partySize   — show must have >= partySize available seats
     *   budgetMaxTotal — partySize × ticketPrice <= budgetMaxTotal
     *
     * Priority modes (soft ranking weights):
     *   BEST_PRICE  — cheapest shows ranked highest
     *   BEST_SEATS  — largest contiguous seat block ranked highest
     *   BEST_TIME   — closest-to-preferred-time shows ranked highest
     *   BALANCED    — balanced multi-factor ranking (default)
     */
    @RequestMapping(value = "/movies/{movieId}/recommend-shows", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<ShowRecommendationResponse>> recommendShows(
            @PathVariable Long movieId,
            @RequestParam(name = "cityId", required = false) Long cityId,
            @RequestParam(name = "languageCode", required = false) String languageCode,
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "formatPreference", required = false) String formatPreference,
            @RequestParam(name = "timePreference", required = false) String timePreference,
            @RequestParam(name = "priority", required = false, defaultValue = "BALANCED") String priority,
            @RequestParam(name = "theatreId", required = false) Long theatreId,
            @RequestParam(name = "partySize", required = false, defaultValue = "2") Integer partySize,
            @RequestParam(name = "budgetMax", required = false) BigDecimal budgetMax,
            @RequestParam(name = "budgetMaxTotal", required = false) BigDecimal budgetMaxTotal,
            @RequestBody(required = false) ShowRecommendationRequest request) {

        ShowRecommendationRequest req = request != null ? request : new ShowRecommendationRequest();

        // Merge query params into request object (query params take precedence over body defaults)
        if (cityId != null && req.getCityId() == null) req.setCityId(cityId);
        if (languageCode != null && req.getLanguageCode() == null) req.setLanguageCode(languageCode);
        if (dateFrom != null && req.getDateFrom() == null) req.setDateFrom(dateFrom);
        if (dateTo != null && req.getDateTo() == null) req.setDateTo(dateTo);
        if (formatPreference != null && req.getFormatPreference() == null) req.setFormatPreference(formatPreference);
        if (timePreference != null && req.getTimePreference() == null) req.setTimePreference(timePreference);
        if (priority != null) req.setPriority(priority);
        if (theatreId != null && req.getTheatreId() == null) req.setTheatreId(theatreId);
        if (partySize != null && partySize > 0) req.setPartySize(partySize);
        if (budgetMaxTotal != null && req.getBudgetMaxTotal() == null) req.setBudgetMaxTotal(budgetMaxTotal);
        if (budgetMax != null && req.getBudgetMax() == null) req.setBudgetMax(budgetMax);

        ShowRecommendationResponse response = showRecommendationEngine.recommendShows(movieId, req);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/friction/evaluate")
    public ResponseEntity<ApiResponse<InteractionFrictionResponse>> evaluateInteractionFriction(
            @RequestBody InteractionFrictionRequest request) {
        InteractionFrictionResponse response = interactionFrictionDetector.detectInteractionFriction(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/guidance/journey-stage")
    public ResponseEntity<ApiResponse<JourneyStageGuidanceResponse>> getJourneyStageGuidance(
            @RequestBody JourneyStageGuidanceRequest request) {
        JourneyStageGuidanceResponse response = adaptiveGuidanceEngine.evaluateJourneyGuidance(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
