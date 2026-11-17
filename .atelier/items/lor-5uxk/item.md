---
schema: atelier.item
schema_version: 3
id: lor-5uxk
title: 'The dashboard prototype: Today, Tasks, Goals and People per direction, switched by a dropdown'
status: draft
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-npa6
- lor-zwfd
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.112471157Z
---

## Goal

Sumeet can use each direction's dashboard: click through it, tick things, open a task, at his desk and on his phone, and flip between directions on the same made-up data.

## Done when

- [ ] `prototype/dashboard/` is a small Vite and React app with no server: it reads a frozen snapshot of the demo set and keeps changes in memory, so every control works and nothing is saved. A Version dropdown (and the URL hash) switches direction; `current` is the shipped dashboard's look on the same screens.
- [ ] A direction is one folder with its own tokens, components and screens; nothing is shared between directions but the data and the shell, so each is free to differ in structure.
- [ ] The shell and `current` are built here. Each direction from the brief is then one item, added by the lead, whose Done when is the brief's required dashboard set and dashboard interactions at 1500 and 412 px, with `shots/prototype.json`, a catalogue for lor-nde9's `scripts/shots.mjs` (which this item does not edit), capturing them.
- [ ] `pnpm proto:dev` reloads a change in under 2 seconds; `scripts/lab/publish.sh` puts the build under `/lab/dashboard/`.

## Scope

`prototype/dashboard/`, `shots/prototype.json`, and the `proto:dev` and `proto:build` scripts in the root `package.json`. The real dashboard under `src/` is not changed.

## Context

- Prototype code is rough on purpose: no tests, no accessibility pass beyond keyboard focus, no data layer. The chosen direction is rebuilt in `src/` by a later mission; the prototype is its reference.
