package com.pvk.cinemas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private TokenRevocationStore revocationStore;

    // 256-bit test secret key
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMinutes = 60;

    @BeforeEach
    void setUp() {
        revocationStore = new TokenRevocationStore();
        tokenProvider = new JwtTokenProvider(secret, expirationMinutes);
    }

    @Test
    @DisplayName("Generate token and extract claims correctly")
    void testGenerateAndValidateToken() {
        String token = tokenProvider.generateToken(100L, "customer@example.com", List.of("ROLE_CUSTOMER"));

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("customer@example.com", tokenProvider.getEmail(token));
        assertEquals(100L, tokenProvider.getUserId(token));
        List<String> roles = tokenProvider.getRoles(token);
        assertEquals(1, roles.size());
        assertEquals("ROLE_CUSTOMER", roles.get(0));
    }

    @Test
    @DisplayName("Revoked token fails validation in RevocationStore")
    void testRevokedTokenFailsValidation() {
        String token = tokenProvider.generateToken(100L, "customer@example.com", List.of("ROLE_CUSTOMER"));

        assertTrue(tokenProvider.validateToken(token));

        String jti = tokenProvider.getJti(token);
        Instant expiry = tokenProvider.getExpiration(token);
        revocationStore.revokeToken(jti, expiry);

        assertTrue(revocationStore.isRevoked(jti));
    }

    @Test
    @DisplayName("Malformed token fails validation")
    void testMalformedToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }
}
