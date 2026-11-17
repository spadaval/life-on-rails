---
schema: atelier.item
schema_version: 3
id: lor-yfm0
title: Today's Ask box keeps its Sent notice until the next message is typed
status: draft
mission: null
labels:
- dashboard
- bug
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T14:42:10.655995729Z
---

## Goal

After a question is sent from Today's Ask box, the 'Sent' notice stays until the next thing happens. Today the box's own code clears the notice straight after `act` sets it, so a sent question gives no sign it went.

## Done when

- [ ] Sending a question from Today leaves the notice visible until the next action or the next keystroke in the box, shown by the verify skill on the test server.
- [ ] A failed send shows its error in the same place.

## Scope

`src/routes/index.tsx`: `act` (lines 55-63) and `AskAgent` (from line 397) on 2026-11-17.

## Context

- Found by GPT-6.1 Sol's fact-check of the design audit on 2026-11-17 (mission lor-2803); read again in the code the same day.
