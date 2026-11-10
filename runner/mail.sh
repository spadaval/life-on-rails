#!/usr/bin/env bash
# mail.sh: if LoR has mail waiting for the agent (and the burst has gone quiet),
# run the agent on it, then take off the queue the messages the agent says it
# handled. Costs nothing when the queue is empty. A failed run, one that never
# started, or a message the agent does not name stays queued for the next tick.
set -euo pipefail
here=$(dirname "$(readlink -f "$0")")
pending=$(lor mail pending)
[ "$(jq -r .due <<<"$pending")" = true ] || exit 0
reply=$("$here/run-agent.sh" "$(jq -r .text <<<"$pending")

Read these in Gmail with the \`google\` skill and handle them as the Mail section of your \`agent\` page says.

End your reply with one line, \`handled: <gmail id> <gmail id> ...\`, naming only the messages you read and dealt with (deciding a message needs nothing counts). Leave out any you could not read: they stay queued for the next run.")
echo "$reply"
# The last `handled:` line names gmail ids; only those that were in this batch count.
handled=$(grep -i '^handled:' <<<"$reply" | tail -n 1 | cut -d: -f2- | tr -cs '[:alnum:]' '\n' || true)
done_ids=$(jq -r --arg handled "$handled" '
  ($handled | split("\n")) as $h
  | [.messages, .observation_ids] | transpose[]
  | select(.[0].gmail_id as $g | $h | index($g)) | .[1]' <<<"$pending")
if [ -z "$done_ids" ]; then
  echo "the run named no handled message; leaving the mail queued" >&2
  exit 1
fi
LOR_THREAD=runner:mail lor mail done $done_ids >/dev/null
