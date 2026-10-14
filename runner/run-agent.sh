#!/usr/bin/env bash
# run-agent.sh "<prompt>": one unattended agent run. Headless Claude Code in the
# assistant directory, with the same skills and `agent` page as the T3 threads.
# Not part of LoR: LoR never starts this; systemd timers do.
#
# The session id doubles as the run's LoR thread, so `claude -r <id>` in
# ~/google/assistant reopens a run that LoR credits with a write.
set -euo pipefail
here=$(dirname "$(readlink -f "$0")")
session=$(cat /proc/sys/kernel/random/uuid)
export LOR_THREAD="claude:$session"
export PATH="$here:$PATH"
cd "$HOME/google/assistant"
echo "run $LOR_THREAD"
exec claude -p "$1" --session-id "$session" --model claude-opus-5-5 --dangerously-skip-permissions \
  --append-system-prompt "You are running unattended, outside T3: nobody reads this output. LOR_THREAD is already set for your lor writes. If Sumeet needs to reply to or decide something, put it in front of him with \`lor intent create --type task --title \"...\" --flag \"<why it needs him>\"\` (or \`lor task flag <id> \"...\"\` on an existing task): it shows under Needs you on Today and his phone notifies him. Record anything else with \`lor log\`."
