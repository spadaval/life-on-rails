---
schema: atelier.review
schema_version: 3
round: 2
commit: ba1776783ed50e913c9f554e972515778763fc5b
reviewers:
- thread:delegated-task:command%3Amcp%3A9355adb5-fe8e-4a8f-bf7c-ba1c83170f37%3Adelegate-task%3A9f025a73-eb86-4899-8543-cc7d69017261
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T20:38:23.048774949Z
---

## Summary

Complexity: high. The launcher now crosses a multiplatform boundary, with a new JSON compatibility reader, platform adapters and fixture state.

Value: high. Sharing home's actual rendering and gestures is necessary for the browser lab and editor.

Risk of merging: medium. Parser and platform regressions would reach the phone, but the saved layout is guarded by parse-back checks and the extraction can be reverted.

The value warrants the extraction. I found no substantially smaller change that delivers the same outcome. All five earlier findings are fixed, and I found no additional blocking problem.

I reviewed `main...ba1776783ed50e913c9f554e972515778763fc5b` and the round-2 delta, including the moved surfaces, common model/parser, Android and wasm adapters, fixture requests, test migration, and build/gesture scripts. I checked the Done when lines and Report against the code and saved evidence.

Independent verification:

- Compiled the current standalone JSON reader with the cached Kotlin compiler and exercised plain/BOM input and the nesting boundary. Both object inputs parse alike; depth 500 parses and 501 raises `JSONException`.
- Ran all nine submitted home gesture scripts through the actual `scripts/lab/run.mjs` against the saved b6 production bundle: drawer, pages, app-menu, folder, drift, search, back-folder, minimap, and wallpaper-keeps. All passed. An additional script verified that Undo still restores Spotify and Strava after changing the wallpaper. The runner also completed a one-off JSON minimap run containing both a literal `%` and `%22`. Recordings and results are under `/home/supadava/.cache/lor-xdoi-review-round2/`.
- Loaded the same bundle in a Chrome context set to UTC. The demo fixture's quick dial reads "Tuesday afternoon" and retains its fixture ranking, as shown in `/home/supadava/.cache/lor-xdoi-review-round2/drawer-utc.png`.
- Compared all 21 saved emulator before/after PNG pairs independently; every pair matches below row 110. The saved before/after layout writes are byte-identical.
- Inspected the saved before, b5 and b6 test XML and build logs. b6 has 131 debug app tests, 131 release app tests, 340 homecore host tests and 338 Node tests, with no failures, errors or skips. The new complete-layout BOM test passed on both homecore targets. The b6 build log ends successfully after both test targets and the production browser distribution.

I did not rebuild Android or wasm, start an emulator, repeat the cold-start/APK measurements, or repeat the edit-time/memory measurements. The Report explicitly identifies the Android named-state entry as untested on a device and the final b5 tree as not reshot on the Fold. Finding 5's fix is verified by the call paths and explicit fixture enablement, not a device run. My initial custom browser harness was not equivalent to the runner; its drag failures were excluded from the result above, and the submitted runner was used for the gesture verdict.

## Finding 1: Preserve leading BOM support when replacing Android's JSON reader
Recommendation: fix now
Was: 1-cd0k.1
Response: fixed in 0de3e66 (the tokener drops a leading U+FEFF as Android's does; `JsonTest` parses a full layout with and without it, on both targets)
Now: fixed

The tokener removes only an initial U+FEFF. The common regression test compares a complete valid layout with and without it and checks that a BOM inside a string stays; the saved b6 results show it passes on both targets. The independent compiled probe also passes.

## Finding 2: Compare both gesture-runner hashes in the same encoding
Recommendation: fix now
Was: 1-cd0k.2
Response: fixed in 0de3e66 (run.mjs decodes both sides, keeping a literal %; a one-off `{"base":"demo"}` minimap run and the named `demo` both pass, `evidence/review-1/`)
Now: fixed

The readiness predicate decodes both operands and preserves undecodable literal percent signs. The actual runner passed both named demo runs and a one-off JSON run containing literal percent text.

## Finding 3: Keep lab edits and Undo when the wallpaper changes
Recommendation: fix now
Was: 1-cd0k.3
Response: fixed in 0de3e66 (one platform per fixture, the ink a flow the wallpaper sets; `gestures/home/wallpaper-keeps.mjs` makes a folder, switches to pale, and the folder stays)
Now: fixed

LabPlatform is remembered by fixture/apps, while wallpaper changes update a separate ink flow. The submitted wallpaper-keeps script passes, and the additional runner script proves Undo still restores the original apps afterward.

## Finding 4: Use the fixture's time zone for quick dial's header and ranking
Recommendation: fix now
Was: 1-cd0k.4
Response: fixed in 0de3e66 (quick dial's builder takes `HomePlatform.zone`; with Chrome in UTC the demo drawer reads "Tuesday afternoon", `evidence/review-1/utc-drawer`)
Now: fixed

The builder now receives HomePlatform.zone, and the remembered inputs include that zone. The independent UTC browser check shows Tuesday afternoon for the New York fixture.

## Finding 5: Enable named-state requests in the Android fixture process too
Recommendation: fix now
Was: 1-cd0k.5
Response: fixed in 0de3e66 (`HomeUi(fixtureRequests = true)` from LabHome on both lab hosts gates the two effects; the phone's HomeUi leaves it false; still not run on a device, as the Report says)
Now: fixed

LabHome constructs HomeUi(fixtureRequests = true) on both lab hosts. AppMenu and Drawer now use that flag for the requests; the ordinary HomeActivity still constructs HomeUi() with its false default. Android LabStateActivity reaches the same LabHome through the current version. No Android fixture run was performed.
