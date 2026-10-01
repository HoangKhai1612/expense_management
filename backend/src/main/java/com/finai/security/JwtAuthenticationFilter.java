package com.finai.security;

import com.finai.user.Role;
import com.finai.user.RoleRepository;
import com.finai.user.User;
import com.finai.user.UserRepository;
import com.finai.user.UserStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates the bearer token on every protected request.
 *
 * Account status is re-read from the database on each request rather than trusted
 * from the token, so an administrator locking a user takes effect on that user's
 * very next call instead of whenever their token happens to expire.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   RoleRepository roleRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(header.substring(PREFIX.length()).trim(), request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        Claims claims = jwtService.parse(token);
        if (claims == null) {
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            log.debug("Access token subject is not a numeric id");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.debug("Access token refers to account {} which no longer exists", userId);
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            // A token minted while the account was active is no longer honoured.
            log.info("Rejected request for account {} because its status is {}", userId, user.getStatus());
            return;
        }

        String role = roleRepository.findById(user.getRoleId())
                .map(Role::getName)
                .orElse("USER");
        AuthPrincipal principal = new AuthPrincipal(user, role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
