---
created_at: "2026-11-07T00:03:36.779553203+00:00"
id: "lor-f3vn"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The minimap editor"
updated_at: "2026-11-07T00:03:36.779553203+00:00"
---

## Goal

The grid is edited on the phone, in a minimap like Smart Launcher's page manager: drag panel types from a palette into cells, move and remove panels, set `full` or `half`, choose each screen's `start`, and open a stack to add, order and remove its blocks. A layout lock, set in the minimap and in the home menu, stops every edit on home (drags, Remove, folder changes) until it is unlocked, as Smart Launcher's padlock does; it is a field in `layout.json`, written with Undo. Every change is written to `layout.json` with Undo, as every phone edit is.

## Done when

Provisional: on `T3_Phone_API35` and `T3_Fold_API35`, each kind of edit made in the minimap, the file read after each and Undo after each restoring its sum; no edit can produce a file the parser refuses (JVM tests over the edit functions); with the layout locked, a long press and drag on home starts no edit and says why in the Undo bar, and unlocking restores editing.

## Unknowns

- Where the minimap opens from (the home menu, a pinch).
- How a block's own settings (a widget's size, an apps block's columns) are edited.
