---
schema: atelier.item
schema_version: 3
id: lor-5fk6
title: 'The launcher and app in the lab per direction: home, drawer, search, panels, a menu, settings and the timer'
status: building
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-npa6
- lor-293p
- lor-zwfd
- lor-oech
- lor-xdoi
- lor-rgx4
- lor-t8lq
implementers:
- thread:delegated-task:command%3Amcp%3A1d7328d9-468a-43e2-acf6-a7c7c564b7bf%3Adelegate-task%3A7f767896-358a-46e5-a4e7-20d71de5c647
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.141851947Z
---

## Goal

Sumeet can use each direction's phone in the browser: swipe to the drawer, search, approve a card, start a timer, fold and unfold, and flip between directions with the Version dropdown.

## Done when

- [x] Each direction from the brief is a registered version in the lab, drawn in Compose in `homecore`'s `wasmJsMain` (so it never ships in the app), from the same fixtures as `current`. It starts once home, the panels and the settings and app screens are in the lab and the launcher loop has been timed with them there, so each concept is built beside the real thing and on a loop known to be fast enough.
- [x] The first direction is built here, with whatever shared pieces a direction needs to be quick to write (a navigation shell that follows the finger, a sheet, a fixture-backed store for taps). Each further direction is one item, added by the lead.
- [x] A direction's Done when is the brief's required set on the cover and inner screens and the brief's phone interactions working by touch and by `scripts/lab/gesture.mjs`, with recordings of the drawer swipe and one drag, and `shots/lab.json` gaining its states, captured on a dark, a pale and a mixed wallpaper.
- [ ] `scripts/lab/publish.sh` puts the build under `/lab/phone/`, the lab's first load over Tailscale is measured in the Report, and on Sumeet's Fold the page fills the screen it is opened on.

## Scope

`android/homecore/src/wasmJsMain/.../concepts/` and `shots/lab.json`. The app's own screens are not changed.

## Context

- Concept code is rough on purpose, as the dashboard prototype's is. What is worth keeping moves to common code when a direction is chosen, in a later mission.

## Report

Queue's phone is in the lab as version `queue`, with a kit the next three directions reuse. All twelve gesture scripts pass, covering the brief's eleven phone interactions and the menu. The loop is near its memory cap: a long session peaked at 4,085 MB of the scope's 4,096 MB, though nothing was killed. It also has a Kotlin crash I worked around rather than fixed (below). Two things are left for the lead or Sumeet: the first load over Tailscale from the real lab, and a look on his Fold.

### Read this first

- **The loop's memory.** With Queue in, a long session peaked at 4,085 MB of the 4,096 MB cap: 3,730 MB anon, the rest page cache. The kernel never throttled or killed anything (`memory.events`: `max 0`, `oom_kill 0`), and the log shows no `OutOfMemoryError`. The Kotlin daemon is 2,519 MB of it against a 1,536 MB heap, so about 1 GB is the JVM's own overhead. I tried two collectors for the daemon, leaving the caps alone. Serial GC saved about 800 MB but made edits 31 to 38 s. Parallel GC with two threads made them 15.5 to 17.7 s, and its lower peak came from a shorter session, so it proved nothing. web-dev.sh is unchanged. The next direction will probably push past the cap. The likely fixes are the Kotlin daemon's metaspace or code cache, or one fewer process (Gradle's own daemon is 687 MB, webpack 545 MB).
- **A Kotlin 2.3.20 crash, worked around.** An incremental compile whose changed files are all in `wasmJsMain` fails with `ArrayIndexOutOfBoundsException` in `WasmIrFileMetadata.fromByteArray`, from `WasmKlibCheckers`. Every build after it fails the same way until web-dev.sh restarts. A change that includes a `webMain` or `commonMain` file builds, and so does a whole-module compile. Turning off `kotlin.incremental.wasm` or `kotlin.incremental.js` did not help. So the concept code is in **`webMain`, not `wasmJsMain`** as the first Done when line says. The app compiles neither, so it still never ships. Editing `lab/Main.kt` still needs a web-dev.sh restart, and web-dev.sh's header now says so. Upgrading Kotlin past this is worth its own item. One other user has reported the same trace; I found no YouTrack issue.
- **Changes outside the Scope**, each in its own commit: the registry hook (`Lab.concepts`); `Fixture.phone.fontScale`, which the lab now applies to every version; four named states every direction needs (`quiet`, `empty-day`, `sync-failed`, `large-text`, listed in STATES.md); a lab anchors fix; the page's fill mode; `publish.sh` serving the build at `/lab/phone/` instead of `/lab/launcher/`; web-dev.sh's header; and `scripts/lab/snap.mjs`.

### The loop: before and after

| | Before Queue (main at 5f869fe) | With Queue |
| --- | --- | --- |
| First build | 68 s (31 to 46 s on a restart) | 32 to 42 s on a restart |
| One-line edit to common code (`home/Undo.kt`) | 21.7 s cold, then 13.2 to 15.3 s | 13.2 to 13.7 s, four times |
| One-line edit to a concept (`queue/QueuePage.kt`) | (none existed) | 21.3 s cold, then 14.2 to 16.1 s |
| Peak memory of the scope | 3,866 MB (about 3,300 MB anon) | 4,085 MB over a long session (3,730 MB anon) |

lor-xdoi measured 8.9 to 9.3 s for the `Undo.kt` edit, so warm edits have grown by about 4 s as the panels and the app screens moved in. Times run from save to served.

### Queue on the phone

The organisation is the brief's. Home keeps the clock, the date, the weather and the next event. Below them sits one line: the running timer with Stop (or Start), and "N waiting in the queue ›", which opens the queue. Then the start page's icons near the thumb, then search. The queue is the page left of home. It merges the agent page and the LoR panel, ranked as the brief says: T3 approvals, questions and failed turns, then LoR's flags, then tasks due today or before. The lead card wears the amber "NOW" reticle; when it clears, the next card slides up and the reticle closes in on it. Unfolded, the queue is the left pane and the open item the right. The drawer is a swipe up, and a swipe down on home opens the shade (a stand-in, since the lab can't open Android's).

- **The look** follows the dashboard prototype's tokens, so the phone and the dashboard read as one product. It has five type steps (13, 15, 17, 22 and the readout) in Saira, with Saira Condensed only for the clock, the readout and the reticle's label. Amber is used only for the lead card, its primary action and the running timer; cyan for the agent; red for failures. There are three depths: rows flat, the lead card raised, and sheets, menus and the drawer as glass. A sheet blurs the page under it (`Backdrop`). Home picks its ink from the wallpaper: dark words on the pale one, and on the mixed one's pale sky.
- **The one timer and one settings surface** (decision 3). The timer has one vocabulary ("No label") and three verbs: Start, Stop and "Switch to…". Settings is one sheet: this phone's sync and its missing permissions first, then connections, home and sensors.
- **Undo, honestly.** Tick, Done, Seen, a due date, a timer change, a moved icon and a dismissed failure all offer Undo. Approve, Deny and an answer show "Approving…", a draining bar and Cancel for three seconds, then go, with no Undo.
- **Nothing only behind a gesture.** Every card has a visible ⋯ menu. The queue is reached from home's line, the drawer from the grid button in the search pill, and quick send from search's "Ask the agent" as well as by a double tap.
- **Search** is the common-ground field. Plain words find apps, people, tasks and settings. Words with a day or a `#goal` offer "Add task" with what was understood shown as chips. Anything else offers "Ask the agent". Enter does the first result.

### Where each required state appears (links are `http://127.0.0.1:3860/` plus the hash)

- **Nothing wants you** (`#queue/quiet`): home's line reads "Nothing waiting"; the queue reads "Nothing wants you" with the next event (`#queue/{"base":"quiet","where":{"cell":[0,0]}}`). When LoR or T3 can't be read, it says "Nothing to show yet" instead, because it can't know.
- **An empty day at 09:00** (`#queue/empty-day`): the glance and the queue at 09:00, with the day's events in "Later today".
- **A failed phone sync** (`#queue/sync-failed`): the second half of home's line, "Phone sync failed · Fix", in red, and a note heading the queue. Fix opens Settings at the sync, with Allow per missing permission and Sync now.
- **T3 disconnected** (`#queue/agent-t3-stale`): a note heading the queue, "T3 is reconnecting · cards as T3 had them 40 min ago". Approve and Deny are disabled, and quick send fails saying why.
- **LoR unreachable, stale** (`#queue/stale`): home's line reads "LoR isn't answering · as of 13:00"; the queue's note reads "Showing what it said 2 h ago". `#queue/failed` (no data) says "Can't reach LoR" with Settings.
- **A crowded queue** (`#queue/overfull`): twelve T3 cards and nine flags, in one scrolling list with the count in its header.
- **Larger text** (`#queue/large-text`): every version is drawn at the fixture's font scale, 1.3 here.

The required screens are home, the drawer and search (results and the keyboard up: `#queue/search-mixed`), the queue on the cover and inner screens (`#queue/agent-waiting`, its question with option descriptions and Other for free text), one menu (`#queue/app-menu`), one sheet (quick send), settings, the timer, and a thread on the inner screen (`#queue/thread-long/inner`). `shots/lab.json` gained 72 Queue rows: every state on both screens over the dark wallpaper, the queue page in the states that change it, the four sheets, and the main screens over the pale and mixed wallpapers. All 72 were captured with no problems to `~/.local/state/lor-evidence/lor-5fk6/shots/`.

The brief's question "that takes several answers or free text" is met by free text (Other, then Send). Queue also handles several answers (pick several, then "Send N answers"), but no fixture has a `multiSelect` question, so that path has never been drawn from a state. A state with one would show it for every direction.

### Interactions, by script

`scripts/lab/gestures/queue/`, each run with `node scripts/lab/run.mjs <script> --version queue --state <s> --out <dir>`. Each checks by anchors that the gesture did what it says. All pass on the final build. Recordings, strips and final frames are in `~/.local/state/lor-evidence/lor-5fk6/gestures/`.

1. `drawer` (demo; also on pale): a swipe up opens the drawer and a swipe down closes it. **Recorded.**
2. `search` (demo): the search field opens search with the keyboard up, "spo" finds Spotify, and a tap opens it.
3. `approve` (agent-waiting): Approve shows Cancel, then the card clears and the question takes the reticle.
4. `answer` (agent-waiting): Other, free words, Send, Cancel's window, cleared.
5. `timer` (demo): Start on home's line, a label, Stop on the line, then Undo shows.
6. `tick` (demo): on the queue, tick a task due today, then Undo brings it back.
7. `move` (demo; also on mixed): a long press opens the menu, which stays; a drag lifts the icon and closes the menu, and Camera lands after T3 Code with Undo. **The recorded drag.**
8. `fold` (thread-long): the thread in its sheet, scrolled back, with 12 characters of reply typed. Unfold: the right pane has the same draft and scroll. Fold: still kept.
9. `reply` (thread-long, inner): a reply typed and sent joins the thread.
10. `quick-send`: a double tap on home, words, Send. It says it went on `demo`, and on `agent-t3-stale` says why not, keeping the words.
11. `sync` (sync-failed): "Fix" on home's line, Allow twice, Sync now, synced.
12. `menu` (demo): a long press on WhatsApp opens its menu, which stays after the lift; Back closes it.

Home's own `drawer` script still passes on `current`.

The scripts found three real bugs, now fixed. At its lower rest point a sheet slid its foot (the reply field) off the screen; it now resizes. On the inner screen the stand-in keyboard covered the reply field; the pane now makes room. A state's app menu never opened.

### Publishing, and the first load

- **boat:** `android/boat-build.sh` with its default tasks (`assembleDebug testDebugUnitTest testReleaseUnitTest :homecore:allTests` and the lab's production bundle) passed in 3 min 20 s on 2837efa. That is 940 tests and 0 failures, `FixtureCatalogueTest` among them, so STATES.md matches the registry and the app still compiles with the common-code changes.
- **The scratch publish:** the production bundle went into the folder `publish.sh` copies, and `LOR_LAB_DIR=~/.cache/lor-5fk6/lab-scratch scripts/lab/publish.sh` put it at `phone/`, listed in the lab's index as "Phone lab". I did not publish to the real lab, because that would replace the dashboard prototype's build; the lead publishes from main.
- **First load**, from a stand-in for lor-web's `/lab/` (`scripts/lab/serve.mjs`'s `labStatic`, with its Content-Security-Policy), cold cache, to `#queue/demo` drawn: 17.8 MB transferred, 1.6 s on loopback, and 8.3 s with Chrome throttled to 20 Mbit/s and 60 ms. No console errors and nothing blocked by the policy (`published-queue-demo.png` in the evidence folder). The real figure over Tailscale needs the real lab, so it is the lead's to take after publishing from main.
- **On the Fold:** the page's fill mode (under "Tool fixes") makes a phone-sized touch screen show only the phone, and follows a fold. I checked it in Chrome at the cover's and the inner screen's sizes. Whether it fills Sumeet's Fold can only be seen on his Fold.

### The kit, for the next directions

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/README.md` says how to add a direction in one line (`Concepts.all`) and what each piece does:
- `ConceptStore`: the fixture as state, every tap's effect, Undo, the cancel window, and fold-survival.
- `PageShell`: pages and a drawer that follow the finger.
- `Sheet`: rest points, drag to dismiss.
- `Backdrop`, `Menu`, `ToastHost`, `StandInKeyboard`, line icons, and placeholder app icons.

The pieces take their look from a `KitLook` the direction provides.

### Tool fixes, each its own commit

- **Lab anchors:** a node leaving no longer takes a name another node still carries. A sheet giving way to a pane on a fold had made the pane's anchors vanish.
- **The lab page fills a phone:** on a touch screen under 1,000 px, the toolbar and bezel go and the phone takes the viewport. The screen follows the viewport's width, so folding the real Fold flips cover and inner and keeps the place. A Lab button shows the toolbar; `?fill` and `?framed` force it either way. Checked in Chrome at 412 × 900 and 875 × 971, with a resize between them: no sideways scroll, and the state kept.
- **`scripts/lab/snap.mjs`:** screenshots lab links one page at a time, cropped to the phone.
- **web-dev.sh's header:** today's numbers and the crash.
