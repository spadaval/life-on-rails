---
created_at: "2026-11-08T03:36:23.191394525+00:00"
id: "lor-llmf"
issue_type: "spike"
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
title: "Spike: home's drawing in the browser through Compose Multiplatform wasmJs"
updated_at: "2026-11-08T03:36:23.191394525+00:00"
---

## Goal

Find out, before anything in `home/` moves, whether home's drawing runs in the browser through Compose Multiplatform's `wasmJs` target, and at what cost. Decision lor-djn2 rests on it.

## Done when

- [ ] In a throwaway worktree (not merged), a Kotlin Multiplatform module with an Android target and a `wasmJs` target holds copies of `Grid.kt`, `GridHost.kt`, `Inner.kt`, `HomeLook.kt` and `Layout.kt`, plus a stack drawing a clock and an apps block with placeholder icons, in the app's Saira fonts (`ui/Look.kt`, `res/font/`) carried by the module. In the copies, parsing moves from `org.json` to kotlinx.serialization, `Layout.kt`'s `java.io` and `java.time` are replaced or moved behind the platform, `GridHost`'s `Log` and its use of `HomeUi` are replaced, and the note lists each such change, since the split will make them for real. Its browser build draws a version 2 layout read from a JSON file, on a canvas inside a plain HTML page served by a static server, and moves between cells by mouse drag.
- [ ] The same module, as the app's dependency, builds with `assembleDebug` on boat, with the app's composables drawing from it, so the Android side is shown to work, not assumed.
- [ ] The note says which Compose Multiplatform and Kotlin versions, and which Multiplatform plugin setup for the Android side, work with the app's build (AndroidX Compose BOM `2026.09.00`, Compose compiler plugin 2.2.21, Android Gradle Plugin 9.2.1 with its built-in Kotlin, Gradle 9.4.1), and what the app would have to change (if anything) to use them.
- [ ] Measured and noted: the clean and the incremental wasm build time on boat, and the bundle's size raw and gzipped.
- [ ] The same layout drawn by the emulator (`T3_Phone_API35`, the spike's build) and by the browser at the same pixel size, overlaid: the largest offset of a tile, label or panel edge, in pixels, with both screenshots and the overlay as an evidence receipt. Both draw in the app's Saira.
- [ ] The note says how a host page passes JSON and images in and gets an edit back (Kotlin/JS interop: `@JsExport` or `external` functions), shown by the page loading the layout through that call.
- [ ] A decision record on this mission settles how the built bundle reaches `pnpm build` and the deploy (committed to the repository, fetched from boat at deploy, or built by `pnpm build` on boat), with the measurements as its reasons, and says how the spec's settled Stack (TanStack Start, React 19, Tailwind) changes for it.

## Scope

A throwaway worktree, a boat VM for the Gradle builds, the emulator. No change to `main` except this item's record, the evidence receipt and the decision record. Not the real split (a later increment), not icons, wallpaper or widgets.

## Context

- Decision lor-djn2 (Kotlin Multiplatform) and the strategy's Risks name what this spike answers.
- Builds go to boat (the `boat` skill and `android/boat-build.sh`); the emulator is local, one at a time.
- If the overlay is off by more than 2 px, or the build can't share the composables with the app, say so plainly with the numbers: the strategy's Authority sends that choice back to Sumeet.
