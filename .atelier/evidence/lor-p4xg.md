---
created_at: "2026-11-08T03:12:22.213916579+00:00"
id: "lor-p4xg"
evidence_type: "validation"
commit: "00a613b46e44ce9fe1618f595cdbb4318561d3d0"
result: "pass"
captured_at: "2026-11-08T03:12:22.213858241+00:00"
path: "/tmp/claude-1000/9uc1-emu"
agent_identity: "lor-9uc1-author"
target:
  kind: "issue"
  id: "lor-9uc1"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-9uc1"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Each block's menu has Move up (disabled and dimmed on the top block), Move down, Move to… and Remove (07, 24 on the final APK). Move down on Clock writes apps clock dock (f07); Move up on Apps writes apps clock dock, bar 'Apps moved to the top.' (f08). Move to… lists 'Column 2: 3 apps · Dock' and 'Column 3: 1 app · Dock' (09); Column 2 puts the clock before page 2's dock: [2, 0] apps(3) clock dock (f10-moveto). Remove of home's Apps leaves [1, 0] clock dock, home shows no apps with 'Apps removed from Column 1; 10 apps left home.' (12, f11-remove). Each Undo gives back 6323a6e7… (f10-undone, f11-undone, 13). On a stack holding only Apps (page 3 after removing its dock, f14, sha afe8bbd7…), Move to… and Remove each show 'Can't: a stack needs a block; remove the panel instead.' (15, 16) and the file's sum stays afe8bbd7…."
updated_at: "2026-11-08T03:12:22.227224349+00:00"
---

Each block's menu has Move up (disabled and dimmed on the top block), Move down, Move to… and Remove (07, 24 on the final APK). Move down on Clock writes apps clock dock (f07); Move up on Apps writes apps clock dock, bar 'Apps moved to the top.' (f08). Move to… lists 'Column 2: 3 apps · Dock' and 'Column 3: 1 app · Dock' (09); Column 2 puts the clock before page 2's dock: [2, 0] apps(3) clock dock (f10-moveto). Remove of home's Apps leaves [1, 0] clock dock, home shows no apps with 'Apps removed from Column 1; 10 apps left home.' (12, f11-remove). Each Undo gives back 6323a6e7… (f10-undone, f11-undone, 13). On a stack holding only Apps (page 3 after removing its dock, f14, sha afe8bbd7…), Move to… and Remove each show 'Can't: a stack needs a block; remove the panel instead.' (15, 16) and the file's sum stays afe8bbd7….
