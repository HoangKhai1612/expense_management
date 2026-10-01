package com.finai.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    /** BCrypt hash. The plaintext is never stored nor logged. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 128)
    private String fullName;

    @Column(name = "phone", length = 32)
    private String phone;

    /** FK to roles.name lookup table; resolved lazily to keep User free of eager joins. */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "row_version")
    private Long rowVersion;

    protected User() {
        // for JPA
    }

    public User(String email, String username, String passwordHash, String fullName, String phone,
                Long roleId) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.roleId = roleId;
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void recordLogin(Instant at) {
        this.lastLoginAt = at;
    }

    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }

    public void changeProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getRoleId() {
        return roleId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
