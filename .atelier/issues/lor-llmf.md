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
closed_at: "2026-11-08T06:02:54.085852270+00:00"
status: "superseded"
title: "Spike: home's drawing in the browser through Compose Multiplatform wasmJs"
updated_at: "2026-11-08T06:02:54.085852270+00:00"
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

## Report

Answered by a throwaway spike outside the merge path (Sumeet, 2026-11-08: "a slop spike that will never merge - disregard normal atelier process"). Worktree `/home/supadava/projects/lor-kmp-spike`, branch `spike/kmp-browser`, committed as `cb91b53` on 2026-11-08 to keep it as a reference, not to be merged. Result: Kotlin Multiplatform draws home in the browser from the phone's own code, to the pixel once text is unhinted. Decision lor-djn2 stands.

What was built: `android/homecore`, a Kotlin Multiplatform module (Android target and `wasmJs`), holding the real `Layout.kt`, `HomeLook.kt`, `Drawer.kt`, `Grid.kt`, `Inner.kt`, `GridHost.kt` (less `gridFinger`), `Follow.kt`, `Motion.kt`, `GridEdit.kt`, `HomeEdit.kt`, `ui/Look.kt` and `ui/Palette.kt`, moved out of the app. The app builds on it (`assembleDebug` on boat); `testDebugUnitTest` passes 322 of 323 (the failure is the spike's stand-in JSON reader being stricter than `org.json` on bare non-JSON text). The stack drawing (`SpikeHome.kt`) is a copy of `HomeScreen.kt`'s narrow drawing without touch or menus; splitting the real `HomeScreen.kt` is still lor-xdoi's.

Versions and build: Compose Multiplatform 1.12.2 maps to AndroidX Compose 1.12.1, the app's BOM `2026.09.00`. Kotlin must move from 2.2.21 to 2.3.20: Compose's wasm klibs are built with 2.3.20 (klib ABI 2.3.0), and 2.2.21 leaves every Compose import unresolved without saying why. AGP 9.2.1 works with `com.android.kotlin.multiplatform.library` and `kotlin { android {} }` (`androidLibrary {}` is deprecated). `settings.gradle.kts`'s `FAIL_ON_PROJECT_REPOS` blocks the Node.js download the wasm build makes; loosen it or declare the Node repository in settings.

Changes common code needs: `org.json` replaced (the spike used a stand-in reader; the split should use kotlinx.serialization or keep a reader with `org.json`'s leniency), `java.time.Instant` to `kotlin.time.Instant`, `LayoutStore` and its `java.io` stay on Android, `android.util.Log` behind an `expect` function, the app's `R.font` fonts set by the platform, `toSortedMap`, `Map.merge` and `Math.round` replaced (`Math.round` rounds half up; `kotlin.math.round` rounds half to even, so use `floor(x + 0.5)`), and `internal` declarations the app uses made public.

Pixels (1080 x 2400, `T3_Phone_API35` against Chrome at density 2.625): icons, folders, dock and panel frames at 0 px offset. Android's default text is 0.9% narrower than the browser's (header 565 px against 570 px); with `TextMotion.Animated` it is 571 px and only edge anti-aliasing differs (decision lor-mc37). Images: `docs/missions/web-layout-editor/spike/compare.png`, `textmotion.png`, `web-fold-inner.png`.

Browser host: the page passes layout JSON in (`window.lorShowLayout`) and gets the edited file back (`window.lorEdited`) through `@JsFun` functions. The phone's parser refuses with its own words (`layouts[0]: unknown field "colour"`), `GridEdit.removePanel` edits and its refusal holds ("home starts there; choose another start first"), mouse drags move between cells by the real grid rules, and at the Fold's inner size (852 x 883 dp) two half stacks share one screen by `InnerScreens` (browser only; no Fold emulator comparison).

Cost: bundle 11.4 MB raw, 4.2 MB gzipped (Skia's `.wasm` 3.3 MB gz, home's `.wasm` 0.73 MB gz, `homecore.js` 0.1 MB gz). Bundle build on boat's warm VM about 45 to 50 s, 1 min 48 s the first time. In the browser, about 110 ms from loaded to drawn; 0.55 to 0.7 s from page load locally. Delivery: decision lor-kbog.
