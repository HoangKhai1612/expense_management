package com.finai.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private CategoryType type;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "color", length = 16)
    private String color;

    /** True for the administrator-managed catalogue, false for a user-owned category. */
    @Column(name = "is_system", nullable = false)
    private boolean systemCategory = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Owner of a personal category; null for a system category. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Category() {
        // for JPA
    }

    private Category(String name, String code, CategoryType type, String icon, String color,
                     boolean systemCategory, Long userId) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.systemCategory = systemCategory;
        this.userId = userId;
    }

    public static Category system(String name, String code, CategoryType type, String icon, String color) {
        return new Category(name, code, type, icon, color, true, null);
    }

    public static Category personal(String name, String code, CategoryType type,
                                    String icon, String color, Long userId) {
        return new Category(name, code, type, icon, color, false, userId);
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public void changeDetails(String name, CategoryType type, String icon, String color) {
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public CategoryType getType() {
        return type;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public boolean isSystemCategory() {
        return systemCategory;
    }

    public boolean isActive() {
        return active;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
