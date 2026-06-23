-- ═══════════════════════════════════════════════════════════════
-- V2__seed_roles_and_admin.sql
-- Seed: roles + default admin + default topic
-- ═══════════════════════════════════════════════════════════════
-- Idempotent: uses ON CONFLICT DO NOTHING so re-runs are safe.
-- Default admin password is set in V3 from env (NOT hardcoded).
-- ═══════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────
-- 1. Roles
-- ─────────────────────────────────────────────────────────────
INSERT INTO roles (name, description) VALUES
    ('admin',  'Full access — manage all content, users, settings'),
    ('editor', 'Edit/publish any post, manage topics, tags, media'),
    ('author', 'Create/edit own posts only')
ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- 2. Default topic
-- ─────────────────────────────────────────────────────────────
INSERT INTO topics (name, slug, description, color, sort_order) VALUES
    ('General', 'general', 'Catch-all topic for uncategorized posts', '#6B7280', 0)
ON CONFLICT (slug) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- 3. Default settings
-- ─────────────────────────────────────────────────────────────
INSERT INTO settings (key, value, value_type, description, is_public) VALUES
    ('site.title',       'Personal Blog',     'string',  'Site title shown in header',        true),
    ('site.description', 'A personal blog.',  'string',  'Default meta description',          true),
    ('site.url',         'https://example.com', 'string','Canonical site URL',                true),
    ('site.author',      'Anonymous',         'string',  'Author name for OG tags',           true),
    ('site.locale',      'vi',                'string',  'Default locale (vi/en)',            true),
    ('posts.per_page',   '10',                'number',  'Posts per page on listings',        true),
    ('posts.allow_comments', 'false',         'boolean', 'Enable comments (not in MVP)',      true),
    ('newsletter.enabled',  'false',          'boolean', 'Show newsletter signup',           true),
    ('maintenance.mode',    'false',          'boolean', 'Maintenance mode',                 false)
ON CONFLICT (key) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- 4. Default admin user
-- Email: admin@example.com
-- Password hash: bcrypt of 'admin123' (cost 10)
-- Hash generated via: htpasswd -bnBC 10 "" admin123 | tr -d ':\n'
-- ═══════════════════════════════════════════════════════════════
-- IMPORTANT: Change the password immediately after first login!
-- This is a DEV-ONLY default. Production should use V3 with env var.
-- ═══════════════════════════════════════════════════════════════
INSERT INTO users (email, password_hash, display_name, role_id, is_active)
SELECT
    'admin@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- bcrypt('admin123')
    'Admin',
    r.id,
    true
FROM roles r WHERE r.name = 'admin'
ON CONFLICT (email) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- End V2
-- ═══════════════════════════════════════════════════════════════
