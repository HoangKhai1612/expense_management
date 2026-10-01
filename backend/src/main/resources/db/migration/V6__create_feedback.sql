-- ============================================================
-- V6: Feedback tickets
-- ============================================================

CREATE TABLE feedback (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(160) NOT NULL,
    content       VARCHAR(2000) NOT NULL,
    category      VARCHAR(32)  NOT NULL DEFAULT 'GENERAL',
    status        VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    admin_reply   VARCHAR(2000),
    handled_by    BIGINT,
    resolved_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_feedback_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_handler FOREIGN KEY (handled_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_feedback_status   CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_feedback_category CHECK (category IN ('BUG', 'FEATURE', 'UI', 'PERFORMANCE', 'OTHER'))
);

CREATE INDEX idx_feedback_user   ON feedback (user_id, created_at DESC);
CREATE INDEX idx_feedback_status ON feedback (status, created_at DESC);

COMMENT ON TABLE feedback            IS 'Tickets raised by end users and triaged by administrators.';
COMMENT ON COLUMN feedback.status   IS 'OPEN -> INVESTIGATING -> RESOLVED -> CLOSED lifecycle.';
COMMENT ON COLUMN feedback.handled_by IS 'Admin who last changed the status; NULL until first action.';
