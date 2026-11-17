---
schema: atelier.review
schema_version: 3
round: 2
commit: ba7669a2fa8863b2394289fad75d162692a85113
reviewers:
- thread:delegated-task:command%3Amcp%3A87a6b8da-a3c1-4a8a-9a05-147697ce2cfd%3Adelegate-task%3A390e2cc7-c401-490a-8fa2-452a69702fd4
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T17:50:35.957174425Z
---

## Summary

Complexity: high. The change adds a CDP gesture API, a recording pipeline, a Compose test surface, and browser input and animation adaptations that future scripts will depend on.

Value: high. Replaying gestures with visible motion is necessary for the lab's planned launcher and app surfaces.

Risk of merging it: low. Failures in this lab tooling and fixture surface are visible and reversible. The shared module also gains a navigation dependency; the report supplies Android build and test evidence.

The value justifies the size. Both fix-now findings from round 1 are fixed. One additional observation about the fling example is deferred because the helper sends the requested physical samples and demonstrates a real fling, even though its precise settled position varies.

Reviewed main...ba7669a2fa8863b2394289fad75d162692a85113 and the changes since ff9cbe3ec72dff2c7206c670d6080b7ab5875b26, including the helper API, runner, mouse forwarding and animation clock, toolbar, anchors, gesture surface, registry/link changes, tests and documentation. Read the supplied evidence from both rounds and the previous review. Built the wasm lab with android/web-dev.sh after pgrep confirmed no local Gradle build or emulator. Used the cache-backed TMPDIR for Chrome. Stopped lor-web-dev.scope when finished and confirmed neither its Gradle/Kotlin daemons nor port 3860 remained.

Independently replayed both original reproductions. A single Escape with canvas focus reached by tabbing from JSON now leaves row 3 selected, closes the menu and increments Back once. Escape with body focus has the same result. The fold-inner/fold-cover strip shows the entire intermediate inner screen, and ffprobe reports a 906-pixel-wide recording rather than the original cover-only crop. A rotate-and-return recording is 1150 pixels wide and retains the wider intermediate shape.

Ran all eight supplied gesture scripts. Each produced recording.webm, strip.png, final.png and run.json without page errors, in 2.21–4.43 seconds. Inspected their final readouts and the fold strip. Tap, double tap, long press, swipe, fold and back matched their comments; drag ended at the documented alternative 211,136. The fling's final-row variance is recorded below. Swipes at speeds 0.25 and 0 both reached page 3/5; quarter speed produced 65 strip tiles versus 42 at full speed and 36 with no motion, taking 5.11 seconds including encoding.

Mouse clicking selected row 3, dragging turned the pager, a held press opened the menu, and the wheel scrolled. Checked Escape and B with canvas focus, F and R retaining row/menu state, S cycling 0.25/0/1, and H clearing the scene. Shortcuts left the hash and speed unchanged while the JSON textarea had focus. pnpm test passed all 206 tests in 23 files; its preliminary dependency check warned about the worktree's node_modules symlink, but Vitest completed successfully.

Evidence and reproduction scripts are in /tmp/lor-verify/lor-293p-review2/. input-readouts.png and helper-readouts.png collect the input and helper outcomes; the recording directories preserve the full artifacts. I did not rerun the boat build or common Kotlin/Android tests, exercise the fixture on an emulator or physical touch screen, or test typing in a real Compose text field. The Android build/test claims were assessed from the report, not independently reproduced.

## Finding 1: One Escape sends Back twice when the canvas has keyboard focus
Recommendation: fix now
Was: 1-z9wz.1
Response: fixed in 0fcfc78: Escape from Compose's canvas is left to Compose; the reviewer's escape-focus.mjs now ends "row 3 · menu shut · back 1", and Escape with focus on the body also gives back 1 (`/tmp/lor-verify/lor-293p/round2/escape-focus`, `escape-body`).
Now: fixed
Confirmed by replaying /tmp/lor-verify/lor-293p-review/escape-focus.mjs against the reviewed commit and a separate body-focus script. Both final readouts show row 3, menu shut, Back 1. Main.kt now leaves canvas Escape to Compose and sends the toolbar back only for other targets. Evidence: /tmp/lor-verify/lor-293p-review2/{escape-focus,escape-body}/final.png.

## Finding 2: Recording crops away intermediate screen sizes when a script folds out and back
Recommendation: fix now
Was: 1-z9wz.2
Response: fixed in 0fcfc78: run.mjs crops to every bezel rect gesture.mjs measured; the reviewer's round-trip.mjs strip now shows the whole inner screen between the two cover frames (`/tmp/lor-verify/lor-293p/round2/round-trip`), and the eight scripts still end on their readouts in 3.1 to 4.6 s (`round2/readouts.png`).
Now: fixed
Confirmed by replaying /tmp/lor-verify/lor-293p-review/round-trip.mjs and a rotate-and-return script. gesture.mjs records each measured bezel after fold/rotate, and run.mjs includes all of them in the crop union. The inner screen is whole in round-trip/strip.png; the recordings measure 906×1150 for folding and 1150×1150 for rotating. Evidence: /tmp/lor-verify/lor-293p-review2/{round-trip,rotate-trip}/.

## Finding 3: The fling example promises a final row that varies between identical runs
Recommendation: defer

scripts/lab/gestures/fling.mjs:1–3 says the 4000 dp/s, 120 dp fling ends at top row 50; the Report also presents this as the example's outcome. The unchanged example instead settled at top row 45 in my first run. Three further runs of the same helper with passive pointer-event tracing settled at rows 45, 50 and 45. This is beyond the drag-only variance described in the report and matters when later scripts use a fling to reach a particular list position.

The trace script is /tmp/lor-verify/lor-293p-review2/fling-trace.mjs. Run it with the same run.mjs command as the supplied fling example. The helper sends the correct 120 dp over 30 ms, including the final lift timestamp. Chrome delivers differently coalesced pointermove samples in the different runs; this suggests a delivery/sampling issue, but does not establish which layer causes the changed settling position. The final images and recordings in fling/ and fling-trace1/ through fling-trace3/ preserve the differing outcomes.

Deferred because this still proves a real fling with correctly stamped input, and the item's Done when lines do not require an exact final list index. Document the variance rather than promising row 50, and have future surface scripts target an anchor or verify the resulting position when it matters. Investigating deterministic settling can accompany the first such consumer; no additional retry or sleep is justified by this evidence.
