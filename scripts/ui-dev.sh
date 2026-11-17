#!/usr/bin/env bash
# Vite's dev server with hot reload, on the TEST database, for designing the
# dashboard: save a route or src/styles.css and the open page shows it.
#   scripts/ui-dev.sh            # 127.0.0.1:3858, in the foreground (Ctrl-C stops it)
#   LOR_FAKE_NOW=2026-11-17T15:00 scripts/ui-dev.sh
# Load the demo set first (pnpm demo:test); links take at= and data= (seeds/demo/README.md).
#
# Like scripts/ui-server.sh it runs no jobs, no WhatsApp channel and no wall listener:
# under `vite dev` src/server/boot.ts starts nothing (LOR_RUN_JOBS is unset), and no
# owner or TRMNL listener exists outside scripts/serve.mjs. A design tool, not a check.
set -euo pipefail
cd "$(dirname "$0")/.."

TEST_DB=life_on_rails_test
DB_URL="${LOR_UI_DEV_DATABASE_URL:-postgres:///${TEST_DB}?host=/var/run/postgresql}"
PORT="${LOR_UI_DEV_PORT:-3858}"
ENV_FILE="${LOR_ENV_FILE:-$HOME/.config/life-on-rails/env}"

refuse() { echo "ui-dev: refused: $*" >&2; exit 3; }

# The env file's value for a key, or empty.
envval() { [[ -f "$ENV_FILE" ]] && sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*['\"]\{0,1\}\([^'\"]*\)['\"]\{0,1\}[[:space:]]*$/\1/p" "$ENV_FILE" | tail -1 || true; }

# The database: the test database by name, with nothing after the path but ?host=.
db_name="${DB_URL#postgres://}"; db_name="${db_name#*/}"; db_query=""
[[ "$db_name" == *\?* ]] && db_query="${db_name#*\?}"
db_name="${db_name%%\?*}"
[[ "$db_name" == "$TEST_DB" ]] || refuse "$DB_URL is not the test database $TEST_DB. Leave LOR_UI_DEV_DATABASE_URL unset."
[[ -z "$db_query" || "$db_query" =~ ^host=[^\&]*$ ]] || refuse "$DB_URL sets more than ?host=, which can name another database."
real_db="$(envval DATABASE_URL)"
[[ -n "$real_db" && "$real_db" == "$DB_URL" ]] && refuse "$DB_URL is the real database (DATABASE_URL in $ENV_FILE)."

# The port: never lor-web's, and never one already taken.
[[ "$PORT" =~ ^[0-9]+$ ]] || refuse "LOR_UI_DEV_PORT wants a number, got $PORT."
for p in 3847 3848 3849 "$(envval LOR_PORT)" "$(envval LOR_SERVE_PORT)" "$(envval LOR_TRMNL_PORT)"; do
  [[ -n "$p" && "$PORT" == "$p" ]] && refuse "port $PORT is the running lor-web's. Use 3858."
done
if ss -Hltn "sport = :$PORT" | grep -q .; then
  refuse "something already listens on 127.0.0.1:$PORT: $(ss -Hltnp "sport = :$PORT" | head -1)"
fi

[[ -e node_modules ]] || { echo "ui-dev: no node_modules here: pnpm install, or in a worktree ln -s ~/projects/life-on-rails/node_modules node_modules" >&2; exit 1; }

# Vite's dependency cache per checkout, so worktrees sharing a node_modules symlink do not share it.
export LOR_VITE_CACHE_DIR="${LOR_VITE_CACHE_DIR:-/tmp/lor-ui-dev/$(basename "$PWD")/vite}"

unset LOR_RUN_JOBS
export DATABASE_URL="$DB_URL" LOR_NO_JOBS=1 LOR_OWNER_LOGIN= LOR_TRMNL_BIND= LOR_CHANNEL_KEY= LOR_CHANNEL_THREAD= \
  PLAID_ENV=sandbox PLAID_ITEMS_FILE=/tmp/lor-ui-plaid-items.json
echo "ui-dev: http://127.0.0.1:$PORT on $TEST_DB (hot reload; Ctrl-C stops it)"
exec node_modules/.bin/vite dev --host 127.0.0.1 --port "$PORT" --strictPort
