---
created_at: "2026-11-08T00:00:35.231889863+00:00"
id: "lor-ax8p"
evidence_type: "validation"
commit: "677524ca9cf4dfc664b58fd9d4c3659011c90f4c"
result: "pass"
captured_at: "2026-11-08T00:00:35.231796679+00:00"
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
title: "In place, no reloads. T3_Fold_API35, APK of 309485f, test server, layout-v1-three-pages-fold.json, folds by agent-device fold closed/open (emulator posture; committed device state CLOSED/OPENED). wm density: 390 folded and 390 unfolded, so one icon size: every LorIcons line reads 'drew <app> at 137 px' (56 dp at 390 dpi); both screens' icons are the same set. One 'HomeActivity created (sw 852 dp ...)' at start, one 'loaded the app list (22 activities)'. After one fold and unfold to warm (no icon drawn by them, densities equal): 34 LorIcons lines and 1 app-list load. Then a second fold, a second unfold, cmd uimode night yes, night no, font_scale 1.3, font_scale 1.0: exactly six LorHome lines, one each: 'configuration changed: screenSize smallestScreenSize screenLayout (sw 443 dp ...)', the same back to sw 852, 'uiMode (night yes)', 'uiMode (night no)', 'fontScale (font 1.3)', 'fontScale (font 1.0)'. LorIcons stayed 34, app-list loads stayed 1, HomeActivity created stayed 1, pid 3909 throughout. Logs: /tmp/claude-1000/ozj2-emu/log-warm.txt, /tmp/claude-1000/ozj2-emu/log-after.txt."
updated_at: "2026-11-08T00:00:35.239494883+00:00"
---

In place, no reloads. T3_Fold_API35, APK of 309485f, test server, layout-v1-three-pages-fold.json, folds by agent-device fold closed/open (emulator posture; committed device state CLOSED/OPENED). wm density: 390 folded and 390 unfolded, so one icon size: every LorIcons line reads 'drew <app> at 137 px' (56 dp at 390 dpi); both screens' icons are the same set. One 'HomeActivity created (sw 852 dp ...)' at start, one 'loaded the app list (22 activities)'. After one fold and unfold to warm (no icon drawn by them, densities equal): 34 LorIcons lines and 1 app-list load. Then a second fold, a second unfold, cmd uimode night yes, night no, font_scale 1.3, font_scale 1.0: exactly six LorHome lines, one each: 'configuration changed: screenSize smallestScreenSize screenLayout (sw 443 dp ...)', the same back to sw 852, 'uiMode (night yes)', 'uiMode (night no)', 'fontScale (font 1.3)', 'fontScale (font 1.0)'. LorIcons stayed 34, app-list loads stayed 1, HomeActivity created stayed 1, pid 3909 throughout. Logs: /tmp/claude-1000/ozj2-emu/log-warm.txt, /tmp/claude-1000/ozj2-emu/log-after.txt.
