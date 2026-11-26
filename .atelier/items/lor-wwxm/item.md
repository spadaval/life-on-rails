---
schema: atelier.item
schema_version: 3
id: lor-wwxm
title: 'Today''s arc: the 09:30 event label runs into the timer suggestions when no timer runs'
status: open
mission: null
labels: []
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: lor-auhc
created_at: 2026-11-26T01:35:11.236667509Z
---

## Goal

On Today with no timer running, the arc's event labels and the row of timer suggestions under the readout never touch, so each reads as what it is.

## Done when

- [ ] On the demo set at 1500 by 950, `/?at=15:00&data=base` (no timer) draws "09:30 Field Guide standup" clear of the "Ship the Field Guide app" suggestion, with visible space between them, and no arc label is cut off at the edge (lor-auhc's fix stays).
- [ ] The same holds at 1100px and on `/?at=09:00&data=base`.

## Scope

`src/components/DayArc.tsx` and where Today places the suggestion row. Found by lor-a8wq: lor-auhc moved the 09:30 label inward so it is no longer clipped (it read "iide standup" at 93d569a3), and it now ends about 5px from the first suggestion chip, so the two read as one row (see `/tmp/claude-1000/.../undo/1-stopped.png` in lor-a8wq's Report, or the gallery's "Today at 15:00").

## Context

Edit 6 of mission lor-40i0 ("The clipped arc label fixed"). Low priority: nothing is hidden or wrong, the label just crowds the chips.
