---
schema: atelier.item
schema_version: 3
id: lor-4v7x
title: Whether the split designs for a test page as well as the editor
status: ready
mission: null
labels:
- launcher
- needs-human
- decision
priority: medium
blocked_by: []
implementers: []
ready:
  by: conversion
  at: 2026-11-08T17:49:55.993759582Z
trivial: null
caused_by: null
created_at: 2026-11-08T17:49:55.993759582Z
---

## Goal

Should the split (lor-xdoi) design for a test page now: platform fakes usable outside the editor, and a small scriptable API in the browser build (load a layout, set the clock, take a screenshot)?

## Done when

- [ ] The question is answered and the answer is written in this item's Report.

## Context

- The web editor draws home with the phone's own Compose code; only the platform (icons, widgets, wallpaper, launching, lifecycle) is faked. Sumeet asked (2026-11-08) whether that drawing could double as a testing tool, to iterate on UI without the emulator.
- It suits what lives in the shared module (grid, panels, drags, folders, the minimap, motion, labels, both Fold screens) and not what stays on Android (lifecycle, widget hosting, icon packs, predictive back, the blur, performance). With the build on this host (lor-8a7n) an edit shows in 12 to 14 s.

## Options

- No; build the editor only, and design a test page later if wanted.
- Yes, those two requirements in lor-xdoi; the test page itself is a later mission after the real-size view (lor-kaq8), its first item measuring the edit loop.

## Decision

Open: Sumeet's call (asked 2026-11-08).

## Links

Relationships of the old record that the new format has no field for.

- related: lor-ncpw (Edit home from LoR's web, drawn as the phone draws it)
