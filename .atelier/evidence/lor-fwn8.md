---
created_at: "2026-11-08T07:23:29.464778137+00:00"
id: "lor-fwn8"
evidence_type: "validation"
commit: "7e2badd30379443674159687ee135b7f1c3537ed"
result: "pass"
captured_at: "2026-11-08T07:23:29.464726902+00:00"
path: "/tmp/lor-verify/lor-wt-f3vn/e2e-fold-2"
agent_identity: "orchestrator-uj0r"
target:
  kind: "issue"
  id: "lor-f3vn"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-f3vn"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "End to end on T3_Fold_API35 in one session, build of 7e2badd's code (APK 9bfdf156), test server :3857 at main 3e17179, from the v1 layout.json with two extra pages (00-v1-layout.json, sha256 6323a6e7). Each NN-*.png has its NN-*.json, the file read after that step. Folded: 02 home menu, 03 minimap; 04 'Stack added at [4, 0].', with an empty apps block and a dock; 05-06 Clock, Move to…, Column 4: 'Clock moved to Column 4.', home [1, 0] is apps and dock, [4, 0] is apps, clock, dock; 07 Move onto [2, 0]: 'Swapped [4, 0] and [2, 0].'; 08 Full: wide full; 09 Inner start: start.wide [2, 0]. 10 Done, screen off, unfold, screen on: 11 shows [2, 0] alone at full width with the clock (logcat: configuration changed, then 'screen on: home goes to its start' at sw 852 dp). Unfolded: 12-13 the minimap's padlock: 'Home is locked.', locked true. 15 a long-press drag on the dock's Phone: nothing picked up, the bar says 'Home is locked. Unlock it from the home menu.', the app menu has no edit entries; layout.json's mtime is 02:21:51.932 before and after. 18 the home menu locked: Panels, Wallpaper, Home settings, Unlock home. 19 Unlock home: 'Home is unlocked.', no locked field. 20 Undo: the file's sha256 is back to 13's, 609ebddc. logs/ has logcat, with an empty crash buffer."
updated_at: "2026-11-08T07:23:29.481231046+00:00"
---

End to end on T3_Fold_API35 in one session, build of 7e2badd's code (APK 9bfdf156), test server :3857 at main 3e17179, from the v1 layout.json with two extra pages (00-v1-layout.json, sha256 6323a6e7). Each NN-*.png has its NN-*.json, the file read after that step. Folded: 02 home menu, 03 minimap; 04 'Stack added at [4, 0].', with an empty apps block and a dock; 05-06 Clock, Move to…, Column 4: 'Clock moved to Column 4.', home [1, 0] is apps and dock, [4, 0] is apps, clock, dock; 07 Move onto [2, 0]: 'Swapped [4, 0] and [2, 0].'; 08 Full: wide full; 09 Inner start: start.wide [2, 0]. 10 Done, screen off, unfold, screen on: 11 shows [2, 0] alone at full width with the clock (logcat: configuration changed, then 'screen on: home goes to its start' at sw 852 dp). Unfolded: 12-13 the minimap's padlock: 'Home is locked.', locked true. 15 a long-press drag on the dock's Phone: nothing picked up, the bar says 'Home is locked. Unlock it from the home menu.', the app menu has no edit entries; layout.json's mtime is 02:21:51.932 before and after. 18 the home menu locked: Panels, Wallpaper, Home settings, Unlock home. 19 Unlock home: 'Home is unlocked.', no locked field. 20 Undo: the file's sha256 is back to 13's, 609ebddc. logs/ has logcat, with an empty crash buffer.
