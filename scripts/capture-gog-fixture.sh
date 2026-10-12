#!/usr/bin/env bash
# Refresh golden gog fixtures by hand on buildbot (needs GOG_KEYRING_* env).
# Scrubs titles/attendees: replaces summaries with fixtures, drops emails.
set -euo pipefail
OUT="tests/google/fixtures"
GOG_BIN="${GOG_BIN:-/home/supadava/.local/bin/gog}"
ACCT="${GOG_ACCOUNT:-sumeet.padavala@gmail.com}"
FROM="$(date -u -d '1 day ago' +%Y-%m-%dT00:00:00Z)"
TO="$(date -u -d '3 days' +%Y-%m-%dT00:00:00Z)"
"$GOG_BIN" --json --no-input --readonly --results-only --enable-commands=calendar \
  -a "$ACCT" calendar events --all --from "$FROM" --to "$TO" --all-pages > /tmp/lor-events-raw.json
python3 - "$OUT/events.json" <<'EOF'
import json, sys
out = sys.argv[1]
rows = json.load(open('/tmp/lor-events-raw.json'))
scrubbed = []
for i, e in enumerate(rows if isinstance(rows, list) else []):
    scrubbed.append({
        "id": e.get("id", f"event-{i}"),
        "calendarId": e.get("calendarId", "primary"),
        "summary": f"Fixture event {i}",
        "status": e.get("status", "confirmed"),
        "start": e.get("start", {}),
        "end": e.get("end", {}),
    })
json.dump(scrubbed, open(out, "w"), indent=2)
print(f"wrote {out} ({len(scrubbed)} events, titles scrubbed)")
EOF
