#!/usr/bin/env bash
# brief.sh: the morning brief, as its own T3 thread. A systemd timer and not a
# T3 schedule: T3 prepares a worktree for a scheduled run, which fails because
# the assistant directory is not a git repo, and it skips a run it missed.
set -euo pipefail
cd "$HOME/google/assistant"
exec t3 thread start . --title "Brief $(date +%F)" --wait - <<'PROMPT'
Give the morning brief as described on your `agent` page. You are unattended: nobody reads this thread. Write `briefs/YYYY-MM-DD` first, then send the same text with `lor message`. If `lor message` refuses, the page still stands: `lor log` the refusal and finish.
PROMPT
