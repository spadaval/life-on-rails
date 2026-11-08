---
created_at: "2026-11-08T06:02:35.052190583+00:00"
id: "lor-sghw"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Text stops shimmering when it scales: TextMotion.Animated everywhere"
updated_at: "2026-11-08T06:02:35.052190583+00:00"
---

## Goal

Text in the app stops shimmering when it scales, and lays out as the browser will draw it: every text style the app sets uses `TextMotion.Animated` (decision lor-mc37). Today a pressed tile's label, a folder opening and the drawer's predictive back re-hint the text at each in-between size, and glyph widths jump.

## Done when

- [ ] Every `TextStyle(` under `android/app/src/main` sets `textMotion = TextMotion.Animated`, shown by a grep listing each with its line. On 2026-11-08 they are `Type`'s seven in `ui/Look.kt`, home's `labelStyle` in `home/HomeScreen.kt`, and the header's four in `home/Header.kt` (the line, the clock and both dates). Material's typography in `Look.kt` copies `Type`, so it follows.
- [ ] On `T3_Phone_API35` against the test server, home's first page and the drawer screenshotted before and after: no label newly cut by its ellipsis, and the header and labels at most about 1% wider. Both pairs recorded as an evidence receipt on this item.
- [ ] On the emulator with animations slowed (`settings put global animator_duration_scale 10`), before and after: a tile pressed and held, a folder opened and closed, and a predictive back on the drawer, each captured as a run of frames (`adb exec-out screencap -p` in a loop, or `screenrecord --bit-rate 20000000`). In each frame, a label's measured width divided by that frame's scale stays within 1 px of its width at rest after the change; before, the jumps are larger. The widths per frame, before and after, recorded as an evidence receipt.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`android/app/src/main/java/com/spadaval/lor/ui/Look.kt`, `home/HomeScreen.kt`'s `labelStyle` and `home/Header.kt`'s styles. Not the web editor.

## Context

- Measurements and the reasoning are on the decision. The real Fold check is the `needs-human` loose end lor-s42k after merge (sharpness at rest, no shimmer on press, folder and back).
- `HomeScreen.kt` is also changed by the open Panels branches (`lor-f3vn`, `lor-clgw`); this is a one-line change there, so whichever lands second takes the other's line.
