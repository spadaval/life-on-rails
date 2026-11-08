---
schema: atelier.review
schema_version: 2
id: lor-frwt
title: approval on lor-f3vn @6a3fcfe4
target: lor-f3vn
identity: reviewer-6get-1
commit: 6a3fcfe44fb504d68cb7d49f5aecac049ff41a02
created_at: 2026-11-08T02:38:43.287207356Z
event:
  event: approval
---

lor-6get (62b522c..6a3fcfe) approved. Every minimap change goes through HomeActions.edit (addPanel, movePanel, setWide, setStart, removePanel, setLocked) or actions.refused, so each writes with Undo or shows the edit's own reason. Locked: tile drags and the palette are not composed, actions(locked) is Show alone, Adding/Moving reset, and LayoutStore.edit's LOCKED rule still refuses any grid change; the padlock's setLocked passes it. Home vs minimap: gridFinger (GridHost.kt) and dragging return at the down while ui.minimap, the minimap's root swallows taps over home's cells, and openMinimap drops a held item, folder and menus. 56bc199: DisposableEffect drops a cell's bounds when it leaves the map (reach starts at 0, so a slot's key never changes in place); the font-scaled least tile width keeps 6 columns at font 1 on 379 dp and only scrolls at larger fonts. The pre-56bc199 APK proof (open/close, first palette adds) is unaffected: the fix changes drops only after the map shrinks and columns only above font scale 1, and the shrink case ran on the final APK (lor-plbj). Blocks move: lor-6get's line and lor-9uc1's new first line agree (5729d6b). Boat assembleDebug testDebugUnitTest --rerun at 6a3fcfe: 315 tests, 0 failures (MinimapTest 8/8); git diff --check clean. Not run: emulator (judged from receipts and screenshots in /tmp/claude-1000/6get-emu).
