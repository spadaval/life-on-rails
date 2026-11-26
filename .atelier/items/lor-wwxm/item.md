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

- [ ] On the demo set at 1500 by 950, after `/?at=15:00&data=running` and Stop, the suggestion row ("Ship the Field Guide app", "Draft the offline maps spec", "Public beta", "Something else...") sits clear of "09:30 Field Guide standup", with visible space between them, and no arc label is cut off at the edge (lor-auhc's fix stays).
- [ ] The same holds at 1100px, and with the longest three suggestions the demo set can produce.

## Scope

`src/components/DayArc.tsx` and where Today places the suggestion row. Found by lor-a8wq: lor-auhc moved the 09:30 label inward so it is no longer clipped (it read "iide standup" at 93d569a3), and the suggestion row is centred under the readout and grows with its labels. With the gallery's suggestions (`/?at=15:00&data=base`) there is about 30px between them; right after stopping the demo's timer the suggestions are longer and the label ends about 5px from "Ship the Field Guide app", so the two read as one row. lor-a8wq's Report has the screenshot.

## Context

Edit 6 of mission lor-40i0 ("The clipped arc label fixed"). Low priority: nothing is hidden or wrong, the label just crowds the chips.
