package com.finai.auth;

import com.finai.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "Username may only contain letters, digits, dot, underscore and hyphen")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain a lowercase letter, an uppercase letter and a digit")
        String password,

        @Size(max = 128, message = "Full name must not exceed 128 characters")
        String fullName,

        @Pattern(regexp = "^$|^[0-9+\\- ]{6,32}$", message = "Phone number format is not valid")
        String phone
) {
    public String normalisedEmail() {
        return email.trim().toLowerCase();
    }

    public String normalisedUsername() {
        return username.trim().toLowerCase();
    }
}
