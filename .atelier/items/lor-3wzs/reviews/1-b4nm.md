---
schema: atelier.review
schema_version: 3
round: 1
commit: 89546e726bbc703f304bdc5b9b5be028f31e4d64
reviewers:
- thread:delegated-task:command%3Amcp%3A86640e37-49c9-4a33-8e65-39d15b541dd8%3Adelegate-task%3A137ac895-32e8-44f2-90fb-21d4dfa5b401
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T17:05:15.515065922Z
---

## Summary

- **Complexity: high.** About 5,500 added lines implement five screens, three responsive layouts, mutable toast/layer state and custom browser-history rules.
- **Value: high.** This supplies the Panes direction required for the design comparison, including the ten dashboard interactions.
- **Risk of merging: low.** The direction runs on fixture data, is isolated from production, and is easy to revert.

The value justifies the implementation. Two targeted correctness fixes remain.

I read the brief, product spec, diff and Panes implementation at `89546e726bbc703f304bdc5b9b5be028f31e4d64`. Scope is correct: the new direction, its registration, the catalogue and the item report. The visual organisation, type scale, glass layers, common row actions and collapsed uncommon controls match the brief. The report identifies the small-laptop chrome tradeoff honestly; its assertion that dismissal is fully answered needs the correction below.

Validation:

- `tsc -p prototype/dashboard/tsconfig.json`, the prototype's Vite production build, and all 206 Vitest tests pass. Tests first encountered the known `/tmp` quota error; rerunning with `TMPDIR` under `~/.cache` passed.
- I replayed the author's interaction script at 1440 and 412, with mouse/keyboard and touch respectively. All ten required interactions pass, as do the script's extra simple Back, sheet, stacked-pane and Undo checks, with no page errors. I then wrote independent screen/state and edge-case checks.
- The six required screen views and four required states render at 1440, 1100 and 412 without horizontal overflow. The no-data goal, empty recorded day, quiet state and twelve flags are present. Stale mode disables the tested task, timer, label, habit and person writes while allowing reads. Captures and results are in `/home/supadava/.cache/lor-3wzs-review/`, including `independent.json` and `edges.json`.
- `scripts/shots.mjs --only 'panes: Tasks with a task open'` produced three images at the catalogue's 1500, 1100 and 412 widths with no problems. I inspected those and the required screen/state captures.
- Publishing to `/home/supadava/.cache/lor-3wzs-review/lab` succeeds. The built Panes direction loads through `labStatic` and its actual CSP, with all four expected font faces loaded, no errors, no CSP violations and no requests outside `/lab/`.

I did not publish the real lab, touch the running LoR service or test a physical phone/OS keyboard. This item is a browser prototype; its ten interactions were checked in a 412px touch context.

## Finding 1: Task titles cannot identify the open task after rename or duplicate creation
Recommendation: fix now

`prototype/dashboard/src/directions/panes/screens/Tasks.tsx:39` resolves `id` by the first matching title. Today and the stacked task panes use the same approach. Meanwhile `details.tsx:146` changes that title without preserving the selection, and `omnibox.tsx:136` makes the added task's Open action navigate by title instead of the ID returned by `addIntent`.

Two browser reproductions demonstrate the shared cause:

1. Open Tasks → Return the library books → More, change Title to `Return the library books renamed`, and press Enter. The row changes and the toast says Renamed, but the detail disappears because the URL still names the old title. See `rename.png` and `independent.json` in the evidence directory.
2. Add `Reply to the App Store review notes fri`, then tap the toast's Open. At both 1440 and 412 it opens the original flagged task with no due date, rather than the new unflagged task due Friday. Clicking either same-named row likewise resolves to the original. See `duplicate-1440.png`, `duplicate-412.png` and `edges.json`.

Keep internal selections attached to stable task identity, including search, nested panes and the added task's Open action. Existing human-readable fixture links can remain supported. Verify that rename keeps the pane open, Undo still follows that task, and each of two same-named tasks opens independently.

## Finding 2: Close reopens an old goal after leaving a stacked task for another goal
Recommendation: fix now

`prototype/dashboard/src/directions/panes/lib.ts:50` replaces only the current history entry when another root item is selected. When a task has been stacked over a goal, the old goal remains in the preceding history entry. `closeHere` at line 65 treats every marked entry alike and goes Back to that old goal.

Reproduced with mouse at 1440 and touch at 412:

1. From the Goals list, open Be a good friend.
2. Open its task Send Noor the photos from the hike.
3. Select Run a half marathon in March from the main goal list, which replaces the stack with that goal.
4. Press its Close button.

Instead of closing to the list, the pane changes back to Be a good friend (`#/panes/goals?id=Be%20a%20good%20friend`). `close-root-1440.png`, `close-root-412.png` and `edges.json` record the result. Browser Back has the same problem. This contradicts the report's rule that changing selected rows does not make Back walk through them, and directly exercises the brief's dismissal risk.

When switching the root selection, reconcile the nested history so that closing the new root returns to the list. Preserve the existing behaviour where closing just the stacked task returns to its parent.
