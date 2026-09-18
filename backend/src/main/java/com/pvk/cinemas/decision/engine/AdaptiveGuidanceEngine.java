package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.decision.dto.RecommendedBlock;
import com.pvk.cinemas.decision.dto.SeatRecommendationRequest;
import com.pvk.cinemas.decision.dto.SeatRecommendationResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdaptiveGuidanceEngine {

    private final SeatRecommendationEngine seatRecommendationEngine;

    public AdaptiveGuidanceEngine(SeatRecommendationEngine seatRecommendationEngine) {
        this.seatRecommendationEngine = seatRecommendationEngine;
    }

    public SeatRecommendationResponse adaptForParty(Long showId, int partySize, String customerPreference) {
        String adaptivePref = (customerPreference != null && !customerPreference.isBlank()) ? customerPreference : "BEST_VIEW";

        SeatRecommendationResponse response = seatRecommendationEngine.recommend(
                showId,
                new SeatRecommendationRequest(partySize, adaptivePref)
        );

        for (RecommendedBlock block : response.getRecommendations()) {
            boolean isContiguous = block.getRationale() != null && block.getRationale().startsWith("Contiguous");
            if (partySize == 1) {
                block.setRationale("Solo viewer focus: Center seat with comfortable viewing angle and screen alignment.");
            } else if (partySize == 2) {
                if (isContiguous) {
                    block.setRationale(String.format("Couple seating: 2 contiguous adjacent seats in Row %s.", extractRow(block)));
                } else {
                    block.setRationale("Couple seating: 2 nearby seats in the preferred viewing area.");
                }
            } else if (block.getRationale() == null || block.getRationale().isEmpty()) {
                if (isContiguous) {
                    block.setRationale(String.format("Group seating: %d contiguous seats together with easy row entry.", partySize));
                } else {
                    block.setRationale(String.format("No single-row contiguous block of %d is currently available. Alternative group seating (Paired group): %d seats arranged in close proximity.", partySize, partySize));
                    block.setTitle("Alternative Group Seating");
                    block.setCategory("SPLIT_GROUP");
                }
            }
        }

        return response;
    }

    public com.pvk.cinemas.decision.dto.JourneyStageGuidanceResponse evaluateJourneyGuidance(com.pvk.cinemas.decision.dto.JourneyStageGuidanceRequest req) {
        if (req == null) {
            return new com.pvk.cinemas.decision.dto.JourneyStageGuidanceResponse("CITY_DISCOVERY", "Select Your City", "Choose a city to explore local PVK multiplexes and real-time showtimes.", "SELECT_CITY", List.of("5 metropolitan zones available with live inventory"));
        }

        String stage = req.getCurrentStage() != null ? req.getCurrentStage().toUpperCase() : "CITY_DISCOVERY";
        String title;
        String prompt;
        String nextAction;
        List<String> tips = new java.util.ArrayList<>();

        switch (stage) {
            case "CITY_DISCOVERY" -> {
                title = "City Selection Stage";
                prompt = req.getCityName() != null ? "You are viewing " + req.getCityName() + ". Choose a movie from current releases." : "Choose your city to discover local PVK multiplexes.";
                nextAction = "SELECT_MOVIE";
                tips.add("Real-time pricing and format capabilities are scoped to your selected city.");
                tips.add("IMAX 70mm and 4DX immersive auditoriums available in select multiplexes.");
            }
            case "MOVIE_SELECTION", "SHOW_SELECTION" -> {
                title = "Theatre & Showtime Selection";
                String movie = req.getMovieTitle() != null ? req.getMovieTitle() : "your movie";
                prompt = "Choose a theatre and showtime for " + movie + ". We recommend checking IMAX & evening slots.";
                nextAction = "SELECT_SHOWTIME";
                tips.add("Use PVK Intelligent Candidate Match to automatically resolve time vs format trade-offs.");
                tips.add("Shows with higher seat availability provide wider contiguous seat selection options.");
            }
            case "SEAT_SELECTION" -> {
                title = "Auditorium Seat Optimization";
                if (req.getSelectedSeatCount() == 0) {
                    prompt = "Select your seats. The preferred viewing area is centered at a comfortable distance from the screen.";
                    nextAction = "SELECT_SEATS";
                    tips.add("Centered seats at mid-auditorium depth offer natural eye-level perspective with minimal neck strain.");
                    tips.add("Dolby Atmos sound immersion is calibrated around the central seating area.");
                } else {
                    prompt = "You have selected " + req.getSelectedSeatCount() + " seat(s). Review viewing quality score and proceed to hold.";
                    nextAction = "INITIATE_HOLD";
                    tips.add("Holding seats locks inventory for 10 minutes, protecting against concurrent reservations.");
                }
            }
            case "SEAT_HOLD", "HOLD_ACTIVE" -> {
                title = "Active Seat Reservation Hold";
                int mins = req.getHoldSecondsRemaining() / 60;
                int secs = req.getHoldSecondsRemaining() % 60;
                String timeFormatted = String.format("%02d:%02d", Math.max(0, mins), Math.max(0, secs));
                prompt = "Your selected seats are held for " + timeFormatted + ". Complete dummy payment to confirm booking.";
                nextAction = "PROCEED_TO_CHECKOUT";
                tips.add("Hold token guarantees exclusive seat allocation during checkout.");
                tips.add("Simulated payment requires no real financial transaction.");
            }
            case "BOOKED", "BOOKING_COMPLETE" -> {
                title = "Booking Confirmed";
                prompt = "Your booking is confirmed! Show your QR code / booking reference at the theatre entrance.";
                nextAction = "VIEW_TICKETS";
                tips.add("Booking reference is stored in your transaction history.");
            }
            default -> {
                title = "Cinema Discovery Guidance";
                prompt = "Follow the guided steps to discover movies, compare formats, and secure optimal seats.";
                nextAction = "EXPLORE_CATALOGUE";
                tips.add("PVK Intelligent Decision Engine assists you at every stage of the booking flow.");
            }
        }

        if (req.getDetectedFrictionType() != null && !"NONE".equalsIgnoreCase(req.getDetectedFrictionType())) {
            tips.add("Active guidance: " + req.getDetectedFrictionType() + " detected — smart recovery options available.");
        }

        return new com.pvk.cinemas.decision.dto.JourneyStageGuidanceResponse(stage, title, prompt, nextAction, tips);
    }

    private String extractRow(RecommendedBlock block) {
        if (block == null || block.getSeatLabels() == null || block.getSeatLabels().isEmpty()) {
            return "";
        }
        String first = block.getSeatLabels().get(0);
        return first.replaceAll("[0-9]", "");
    }
}
