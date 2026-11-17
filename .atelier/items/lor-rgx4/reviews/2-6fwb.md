---
schema: atelier.review
schema_version: 3
round: 2
commit: 941d1b0fefd25b6eca878e8340d1507c0a71c7fe
reviewers:
- thread:delegated-task:command%3Amcp%3A814f7d9f-aa80-4cd9-9628-4d9cf7202f06%3Adelegate-task%3Af01f2000-25c8-45d8-b7c4-86c9bb898fd4
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T19:25:15.335456249Z
---

## Summary

- Complexity: high. Several UI surfaces move across a module boundary and share a state/actions contract between the phone and lab.
- Value: high. Drawing the real panels in the lab is necessary for the planned design work.
- Risk of merging: medium. Adapter mistakes can change the phone or mislead lab work, but the change is reversible and introduces no persistent write path.

The extraction is worth its size. Most drawing moves mechanically; I found no smaller change that provides the required shared surfaces. The previous blocking finding is fixed, and I found no new blocker.

I reviewed the round-2 diff from `9866e29` to `941d1b0`, checked the common state/actions contracts and app adapters against their former drawing code, and traced pairing, question reads, thread reads, held task writes and quick-send routing. The drawing uses no Context, LorClient, T3Connection or Prefs; reads and effects remain in the app. The shared kit replaces the duplicate sheet controls. The named states and catalogue links cover the requested surfaces and sizes, and lor-5fk6 retains the timing prerequisite. `git diff --check main...941d1b0` passed.

On boat I ran `android/boat-build.sh "$PWD" /tmp/lor-rgx4-review2-build assembleDebug testDebugUnitTest :homecore:allTests :homecore:wasmJsBrowserDevelopmentExecutableDistribution`. Gradle passed in 43 seconds (13 tasks executed, 84 up to date), including compilation and linking of the wasm tests. XML reports show 403 app tests and 66 homecore tests, with no failures, errors or skips; the latter includes both new LabPanelsTest cases. Browser test execution remains disabled as documented. I did not rerun release tests or a cold build. Fetching succeeded, but extraction hit the host's /tmp quota; I extracted the completed archive into `/home/supadava/.cache/lor-rgx4-review2/` instead. The build log and test XML are there.

Against that freshly built lab bundle I replayed approve, answer, timer and tick through scripts/lab/run.mjs. All passed with empty browser-error lists. I inspected the tick's final image: the task is checked and struck through. I also captured and inspected quick send with only a Maps project and with no projects, and the inner-screen long transcript. Maps appears without an invented assistant project, the empty server has no chips, and both sheets keep Send disabled until a destination exists. The transcript's last messages now show valid times through 14:38 in New York. Recordings, final images and these extra screenshots are in the same review cache.

I verified all 79 evidence artifact hashes (excluding the manifest's self-entry) and independently compared all 13 emulator image pairs using decoded, cropped PNG hashes. Every pair matches below row 130. I did not drive another emulator or pair one with T3. These pairs cover the Fold's cover and inner screens under the Report's lead-approved substitution; they do not prove before/after equality of paired T3 cards, messages or replies. Those paths are supported by code inspection, tests and lab gestures. Round 1's deferred locale-formatting issue remains unchanged and nonblocking.

## Finding 1: Quick send invents projects instead of drawing the fixture's project list
Recommendation: fix now
Was: 1-fxnt.1
Response: fixed in 8352a44: the sheet draws the fixture's `T3.projects`, each thread under the project its title names; LabPanelsTest covers an empty list and a project with no thread.
Now: fixed

`LabPanels.sheet` now passes the explicit project map to SheetUi and resolves fixture thread project titles through that map. Both regression tests pass. I reran the original Maps-with-no-threads reproduction against the new bundle and checked an empty project list too; neither invents assistant or enables an unroutable Send. The screenshots are `sheet-maps.png` and `sheet-empty.png` in the review cache above.
