#!/usr/bin/env bash
# nightly.sh: the agent's nightly run, as its own T3 thread, so it can be read
# and answered on the phone. The timer stays in systemd because T3's scheduler
# skips a fixed-time run it missed, and this one must catch up.
set -euo pipefail
cd "$HOME/google/assistant"
exec t3 thread start . --title "Nightly $(date +%F)" --wait - <<'PROMPT'
Do your nightly run as described on your `agent` page. You are unattended: nobody reads this thread, so anything for Sumeet goes where that page says.
PROMPT
