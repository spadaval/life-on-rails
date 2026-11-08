---
created_at: "2026-11-08T03:34:45.919067971+00:00"
id: "lor-sj8h"
evidence_type: "validation"
commit: "db4cb4fe963e9a0c2ab38954c0bbdc435e1effc8"
result: "pass"
captured_at: "2026-11-08T03:34:45.919012197+00:00"
path: "/tmp/claude-1000/sbth-emu/fold"
agent_identity: "lor-sbth-evidence"
target:
  kind: "issue"
  id: "lor-sbth"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-sbth"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "End to end on T3_Fold_API35, one emulator session, lor-sbth head db4cb4f (APK built on boat, sha256 3fd56e90...), against the test server (main checkout's scripts/ui-server.sh, :3857, life_on_rails_test, weather live through the env file's key; the app's server_url http://10.0.2.2:3857). Test rows: a Milpitas location observation (37.4323, -121.8996) and a google_event 'Dinner at the Milpitas place' at 23:38 local; an alarm at 7:00 set with SET_ALARM (label sbth-e2e). Start: version 1 layout.json with two extra pages (ph30-emu/layout-v1-two-pages-fold.json), folded by agent-device. Markor isn't on the Fold, so Markor 2026.10.0 from F-Droid (signed by F-Droid's key) was installed with a six-line todo.txt for the list widget. Steps (screenshots in sbth-emu/fold): (01) folded start: the clock block with 'Dinner at the Milpitas place at 23:38', 'Alarm 7:00', '63° Clear', then the apps. (02-05) Add widget from the home menu, Clock's Digital: Android's bind dialog, Create, its configure screen, Solid: 'bound 6 ... (the dialog allowed it)', 'added 6 ... at [1, 0], 100 dp', 'size 6: 419x100 dp'. (06-08) Markor's todo widget: bind dialog, Create: 'added 7 ... 80 dp', 'size 7: 419x80 dp'; home's stack: clock block (event, alarm, weather), Digital, Markor's list (Task number 1, 2, ...), apps, dock. curl of /device/home at 03:31:14Z: weather temp_c 17 Clear (63 °F), the event (curl-device-home.txt). (09) unfold: home is the left half paired with page 2 ('unfolded: at [1, 0] on [[1, 0], [2, 0]]'), the same blocks in order, 'size 6: 402x100 dp', 'size 7: 402x80 dp'. (10) fold back: narrow again, 'size 6: 419x100', 'size 7: 419x80'; the file holds ids 6 and 7. (11) am force-stop, then home back in a new process (pid 11396): 'sweep: the host holds [6, 7]; deleted []', no bind dialog, both drawn and sized 419 dp. (12) the file read at the end (12-layout-final.json): version 2, [1, 0] stack = clock, widget 6 (deskclock DigitalAppWidgetProvider, 100), widget 7 (markor TodoWidgetProvider, 80), apps, dock. Note: with a page beside it, home unfolded is a half of the pair (lor-ph30), not lor-j87u's full-width two-column flow. Afterwards: the alarm deleted in Clock, test rows deleted, Markor and its todo.txt removed, layout.json and launch-stats.json put back (the next start's sweep deleted 6 and 7), unfolded with device_state reset, font scale 1.0 throughout."
updated_at: "2026-11-08T03:34:45.933249403+00:00"
---

End to end on T3_Fold_API35, one emulator session, lor-sbth head db4cb4f (APK built on boat, sha256 3fd56e90...), against the test server (main checkout's scripts/ui-server.sh, :3857, life_on_rails_test, weather live through the env file's key; the app's server_url http://10.0.2.2:3857). Test rows: a Milpitas location observation (37.4323, -121.8996) and a google_event 'Dinner at the Milpitas place' at 23:38 local; an alarm at 7:00 set with SET_ALARM (label sbth-e2e). Start: version 1 layout.json with two extra pages (ph30-emu/layout-v1-two-pages-fold.json), folded by agent-device. Markor isn't on the Fold, so Markor 2026.10.0 from F-Droid (signed by F-Droid's key) was installed with a six-line todo.txt for the list widget. Steps (screenshots in sbth-emu/fold): (01) folded start: the clock block with 'Dinner at the Milpitas place at 23:38', 'Alarm 7:00', '63° Clear', then the apps. (02-05) Add widget from the home menu, Clock's Digital: Android's bind dialog, Create, its configure screen, Solid: 'bound 6 ... (the dialog allowed it)', 'added 6 ... at [1, 0], 100 dp', 'size 6: 419x100 dp'. (06-08) Markor's todo widget: bind dialog, Create: 'added 7 ... 80 dp', 'size 7: 419x80 dp'; home's stack: clock block (event, alarm, weather), Digital, Markor's list (Task number 1, 2, ...), apps, dock. curl of /device/home at 03:31:14Z: weather temp_c 17 Clear (63 °F), the event (curl-device-home.txt). (09) unfold: home is the left half paired with page 2 ('unfolded: at [1, 0] on [[1, 0], [2, 0]]'), the same blocks in order, 'size 6: 402x100 dp', 'size 7: 402x80 dp'. (10) fold back: narrow again, 'size 6: 419x100', 'size 7: 419x80'; the file holds ids 6 and 7. (11) am force-stop, then home back in a new process (pid 11396): 'sweep: the host holds [6, 7]; deleted []', no bind dialog, both drawn and sized 419 dp. (12) the file read at the end (12-layout-final.json): version 2, [1, 0] stack = clock, widget 6 (deskclock DigitalAppWidgetProvider, 100), widget 7 (markor TodoWidgetProvider, 80), apps, dock. Note: with a page beside it, home unfolded is a half of the pair (lor-ph30), not lor-j87u's full-width two-column flow. Afterwards: the alarm deleted in Clock, test rows deleted, Markor and its todo.txt removed, layout.json and launch-stats.json put back (the next start's sweep deleted 6 and 7), unfolded with device_state reset, font scale 1.0 throughout.
