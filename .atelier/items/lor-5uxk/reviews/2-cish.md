---
schema: atelier.review
schema_version: 3
round: 2
commit: d4477dfa9cd1fefbd803d95238a61472e94d46f7
reviewers:
- thread:delegated-task:command%3Amcp%3Abd6c1db2-f61b-4e91-9b92-cfcace9f294d%3Adelegate-task%3A2502d8a3-37fc-4c24-a2ab-3ed928aa0921
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T15:43:06.227275891Z
---

## Summary

Complexity: high. The browser store, router shim and direction/style loading contract are foundations that later directions will depend on, alongside copies of the shipped screens.

Value: high. Comparing interactive dashboard directions requires this shared demo data, shell and shipped baseline.

Risk of merging: low. The static prototype uses made-up data and is isolated from the running dashboard; its failures show and are cheap to revert. The production CSS change only excludes prototype classes from Tailwind scanning.

The value warrants the shape. A frozen copy of the shipped screens keeps the comparison baseline independent of production changes. All four round 1 findings are fixed, and I found no additional issue requiring a change before merge.

Reviewed `main...d4477dfa9cd1fefbd803d95238a61472e94d46f7` and the changes since round 1, the item's Done when and Report, the prior review, the mission and dashboard brief. Inspected the store, derived lists, adapters, route shim, shell/style loading, direction contract, copied screen controls, snapshot producer and build/publish integration. The new design directions and their improved interactions remain separate items, as this item's Done when specifies; `current` is the shipped baseline.

`pnpm proto:build`, `node_modules/.bin/tsc -p prototype/dashboard`, root `pnpm typecheck` and all 206 tests pass. pnpm emits the reported node_modules-symlink install warning but runs its scripts successfully. `pnpm proto:dev` serves this checkout on 3859. The catalogue generated all 33 images at 1500, 1100 and 412 px in 9.2 seconds with no problems; inspected desktop Today with a selected block, desktop People and phone Tasks with a task open.

Published the fresh build only to `/tmp/lor-review2-5uxk.KCH0QP/lab`, then served it on 3861 through the real `labStatic` handler and its CSP. Reran the round 1 interaction and reproduction scripts, adapting assertions that had expected the old failures. Adding, changing a due date, ticking/reopening, completing a flag, relabelling, opening a search result and opening a person pass at 1500 and 412 px, with no page errors or requests outside `/lab/dashboard/`. Add and timer stop/start pass both on localhost and on the insecure HTTP origin `http://review.invalid:3861`, resolved locally. Ben Okoro gains the correct overdue membership when given a cadence. In dev, both a held old direction import and a held old stylesheet leave the newly selected Template intact. An ordinary dropdown round trip preserves the added task, path and scene and restores only the selected stylesheet; unknown versions fall back to current.

Evidence and scripts are in `/tmp/lor-review2-5uxk.KCH0QP/`: `origin.mjs`, `race.mjs`, `people.mjs`, `interactions.mjs`, `interactions.json`, `regressions.mjs`, `regressions.json`, screenshots and `shots/index.html`. I did not run the snapshot producer, change the test database, publish the real lab, test a physical phone or time hot reload independently. The HTTP-origin check reproduces the relevant browser restriction without touching the real service. Only the review file was edited.

## Finding 1: [P1] Generate prototype IDs on the HTTP origin used by the lab
Recommendation: fix now
Was: 1-0t9a.1
Response: fixed in 7379aef
Now: fixed

The UUID helper uses `crypto.getRandomValues`. In the fresh published build on insecure `http://review.invalid:3861`, `isSecureContext` is false and `crypto.randomUUID` is undefined, yet task creation and timer stop/start succeed with no page errors or requests outside the prototype.

## Finding 2: [P2] Prevent an abandoned version load from replacing the selected version's CSS
Recommendation: fix now
Was: 1-0t9a.2
Response: fixed in 7379aef
Now: fixed

The effect checks its lifetime before linking sheets and stylesheet completion checks the selected version before removing other sheets or changing `data-version`. Held current's import, selected Template and released current: Template kept its heading, background, `data-version` and sole stylesheet. Repeated with current's actual stylesheet request held instead of its import; Template again remained intact.

## Finding 3: [P2] Derive People filters from the edited people rather than frozen membership
Recommendation: fix now
Was: 1-0t9a.3
Response: fixed in 7379aef
Now: fixed

`peopleIn` derives membership and ordering from edited people, matching the production reader's filters. Removing Grandpa Joe's cadence removes him immediately from Due a hello and Keeping in touch, including after navigating between them. Giving never-contacted Ben Okoro a weekly cadence puts him in Due a hello with “7 days past the cadence”.

## Finding 4: [P2] Remove the remembered-label checkbox that the prototype cannot apply
Recommendation: fix now
Was: 1-0t9a.4
Response: fixed in 7379aef
Now: fixed

The unsupported checkbox and its component state are removed; the relabel adapter receives false remember flags. The selected-block catalogue capture and browser check show no remembered-label control. Ordinary relabelling still succeeds in the built prototype at both widths.
