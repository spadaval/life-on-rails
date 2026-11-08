---
schema: atelier.review
schema_version: 2
id: lor-vfxq
title: approval on lor-ph30 @f4e1c258
target: lor-ph30
identity: reviewer-sqfe-1
commit: f4e1c2582f2da583914873a313550af382301b73
created_at: 2026-11-08T00:37:16.790038309Z
event:
  event: approval
---

Review of lor-sqfe's slice 749419f..f4e1c25 (383b6b6 code, f4e1c25 records). Boat build by the reviewer (review-sqfe-build, assembleDebug testDebugUnitTest --rerun): passes, 302 tests, 0 failures, DrawerBottomTest's 5 included; git diff --check clean. Every Done when line holds against the code and the receipts' screenshots and snapshots, which I read. The inset change outside Scope: WindowInsets.bars is systemBars.union(displayCutout), which is safeDrawing without ime, so cutouts and bars pad as before on every cell (cellInsetsPadding's per-side rule unchanged) and on the row dots. Everything that pads for the keyboard still does: the drawer (cellInsetsPadding then imePadding, so the nav bar isn't counted twice), the agent page's columns, and the folder panel, the undo bar and the top bar, which keep safeDrawing (folder name field above the keyboard, 23). targetSdk 36 with enableEdgeToEdge, so adjustResize never resizes the window. The reversed results: Go opens results.go whatever the order; listTakes is right for both directions and tested; TalkBack now reads results top-down, the lowest-ranked block first and the field last, which follows from the layout the item asks for. The uiautomator deviation is sound: the agent-device snapshot reads the same UiAutomation tree, and 01-drawer-sections.json shows every control's top at y 2037 or lower, under 1200. Residual: a half agent page beside a half drawer shrinks while the drawer is searched. This needs a hand-edited file, the Report names it, and it is no worse than adjustPan's pan.
