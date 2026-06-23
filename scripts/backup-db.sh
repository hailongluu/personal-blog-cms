#!/bin/bash
# ============================================================
# backup-db.sh — PostgreSQL database backup via pg_dump
# ============================================================
set -euo pipefail

BACKUP_DIR="/home/halo/vibe-code/personal-blog-cms/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
CONTAINER="blog-postgres"
DB_USER="blog_user"
DB_NAME="blog_db"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting database backup..."

# Run pg_dump inside the container, pipe through gzip
docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_DIR/db_${TIMESTAMP}.sql.gz"

# Verify the backup file is non-empty
if [ -s "$BACKUP_DIR/db_${TIMESTAMP}.sql.gz" ]; then
    echo "[$(date)] Backup complete: db_${TIMESTAMP}.sql.gz ($(du -h "$BACKUP_DIR/db_${TIMESTAMP}.sql.gz" | cut -f1))"
else
    echo "[$(date)] ERROR: Backup file is empty!" >&2
    exit 1
fi

# Keep last 7 days, delete older
find "$BACKUP_DIR" -name "db_*.sql.gz" -mtime +7 -delete 2>/dev/null || true

echo "[$(date)] Cleanup done. Current backups:"
ls -lh "$BACKUP_DIR"/db_*.sql.gz 2>/dev/null || echo "  (none)"
