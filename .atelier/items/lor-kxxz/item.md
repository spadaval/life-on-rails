---
schema: atelier.item
schema_version: 3
id: lor-kxxz
title: Launcher robustness items carried from reviews
status: draft
mission: null
labels:
- launcher
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-07T00:03:36.802290443Z
---

## Goal

Fix the launcher robustness items carried from T3 Launcher's and Launcher polish's reviews that aren't about folding (those are in the Panels mission's Increment 2). Each is small and independent; pick it up when the launcher is next quiet, or when one of them bites.

## Done when

- [ ] Each item below is fixed with a test or emulator check, or recorded as not worth fixing with a reason: - R3: a duplicate thread when a slow launch is retried; - R4: quick-send text lost after a failed send once the sheet is gone; - R5: the thumbnail decoded at full size on the main thread; - Increment 4's review (b): `Held.takes` runs a full `drop` on every move over a target; - Increment 4's review (c): a drag cut off by screen-off, a call or gesture navigation may drop where the finger was; - the test of two steps on one unpinned folder builds its second `from` itself instead of calling `Held.step`; - `LookTest` checks `Palette`'s constants, not that `wallInk()` picks them; - R10: `animatePlacement` can restart its tween several times in one frame.

## Context

Detail for each in `docs/missions/launcher-polish/plan.md` (old Increment 8, and Increment 7's review for R10).
