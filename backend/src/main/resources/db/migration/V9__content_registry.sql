-- V9: content_registry table — managed by admin Scheduled Tasks page
-- Originally created manually by content-engine cron job; now formalizing under Flyway.

CREATE TABLE IF NOT EXISTS content_registry (
    id           BIGSERIAL PRIMARY KEY,
    slug         VARCHAR(300) NOT NULL UNIQUE,
    source       VARCHAR(100) NOT NULL,
    source_url   TEXT,
    topic        TEXT,
    pillar       VARCHAR(100),
    funnel       VARCHAR(20),
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_registry_pillar    ON content_registry(pillar);
CREATE INDEX IF NOT EXISTS idx_registry_published ON content_registry(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_registry_source    ON content_registry(source);
