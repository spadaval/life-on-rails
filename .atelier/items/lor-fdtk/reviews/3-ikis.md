---
schema: atelier.review
schema_version: 3
round: 3
commit: 78dda07d5ef29dcf2a5a7bddc533b4e795ce6020
reviewers:
- thread:delegated-task:command%3Amcp%3A66276f89-0d39-4dbb-a3a6-0a84e6042e28%3Adelegate-task%3Aa9e397fd-26a9-4c5f-b8a0-d33037c4a895
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-18T02:26:05.865105035Z
---

## Summary

- **Complexity: high.** About 4,400 added lines implement a complete concept with separate page, pane, focus, drag and fold state, plus two changes to shared kit behaviour.
- **Value: high.** The design comparison needs Panes working by touch on both Fold screens; static frames would not test its defining list-and-detail interaction.
- **Risk of merging: low.** The change stays in the fixture-backed browser lab. The shared sheet and keyboard also affect Queue and Timeline, whose relevant gesture checks pass.

The value warrants the size. The code implements the brief's required comparison rather than a new production subsystem; I found no substantially smaller change that delivers these screens and interactions.

Reviewed `main...78dda07d5ef29dcf2a5a7bddc533b4e795ce6020` and the round-2-to-round-3 diff against the product spec, lab brief, every Done when line and the Report. Read all nine Panes source files, its registration and font additions, the shared keyboard/sheet changes, relevant kit store and shell code, the gesture scripts, screenshot catalogue and publish source. The catalogue adds 98 Panes rows: 80 dark, 9 pale and 9 mixed. The Report's dark subtotal of 78 is a counting typo. Verified the existing publish source includes the phone bundle under `/lab/phone/`, and inspected the supplied scratch-publish and risk evidence.

Served the saved full boat bundle from `/home/supadava/.cache/lor-fdtk/boat/lab` on loopback port 3861. The Report identifies it as the 64bbdbc6 build; only the item's Report changed between that commit and the reviewed commit. Independently obtained passing runs for all 25 Panes gesture scripts, covering all eleven required interactions and both new regressions. The settings check performs both drags: the first returns to the section list at its original sheet height, the second closes it. Also ran Queue and Timeline's quick-send, timer, sync and search scripts: all eight passed. All successful runs recorded no page errors. The initial batch's multi-answer invocation used an encoded JSON state incorrectly; passing decoded JSON corrected that invocation. The separate sheet-switching check had one failure followed by passing focused checks; see Finding 2.

`pnpm test`: 23 files, 206 tests passed. Captured and inspected representative large-text, quiet, empty-day, stale and overfull states, including both screen sizes and the three wallpapers. Fresh recordings, frames, anchors and run metadata are under `/home/supadava/.cache/lor-fdtk-review3/`.

Did not rebuild Gradle, rerun the reported 940 boat tests, recapture all 98 catalogue rows, publish to the real lab, or test a physical phone/emulator. No production app source changes. TypeScript typechecking was not rerun because no TypeScript source changed. The carried fix-now finding is fixed; the remaining test issue is deferred.

## Finding 1: Settle the settings sheet when a drag returns to the section list
Recommendation: fix now
Was: 2-kcb7.5
Response: fixed in 151796d6: the kit's sheet returns to its rest point when its owner keeps it open after a drag-to-dismiss, and its drag handler calls the current dismissal (the second drag, from the list, ran the stale "back to the list" one and never closed); `settings-drag.mjs` does both drags and checks the sheet's resting top.
Now: fixed

The change in `kit/Sheet.kt:86` now dismisses through the current callback and restores the remembered rest detent when the owner keeps the sheet open. Updating the callback also lets the second drag close Settings after the first changes its section. Independently reran `settings-drag.mjs`; both assertions passed. Evidence: `/home/supadava/.cache/lor-fdtk-review3/settings-drag/`.

## Finding 2: Investigate the intermittent sheet-switching script failure
Recommendation: defer

`scripts/lab/gestures/panes/sheet.mjs:14` takes the first task from the anchor map, then line 18 takes the first flag whose bounds intersect the screen. Those choices do not establish that the tap point is exposed above the sheet or outside a row's embedded actions. In the initial batch, this script failed at line 20, “a flag opened into the same sheet”; the task detail remained open instead. The failure is recorded in `/home/supadava/.cache/lor-fdtk-review3/rerun-summary.json`.

A focused touch probe and an isolated run of the unchanged script both succeeded. The instrumented run also succeeded: its selected flag was at y=178–312, above the sheet's y=538 top. Probe source, target bounds, before/after screenshots and the successful recording are in `/home/supadava/.cache/lor-fdtk-review3/sheet-target.mjs`, `sheet-target.json`, `sheet-target-before.png`, `sheet-target-after.png` and `sheet-instrumented/`. The exact cause of the batch failure is not established; do not hide it with a larger wait or a retry. Investigate target selection and gesture timing alongside the existing lab-input item `lor-m7r6`.

Deferred because all eleven required interactions pass, the carried settings defect is independently fixed, and focused checks show switching the open row works. This extra script's intermittent failure is not evidence of a remaining Panes interaction defect.
