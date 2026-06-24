-- V8: Comments system
-- Supports nested replies (parent_id self-reference), moderation queue (status),
-- soft delete (deleted_at). IP and user-agent stored for spam filtering.

CREATE TABLE comments (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT       NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    parent_id       BIGINT       REFERENCES comments(id) ON DELETE CASCADE,
    author_name     VARCHAR(120) NOT NULL,
    author_email    CITEXT       NOT NULL,
    content         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'approved', 'rejected', 'spam', 'deleted')),
    ip_address      INET,
    user_agent      VARCHAR(500),
    moderated_by    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    moderated_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_comments_post_status   ON comments(post_id, status, created_at);
CREATE INDEX idx_comments_parent        ON comments(parent_id);
CREATE INDEX idx_comments_status        ON comments(status, created_at DESC);
CREATE INDEX idx_comments_email         ON comments(author_email);
