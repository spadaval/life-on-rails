---
schema: atelier.review
schema_version: 2
id: lor-zdsk
title: finding on lor-f3vn @c52f1f6d
target: lor-f3vn
identity: reviewer-9uc1-1
commit: c52f1f6d639363fb569c253f645435029f63300f
created_at: 2026-11-08T03:58:05.505083380Z
event:
  event: finding
  recommendation: defer
---

Minimap.kt BlockEditor: a Dock row can still be dragged or moved up, and the Undo bar says e.g. 'Dock moved to the top.', but home always draws the dock last (see the layout-v2.md finding), so the edit changes the file and nothing on screen. This is the same gap lor-9uc1 closed for the clock. It harms nothing and Undo reverts it. Follow-up: pin the dock last in the block list (no handle, Move up/down disabled, a palette drop never after it) or draw it in place on home.
