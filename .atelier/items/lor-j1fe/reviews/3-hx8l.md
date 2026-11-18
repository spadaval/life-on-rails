---
schema: atelier.review
schema_version: 3
round: 3
commit: a7e79e395e47b31f493b33ff506bf993a659fb21
reviewers:
- thread:delegated-task:command%3Amcp%3A5b22cd2c-aeb6-4101-9cad-9bbd50cb72d6%3Adelegate-task%3Acce105b8-fda8-4b6e-8449-7beaa15454e0
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-18T00:34:05.192997384Z
---

## Summary

- **Complexity: high.** The whole change adds a day model, twelve Compose files, eighteen gesture scripts and selective concept compilation with generated registration.
- **Value: high.** These screens and interactions are needed to compare Timeline with the other phone directions.
- **Risk of merging: low.** This is a fixture-backed browser concept with visible failures, reversible changes and no live writes or changes to Android's shipped screens.

The value justifies the scope. Compiling one concept at a time addresses the measured memory constraint without changing its cap. I found no substantially smaller change that would deliver the required comparison. One additional unavailable-state defect needs correction.

I reviewed `main...a7e79e395e47b31f493b33ff506bf993a659fb21`, concentrating on the changes since round 2, against the item, design brief and product rules. I read the Timeline model and screens, the shared store/menu/shell integration, generated registration, selective-build script and publishing path. The required named states and screens are represented. The catalogue has 74 Timeline entries: 58 dark, eight pale and eight mixed. The Report discloses the additional block fixtures and build-selection scope change. I inspected its mostly-unlabelled-day and empty-day risk screenshots, plus the crowded queue and larger-text day page. The screenshots support the stated design risk.

I rebuilt this commit with `android/web-dev.sh --only timeline` in its capped scope: compilation and webpack succeeded (Gradle reported 41 seconds). All eighteen supplied gesture scripts passed through CDP touch events using `scripts/lab/gesture.mjs`, with twenty cases including the inner-screen drawer and disconnected quick send. This covers the eleven required interactions, both fold directions with draft and scroll retained, timer endpoints and Undo, planning/unplanning, relabelling, answer cancellation, and stale controls. The drawer, move, timer and stale-inner runs include recordings. Other successful runs have final screenshots and anchor dumps in `/tmp/lor-review-j1fe-round3/`. My long custom batch lost Chrome during quick send; I ran that case and the remaining cases in a separate batch, where they passed. No product assertion failed in these runs.

Additional touch checks reproduced Finding 3. `pnpm test` passed all 206 tests in 23 files, and `git diff --check main...a7e79e39 -- . ':!.atelier'` passed. I did not repeat boat's full build/Android tests, the long-session memory measurements, all 74 captures or the scratch publishing experiment. The local build excludes Queue; its sources are unchanged. No production service, real phone or emulator was exercised. I did not run TypeScript checks for this Kotlin/script/catalogue change.

## Finding 1: Preserve exact timer endpoints instead of adding a minute
Recommendation: fix now
Was: 2-wum1.6
Response: fixed in d76cef89: a timer's block ends at the moment it stopped, so a quick switch shares its boundary with the next; `timer.mjs` checks both blocks' exact endpoints.
Now: fixed

Verification: `DayModel.kt` now keeps the stop instant in `DayBlock.end` and uses millisecond-based timer ids. The minimum drawing span no longer changes the recorded block's endpoint. The rebuilt timer script passed its checks for short completed sessions, their shared switch boundary and Undo. Short durations now read `<1m` or `under a minute` rather than a fabricated full minute.

## Finding 2: Disable the inner screen's task ticks while LoR is stale
Recommendation: fix now
Was: 2-wum1.7
Response: fixed in d76cef89: the inner screen's task ticks are disabled while LoR isn't answering; `stale-inner.mjs` checks it (and the same tap saves when LoR is live).
Now: fixed

Verification: `DueRow` passes `enabled = store.lorLive` to `Check`, matching the cover's untimed sheet and task detail. The rebuilt stale-inner script tapped the task tick without producing a successful Done/Undo notice. Ordinary task ticking and Undo still pass. Finding 3 concerns other task mutation entry points.

## Finding 3: Refuse task changes from search and menus while LoR is unavailable
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Detail.kt:217` and `:224-225` create enabled due-date menu actions regardless of `store.lorLive`. `Drawer.kt:200` likewise adds a task without checking availability. `ConceptStore.setDue` and `addTask` do not guard these calls. These paths therefore claim a successful save in the same state where task ticks, detail date choices, relabelling and planning correctly refuse writes.

Reproduced on the rebuilt commit:

1. Open `#timeline/stale/inner/dark`, swipe to the day page and scroll the right pane to ordinary tasks. Open the More menu for `task:00000000-0000-7000-8000-000000000016` (Write release notes for beta 0.4), then tap **No date**. The task disappears from Due, no time yet, its count drops from seven to six and a **No due date / Undo** notice appears beside “LoR isn't answering.” See `/tmp/lor-review-j1fe-round3/stale-before-menu.png`, `stale-menu.png` and `stale-after-date.png`, with matching anchor dumps.
2. Open `#timeline/{"base":"stale","where":{"open":"SEARCH","typed":"test today"}}`, then tap **Add task**. The query clears and **Added “Test” / Undo** appears despite the unavailable server. See `stale-search-before.png` and `stale-search-after.png` in the same directory.

Enforce availability at the task mutation boundary used by Timeline, and have the menu and search controls follow it. Exercise those paths alongside the checkbox so another presentation cannot silently bypass the unavailable-state rule.
