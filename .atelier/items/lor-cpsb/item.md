---
schema: atelier.item
schema_version: 3
id: lor-cpsb
title: 'Queue in the prototype: Start over brings back dismissed items, and the phone hint names the q key'
status: ready
mission: null
labels:
- dashboard
- design
priority: low
blocked_by: []
implementers: []
ready:
  by: lead of lor-2803, found during the mission
  at: 2026-11-17T19:31:03.050152343Z
trivial: null
caused_by: null
created_at: 2026-11-17T19:31:03.011396322Z
---

## Goal

Two small findings the reviewer of lor-vx69 deferred. The prototype's Start over button does not undo Queue's own dismissals such as Not today, because the shell sends no reset signal to a direction; and at phone width the add hint loses its mention of `q`.

## Done when

- [ ] Start over resets a direction's own state in every direction, through one signal from the shell.
- [ ] Queue's add hint at 412 px says how to add.

## Scope

`prototype/dashboard/`.

## Context

- Found during mission lor-2803 on 2026-11-17 and filed by its lead.
