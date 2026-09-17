package com.pvk.cinemas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TokenRevocationStoreTest {

    private TokenRevocationStore revocationStore;

    @BeforeEach
    void setUp() {
        revocationStore = new TokenRevocationStore();
    }

    @Test
    @DisplayName("Store and check revoked token before expiry")
    void testRevokeAndCheck() {
        String tokenId = "jwt-test-uuid-12345";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        assertFalse(revocationStore.isRevoked(tokenId));

        revocationStore.revokeToken(tokenId, expiresAt);

        assertTrue(revocationStore.isRevoked(tokenId));
    }

    @Test
    @DisplayName("Expired revoked token is cleaned up")
    void testExpiredRevocationCleanedUp() {
        String tokenId = "jwt-test-uuid-expired";
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES);

        revocationStore.revokeToken(tokenId, expiresAt);

        // When expired, isRevoked returns false because token itself is expired by JWT spec
        assertFalse(revocationStore.isRevoked(tokenId));
    }
}
