---
created_at: "2026-11-08T07:23:29.384013159+00:00"
id: "lor-hvlq"
evidence_type: "validation"
commit: "7e2badd30379443674159687ee135b7f1c3537ed"
result: "fail"
captured_at: "2026-11-08T07:23:29.383909406+00:00"
path: "/tmp/lor-verify/lor-wt-f3vn/e2e-fold-2/first-run-bug"
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
title: "End to end on T3_Fold_API35, build of 76cf4b6 (APK 13b6a9c1), test server :3857 at main 3e17179: every step to 10 held, but unfolded after a screen-off home showed [1, 0] (11-unfolded-after-screen-off.png) instead of start.wide [2, 0]. LorHome logged 'screen on' at sw 443 dp, 36 ms before the unfold's configuration change; 12-unfolded-sleep-wake-nofold.png shows the reset works when nothing folds while the screen is off. Fixed in 7e2badd."
updated_at: "2026-11-08T07:23:29.401360647+00:00"
---

End to end on T3_Fold_API35, build of 76cf4b6 (APK 13b6a9c1), test server :3857 at main 3e17179: every step to 10 held, but unfolded after a screen-off home showed [1, 0] (11-unfolded-after-screen-off.png) instead of start.wide [2, 0]. LorHome logged 'screen on' at sw 443 dp, 36 ms before the unfold's configuration change; 12-unfolded-sleep-wake-nofold.png shows the reset works when nothing folds while the screen is off. Fixed in 7e2badd.
