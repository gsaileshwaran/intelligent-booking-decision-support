package com.pvk.cinemas.decision.engine;

import com.pvk.cinemas.decision.dto.InteractionFrictionRequest;
import com.pvk.cinemas.decision.dto.InteractionFrictionResponse;
import org.springframework.stereotype.Component;

@Component
public class InteractionFrictionDetector {

    public InteractionFrictionResponse detectInteractionFriction(InteractionFrictionRequest request) {
        if (request == null) {
            return new InteractionFrictionResponse();
        }

        // 1. Concurrency / Hold Failure Friction
        if (request.getFailedHoldAttempts() >= 1) {
            return new InteractionFrictionResponse(
                    true,
                    "CONCURRENCY_CONFLICT",
                    "HIGH",
                    "Seat Selection Concurrency Event",
                    "One or more selected seats were reserved by another customer during the transaction window.",
                    "ACTIVATE_RECOVERY_STRATEGY",
                    request.getFailedHoldAttempts()
            );
        }

        // 2. Showtime Selection Instability (Repeated showtime changes)
        if (request.getTimeSelectionCount() >= 3) {
            String severity = request.getTimeSelectionCount() >= 5 ? "HIGH" : "MEDIUM";
            String timesDetail = (request.getRecentSelectedTimes() != null && !request.getRecentSelectedTimes().isEmpty())
                    ? " (" + String.join(" -> ", request.getRecentSelectedTimes()) + ")"
                    : "";

            return new InteractionFrictionResponse(
                    true,
                    "TIME_SELECTION_INSTABILITY",
                    severity,
                    "Showtime Selection Instability",
                    "The interaction pattern indicates repeated time-selection changes" + timesDetail + " with " + request.getTimeSelectionCount() + " changes within the session window.",
                    "SHOW_RECOMMENDED_SHOWTIME",
                    request.getTimeSelectionCount()
            );
        }

        // 3. Seat Selection Churn (Rapid clicking/unclicking seats)
        if (request.getSeatToggleCount() >= 6) {
            String severity = request.getSeatToggleCount() >= 10 ? "HIGH" : "MEDIUM";
            return new InteractionFrictionResponse(
                    true,
                    "SEAT_SELECTION_CHURN",
                    severity,
                    "Frequent Seat Modification Pattern",
                    "The interaction pattern indicates repeated seat toggling (" + request.getSeatToggleCount() + " changes). Consider using PVK 1-Click Sweet-Spot Auto Alignment.",
                    "AUTO_SELECT_OPTIMAL_BLOCK",
                    request.getSeatToggleCount()
            );
        }

        // 4. Filter Instability (Repeated filter tweaks)
        if (request.getFilterChangeCount() >= 4) {
            return new InteractionFrictionResponse(
                    true,
                    "FILTER_INSTABILITY",
                    "MEDIUM",
                    "Discovery Filter Instability",
                    "The interaction pattern indicates frequent filter changes (" + request.getFilterChangeCount() + " modifications). Resetting to broad candidate view.",
                    "RESET_OR_EXPAND_FILTERS",
                    request.getFilterChangeCount()
            );
        }

        return new InteractionFrictionResponse();
    }
}
