---
created_at: "2026-11-08T07:28:20.035455439+00:00"
id: "lor-op8w"
evidence_type: "validation"
commit: "45564e6aeebe57c7f1fc2653516d1d0556b5f008"
result: "pass"
captured_at: "2026-11-08T07:28:20.035406337+00:00"
path: "/tmp/lor-verify/lor-wt-f3vn/r1-dock"
agent_identity: "orchestrator-uj0r"
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
title: "Finding lor-1hbc, the pinned dock on screen. T3_Phone_API35, APK 9bfdf156 (76cf4b6 plus 7e2badd, which touches only HomeActivity and HomeScreen), from the v1 layout.json (sha256 6323a6e7). 01: home's Blocks lists Clock and Apps with handles and Dock with none, under the hint 'Drag a handle to reorder. Tap a block for its menu. The dock stays at the bottom.' 02: the dock's menu has Move up and Move down dimmed and [disabled] in the accessibility tree. 04: a tap on its dimmed Move up writes nothing (mtime unchanged). 05-07: a drag of Apps' handle down past the dock logs 'block 1 dragged to place 1' and writes nothing (still 6323a6e7). 08-09: a drag of Clock's handle onto the dock row logs 'block 0 dragged to place 1', the bar says 'Clock moved under Apps.', and the file reads apps, clock, dock. 11: Apps on top has Move up disabled and Move down enabled. 12: Clock, now above the dock, has Move down disabled and Move up enabled. logs/ has logcat, with an empty crash buffer."
updated_at: "2026-11-08T07:28:20.054052790+00:00"
---

Finding lor-1hbc, the pinned dock on screen. T3_Phone_API35, APK 9bfdf156 (76cf4b6 plus 7e2badd, which touches only HomeActivity and HomeScreen), from the v1 layout.json (sha256 6323a6e7). 01: home's Blocks lists Clock and Apps with handles and Dock with none, under the hint 'Drag a handle to reorder. Tap a block for its menu. The dock stays at the bottom.' 02: the dock's menu has Move up and Move down dimmed and [disabled] in the accessibility tree. 04: a tap on its dimmed Move up writes nothing (mtime unchanged). 05-07: a drag of Apps' handle down past the dock logs 'block 1 dragged to place 1' and writes nothing (still 6323a6e7). 08-09: a drag of Clock's handle onto the dock row logs 'block 0 dragged to place 1', the bar says 'Clock moved under Apps.', and the file reads apps, clock, dock. 11: Apps on top has Move up disabled and Move down enabled. 12: Clock, now above the dock, has Move down disabled and Move up enabled. logs/ has logcat, with an empty crash buffer.
