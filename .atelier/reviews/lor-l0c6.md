---
schema: atelier.review
schema_version: 2
id: lor-l0c6
title: approval on lor-sbth @3229a0e5
target: lor-sbth
identity: reviewer-r0f9-1
commit: 3229a0e5363cee308d9d4e6bf0fd2095f11e2e9f
created_at: 2026-11-08T02:37:04.791271171Z
event:
  event: approval
---

Re-review of the fixes for lor-wueo and lor-qbfx only (728cef6, 5e63a2a..3229a0e). R1: the in-flight Adding now lives in WidgetHost (process scope, held by HomeStore), so a HomeActivity recreated behind the configure screen finds it; the configure result is taken only when its EXTRA_APPWIDGET_ID matches the add or is absent; the bind dialog's result goes through ActivityResultRegistry, which survives recreation; a process death still leaves the id to the next sweep, as the comment now says. Receipt lor-o595 shows a locale change mid-configure placing widget 8. R2: gridFinger now returns on a consumed press. In the Initial pass the root sees the press before any child, so only an earlier Initial-pass handler on the root or an ancestor can have consumed it, and the only one is closeResizeOutside (dragging doesn't consume the press; the drawer's list and letter rail, a widget's AndroidView, folder taps and lor-f3vn's Minimap all consume in the Main pass, after gridFinger; the Minimap uses a ui.minimap check instead). Receipt lor-o595 shows a swipe outside the frame closing it without moving the grid, then the next swipe moving it. lor-f3vn will meet a one-line textual conflict on gridFinger's early return. Boat assembleDebug testDebugUnitTest --rerun at 3229a0e: tests ran fresh, 43 suites, 314 tests, 0 failures. git diff --check clean. Resolves lor-wueo and lor-qbfx.
