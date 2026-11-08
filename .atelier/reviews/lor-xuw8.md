---
schema: atelier.review
schema_version: 2
id: lor-xuw8
title: approval on lor-ph30 @20f95287
target: lor-ph30
identity: reviewer-ozj2-1
commit: 20f9528791f0180980bef42432103d2684203b8d
created_at: 2026-11-08T00:05:13.618584934Z
event:
  event: approval
---

Review of lor-ozj2's slice 7bea31e..20f9528. Boat build of 20f9528 run by the reviewer twice (review-ozj2-build, and review-ozj2-build2 with testDebugUnitTest --rerun): assembleDebug passes, 276 tests, 0 failures, including CardCallsTest, IconCacheTest, DragTest's slot tests, GridPlaceTest's touched-half tests and AgentStatesTest's FormScroll test. git diff --check is clean. Read the diff against each Done when line and the receipts lor-ax8p, lor-v9wu, lor-f0c5, lor-b6x9 and lor-ultr (logs and screenshots checked, emulator not re-run). The AgentPage.kt FormScroll change is outside lor-ozj2's Scope but inside lor-ph30's, and a Done when line needs it; accepted. F4: a late result completes because LiveUpdates holds the connection for the process; lor-0fbs (defer) covers busy state lost on a move away. Merge with main: git merge-tree shows content conflicts in HomeScreen.kt's FolderPanel (lor-gnk0's full-screen folder still builds Zone.Area, Zone.Item and the old Source) and in DragTest.kt (two sets of added tests). The resolution must port FolderPanel to Slot, DropMark and Source(b, slot, 1), and it needs its own review. lor-v692's dropHeld change (stripEmptyPanels(h.original.current)) auto-merges and agrees with slot pickup, and lor-gnk0's FolderExit reads the resolved zones list, so it works as it is.
