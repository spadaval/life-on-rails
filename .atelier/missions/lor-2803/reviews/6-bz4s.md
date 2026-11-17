---
schema: atelier.review
schema_version: 3
round: 6
commit: 4d6212c5a8038a3f9160329489ffa0961adc8af3
reviewers:
- 01a1e83f-008c-7cb2-ae26-c66565be1542
model: grok-4.6
verdict: approve
created_at: 2026-11-17T06:44:11.368213658Z
---

## Summary

Complexity: high. The plan still adds a design-lab subsystem, pulls the homecore split ahead of ncpw's sync, then two viewers and three to five concepts; several owners, and a contract ncpw's items must keep.

Value: high. Sumeet named roughness as the biggest issue and asked for rapid-development tools plus 3–5 usable concepts with a dashboard prototype and an in-browser phone mockup.

Risk of merging it: medium. Agreeing the plan authorizes a Kotlin upgrade and extracting the running launcher into a shared module; a break shows on the emulator receipts, and the real phone is out of scope.

The outcome is still worth the size. I am not recording Shrink or Reject. Round 6 closes the carried set-up card finding: `home/Setup.kt` now has an owner (lor-rgx4), and that item adds q165's first-run state.

Checked commit `4d6212c` as it stands: the mission, all thirteen of its items, lor-xdoi, lor-dkut, lor-3hnk, lor-sghw, lor-3ilp, mission lor-ncpw (`mission.md` and `design.md`), `.atelier/missions/lor-2803/audit/` (README plus the three inventories), and the current code those items name (`useNow` / `useTick`, `people.tsx` `filter=overdue`, `notes.tsx` `version` for history, `home/Setup.kt` and its call from `HomeScreen.kt:820`, every `home/*.kt` file). Compared each item's `blocked_by` to its Done when, the ten q165 launcher states to the three move items, and the carried finding to the round-6 text of lor-rgx4 and lor-xdoi. Listed drawing files under `home/` against xdoi line 38, rgx4 and t8lq. `atelier list --mission lor-ncpw` on this checkout reads ncpw as agreed, with lor-3hnk and lor-sghw able to start.

Could not check: whether the 15-second wasm loop holds under the lor-0v68 cap (lor-vbfd already takes a miss to Sumeet); a live `/lab/` (it does not exist yet); the two claude.ai artifact links (only Sumeet can open them; the inventories are in-repo). Did not read another reviewer's file. Left 3-js07.16 (ncpw's `design.md` still has the split wait for lor-dkut) with lor-3ilp; it was already deferred and is not needed before this mission starts once ncpw stays agreed.

## Finding 1: q165's first-run set-up card is not in any item's move list
Recommendation: fix now
Was: 5-hd7a.2
Now: fixed

lor-rgx4 line 29 now names `home/Setup.kt` among the drawing that moves into `homecore`, as the set-up card on a first-run home, which draws the pair card's actions. Line 32 adds q165's first-run state to the lab's `current` version, with each of the three steps done and not done. Scope (line 38) includes `home/Setup.kt`. lor-xdoi line 38 says no drawing file under `home/` is left without an owner, and that `Setup.kt` moves with lor-rgx4 (Home settings with lor-t8lq). The remaining unnamed `home/` files (`Answers.kt`, `HomeModel.kt`, `QuickDial.kt`, `Search.kt`, `SettingsPages.kt`, `WidgetEdit.kt`) have no `@Composable` drawing; they are matchers, ranking, data and edits.

The first-run fixture therefore has both an owner and a lab state, so the acceptance link can draw the card.
