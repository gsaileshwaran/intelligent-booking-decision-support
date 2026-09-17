package com.pvk.cinemas.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenRevocationStore {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(String jti, Instant expiration) {
        if (jti != null && expiration != null) {
            revokedTokens.put(jti, expiration);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiration = revokedTokens.get(jti);
        if (expiration == null) {
            return false;
        }
        if (Instant.now().isAfter(expiration)) {
            revokedTokens.remove(jti);
            return false;
        }
        return true;
    }

    public void clear() {
        revokedTokens.clear();
    }
}
