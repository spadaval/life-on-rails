---
created_at: "2026-11-08T01:03:34.645580738+00:00"
id: "lor-ar10"
evidence_type: "validation"
commit: "8a63ed601defc89ccbb3d38dc445dc3a8dd9aea0"
result: "pass"
captured_at: "2026-11-08T01:03:34.645488817+00:00"
path: "/tmp/claude-1000/ph30-emu/fold/e2e"
agent_identity: "lor-ph30-evidence"
target:
  kind: "issue"
  id: "lor-ph30"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-ph30"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "End to end on T3_Fold_API35, one emulator session, lor-ph30 head 8a63ed6 (APK built on boat, sha256 0ac13c26...), against the test server (main checkout's scripts/ui-server.sh, :3857; the app's server_url http://10.0.2.2:3857). Layout: version 1 with two extra pages (ph30-emu/layout-v1-two-pages-fold.json: qdmy-emu/layout-v1-two-pages.json with its pages' apps swapped for apps installed on the Fold, as lor-el51 did, since Notes, Paint, Lexica, AntennaPod and NewPipe aren't on it). Input by agent-device (pan, press, type, fold closed/open, home); screen off and on by adb input keyevent SLEEP/WAKEUP; stale device_state override reset first. Steps, each screenshotted in ph30-emu/fold/e2e: (01) unfolded, home starts on its wide start: home left, page 2 (Clock, Files, TMoble) right, LorGrid 'unfolded: at [1, 0] on [[1, 0]]'. (02, 03) a swipe up opened the drawer ([1, 1]); 'ma' typed in the bottom field: Data usage, Maps, Messages. (04) fold: the drawer on the cover screen (1080x2424) with 'ma' in the field and the same results; 'configuration changed: screenSize smallestScreenSize screenLayout (sw 443 dp)', 'folded: at [1, 1]'. (05) unfold: the drawer again with 'ma'; 'unfolded: at [1, 1]'. (06) home press: the wide start ([1, 0] with [2, 0]). (07, 08) a swipe left to page 3 ([3, 0], YT Music and Contacts, at full width), a tap on it. (09) fold: page 3 on the cover screen; 'folded: at [3, 0]'. (10) screen off (wakefulness Asleep), unfold (still Asleep), screen on: no keyguard, the wide start (home and page 2); 'screen on: home goes to its start'. Logcat over the whole session (logcat-lor.txt, logcat-events.txt): one 'HomeActivity created' and one wm_on_create_called for HomeActivity (token 139114998, pid 4061 throughout), one wm_on_restart_called at the screen on, no destroy or relaunch; the app list loaded once. A first attempt was discarded before step 1: agent-device 'open com.spadaval.lor' started LoR's main app over home, and the original two-page file showed page 2 empty on the Fold (its apps not installed); logcat and screenshots were cleared and the session restarted with force-stop."
updated_at: "2026-11-08T01:03:34.656461537+00:00"
---

End to end on T3_Fold_API35, one emulator session, lor-ph30 head 8a63ed6 (APK built on boat, sha256 0ac13c26...), against the test server (main checkout's scripts/ui-server.sh, :3857; the app's server_url http://10.0.2.2:3857). Layout: version 1 with two extra pages (ph30-emu/layout-v1-two-pages-fold.json: qdmy-emu/layout-v1-two-pages.json with its pages' apps swapped for apps installed on the Fold, as lor-el51 did, since Notes, Paint, Lexica, AntennaPod and NewPipe aren't on it). Input by agent-device (pan, press, type, fold closed/open, home); screen off and on by adb input keyevent SLEEP/WAKEUP; stale device_state override reset first. Steps, each screenshotted in ph30-emu/fold/e2e: (01) unfolded, home starts on its wide start: home left, page 2 (Clock, Files, TMoble) right, LorGrid 'unfolded: at [1, 0] on [[1, 0]]'. (02, 03) a swipe up opened the drawer ([1, 1]); 'ma' typed in the bottom field: Data usage, Maps, Messages. (04) fold: the drawer on the cover screen (1080x2424) with 'ma' in the field and the same results; 'configuration changed: screenSize smallestScreenSize screenLayout (sw 443 dp)', 'folded: at [1, 1]'. (05) unfold: the drawer again with 'ma'; 'unfolded: at [1, 1]'. (06) home press: the wide start ([1, 0] with [2, 0]). (07, 08) a swipe left to page 3 ([3, 0], YT Music and Contacts, at full width), a tap on it. (09) fold: page 3 on the cover screen; 'folded: at [3, 0]'. (10) screen off (wakefulness Asleep), unfold (still Asleep), screen on: no keyguard, the wide start (home and page 2); 'screen on: home goes to its start'. Logcat over the whole session (logcat-lor.txt, logcat-events.txt): one 'HomeActivity created' and one wm_on_create_called for HomeActivity (token 139114998, pid 4061 throughout), one wm_on_restart_called at the screen on, no destroy or relaunch; the app list loaded once. A first attempt was discarded before step 1: agent-device 'open com.spadaval.lor' started LoR's main app over home, and the original two-page file showed page 2 empty on the Fold (its apps not installed); logcat and screenshots were cleared and the session restarted with force-stop.
