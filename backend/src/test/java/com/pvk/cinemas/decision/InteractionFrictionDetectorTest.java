package com.pvk.cinemas.decision;

import com.pvk.cinemas.decision.dto.InteractionFrictionRequest;
import com.pvk.cinemas.decision.dto.InteractionFrictionResponse;
import com.pvk.cinemas.decision.engine.InteractionFrictionDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteractionFrictionDetectorTest {

    private InteractionFrictionDetector detector;

    @BeforeEach
    void setUp() {
        detector = new InteractionFrictionDetector();
    }

    @Test
    @DisplayName("Detect TIME_SELECTION_INSTABILITY when user toggles showtimes 3 or more times")
    void testTimeSelectionInstabilityDetected() {
        InteractionFrictionRequest req = new InteractionFrictionRequest();
        req.setTimeSelectionCount(4);
        req.setTimeWindowSeconds(60);
        req.setRecentSelectedTimes(List.of("7:00 PM", "8:00 PM", "6:30 PM", "7:00 PM"));

        InteractionFrictionResponse resp = detector.detectInteractionFriction(req);

        assertTrue(resp.isDetected());
        assertEquals("TIME_SELECTION_INSTABILITY", resp.getFrictionType());
        assertEquals("MEDIUM", resp.getSeverity());
        assertTrue(resp.getDescription().contains("repeated time-selection changes"));
        assertEquals("SHOW_RECOMMENDED_SHOWTIME", resp.getSuggestedAction());
    }

    @Test
    @DisplayName("Detect CONCURRENCY_CONFLICT immediately upon failed seat hold attempt")
    void testConcurrencyConflictDetected() {
        InteractionFrictionRequest req = new InteractionFrictionRequest();
        req.setFailedHoldAttempts(1);

        InteractionFrictionResponse resp = detector.detectInteractionFriction(req);

        assertTrue(resp.isDetected());
        assertEquals("CONCURRENCY_CONFLICT", resp.getFrictionType());
        assertEquals("HIGH", resp.getSeverity());
        assertEquals("ACTIVATE_RECOVERY_STRATEGY", resp.getSuggestedAction());
    }

    @Test
    @DisplayName("Normal interaction does not trigger friction signals")
    void testNormalInteractionProducesNoFriction() {
        InteractionFrictionRequest req = new InteractionFrictionRequest();
        req.setTimeSelectionCount(1);
        req.setSeatToggleCount(2);
        req.setFilterChangeCount(1);
        req.setFailedHoldAttempts(0);

        InteractionFrictionResponse resp = detector.detectInteractionFriction(req);

        assertFalse(resp.isDetected());
        assertEquals("NONE", resp.getFrictionType());
        assertEquals("NONE", resp.getSeverity());
    }
}
