package com.finai.auth;

import com.finai.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AuthResponse", description = "Result of a successful registration or login.")
public record AuthResponse(
        @Schema(description = "Bearer access token, sent as `Authorization: Bearer <token>`")
        String accessToken,
        @Schema(description = "Token type; always Bearer")
        String tokenType,
        @Schema(description = "Access token lifetime in seconds")
        long expiresIn,
        UserSummary user
) {
    public static AuthResponse of(String accessToken, long expiresIn, UserSummary user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }

    @Schema(name = "UserSummary", description = "Public projection of an account. Never contains the password hash.")
    public record UserSummary(
            Long id,
            String email,
            String username,
            String fullName,
            String phone,
            String role,
            UserStatus status,
            Instant lastLoginAt,
            Instant createdAt
    ) {
    }
}
