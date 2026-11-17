---
schema: atelier.item
schema_version: 3
id: lor-5fk6
title: 'The launcher and app in the lab per direction: home, drawer, search, panels, a menu, settings and the timer'
status: building
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
implementers:
- thread:delegated-task:command%3Amcp%3A1d7328d9-468a-43e2-acf6-a7c7c564b7bf%3Adelegate-task%3A7f767896-358a-46e5-a4e7-20d71de5c647
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

## Report

### The launcher loop before Queue (2026-11-17, main at 5f869fe)

Home, the panels and the app screens are all in the module. Timed with `android/web-dev.sh` (the 4 GB, 512 MB-swap scope, unchanged), on this host with no other Gradle build or emulator running:

- **First build:** 68 s (31 s on a restart with the libraries' link cache warm).
- **A one-line edit to common code** (`home/Undo.kt`'s bar), six times: 21.7 s cold, then 15.3, 14.0, 13.9, 13.2 and 13.2 s from save to served. lor-xdoi measured 8.9 to 9.3 s for the same edit, so warm edits have grown by about 5 s since home's move, and the cold one is past xdoi's 15 s line.
- **A one-line edit to wasm-only code:** 12.8 s (`ui/Fonts.wasmJs.kt`), 9.1 to 9.9 s (`ui/LocalClock.wasmJs.kt`).
- **Memory:** the scope's peak was 3,866 MB of its 4,096 MB cap (anon about 3,300 MB of it; the rest is page cache). No swap was used, `memory.events` shows no `max` or OOM, and the log shows no `OutOfMemoryError`.
- **A bug found:** any edit to `lab/Main.kt` crashes the incremental wasm compiler (`ArrayIndexOutOfBoundsException` in `WasmIrFileMetadata.fromByteArray`, from `WasmKlibCheckers`), and every build after it fails the same way until web-dev.sh restarts. Edits to the other wasm files don't. `Main.kt` is the one file with `fun main()`.
