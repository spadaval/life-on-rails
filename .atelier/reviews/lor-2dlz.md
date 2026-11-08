---
schema: atelier.review
schema_version: 2
id: lor-2dlz
title: approval on lor-f3vn @46602b84
target: lor-f3vn
identity: reviewer-f3vn-fix-4
commit: 46602b84223e60a55ebd7965ffbeae5a397e823b
created_at: 2026-11-08T07:45:12.489683032Z
event:
  event: approval
---

lor-f3vn approved at 46602b8 (records only since 7c190c2). lor-slsy: redo-locked-drag/01-03 show home (the [2, 0] stack, the dock's Phone) with Phone still in the dock mid-drag, the bar 'Home is locked. Unlock it from the home menu.' and Phone's menu with New contact, App info and Move to section... only; the down (1186,1958) is on Phone and the up toward the date; mtime-before/after both 02:36:33.984; 01 and 03 JSON both sha256 609ebddc; top-activity-after.txt holds HomeActivity. Its 'before' dumpsys is in no file, but logcat-events.txt has focus leaving HomeActivity for the menu's Pop-Up Window at 02:41:22, so home was in front. lor-8ios: logcat-lor.txt has 'at [3, 0] from [2, 0]' at 02:41:54.450 and 01 shows [3, 0] and [4, 0]; logcat-events.txt has Home settings paused for sleep at 02:42:06.6 and resumed at 09.06; then 'screen on: home goes to its start' at 12.978 and 'the minimap opens from Home settings' at .980; 04 is the open minimap with [2, 0] selected, which Minimap.kt:346 takes from ui.grid.cell, so home was moved back before it opened. lor-5i6p: 7c190c2 is correct in either order of onStart and onNewIntent (slept || toStart sets minimapAfterStart, the toStart effect opens it after homePressed); e2e-fold-3/21 shows the no-fold screen-off still lands on the inner start. lor-rj7u (layout-v2.md:118), lor-zdsk and lor-1hbc (receipt lor-op8w) are fixed. The Report and the 2026-11-08 notes match the receipts. End to end 'in one session': met. lor-slsy and lor-8ios ran in the same app process (pid 3700, created 02:34:58) as lor-ex2u, with layout.json untouched since lor-ex2u's Undo (mtime 02:36:33.984, sha256 equal to step 14's). The locked drag ran about 5 minutes after the run's Unlock and Undo rather than between them, on byte-identical locked state; neither receipt says so. Proof: build-minimapfix has 345 tests, 0 failures, APK 07aac4f3; no android/ or src/ change since 7c190c2; git diff --check HEAD^ HEAD clean.
