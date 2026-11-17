---
schema: atelier.review
schema_version: 3
round: 1
commit: 443e2a79207826bcf0cb956ee55f55e72efab85e
reviewers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3Ab80844a3-1516-46ed-8efe-f20c728b06dd
model: gpt-6-astra
verdict: changes
created_at: 2026-11-17T06:00:41.927967372Z
---

## Summary

Reviewed mission lor-2803 at 443e2a79207826bcf0cb956ee55f55e72efab85e, all twelve new items, the changed lor-xdoi/lor-dkut ordering, and the prerequisite Kotlin/text items. Cross-checked the product spec, current Today clock, server functions and listener implementation. This is a plan review; no builds or tests were run. The linked Claude audit was inaccessible through the web reader, so I could not independently check its inventory or quoted conclusions. Browser performance, device parity and the unidentified third critic remain unverified.

Complexity: high. This moves production Android drawing into a cross-platform module and adds fixtures, gesture tooling, publishing and two families of prototypes.

Value: high. A fast way to inspect and interact with alternatives directly serves the requested design iteration.

Risk of merging it: medium. The plan authorizes changes to production drawing and serving, even though the concepts themselves are disposable; failures should be observable and reversible.

The value justifies the work, including the explicitly requested shared-code browser loop. The plan needs the following corrections before starting; none requires abandoning that approach.

## Finding 1: Make the gesture item wait for the screens its scripts exercise
Recommendation: fix now

`.atelier/items/lor-293p/item.md:13` waits only on lor-vbfd, whose initial current version is a kit sheet. Its completion criteria at line 30 nevertheless require drawer, folder, app-menu, approval and timer scripts. Those surfaces arrive through lor-xdoi, lor-rgx4 and lor-t8lq, and fixtures through lor-oech. The phone-concept item then waits on the gesture item, but not on those moves. The graph therefore presents work as executable before its required UI exists. Also, lor-xdoi promises fixtures using lor-oech's shape without waiting on it. Encode the actual prerequisites, or split the generic gesture runner from its surface-specific scripts and attach those scripts to the corresponding moves. Ensure the current-version comparison is complete before concepts are judged.

## Finding 2: Assign interactions to each viewer instead of requiring the same eight in both
Recommendation: fix now

`.atelier/missions/lor-2803/mission.md:38` requires a recording of every interaction in each viewer, and `.atelier/items/lor-5uxk/item.md:29` gives dashboard directions the brief's eight interactions. But `.atelier/items/lor-npa6/item.md:28` includes opening the app drawer, moving an icon, approving a T3 card and answering a question. The dashboard's required surfaces are Today, Tasks, Goals and People, and the product spec explicitly puts T3 approvals on the phone rather than in LoR's dashboard. As written, a dashboard cannot pass without unrelated expansion that conflicts with that boundary. Give the brief a per-viewer interaction matrix and have concept items and acceptance use it. Keep each direction comparable within its own viewer.

## Finding 3: Give someone ownership of dashboard states that open by URL
Recommendation: fix now

`.atelier/missions/lor-2803/mission.md:37` requires ten dashboard states to open by URL alone. `.atelier/items/lor-nde9/item.md:28` only assigns a screenshot script and a table of shots; a script can click into a selected block, picked task rows or an open editor without providing any reproducible URL. The phone explicitly gets a state loader and URL contract in lor-oech; the dashboard does not. Assign the dashboard state loader/link contract to an item, including how the chosen clock and data variant are restored. Use those same links in the shot catalogue and acceptance. Otherwise every existing item's checklist can pass while this mission criterion remains unimplemented.

## Finding 4: Freeze the browser clock as well as the test server clock
Recommendation: fix now

`.atelier/items/lor-zj7x/item.md:28` specifies a fixed test-server clock, but `src/routes/index.tsx:33` initializes from the server timestamp and then immediately overwrites it with `Date.now()` in an effect, repeating every 20 seconds. The remaining-day quantity, next event and running timer use that browser value. Freezing only the server therefore still draws the real time after hydration and cannot reliably produce the four promised Today states. Explicitly include a test-only browser clock contract and a hydrated-page check after a clock tick, while preserving the real service's clock. The contract must also work when a human opens the state, not only inside the screenshot runner.

## Finding 5: Static serving does not itself prevent access to production data
Recommendation: fix now

`.atelier/items/lor-zwfd/item.md:26` claims that nothing under `/lab/` can read or write LoR data because it serves static files. JavaScript in those files runs on the production dashboard's origin and can call its endpoints. `scripts/serve.mjs` routes owner-authenticated requests to the real app, and `src/server/functions/today.ts:82` and `src/server/functions/memory.ts:86` expose mutations used by that dashboard. A directory-traversal test does not establish the claimed separation. State precisely what is guaranteed: at minimum require both prototype clients and the current-version adapters to use only fixture stores, and check their interactions for production requests. If “cannot” is intended as an enforced browser boundary, specify and test that boundary rather than treating static files as one. This matters especially when recreating current screens by reusing existing components.

## Finding 6: Measure the fifteen-second loop after the real screens join the module
Recommendation: fix now

`.atelier/missions/lor-2803/mission.md:36` uses lor-vbfd's Report as the launcher performance proof. At that point `.atelier/items/lor-vbfd/item.md:28` has moved three UI files and fonts, and line 30 explicitly permits a kit sheet. The large home, panel and settings moves happen later. Passing the cap and fifteen-second rebuild on that small dependency graph does not establish that editing the actual launcher meets the promised loop. Add the three timed edits and peak-memory measurement after those moves, before concepts start, with the existing escalation if the target fails. Keep the early measurement as a feasibility check, but do not let it discharge the final criterion.
