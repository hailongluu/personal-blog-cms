-- ═══════════════════════════════════════════════════════════════
-- V900__h2_test_schema.sql — H2-compatible test schema
-- Replaces Postgres-specific types (CITEXT, INET, uuid-ossp, gen_random_uuid, inet casts)
-- with portable H2 equivalents. Only used in tests.
-- ═══════════════════════════════════════════════════════════════

-- Drop any existing schema (from previous run in same test suite)
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS post_tags CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS media CASCADE;
DROP TABLE IF EXISTS newsletter_subscribers CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS topics CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS settings CASCADE;
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- ─────────────────────────────────────────────────────────────
-- Roles
-- ─────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description CLOB,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      CLOB,
    bio             CLOB,
    role_id         BIGINT       NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role_id);

-- ─────────────────────────────────────────────────────────────
-- Sessions
-- ─────────────────────────────────────────────────────────────
CREATE TABLE sessions (
    id              UUID PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    refresh_token   VARCHAR(500) NOT NULL UNIQUE,
    user_agent      CLOB,
    ip_address      VARCHAR(45),                   -- IPv4 or IPv6 string
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sessions_user ON sessions(user_id);

-- ─────────────────────────────────────────────────────────────
-- Topics
-- ─────────────────────────────────────────────────────────────
CREATE TABLE topics (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    description     CLOB,
    color           VARCHAR(7),
    icon            VARCHAR(50),
    parent_id       BIGINT,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- Tags
-- ─────────────────────────────────────────────────────────────
CREATE TABLE tags (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL UNIQUE,
    slug            VARCHAR(60)  NOT NULL UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- Posts
-- ─────────────────────────────────────────────────────────────
CREATE TABLE posts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    slug                VARCHAR(280) NOT NULL UNIQUE,
    excerpt             CLOB,
    content_markdown    CLOB         NOT NULL,
    content_html        CLOB         NOT NULL,
    cover_image_url     CLOB,
    og_image_url        CLOB,
    status              VARCHAR(20)  NOT NULL DEFAULT 'draft',
    visibility          VARCHAR(20)  NOT NULL DEFAULT 'public',
    author_id           BIGINT       NOT NULL,
    topic_id            BIGINT,
    reading_time_min    INTEGER      NOT NULL DEFAULT 0,
    view_count          BIGINT       NOT NULL DEFAULT 0,
    published_at        TIMESTAMP,
    scheduled_at        TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    meta_title          VARCHAR(255),
    meta_description    VARCHAR(500),
    canonical_url       CLOB,
    FOREIGN KEY (author_id) REFERENCES users(id),
    FOREIGN KEY (topic_id) REFERENCES topics(id)
);

CREATE TABLE post_tags (
    post_id     BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────
-- Projects
-- ─────────────────────────────────────────────────────────────
CREATE TABLE projects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    slug            VARCHAR(280) NOT NULL UNIQUE,
    description     CLOB,
    content_markdown CLOB,
    cover_image_url CLOB,
    project_url     CLOB,
    repo_url        CLOB,
    tech_stack      VARCHAR(1000),
    status          VARCHAR(20)  NOT NULL DEFAULT 'in_progress',
    is_featured     BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    started_at      DATE,
    completed_at    DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- Media
-- ─────────────────────────────────────────────────────────────
CREATE TABLE media (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename        VARCHAR(255) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    width           INTEGER,
    height          INTEGER,
    storage_path    CLOB         NOT NULL,
    public_url      CLOB         NOT NULL,
    alt_text        CLOB,
    caption         CLOB,
    uploaded_by     BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- ─────────────────────────────────────────────────────────────
-- Newsletter subscribers
-- ─────────────────────────────────────────────────────────────
CREATE TABLE newsletter_subscribers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    confirm_token   VARCHAR(100),
    confirmed_at    TIMESTAMP,
    unsubscribed_at TIMESTAMP,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- Settings
-- ─────────────────────────────────────────────────────────────
CREATE TABLE settings (
    key             VARCHAR(100) PRIMARY KEY,
    value           CLOB         NOT NULL,
    value_type      VARCHAR(20)  NOT NULL DEFAULT 'string',
    description     CLOB,
    is_public       BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT
);

-- ─────────────────────────────────────────────────────────────
-- Seed: roles + admin user + settings + default topic
-- (Password hash for 'admin123' generated with BCrypt cost 10)
-- ═══════════════════════════════════════════════════════════════

INSERT INTO roles (name, description) VALUES
    ('admin',  'Full access'),
    ('editor', 'Edit/publish any post'),
    ('author', 'Create/edit own posts');

INSERT INTO topics (name, slug, description, color, sort_order) VALUES
    ('General', 'general', 'Default topic', '#6B7280', 0);

INSERT INTO users (email, password_hash, display_name, role_id, is_active)
SELECT 'admin@example.com',
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Admin',
       r.id,
       TRUE
FROM roles r WHERE r.name = 'admin';

INSERT INTO settings (key, value, value_type, description, is_public) VALUES
    ('site.title', 'Personal Blog', 'string', 'Site title', TRUE),
    ('site.url', 'http://localhost', 'string', 'Site URL', TRUE);

-- ═══════════════════════════════════════════════════════════════
-- End V900
-- ═══════════════════════════════════════════════════════════════
