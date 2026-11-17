---
schema: atelier.review
schema_version: 3
round: 1
commit: d43a8e3f3a8b972041d4274c1582f7557af5b173
reviewers:
- thread:delegated-task:command%3Amcp%3Ab81bed6f-1d6d-44e2-9a3a-5ce4da3e090f%3Adelegate-task%3A52a10692-d38f-4c72-81ea-a7ed77340f04
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T16:16:13.178504126Z
---

## Summary

Complexity: high. This is about 4,400 lines across a new direction, with its own queue state, toast/Undo handling, floating layers and five screens.

Value: high. A working Queue direction is needed to compare the dashboard concepts on the brief's common interactions.

Risk of merging: low. This is an isolated prototype over made-up data, with visible failures and an easy revert; it changes no production write path.

The value justifies the implementation, but the three fix-now findings below prevent approval.

Reviewed `main...d43a8e3f3a8b972041d4274c1582f7557af5b173`, the item, the product spec and `docs/design/lab/brief.md`. Read all Queue components and screens, its tokens and styles, the shared store and navigation contracts, the screenshot catalogue and the lab publishing script. Scope is confined to Queue, its registration, the catalogue and the item record.

Ran the prototype on port 3861. Used the T3 browser to exercise all ten dashboard interactions at 1440 and 412px: add, tick/untick, open/change due date, flag Done, timer start/stop, relabel, search/open, open a person, switch timer, and Undo relabel. Also switched and stopped a timer from People. Those paths worked, including restoring the label count from 11 to 12. Timer changes offered no Undo. Checked the required screen/state links at 1440, 1100 and 412px, including the expanded task/person, goals with three “No data yet” rows, the empty morning, quiet scene, stale banner and twelve-item crowded queue. The 1100px overflow below was reproduced with the original running timer, rather than the shorter unlabelled timer used during interaction checks.

`node_modules/.bin/tsc -p prototype/dashboard/tsconfig.json` passed. `node_modules/.bin/vitest run` passed all 206 tests in 23 files. `node_modules/.bin/vite build --config prototype/dashboard/vite.config.ts` passed. The catalogue contains 14 Queue entries, and the existing gallery has all 42 corresponding images. Opened the published `/lab/dashboard/#/queue/` successfully; its resource paths stayed under `/lab/`. The report's risk assessment is supported by the long phone-width list. I did not regenerate the entire gallery or republish the shared lab while other directions are in progress, and did not run Android builds or production-server checks.

## Finding 1: Keep the top bar inside the required 1100px viewport
Recommendation: fix now

`prototype/dashboard/src/directions/queue/styles.css:1865` applies the narrower top-bar layout only below 1100px. At exactly the required 1100px width, the page links, 340px timer chip and 340px search field need more room than the header provides. Open `#/queue/tasks?id=Reply%20to%20the%20App%20Store%20review%20notes&still=1&shell=0` with the default afternoon data: `document.documentElement.scrollWidth` is 1158 while `innerWidth` is 1100. The search field extends to x=1158, and its input itself extends to x=1119. Goals and People have the same problem because they also show the timer chip. This introduces sideways scrolling on a specifically required comparison width and clips the common search control. Make the header fit at 1100px with the original long timer label, and check the nearby breakpoint widths too. Evidence: `/home/supadava/.t3/userdata/browser-artifacts/browser-screenshot-localhost-mwkweyst-80c54084.png`.

## Finding 2: Prevent timer-picker writes while LoR is shown as unreachable
Recommendation: fix now

`prototype/dashboard/src/directions/queue/timer.tsx:86-98` renders the switch picker and calls `switchTo` without checking `stale`. Disabling its opener does not disable a picker opened by the supported `timer=` parameter. Open `#/queue/?state=stale&timer=switch&still=1`: all recent-label buttons in the picker are enabled. Clicking “Public beta” displays “Switched to Public beta” and replaces the 47-minute timer with a new 00:00 timer, while the banner still says timers must wait until LoR is back. I reproduced this in the browser. The idle/start picker at lines 49-63 has the same missing protection, and an already-open TimerChip picker also lacks a stale guard. Disable or close these pickers and guard their mutations whenever stale, including direct links and changes of state while a picker is open. This is needed for the required stale state to show an honest interaction model, and contradicts the report's claim that every write control is disabled. Evidence: `/home/supadava/.t3/userdata/browser-artifacts/browser-screenshot-localhost-mwkwfiez-496217ee.png`.

## Finding 3: Implement the one thing stepping aside when its time has passed
Recommendation: fix now

`prototype/dashboard/src/directions/queue/lib.ts:139-143` always ranks the chosen task first until it is completed, manually set aside, or the scene is quiet. There is no expiry metadata or clock check, and Today calls `queue(s, mode, l)` without its current time. The Queue brief explicitly requires the one thing to “step aside once its time has passed rather than accusing.” The report correctly admits this is not built, so the first Done-when line is not yet met. The fixture even says to finish this spec before the 14:00 review, while the default 15:00 scene still puts it first. Give the direction's chosen one thing an explicit expiry and demonstrate the transition without completing or deleting the task. The missing shared fixture field is not a blocker: this direction already supplies its chosen title and explanation locally.

## Finding 4: Start over leaves Queue's local dismissals in place
Recommendation: defer

`prototype/dashboard/src/directions/queue/lib.ts:119-121` resets `aside`, `seen` and `crowd` only when the scene name changes. The prototype's “Start over” resets the store with the same scene name, so Queue retains those dismissals. Reproduction: in the afternoon, click “Not today”, open the Version control, and click “Start over”. The shell now says “Nothing changed yet”, but “Find a physio for the left knee” still leads instead of the original one thing. A full reload restores it. Coordinate a reset signal for direction-local state in later prototype work; the required interactions themselves remain usable, so this optional demo-reset repair can follow separately.
