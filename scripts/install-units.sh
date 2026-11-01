#!/usr/bin/env bash
# Link and start the user units. Linger is already on for this user, so they
# start at boot without a login.
set -euo pipefail
cd "$(dirname "$0")/systemd"
systemctl --user link "$PWD/lor-web.service" "$PWD/lor-backup.service" "$PWD/lor-backup.timer" \
  "$PWD/assistant-mail.service" "$PWD/assistant-mail.timer" "$PWD/assistant-nightly.service" "$PWD/assistant-nightly.timer" \
  "$PWD/assistant-brief.service" "$PWD/assistant-brief.timer" \
  "$PWD/assistant-work.service" "$PWD/assistant-work.timer"
systemctl --user daemon-reload
systemctl --user enable --now lor-web.service lor-backup.timer assistant-mail.timer assistant-nightly.timer assistant-brief.timer assistant-work.timer
systemctl --user --no-pager status lor-web.service | head -5
