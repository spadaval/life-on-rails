---
created_at: "2026-11-08T03:36:23.255170294+00:00"
id: "lor-xdoi"
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
    id: "lor-clgw"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-f3vn"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-llmf"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
  - kind: "issue"
    id: "lor-sbth"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "One home, two platforms: home's drawing in a shared Kotlin module"
updated_at: "2026-11-08T03:36:23.255170294+00:00"
---

## Goal

Home's drawing, its parser and its edits live in one Kotlin Multiplatform module that the Android app builds from and the browser can build from, with no change to home on the phone. What only Android has (icons and icon packs, widget hosting, the wallpaper, launching, the store's files) is passed in by the app.

## Done when

Provisional, to be made concrete once the spike and Panels have landed:
- [ ] The module builds for Android and `wasmJs` on boat; the app uses it; `testDebugUnitTest`, moved where the code moved, passes.
- [ ] Home on `T3_Phone_API35` and `T3_Fold_API35` looks and behaves as before: screenshots of each cell before and after, pixel-identical, and the home press still instant (cold and warm start times before and after).
- [ ] A minimal page draws a version 2 layout from JSON with placeholder icons.

## Unknowns

- The module's boundary in `HomeScreen.kt` and how platform services are passed in; settled by the spike's findings.
- Whether the Compose Multiplatform version needs the app's Compose BOM to move.
- Waits for the spike lor-llmf and for no Panels branch on `home/` (`lor-f3vn`, `lor-sbth`, `lor-clgw`).
