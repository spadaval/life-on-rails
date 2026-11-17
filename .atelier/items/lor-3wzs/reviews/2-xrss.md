---
schema: atelier.review
schema_version: 3
round: 2
commit: 3b5be13bb379519baef044892bd0de44940320ff
reviewers:
- thread:delegated-task:command%3Amcp%3A86640e37-49c9-4a33-8e65-39d15b541dd8%3Adelegate-task%3A05c33c53-a0d8-4e6f-b71c-aff4f9595173
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T17:16:11.461647736Z
---

## Summary

- **Complexity: high.** The direction adds five screens, responsive pane layouts, mutable feedback state and custom browser history across about 5,500 lines.
- **Value: high.** It supplies the Panes direction and dashboard interactions needed for the design comparison.
- **Risk of merging: low.** It uses fixture data in an isolated prototype and can be reverted without affecting production data.

The value justifies the implementation. The history fix holds, and ID-based selections fix the original row and duplicate-task reproductions. A supported title-link path still loses its selection on rename, and the configured merge check fails on two trailing blank lines.

I read the product spec, design brief, implementation, round 1 review and changes from `89546e7` to `3b5be13`. The scope remains the direction, registration, shots catalogue and Atelier records. The layout, type scale, layering and collapsed uncommon controls match the brief; the report explains the small-laptop chrome tradeoff and provides screenshot references.

Validation on this checkout:

- `tsc -p prototype/dashboard/tsconfig.json`, the prototype Vite production build, and all 206 Vitest tests pass, with `TMPDIR` under `/home/supadava/.cache/lor-3wzs-review2/`.
- Using T3 Code's browser at port 3863, I exercised the ten dashboard interactions at 1440 and 412, including add, tick/untick, due-date changes, flag Done, timer start/stop/switch, relabel/Undo, search and person selection. Reversible changes offered Undo; timer changes and Ask did not. I also checked phone half/full sheet controls and desktop Escape through stacked panes.
- At both widths, a task opened from its row retains its ID through rename and Undo. Adding a task with the existing App Store task's title opens the new Friday task; the same-named rows open different IDs with their respective due dates.
- Switching from a task stacked over Be a good friend to Run a half marathon in March now makes both Close and Back return to the goal list. Closing only the stacked task still leaves its parent open.
- The six required screen views and four states render at 1440, 1100 and 412 without horizontal overflow. Afternoon has two flags, crowded has twelve, quiet and empty show their expected copy, and the goal shows No data yet. Stale mode disables the tested row and task-editor writes while search remains usable and preserves the stale banner.
- The shots runner, with only `panes: a task stacked over its goal`, captured 1500, 1100 and 412 with no problems. I inspected all three images under `/home/supadava/.cache/lor-3wzs-review2/shots/`. Publishing the built prototype and galleries to `/home/supadava/.cache/lor-3wzs-review2/lab` succeeds.

I did not publish the real lab, touch the running service, test a physical phone or rerun round 1's built-lab CSP browser check. Browser interaction checks used preview actions and DOM events at the stated viewport sizes. The merge check is covered by Finding 3.

## Finding 1: Task titles cannot identify the open task after rename or duplicate creation
Recommendation: fix now
Was: 1-b4nm.1
Response: fixed in 8a4a052
Now: kept

The row, search and add paths now use IDs, and the original duplicate-task reproduction passes. The title fallback in `prototype/dashboard/src/directions/panes/lib.ts:63`, used by `screens/Tasks.tsx:39`, still resolves the selection afresh against the mutable title on each render. It never attaches an incoming title-based selection to the resolved ID.

At both 1440 and 412, open `http://127.0.0.1:3863/#/panes/tasks?id=Return%20the%20library%20books`, expand More, change Title to `Return the library books renamed`, then blur the field. The toast says Renamed and the row changes, but the detail disappears. The URL still contains the old title. Undo restores the detail. This is the supported title-link path retained for fixture links, not an invalid task ID.

Evidence: `/home/supadava/.t3/userdata/browser-artifacts/browser-screenshot-localhost-mwkyn1np-16552bc4.png` (1440) and `/home/supadava/.t3/userdata/browser-artifacts/browser-screenshot-localhost-mwkyn252-733e9c3f.png` (412).

Resolve a valid incoming title link to stable identity before edits can change it, preserving the history and close rules. Check this for root and stacked task selections as well as the already-fixed UI-origin selections.

## Finding 2: Close reopens an old goal after leaving a stacked task for another goal
Recommendation: fix now
Was: 1-b4nm.2
Response: fixed in 8a4a052
Now: fixed

Verified at 1440 and 412: open Be a good friend, stack Send Noor the photos from the hike, select Run a half marathon in March from the goal list, then Close. The result is the goal list with no detail. Repeating with browser Back gives the same result. Closing just the stacked task returns to Be a good friend. The entry count and removal of the stacked entry in `lib.ts` address the reported cause.

## Finding 3: Two trailing blank lines fail the required merge check
Recommendation: fix now

`prototype/dashboard/src/directions/panes/details.tsx:380` and `prototype/dashboard/src/directions/panes/screens/Today.tsx:272` add blank lines at EOF. `git diff --check main HEAD -- . ':!.atelier'` exits 2 and reports both lines.

The repository's `.atelier/config.toml` requires `git diff --check HEAD^ HEAD -- . ':!.atelier'` on the merge commit. Its first parent is main, so these additions fail that check even though the branch's latest commit changes only Atelier records. Remove the two extra blank lines and verify the complete item diff against main passes.
