#!/usr/bin/env bash
# (Re)start a throwaway server on the TEST database for scripts/ui-check.mjs.
# Port 3857, no owner listener. Logs to /tmp/lor-ui-server.log.
set -euo pipefail
cd "$(dirname "$0")/.."
PID=$(ss -ltnpH "sport = :3857" 2>/dev/null | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2 || true)
[ -n "${PID:-}" ] && kill "$PID" && sleep 1
DATABASE_URL='postgres:///life_on_rails_test?host=/var/run/postgresql' LOR_PORT=3857 LOR_OWNER_LOGIN= \
  nohup "${HOME}/.local/opt/node/bin/node" scripts/serve.mjs > /tmp/lor-ui-server.log 2>&1 &
for _ in $(seq 1 40); do
  curl -sf -o /dev/null http://127.0.0.1:3857/health && { echo "ui server up on :3857"; exit 0; }
  sleep 0.25
done
echo "ui server did not start; see /tmp/lor-ui-server.log" >&2
exit 1
