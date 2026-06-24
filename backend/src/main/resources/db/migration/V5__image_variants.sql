-- V5: Image variants — thumbnail + WebP support
ALTER TABLE media ADD COLUMN IF NOT EXISTS thumbnail_path TEXT;
ALTER TABLE media ADD COLUMN IF NOT EXISTS thumbnail_url TEXT;
ALTER TABLE media ADD COLUMN IF NOT EXISTS webp_path TEXT;
ALTER TABLE media ADD COLUMN IF NOT EXISTS webp_url TEXT;
