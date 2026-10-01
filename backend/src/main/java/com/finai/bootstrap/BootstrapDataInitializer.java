package com.finai.bootstrap;

import com.finai.user.Role;
import com.finai.user.RoleRepository;
import com.finai.user.User;
import com.finai.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the bootstrap administrator on first start.
 *
 * The credentials come from environment variables, never from a migration file, so
 * no password hash is ever committed to the repository. If the variables are absent
 * the application still starts; it simply has no administrator, which is reported
 * loudly rather than silently worked around with a default password.
 */
@Component
public class BootstrapDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapDataInitializer(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PasswordEncoder passwordEncoder,
                                    @Value("${app.bootstrap-admin.email:}") String adminEmail,
                                    @Value("${app.bootstrap-admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Role ADMIN is missing. Flyway migration V9__seed_reference_data.sql must run first."));

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("No bootstrap administrator configured. Set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD "
                    + "to create the first administrator account.");
            return;
        }
        if (adminPassword.length() < 8) {
            log.warn("APP_ADMIN_PASSWORD is shorter than 8 characters; the administrator will not be created.");
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Bootstrap administrator {} already exists; nothing to do.", email);
            return;
        }

        User admin = new User(email, "admin", passwordEncoder.encode(adminPassword),
                "System Administrator", null, adminRole.getId());
        userRepository.save(admin);
        log.info("Created bootstrap administrator account {}", email);
    }
}
