---
schema: atelier.review
schema_version: 2
id: lor-rs7o
title: approval on lor-eh0y @cbcbb8a1
target: lor-eh0y
identity: reviewer-qdmy-1
commit: cbcbb8a1de435e2cb2261b6a325d67130ecd5af6
created_at: 2026-11-07T22:26:00.095818012Z
event:
  event: approval
---

Re-reviewed the fix range 9f6b69c..cbcbb8a (296ab86, 4cbf748). R1 lor-glg9 fixed: offsetOf places only a move's two cells; my own 30 fps screenrecord on emulator-5554 (head APK from boat, version 1 file with two extra pages) of a home press from page 2 shows only page 2 and home in every frame, logcat 'at [1, 0] from [3, 0]'. R2 lor-ymm9 fixed: wayTo returns back.way.opposite for the row the last vertical move came from; GridTest and GridPlaceTest cover both drawer directions. 4cbf748's placeWithLayer keeps placement, so drop zones still follow cells (lor-w307's 4.5 and 4.6 rerun). Unplaced cells during a move keep only off-screen bounds, and onGloballyPositioned never registers a cell that was never placed. Boat assembleDebug testDebugUnitTest: 232 tests, 0 failures (GridPlaceTest 3, GridTest 11). git diff --check clean. Weighed lor-chhu's strips, lor-w307's three rotated runs and lor-3nhh for the increment's lines.
