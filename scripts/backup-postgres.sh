#!/usr/bin/env bash
# Nightly pg_dump (systemd timer). Keeps 7 days.
set -euo pipefail
OUT_DIR="${HOME}/.local/share/life-on-rails/backup"
mkdir -p "$OUT_DIR"
STAMP="$(date -Iseconds)"
pg_dump -Fc life_on_rails > "${OUT_DIR}/lor-${STAMP}.dump"
find "$OUT_DIR" -name 'lor-*.dump' -mtime +7 -delete
echo "backup ok: lor-${STAMP}.dump"
