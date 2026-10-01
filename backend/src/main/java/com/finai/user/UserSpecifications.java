package com.finai.user;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

/** Searchable criteria for the admin user list (keyword + status filter). */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> emailContains(String keyword) {
        String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), like);
    }

    public static Specification<User> usernameContains(String keyword) {
        String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("username")), like);
    }

    public static Specification<User> fullNameContains(String keyword) {
        String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), like);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<User> createdAfter(Instant since) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
