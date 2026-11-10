#!/usr/bin/env bash
# mail.sh: if LoR has mail waiting for the agent (and the burst has gone quiet),
# run the agent on it, then take it off the queue. Costs nothing when the queue
# is empty. The queue is cleared only after a run that finished and replied; a
# failed run, or one that never started, leaves the mail for the next tick.
set -euo pipefail
here=$(dirname "$(readlink -f "$0")")
pending=$(lor mail pending)
[ "$(jq -r .due <<<"$pending")" = true ] || exit 0
reply=$("$here/run-agent.sh" "$(jq -r .text <<<"$pending")

Read these in Gmail with the \`google\` skill and handle them as the Mail section of your \`agent\` page says.")
if [ -z "$reply" ]; then
  echo "the run gave no reply; leaving the mail queued" >&2
  exit 1
fi
echo "$reply"
LOR_THREAD=runner:mail lor mail done $(jq -r '.observation_ids[]' <<<"$pending") >/dev/null
