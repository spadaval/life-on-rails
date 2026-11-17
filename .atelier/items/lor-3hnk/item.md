---
schema: atelier.item
schema_version: 3
id: lor-3hnk
title: The app builds on Kotlin 2.3.20, ready for the shared module
status: in_review
mission: lor-ncpw
labels:
- launcher
- feature
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A6d975590-ea31-4811-8a80-d5126f0e49e2
ready:
  by: conversion
  at: 2026-11-08T16:36:19.068813128Z
trivial: null
caused_by: null
created_at: 2026-11-08T16:36:19.068813128Z
---

## Goal

The app builds with Kotlin 2.3.20, which Compose Multiplatform's browser libraries need (spike lor-llmf), with no change to what the app does. Doing it alone, before the split (lor-xdoi), keeps any compiler fallout separate from moving files.

## Done when

- [x] Kotlin moves from 2.2.21 to 2.3.20: the Compose compiler plugin's version in `android/build.gradle.kts`, and AGP 9's built-in Kotlin pinned to the same version (how is chosen and recorded on this item). Shown by the Kotlin Gradle plugin's version in `./gradlew buildEnvironment`'s output in the boat log. Nothing else in the build changes except what 2.3.20 itself requires, each such change listed in the Report with its reason.
- [x] Any new compiler warnings or errors in the app's code are fixed in the same change, and the Report lists them.
- [x] `assembleDebug testDebugUnitTest` passes on boat, and the APK's size before and after is in the Report.
- [x] On `T3_Phone_API35` against the test server, home's first page, the drawer and a folder open are pixel-identical to `main` before the change: screenshots before and after recorded as an evidence receipt.

## Scope

`android/build.gradle.kts`, `android/app/build.gradle.kts`, the Gradle wrapper if 2.3.20 needs a newer Gradle, and any app source the compiler upgrade forces. Not the shared module, and not the Node.js repository the wasm build needs (lor-xdoi).

## Context

- The spike's versions and why (`lor-llmf`'s Report): Compose's wasm klibs are built with 2.3.20 (klib ABI 2.3.0); 2.2.21 leaves every Compose import unresolved without saying why. AGP 9.2.1's built-in Kotlin is set to the same version.
- The spike's build changes are on `spike/kmp-browser` (`cb91b53`, never merged), worktree `/home/supadava/projects/lor-kmp-spike`.
- Builds go to boat (`android/boat-build.sh`).

## Report

The app builds and its 422 unit tests pass on Kotlin 2.3.20, and home, a folder and the drawer draw exactly as before. The build change is one literal.

**How Kotlin is pinned.** `android/build.gradle.kts` sets the Compose compiler plugin to `2.3.20`, and that one version also pins AGP 9.2.1's built-in Kotlin. AGP asks for `kotlin-gradle-plugin:2.2.10`. `compose-compiler-gradle-plugin` depends on `kotlin-gradle-plugin` at its own version, through the `kotlin-gradle-plugins-bom`, and Gradle takes the higher one. `./gradlew buildEnvironment` on boat:

- before: `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 -> 2.2.21`
- after: `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 -> 2.3.20`

So 2.2.21 was already pinned this way, and one literal keeps both versions in step. I did not add an explicit `kotlin-gradle-plugin:2.3.20` on the root buildscript classpath. It would be a second literal to keep equal to the first, and it would change nothing that resolves. If AGP's own request ever rises above the Compose plugin's version, Gradle takes AGP's, and `buildEnvironment` shows it.

**What moved with it** (what 2.3.20 itself requires; no other build file changed, and Gradle 9.4.1 is unchanged):

- The APK's `kotlin-stdlib` follows the Kotlin Gradle plugin, as AGP's built-in Kotlin adds the matching one. Its dex now has 2.3's new classes, such as `kotlin/IntroducedAt` and `kotlin/coroutines/jvm/internal/TailCallBaseContinuationImpl`.
- The other library dexes are the same size. `kotlinx-coroutines` stays at 1.9.0.

**APK size** (`app-debug.apk`): 43,200,217 bytes before, 43,249,665 after (+49,448, +0.11%). Most of the growth is the stdlib dex (`classes14.dex` +51,772). The app's own dexes change by a few hundred bytes each.

**New compiler warnings, all fixed** (b78a05f). Kotlin 2.3's smart casts made a cast or `!!` redundant in 12 places:

- `home/DrawerScreen.kt:226`: No cast needed (second `as SearchShortcut` in the same lambda).
- `quicksend/QuickSendActivity.kt:189`: Unnecessary safe call. `capture?.appLabel` inside `jpeg?.let`; `jpeg` comes from `capture?.jpeg`, so `capture` is known non-null.
- `HomeEditTest.kt:227, 232, 290, 293, 392`: Unnecessary `!!` on `x.at` after `x.at!!` in the same call.
- `SearchTest.kt:185` (twice): No cast needed (`entries()` already returns `Found<String>`).
- `DeviceHomeTest.kt:29`: Unnecessary `!!` on `home.one` after `home.one!!` on the line before.
- `CommandsTest.kt:82`: Unnecessary `!!` on `reply.error`. `CommandsTest.kt:163`: No cast needed on `reply.value` after the cast on line 160.

After the fixes, a `clean` build's warnings are identical to the base's 13. All 13 are older: 1 deprecation in `AppUsageCollector.kt`, and 12 coroutine opt-ins in `ThreadReadsTest.kt`. No errors.

**Build on boat:** `clean buildEnvironment assembleDebug testDebugUnitTest` succeeded, with 422 tests, 0 failures and 0 skipped (the same 422 as the base). Both Kotlin compile tasks ran in both builds, so the warning comparison covers every file.

**Pixel check** (evidence: `/tmp/lor-verify/lor-3hnk/`, with `README.txt`)

- **Setup.** `T3_Phone_API35` on the Mac, `server_url` `http://10.0.2.2:3857`. A layout with a folder "Tools" (YouTube, Settings) was written to `files/layout.json` for every run, then the original was restored. LoR's `HomeActivity` was opened directly, as the emulator's default home is the Pixel launcher.
- **Screens shot:** home's first page (with its set-up card), the folder open, the drawer's Quick dial tab, and its All tab. Each was shot with the base APK (6dcaa4a, the branch base with no code change), with the same APK again (`before2/`, to measure noise), and with b78a05f's APK (`after/`).
- **Result.** `diff.py` compares every pixel. Every differing pixel is inside the system status bar (y 47–79): the clock's digits, and on the All tab the signal-strength icon (x 933–965). Base against base shows the same kind of noise. Below the status bar, every screen is identical, and the accessibility snapshot text is identical too.
- The build logs are beside the shots (`before/boat-build.log`, `after/boat-build.log`).

**Found along the way, not fixed here (out of scope):**

- `android/boat-build.sh` exits 1 with no message when the boat VM is stopped. Its `boat scp` output goes to `/dev/null` under `set -e`. Worth an item.
- The verify skill tells authors to run `atelier evidence record`, which this Atelier does not have.

