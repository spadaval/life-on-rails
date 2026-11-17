---
schema: atelier.item
schema_version: 3
id: lor-rgx4
title: The agent page and the LoR panel draw from plain state in the shared module
status: building
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-oech
- lor-293p
implementers:
- thread:delegated-task:command%3Amcp%3A275230a9-0f8b-446e-b3b1-4303d9895adf%3Adelegate-task%3Aa3fbe062-265f-43a0-850d-ae83ecbc298f
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.995744340Z
---

## Goal

The two panels Sumeet called 'pretty bad overall' (lor-hybv) can be seen and changed in the lab. Their drawing takes plain state and reports taps; reading LoR and T3 stays in the app.

## Done when

- [x] The drawing of the agent page, the thread column, the pair card, the set-up card on a first-run home (`home/Setup.kt`, which draws the pair card's actions), the LoR panel, LoR's four blocks and the quick-send sheet is in `homecore`'s common code as functions of a state object and a callbacks object, with no `Context`, `LorClient`, `T3Connection` or `Prefs` in them. `Pill`, `Field`, `SectionHeader` and the state lines move to the module's kit and quick send's copies are deleted.
- [x] The app builds those state objects from what it reads today and behaves as before: on `T3_Phone_API35` and `T3_Fold_API35`'s inner screen against the test server with the demo set, each panel is screenshotted before and after and is pixel-identical (an evidence receipt).
- [x] The lab's `current` version shows both panels, the blocks and the sheet at both screen sizes in these states: a first-run home with the set-up card (the state lor-q165 names), with each of its three steps done and not done; T3 unpaired, connecting, disconnected and stale; nothing waiting; one approval, a question with options, a failed turn and two LoR flags together; twelve cards; a running timer; a thread open with a long transcript; the LoR panel's Tasks and People tabs; quick send with a screenshot.
- [x] Gesture scripts for approving a card, answering a question, starting and stopping the timer, and ticking a task in the LoR panel's Due soon run against `current` with `scripts/lab/run.mjs` (lor-293p), and their recordings are in the receipt.
- [x] The launcher loop is timed once, with home, the panels and the app screens all on main, by lor-5fk6 before it builds a concept (its first Done when line already requires that). This item hands it one finding: on main, `assembleDebug testDebugUnitTest :homecore:allTests` ran out of heap in the wasm test link beside the app's compile.
- [x] boat: `assembleDebug testDebugUnitTest :homecore:allTests` pass, with the test counts before and after in the Report.

## Scope

`agent/`, `lor/`, `quicksend/` drawing files, `home/Setup.kt`, and their state builders; fixtures for them. No change to what the panels show or how they look.

## Context

- The audit's inventory of these screens is `.atelier/missions/lor-2803/audit/launcher-panels-app.md`; the file list is under its section 1.

## Report

The agent page, its thread column and pair card, the LoR panel, LoR's four blocks, the set-up card and the quick-send sheet now draw in `homecore`'s common code from a state object and an actions object. The lab's `current` draws all of them. On the Fold emulator, every one of those surfaces draws pixel-identical before and after, on the cover and the inner screen. The timing line changed on the lead's word (under "Changed on the lead's word").

### What moved where

- The kit is `homecore/.../ui/Kit.kt`: `Pill`, `Field`, `SectionHeader`, `Failed`, `Unseen`, `Unreachable` (text, retrying, onRetry), `More`, `MenuItem`, and the text sizes as `BodyText` and `SmallText`. Quick send's own `Pill`, `Field` and `Small` are deleted. Its pill was not the same as the agent page's: it had no 48 dp touch target and was one line, and when disabled only its label dimmed while the fill stayed full amber. To keep the sheet's pixels the kit's `Pill` takes `dense = true` for it. That disabled look breaks DESIGN.md's "the same button at 45%" rule; it is kept here because this item changes no look.
- The state and actions are in `agent/PanelUi.kt`. `PanelUi` holds `LorUi` (the replies with writes held over, the read state, the writes' busy, failures and done, and the phone's clock and zone), `T3Ui` (the connection as `T3Link`, threads, the cards' calls), the notice, the chosen thread, the replies and the LoR tab. `PanelActions` holds every tap. The reads a part makes while it is shown are composable functions in `PanelActions`: a question's choices, the Start menu's intents and the thread column's messages. The app implements them with the same `remember` and `LaunchedEffect` keys as before (`agent/PanelBuild.kt`), and the lab answers them from the fixture. Home's grid reaches the panels through `InGrid`, which carries `cellInsetsPadding` and `scrollsInGrid`, so no `home/` file of lor-xdoi's is imported.
- The drawing files are `agent/AgentPageView.kt`, `ThreadView.kt` and `PairView.kt`, `lor/LorPanelView.kt` and `LorBlockView.kt`, `home/SetupView.kt` and `quicksend/SheetView.kt`. None of them uses `Context`, `LorClient`, `T3Connection` or `Prefs`.
- The pure rules moved with their tests (`commonTest`, JUnit to kotlin.test): `AgentQueue` with `T3Card`, `AgentStates`, `LorRows`, `UserInput`, `Destination`, `ThreadChoice.shown`/`others`, and `Setup.left` with `LorReach` and `SetupStep`. Three things changed in the move:
  - `Destination.segments` used `URLDecoder`. A common `percentDecoded` replaces it, with a new test against its behaviour.
  - `LorRows.ago` rounds half up with `floor(x + 0.5)`.
  - `Setup.paired(status)` stays in the app as an extension on the common `Setup`, so its test is unchanged.
- T3's shared types `ThreadState`, `PendingRequest`, `T3Thread`, `Question` and `ThreadMessage` moved to `homecore/.../t3/T3Model.kt` under the same names, so `Shell`, `Commands`, `T3Connection` and `T3DebugScreen` compile unchanged.
- The app keeps thin entry points with the same signatures: `AgentPage()`, `LorPanel()`, `LorBlock(block, grid)`, `SetupCard(modifier)` and `QuickSendActivity`'s `Sheet`. `HomeScreen.kt` is untouched. Also left in the app:
  - `AgentModel`, `CardCalls`, `Replies` and `threadReads`/`readThread`;
  - the thread chosen in the wide form, now `ChosenThread`;
  - the LoR tab, in `LorPanelState`;
  - pairing, as `pairCalls`;
  - the set-up card's checks and its show-once rule;
  - the screenshot's decode;
  - the toast;
  - `BackHandler`.
- `LorWrites` gained `doneHeld()`, and `LorWrites.TIMER` is now `LorUi.TIMER`.

### Time and dates in common code

`homecore` now depends on kotlinx-datetime 0.7.1. Its wasm build already pulled this version in through Compose, and it is `api` because the app builds `LocalDate`s. Named zones are not in its browser build without the `@js-joda/timezone` npm package, which would change the yarn lock. Instead `ui/LocalClock.kt` asks the platform for a zone's offset: java.time in the app and `Intl.DateTimeFormat` in the lab. It writes `H:mm`, `EEE d MMM` and `d MMM` with English names. On an en-GB phone, Java writes September as "Sept" in the Today header and thread dates, so after this change the header says "Sep" there. The emulators are en-US, so their shots are not affected. `AgentStatesTest` checks New York times on the JVM, and the lab draws them right (Dentist at 16:30 from 21:30Z).

### LoR's replies: a second reader, for now

The panels draw common `LorHome`, `LorTasks` and `LorPeople` (`lor/LorData.kt`). The app converts its own `DeviceHome`, `DeviceTasks` and `DevicePeople` to them, and the lab reads a fixture's JSON with `LorReplies`. The app's org.json parsers stay because lor-xdoi owns the move of `DeviceHome` and its reader. `FixtureWireTest.theAppAndTheLabReadEveryReplyTheSame` checks that both give the same result for every fixture. When lor-xdoi's common reader lands, the conversion and `LorReplies` should become that one reader.

### The lab

- `current` draws a panel when the state points at one (`lab/LabPanels.kt`, `PanelScreen`), and the kit sheet for the rest. It points at one in five ways:
  - `where.cell` at the layout's `t3` panel draws the agent page, at the `lor` panel the LoR panel, and at a stack with LoR's blocks those blocks.
  - `where.open` THREAD with a thread id as `where.target` opens that thread beside the agent page on the inner screen.
  - `where.target` "people" opens the LoR panel's People tab.
  - `where.open` QUICK_SEND draws the sheet, and `where.target` "assist" adds a stand-in screenshot.
  - A state with no layout file and set-up not done draws the set-up card.
- Taps change the drawn state as a successful call would.
- New named states, also in STATES.md: `first-run`, `agent-waiting`, `agent-t3-stale` and `thread-long`. The other required states are one-off links in `shots/lab.json`'s "Launcher lab · panels" rows.
- When lor-xdoi's grid draws home in the module, its wasm slot for these panels can call the same `LabPanels.ui` and `actions`.
- Lab anchors: `card:<thread>` with `/approve`, `/deny`, `/answer`, `/q<i>/o<j>` and `/send`; `waits:<thread>` for the thread column's copy of a card; `flag:<id>`; `tick:<task id>`; `tab:tasks` and `tab:people`; `timer-start`, `timer-pause`, `timer-stop` and `timer-untitled`. `timer-untitled` is inside the Start menu's popup, and a tap on it lands.

### Evidence

All evidence is under `/home/supadava/.cache/lor-rgx4/evidence/`. `MANIFEST.json` lists every file with its sha256. Everything was built on boat at `fae2f95` (this branch with main merged; the Report commit after it changes only this file) and served with a static server on :3871.

- `lab/`: 34 shots. Each state is drawn on the cover and the inner screen:
  - `first-run` with no step done, with two steps done, and with all three done;
  - the agent page with T3 unpaired, connecting, disconnected and stale;
  - nothing waiting;
  - `agent-waiting`;
  - twelve cards;
  - a running timer;
  - `thread-long`;
  - the LoR panel's Tasks and People tabs;
  - LoR's four blocks;
  - quick send with a screenshot.
- `gestures/{approve,answer,timer,tick}/`: each has `recording.webm`, `strip.png`, `final.png` and `run.json`, from `node scripts/lab/run.mjs scripts/lab/panels/<name>.mjs`. Each script checks its own result:
  - `approve` and `answer` check that the card is gone afterwards.
  - `timer` checks that Stop shows after Untitled starts it, and Start after Stop.
  - `tick` scrolls Due soon into view and ticks the first task, which `final.png` shows struck through.

### Tests

The before build is `main` at `3be35dc`, built with boat-build.sh's default tasks.

| Suite | Before | After |
| --- | --- | --- |
| app `testDebugUnitTest` | 433 | 400 |
| app `testReleaseUnitTest` | 433 | 400 |
| `homecore` `testAndroidHostTest` | 24 | 59 |

After main (with lor-t8lq) was merged in, at `47d839d`, the app has 403 (both build types) and `homecore` 64, none failing; the extra tests are lor-t8lq's. Before that merge, the counts were as in the table. None failed. The counts reconcile: 34 tests moved from the app to `homecore`, and each module gained one test (`FixtureWireTest`'s comparison of the two readers, and `DestinationTest`'s percent-decoding).

- `:homecore:allTests` alone passes at the item's head, with the wasm test link included (59).
- `assembleDebug testDebugUnitTest :homecore:allTests` together passes, but it ran up to date after the builds above, so it doesn't show that a cold run fits in memory. I didn't run it cold with `--rerun-tasks` because boat was at load 34 from other threads' builds; that run is left to do.
- On `main`, before this change, that same combination failed: `compileTestDevelopmentExecutableKotlinWasmJs` ran out of memory ("GC overhead limit exceeded") beside `:app:compileDebugKotlin`. That is outside this item. I recorded the counts from the default tasks instead.

The APK grew from 43,905,223 to 44,675,507 bytes (+770 KB), mostly from kotlinx-datetime and the moved code.

### On the emulator: before and after

The lead's decision (2026-11-17): take the pairs on the Mac's `T3_Fold_API35`, folded and unfolded, instead of `T3_Phone_API35`, which lor-xdoi needed. The folded cover screen is a phone-width screen.

- **Builds.** Before is main at `3be35dc` (`/home/supadava/.cache/lor-rgx4/before.apk`). After is this branch at `47d839d`, with main and lor-t8lq merged in (`after.apk`, sha256 `7ca91900…`). Both are debug builds from boat.
- **Server.** The test server on :3857 ran from this checkout with `LOR_FAKE_NOW=2026-11-17T15:00` and the demo set.
- **How each shot was taken.** `shoot.sh` (in `/home/supadava/.cache/lor-rgx4/shoot/`) writes one `layout.json` per surface, with that surface's cell as the start. It force-stops LoR and lets the system restart it as home, waits for a line of the surface's text and for the UI to go quiet, then takes a screenshot. The surfaces are:
  - the agent page;
  - a stack with `today` and `needs-you`;
  - a stack with `running` and `t3-queue`;
  - the LoR panel, and on the inner screen its People tab too;
  - the set-up card (`setup_done` taken out of the prefs);
  - the quick-send sheet (a home press on home at rest).
- **Result.** `pngdiff.py` compared each pair with the status bar's top 130 rows left out. All 13 pairs are identical: 6 on the cover screen, 7 on the inner. With nothing left out, a before and an after differ only in the status bar's clock (268 pixels). Different surfaces differ widely, so the shots are not stuck on one screen.
- **Files.** Everything is in `/home/supadava/.cache/lor-rgx4/evidence/emulator/`: `fold-{cover,inner}-{before,after}/` and `compare.txt`. Checksums are in `/home/supadava/.cache/lor-rgx4/evidence/MANIFEST.json`.
- **What no emulator shot shows.** T3 is not paired on the emulators, so no shot draws T3's cards (approve, answer, failed), a question's choices, the Running rows, the Threads list, the thread column or its reply box. Each of those draws the pair card or an unpaired line instead. For them the evidence is the lab's: the shots under `evidence/lab/` and the approve and answer gesture recordings, drawn by the same common functions from fixtures. The app's wiring for them (`PanelBuild.kt`'s question read, thread read and reply send) has compiled and kept its effects' keys, but no emulator check has run it.
- **The Mac's phone.** At 13:23, before I saw that the Mac's `T3_Phone_API35` was held by a stale session (`t3-8a333b78…`), `verify-lor install` put main's APK on it. My first `shoot.sh` run also wrote a layout there and force-stopped LoR. I restored the layout and told lor-xdoi, which then used boat's emulators.

### Changed on the lead's word

The timing line, 2026-11-17, as for lor-t8lq. It asked for a one-line edit to a moved composable to be timed three times with `android/web-dev.sh`. That never ran here, because the lead kept this machine's one local wasm build for lor-xdoi. The line now hands the timing to lor-5fk6, which times the launcher loop once, with home, the panels and the app screens all on main.

### Found on the way

- Fixed in its own commit: `scripts/lab/run.mjs` couldn't open a one-off state given as JSON. It waited for `data-drawn` to equal the unencoded hash and timed out. It now encodes the state as the lab's `LabLink.hash` does.
- Not fixed:
  - verify-lor's SKILL.md documents `atelier evidence record`, which this Atelier does not have.
  - `scripts/shots.mjs` times out on the first link of a lab run while the wasm loads cold; a retry draws it.
  - The `:homecore:allTests` out-of-memory failure on main, above.

