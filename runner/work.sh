#!/usr/bin/env bash
# work.sh: start a T3 thread on the oldest task queued for the agent. One per
# tick, so a burst of tasks does not become a burst of agents.
set -euo pipefail
next=$(lor work next)
id=$(jq -r '.task.id // empty' <<<"$next")
[ -n "$id" ] || exit 0
title=$(jq -r .task.title <<<"$next")
cd "$HOME/google/assistant"
thread=$(t3 thread start . --title "Work: $title" --json - <<PROMPT | jq -r .threadId
Do this queued task on your own: run \`lor context --task $id --md\` and follow your \`agent\` page. You are unattended: nobody reads this thread. Finish with \`lor done $id --memo "<what happened>"\` and tell Sumeet with \`lor message\` if the outcome is one he would want to know now, otherwise it goes in the brief. If you need him, flag the task with why.
PROMPT
)
LOR_THREAD=$thread lor work claim "$id" >/dev/null
echo "started $id in $thread"
