-- ============================================================
-- V2: Categories
--   is_system = TRUE   -> global catalogue managed by ADMIN (user_id IS NULL)
--   is_system = FALSE  -> personal category owned by exactly one user (user_id NOT NULL)
-- ============================================================

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    type        VARCHAR(16)  NOT NULL,
    icon        VARCHAR(64),
    color       VARCHAR(16),
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    user_id     BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_categories_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_categories_scope CHECK (
        (is_system = TRUE  AND user_id IS NULL) OR
        (is_system = FALSE AND user_id IS NOT NULL)
    )
);

-- A system category code is unique globally; a personal code is unique per user.
CREATE UNIQUE INDEX uq_categories_system_code ON categories (code) WHERE is_system = TRUE;
CREATE UNIQUE INDEX uq_categories_user_code   ON categories (user_id, code) WHERE is_system = FALSE;
CREATE INDEX idx_categories_user ON categories (user_id);
CREATE INDEX idx_categories_type ON categories (type, is_active);

COMMENT ON TABLE  categories           IS 'Spending/income categories. System scope is administered; personal scope belongs to one user.';
COMMENT ON COLUMN categories.code     IS 'Stable machine key, e.g. FOOD, TRANSPORT. Unique within its scope.';
COMMENT ON COLUMN categories.is_active IS 'Soft delete flag. Inactive categories are hidden from pickers but keep historical transactions.';
