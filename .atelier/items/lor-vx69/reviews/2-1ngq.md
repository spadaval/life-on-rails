---
schema: atelier.review
schema_version: 3
round: 2
commit: e9a3320e4b9b835afbc27e1523f237050d6c25ee
reviewers:
- thread:delegated-task:command%3Amcp%3Ab81bed6f-1d6d-44e2-9a3a-5ce4da3e090f%3Adelegate-task%3A9e26e689-756a-4a53-b97f-36553481423f
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T16:31:25.483636598Z
---

## Summary

Complexity: high. The full change adds about 4,500 lines for five screens, a ranked queue, local dismissals, Undo, floating layers and responsive styles.

Value: high. The working Queue direction is needed to compare the dashboard concepts using the brief's common screens and interactions.

Risk of merging: low. The change is an isolated prototype over made-up data; failures are visible, easy to revert and do not affect production writes.

The value justifies the implementation. All three previous fix-now findings are fixed. The two minor findings below can follow separately and do not prevent the required comparison.

Read the item, product spec, design brief, the full Queue implementation, its shared store/navigation contracts, registration, shot catalogue and publishing script. Reviewed both `main...e9a3320` and the changes since round 1. Scope remains Queue, its registration, the catalogue and Atelier records.

Ran the prototype on port 3861 and used the T3 browser. Exercised all ten interactions at 1440 and 412px through DOM inputs, buttons and links: add, tick/untick, open/change due date, flag Done, timer start/stop, relabel, search/open, open a person, switch timer and Undo relabel. Also stopped the running timer from People. Checked the corresponding Undo feedback, including no Undo for timer mutations and the relabel count returning from 11 to 12. These paths passed at both widths.

Opened all required screen/state links at 1440, 1100 and 412px and checked their rendered content and document width. Tasks and People open their rows in place; Goals has three no-data rows; quiet, empty morning, stale and twelve-item crowded states render. No horizontal document overflow occurred. Checked the original long timer label on Tasks at 1440, 1101, 1100, 1099, 1000, 960, 959, 900, 899, 720, 719 and 412px. Checked fresh stale start/switch links and entering stale with a picker already open in both TimerBar and TimerChip. Called the queue derivation at 13:59:59, 14:00:00 and 15:00:00 to verify the expiry boundary without changing the task or running timer.

`node_modules/.bin/tsc -p prototype/dashboard/tsconfig.json` passed. `node_modules/.bin/vitest run` passed all 206 tests in 23 files. `node_modules/.bin/vite build --config prototype/dashboard/vite.config.ts` passed. The catalogue has 14 Queue entries and the existing gallery has all 42 current Queue images. The published `/lab/dashboard/#/queue/` loads the updated Queue build, and its resource requests stay under `/lab/`. The long phone-width list supports the Report's stated risk. The Report's original build-stamp reference is older than the currently published `66209e1` stamp, which includes the fixes.

Did not regenerate the complete gallery or republish the shared lab while the other directions are in progress. Did not run Android builds, root application checks or production write paths. Closed the review tab and stopped the dev server before finishing.

## Finding 1: Keep the top bar inside the required 1100px viewport
Recommendation: fix now
Was: 1-hiyq.1
Response: fixed in 66209e1
Now: fixed

`styles.css` now allows the search field and timer chip to shrink, hides the timer label below 960px and moves navigation onto a second row below 900px. With the original timer on Tasks at exactly 1100px, document scrollWidth is 1100 and the search field ends at x=1076. The adjacent widths listed above also fit. Goals and People fit at all three required widths.

## Finding 2: Prevent timer-picker writes while LoR is shown as unreachable
Recommendation: fix now
Was: 1-hiyq.2
Response: fixed in 66209e1
Now: fixed

`timer.tsx` now passes stale into the shared timer actions, refuses all three mutations there and omits each picker while stale. Fresh `state=stale&timer=switch` and `scene=empty&state=stale&timer=start` links render no picker and disable their timer controls. Entering stale after opening either the Today switch picker or the People timer-chip picker removes the picker and disables Switch and Stop. Normal start/switch/stop still work at both interaction widths.

## Finding 3: Implement the one thing stepping aside when its time has passed
Recommendation: fix now
Was: 1-hiyq.3
Response: fixed in 66209e1
Now: fixed

`lib.ts` now gives the chosen one thing a 14:00 local expiry, and Today passes its clock into `queue`. Immediately before 14:00 the spec task leads; at 14:00 and 15:00 the oldest flag leads instead. The spec task remains active and its running timer remains intact. The morning and afternoon links show the two sides of that transition.

## Finding 4: Start over still retains Queue's local dismissals
Recommendation: defer

The earlier deferred finding `1-hiyq.4` still holds. `prototype/dashboard/src/directions/queue/lib.ts:121-123` clears local state only when the scene name changes, whereas the shell's Start over resets the same scene. In the morning scene, choose Not today, then Version → Start over. The shell says Nothing changed yet, but Organise Sam's birthday dinner still leads instead of Draft the offline maps spec. A reload restores it. Coordinate a store reset signal for direction-local dismissals later. This affects the optional demo reset, and does not prevent the required interactions or linked screenshots.

## Finding 5: The phone-width add hint loses the key it describes
Recommendation: defer

`prototype/dashboard/src/directions/queue/screens/Tasks.tsx:69` says “Press q anywhere to add one”, but `prototype/dashboard/src/directions/queue/styles.css:1950-1954` hides every `.q-kbd` below 720px. At 412px the visible hint becomes “Press anywhere to add one.” Pressing the page does not add a task. Hide the whole keyboard hint at phone width or replace it with a hint pointing to the search/add field. This is minor prototype copy: the visible field still provides the required add interaction, verified above.
