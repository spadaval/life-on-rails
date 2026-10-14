#!/usr/bin/env bash
# Nightly pg_dump (lor-backup.timer). Keeps the last 7.
# Restore: pg_restore --clean --if-exists -d life_on_rails <file>
set -euo pipefail
OUT_DIR="${HOME}/.local/share/life-on-rails/backup"
mkdir -p "$OUT_DIR"
OUT="${OUT_DIR}/lor-$(date +%Y%m%dT%H%M%S).dump"
pg_dump -Fc -h /var/run/postgresql life_on_rails > "$OUT"
ls -1t "$OUT_DIR"/lor-*.dump | tail -n +8 | xargs -r rm --
echo "backup ok: $OUT"
