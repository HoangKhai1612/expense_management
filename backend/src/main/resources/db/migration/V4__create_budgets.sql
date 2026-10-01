-- ============================================================
-- V4: Budgets
--   usage is NOT stored. It is derived from transactions at read time so that
--   it can never drift out of sync with the underlying movements.
-- ============================================================

CREATE TABLE budgets (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    category_id  BIGINT         NOT NULL,
    amount       NUMERIC(15, 2) NOT NULL,
    period_type  VARCHAR(16)    NOT NULL DEFAULT 'MONTHLY',
    period_start DATE           NOT NULL,
    period_end   DATE           NOT NULL,
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_budgets_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_budgets_amount      CHECK (amount > 0),
    CONSTRAINT ck_budgets_period      CHECK (period_type IN ('MONTHLY', 'WEEKLY', 'YEARLY')),
    CONSTRAINT ck_budgets_period_span CHECK (period_end >= period_start),
    -- One active budget per user / category / period window.
    CONSTRAINT uq_budgets_user_cat_start UNIQUE (user_id, category_id, period_start)
);

CREATE INDEX idx_budgets_user     ON budgets (user_id, is_active);
CREATE INDEX idx_budgets_period   ON budgets (period_start, period_end);
CREATE INDEX idx_budgets_category ON budgets (category_id);

COMMENT ON TABLE  budgets             IS 'Spending limit per user and category for a date window.';
COMMENT ON COLUMN budgets.period_type IS 'Window granularity. Only MONTHLY is produced by the app UI today.';
COMMENT ON COLUMN budgets.amount     IS 'Planned ceiling for the window. usage% = used / amount.';
