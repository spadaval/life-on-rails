---
created_at: "2026-11-07T22:59:26.527112048+00:00"
id: "lor-deqx"
evidence_type: "validation"
commit: "db26b1d9f2b84b95f44c0c2b54bb2006d3974336"
result: "pass"
captured_at: "2026-11-07T22:59:26.527031969+00:00"
path: "/tmp/claude-1000/gnk0-emu"
agent_identity: "lor-gnk0-author"
target:
  kind: "issue"
  id: "lor-gnk0"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-gnk0"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "PRESSED STATE on T3_Phone_API35 (emulator-5554), db26b1d's APK (boat, assembleDebug). Width of Settings' blue disc on the row through its centre, per recorded frame (bin/widths.py over screenrecord, input motionevent DOWN, hold, CANCEL): scale 1 (31-press-anim.mp4): 137 px at rest, 135 134 130 126 then 122 over about 160 ms, held at 122, back 125 129 133 134 135 137 over about 150 ms after the cancel. Animator duration scale 5 (32-press-scale5.mp4): the same shrink stretched over about 500 ms. Scale 0 (33-press-scale0.mp4): 137 then 122 in the next frame, 122 held, then 137 in one frame: no in-between frame, nothing left part-way. Same 0.9 on the dock (Chrome 137 to 123 px, 35-dock-pressed.png), the drawer (Maps 137 to 125 px mid-press, 37-drawer-*.png) and search's apps (Safety 137 to 125 px, 39-search-pressed.png); the drawer's and search's show later as Compose delays a press in a scrolling list. A tap on Settings launched it (topResumedActivity Settings) and its window grew from the icon (34-tap-launch-tiles.png). Long press still opened the folder's menu (47-folder-menu.png) and drags still picked up (the drag receipts)."
updated_at: "2026-11-07T22:59:26.533070796+00:00"
---

PRESSED STATE on T3_Phone_API35 (emulator-5554), db26b1d's APK (boat, assembleDebug). Width of Settings' blue disc on the row through its centre, per recorded frame (bin/widths.py over screenrecord, input motionevent DOWN, hold, CANCEL): scale 1 (31-press-anim.mp4): 137 px at rest, 135 134 130 126 then 122 over about 160 ms, held at 122, back 125 129 133 134 135 137 over about 150 ms after the cancel. Animator duration scale 5 (32-press-scale5.mp4): the same shrink stretched over about 500 ms. Scale 0 (33-press-scale0.mp4): 137 then 122 in the next frame, 122 held, then 137 in one frame: no in-between frame, nothing left part-way. Same 0.9 on the dock (Chrome 137 to 123 px, 35-dock-pressed.png), the drawer (Maps 137 to 125 px mid-press, 37-drawer-*.png) and search's apps (Safety 137 to 125 px, 39-search-pressed.png); the drawer's and search's show later as Compose delays a press in a scrolling list. A tap on Settings launched it (topResumedActivity Settings) and its window grew from the icon (34-tap-launch-tiles.png). Long press still opened the folder's menu (47-folder-menu.png) and drags still picked up (the drag receipts).
