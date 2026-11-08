---
created_at: "2026-11-08T16:36:19.068813128+00:00"
id: "lor-3hnk"
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
title: "The app builds on Kotlin 2.3.20, ready for the shared module"
updated_at: "2026-11-08T16:36:19.068813128+00:00"
---

## Goal

The app builds with Kotlin 2.3.20, which Compose Multiplatform's browser libraries need (spike lor-llmf), with no change to what the app does. Doing it alone, before the split (lor-xdoi), keeps any compiler fallout separate from moving files.

## Done when

- [ ] Kotlin moves from 2.2.21 to 2.3.20: the Compose compiler plugin's version in `android/build.gradle.kts`, and AGP 9's built-in Kotlin pinned to the same version (how is chosen and recorded on this item). Shown by the Kotlin Gradle plugin's version in `./gradlew buildEnvironment`'s output in the boat log. Nothing else in the build changes except what 2.3.20 itself requires, each such change listed in the Report with its reason.
- [ ] Any new compiler warnings or errors in the app's code are fixed in the same change, and the Report lists them.
- [ ] `assembleDebug testDebugUnitTest` passes on boat, and the APK's size before and after is in the Report.
- [ ] On `T3_Phone_API35` against the test server, home's first page, the drawer and a folder open are pixel-identical to `main` before the change: screenshots before and after recorded as an evidence receipt.

## Scope

`android/build.gradle.kts`, `android/app/build.gradle.kts`, the Gradle wrapper if 2.3.20 needs a newer Gradle, and any app source the compiler upgrade forces. Not the shared module, and not the Node.js repository the wasm build needs (lor-xdoi).

## Context

- The spike's versions and why (`lor-llmf`'s Report): Compose's wasm klibs are built with 2.3.20 (klib ABI 2.3.0); 2.2.21 leaves every Compose import unresolved without saying why. AGP 9.2.1's built-in Kotlin is set to the same version.
- The spike's build changes are on `spike/kmp-browser` (`cb91b53`, never merged), worktree `/home/supadava/projects/lor-kmp-spike`.
- Builds go to boat (`android/boat-build.sh`).
