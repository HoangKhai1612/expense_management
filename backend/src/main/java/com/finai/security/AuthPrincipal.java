package com.finai.security;

import com.finai.user.User;
import com.finai.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated principal. Carries the account status so the request filter can
 * reject a token issued before the account was locked by an administrator.
 */
public class AuthPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final String role;
    private final UserStatus status;

    public AuthPrincipal(User user, String role) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = role;
        this.status = user.getStatus();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
