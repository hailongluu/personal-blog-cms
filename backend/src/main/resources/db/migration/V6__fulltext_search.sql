-- V6: Full-text search with tsvector + GIN index
-- Uses 'simple' config for multi-language support (Vietnamese + English)
-- Trigger-based approach: auto-updates search_vector on INSERT/UPDATE

ALTER TABLE posts ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Populate existing rows
UPDATE posts SET search_vector =
  setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
  setweight(to_tsvector('simple', coalesce(excerpt, '')), 'B') ||
  setweight(to_tsvector('simple', coalesce(content_markdown, '')), 'C');

-- Trigger function: auto-update search_vector
CREATE OR REPLACE FUNCTION posts_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(NEW.excerpt, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(NEW.content_markdown, '')), 'C');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger
DROP TRIGGER IF EXISTS trg_posts_search_vector ON posts;
CREATE TRIGGER trg_posts_search_vector
  BEFORE INSERT OR UPDATE OF title, excerpt, content_markdown ON posts
  FOR EACH ROW EXECUTE FUNCTION posts_search_vector_update();

-- GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS idx_posts_search_vector ON posts USING GIN (search_vector);
