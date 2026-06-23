#!/bin/bash
# ============================================================
# backup-media.sh — Media/uploads backup via tar+gzip
# ============================================================
set -euo pipefail

BACKUP_DIR="/home/halo/vibe-code/personal-blog-cms/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
UPLOADS_PATH="/data/uploads"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting media backup..."

# Check if uploads directory exists and has content
if [ -d "$UPLOADS_PATH" ] && [ "$(ls -A "$UPLOADS_PATH" 2>/dev/null)" ]; then
    tar -czf "$BACKUP_DIR/media_${TIMESTAMP}.tar.gz" -C /data uploads
    echo "[$(date)] Media backup complete: media_${TIMESTAMP}.tar.gz ($(du -h "$BACKUP_DIR/media_${TIMESTAMP}.tar.gz" | cut -f1))"
else
    echo "[$(date)] No media to backup (directory empty or missing)"
fi

# Keep last 7 days, delete older
find "$BACKUP_DIR" -name "media_*.tar.gz" -mtime +7 -delete 2>/dev/null || true

echo "[$(date)] Cleanup done. Current media backups:"
ls -lh "$BACKUP_DIR"/media_*.tar.gz 2>/dev/null || echo "  (none)"
