---
created_at: "2026-11-08T07:38:13.019606545+00:00"
id: "lor-ex2u"
evidence_type: "validation"
commit: "7c190c2d25bb54037619908ec2a77d9c0143441c"
result: "pass"
captured_at: "2026-11-08T07:38:13.019556742+00:00"
path: "/tmp/lor-verify/lor-wt-f3vn/e2e-fold-3"
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
title: "End to end on T3_Fold_API35 in one session, APK 07aac4f3 (the code of 7c190c2), test server :3857 at main 3e17179, from the v1 layout.json (00, sha256 6323a6e7). Each NN-*.png has its NN-*.json, the file read after it, and logcat-lor.txt and logcat-events.txt cover the whole run from step 01 (logcat cleared once, before it). Folded: 02 home menu, 03 minimap; 04 'Stack added at [4, 0].'; 05-06 the clock moved to it: 'Clock moved to Column 4.', [1, 0] is apps and dock, [4, 0] is apps, clock, dock; 07 'Swapped [4, 0] and [2, 0].'; 08 Full; 09 'The inner screen starts at [2, 0].', start.wide [2, 0]. 10 Done, screen off, unfold, screen on: 11 shows [2, 0] alone at full width with the clock. logcat-lor.txt at 02:35:47.817 has 'configuration changed' (sw 852 dp), then at .818 'screen on: home goes to its start' at sw 852 dp. 14 the padlock: 'Home is locked.' 15 a long-press drag on the dock's Phone picks nothing up, the bar says 'Home is locked. Unlock it from the home menu.' and layout.json's mtime is 02:36:10.808 before and after. 17 the home menu locked. 18 Unlock home: 'Home is unlocked.' 19 Undo: sha256 back to 14's, 609ebddc. No fold: 20 Show on [3, 0] moved home there (logcat-nofold.txt: 'at [3, 0] from [2, 0]'), and after a screen-off and on 21 is back on [2, 0], the inner start. logcat-crash.txt is empty."
updated_at: "2026-11-08T07:38:13.036148605+00:00"
---

End to end on T3_Fold_API35 in one session, APK 07aac4f3 (the code of 7c190c2), test server :3857 at main 3e17179, from the v1 layout.json (00, sha256 6323a6e7). Each NN-*.png has its NN-*.json, the file read after it, and logcat-lor.txt and logcat-events.txt cover the whole run from step 01 (logcat cleared once, before it). Folded: 02 home menu, 03 minimap; 04 'Stack added at [4, 0].'; 05-06 the clock moved to it: 'Clock moved to Column 4.', [1, 0] is apps and dock, [4, 0] is apps, clock, dock; 07 'Swapped [4, 0] and [2, 0].'; 08 Full; 09 'The inner screen starts at [2, 0].', start.wide [2, 0]. 10 Done, screen off, unfold, screen on: 11 shows [2, 0] alone at full width with the clock. logcat-lor.txt at 02:35:47.817 has 'configuration changed' (sw 852 dp), then at .818 'screen on: home goes to its start' at sw 852 dp. 14 the padlock: 'Home is locked.' 15 a long-press drag on the dock's Phone picks nothing up, the bar says 'Home is locked. Unlock it from the home menu.' and layout.json's mtime is 02:36:10.808 before and after. 17 the home menu locked. 18 Unlock home: 'Home is unlocked.' 19 Undo: sha256 back to 14's, 609ebddc. No fold: 20 Show on [3, 0] moved home there (logcat-nofold.txt: 'at [3, 0] from [2, 0]'), and after a screen-off and on 21 is back on [2, 0], the inner start. logcat-crash.txt is empty.
