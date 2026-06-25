-- V10: Add status tracking + post link to content_registry
-- status: collected → published (after post created) | rejected (manually dismissed)
-- post_id: link to the post created from this collected item (NULL until published)

ALTER TABLE content_registry
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'collected'
    CHECK (status IN ('collected', 'published', 'rejected'));

ALTER TABLE content_registry
  ADD COLUMN IF NOT EXISTS post_id BIGINT REFERENCES posts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_registry_status ON content_registry(status);
