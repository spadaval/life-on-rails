---
schema: atelier.item
schema_version: 3
id: lor-293p
title: 'Gestures by script in the lab: swipe, fling, long press, drag and fold, with a recording'
status: in_review
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-vbfd
implementers:
- thread:delegated-task:command%3Amcp%3A275230a9-0f8b-446e-b3b1-4303d9895adf%3Adelegate-task%3A0a95ef95-ae92-4147-bb88-abd412041ae7
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.965574679Z
---

## Goal

Motion and touch can be tried and replayed in the browser: a swipe that follows the finger, a long press that becomes a drag, a fold in the middle of a gesture. A person uses the mouse or a touch screen; an agent uses a script and gets a recording to look at.

## Done when

- [x] In the lab, mouse and touch both drive Compose's pointer input: a drag is a swipe, a held press is a long press, a wheel scrolls. A toolbar gives Home, Back, Fold and Unfold, rotate, and a slow-motion switch (animation scale 1, 0.25, 0); the same are keys.
- [x] `scripts/lab/gesture.mjs` exports `tap`, `doubleTap`, `longPress`, `swipe` (direction, distance in dp, duration), `fling` (speed in dp/s), `drag` (path, holds in ms), `fold` and `back`, sending real touch events to the page through Chrome's protocol, with distances in the phone frame's dp.
- [x] `node scripts/lab/run.mjs <script> --version <v> --state <s> --out <dir>` plays a gesture script and writes a WebM recording, a frame strip PNG (every 50 ms across the gesture) and the final screenshot. The runner is proven here on a gesture test surface registered in the lab for this purpose (a pager that follows the finger, a scrolling list, a tile that opens a menu on long press and can be dragged, a two-pane layout that folds), with one script per helper. Scripts for real surfaces are written by the item that brings the surface into the lab: lor-xdoi adds the drawer, paging, app menu, drag, long press into drag, search and folder back; lor-rgx4 adds approving a card, answering a question and the timer; and each concept adds its own.
- [x] A run of one script takes under 10 seconds once the lab is built.

## Scope

`scripts/lab/`, the lab's toolbar. Emulator gestures stay with T3 Code's device tools.

## Context

- Compose for the web draws to one canvas, so scripts address positions in dp or a fixture's named anchors, not DOM nodes.

## Report

A gesture script now plays real touches on the launcher lab and returns a recording, a strip and the final screen, in 2.5 to 4.3 s a run. Each of the eight scripts in `scripts/lab/gestures/` ends where its comment says. lor-xdoi and lor-rgx4 can write their scripts against it.

**Run it.** Serve the lab with `android/web-dev.sh`, then:

`node scripts/lab/run.mjs scripts/lab/gestures/drag.mjs --version gestures --state demo --out <dir>`

- The run writes `recording.webm`, `strip.png`, `final.png` and `run.json` into `<dir>`.
- `strip.png` shows the screen every 50 ms from the first touch until it settles, each tile stamped with its time.
- `run.json` holds the link, each gesture's times, the frames Chrome sent and the anchors at the end.
- `--screen`, `--wallpaper`, `--landscape` and `--speed` set the start; a script may also export `link`.

**The eight scripts, each run once after the last change** (`/usr/bin/time`, with the lab already built):

| Script | Readout at the end | Time |
|---|---|---|
| tap | row 3 selected | 3.04 s |
| double-tap | double taps 1 | 2.46 s |
| long-press | menu open | 2.98 s |
| swipe (two of 300 dp) | page 3/5 | 4.03 s |
| fling (4000 dp/s, 120 dp) | top row 50, the list's end | 3.80 s |
| drag (long press, then +120,0 and +200,120 dp) | tile 216,136, menu shut | 3.84 s |
| fold (page turned and list flung, then unfolded mid-swipe) | page 2/5, top row 28, two panes 875×971 | 3.91 s |
| back (predictive to 60% and let go, a full one, then the button) | back 1, menu shut | 4.26 s |

**`gesture.mjs`**
- It exports `tap`, `doubleTap`, `longPress`, `swipe(direction, dp, {duration, from, hold})`, `fling(direction, dp/s, {distance, from})`, `drag(path, holds, {speed})`, `fold(to?)` and `back({predictive, duration, to, cancel})`.
- It also exports `rotate`, `home`, `speed`, `wait`, `anchors` and `at`.
- `down`, `move`, `hold` and `up` cover anything else, such as a fold with the finger down.
- Every touch is a CDP `Input.dispatchTouchEvent` at the phone's dp.
- A point is `[x, y]` in dp, an anchor's name, or `{ anchor, at: [fx, fy], by: [dx, dy] }`. A surface names anchors with the new `Modifier.labAnchor("name")`, and the page publishes them in the body's `data-anchors`.
- `drag` reads every point before the finger goes down. Otherwise an anchor that follows the finger, like the tile, moves the path along with it.

**Chrome takes 30 to 50 ms to deliver each touch to a headless page.** In the first version, a 500 ms swipe took 2.3 s and the fling moved only two rows.
- Moves are now sent without waiting on each one, stamped with the time they were due.
- The lift is stamped on the gesture's own clock. Stamped by when it was sent, it made the finger look stopped, and nothing flung.
- After the fix, the 4000 dp/s fling carries the list to its end.

**The lab page**
- **Mouse as a finger.** A mouse drag scrolled nothing, because Compose for the web scrolls only on the wheel. A press of the main button on the screen is now sent on as a touch pointer, so a drag is a swipe and a held press is a long press. The wheel goes through unchanged.
- **The canvas wrap.** Chrome sends a `pointerenter` straight to the canvas, past every listener on `window`. One that got through left Compose with a hovering mouse beside the finger, and taps failed. So index.html wraps the canvas's own pointer listeners so they never hear a real mouse.
  - This patches `HTMLCanvasElement.prototype.addEventListener`. It is the one piece that leans on how Compose listens.
  - Proven with the mouse in a headless Chrome: a click selects a row, a drag turns the page, a long press dragged off the screen's left edge moves the tile and lets go, and the wheel scrolls.
- **Touch dots.** Each finger gets a dot, as Android's "Show taps" draws it. The dots show in recordings.
- **Toolbar.** Home, Back, Fold/Unfold, Rotate and Speed, with keys H, Escape or B, F, R and S. Keys are ignored while typing in a field or with a modifier held.
  - Rotate is a fifth hash part, `landscape`. `LabLinkTest` covers it.
  - The scene is now keyed on version, state and Home presses, not the whole link. A fold, a turn or a new wallpaper keeps the surface's place (page, scroll, tile), as the phone does.
- **Back.** It goes through Compose's navigation-event dispatcher, using a `DirectNavigationEventInput`, so common code hears it with `NavigationBackHandler`.
  - I added `navigationevent-compose:1.1.0` to commonMain. Compose's ui already brings it in on common; it is now explicit for Android too.
  - Each key does its job: S steps 1, ¼, none; F folds and unfolds; R turns and straightens; Escape and B are each one back (with the menu open and a row selected, Escape shut the menu and B cleared the row); H starts the state again (`keys.png`). Focus was on the page's body throughout: in headless Chrome, Compose's canvas did not take focus even after taps, so I could not try Escape with the canvas focused. The typing guard reads the event's real target (`composedPath()[0]`), so a field inside Compose's shadow root is covered, but no surface here has a text field. lor-xdoi's search script should check that typing letters in it doesn't fold or rotate the lab.
  - A back with no handler enabled changes neither the history nor the hash.

**Speed: how I read "animation scale 0.25".** I took it as quarter speed (animations four times as long), and 0 as no animation, where each one ends on its next frame.
- `ComposeViewport` takes no coroutine context, so a `MotionDurationScale` can't reach the scene.
- index.html instead warps the timestamps `requestAnimationFrame` hands Compose. Pointer events keep their real stamps.
- Checked on the fling and the pager:
  - A 1500 dp/s fling ends on top row 11 every time: three runs at 1, three at ¼, two at 0.
  - The pager's settle after a swipe filled 43 strip tiles at 1, 65 at ¼ and 36 at 0.
- Gesture detection is the same at every speed. `long-press`, `double-tap`, `back`, `swipe` and `fold` at ¼ and at 0 end with the same readouts as at 1 (`speeds/readouts.png`). The one difference is fold at ¼, which ends on top row 14 instead of 28: the slowed fling hasn't finished when the fold comes.

**Fold mid-gesture.** The finger keeps its dp position, and the surface keeps its state. The pager's own drag starts again from where the finger is, because the pager moves into the left pane, which is what a layout change does on the phone. `fold.mjs` says so.

**The `gestures` version** (`GestureSurface.kt`) is the test surface. It has a pager that follows the finger, a 60-row list, and a tile: double tap counts, a long press opens a menu, and drifting 8 dp turns the press into a drag that shuts the menu. A predictive back shrinks the menu as it comes. It shows two panes on the inner screen and one column on the cover. A readout states what the last gesture did. It is listed in STATES.md, which also gains a "Gestures" section, and has three rows in `shots/lab.json`: `scripts/shots.mjs --catalogue shots/lab.json --only gestures` took 2.1 s with no problems.

**A known 2% variance in drag.** `drag.mjs` ends at 216,136 or 211,136. Over seven runs (`drag-variance/`) it gave 211 twice and 216 once at speed 1, and 211 at ¼ and at 0, so speed is not the cause. The 5 dp is one 8 ms move step at 600 dp/s, lost from the first segment after the long press; y, from the second segment, is always exact. I did not find which layer drops it (Chrome's touch handling or Compose's drag after a long press). A script that needs an exact drop point should use an anchor and a margin wider than 5 dp.

**Frames.** Chrome's screencast sends a frame only when the screen changes: 18 to 49 a second while something moves, and few when still.
- The strip shows, for each 50 ms tick, the latest frame drawn by then, timed by Chrome's own frame stamp.
- While something moved, the longest gap between frames was 117 ms, so a strip can repeat a tile now and then.

**For the reviewer and the lead**
- **Chrome would not start under this host's default TMPDIR.** `playwright` failed at `page.goto` because `/tmp/claude-1000`, Claude Code's temp folder, was at its quota from other sessions' files. Setting `TMPDIR` elsewhere fixed it. That is this machine's state, not the code's. I did not check whether it explains `shots.mjs`'s timeouts, and I captured at most three shots at a time.
- **Boat** (`android/boat-build.sh`, default tasks, on the committed Android tree) passed in 3 min 6 s: the debug APK built, so `navigationevent-compose` resolves for Android. Every app and homecore test came back with no failures, among them `LabLinkTest` (13 tests, including the new landscape ones), `FixturesTest` and `FixtureCatalogueTest`. `pnpm test` passed: 206 tests in 23 files.
- **Not tried on the emulator.** `NavigationBackHandler` in the debug app's fixture activity (`--es lab_version gestures`) needs the activity to provide a dispatcher owner, which a ComponentActivity does; a boat compile does not show that it works.
- **`labAnchor` costs a state write per layout pass**, and in the app nothing reads it. It is fine for the test surface. lor-xdoi should decide whether real surfaces wear it in release builds.
- **Runner fix found late.** One run with a fold and two turns got a frame from Chrome smaller than the viewport, and ffmpeg's crop failed on it. Every frame is now scaled to the viewport first.
- **Kotlin's wasm crash** happened once (`ArrayIndexOutOfBoundsException` after an edit to Main.kt), and restarting `web-dev.sh` cleared it. It was also restarted once for the new dependency.
- **Evidence** is in `/tmp/lor-verify/lor-293p/`: each script's `recording.webm`, `strip.png`, `final.png` and `run.json`, plus `readouts.png` (the eight readouts), `speeds/`, `drag-variance/`, `keys.png` and `shots/` (the three new lab shots). The landscape shot is clipped by the bezel's last pixel at the gallery's 1000 px width; the screen is whole.
- **Not done here:** scripts for real surfaces (lor-xdoi, lor-rgx4). Anchors on real surfaces are theirs to add.

### Responses

- 1-z9wz.1: fixed in 0fcfc78: Escape from Compose's canvas is left to Compose; the reviewer's escape-focus.mjs now ends "row 3 · menu shut · back 1", and Escape with focus on the body also gives back 1 (`/tmp/lor-verify/lor-293p/round2/escape-focus`, `escape-body`).
- 1-z9wz.2: fixed in 0fcfc78: run.mjs crops to every bezel rect gesture.mjs measured; the reviewer's round-trip.mjs strip now shows the whole inner screen between the two cover frames (`/tmp/lor-verify/lor-293p/round2/round-trip`), and the eight scripts still end on their readouts in 3.1 to 4.6 s (`round2/readouts.png`).
