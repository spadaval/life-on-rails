---
schema: atelier.item
schema_version: 3
id: lor-293p
title: 'Gestures by script in the lab: swipe, fling, long press, drag and fold, with a recording'
status: draft
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-vbfd
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.965574679Z
---

## Goal

Motion and touch can be tried and replayed in the browser: a swipe that follows the finger, a long press that becomes a drag, a fold in the middle of a gesture. A person uses the mouse or a touch screen; an agent uses a script and gets a recording to look at.

## Done when

- [ ] In the lab, mouse and touch both drive Compose's pointer input: a drag is a swipe, a held press is a long press, a wheel scrolls. A toolbar gives Home, Back, Fold and Unfold, rotate, and a slow-motion switch (animation scale 1, 0.25, 0); the same are keys.
- [ ] `scripts/lab/gesture.mjs` exports `tap`, `doubleTap`, `longPress`, `swipe` (direction, distance in dp, duration), `fling` (speed in dp/s), `drag` (path, holds in ms), `fold` and `back`, sending real touch events to the page through Chrome's protocol, with distances in the phone frame's dp.
- [ ] `node scripts/lab/run.mjs <script> --version <v> --state <s> --out <dir>` plays a gesture script and writes a WebM recording, a frame strip PNG (every 50 ms across the gesture) and the final screenshot. Ten ready scripts cover: open and close the drawer, page left and right, open an app menu, pick up and drop an icon onto another, a long press drifting into a drag, search and open, approve a card, start and stop a timer, fold and unfold on a half panel, predictive back out of a folder.
- [ ] A run of one script takes under 10 seconds once the lab is built.

## Scope

`scripts/lab/`, the lab's toolbar. Emulator gestures stay with T3 Code's device tools.

## Context

- Compose for the web draws to one canvas, so scripts address positions in dp or a fixture's named anchors, not DOM nodes.
