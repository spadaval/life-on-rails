---
schema: atelier.review
schema_version: 3
round: 2
commit: dc0babead1a56537b6dfb7e03f5db29e71878dca
reviewers:
- 01a1e81f-a2a6-7862-82b6-6ac2b9729678
model: grok-4.6
verdict: changes
created_at: 2026-11-17T06:09:44.615357943Z
---

## Summary

Complexity: high. The plan still adds a design-lab subsystem (demo clock and data, shot gallery, Compose-wasm lab, gesture runner, `/lab/` on the owner listener), brings the homecore split forward, and builds two viewers plus three to five concepts on top.
Value: high. Sumeet named roughness as the biggest issue and asked for rapid-development tools plus 3–5 usable concepts with a dashboard prototype and an in-browser phone mockup.
Risk of merging it: medium. Agreeing the plan authorizes a Kotlin upgrade and extracting the running launcher into a shared module; a break shows on the emulator receipts, and the real phone is out of scope.

The outcome is still worth the size. Round 2 answered the round-1 holes on per-viewer interactions, linkable dashboard states, the browser clock, a CSP boundary on `/lab/`, the loop timed after the moves, the gesture runner split, lor-xdoi's Done when, the acceptance item, and the inventories in the repo. I am not recording Shrink or Reject.

Checked commit `dc0babe` as it stands: the mission, all thirteen of its items (including lor-q165), the edits to lor-xdoi, lor-dkut, lor-vbfd, lor-3hnk and lor-sghw, mission lor-ncpw (mission.md and design.md), `docs/design/lab/audit/` (README plus the three inventories), and the current code those items name (`src/routes/index.tsx`, `src/routes/__root.tsx`, `src/routes/tasks.tsx`, `src/routes/notes.tsx`, `src/server/functions/today.ts`, `src/server/today.ts`, `scripts/serve.mjs`). Compared each item's `blocked_by` to its Done when, and each carried finding to the round-2 text.

Could not check: the audit page at `https://claude.ai/artifact/TGotPxRg56dJfqe9kQKMuD` (the inventories are now in-repo, so this no longer blocks the twenty named states); whether the 15-second wasm loop holds under the lor-0v68 cap (lor-vbfd already takes a miss to Sumeet); a live `/lab/` (it does not exist yet). Did not read another reviewer's file.

## Finding 1: Make the gesture item wait for the screens its scripts exercise
Recommendation: fix now
Was: 1-poyq.1
Now: fixed
Round 2 took the split the finding offered. lor-293p's third Done when (`.atelier/items/lor-293p/item.md` line 30) now proves the runner on a lab test surface and assigns real-surface scripts to the items that bring those surfaces in. lor-xdoi waits on lor-oech (`.atelier/items/lor-xdoi/item.md` lines 12–16) and its Context (line 44) takes the drawer, paging, menu, drag, search and folder-back scripts. lor-rgx4 waits on lor-293p and adds the card, question and timer scripts (`.atelier/items/lor-rgx4/item.md` lines 14–16 and 32).

## Finding 2: Assign interactions to each viewer instead of requiring the same eight in both
Recommendation: fix now
Was: 1-poyq.2
Now: fixed
lor-npa6 (`.atelier/items/lor-npa6/item.md` line 28) now splits the required set and the interactions by viewer, and keeps T3 approvals and questions on the phone. The mission's `usable-concepts` line, lor-5uxk line 30, lor-5fk6 line 32 and lor-q165 line 32 all use that per-viewer list.

## Finding 3: Give someone ownership of dashboard states that open by URL
Recommendation: fix now
Was: 1-poyq.3
Now: fixed
lor-nde9's second Done when (`.atelier/items/lor-nde9/item.md` line 28) now makes every shot a link, adds search parameters for states that are not in the URL today, names the clock as `at=` from lor-zj7x, and requires that opening a link by hand draws the same state. The mission points dashboard links at lor-nde9 and phone links at lor-oech, shown by lor-q165. A leftover mismatch between nde9's shot list and q165's ten dashboard names is Finding 15.

## Finding 4: Freeze the browser clock as well as the test server clock
Recommendation: fix now
Was: 1-poyq.4
Now: fixed
lor-zj7x's third Done when (`.atelier/items/lor-zj7x/item.md` line 28) now requires the page to keep the test-server clock after load, names `useNow` in `src/routes/index.tsx` and `useTick` in `__root.tsx`, adds `?at=` on any page for a human-opened tab, and checks that remaining time is unchanged after one 20-second tick. The real database ignores `at=` and `LOR_FAKE_NOW`. The code at this commit still overwrites with `Date.now()` (`src/routes/index.tsx` lines 33–40, `__root.tsx` lines 166–171); that is now this item's work.

## Finding 5: Static serving does not itself prevent access to production data
Recommendation: fix now
Was: 1-poyq.5
Now: fixed
lor-zwfd's second Done when (`.atelier/items/lor-zwfd/item.md` line 27) now requires a CSP `connect-src` limited to `/lab/` on this origin, `form-action 'none'`, fixture-only stores in the prototype and in the lab's `current`, a browser test that `fetch('/api/v1/status')`, a server function and a form post are refused, and a second test that viewer interactions make no request outside `/lab/`.

## Finding 6: Measure the fifteen-second loop after the real screens join the module
Recommendation: fix now
Was: 1-poyq.6
Now: fixed
The mission's `fast-loop` line (`.atelier/missions/lor-2803/mission.md` line 36) now takes the launcher time twice: lor-vbfd as a first check, then lor-xdoi, lor-rgx4 and lor-t8lq as the measurement that counts. Each of those three items times a one-line edit after its move (lor-xdoi Context line 44; lor-rgx4 line 33; lor-t8lq line 31). lor-q165 line 33 repeats those numbers. The empty lab no longer discharges the criterion. Phone concepts can still start before that counting measurement; that leftover is Finding 9.

## Finding 7: lor-xdoi still owns the module setup lor-vbfd took, and still says it waits on lor-dkut
Recommendation: fix now
Was: 1-vw31.1
Now: fixed
lor-xdoi's first Done when (`.atelier/items/lor-xdoi/item.md` line 30) now moves home's drawing into the module lor-vbfd already made. Context lines 44 and 47 drop the wait on lor-dkut and the skipped-but-still-ticked setup checkboxes. lor-dkut waits on lor-xdoi (`.atelier/items/lor-dkut/item.md` lines 12–14). ncpw's design.md Risks bullet on the split now says it comes before lor-dkut, with lor-vbfd making the module first. lor-3hnk Scope line 35 still names lor-xdoi as the owner of the wasm Node.js repository; that leftover does not restore the Done-when clash.

## Finding 8: lor-vbfd moves Look.kt while lor-sghw, still ready, edits it
Recommendation: fix now
Was: 1-vw31.2
Now: fixed
lor-vbfd's `blocked_by` (`.atelier/items/lor-vbfd/item.md` lines 13–16) now includes lor-sghw, so Animated text lands in the app before `ui/Look.kt` moves.

## Finding 9: Gesture scripts and phone concepts can start while current is still a kit sheet
Recommendation: fix now
Was: 1-vw31.3
Now: kept
The 293p half is fixed (Finding 1): the runner is proven on a test surface, and surface scripts moved to lor-xdoi and lor-rgx4. The comparison-page half is fixed: lor-glev now waits on lor-xdoi, lor-rgx4 and lor-t8lq (`.atelier/items/lor-glev/item.md` lines 12–17).

lor-5fk6 still does not. Its `blocked_by` (`.atelier/items/lor-5fk6/item.md` lines 12–16) is lor-npa6, lor-293p, lor-zwfd and lor-oech. It does not wait on the items that put today's launcher into `current`. Its Done when (lines 30–32) still registers each direction "from the same fixtures as `current`" and requires the brief's phone set (home, drawer, search, agent page, LoR panel, a menu, a sheet, settings, timer) plus the phone interactions. Until lor-xdoi, lor-rgx4 and lor-t8lq land, `current` is the kit sheet lor-vbfd's third Done when allows.

That also skips the loop measurement that counts. The mission (`.atelier/missions/lor-2803/mission.md` line 36) says that measurement is the Reports of lor-xdoi, lor-rgx4 and lor-t8lq, and that a time over 15 seconds goes to Sumeet before more concepts are built on it. lor-5fk6 can start, and its first direction can be ticked, as soon as the empty lab exists.

Put lor-xdoi, lor-rgx4 and lor-t8lq on lor-5fk6's `blocked_by`, as lor-glev already does, or cut 5fk6's first direction down to a shell that does not claim the brief's phone set until those moves land.

## Finding 10: lor-nde9 and lor-vbfd both own scripts/shots.mjs and do not wait on each other
Recommendation: fix now
Was: 1-vw31.4
Now: fixed
lor-vbfd now waits on lor-nde9 (`.atelier/items/lor-vbfd/item.md` lines 13–16) and its last Done when (line 35) adds `--lab` to the script lor-nde9 wrote. The create-the-file clash is gone. lor-5uxk still adds `--prototype` to the same file without waiting on lor-vbfd; that leftover is Finding 14.

## Finding 11: LOR_FAKE_NOW on the test server cannot draw Today at 00:05
Recommendation: fix now
Was: 1-vw31.5
Now: fixed
Same contract as Finding 4. lor-zj7x now requires one fake clock in the test server and on the page, names the two client tickers, and checks a hydrated Today at 09:00 after a 20-second tick. `getTodayFn` (`src/server/functions/today.ts` line 35) and `getToday` (`src/server/today.ts` line 209) still take `new Date()` at this commit; that is now in scope as "where now is taken" (lor-zj7x Scope line 35) because the Done when cannot pass unless they read the fake clock.

## Finding 12: Two success criteria have no item, and lor-glev declares the mission finished without them
Recommendation: fix now
Was: 1-vw31.6
Now: fixed
lor-q165 exists, names the twenty states, and is the mission's `any-state` and `usable-concepts` proof (`.atelier/missions/lor-2803/mission.md` lines 37–38). lor-glev's last Done when (`.atelier/items/lor-glev/item.md` line 35) finishes the comparison page and points acceptance at lor-q165. The three inventories are in `docs/design/lab/audit/` (README, `dashboard.md`, `launcher-home.md`, `launcher-panels-app.md`). A leftover mismatch between those named states and the link contracts is Finding 15.

## Finding 13: lor-5uxk requires the brief's eight interactions of the dashboard prototype
Recommendation: fix now
Was: 1-vw31.7
Now: fixed
lor-npa6 line 28 now has a dashboard interaction list (add a task, tick and untick, open a task and change its due date, mark a flagged item done, start and stop a timer, relabel a block, search and open, open a person). lor-5uxk line 30 requires "the brief's required dashboard set and dashboard interactions". Today already has start/stop on `TimerCard` (`src/routes/index.tsx` lines 209–235), so the dashboard timer is a real control on those four screens, not a phone-only leftover.

## Finding 14: lor-5uxk and lor-vbfd both add flags to scripts/shots.mjs and do not wait on each other
Recommendation: fix now
lor-nde9 creates `scripts/shots.mjs`. lor-vbfd's last Done when (`.atelier/items/lor-vbfd/item.md` line 35) adds `--lab` to that file. lor-5uxk's third Done when (`.atelier/items/lor-5uxk/item.md` line 30) adds `--prototype` to the same file. lor-5uxk's `blocked_by` (lines 12–14) is lor-npa6 and lor-zwfd. lor-npa6 waits on lor-nde9, so the script exists, but lor-5uxk does not wait on lor-vbfd. Both items can start after lor-nde9 and both write `scripts/shots.mjs`.

Give one of them both flags, or put lor-vbfd on lor-5uxk's `blocked_by` (or the other way around) so the flags land in one change.

## Finding 15: q165's twenty states are not the links nde9 and oech promise
Recommendation: fix now
lor-q165 lines 30–31 name twenty states that someone who built none of it must open by link. lor-nde9 line 28 owns dashboard links as a shot table: four clock times, a past day, a selected block, a task open, rows picked, quick add open, a Goals item with the editor open, People list and hub, Notes history, Search, Status. That list is not q165's list. q165 wants "Today at 15:00 with a running timer" (clock plus running timer in one URL), "quick add open with a parent and a date typed" (the field contents, not only the dialog), and People's list on "Due a hello" (a specific filter). An implementer can tick lor-nde9's table without producing those three links, and lor-q165 then has nowhere to point.

On the phone, lor-oech line 32 opens named fixtures from the State dropdown and the URL hash (`#current/home-full` in lor-vbfd). Fixtures are data: layout, app list, T3, timer, sensors, settings (lor-oech line 29). q165 wants "the drawer's All tab mid-scroll" and "an app's long-press menu", which are scroll offset and an open popover, not fixture payloads. Nothing in lor-oech, lor-xdoi or lor-vbfd requires those two to be restorable from a link.

Put q165's twenty names on lor-nde9's shot table and on lor-oech's `STATES.md` (or on the items that add those fixtures), including how the extra bits (typed quick-add, mid-scroll, open menu, running timer at 15:00) live in the URL. Otherwise every existing item's checklist can pass while acceptance still has no links.
