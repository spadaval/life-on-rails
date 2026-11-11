---
schema: atelier.item
schema_version: 3
id: lor-dx87
title: Apps and folders sit in any free cell of home's grid
status: draft
mission: null
labels:
- launcher
- feature
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-10T05:48:59.867786730Z
---

## Goal

An app or folder can be put in any free cell of a home page and stays there, with gaps allowed. Sumeet (2026-11-10): "I don't think we can put icons at arbitrary locations, we seem to be doing the iOS thing of it has to be a continuous flow grid. Which is so weird."

## Done when

- [ ] On the real Fold, an app dragged to an empty cell with gaps before it stays in that cell after a restart, on both screens.
- [ ] `layout-v2.md` says how a position is written, and a file without positions still reads as today's flow.

## Context

- Within an `apps` block positions are list order, filled in reading order (`home/Layout.kt`, the comment at line 48: "so an agent edits a list rather than coordinates"). This is a schema change.

## Unknowns

- How a fixed cell behaves when the column count differs between the cover and inner screens.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-mlx6 (Real Fold: today's home on the grid)
