-- ═══════════════════════════════════════════════════════════════
-- V4__password_reset_tokens.sql
-- Personal Blog CMS — Password Reset Tokens (SPEC §7)
-- ═══════════════════════════════════════════════════════════════
-- Single-use tokens for "forgot password" flow.
-- Token stored as SHA-256 hash (raw token sent via email).
-- TTL: 1 hour. Marked used after consumption.

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL UNIQUE,  -- SHA-256 hex
    expires_at  TIMESTAMP   NOT NULL,
    used_at     TIMESTAMP,                     -- NULL = unused
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Fast lookup by token hash (used on every reset request)
-- The UNIQUE constraint already creates an index.

-- Cleanup query: delete expired tokens periodically
-- (called from a scheduled task or manually)
CREATE INDEX idx_password_reset_expires
    ON password_reset_tokens(expires_at)
    WHERE used_at IS NULL;

-- List active tokens per user (for admin UI / debug)
CREATE INDEX idx_password_reset_user
    ON password_reset_tokens(user_id, created_at DESC);
