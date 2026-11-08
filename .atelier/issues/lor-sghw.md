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

- [ ] `Type`'s styles (`ui/Look.kt`) and home's `labelStyle` (`home/HomeScreen.kt`) carry `textMotion = TextMotion.Animated`, and no text style elsewhere in `android/` sets `TextMotion.Static` (grep).
- [ ] On `T3_Phone_API35` against the test server, home's first page and the drawer screenshotted before and after: no label newly cut by its ellipsis, and the header and labels at most about 1% wider. Both pairs recorded as an evidence receipt on this item.
- [ ] On the emulator, a screen recording (`adb shell screenrecord`) of a tile pressed and held, a folder opened and closed, and a predictive back on the drawer, before and after, frames compared at 2x: no glyph changes width between frames after the change. Recorded as an evidence receipt.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`android/app/src/main/java/com/spadaval/lor/ui/Look.kt` and `home/HomeScreen.kt`'s `labelStyle`, plus any other `TextStyle` built outside `Type` that home draws. Not the web editor.

## Context

- Measurements and the reasoning are on the decision. The real phone and Fold check is a `needs-human` loose end after merge (sharpness at rest, no shimmer on press, folder and back).
- `HomeScreen.kt` is also changed by the open Panels branches (`lor-f3vn`, `lor-clgw`); this is a one-line change there, so whichever lands second takes the other's line.
