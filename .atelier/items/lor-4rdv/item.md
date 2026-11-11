---
schema: atelier.item
schema_version: 3
id: lor-4rdv
title: 'Real Fold: the panels and uploads after Tailscale comes up'
status: ready
mission: null
labels:
- chain
- needs-human
- loose_end
priority: medium
blocked_by:
- lor-sgl4
implementers: []
ready:
  by: conversion
  at: 2026-11-10T06:11:15.995564520Z
trivial: null
caused_by: null
created_at: 2026-11-10T06:11:15.995564520Z
---

## Goal

On the real Fold, after the mission's first increment is installed: how long the LoR blocks and the T3 panel take to show live data once Tailscale is switched on, and whether queued rows arrive then. The emulator cannot run Tailscale, so this is the first time the network callback meets One UI and a real tunnel.

## Done when

- [ ] An evidence receipt on this item from Sumeet's use or a session he allows: the seconds from the tunnel being up to live data in each panel, and whether the queue drained, with a failure filed as a bug linked here.
