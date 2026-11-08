---
schema: atelier.review
schema_version: 2
id: lor-lyh3
title: approval on lor-sbth @82cf37c9
target: lor-sbth
identity: reviewer-vtmn-1
commit: 82cf37c9ac20048cf22904a0551a35d202a190a6
created_at: 2026-11-08T01:12:07.003165609Z
event:
  event: approval
---

Review of lor-vtmn (main..lor-sbth, 0bc0a45 + 82cf37c): checked each Done when line against Layout.kt's widget parse (fields, unknown field, id, provider package/class with .Short expanded, height 40..2000, duplicate id naming both places, per layout), Panel.Stack.takes letting only widget repeat, WidgetEdit.kt's addWidget (before apps, else dock, else last), removeWidget and setWidgetHeight by id with refusals, widgetIdsToDelete over all layouts of all parsed files, and layout-v2.md against decision lor-9wlx. isEmptyPage treats a stack with a widget as not empty, so app edits don't strip it. The HomeScreen.kt branch (widget draws nothing until lor-r0f9) is the least that compiles and never reaches main alone. The API covers lor-r0f9's add, resize, remove, gone check and sweep, and lor-f3vn's note (addBlock refuses widget, moveBlock/removeBlock take it, GridEditTest's layouts hold widgets). Proof: boat assembleDebug testDebugUnitTest --rerun at 82cf37c, 43 suites, 311 tests, 0 failures (WidgetTest 9, GridEditTest 12, LayoutTest 11); git diff --check clean. No findings.
