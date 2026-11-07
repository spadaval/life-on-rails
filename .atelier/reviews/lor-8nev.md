---
schema: atelier.review
schema_version: 2
id: lor-8nev
title: finding on lor-r0f9 @4220a2d8
target: lor-r0f9
identity: reviewer-sbth-clgw-plan-1
commit: 4220a2d8f2c9a8b5f26f4c0fe132985e7c524299
created_at: 2026-11-07T22:23:32.748690385Z
event:
  event: finding
  recommendation: fix_now
---

R3 Done when line 3 proves 'the views are not recreated' by 'the LorWidgets bind count stays the same', but binding happens only when a widget is added, so a fold never changes the bind count whether or not the AppWidgetHostView is recreated. The claim may also be false by construction: a block moving from the narrow single column into a wide-flow column (lor-j87u) sits at a different place in the composition, which disposes its AndroidView unless movableContentOf or one layout is used. Fix: log and count view creations (createView) instead, or drop the non-recreation claim and check only that the widget redraws at its new size with the same id.
