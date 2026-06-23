-- ═══════════════════════════════════════════════════════════════
-- V3__post_types_and_publishing.sql
-- Personal Blog CMS — Post types, subtitle, featured, publish timestamps
-- Adds fields per SPEC §8.3.3 + §8.3.4
-- ═══════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────
-- 1. Post type — enum check (ESSAY | RESEARCH_BRIEF | FIELD_NOTE |
--    BUILD_LOG | PLAYBOOK | REVIEW | PERSONAL_LOG)
-- ─────────────────────────────────────────────────────────────

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS type VARCHAR(30) NOT NULL DEFAULT 'ESSAY'
        CHECK (type IN ('ESSAY', 'RESEARCH_BRIEF', 'FIELD_NOTE', 'BUILD_LOG',
                        'PLAYBOOK', 'REVIEW', 'PERSONAL_LOG'));

-- ─────────────────────────────────────────────────────────────
-- 2. Subtitle — optional tagline shown under title
-- ─────────────────────────────────────────────────────────────

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS subtitle VARCHAR(1000);

-- ─────────────────────────────────────────────────────────────
-- 3. Featured — flag for homepage / topic-page highlight
-- ─────────────────────────────────────────────────────────────

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_posts_featured
    ON posts(featured) WHERE featured = TRUE AND deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────
-- 4. First / last publish timestamps
--    - first_published_at set on first transition to PUBLISHED
--    - last_published_at updated on every publish
-- ─────────────────────────────────────────────────────────────

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS first_published_at TIMESTAMPTZ;

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS last_published_at TIMESTAMPTZ;

-- Backfill: derive first_published_at from existing published_at
UPDATE posts
   SET first_published_at = published_at,
       last_published_at  = published_at
 WHERE status = 'published'
   AND published_at IS NOT NULL
   AND first_published_at IS NULL;

-- ─────────────────────────────────────────────────────────────
-- 5. Helpful indexes for filtering
-- ─────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_posts_type     ON posts(type)     WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_posts_status   ON posts(status)   WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_posts_author   ON posts(author_id) WHERE deleted_at IS NULL;
