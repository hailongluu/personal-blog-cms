-- ═══════════════════════════════════════════════════════════════
-- V1__extensions_and_initial.sql
-- Personal Blog CMS — Extensions + Initial Schema
-- ═══════════════════════════════════════════════════════════════
-- This migration sets up PostgreSQL extensions and creates the
-- core tables defined in SPEC section 9 (19 tables).
-- ═══════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────
-- 1. Extensions
-- ─────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- uuid_generate_v4()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";       -- gen_random_uuid(), crypt()
CREATE EXTENSION IF NOT EXISTS "citext";         -- case-insensitive text
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- trigram indexes for search

-- ─────────────────────────────────────────────────────────────
-- 2. Enums (using CHECK constraints for portability)
-- ─────────────────────────────────────────────────────────────
-- Note: Postgres native enums work but are harder to migrate.
-- Using VARCHAR + CHECK for flexibility per SPEC section 9.6.

-- ─────────────────────────────────────────────────────────────
-- 3. Core tables — Roles & Users
-- ─────────────────────────────────────────────────────────────

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,    -- 'admin', 'editor', 'author'
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           CITEXT       NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,        -- bcrypt
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      TEXT,
    bio             TEXT,
    role_id         BIGINT       NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_email      ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role       ON users(role_id);
CREATE INDEX idx_users_active     ON users(is_active) WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────
-- 4. Sessions (HttpOnly cookie auth tracking)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token   VARCHAR(500) NOT NULL UNIQUE,
    user_agent      TEXT,
    ip_address      INET,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ
);

CREATE INDEX idx_sessions_user      ON sessions(user_id);
CREATE INDEX idx_sessions_expires   ON sessions(expires_at) WHERE revoked_at IS NULL;

-- ─────────────────────────────────────────────────────────────
-- 5. Topics (categories)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE topics (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    description     TEXT,
    color           VARCHAR(7),                   -- hex color #RRGGBB
    icon            VARCHAR(50),
    parent_id       BIGINT       REFERENCES topics(id) ON DELETE SET NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_topics_slug         ON topics(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_topics_parent       ON topics(parent_id);
CREATE INDEX idx_topics_sort         ON topics(sort_order);

-- ─────────────────────────────────────────────────────────────
-- 6. Tags
-- ─────────────────────────────────────────────────────────────

CREATE TABLE tags (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL UNIQUE,
    slug            VARCHAR(60)  NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_tags_slug  ON tags(slug);
CREATE INDEX idx_tags_name_trgm ON tags USING gin (name gin_trgm_ops);

-- ─────────────────────────────────────────────────────────────
-- 7. Posts
-- ─────────────────────────────────────────────────────────────

CREATE TABLE posts (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    slug                VARCHAR(280) NOT NULL UNIQUE,
    excerpt             TEXT,
    content_markdown    TEXT         NOT NULL DEFAULT '',
    content_html        TEXT         NOT NULL DEFAULT '',
    cover_image_url     TEXT,
    og_image_url        TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'reviewing', 'published', 'archived')),
    visibility          VARCHAR(20)  NOT NULL DEFAULT 'public'
        CHECK (visibility IN ('public', 'unlisted', 'private')),
    author_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    topic_id            BIGINT       REFERENCES topics(id) ON DELETE SET NULL,
    reading_time_min    INTEGER      NOT NULL DEFAULT 0,
    view_count          BIGINT       NOT NULL DEFAULT 0,
    published_at        TIMESTAMPTZ,
    scheduled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    -- SEO
    meta_title          VARCHAR(255),
    meta_description    VARCHAR(500),
    canonical_url       TEXT
);

CREATE INDEX idx_posts_slug         ON posts(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_posts_status       ON posts(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_posts_published    ON posts(published_at DESC) WHERE status = 'published' AND deleted_at IS NULL;
CREATE INDEX idx_posts_author       ON posts(author_id);
CREATE INDEX idx_posts_topic        ON posts(topic_id);
CREATE INDEX idx_posts_scheduled    ON posts(scheduled_at) WHERE status = 'draft' AND scheduled_at IS NOT NULL;
CREATE INDEX idx_posts_title_trgm   ON posts USING gin (title gin_trgm_ops);

-- ─────────────────────────────────────────────────────────────
-- 8. Post ↔ Tags (many-to-many)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE post_tags (
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX idx_post_tags_tag  ON post_tags(tag_id);

-- ─────────────────────────────────────────────────────────────
-- 9. Projects (portfolio)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    slug            VARCHAR(280) NOT NULL UNIQUE,
    description     TEXT,
    content_markdown TEXT,
    cover_image_url TEXT,
    project_url     TEXT,
    repo_url        TEXT,
    tech_stack      TEXT[],                   -- Postgres array
    status          VARCHAR(20)  NOT NULL DEFAULT 'in_progress'
        CHECK (status IN ('planning', 'in_progress', 'completed', 'archived')),
    is_featured     BOOLEAN      NOT NULL DEFAULT false,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    started_at      DATE,
    completed_at    DATE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_projects_slug       ON projects(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_projects_featured   ON projects(is_featured, sort_order) WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────
-- 10. Media (uploaded files)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE media (
    id              BIGSERIAL PRIMARY KEY,
    filename        VARCHAR(255) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    width           INTEGER,
    height          INTEGER,
    storage_path    TEXT         NOT NULL,    -- relative path under /data/uploads
    public_url      TEXT         NOT NULL,    -- /uploads/yyyy/mm/uuid.ext
    alt_text        TEXT,
    caption         TEXT,
    uploaded_by     BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_media_uploaded_by   ON media(uploaded_by);
CREATE INDEX idx_media_mime          ON media(mime_type);

-- ─────────────────────────────────────────────────────────────
-- 11. Newsletter subscribers
-- ─────────────────────────────────────────────────────────────

CREATE TABLE newsletter_subscribers (
    id              BIGSERIAL PRIMARY KEY,
    email           CITEXT       NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'confirmed', 'unsubscribed', 'bounced')),
    confirm_token   VARCHAR(100),
    confirmed_at    TIMESTAMPTZ,
    unsubscribed_at TIMESTAMPTZ,
    ip_address      INET,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_newsletter_status   ON newsletter_subscribers(status);

-- ─────────────────────────────────────────────────────────────
-- 12. Settings (singleton key-value store)
-- ─────────────────────────────────────────────────────────────

CREATE TABLE settings (
    key             VARCHAR(100) PRIMARY KEY,
    value           TEXT         NOT NULL,
    value_type      VARCHAR(20)  NOT NULL DEFAULT 'string'
        CHECK (value_type IN ('string', 'number', 'boolean', 'json')),
    description     TEXT,
    is_public       BOOLEAN      NOT NULL DEFAULT false,  -- exposed via public API
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      BIGINT       REFERENCES users(id) ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────
-- 13. updated_at trigger function (shared)
-- ─────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
CREATE TRIGGER trg_roles_updated_at     BEFORE UPDATE ON roles     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_users_updated_at     BEFORE UPDATE ON users     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_topics_updated_at    BEFORE UPDATE ON topics    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_posts_updated_at     BEFORE UPDATE ON posts     FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_projects_updated_at  BEFORE UPDATE ON projects  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_settings_updated_at  BEFORE UPDATE ON settings  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ═══════════════════════════════════════════════════════════════
-- End V1
-- ═══════════════════════════════════════════════════════════════
