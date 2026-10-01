package com.finai.auth;

import com.finai.common.ApiException;
import com.finai.security.JwtService;
import com.finai.user.Role;
import com.finai.user.RoleRepository;
import com.finai.user.User;
import com.finai.user.UserRepository;
import com.finai.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates an account and immediately returns a usable session, so the client
     * does not have to re-submit the password it just registered with.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.normalisedEmail();
        String username = request.normalisedUsername();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS",
                    "An account already exists for this email address.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("USERNAME_ALREADY_EXISTS",
                    "This username is already taken.");
        }

        Role role = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Role USER is missing. Database migrations V9 must have run."));

        User user = new User(email, username, passwordEncoder.encode(request.password()),
                blankToNull(request.fullName()), blankToNull(request.phone()), role.getId());
        user = userRepository.save(user);

        // The identifier is logged, never the password.
        log.info("Registered account {} ({})", user.getId(), email);

        return issue(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.normalisedIdentifier();

        Optional<User> found = userRepository.findByEmailIgnoreCase(identifier);
        if (found.isEmpty()) {
            found = userRepository.findByUsernameIgnoreCase(identifier);
        }

        if (found.isEmpty()) {
            // Run a hash comparison anyway so a missing account is not
            // distinguishable from a wrong password by response time.
            passwordEncoder.matches(request.password(),
                    "$2a$12$invalidinvalidinvalidinvalidinvalidinvalidinvalidinvalidinv");
            throw invalidCredentials();
        }

        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.info("Failed login for account {} ({}): wrong password", user.getId(), identifier);
            throw invalidCredentials();
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            log.info("Rejected login for account {} because it is locked", user.getId());
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_LOCKED", "This account has been locked. Please contact support.");
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_DEACTIVATED", "This account has been deactivated.");
        }

        user.recordLogin(Instant.now());
        log.info("Successful login for account {} ({})", user.getId(), user.getEmail());

        return issue(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserSummary describe(User user) {
        String role = roleRepository.findById(user.getRoleId())
                .map(com.finai.user.Role::getName)
                .orElse(Role.USER);
        return toSummary(user, role);
    }

    AuthResponse issue(User user) {
        String role = roleRepository.findById(user.getRoleId())
                .map(com.finai.user.Role::getName)
                .orElse(Role.USER);
        String token = jwtService.issueToken(user.getId(), user.getEmail(), role);
        return AuthResponse.of(token, jwtService.getExpiresInSeconds(), toSummary(user, role));
    }

    static AuthResponse.UserSummary toSummary(User user, String role) {
        return new AuthResponse.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getPhone(),
                role,
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    private ApiException invalidCredentials() {
        return new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS", "Email/username or password is incorrect.");
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
