---
schema: atelier.item
schema_version: 3
id: lor-m7r6
title: Scripted gestures in the lab land where they are aimed
status: ready
mission: null
labels:
- tools
priority: low
blocked_by: []
implementers: []
ready:
  by: lead of lor-2803, found during the mission
  at: 2026-11-17T19:31:02.968167040Z
trivial: null
caused_by: null
created_at: 2026-11-17T19:31:02.929728581Z
---

## Goal

A gesture script's drag ends where it says and a fling ends on the same row every run. Today (lor-293p) a drag can end 5 dp short and a fling varies by about five rows between identical runs; the layer that causes it was not found.

## Done when

- [ ] The cause is named in the Report.
- [ ] Ten runs of the drag and the fling scripts end within 1 dp and on the same row, or the Report says why that cannot be had and what scripts should check instead.

## Scope

`scripts/lab/` and the lab's input handling in `homecore`'s `wasmJsMain`.

## Context

- Found during mission lor-2803 on 2026-11-17 and filed by its lead.
