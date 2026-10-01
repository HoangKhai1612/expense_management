-- ============================================================
-- V5: Notifications (budget alerts and system messages)
--   de-duplicated on (user_id, type, reference_id) so an alert fires once
--   per budget crossing rather than on every recomputation.
-- ============================================================

CREATE TABLE notifications (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    type           VARCHAR(32)  NOT NULL,
    level          VARCHAR(16)  NOT NULL DEFAULT 'INFO',
    title          VARCHAR(160) NOT NULL,
    message        VARCHAR(500) NOT NULL,
    reference_type VARCHAR(32),
    reference_id   BIGINT,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_level CHECK (level IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_notifications_type  CHECK (type IN ('BUDGET_WARNING', 'BUDGET_EXCEEDED', 'SYSTEM'))
);

CREATE UNIQUE INDEX uq_notifications_dedupe
    ON notifications (user_id, type, reference_type, reference_id)
    WHERE is_read = FALSE;

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read, created_at DESC);

COMMENT ON TABLE  notifications            IS 'In-app messages for a user, primarily budget threshold alerts.';
COMMENT ON COLUMN notifications.level     IS 'INFO < WARNING < CRITICAL; WARNING maps to the 80% threshold, CRITICAL to 100%.';
COMMENT ON COLUMN notifications.reference_id IS 'Primary key of the related row, e.g. budgets.id.';
