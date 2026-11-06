#!/usr/bin/env bash
# (Re)start a throwaway server on the TEST database for scripts/ui-check.mjs.
# Port 3857, no owner listener, no wall display listener, no jobs and no WhatsApp
# channel (a second poller on the real key gets 409 Duplicate request and steals
# the owner's messages). Logs to /tmp/lor-ui-server.log.
# Plaid is always Sandbox here, with its tokens in a file of its own.
set -euo pipefail
cd "$(dirname "$0")/.."
# Every earlier test server, listening or not: any one left running keeps its jobs.
for p in $(pgrep -f 'scripts/serve.mjs' || true); do
  grep -qzxF 'LOR_PORT=3857' "/proc/$p/environ" 2>/dev/null && kill "$p" || true
done
sleep 1
DATABASE_URL='postgres:///life_on_rails_test?host=/var/run/postgresql' LOR_PORT=3857 LOR_OWNER_LOGIN= \
  LOR_NO_JOBS=1 LOR_TRMNL_BIND= LOR_CHANNEL_KEY= LOR_CHANNEL_THREAD= PLAID_ENV=sandbox PLAID_ITEMS_FILE=/tmp/lor-ui-plaid-items.json \
  nohup "${HOME}/.local/opt/node/bin/node" scripts/serve.mjs > /tmp/lor-ui-server.log 2>&1 &
for _ in $(seq 1 40); do
  curl -sf -o /dev/null http://127.0.0.1:3857/health && { echo "ui server up on :3857"; exit 0; }
  sleep 0.25
done
echo "ui server did not start; see /tmp/lor-ui-server.log" >&2
exit 1
