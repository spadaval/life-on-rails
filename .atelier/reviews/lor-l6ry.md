---
schema: atelier.review
schema_version: 2
id: lor-l6ry
title: finding on lor-f3vn @1b8b8ec0
target: lor-f3vn
identity: reviewer-f3vn-fix-3
commit: 1b8b8ec02ef5a72d9a3c40c77faf9e98bc82522c
created_at: 2026-11-08T07:40:45.815302618Z
event:
  event: finding
  recommendation: fix_now
---

low: .atelier/evidence/lor-ex2u.md, step 15: the receipt says 15 shows a long-press drag on the dock's Phone picking nothing up with the bar saying 'Home is locked. Unlock it from the home menu.', but e2e-fold-3/15-locked-drag-mid.png and 16-locked-drag-after.png both show quick send open over a dimmed home ('Ask an agent...', T3 disconnected), and no Undo bar. logcat-events.txt has a HOME intent at 02:36:15.599 and ActivityTaskManager starts QuickSendActivity at .605, so the drag ran over quick send, not on home. The mtime claim (02:36:10.808 before and after) is in no saved file either; only 14 and 16's JSON sha256 match (609ebddc), which does show nothing was written. So lor-ex2u, which the Report says replaces lor-fwn8, doesn't show the Done when's 'a drag on home picks nothing up and the Undo bar says why'. 7c190c2 doesn't touch the drag code, so lor-fwn8's 15-16 (7e2badd's code) likely still hold. Fix: correct the receipt's step 15 to say what 15-16 show and cite lor-fwn8's 15-16 for the locked drag (or re-run that step on home), and drop or source the mtime claim. Also, the Report's Proof says 'see the note below' for lor-fwn8, but that note is above the Report.
