package com.finai.auth;

import com.finai.user.UserStatus;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Email or username is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
    public String normalisedIdentifier() {
        return identifier.trim().toLowerCase();
    }

    /** @return a stable machine code describing why a login was refused. */
    public static String codeFor(UserStatus status) {
        return switch (status) {
            case LOCKED -> "ACCOUNT_LOCKED";
            case DEACTIVATED -> "ACCOUNT_DEACTIVATED";
            case ACTIVE -> "INVALID_CREDENTIALS";
        };
    }
}
