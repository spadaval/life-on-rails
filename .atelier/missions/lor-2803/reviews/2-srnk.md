---
schema: atelier.review
schema_version: 3
round: 2
commit: dc0babead1a56537b6dfb7e03f5db29e71878dca
reviewers:
- human
model: kimi-k3
verdict: changes
created_at: 2026-11-17T06:09:30.572246181Z
---

## Summary

Reviewed the plan of mission lor-2803 as commit dc0babe holds it: the mission, all thirteen items, the lor-xdoi rework, mission lor-ncpw's mission.md and design.md, both round-1 reviews, and the full diff from the round-1 commit 443e2a7. Compared every `blocked_by` edge against each item's Done when, checked the graph for cycles, and checked each acceptance state in lor-q165 against the data some item promises. Verified the code the round-1 findings name at this commit: `useNow` in `src/routes/index.tsx:33-40` and `useTick` in `src/routes/__root.tsx:165` do overwrite the server clock with `Date.now()`; `getTodayFn` (`src/server/functions/today.ts:35`) and `getToday` (`src/server/today.ts:209`) take `new Date()`; Notes history and its diff live in `src/routes/notes.tsx:372-409` over `page_version` (`src/db/schema.ts:460`); the checked-in audit (`docs/design/lab/audit/launcher-home.md:190`) confirms search returns Apps, Shortcuts, Settings and Contacts. This is a plan review; no builds or tests were run. I did not read the other round-2 review (2-ffj8); this review is blind.

All thirteen carried findings are addressed in substance; seven new ones follow. Could not check: whether the 15-second wasm loop holds under the lor-0v68 cap (nothing is built yet); the two claude.ai artifact links (only Sumeet can open them; the inventories are now in-repo, which covers the twenty states); whether CSP path-scoped `connect-src` behaves as intended across browsers (an implementation question for lor-zwfd, whose tests will answer it). Noted, not a finding: dc0babe flips mission lor-ncpw from ready to open for re-agreement, which is coherent with lor-2803's asserted order since both quote Sumeet's direction of 2026-11-17, and `atelier status` at this commit still lists lor-3hnk and lor-sghw as startable.

## Finding 1: Make the gesture item wait for the screens its scripts exercise
Recommendation: fix now
Was: 1-poyq.1
Now: fixed

lor-293p's third Done when now proves the runner on a registered gesture test surface and gives the surface scripts to the items that move the surfaces (lor-xdoi, lor-rgx4, each concept). lor-xdoi's `blocked_by` gained lor-oech and lor-293p; lor-rgx4's gained lor-293p; lor-5fk6's gained lor-oech; lor-glev's gained lor-xdoi, lor-rgx4 and lor-t8lq, so concepts are judged only after `current` is complete. A residual checkbox gap on lor-xdoi is Finding 14.

## Finding 2: Assign interactions to each viewer instead of requiring the same eight in both
Recommendation: fix now
Was: 1-poyq.2
Now: fixed

lor-npa6:28 now gives a per-viewer interaction set (dashboard: add/tick/open a task, due date, flagged done, timer, relabel, search, open a person; phone: drawer, search, approve, answer, timer, tick, move icon, fold), with T3 approvals and questions explicitly kept on the phone. lor-5uxk:30 requires the dashboard set, lor-5fk6:32 the phone set, and mission.md:38 says "that viewer's interactions".

## Finding 3: Give someone ownership of dashboard states that open by URL
Recommendation: fix now
Was: 1-poyq.3
Now: fixed

lor-nde9:28 now makes every shot a link, with search parameters for states that are not in the URL today, clock and data named in the link, a `--list` table of names and links, and an open-by-hand check; its Scope adds the route search parameters. mission.md:37 names lor-nde9's links for the dashboard.

## Finding 4: Freeze the browser clock as well as the test server clock
Recommendation: fix now
Was: 1-poyq.4
Now: fixed

lor-zj7x:28 now names the browser call sites (`useNow` in `src/routes/index.tsx`, `useTick` in `__root.tsx`), requires the page to keep the server-handed fixed time, adds `?at=` per tab for a human opening the state, includes the hydrated-page check past one 20-second tick, and keeps all of it inert on the real database. The named call sites match the code at this commit.

## Finding 5: Static serving does not itself prevent access to production data
Recommendation: fix now
Was: 1-poyq.5
Now: fixed

lor-zwfd:27 now states the enforced boundary: a CSP whose `connect-src` allows only `/lab/` and whose `form-action` is `'none'`, fixture-stores-only for the prototype and the lab's `current`, a hostile-page browser test (fetch, server function, form post all refused) and a per-viewer request audit. Two residues are Findings 15 and 16.

## Finding 6: Measure the fifteen-second loop after the real screens join the module
Recommendation: fix now
Was: 1-poyq.6
Now: fixed

mission.md:36 now takes the launcher's time twice and names the post-move Reports the measurement that counts; lor-rgx4:33 and lor-t8lq:31 have the timed-edit Done when with peak memory and escalation; lor-q165:33 repeats the measurements at acceptance. lor-xdoi's share of the measurement sits in Context, not Done when: Finding 14.

## Finding 7: lor-xdoi still owns the module setup lor-vbfd took, and still says it waits on lor-dkut
Recommendation: fix now
Was: 1-vw31.1
Now: fixed

lor-xdoi's first Done when now reads "which lor-vbfd made with `ui/Look.kt`, `ui/Palette.kt` and `ui/Press.kt` already in it"; the `web-dev.sh`, `settings.gradle.kts` and boat-build-wasm checkboxes are gone or re-worded as lor-vbfd's; the stale Context bullet waiting on lor-dkut is replaced; ncpw's design.md risk bullet now gives the new order with Sumeet's 2026-11-17 direction. Verified lor-dkut's `blocked_by` is lor-pmzz and lor-xdoi at this commit.

## Finding 8: lor-vbfd moves Look.kt while lor-sghw, still ready, edits it
Recommendation: fix now
Was: 1-vw31.2
Now: fixed

lor-vbfd's `blocked_by` is now lor-3hnk, lor-sghw and lor-nde9, so Animated text lands in the app before `ui/Look.kt` moves.

## Finding 9: Gesture scripts and phone concepts can start while current is still a kit sheet
Recommendation: fix now
Was: 1-vw31.3
Now: fixed

The finding's second option was taken for lor-293p (runner plus test surface; scenario scripts moved to the surface owners), and the first for the rest: lor-5fk6 waits on lor-oech for fixtures, lor-glev waits on lor-xdoi, lor-rgx4 and lor-t8lq, and lor-q165 gates the mission's acceptance on the moves. A concept can still be drawn before `current` is complete, but it is judged and accepted only after, which discharges the finding's harm.

## Finding 10: lor-nde9 and lor-vbfd both own scripts/shots.mjs and do not wait on each other
Recommendation: fix now
Was: 1-vw31.4
Now: fixed

lor-nde9 keeps the script; lor-vbfd's `blocked_by` gained lor-nde9 and its Done when now reads "`scripts/shots.mjs`, which lor-nde9 wrote, gains `--lab`". lor-5uxk adds `--prototype` after lor-nde9 through lor-npa6. A scope residue is Finding 17.

## Finding 11: LOR_FAKE_NOW on the test server cannot draw Today at 00:05
Recommendation: fix now
Was: 1-vw31.5
Now: fixed

Same fix as Finding 4: lor-zj7x:28 makes the clock one value in the test server and the page, names the ticking call sites, adds `?at=` per tab and the tick check, all inert off the test database.

## Finding 12: Two success criteria have no item, and lor-glev declares the mission finished without them
Recommendation: fix now
Was: 1-vw31.6
Now: fixed

lor-q165 is in the plan with the twenty states named in-repo (ten dashboard at line 30, ten launcher and app at line 31, including one approval, one failed sync and one inner-screen thread), the per-viewer interaction recordings and the loop re-measurements. The three audit inventories are checked in under `docs/design/lab/audit/`. lor-glev's last Done when now ends "the mission's acceptance is lor-q165".

## Finding 13: lor-5uxk requires the brief's eight interactions of the dashboard prototype
Recommendation: fix now
Was: 1-vw31.7
Now: fixed

lor-npa6:28 split the interactions per viewer and lor-5uxk:30 now requires "the brief's required dashboard set and dashboard interactions", so no dashboard concept must swipe a drawer or approve a T3 card.

## Finding 14: lor-xdoi's gesture scripts and loop measurement are only in Context, never in Done when
Recommendation: fix now

lor-293p:30 states "lor-xdoi adds the drawer, paging, app menu, drag, long press into drag, search and folder back", and mission.md:36 names lor-xdoi's Report one of the three that carry the launcher loop measurement that counts. But lor-xdoi's Done when (`.atelier/items/lor-xdoi/item.md:30-36`) has no checkbox for either: both obligations appear only in the Context bullet at line 44 ("gesture scripts (lor-293p's runner) for the drawer, paging..." and "a one-line edit to a home composable is timed three times... a time over 15 seconds goes to the lead"). The sibling items lor-rgx4 (lines 32-33) and lor-t8lq (line 31) hold the same obligations as Done when checkboxes. An implementer can tick every lor-xdoi box and deliver no scripts and no timing, and lor-q165's re-measurement then has no Report to repeat from. Move both sentences from Context into Done when.

## Finding 15: lor-zwfd blocks on nothing, but its Done when needs the outputs of items that block on it
Recommendation: fix now

lor-zwfd's `blocked_by` is empty (`.atelier/items/lor-zwfd/item.md:12-13`), yet its Done when requires: publish.sh copying "the launcher lab's built bundle, the dashboard prototype's build and the latest galleries" (line 28) — outputs of lor-vbfd/lor-5fk6, lor-5uxk and lor-nde9; a test that "runs each viewer's interactions and finds no request outside `/lab/`" (line 27) — the viewers are lor-5uxk's prototype and the lab; and the lab's first load over Tailscale measured (line 29) — which needs the lab bundle. Meanwhile lor-5uxk and lor-5fk6 both block on lor-zwfd and use its publish.sh. The item can start immediately but can never be completed until its own dependents deliver: a cycle in substance. Keep serving, the traversal test, the hostile-page CSP test and publish.sh (proven on stand-in bundles) in lor-zwfd; give the per-viewer request audit to the viewer items or lor-q165, and the first-load measurement to lor-vbfd's Report or lor-q165.

## Finding 16: lor-xdoi's "the web takes from LoR's own data" contradicts the /lab boundary lor-zwfd just promised
Recommendation: fix now

lor-xdoi's second Done when (`.atelier/items/lor-xdoi/item.md:31`) routes the header's next event and weather through "`DeviceHome`, which the web takes from LoR's own data", implemented by `wasmJsMain`. lor-zwfd:27 forbids exactly that for the lab ("no prototype or lab code calls LoR... fixture stores only") and fences it with CSP, and lor-oech:33 checks the demo `/device/home` payload in as the default fixture "so the lab and the dashboard show the same made-up day". The xdoi sentence is stale wording from when the module's web consumer was the dashboard's real-size page (lor-kaq8, out of this mission); read literally, an implementer wires a fetch from the lab that the CSP test then refuses. Say in lor-xdoi that in the lab `DeviceHome` comes from lor-oech's fixture, and that live LoR data is lor-kaq8's affair.

## Finding 17: lor-5uxk's Done when touches two files its Scope does not name
Recommendation: fix now

lor-5uxk's Scope is "`prototype/dashboard/`. The real dashboard under `src/` is not changed." (`.atelier/items/lor-5uxk/item.md:35`). Its Done when adds the `--prototype` flag "to lor-nde9's script" (line 30, i.e. `scripts/shots.mjs`) and a `pnpm proto:dev` command (line 31), which lives in the root `package.json`. After round 1's file-ownership finding on this same script, the Scope should name what the item touches: add `scripts/shots.mjs` and `package.json`.

## Finding 18: `current` gets no gesture script for "tick a task", one of the brief's eight phone interactions
Recommendation: fix now

lor-npa6:28 makes "tick a task" one of the eight phone interactions every concept must make work by touch and by `scripts/lab/gesture.mjs` (lor-5fk6:32), and lor-293p:30 says each concept adds its own scripts. For `current`, lor-293p:30 and lor-rgx4:32 together assign scripts for the drawer, paging, menus, drags, search, folder back, card approval, answering and the timer — but not for ticking a task, which lives on the LoR panel's Tasks tab, a surface lor-rgx4 moves (its Done when at line 31 includes "the LoR panel's Tasks and People tabs"). Concepts end up with a replayable recording of an interaction `current` cannot replay, and lor-glev's critics compare against `current`. Add task ticking to lor-rgx4's script list and to lor-293p's enumeration.

## Finding 19: The demo set promises no page with versions, but two downstream states need a page history diff
Recommendation: fix now

lor-q165:30 requires "Notes on a page's history with a diff showing" by link alone, and lor-nde9:28 shoots "Notes index, page, history and graph". The diff is drawn from `page_version` (`src/db/schema.ts:460`; panel and diff `<pre>` at `src/routes/notes.tsx:372-409`), so at least one demo page needs two or more versions. lor-zj7x's content list (`.atelier/items/lor-zj7x/item.md:27`) promises "about 30 wiki pages with links between them, two days' write-ups and a pinned page" — write-ups on two days are not two versions of one page, and no other line supplies versions. Add versioned pages to the list (and to the `seeds/demo/README.md` state table).

## Finding 20: lor-oech's fixture domains omit contacts and shortcuts, but acceptance requires contact search results
Recommendation: fix now

lor-q165:31 requires, by link alone, "search with app, contact and settings results" in the lab's `current`. The audit (`docs/design/lab/audit/launcher-home.md:190`) shows launcher search returns Apps, Shortcuts, Settings and Contacts. lor-oech's fixture data domains (`.atelier/items/lor-oech/item.md:29`) enumerate "the layout, the app list with placeholder or bundled icons, `/device/home`'s payload, T3's threads, cards and messages, timer state, sensor and sync status, settings" — no contacts, no shortcuts. The catalogue line (line 30) covers surfaces "in its ordinary... condition", but the domain list is what an implementer writes fixtures from. Add contacts (and shortcuts, for the same reason) to the fixture domains.
