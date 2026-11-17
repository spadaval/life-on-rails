---
schema: atelier.item
schema_version: 3
id: lor-vbfd
title: 'The launcher lab: the app''s Compose code running in a browser phone frame, with version and state in the URL'
status: approved
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
implementers:
- thread:delegated-task:command%3Amcp%3Ad505dd17-c64d-476e-8d30-f41010a20b4c%3Adelegate-task%3A724e1778-90c2-43fc-8e37-f33b7945a559
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.903336399Z
---

## Goal

A page in the browser runs Compose code from a shared Kotlin module inside a phone frame, rebuilt on this machine within seconds of a change. It is where the launcher and app are designed: the current screens and every concept are entries in one version dropdown.

## Done when

- [x] `android/homecore` is a Kotlin Multiplatform library (Android and `wasmJs`) on Kotlin 2.3.20 and Compose Multiplatform, holding `ui/Look.kt`, `ui/Palette.kt`, `ui/Press.kt` and the Saira fonts; the app builds from it with no visible change (boat: `assembleDebug testDebugUnitTest` pass, and the test count before and after is in the Report).
- [x] The lab page (`homecore`'s `wasmJsMain`) draws a phone frame at the Fold's cover size and its inner size with a fold switch, a wallpaper picker with at least a dark, a pale and a mixed image, a Version dropdown and a State dropdown. Version, state, screen and wallpaper are all in the URL hash (`#current/home-full/inner/pale`; screen is `cover` or `inner`, and the last two may be left off for the cover screen on the dark wallpaper), so a link opens one exact screen and a catalogue entry is only a name and a link.
- [x] A version is a registered composable tree in common code; `current` is the first, and adding one is a single registration. A state is a named fixture (lor-oech fills the list). Until screens move in, `current` shows a kit sheet: every colour, type step, the chamfered panel, the selection, the press scales and the motion tokens.
- [x] `android/web-dev.sh` runs the continuous development build as decision lor-0v68 allows: a `systemd-run --user --scope` capped at `MemoryMax=4G` and `MemorySwapMax=512M`, Gradle `-Xmx1g`, the Kotlin daemon at `-Xmx1536m -Dlor.web=1`, two workers, incremental wasm on, refusing to start beside an emulator here. After a one-line edit to a composable the page shows the change in at most 15 seconds; the measured time and peak memory are in the Report. If it cannot be brought under 15 seconds, the item says what was tried and the lead takes the number to Sumeet.
- [x] `android/settings.gradle.kts` lets the wasm build fetch Node.js while keeping `FAIL_ON_PROJECT_REPOS`; `android/boat-build.sh` also builds and returns the wasm distribution.
- [x] `shots/lab.json`, a catalogue for lor-nde9's `scripts/shots.mjs` (which this item does not edit), captures version by state by screen size into the same gallery as the dashboard's.
- [x] The lab reads fixtures only: nothing in `homecore`'s browser build calls LoR or T3.

## Scope

`android/homecore` (new), the three `ui/` files moving, the app's build files, `android/web-dev.sh`, `android/boat-build.sh`, `shots/lab.json`. Not home's drawing (lor-xdoi), not the panels (lor-rgx4), not the settings and app screens (lor-t8lq).

## Context

- Spike lor-llmf (branch `spike/kmp-browser`, `cb91b53`, worktree `/home/supadava/projects/lor-kmp-spike`) is the reference: Compose Multiplatform 1.12.2, `material3` 1.9.0, AGP 9.2.1; its build settings came from spike lor-8a7n.
- This item takes the module's creation and the three `ui/` files out of lor-xdoi, which then moves home's drawing into a module that exists.
- The lab is a design tool. It is not the dashboard's real-size page (lor-kaq8), and its bundle is a development build that never ships in `pnpm build`.

## Unknowns

- How long the first cold build takes under the cap, and whether Gradle's continuous mode or a watch script gives the shorter loop.

## Report

The lab runs here: `android/web-dev.sh` serves it at `http://127.0.0.1:3860/`, and a link such as `#current/home-full/inner/pale` opens one exact screen. A one-line edit to a composable reaches the open page in 11.2 and 11.4 s for the first edit after a start (two runs), and in 7.7 to 8.5 s after that. Peak memory is 3.2 to 3.7 GB of the 4 GB cap, on a lab that holds only the kit sheet. The lead should watch that headroom as home, the panels and the app screens move in, since the mission measures the loop again then.

Commits:
- 6a8a9d2: the module, and the look moved into it with no visible change.
- 5544296: the lab, `web-dev.sh`, the `boat-build.sh` change and `shots/lab.json`.
- 14f2db1: `web-dev.sh` starts warm (see "The loop").
- 4d19d7a: AGENTS.md. This is outside the item's Scope, so it is its own commit. The old lines named `boat-build.sh`'s former default tasks and listed Node tests as runnable here.

6a8a9d2 was not built on its own; the builds below ran on the combined tree.

Evidence is in `/tmp/lor-verify/lor-vbfd/`: `launcher/`, `lab/` and `build/`. `atelier evidence record`, which the verify skill describes, is not a command in this Atelier, so I name the files here.

**The module.** `android/homecore` is a Kotlin Multiplatform library for Android and wasmJs, on Kotlin 2.3.20, Compose Multiplatform 1.12.2 and material3 1.9.0, as in spike lor-llmf. It holds `ui/Look.kt`, `ui/Palette.kt`, `ui/Press.kt` and the three Saira files, which move to `androidMain/res/font` through AGP's `androidResources { enable = true }`. The licence stays in the app's `assets/fonts/OFL.txt`, so the APK still ships it.
- `Saira` and `SairaCondensed` are `expect val`s. Android builds them from `com.spadaval.lor.homecore.R.font`. The browser builds them from the same files, which `wasmJsProcessResources` copies into the bundle's `fonts/`.
- On wasm, reading a font before the page has loaded it throws. Otherwise `Type`'s styles would capture the default font without any error.
- `Palette.over` used `Math.round`, which is JVM only. It now uses `floor(x + 0.5)`, which is how `Math.round` is defined for doubles.
- `LookTest` stays in `:app`, so the two test counts compare.

**No visible change.** The before APK was built from a43b9e5 (main at ec677a2), and the after APK from the tree of 5544296. Their sha256s are in `build/apks.sha256`.
- On boat, `assembleDebug testDebugUnitTest` passed both times. The app's unit tests were 422 before and 422 after, with no failures. homecore adds 8 tests (`:homecore:testAndroidHostTest`), all passing. The logs are `build/boat-before.log` and `build/boat-after.log`.
- `:app:dependencies --configuration debugRuntimeClasspath` before and after (`build/before-resolved.txt`, `build/after-resolved.txt`): no `androidx.*` module changed version (Compose 1.12.1, material3 1.4.0). What it adds is JetBrains' Android modules over those same androidx libraries (`org.jetbrains.compose.*` 1.12.2, material3 1.9.0, lifecycle 2.9.6, savedstate 1.3.6).
- The APK went from 43,249,665 to 43,348,087 bytes (+98 KB). It holds the same three `res/font/*.ttf` files at the same sizes, and `assets/fonts/OFL.txt`.
- On `T3_Phone_API35` on the Mac I installed each APK in turn and screenshotted Settings (Saira body and Saira Condensed titles) and Timer. Decoded to pixels, each before-and-after pair differs only in rows 50 to 77, the status bar clock (`launcher/`).

**The lab page** is homecore's `wasmJsMain`.
- It draws a frame at the Fold 7's real screen sizes in dp: the cover at 480x1120 and the inner at 875x971. I read these from Sumeet's SM-F966U1 over adb: 1080x2520 and 1968x2184 px, both at the override density of 360.
- It has a Fold/Unfold switch, Version and State dropdowns, and a wallpaper picker. The three wallpapers are dark, pale and mixed (a bright sky over dark ground), drawn by `homecore/wallpapers/make.py`.
- The URL hash is the only state: `#version/state[/screen[/wallpaper]]`. Only trailing defaults are left off, so `#current/home-full` means the cover screen on the dark wallpaper. The controls only write the hash.
- In a headless Chrome I clicked Fold, picked a state and a wallpaper, clicked Fold again, and went Back. After each step the hash, the controls, the frame's width and the drawn hash agreed, with no page errors (`lab/controls.mjs`, `lab/clicked.png`).
- `#concept-x/home-full` shows "No version "concept-x"" in red and draws the default (`lab/unknown.png`), so a mistyped link says so.
- Once a hash has drawn, the body's `data-drawn` attribute holds it. The catalogue waits on that.
- ComposeViewport resizes its canvas on the window's resize, not its container's. A fold switch therefore fires a window `resize`; without it, the canvas stayed at the old size and stretched.

**Versions and states.** `lab/Lab.kt` holds `Lab.versions`, a list of `Version(name, about) { scene -> ... }`. `current` is first, and a concept is one more line in that list. `lab/Fixtures.kt` holds the named states `home-full` and `first-run`. A fixture is only a name and a note for now; lor-oech gives it its data. `current` draws `KitSheet`:
- every Palette colour with its hex, and the alpha ones (state fill, scrim, veil, folder glass, folder dim) over the wallpaper;
- the seven type steps;
- `framedPanel()` on Panel Face and on Raised, and a `selection()` row;
- `PressScale.Tile` and `PressScale.Row`, each at rest, held at its pressed scale, and as a live button;
- the motion tokens: `quick` and `move` with their easing curves, and `settle`'s spring values.

**The loop** (`android/web-dev.sh`): `systemd-run --user --scope` in `agents.slice` with `MemoryMax=4G` and `MemorySwapMax=512M`, Gradle `-Xmx1g`, the Kotlin daemon at `-Xmx1536m -Dlor.web=1`, `--max-workers=2`, `kotlin.incremental.wasm=true`, and `wasmJsBrowserDevelopmentRun --continuous`.
- It refuses with exit 3 in four cases, each tried here:
  - a `qemu-system` process is running (tried with a stand-in process of that name);
  - `lor-web-dev.scope` is already running (tried with a stand-in scope);
  - the port belongs to lor-web, the test servers or the dashboard prototype (3847 to 3849, 3857 to 3859);
  - the port is taken. This one fired for real: lor-5uxk's prototype holds 3859, which is why the lab moved to 3860.
- `--dry-run` prints the command.
- On Ctrl-C it stops the scope, Kotlin daemon included, and prints the peak memory.
- The dev server binds 127.0.0.1. `webpack.config.d/devserver.js` drops the Gradle project folder that Kotlin adds to what the server serves. Before that change `local.properties` was served; after it, it returns 404.

Each time below runs from the file write until the open page had reloaded and drawn again. The probe changed one `Text` in `KitSheet`, a headless Chrome watched the page, and the file was put back afterwards (`lab/loop.mjs`).

| Start | First edit | Later edits | Peak memory |
|---|---|---|---|
| Outputs up to date (runs 1 and 2) | 17.7, 17.1 s | 3.2 to 10.7 s | 3,348 to 3,697 MB, swap at most 392 MB |
| Up to date, warm-up compile in a separate Gradle run (runs 3 and 4) | 15.8, 16.1 s | 7.8 to 10.3 s | 3,429 to 3,466 MB |
| Up to date, plain script (run 5, timestamped) | 14.9 s | 7.5 to 8.6 s | 3,436 MB |
| `homecore/build` deleted (cold) | 9.9 s | 7.5, 7.9 s | 3,405 MB (2,838 MB at served) |
| Committed script: homecore's own outputs removed at start (two runs) | 11.2, 11.4 s | 7.7 to 8.5 s | 3,235, 3,247 MB, no swap |

- Where the time goes, from the timestamped log (`lab/web-dev-timestamped.log`): Gradle notices the change in 0.3 s. The compile takes 2.3 to 6 s and the incremental development link 4.7 to 7.3 s. Webpack and the reload take about 0.3 s.
- Why the first edit was slow: when everything is up to date at start, the continuous Gradle process compiles nothing. The first edit then meets that process cold.
- A warm-up compile in a separate Gradle run warmed only the Kotlin daemon, not the continuous process, and didn't help.
- A cold start, where that same process compiles and links, gave a 9.9 s first edit. So the committed `web-dev.sh` removes homecore's own compiled code and development link before starting, and keeps the link's 111 MB cache of the libraries.
- Start to served: 22.4 and 25.5 s with the committed script. From a deleted `homecore/build` it took 47.8 s. That run did not need to download dependencies, since they were cached.
- The item's Unknowns:
  - The cold start under the cap is above.
  - Only Gradle's continuous mode was measured, not a watch script.
  - The configuration cache, which Gradle's log suggests, was not tried.

**Node.js under `FAIL_ON_PROJECT_REPOS`.** `settings.gradle.kts` declares ivy repositories for Node.js (nodejs.org), Yarn and Binaryen (GitHub releases), using the patterns read from kotlin-gradle-plugin 2.3.20's `*SetupTask` classes. Each is limited to its one module. The root build sets each wasm env spec's `downloadBaseUrl` to null, so the plugin adds no project repositories.
- Proof: I moved this host's cached Node and Yarn (`~/.gradle/nodejs`, `~/.gradle/yarn`, and their module-cache entries) aside and built. The log shows `Downloading https://nodejs.org/dist/v25.0.0/...` and `https://github.com/yarnpkg/yarn/releases/download/v1.22.17/...`, and the build succeeded.
- Binaryen is only fetched for a production bundle, which nothing here builds, so its repository is unproven.
- `android/kotlin-js-store/yarn.lock` is committed.

**`boat-build.sh`** now also runs `:homecore:testAndroidHostTest` and `:homecore:wasmJsBrowserDevelopmentExecutableDistribution` by default. It keeps homecore's and the root's build folders between runs, and returns `homecore/test-results/` and `lab/` (the bundle) beside the APK and the app's reports. The run above returned all of them.

**Shots.** `shots/lab.json` has `current` × `home-full`, `first-run` × cover, inner on the dark wallpaper. The base is `:3860`, there is one 1000 px width (the hash sets the screen size), and it waits on `data-drawn === location.hash`.
- `node scripts/shots.mjs --catalogue shots/lab.json` wrote 4 images in 2.5 s with no problems (copies in `lab/`). `tests/shots.test.ts` passes.
- `shots.mjs` writes one gallery per catalogue, so "the same gallery as the dashboard's" is met as a sibling: `~/.local/state/lor-shots/lab/` beside `.../dashboard/`. `scripts/lab/publish.sh` copies that whole folder to `/lab/shots/`. The lead should confirm that reading.

**Fixtures only.** The page fetches only `fonts/*.ttf`, `wallpapers/*.png` and its own scripts, all by relative paths. A headless Chrome load listed every request: with the kit's text, none left the page's folder.
- Served under lor-web's own `/lab/` policy (`labPolicy` from `scripts/lab/serve.mjs`, by a stand-in server at `/lab/launcher/`), the page drew with no console errors and no request outside `/lab/` (`lab/csp.png`, `lab/csp-check.mjs`).
- One trap: a glyph Saira lacks (the kit first had "◆") makes Compose fetch a Noto fallback from fonts.gstatic.com. I replaced the glyph. Under `/lab/`, `connect-src` would block that request anyway.

**For the reviewer and the lead**
- The emulator: `T3_Fold_API35` had been running on this host for six days, which blocks this build under lor-0v68. The lead stopped it at my request (it was left over from 2026-11-11).
- `wasmJsNodeTest` cannot run homecore's tests: Compose's Skia (`skiko.mjs`) fails to load under Node. The common tests therefore run on the JVM as Android host tests on boat. The init.d guard still allows `wasmJsNodeTest`, which is harmless.
- `tsc --noEmit` in this worktree reports errors in `src/routes/tasks.tsx` and `src/routes/trmnl/$.ts`. This item does not touch them. I did not run `pnpm`; I symlinked `node_modules` from the main checkout, as other worktrees do, to run `scripts/shots.mjs` and vitest.
- The verify skill's Evidence section names `atelier evidence record`, which this Atelier does not have. That is worth a backlog item.

### Responses

- 1-7fl5.1: fixed in b5e1cd7: the five alpha samples sit straight on the wallpaper, with only their labels on a panel. `/tmp/lor-verify/lor-vbfd/round2/glazes-{dark,pale,mixed}.png` show them change with the wallpaper.
- 1-7fl5.2: fixed in b5e1cd7: the marker is keyed on the hash as written. In `round2.mjs`, `#current/home-full` → `#current/home-full/cover/dark` → `#concept-x/home-full` each set `data-drawn` to the new hash.
