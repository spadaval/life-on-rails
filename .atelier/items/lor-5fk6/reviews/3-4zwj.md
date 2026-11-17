---
schema: atelier.review
schema_version: 3
round: 3
commit: 16bcc3b699a4e68ee7981bea7c52b6e03d99736a
reviewers:
- thread:delegated-task:command%3Amcp%3Aa3fc2d13-90ae-4716-8d51-b28bcfda6fa8%3Adelegate-task%3Acae6812c-7e1d-47fa-a512-3e0f0dbfdabc
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T23:24:58.219211190Z
---

## Summary

Complexity: high. The complete change adds a browser-only concept and reusable interaction kit across roughly 4,600 lines, plus shared fixture, registry, anchor and viewport support.

Value: high. The mission needs a usable first phone direction for comparison, and the kit supports the remaining directions.

Risk of merging: low. The concept changes browser fixtures, sends nothing, and is easy to revert. The app's screens are unchanged.

The value warrants the size. I found no substantially smaller change that delivers the required screens, touch interactions and reusable pieces. The carried fix-now finding is fixed. The additional keyboard finding below can follow later because tapping search results works and the required phone interactions pass.

Reviewed `main...16bcc3b699a4e68ee7981bea7c52b6e03d99736a`, the round-3 delta, the acceptance criteria and `docs/design/lab/brief.md`. Checked browser-only source placement, registration, shared fixtures, queue ranking, completion and Undo, search/detail resolution, timer clock, sheets, fold state, settings, screenshot coverage and publish routing. The catalogue has 72 Queue rows and all 72 submitted PNGs exist. The supplied boat XML receipts total 940 tests with zero failures, errors or skips; I did not repeat the Android build.

Built the reviewed commit locally with `android/web-dev.sh` in its capped scope: successful in 38 seconds. Replayed the submitted Queue touch helpers and assertions in fresh Chrome contexts: all 19 runs passed with no page errors. These include searched flagged and undated tasks on both screen sizes, delayed timer start/readout agreement, both fold directions with draft/scroll preservation, approval, free-text answer, reply, drag, tick/Undo, sync repair, and both quick-send outcomes. Independently reopened Settings through home's Fix after the Sensors section: the failed-sync row and Sync now return into view. Timer search opens the timer. Resizing a touch viewport from 412 by 900 to 875 by 971 and back preserves the open thread and seven-character draft, fills the viewport, and produces no sideways scrolling or page errors; I inspected the resulting inner-screen capture.

`pnpm test` passes all 206 tests. `pnpm typecheck` still fails on missing `src/routeTree.gen` and subsequent route typing errors, matching existing item lor-iurl; there are no TypeScript application changes here. `git diff --check main...HEAD` passes. I inspected the publish path and supplied evidence but did not independently repeat the scratch production publish, first-load measurements, edit timings or long-session memory measurement. Real Tailscale load and the physical Fold check remain for the lead/owner as the item expressly allows. The compiler workaround and near-cap memory use are disclosed in the report.

This round's screenshots and anchors are under `/tmp/lor-5fk6-round3/`; gesture results are in `/tmp/lor-5fk6-round3-results.log`. Extra settings, viewport and keyboard checks are recorded in the adjacent `-extra.log`, `-fill.log` and `-enter.log` files.

## Finding 1: Completing a searched flagged task must also clear its queue flag
Recommendation: fix now
Was: 2-5x97.4
Response: fixed in beb1f778: a flagged task opens as its flag from search, and the store's tick completes a flagged task as flagDone does, with one Undo; `search-flag.mjs` checks it leaves the queue and Undo restores it.
Now: fixed

`ConceptStore.item` resolves a searched task to its existing flag before falling back to the complete task list. `ConceptStore.tick` also delegates flagged completion to `flagDone`, clearing Needs you and marking the task done in one Undo operation. The new search-flag regression passes on both cover and inner screens: Done removes the queue flag and Undo restores it. The undated search-task regression still passes on both screens, including changing its date.

## Finding 2: Search's Enter key does not open the matching app
Recommendation: defer

In `android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/queue/Detail.kt:69-76`, `Field` publishes `ui.onEnter` only when focus changes, while the search callback in `Drawer.kt:79-88` closes over that composition's `query`. When home opens search, that query is empty; typing updates the visible results without refreshing the stand-in keyboard's callback. The physical Enter path also fails to run the search action in the browser.

Reproduced on `#queue/demo`: tap home's search, type `spo`, and press physical Enter. Spotify stays in the results and no launch notice appears. Refocus the search and tap the drawn keyboard's Enter key: the keyboard closes, but Spotify still has not opened. See `/tmp/lor-5fk6-round3-enter.log`, `app-enter.png` and `app-enter.json`. This contradicts the report's claim that Enter does the first result. Wire both Enter paths to the current search action and verify it after typing without moving focus.

Deferred because tapping Spotify works, the required search/open-app touch script passes, and this remains a fixture-only prototype. It does not block comparing the direction's required screens and interactions.
