---
schema: atelier.review
schema_version: 3
round: 5
commit: e1019107e89957765ed7cb1b5abeb4522e44ebb0
reviewers:
- 01a1e838-e7ae-7353-9a65-b2a183376cc8
model: grok-4.6
verdict: changes
created_at: 2026-11-17T06:37:08.760854755Z
---

## Summary

Complexity: high. The plan still adds a design-lab subsystem, pulls the homecore split ahead of ncpw's sync, then two viewers and three to five concepts; several owners, and a contract ncpw's items must keep.

Value: high. Sumeet named roughness as the biggest issue and asked for rapid-development tools plus 3–5 usable concepts with a dashboard prototype and an in-browser phone mockup.

Risk of merging it: medium. Agreeing the plan authorizes a Kotlin upgrade and extracting the running launcher into a shared module; a break shows on the emulator receipts, and the real phone is out of scope.

The outcome is still worth the size. I am not recording Shrink or Reject. Round 5 closes the carried publish finding. One acceptance state still has no owner: the first-run set-up card lives on home and is named in q165, and no move item lists `Setup.kt`.

Checked commit `e101910` as it stands: the mission, all thirteen of its items, lor-xdoi, lor-dkut, lor-3hnk, lor-sghw, lor-3ilp, mission lor-ncpw (`mission.md` and `design.md`), `.atelier/missions/lor-2803/audit/` (README plus the three inventories), and the current code those items name (`useNow` / `useTick`, `people.tsx` `filter=overdue`, `notes.tsx` `version` for history, `home/Setup.kt` and its call from `HomeScreen.kt`). Compared each item's `blocked_by` to its Done when, and the carried finding to the round-5 text of lor-glev and lor-zwfd. `atelier list --mission lor-ncpw` on this checkout reads ncpw as agreed, with lor-3hnk and lor-sghw able to start.

Could not check: whether the 15-second wasm loop holds under the lor-0v68 cap (lor-vbfd already takes a miss to Sumeet); a live `/lab/` (it does not exist yet); the two claude.ai artifact links (only Sumeet can open them; the inventories are in-repo). Did not read another reviewer's file.

## Finding 1: Nothing both owns and copies the comparison page into the served `/lab/` tree
Recommendation: fix now
Was: 4-ajse.7
Now: fixed

lor-zwfd line 29 now makes `publish.sh` a list of source and folder pairs that a later item extends by one line, rebuilds the served tree from that list on a clean publish, and names lor-glev's add: `docs/design/lab/compare/` to `/lab/compare/`, linked first from the index when present. lor-glev line 33 and its Scope (line 39) take that one line. The two stories agree, and a publish after glev lands serves the comparison from source.

## Finding 2: q165's first-run set-up card is not in any item's move list
Recommendation: fix now

lor-q165 line 31 requires, by link alone in the lab's `current` version, "a first-run home with the set-up card". lor-oech line 30 puts q165's ten launcher states in `STATES.md` "under those names, each added by the item that moves its surface".

The card is `SetupCard` in `android/app/src/main/java/com/spadaval/lor/home/Setup.kt` (lines 75–158), called from the start stack's notes in `HomeScreen.kt` line 820. The audit inventories it as home §1.18 (`.atelier/missions/lor-2803/audit/launcher-home.md` lines 494–504) and as panels-app §1.13.

No move item names it. lor-xdoi line 30 lists the home files that go into `homecore` and the Android halves that stay in the app: `Setup.kt` is in neither list. lor-rgx4 moves the agent page, pair card, LoR panel and `Pill`; lor-t8lq moves Home settings and the app tabs. The card is Android-heavy (`Prefs`, `T3Connection`, `LorClient`, `isHome`, `MainActivity.open`), so an xdoi extract of `HomeScreen.kt`'s drawing will leave it as a slot or in the app, the way line 30 already keeps `AgentPage`. AgentPage is later moved by lor-rgx4. Nothing does that for the set-up card, so the first-run fixture has no drawing in the lab.

Name `home/Setup.kt` on lor-xdoi (drawing from fixture state and callbacks, Android reads and intents staying in the app) and have that item add q165's first-run fixture, the same shape as the menus on lines 38–39.
