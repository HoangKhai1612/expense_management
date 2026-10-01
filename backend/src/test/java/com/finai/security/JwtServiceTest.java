package com.finai.security;

import com.finai.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtService serviceWith(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpiration(Duration.ofHours(1));
        return new JwtService(properties);
    }

    @Test
    @DisplayName("a freshly issued token carries the subject, email and role")
    void issuesTokenWithIdentityClaims() {
        Claims claims = serviceWith(SECRET).parse(serviceWith(SECRET).issueToken(42L, "a@b.com", "ADMIN"));

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("a@b.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a secret shorter than 32 bytes fails fast at construction")
    void rejectsWeakSecret() {
        assertThatThrownBy(() -> serviceWith("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void rejectsForeignSignature() {
        String otherSecret = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        String token = serviceWith(otherSecret).issueToken(1L, "a@b.com", "USER");

        assertThat(serviceWith(SECRET).parse(token)).isNull();
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void rejectsTamperedToken() {
        String token = serviceWith(SECRET).issueToken(1L, "a@b.com", "USER");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";

        assertThat(serviceWith(SECRET).parse(tampered)).isNull();
    }

    @Test
    @DisplayName("garbage and empty tokens are rejected rather than throwing")
    void rejectsGarbage() {
        JwtService service = serviceWith(SECRET);

        assertThat(service.parse("not-a-token")).isNull();
        assertThat(service.parse("")).isNull();
        assertThat(service.parse(null)).isNull();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpiredToken() throws InterruptedException {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpiration(Duration.ofMillis(1));
        String token = new JwtService(properties).issueToken(1L, "a@b.com", "USER");

        Thread.sleep(1500);

        assertThat(new JwtService(properties).parse(token)).isNull();
    }

    @Test
    @DisplayName("the reported expiry matches the configured duration")
    void reportsConfiguredExpiry() {
        assertThat(serviceWith(SECRET).getExpiresInSeconds()).isEqualTo(3600);
    }
}
