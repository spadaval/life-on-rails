---
schema: atelier.review
schema_version: 3
round: 2
commit: bd3d81ee801290c30b5792338cbefd78ee8aa880
reviewers:
- thread:delegated-task:command%3Amcp%3Ab901dad6-ca34-44dd-a709-7bf3d9c92e90%3Adelegate-task%3Af3fcaa64-a868-4110-9db6-2245ced8dcb2
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T16:30:33.084370833Z
---

## Summary

Reviewed `main...bd3d81ee801290c30b5792338cbefd78ee8aa880` and the changes since round 1 against the Done when lines, the brief's Timeline and common-ground sections, and the Report. Read the direction, its shared store/navigation contracts, registration, catalogue and existing publisher. Its isolated route, untimed column, popover/sheet editing, required screens and linked states implement the requested concept. The Report explains the ten interactions and addresses the route's unlabelled-day risk with before/after screenshots.

- **Complexity: high.** About 5,000 added lines implement URL-driven editors, geometric placement and direction-local planning/feedback state beside the shared store.
- **Value: high.** The complete interactive direction is needed for the owner's design comparison.
- **Risk of merging: low.** It is an isolated prototype on made-up, tab-local data; a failure is visible and reverting it is cheap.

The value warrants the size. I do not see a much smaller change that delivers the requested complete concept. Findings 2–4 are fixed. Finding 1's original running-timer sequence works now, but its loss of block identity remains for earlier blocks in the same minute. Finding 5 is a newly reproduced phone feedback/layout problem. Both should be fixed before presenting the direction as complete.

Validation against the freshly built prototype on this checkout's already-running preview server at `127.0.0.1:3863`:

- `node_modules/.bin/tsc --noEmit -p prototype/dashboard/tsconfig.json` and `pnpm proto:build` passed. pnpm emitted its existing symlinked-node_modules install warning before running the successful build.
- Drove the ten dashboard interactions at 1440 and 412 px: parsed task creation, tick/untick, due-date change, flagged Done, timer start/stop, relabel, search/open, person, timer switch, and both the Undo button and Cmd-Z. Also exercised timer start/switch/stop away from Today. The ordinary flows pass after dismissing the notice that exposes Finding 5. The live-clock multi-switch/closed-block sequence exposes Finding 1 at 1440, 1100 and 412. No page errors were reported.
- Rechecked Today's task-editor bounds at all three requested widths, stale navigation through every page and a person search result, and disabled task planning. Also checked both plan-removal controls with an existing plan and the stale link-opened gap planner. The popover now fits the viewport; these stale writes stay disabled.
- Captured all 15 Timeline catalogue entries with `node scripts/shots.mjs --catalogue shots/prototype.json --base http://127.0.0.1:3863`, one entry per invocation into separate scratch directories: 45 images at 1500/1100/412, all batches reporting `problems: []`. An initial separate `pnpm shots` invocation returned screenshot-write error -122; the complete run of the same runner subsequently succeeded. Also opened and captured all required screens at 1440 and checked for horizontal page overflow.
- Inspected the linked quiet, empty, stale and crowded layouts. The existing publisher is unchanged and its prototype source includes the new build. I did not rerun publication or replace the real `/lab/` or shared screenshot folder; the scratch publication receipt from round 1 remains applicable. A combined real gallery still needs the post-merge run described in the Report.

Evidence and reproducible browser scripts are in `/tmp/lor-review-8mbs-r2/`: `check.mjs` / `check.json`, `flows.mjs` / `flows.json`, `notice-and-screens.mjs` / `notice-and-screens.json`, `catalogue.json`, `catalogue-0` through `catalogue-14`, and the named screenshots below. No production server/database or Android behavior was changed or exercised.

## Finding 1: Keep the running block's identity when opening Switch
Recommendation: fix now
Was: 1-86sj.1
Response: fixed in f023afa
Now: kept

The original repeat-Switch sequence now works, but the identity problem remains. `prototype/dashboard/src/directions/timeline/screens/Today.tsx:327` still writes only `hm(b.t_start)` when a block is pressed. Lines 166–168 now resolve that minute to its newest block. This makes the running block reachable by sacrificing the ability to select earlier blocks that started in the same minute.

Reproduced at 1440, 1100 and 412 with the live clock: load `#/timeline/?shell=0`, switch the initial timer to Public beta, then switch it to Meetings within the same minute. Close the editor and press the newly closed Public beta block. The editor says “Switch from Meetings”, and the running Meetings block is selected. The clicked closed block cannot be relabelled; the editor instead offers to switch or relabel unrelated running time. This is the same lost-identity cause as round 1, now breaking required interaction 6 after interaction 9. Resolve click targets by block id or a timestamp that preserves seconds; retain the minute-based catalogue link as a separate lookup path.

Evidence: [closed Public beta opens Meetings at 1440](/tmp/lor-review-8mbs-r2/same-minute-closed-1440.png), [the same at 412](/tmp/lor-review-8mbs-r2/same-minute-closed-412.png). `check.mjs` reproduces it and `check.json` records all three widths.

## Finding 2: Keep Today's task popovers out of the column's clipping area
Recommendation: fix now
Was: 1-86sj.2
Response: fixed in f023afa
Now: fixed

`Popover.tsx` now portals desktop editors outside the scrolling columns, positions them against the viewport and constrains their height with internal scrolling. Reopened the flagged task on Today and changed its due date at 1440, 1100 and 412. Bounds were inside the visible viewport at every width (1440: x=722, y=418, width=380, height≈526; 1100: x=426 with the same size). Close and lower controls are available. Evidence: `task-open-1440.png`, `task-open-1100.png`, `task-open-412.png` and the bounds in `check.json`, all under `/tmp/lor-review-8mbs-r2/`.

## Finding 3: Preserve the unreachable state when navigating within Timeline
Recommendation: fix now
Was: 1-86sj.3
Response: fixed in f023afa
Now: fixed

The direction's new `lib/nav.ts` retains its own `state` parameter, and its page/detail/search paths use that wrapper. From stale Today, navigated Tasks → Goals → People → Notes → Today, then searched for and opened Maya's person page at 1440, 1100 and 412. Every URL retained `state=stale`, the warning remained and write controls stayed disabled. See `check.mjs` and `check.json`.

## Finding 4: Disable planning writes while the stale banner says changes are off
Recommendation: fix now
Was: 1-86sj.4
Response: fixed in f023afa
Now: fixed

TaskEditor plan chips and its removal button, Today's route removal button, and GapPlanner candidates now carry the stale disabled state. Checked all four entry points, including an existing plan before entering stale mode and a gap planner opened through its URL. None allowed a planning write. See `check.json` and `flows.json`.

## Finding 5: Keep phone Undo notices from covering task due-date controls
Recommendation: fix now

`prototype/dashboard/src/directions/timeline/styles.css:874–894` centers `.tl-notice` with `left: 50%` and a transform, but gives it only `max-width`, leaving its used width to shrink-to-fit. At 412 px it becomes roughly 237 px wide, wrapping a normal task-change message into five lines. Its z-index of 70 also places it above the bottom sheet (60), directly over the editor's active controls.

Reproduced on the existing fixture: open Tasks at 412×950, tick “Fix the dripping bathroom tap”, untick it in Done today, then open that task immediately. The eight-second “Not done” Undo notice occupies x≈88–324 and y≈815–934, covering Tomorrow, Mon 23 and No date. Hit testing the centre of Tomorrow returns the notice's text; the centre of Mon 23 hits Undo, so a tap intended to set that date instead ticks the task again. Browser clicks on Tomorrow fail because the notice intercepts them. Dismissing the notice allows the flow to finish, but routine feedback should not obstruct or substitute another action for the next required interaction.

Give phone feedback the intended available width and place/reserve it so it cannot cover the open sheet's controls, while keeping Undo usable. Evidence: [notice covering the due-date controls](/tmp/lor-review-8mbs-r2/notice-covers-due-412.png), with rectangles and hit targets in `notice-and-screens.json`.
