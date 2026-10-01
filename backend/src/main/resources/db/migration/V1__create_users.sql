-- ============================================================
-- V1: Users and roles
-- ============================================================

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(32)  NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(128),
    phone         VARCHAR(32),
    role_id       BIGINT       NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- Optimistic locking guard: a lock/unlock race must not silently overwrite
    -- the other administrator's change.
    row_version   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT fk_users_role     FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT ck_users_status   CHECK (status IN ('ACTIVE', 'LOCKED', 'DEACTIVATED'))
);

CREATE INDEX idx_users_role   ON users (role_id);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created ON users (created_at DESC);

COMMENT ON TABLE  users            IS 'Application accounts. Passwords are stored only as BCrypt hashes.';
COMMENT ON COLUMN users.status     IS 'ACTIVE = may authenticate, LOCKED = blocked by admin, DEACTIVATED = retired.';
COMMENT ON COLUMN users.email      IS 'Stored lower-cased; unique across the system.';
COMMENT ON COLUMN users.username   IS 'Stored lower-cased; unique across the system.';
