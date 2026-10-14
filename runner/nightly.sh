#!/usr/bin/env bash
# nightly.sh: the agent's nightly run.
set -euo pipefail
exec "$(dirname "$(readlink -f "$0")")/run-agent.sh" "Do your nightly run as described on your \`agent\` page."
