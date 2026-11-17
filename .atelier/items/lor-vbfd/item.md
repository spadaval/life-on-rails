---
schema: atelier.item
schema_version: 3
id: lor-vbfd
title: 'The launcher lab: the app''s Compose code running in a browser phone frame, with version and state in the URL'
status: draft
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-3hnk
- lor-sghw
- lor-nde9
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.903336399Z
---

## Goal

A page in the browser runs Compose code from a shared Kotlin module inside a phone frame, rebuilt on this machine within seconds of a change. It is where the launcher and app are designed: the current screens and every concept are entries in one version dropdown.

## Done when

- [ ] `android/homecore` is a Kotlin Multiplatform library (Android and `wasmJs`) on Kotlin 2.3.20 and Compose Multiplatform, holding `ui/Look.kt`, `ui/Palette.kt`, `ui/Press.kt` and the Saira fonts; the app builds from it with no visible change (boat: `assembleDebug testDebugUnitTest` pass, and the test count before and after is in the Report).
- [ ] The lab page (`homecore`'s `wasmJsMain`) draws a phone frame at the Fold's cover size and its inner size with a fold switch, a wallpaper picker with at least a dark, a pale and a mixed image, a Version dropdown and a State dropdown. Version and state are in the URL hash (`#current/home-full`), so a link opens one exact screen.
- [ ] A version is a registered composable tree in common code; `current` is the first, and adding one is a single registration. A state is a named fixture (lor-oech fills the list). Until screens move in, `current` shows a kit sheet: every colour, type step, the chamfered panel, the selection, the press scales and the motion tokens.
- [ ] `android/web-dev.sh` runs the continuous development build as decision lor-0v68 allows: a `systemd-run --user --scope` capped at `MemoryMax=4G` and `MemorySwapMax=512M`, Gradle `-Xmx1g`, the Kotlin daemon at `-Xmx1536m -Dlor.web=1`, two workers, incremental wasm on, refusing to start beside an emulator here. After a one-line edit to a composable the page shows the change in at most 15 seconds; the measured time and peak memory are in the Report. If it cannot be brought under 15 seconds, the item says what was tried and the lead takes the number to Sumeet.
- [ ] `android/settings.gradle.kts` lets the wasm build fetch Node.js while keeping `FAIL_ON_PROJECT_REPOS`; `android/boat-build.sh` also builds and returns the wasm distribution.
- [ ] `scripts/shots.mjs`, which lor-nde9 wrote, gains `--lab`: it captures version by state by screen size into the same gallery as the dashboard's.

## Scope

`android/homecore` (new), the three `ui/` files moving, the app's build files, `android/web-dev.sh`, `android/boat-build.sh`, `scripts/shots.mjs`. Not home's drawing (lor-xdoi), not the panels (lor-rgx4), not the settings and app screens (lor-t8lq).

## Context

- Spike lor-llmf (branch `spike/kmp-browser`, `cb91b53`, worktree `/home/supadava/projects/lor-kmp-spike`) is the reference: Compose Multiplatform 1.12.2, `material3` 1.9.0, AGP 9.2.1; its build settings came from spike lor-8a7n.
- This item takes the module's creation and the three `ui/` files out of lor-xdoi, which then moves home's drawing into a module that exists.
- The lab is a design tool. It is not the dashboard's real-size page (lor-kaq8), and its bundle is a development build that never ships in `pnpm build`.

## Unknowns

- How long the first cold build takes under the cap, and whether Gradle's continuous mode or a watch script gives the shorter loop.
