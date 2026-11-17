---
schema: atelier.review
schema_version: 3
round: 2
commit: dc0babead1a56537b6dfb7e03f5db29e71878dca
reviewers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A0c6d036b-1844-42dc-8027-685bbd95def1
model: gpt-6-astra
verdict: changes
created_at: 2026-11-17T06:09:19.667478319Z
---

## Summary

Complexity: high. This plan moves production Compose drawing into a shared module and adds fixture, gesture, publishing and prototype tools across two platforms.
Value: high. The owner explicitly needs rapid design iteration and usable alternatives to the current screens.
Risk of merging the plan: medium. The refactors touch the running app, but the plan requires unchanged behavior and checks before landing; the plan itself is reversible.

The value warrants the work, given the explicitly requested browser implementation and existing spike. I checked the mission and all thirteen items at dc0babe, the three prerequisite items in lor-ncpw, the updated sync order and ncpw design, the product spec, the checked-in audit inventory, and the app-menu source. I traced the dependency paths and checked every carried finding against the revised completion criteria. This is a plan review: I did not run builds, time the loops, exercise a prototype, or validate the proposed CSP in a browser. Those implementations do not exist yet.

Most earlier findings are resolved. Two actionable problems remain: phone concepts can still begin before the current screens and their full-module timing measurements, and the split explicitly leaves the app-menu drawing behind despite requiring it in the lab.

## Finding 1: Make the gesture item wait for the screens its scripts exercise
Recommendation: fix now
Was: 1-poyq.1
Now: fixed

lor-293p now proves a generic runner on a dedicated gesture test surface. Surface scripts belong to lor-xdoi and lor-rgx4, which wait for that runner; lor-xdoi also waits for lor-oech. The remaining phone-concept dependency gap is retained under Finding 9.

## Finding 2: Assign interactions to each viewer instead of requiring the same eight in both
Recommendation: fix now
Was: 1-poyq.2
Now: fixed

lor-npa6 now gives explicit dashboard and phone interaction sets; the mission, concept items and lor-q165 use the appropriate set.

## Finding 3: Give someone ownership of dashboard states that open by URL
Recommendation: fix now
Was: 1-poyq.3
Now: fixed

lor-nde9 now owns route search parameters, named clock/data links, a printed shot-link catalogue, and manual reproduction from those links.

## Finding 4: Freeze the browser clock as well as the test server clock
Recommendation: fix now
Was: 1-poyq.4
Now: fixed

lor-zj7x now requires the same fixed clock in the server and browser, a per-tab at parameter restricted to the test database, and a check after the browser tick.

## Finding 5: Static serving does not itself prevent access to production data
Recommendation: fix now
Was: 1-poyq.5
Now: fixed

lor-zwfd now explicitly recognizes the shared origin, requires fixture-only clients, specifies a CSP and blocked-request tests, and checks viewer interactions for requests outside /lab/. This resolves the original claim that static serving alone provides separation; implementation still needs to prove the proposed policy works.

## Finding 6: Measure the fifteen-second loop after the real screens join the module
Recommendation: fix now
Was: 1-poyq.6
Now: fixed

The mission and all three move items now require post-move timed edits with peak memory; lor-q165 repeats the measurements. The early kit-sheet timing is no longer the final evidence. The ordering gap before concept work remains in Finding 9.

## Finding 7: lor-xdoi still owns the module setup lor-vbfd took, and still says it waits on lor-dkut
Recommendation: fix now
Was: 1-vw31.1
Now: fixed

lor-xdoi now describes extending the existing module, removes the duplicate setup requirements, and follows the fixture and gesture tools. Its Context and lor-ncpw/design.md now put the split before lor-dkut.

## Finding 8: lor-vbfd moves Look.kt while lor-sghw, still ready, edits it
Recommendation: fix now
Was: 1-vw31.2
Now: fixed

lor-vbfd now has lor-sghw in blocked_by, so Look.kt is edited before it moves.

## Finding 9: Gesture scripts and phone concepts can start while current is still a kit sheet
Recommendation: fix now
Was: 1-vw31.3
Now: kept

Partly fixed: lor-293p now uses its own test surface, and lor-glev waits for the three moves. However, .atelier/items/lor-5fk6/item.md:12-16 still waits only for lor-npa6, lor-293p, lor-zwfd and lor-oech. None waits for lor-xdoi, lor-rgx4 or lor-t8lq. lor-oech explicitly supplies only the initial fixture shape and grows with those moves. Thus the phone-concept item can start and implement its complete home/panels/settings direction while current is a kit sheet and before any full-module performance measurement. This also defeats the mission fast-loop criterion that a failed post-move measurement reaches Sumeet before more concepts are built on it. Add the three moves to lor-5fk6 prerequisites (and carry that gate to the later direction items). Delaying only the final critique does not order the implementation.

## Finding 10: lor-nde9 and lor-vbfd both own scripts/shots.mjs and do not wait on each other
Recommendation: fix now
Was: 1-vw31.4
Now: fixed

lor-vbfd now waits on lor-nde9 and explicitly adds --lab to the script lor-nde9 created. The dashboard prototype follows lor-npa6, which follows lor-nde9, and owns its --prototype extension.

## Finding 11: LOR_FAKE_NOW on the test server cannot draw Today at 00:05
Recommendation: fix now
Was: 1-vw31.5
Now: fixed

lor-zj7x now includes the browser clock and hydrated-page check, not only LOR_FAKE_NOW. Its test-database restriction applies to both overrides.

## Finding 12: Two success criteria have no item, and lor-glev declares the mission finished without them
Recommendation: fix now
Was: 1-vw31.6
Now: fixed

lor-q165 exists with twenty named states, per-viewer interaction checks and timings. The audit inventories are checked into .atelier/missions/lor-2803/audit/. lor-glev now finishes only its page and points to lor-q165 for mission acceptance.

## Finding 13: lor-5uxk requires the brief's eight interactions of the dashboard prototype
Recommendation: fix now
Was: 1-vw31.7
Now: fixed

lor-5uxk now requires the dashboard interaction set defined in lor-npa6, which excludes phone approvals, drawer and icon moves.


## Finding 14: Move the app-menu drawing needed by current into the lab
Recommendation: fix now

`.atelier/items/lor-xdoi/item.md:30` expressly leaves `AppMenu.kt`'s menu drawing in the Android app, while line 44 assigns that same item browser scripts for an app menu and a long press drifting into a drag. `.atelier/items/lor-q165/item.md` also requires current to open an app's long-press menu by URL. The audit (`.atelier/missions/lor-2803/audit/launcher-home.md:266`) and `android/app/src/main/java/com/spadaval/lor/home/AppMenu.kt` identify this as the actual shipped AppMenu composable. Neither the panels move nor the settings move owns it. Consequently following the explicit split boundary leaves a required current surface unavailable in wasm; moving only HomeContext and HomeActions does not move its drawing.

Include the app-menu drawing and its popover dependencies in lor-xdoi's shared drawing, with shortcuts and Android actions supplied as state/callbacks, and require its fixture and script there. If a separate browser implementation is intended instead, assign it explicitly and explain how it satisfies the current-code comparison. Remove the contradictory instruction to keep that drawing only in the app.
