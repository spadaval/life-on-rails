---
schema: atelier.item
schema_version: 3
id: lor-2fkr
title: Unfolding puts what was on the cover screen on the right half
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
created_at: 2026-11-10T05:48:59.743783537Z
---

## Goal

When the Fold opens, the panel that was on the cover screen is on the right half of the inner screen. Sumeet (2026-11-10): folding and unfolding work, but "you would normally expect continuity to mean that whatever was open on the front display now opens on the right side of the display, instead it opens on the left side. Which feels odd."

## Done when

- [ ] On the real Fold, unfolding from a half panel shows it on the right half, with its row's neighbour on the left, and folding returns to it.
- [ ] `layout-v2.md` and the spec say how the pair is chosen.

## Context

- Today a row's inner screens are built left to right by column: a half pairs with the next panel in its row (`InnerScreens` in `home/Inner.kt`, `wide` in `docs/missions/launcher-panels/layout-v2.md`, decisions lor-y7qi and lor-b1dx). So the cover's panel lands on the left.

## Unknowns

- What sits on the left when the cover's panel is the first half in its row, and whether pairing by the panel before it changes which panels can ever share a screen.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-znfb (Real Fold: the inner screen, and folding that keeps your place)
