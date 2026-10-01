package com.finai.feedback;

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
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private FeedbackCategory category = FeedbackCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FeedbackStatus status = FeedbackStatus.OPEN;

    @Column(name = "admin_reply", length = 2000)
    private String adminReply;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Feedback() {
        // for JPA
    }

    public Feedback(Long userId, String title, String content, FeedbackCategory category) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = FeedbackStatus.OPEN;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public void handle(FeedbackStatus newStatus, String adminReply, Long adminId) {
        this.status = newStatus;
        if (adminReply != null && !adminReply.isBlank()) {
            this.adminReply = adminReply.trim();
        }
        this.handledBy = adminId;
        if (newStatus == FeedbackStatus.RESOLVED || newStatus == FeedbackStatus.CLOSED) {
            this.resolvedAt = Instant.now();
        } else {
            this.resolvedAt = null;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
