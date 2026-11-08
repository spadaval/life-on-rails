---
created_at: "2026-11-08T02:36:04.764014257+00:00"
id: "lor-o595"
evidence_type: "validation"
commit: "728cef6967c1605d0f9971d6a199191216161d28"
result: "pass"
captured_at: "2026-11-08T02:36:04.763956981+00:00"
path: "/tmp/claude-1000/r0f9-emu/fix"
agent_identity: "lor-r0f9-author"
target:
  kind: "issue"
  id: "lor-r0f9"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-r0f9"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Fixes for lor-wueo and lor-qbfx on T3_Phone_API35, build of 728cef6's code (boat, 43 suites, 314 tests, 0 failures), agent-device input. R1: Digital added through the picker, bind dialog allowed ('bound 8'), then at its configure screen 'cmd locale set-app-locales com.spadaval.lor --locales fr-FR' (locale is not in HomeActivity's configChanges). Choosing Solid: events log wm_on_destroy_called then wm_on_create_called for HomeActivity in pid 11533, 'HomeActivity created', 'configure 8: done', 'added 8 ... at [1, 0], 100 dp'; the file holds widget 8 (r1-locale-mid-configure-added.png). R2: clock widget's Resize frame open (r2-frame-open.png); a horizontal swipe from (900, 1900), outside the frame: no LorMotion or LorGrid line, the frame closed (r2-after-swipe-outside.png, r2-strip.png); the next same swipe moved the grid ('grid [1, 0] to [2, 0] ... goes'). Afterwards LoR's app locale cleared and the file lor-6get left restored (sweep deleted 8)."
updated_at: "2026-11-08T02:36:04.775120148+00:00"
---

Fixes for lor-wueo and lor-qbfx on T3_Phone_API35, build of 728cef6's code (boat, 43 suites, 314 tests, 0 failures), agent-device input. R1: Digital added through the picker, bind dialog allowed ('bound 8'), then at its configure screen 'cmd locale set-app-locales com.spadaval.lor --locales fr-FR' (locale is not in HomeActivity's configChanges). Choosing Solid: events log wm_on_destroy_called then wm_on_create_called for HomeActivity in pid 11533, 'HomeActivity created', 'configure 8: done', 'added 8 ... at [1, 0], 100 dp'; the file holds widget 8 (r1-locale-mid-configure-added.png). R2: clock widget's Resize frame open (r2-frame-open.png); a horizontal swipe from (900, 1900), outside the frame: no LorMotion or LorGrid line, the frame closed (r2-after-swipe-outside.png, r2-strip.png); the next same swipe moved the grid ('grid [1, 0] to [2, 0] ... goes'). Afterwards LoR's app locale cleared and the file lor-6get left restored (sweep deleted 8).
