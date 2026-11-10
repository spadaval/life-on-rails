#!/usr/bin/env bash
# run-agent.sh "<prompt>": one unattended agent run that is not a T3 thread.
# Headless Claude Code in the assistant directory, with the same skills and
# `agent` page as the T3 threads. Mail uses it, because a thread for every
# burst of mail would be clutter. Not part of LoR: systemd timers start this.
#
# Prints the agent's reply. Exits non-zero unless the run finished and used at
# least one tool, so a caller can tell a run that did the work from one that
# never started.
#
# The session id doubles as the run's LoR thread, so `claude -r <id>` in
# ~/google/assistant reopens a run that LoR credits with a write.
set -euo pipefail
session=$(cat /proc/sys/kernel/random/uuid)
export LOR_THREAD="claude:$session"
cd "$HOME/google/assistant"
echo "run $LOR_THREAD" >&2
out=$(claude -p "$1" --session-id "$session" --model claude-opus-5-5 --dangerously-skip-permissions --output-format json \
  --append-system-prompt "You are running unattended, outside T3: nobody reads this output, and there is no T3CODE_THREAD_ID. LOR_THREAD is already set for your lor writes. Reach Sumeet the way your \`agent\` page says: \`lor message\` for what cannot wait, a flagged task when he has something to do, \`lor log\` for the rest." </dev/null)
if ! jq -e '.is_error == false and .num_turns > 1' <<<"$out" >/dev/null; then
  echo "run $LOR_THREAD did not finish its work:" >&2
  jq -c '{subtype, is_error, num_turns, result}' <<<"$out" >&2 || echo "$out" >&2
  exit 1
fi
jq -r .result <<<"$out"
