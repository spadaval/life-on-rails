---
schema: atelier.review
schema_version: 3
round: 3
commit: 4644c7323aa16d20f6daee8ad9df55bb18dd2093
reviewers:
- thread:delegated-task:command%3Amcp%3A1b0930fd-268e-4a67-8421-33356629b7ae%3Adelegate-task%3Ac8420abf-ad82-4b00-9fc7-87d5ebc8d179
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-18T04:02:24.897897286Z
---

## Summary

- **Complexity: high.** About 4,700 added lines introduce Context grouping, navigation and detail state across nine Kotlin files, plus changes to shared lab code.
- **Value: high.** A working fourth direction is needed to compare the organisations in the agreed design brief.
- **Risk of merging: low.** The screens use fixtures and compile only into the lab; the shared store and browser changes are visible and easy to revert.

The value warrants the size. A static mock would not exercise the required interactions, and the implementation reuses the kit's store, navigation shell and sheets.

Reviewed `4644c7323aa16d20f6daee8ad9df55bb18dd2093` against the brief, Done when lines and Report. Read the Context Kotlin, registration, shared-store change, browser gesture change, catalogue and publish path, with particular attention to the changes since round 2. The new add applies its task and context link together and removes both on Undo. Its counter advances independently of the task list, so an undone task's id cannot be reused by the next add. Existing callers retain their default behaviour.

Replayed all twenty-two Context gesture scripts on the saved full boat bundle, with each script's documented fixture and the unnamed timer's JSON state. All pass. These cover the eleven required interactions, drawer swipe and icon drag, both quick-send outcomes, sync repair, timer switching, task moves and recent tabs. The fold checks preserve the thread, draft and scroll. The overscroll regression passes on both Context and current; Timeline's stale-write check also passes. All twenty-five runs have empty page-error arrays. Recordings, strips and final frames are in `/tmp/lor-8aqh-review-3/` and `/tmp/lor-8aqh-review-3-shared/`.

Independently replayed `add-goal.mjs` as well: an unknown #word is refused, an add with #offline opens under Offline maps, and after Undo a subsequent add without a goal opens under Loose ends. Its final screenshot visibly shows Second under Loose ends. Evidence is in `/tmp/lor-8aqh-review-3-targeted/add-goal/`.

`pnpm test` passes all 206 tests. Inspected the saved boat build log (BUILD SUCCESSFUL, 3 min 22 s) and all 150 test-suite XML files: 940 tests, zero failures, errors or skips. The Report names bafe37f9 as the build source; that commit differs from the reviewed head only in the item record. Used this saved bundle rather than independently compiling Kotlin or rerunning Android tests. Typecheck was not run; no TypeScript changed.

Confirmed 86 Context catalogue rows and saved captures: 68 dark, 9 pale and 9 mixed. Required named states and cover/inner screens are present. Inspected representative captures of quiet and crowded queues, failed sync, search with keyboard, both question kinds and the full home at font scale 1.3. The Report answers both stated design risks and identifies the made-up context relationships. The existing publish script copies the phone distribution under `/lab/phone/`; did not repeat the scratch publish or change the live lab.

No new findings. The carried finding is fixed.


## Finding 1: Undo the context link together with the added task
Recommendation: fix now
Was: 2-dlid.2
Response: fixed in bafe37f9. The cause was two: the link was set outside the add's Undo, and the kit reused ids from the list's size, so a stale link matched the next task. The kit's `addTask` now takes the concept's own state as part of the add (applied and undone together) and never reuses an id; Context files the link through it. `add-goal.mjs` now undoes an add with #offline, adds "Second tomorrow" with no goal, and checks it opens in Loose ends.
Now: fixed
`Drawer.kt` now passes the context link and its removal to `ConceptStore.addTask`, so one Undo reverses both. `Store.kt` generates ids with a monotonic counter instead of the list size. The expanded regression script passes on the saved boat bundle, including the second task without a goal; it appears under Loose ends rather than inheriting Offline maps.
