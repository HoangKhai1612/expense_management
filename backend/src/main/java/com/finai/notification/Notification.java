package com.finai.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 16)
    private NotificationLevel level = NotificationLevel.INFO;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "reference_type", length = 32)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
        // for JPA
    }

    public Notification(Long userId, String type, NotificationLevel level, String title, String message,
                        String referenceType, Long referenceId) {
        this.userId = userId;
        this.type = type;
        this.level = level;
        this.title = title;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
