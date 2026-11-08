---
schema: atelier.review
schema_version: 2
id: lor-my0o
title: approval on lor-f3vn @62b522cb
target: lor-f3vn
identity: reviewer-zzyy-1
commit: 62b522cb7f5ed749e6982a69cd534f6874bc9de2
created_at: 2026-11-08T01:19:25.101284224Z
event:
  event: approval
---

lor-zzyy (main..lor-f3vn @ 62b522c) approved. landOver (Drag.kt) throws HOME_CHANGED unless now equals original in every field but locked, so a drop or folder + over a file changed in any other way is still refused; only an outside lock alone reaches LayoutStore.edit's LOCKED rule (HomeLockTest covers all three cases). Locked: appEdits leaves Move to section and Hide-when-unplaced (matches lor-73li's store rule) for every AppMenu caller (home, dock, folder, drawer, search); the one dragging handler serves all four drag layers and says LOCKED_SAY only after the 16 dp move, so a plain long press for App info is silent; folder tile opens no FolderMenu (the only path to Rename, Cover, Remove); FolderName is text with no +; Edit home hidden and HomeScreen drops edit mode on any locked file. Home settings' edits touch only hidden/drawer/look. Receipts' snapshots d1-d11 match the menu lines; agent-device snapshot in place of uiautomator dump reads the same accessibility tree, acceptable. Boat assembleDebug testDebugUnitTest --rerun: 307 tests, 0 failures, HomeLockTest's 5 ran fresh. git diff --check clean; atelier check lor-zzyy passes.
