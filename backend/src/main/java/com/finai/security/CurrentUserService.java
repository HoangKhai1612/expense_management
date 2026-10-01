package com.finai.security;

import com.finai.common.ApiException;
import com.finai.user.User;
import com.finai.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Single access point for "who is calling?". Controllers never read
 * SecurityContextHolder directly, so an unauthenticated call always fails the
 * same way instead of producing a null identifier deep in a service.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw ApiException.unauthorized("Authentication is required.");
        }
        return principal;
    }

    public Long requireUserId() {
        return requirePrincipal().getId();
    }

    public User requireUser() {
        Long id = requireUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.unauthorized("The authenticated account no longer exists."));
    }
}
