-- ============================================================
-- V8: Audit log for administrative actions
-- ============================================================

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    admin_id    BIGINT,
    admin_name  VARCHAR(64),
    action      VARCHAR(48)  NOT NULL,
    target_type VARCHAR(32),
    target_id   BIGINT,
    result      VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS',
    detail      VARCHAR(500),
    ip_address  VARCHAR(64),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_audit_logs_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_logs_admin    ON audit_logs (admin_id, created_at DESC);
CREATE INDEX idx_audit_logs_action   ON audit_logs (action, created_at DESC);
CREATE INDEX idx_audit_logs_target   ON audit_logs (target_type, target_id);

COMMENT ON TABLE  audit_logs            IS 'Append-only trail of privileged operations, for security review and debugging.';
COMMENT ON COLUMN audit_logs.action    IS 'Stable action key, e.g. ADMIN_LOCK_USER, ADMIN_UPDATE_FEEDBACK.';
COMMENT ON COLUMN audit_logs.admin_name IS 'Denormalised so the trail survives account deletion.';
