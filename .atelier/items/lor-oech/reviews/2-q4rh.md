---
schema: atelier.review
schema_version: 3
round: 2
commit: 7f86160afb0f0a68fed93ddf46a05163f7872f37
reviewers:
- thread:delegated-task:command%3Amcp%3Aba6521a9-8596-4554-b3d2-ac22108b2ed5%3Adelegate-task%3Aa1b6a9cb-c936-4d53-abb9-2f79ae6bdef0
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T16:58:46.337525341Z
---

## Summary

Complexity: high. This defines the fixture contract shared by Android and wasm, with three later screen migrations depending on it.

Value: high. Named, reproducible states are needed to develop and audit screens without live accounts or hand-written layouts.

Risk of merging it: medium. Most of the change is development tooling, but the debug entry changes application service startup.

The value justifies the shape. Composable parts and wire-format replies keep the initial fixture contract small enough for the requested sources and both entry points. The two earlier findings are fixed. No new findings.

Reviewed `main...7f86160afb0f0a68fed93ddf46a05163f7872f37` and the changes since round 1 against the Done when lines and Report: fixture data and composition, JSON/hash/drop loading, the catalogue, demo generator and payloads, Android lifecycle and manifests, debug/release source sets, service gating and tests. Only the kit sheet has moved into homecore, so reserving the ten real-screen names for lor-xdoi, lor-rgx4 and lor-t8lq is consistent with this item's scope. STATES.md names their owners.

Independent checks:

- `pnpm test`: 206 passed. `git diff --check main...HEAD` passed.
- Ran `android/boat-build.sh` with `assembleDebug testDebugUnitTest testReleaseUnitTest :homecore:testAndroidHostTest`: 433 debug, 433 release and 23 homecore tests passed, with no failures or skips. This covers fixture and hash round trips, real wire parsers, catalogue agreement, service-gate orders and the absent release fixture entry. Reports are under `/home/supadava/.cache/lor-oech-review2-build/`.
- Cold-launched `overfull` through HomeActivity on boat's already-running T3_Fold_API35 using the device tools. The kit sheet and orange fixture banner appeared. The installed APK matches the fresh build's SHA-256 (`9328b0c64aee8b1aefd46497929cc4ff7ddf68c98dce27f1f8ead85c3b40fd3a`). The main process logged no layout read or app service startup during the fixture-only interval; opening the real HomeActivity in that same process then updated the periodic sync, read layout.json and began LoR reads. WorkManager's initializer independently re-registers existing work after force-stop; this is distinct from the app's deferred startup, and is documented in the Report. Logs: `/tmp/lor-oech-review2-evidence/cold-logcat.txt` and `main-logcat.txt`.
- Ran Chrome against the supplied current browser bundle, serving its files through request routing under the repository's lab CSP. On a plain HTTP hostname with no clipboard API, JSON opened a dialog with all 20,679 characters selected and no Copy button. On localhost, Copy wrote exactly that JSON and a rejected write displayed its error. Full fixture drops, a JSON patch containing slashes, a percent sign and an emoji, invalid-file errors and returning to a named state all passed without page errors. Reproduction and output: `/tmp/lor-oech-review2-evidence/browser.mjs` and `browser.txt`.

I did not rebuild the wasm bundle, reset the shared test database to rerun `demo-fixture.mjs --check`, connect to a real T3 server or test a physical phone. The checked-in demo replies parse with the actual app parsers. The emulator check establishes the app's startup order, rather than claiming to measure live T3 traffic or suppress already-scheduled background work.

## Finding 1: Keep cold fixture launches from starting the real app services
Recommendation: fix now
Was: 1-2l7g.1
Response: fixed in b53a9c0
Now: fixed

`LorApp.onCreate` now defers debug service startup to the first activity's pre-create callback. A fixture hand-off holds the gate past its background-entry timeout; a subsequent real activity starts services once. The separate fixture process still returns before service startup. `ServiceGateTest` covers these orders, and the cold-launch/control logs above show the intended distinction on the emulator. Release startup remains immediate.

## Finding 2: Make the JSON export work on the published lab's HTTP origin
Recommendation: fix now
Was: 1-2l7g.2
Response: fixed in b53a9c0
Now: fixed

The JSON button now opens a selected-text dialog on both origins. The Copy button is hidden without a secure-context clipboard API. Where available, its promise is awaited and rejection is shown. The independent browser checks above passed for the HTTP/manual-copy path, successful clipboard copying and rejection.
