---
schema: atelier.item
schema_version: 3
id: lor-5fk6
title: 'The launcher and app in the lab per direction: home, drawer, search, panels, a menu, settings and the timer'
status: ready
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-npa6
- lor-293p
- lor-zwfd
- lor-oech
- lor-xdoi
- lor-rgx4
- lor-t8lq
implementers: []
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.141851947Z
---

## Goal

Sumeet can use each direction's phone in the browser: swipe to the drawer, search, approve a card, start a timer, fold and unfold, and flip between directions with the Version dropdown.

## Done when

- [ ] Each direction from the brief is a registered version in the lab, drawn in Compose in `homecore`'s `wasmJsMain` (so it never ships in the app), from the same fixtures as `current`. It starts once home, the panels and the settings and app screens are in the lab and the launcher loop has been timed with them there, so each concept is built beside the real thing and on a loop known to be fast enough.
- [ ] The first direction is built here, with whatever shared pieces a direction needs to be quick to write (a navigation shell that follows the finger, a sheet, a fixture-backed store for taps). Each further direction is one item, added by the lead.
- [ ] A direction's Done when is the brief's required set on the cover and inner screens and the brief's phone interactions working by touch and by `scripts/lab/gesture.mjs`, with recordings of the drawer swipe and one drag, and `shots/lab.json` gaining its states, captured on a dark, a pale and a mixed wallpaper.
- [ ] `scripts/lab/publish.sh` puts the build under `/lab/phone/`, the lab's first load over Tailscale is measured in the Report, and on Sumeet's Fold the page fills the screen it is opened on.

## Scope

`android/homecore/src/wasmJsMain/.../concepts/` and `shots/lab.json`. The app's own screens are not changed.

## Context

- Concept code is rough on purpose, as the dashboard prototype's is. What is worth keeping moves to common code when a direction is chosen, in a later mission.
