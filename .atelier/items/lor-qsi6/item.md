---
schema: atelier.item
schema_version: 3
id: lor-qsi6
title: The minimap reworked so that arranging panels is obvious
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
created_at: 2026-11-10T05:48:59.805682247Z
---

## Goal

Arranging home's panels is intuitive on the Fold. Sumeet (2026-11-10): the minimap "just kinda sucks overall", the way it is fixed in place is "super weird", and it is "not very intuitive".

## Done when

- [ ] Sumeet has said what he expected it to do, and a design answers that before code is written.
- [ ] He arranges a panel on the real Fold without help and calls it fine.

## Unknowns

- Which parts are wrong: where it sits, how it is opened, the palette, the drags, or the idea of a map at all. Ask him before designing. Smart Launcher's page manager was the reference (lor-f3vn).

## Notes

- 2026-11-10 [human] Sumeet, 2026-11-10, on what is wrong. Smart Launcher's minimap shows the screens themselves laid out in miniature, like a tile map. Ours draws a massive virtual grid with slots, and you build home by dragging text onto the grid: "it feels weird and it looks weird". The words mean nothing to him ("What's a stack, for example"). "Most of my interactions are just tapping random buttons and seeing random bullshit happen." So the rework shows each screen as a miniature of what it really looks like, arranged as they sit, moved by dragging the miniatures, with no schema words on screen.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-92o7 (Real Fold: the minimap and the layout lock)
