-- ============================================================
-- V3: Transactions (income / expense)
--   Ownership is enforced by transactions.user_id NOT NULL.
-- ============================================================

CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    category_id      BIGINT         NOT NULL,
    type             VARCHAR(16)    NOT NULL,
    amount           NUMERIC(15, 2) NOT NULL,
    note            VARCHAR(500),
    transaction_date DATE           NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_transactions_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_transactions_type   CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0)
);

-- Every list screen filters by owner then by date; this is the primary access path.
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date DESC);
CREATE INDEX idx_transactions_user_type ON transactions (user_id, type, transaction_date DESC);
CREATE INDEX idx_transactions_category  ON transactions (category_id);
CREATE INDEX idx_transactions_created   ON transactions (created_at DESC);

COMMENT ON TABLE  transactions            IS 'Money movements owned by exactly one user. Cross-user access is rejected by the API layer.';
COMMENT ON COLUMN transactions.amount    IS 'Strictly positive; direction is carried by type, not by sign.';
COMMENT ON COLUMN transactions.transaction_date IS 'Date the user assigned to the entry; may differ from created_at.';
