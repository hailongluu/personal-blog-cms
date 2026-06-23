#!/bin/bash
# ============================================================
# restore-db.sh — Restore PostgreSQL database from backup
# Usage: ./restore-db.sh <backup-file.sql.gz>
# ============================================================
set -euo pipefail

CONTAINER="blog-postgres"
DB_USER="blog_user"
DB_NAME="blog_db"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ── Argument parsing ──────────────────────────────────────
if [ $# -lt 1 ]; then
    echo "Usage: $0 <backup-file.sql.gz>"
    echo ""
    echo "Available backups:"
    ls -1t /home/halo/vibe-code/personal-blog-cms/backups/db_*.sql.gz 2>/dev/null || echo "  (none found)"
    exit 1
fi

BACKUP_FILE="$1"

# ── Validate backup file ──────────────────────────────────
if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}ERROR: Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

if [ ! -s "$BACKUP_FILE" ]; then
    echo -e "${RED}ERROR: Backup file is empty: $BACKUP_FILE${NC}"
    exit 1
fi

echo "============================================"
echo "  DATABASE RESTORE"
echo "============================================"
echo ""
echo -e "  Backup file : ${YELLOW}$BACKUP_FILE${NC}"
echo -e "  Size        : $(du -h "$BACKUP_FILE" | cut -f1)"
echo -e "  Container   : $CONTAINER"
echo -e "  Database    : $DB_NAME"
echo ""
echo -e "${RED}WARNING: This will DROP and RECREATE the database!${NC}"
echo -e "${RED}All current data will be LOST.${NC}"
echo ""

# ── Confirmation ──────────────────────────────────────────
read -r -p "Type 'YES' (uppercase) to confirm restore: " CONFIRM
if [ "$CONFIRM" != "YES" ]; then
    echo "Restore cancelled."
    exit 0
fi

echo ""
echo "[$(date)] Starting restore..."

# ── Terminate existing connections ────────────────────────
echo "[$(date)] Terminating existing connections to $DB_NAME..."
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pg_stat_activity.pid)
     FROM pg_stat_activity
     WHERE pg_stat_activity.datname = '$DB_NAME'
       AND pid <> pg_backend_pid();" > /dev/null 2>&1 || true

# ── Drop and recreate database ────────────────────────────
echo "[$(date)] Dropping database $DB_NAME..."
docker exec "$CONTAINER" dropdb -U "$DB_USER" --if-exists "$DB_NAME"

echo "[$(date)] Creating database $DB_NAME..."
docker exec "$CONTAINER" createdb -U "$DB_USER" "$DB_NAME"

# ── Restore from backup ───────────────────────────────────
echo "[$(date)] Restoring from backup..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  RESTORE COMPLETE${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "[$(date)] Database $DB_NAME restored successfully from:"
echo "  $BACKUP_FILE"
echo ""
echo "Next steps:"
echo "  1. Run migrations if needed: cd backend && ./mvnw flyway:migrate"
echo "  2. Restart the API: docker compose restart blog-api"
echo "  3. Verify: curl http://localhost:8080/actuator/health"
