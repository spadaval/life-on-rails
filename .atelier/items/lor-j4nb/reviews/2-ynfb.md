---
schema: atelier.review
schema_version: 3
round: 2
commit: 159eea461b3e75634acd42bf7ce8ea0f7da53bd6
reviewers:
- thread:delegated-task:command%3Amcp%3A3a509556-bf30-417d-8290-19b0b83bf1f9%3Adelegate-task%3A4d7b7164-0b32-45e3-a48e-aa670f3703ad
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-26T00:45:25.754682610Z
---

## Summary

Complexity: low. The revision changes derived task counts and flex layout, with no new stored state or scoring behavior.

Value: medium. Goals is a current screen, and the revision preserves tasks under habits while making names readable at narrower widths.

Risk of merging: low. These are visible UI changes that are cheap to revert; they do not change stored data or scoring.

The value is worth this size of change. The set of task ids lets the footer exclude tasks already represented by visible project or goal rows without losing habit-owned tasks. Wrapping the shared Panel header is a small, appropriate fix for its title and aside competing for width.

I read the changes from 49a4603b through submitted commit 159eea46, traced the task ancestry and Tasks filter, and checked both carried findings. I also checked the unchanged Meter and Weight hints against their callers and scoring readers. The revision retains equal-height card rows, explicit kind labels and cadence, and distinct over-target rendering.

`pnpm typecheck` passed. `pnpm test` passed all 213 tests in 25 files. Both commands emitted the existing symlinked-node_modules install warning before running successfully. `git diff --check` passed for the whole change and the revision.

The independent `verify-lor screen goals` run passed all 10 assertions with no browser errors. It checked the two project tasks, the two other tasks including the habit-owned task, Did it writing one commitment, row selection, the editor and Weight hint, and the project task link. Its log and screenshots are in `/tmp/lor-verify/t3-3debaeed-5035163d6aad5ee9/review-r2-goals/`. Before running it, doctor identified this checkout's built copy on the test database at :3857, and the copied goals.tsx and ui.tsx matched the submitted source.

I inspected the author's `r2-shots/goals@1500.png`, the `r2-widths/goals-1280.png` and `goals-375.png` screenshots, and `r2-widths/geometry.json` under the same evidence root. At 1500px the card pairs share their top and bottom; at 1280px the habit names and goal titles are readable, with details below when needed; the 375px page fits its viewport. The supplied gallery receipt reports no problems. I did not repeat the gallery or complete extra footer-link and deep-nesting probes: another checkout replaced the shared test fixtures and stopped this checkout's server after the passing flow, so the supplemental browser could no longer find the wedding card. This is the shared verification interference already recorded in lor-3ydk and lor-ujhg, not evidence against this revision. I started no additional server and left the other checkout's server alone.

## Finding 1: Keep tasks under habits represented after removing the goal total
Recommendation: fix now
Was: 1-122j.1
Response: fixed in a7a5f5ea
Now: fixed

The footer now subtracts the task ids represented by visible project or goal rows from all of the goal's descendant task ids. A task under a habit therefore remains in the footer, and selecting a habit with tasks exposes its Tasks link. The extended flow independently passed with two tasks outside the project, including Buy Neha a card under Call Neha, and no four-task total.

## Finding 2: Make the added metadata fit narrower cards without hiding their names
Recommendation: fix now
Was: 1-122j.2
Response: fixed in a7a5f5ea and de999628
Now: fixed

The row name no longer truncates, its group has a minimum width, and the details move below it when they do not fit. Panel headers also wrap instead of squeezing the goal title beside the hours. The supplied 1280px and 375px screenshots and geometry address the reported demo regressions: the names are readable and document width equals viewport width.
