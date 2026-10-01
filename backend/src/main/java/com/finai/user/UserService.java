package com.finai.user;

import com.finai.common.ApiException;
import com.finai.security.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    public record UpdateProfileRequest(
            @Size(max = 128, message = "Full name must not exceed 128 characters")
            String fullName,

            @Pattern(regexp = "^$|^[0-9+\\- ]{6,32}$", message = "Phone number format is not valid")
            String phone
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                    message = "New password must contain a lowercase letter, an uppercase letter and a digit")
            String newPassword
    ) {
    }

    public record ProfileView(
            Long id,
            @Email String email,
            String username,
            String fullName,
            String phone,
            UserStatus status,
            Instant lastLoginAt,
            Instant createdAt
    ) {
    }

    @Transactional(readOnly = true)
    public ProfileView getProfile() {
        User user = currentUserService.requireUser();
        return toView(user);
    }

    @Transactional
    public ProfileView updateProfile(@Valid UpdateProfileRequest request) {
        User user = currentUserService.requireUser();
        user.changeProfile(blankToNull(request.fullName()), blankToNull(request.phone()));
        return toView(userRepository.save(user));
    }

    @Transactional
    public void changePassword(@Valid ChangePasswordRequest request) {
        User user = currentUserService.requireUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("CURRENT_PASSWORD_INCORRECT",
                    "The current password is not correct.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("PASSWORD_UNCHANGED",
                    "The new password must be different from the current password.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    static ProfileView toView(User user) {
        return new ProfileView(user.getId(), user.getEmail(), user.getUsername(),
                user.getFullName(), user.getPhone(), user.getStatus(),
                user.getLastLoginAt(), user.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
