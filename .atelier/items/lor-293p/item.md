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
- [ ] `node scripts/lab/run.mjs <script> --version <v> --state <s> --out <dir>` plays a gesture script and writes a WebM recording, a frame strip PNG (every 50 ms across the gesture) and the final screenshot. The runner is proven here on a gesture test surface registered in the lab for this purpose (a pager that follows the finger, a scrolling list, a tile that opens a menu on long press and can be dragged, a two-pane layout that folds), with one script per helper. Scripts for real surfaces are written by the item that brings the surface into the lab: lor-xdoi adds the drawer, paging, app menu, drag, long press into drag, search and folder back; lor-rgx4 adds approving a card, answering a question and the timer; and each concept adds its own.
- [ ] A run of one script takes under 10 seconds once the lab is built.

## Scope

`scripts/lab/`, the lab's toolbar. Emulator gestures stay with T3 Code's device tools.

## Context

- Compose for the web draws to one canvas, so scripts address positions in dp or a fixture's named anchors, not DOM nodes.
