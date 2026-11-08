---
created_at: "2026-11-07T23:37:24.803765046+00:00"
id: "lor-92o7"
issue_type: "validation"
labels:
- "launcher"
- "needs-human"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-f3vn"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Real Fold: the minimap and the layout lock"
updated_at: "2026-11-07T23:37:24.803765046+00:00"
---

## Goal

Check on the real Fold that the minimap and the layout lock (lor-f3vn) work on One UI, on both screens, by Sumeet's hand: the minimap opened from the home menu; a panel dragged from the palette, a panel moved and two swapped by the finger; a stack set Full and Half and seen on the inner screen; the inner screen's start set and seen after the screen has been off; a block reordered by its handle and moved to another stack; Undo after an edit; and home locked from the home menu, with a long press opening an app's menu without its editing entries, a drag picking nothing up, and unlocking restoring editing. And whether the minimap reads as Smart Launcher's page manager does for him.

## Done when

Each check has a result in an evidence receipt on this item, from Sumeet's use or a session he allows; a failure files a bug linked here. One result says whether a drag in the minimap ever moved the grid under it or pulled down the shade.

## Context

Reason it is after merge: a device no agent can reach, installing on Sumeet's phone is his call, and whether the minimap feels like Smart Launcher's is his judgment. The stand-in, run before merge, is lor-f3vn's end-to-end check and its children's checks on `T3_Phone_API35` and `T3_Fold_API35`.
