---
schema: atelier.review
schema_version: 3
round: 1
commit: ff9cbe3ec72dff2c7206c670d6080b7ab5875b26
reviewers:
- thread:delegated-task:command%3Amcp%3A87a6b8da-a3c1-4a8a-9a05-147697ce2cfd%3Adelegate-task%3A176d6a5b-fab3-4d95-9a9d-1efb564084d9
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T17:42:03.993748568Z
---

## Summary

Complexity: high. This adds a CDP gesture API, a recording pipeline, a Compose test surface, and browser input/animation adaptations that future surface scripts will depend on.

Value: high. Replaying and watching gestures is necessary for the lab's planned launcher and app surfaces; a screenshot alone cannot check motion.

Risk of merging it: low. The behavior is confined to lab tooling and a fixture surface, failures are visible, and the change is readily reversible. The common module also gains a navigation dependency; the author reports a passing Android build.

The value justifies the implementation's size, but two reproducible defects need fixing before merge.

Reviewed main...ff9cbe3ec72dff2c7206c670d6080b7ab5875b26, including every changed implementation file, the helper scripts, documentation, and the supplied evidence under /tmp/lor-verify/lor-293p/. Built the lab using android/web-dev.sh in its capped scope after confirming no local Gradle build or emulator was running. Stopped lor-web-dev.scope when finished and confirmed port 3860 was free.

Independently ran all eight supplied gesture scripts against #gestures/demo. Every run produced its four artifacts, had no page errors, and matched its documented final readout: row 3, double taps 1, menu open, page 3/5, top row 50, tile 216,136, page 2/5 with top row 28 on the inner screen, and back 1. Runs took 2.24–4.00 seconds. Inspected the resulting readouts and the round-trip fold strip. Also ran swipe at speeds 0.25 and 0; both ended on page 3/5, with more frames at quarter speed and fewer with no motion. Mouse clicks selected row 3, mouse dragging turned a page, a held mouse press opened the menu, and the wheel scrolled. Real CDP touches also scrolled the list with a viewport shorter than the phone, without scrolling the outer page or cancelling the pointer. Back worked with focus on the page body; the canvas focus case fails as described below. pnpm test passed all 206 tests in 23 files (pnpm's preliminary dependency verification warned about the worktree's node_modules symlink, but Vitest completed successfully).

Reviewer evidence and minimal reproduction scripts are in /tmp/lor-verify/lor-293p-review/. readouts.png shows the eight helper outcomes; mouse-readouts.png shows mouse actions and the focused Escape failure. I did not rerun the boat build or Android host tests, exercise the fixture on an emulator, use a physical touch screen, or test typing inside a real Compose text field. Those remain limits of this review; the supplied Android build/test report was read, not independently reproduced.

## Finding 1: One Escape sends Back twice when the canvas has keyboard focus
Recommendation: fix now

android/homecore/src/wasmJsMain/kotlin/com/spadaval/lor/lab/Main.kt:249 registers a bubbling document keydown listener, and line 277 sends backNow() for Escape in addition to Compose's own Escape handling on the canvas. Calling preventDefault() at line 283 does not prevent that duplicate delivery.

Reproduced through normal keyboard navigation: select row 3, long-press the tile to open its menu, focus the toolbar's JSON button, press Tab once to enter the canvas, and press Escape once. Before Escape the readout is row 3, menu open, back 0. After it the readout is row none, menu shut, back 2. A single toolbar Back or B closes only the menu and leaves row 3 selected. The canvas already has tabindex=0; no forced tabindex or canvas focus is needed for this reproduction.

Run: `TMPDIR=$HOME/.cache/lor-review-293p/tmp node scripts/lab/run.mjs /tmp/lor-verify/lor-293p-review/escape-focus.mjs --version gestures --state demo --out /tmp/lor-verify/lor-293p-review/escape-focus`. The script, escape-before.png, and escape-focus/final.png preserve the reproduction. Route Escape through one navigation path so it produces exactly one Back whether focus is on the body or inside Compose, while retaining the typing guard. This is required for the toolbar and its keys to behave alike, and prevents a future real surface from popping two navigation levels.

## Finding 2: Recording crops away intermediate screen sizes when a script folds out and back
Recommendation: fix now

scripts/lab/run.mjs:144–149 calculates the recording and strip crop from only the phone's initial and final bounds. A larger intermediate screen is omitted whenever the script returns to its initial shape. This affects the exported artifacts even though the gestures themselves succeed.

Reproduced with `await fold("inner"); await wait(300); await fold("cover")` starting on the cover screen. The WebM is only 510 pixels wide, matching the cover bezel, even while the inner screen and its bezel are 905 pixels wide. The strip's inner-screen frames at roughly 300–800 ms lose both sides of the screen, including part of the readout and pager. The same issue applies to a rotate-and-return script.

Run: `TMPDIR=$HOME/.cache/lor-review-293p/tmp node scripts/lab/run.mjs /tmp/lor-verify/lor-293p-review/round-trip.mjs --version gestures --state demo --out /tmp/lor-verify/lor-293p-review/round-trip`. round-trip/strip.png and round-trip/recording.webm show the clipping; middle-full.png shows the complete intermediate screen. Include all screen bounds visited during the recording, or use a fixed frame that fits every supported shape, so fold/unfold and rotate sequences remain reviewable throughout.
