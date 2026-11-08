---
schema: atelier.review
schema_version: 2
id: lor-36vo
title: approval on lor-sbth @748a6909
target: lor-sbth
identity: reviewer-r0f9-1
commit: 748a690910c3b8808bc20f8851ac69694380a4fe
created_at: 2026-11-08T02:23:32.913936120Z
event:
  event: approval
---

Review of lor-r0f9 (82cf37c..748a690: eea4aea, 1e0a51e, 748a690). Checked each Done when line against Widgets.kt (host id 1024, views from the application context, LorWidgets lines, CheckLongPressHelper-style long press, menu with Settings only for WIDGET_FEATURE_RECONFIGURABLE, resize within WidgetSize.resizeRange, gone note), HomeActivity.kt (onStart/onStop listening, bind then configure with MODE_BACKGROUND_ACTIVITY_START_ALLOWED on 34+, every cancel and refused edit deletes the id), HomeStore.kt's sweep and HomeScreen.kt's StackScroll/FillColumn, and receipts lor-3ztb and lor-ctox with their screenshots. Out-of-Scope GridHost.kt: only the press point added to GridScrolls (existing scrollsInGrid callers ignore it) and gridFinger's early return while a resize frame or the picker is open; nothing else in the grid's gestures changed. Sweep: runs once per process, only after layout.json parses, with every layout of layout.json plus layout.good.json when it parses; an add can't be in flight before that first parse, and an add while the file is broken is refused and its id deleted. A good file that doesn't parse can't be restored from, so its ids are not lost. Bind and configure: home press clears the task (cancel deletes the id); recreation or process death drops the result (finding lor-wueo, defer). No widgets, no change: b0 and b2 differ by at most 3 levels per channel below the status bar (emulator rendering noise, 0 px above that), so the claim holds; StackScroll is disabled and reports no scroll while it fits. Lock: Add widget, Resize and Remove hidden on LayoutFile.locked, ahead of lor-zzyy. lor-lz97 can reuse StackScroll as it is, provided its blocks don't use lazy lists (the scroll gives infinite height). Boat assembleDebug testDebugUnitTest --rerun at 748a690: tests ran fresh, 43 suites, 314 tests, 0 failures (WidgetTest 12/12). git diff --check clean. Two defer findings: lor-wueo, lor-qbfx.
