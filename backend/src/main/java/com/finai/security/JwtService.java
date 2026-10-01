package com.finai.security;

import com.finai.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates HMAC-SHA256 access tokens.
 * The signing key is derived from configuration; it is never hard-coded.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes for HS256; current length is " + secret.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public String issueToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getExpiration());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /** @return the parsed claims, or null when the token is unusable. */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected access token: {}", ex.getMessage());
            return null;
        }
    }

    public long getExpiresInSeconds() {
        return properties.getExpiration().toSeconds();
    }

    public Duration getExpiration() {
        return properties.getExpiration();
    }
}
