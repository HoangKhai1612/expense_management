package com.finai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC signing secret. Supplied via the JWT_SECRET environment variable. */
    private String secret;

    /** Access token lifetime. */
    private Duration expiration = Duration.ofHours(12);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }
}
