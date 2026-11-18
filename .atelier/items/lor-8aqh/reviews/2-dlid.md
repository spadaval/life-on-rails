---
schema: atelier.review
schema_version: 3
round: 2
commit: 32442099801a5ba965821dfdda481ec32b353e0c
reviewers:
- thread:delegated-task:command%3Amcp%3A1b0930fd-268e-4a67-8421-33356629b7ae%3Adelegate-task%3Ae97bf017-00a9-459c-a143-6895949d5624
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-18T03:48:39.365078417Z
---

## Summary

- **Complexity: high.** About 4,700 added lines introduce a Context model and navigation, search, timer and detail state across nine Kotlin files.
- **Value: high.** The fourth working direction is needed to compare the organisations in the agreed design brief.
- **Risk of merging: low.** This is fixture-backed lab code; the shared browser gesture fix is visible and easy to revert.

The value warrants the size. A smaller static mock would not exercise the required interactions, and the implementation already reuses the kit's store, shell and sheets.

Reviewed the branch at `32442099801a5ba965821dfdda481ec32b353e0c`, including the Kotlin, kit contracts, registration, catalogue, brief and Report, with particular attention to the changes since round 1. All twenty-two Context scripts pass on the saved full boat bundle, using their documented states. The overscroll regression also passes on current and Context. Every run has an empty page-error array. Recordings, strips, final frames and logs are in `/tmp/lor-8aqh-review-2/`. These exercise all eleven required phone interactions, including the drawer swipe, icon drag, thread reply, failure paths and retained thread, draft and scroll across folding. The task Move and recent-tab changes also pass their scripts.

`pnpm test` passes, 206 tests. Inspected the saved boat build log (BUILD SUCCESSFUL, 4 min 39 s) and its 150 test-suite XML files: 940 tests, zero failures, errors or skips. The reported build source `557a1acb` differs from the reviewed head only in the item record. Used that saved bundle; did not independently compile Kotlin, rerun Android tests or run typecheck.

Confirmed 86 Context catalogue rows (68 dark, 9 pale, 9 mixed) and 86 saved captures, with the required named states, cover/inner layouts, questions, settings, timer and thread. Inspected representative screen and risk captures, including the crowded queue, failed sync, both question kinds and all 24 home icons at font scale 1.3. The Report answers both stated design risks and acknowledges the made-up relationships. The existing publish script includes the phone distribution under `/lab/phone/`; did not repeat the scratch publish or touch the live lab.

The original goal-loss path is fixed, but the added relationship does not participate in Undo. The extra touch sequence below reproduces incorrect grouping after Undo without page errors.

## Finding 1: Save the goal that search previews when adding a task
Recommendation: fix now
Was: 1-2l4w.1
Response: fixed in 557a1acb. Search resolves a typed `#word` to a project or goal (its exact title, else the one title word it starts), shows that context's name in the preview, and Add puts the task under it; Undo removes the task. A `#word` that names no context shows "No context matches #word" with what to do, and Add is not offered. `add-goal.mjs` checks both.
Now: fixed

`add-goal.mjs` passes: an unknown #word is refused, and a task added with #offline opens under Offline maps. The previewed relationship is now retained. Finding 2 concerns its rollback.

## Finding 2: Undo the context link together with the added task
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/context/Drawer.kt:227-229` assigns `ui.links[id]` after `store.addTask` has installed its Undo. That Undo restores only `store.tasks` (`kit/Store.kt:205-208`), leaving the context link behind. IDs are derived from the list size, so the next add reuses the undone task's ID. An unrelated task added without any #goal silently inherits the discarded task's context.

Reproduced on the saved boat bundle: search for `First tomorrow #offline`, tap Add task, tap Undo, then add `Second tomorrow` with no #goal. Search for Second and open it: it appears under Offline maps instead of Loose ends. The add and link need one rollback that removes/restores both pieces of state. A regression check should cover adding without a goal after undoing an add with one; the existing add-goal script stops before exercising Undo.

Evidence: `/tmp/lor-8aqh-review-2/undo-inherited-context.png` shows Second under Offline maps; `/tmp/lor-8aqh-review-2/undo-add/` contains the recording and anchors. The touch-only reproduction script is `/tmp/lor-8aqh-undo-review.mjs`.
