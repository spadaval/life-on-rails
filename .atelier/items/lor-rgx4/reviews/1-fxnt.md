---
schema: atelier.review
schema_version: 3
round: 1
commit: 9866e298ead7c763b7b888950395bab1243e6f1d
reviewers:
- thread:delegated-task:command%3Amcp%3A814f7d9f-aa80-4cd9-9628-4d9cf7202f06%3Adelegate-task%3Ad8b9dae8-b687-404b-b1d9-c7a0bc27e44b
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T19:14:01.904745770Z
---

## Summary

- Complexity: high. This moves several surfaces and their rules across a module boundary and introduces the state/actions contract that the app and lab must both keep.
- Value: high. Drawing the real panels in the lab is necessary for the planned design work.
- Risk of merging: medium. Wrong adapters can change the phone or mislead lab work, but this adds no persistent writes and is reversible.

The extraction is worth its size. Most of the drawing is a mechanical move, and I found no substantially smaller way to expose these surfaces. One lab adapter must be corrected before merge; the two other findings can wait.

I reviewed `git diff main...9866e298ead7c763b7b888950395bab1243e6f1d`, compared the moved drawing and pure rules with their former app implementations, and traced the app adapters, held writes, pairing, thread reads, question reads and quick-send callbacks. The drawing is in common code without the prohibited Android/service dependencies, and the kit parts are shared. The original read/effect keys and write paths are retained. `git diff --check` passed.

I ran `android/boat-build.sh "$PWD" /tmp/lor-rgx4-review-build assembleDebug testDebugUnitTest :homecore:allTests --rerun-tasks`. It passed in 1m22s, with 93 actionable tasks executed, including the wasm test link. The XML reports contain 403 app tests and 64 homecore tests, with no failures, errors or skips. The browser test task itself remains disabled as documented. The supplied before XML reports contain 433 app tests in each build type and 24 homecore tests. The Report's counts after the main merge reconcile. This review's forced run closes the Report's outstanding concern about an up-to-date combined run; I did not rerun release tests.

I verified the hashes of all 79 artifacts listed alongside the manifest (excluding the manifest's self-entry), reran all 13 before/after PNG comparisons, and confirmed zero differing pixels below row 130. I inspected representative lab, emulator and gesture images. I replayed approve, answer, timer and tick successfully against the supplied lab bundle, then built a fresh lab bundle from the reviewed checkout at `/tmp/lor-rgx4-review-lab` and replayed all four successfully again. The fresh bundle also reproduced Finding 1 against the merged fixture contract. Review artifacts are under `/tmp/lor-rgx4-review/` (the `head-*` directories are the fresh bundle's recordings); the new boat logs and test XML are under `/tmp/lor-rgx4-review-build/`.

The named states and catalogue links cover the requested panels, blocks, setup steps, connection states, long transcript, tabs and screenshot sheet. The timing handoff is recorded against lor-5fk6 as the revised criterion requires. I did not drive another emulator session or pair an emulator with T3. The supplied comparisons cover the Fold's cover and inner screen under the reported lead-approved substitution; they do not establish before/after equality of paired T3 cards, messages or replies. For those paths I checked the moved code, adapters, tests and lab gestures, rather than claiming live Android evidence.

## Finding 1: Quick send invents projects instead of drawing the fixture's project list
Recommendation: fix now

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/lab/LabPanels.kt:274` constructs projects from `listOf("assistant") + threads.map { it.project }` and fabricates their IDs. The merged fixture already supplies the actual project list in `T3.projects`. As a result, a project with no thread cannot appear, and a server with no assistant project always appears to have an Inbox. This makes the new lab surface disagree with the app, whose `QuickSendActivity.kt` passes `shell.projects` to `SheetUi`.

I reproduced this with the freshly built head bundle and the one-off state `{"base":"empty","t3":{"status":"LIVE","projects":{"p-maps":"Maps"},"threads":[]},"where":{"open":"QUICK_SEND","typed":"hello"}}`. `/tmp/lor-rgx4-review/sheet-projects-current.png` shows an assistant chip, “To assistant · inbox · a new thread”, and enabled Send; Maps is absent. The app given these projects shows Maps and asks the owner to pick a project because assistant is missing. Build the sheet from the fixture's explicit project map and resolve its threads against that map. Check both an empty project list and a project without a thread.

## Finding 2: The long-transcript fixture gives its last ten messages invalid timestamps
Recommendation: defer

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/lab/Parts.kt:228` places `i * 2` directly in the minute field for forty messages. Messages 30 through 39 therefore have times from `19:60:00Z` to `19:78:00Z`. `ThreadView.kt` calls `LocalClock.parse`, which returns null for these, so the initially visible end of the long transcript silently loses its time labels. This is visible in the supplied long-transcript inner-screen shot as well. Generate the timestamps by adding two-minute durations to an instant. This can wait because it affects fixture fidelity, not the phone's real transcripts.

## Finding 3: The new clock formatter changes the phone's localized date labels
Recommendation: defer

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/ui/LocalClock.kt:22` and `:44` hard-code English month and weekday abbreviations. The former `DateTimeFormatter.ofPattern("EEE d MMM")` and `ofPattern("d MMM")` use the phone's format locale. For 2026-09-01, I confirmed the former formatter gives `Tue 1 Sept` in en-GB and `mar. 1 sept.` in fr-FR; the common formatter gives `Tue 1 Sep` in both. This affects Today's header and older thread/message dates. The Report discloses the en-GB difference, so the unchanged-look claim is correctly limited to the en-US emulators. Restore platform formatting or carry the app's formatted labels into common state when this formatting is next touched; this small locale difference need not block the extraction.
