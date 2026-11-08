---
created_at: "2026-11-08T00:00:35.343607415+00:00"
id: "lor-b6x9"
evidence_type: "validation"
commit: "677524ca9cf4dfc664b58fd9d4c3659011c90f4c"
result: "pass"
captured_at: "2026-11-08T00:00:35.343522787+00:00"
path: "/tmp/claude-1000/ozj2-emu"
agent_identity: "lor-ozj2-author"
target:
  kind: "issue"
  id: "lor-ozj2"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-ozj2"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Screen-off goes to the start of the screen it comes back on (APK of 677524c, layout-v1-folder-page3.json: a Media folder on page 3). Unfolded on pages 3 and 4 with Media open (s2-folder-open), input keyevent SLEEP, a fold, input keyevent WAKEUP: log 'configuration changed ... sw 443 dp', 'folded: at [3, 0]', then 'screen on: home goes to its start (sw 443 dp ...)'; screen s3-woke-folded is the cover screen's home (narrow start) with no folder. Folded on page 3, SLEEP (mWakefulness=Asleep), an unfold, WAKEUP: 'unfolded: at [3, 0] on [[3, 0], [4, 0]]' then 'screen on: home goes to its start (sw 852 dp ...)'; s5-woke-unfolded is home and page 2 side by side (the file is version 1, so the wide start is home's). On this emulator the posture change itself turns the screen on (SCREEN_ON 40-100 ms after the configuration line), so the configuration always arrived first here."
updated_at: "2026-11-08T00:00:35.351300209+00:00"
---

Screen-off goes to the start of the screen it comes back on (APK of 677524c, layout-v1-folder-page3.json: a Media folder on page 3). Unfolded on pages 3 and 4 with Media open (s2-folder-open), input keyevent SLEEP, a fold, input keyevent WAKEUP: log 'configuration changed ... sw 443 dp', 'folded: at [3, 0]', then 'screen on: home goes to its start (sw 443 dp ...)'; screen s3-woke-folded is the cover screen's home (narrow start) with no folder. Folded on page 3, SLEEP (mWakefulness=Asleep), an unfold, WAKEUP: 'unfolded: at [3, 0] on [[3, 0], [4, 0]]' then 'screen on: home goes to its start (sw 852 dp ...)'; s5-woke-unfolded is home and page 2 side by side (the file is version 1, so the wide start is home's). On this emulator the posture change itself turns the screen on (SCREEN_ON 40-100 ms after the configuration line), so the configuration always arrived first here.
