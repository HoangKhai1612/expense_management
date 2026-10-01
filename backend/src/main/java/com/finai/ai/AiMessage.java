package com.finai.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One turn of an AI conversation, persisted so the transcript is auditable. */
@Entity
@Table(name = "ai_messages")
public class AiMessage {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";
    public static final String ROLE_SYSTEM = "SYSTEM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** The verified facts the answer was built from, one `key=value` per line. */
    @Column(name = "facts", columnDefinition = "text")
    private String facts;

    @Column(name = "engine", nullable = false, length = 32)
    private String engine = "LOCAL";

    @Column(name = "latency_ms")
    private Long latencyMs;

    /** False means the reply carried no database fact, so it must contain no figure. */
    @Column(name = "has_grounding_facts", nullable = false)
    private boolean hasGroundingFacts = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AiMessage() {
        // for JPA
    }

    public AiMessage(Long conversationId, Long userId, String role, String content, String facts,
                     String engine, Long latencyMs, boolean hasGroundingFacts) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.facts = facts;
        this.engine = engine;
        this.latencyMs = latencyMs;
        this.hasGroundingFacts = hasGroundingFacts;
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getFacts() {
        return facts;
    }

    public String getEngine() {
        return engine;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public boolean isHasGroundingFacts() {
        return hasGroundingFacts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
