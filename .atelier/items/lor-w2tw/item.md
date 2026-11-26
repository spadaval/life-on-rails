---
schema: atelier.item
schema_version: 3
id: lor-w2tw
title: The Today and Goals checks drive the pages as they are on main
status: in_review
mission: lor-40i0
labels: []
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3Aa51dd0ff-c339-42c3-a502-f77c8982c242%3Adelegate-task%3A187459b5-4233-4388-bbff-96ad8c6039c2
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:47:15.500746952Z
---

## Goal

`verify-lor screen today` and `verify-lor screen goals` prove what Today and Goals do, so the items that change those pages have a check that can fail. Today the `today` flow fails at its first step and the `goals` flow passes without asserting anything. This is lor-mxrc's work, done here because that item sits in a mission that is not agreed; Once this merges, the lead drops lor-mxrc as done by this item and moves lor-thnc's wait onto this item, on a backlog branch.

## Done when

- [x] The `today` flow drives Today as `src/routes/index.tsx` draws it on main: start, pause and stop a timer from its card, select and relabel a block in the Planned and actual lanes, and it passes through `verify-lor screen today`.
- [x] The `goals` flow's `tasks for vendors:` line lists Shortlist venues and Call the florist, and the flow fails if the filter shows anything else. It also asserts Did it, selecting a row and opening the editor.
- [x] `.claude/skills/verify-lor/references/features/today.md`, `goals.md` and the skill's Drive section no longer describe the broken flows.
- [x] The `today` flow runs at 1500 by 950, as the `goals` flow does and as this mission's criteria are measured.
- [x] The Report shows each flow failing against a deliberately wrong expectation and passing against the right one.

## Scope

`scripts/ui-check.mjs` and the verify-lor feature map. No product changes.

## Context

- The `today` flow waits for `button:has-text("Just start")`, "Running since", "Start a timer", `section[aria-label="Timeline"]`, "Accept all" and "Confirmed.", none of which exist in `src/routes/index.tsx` (`scripts/ui-check.mjs`, around lines 347 to 366).
- The `goals` flow logs a regex match at about line 406 and asserts nothing.

## Report

Both flows now fail when the page is wrong. `screen today` drives Today as `src/routes/index.tsx` draws it, and `screen goals` checks every step, including the filtered task list. Commit bd303be2.

**A bug the flow had to work around (not fixed here, scope says no product changes).** On Today at 1500x950, the picker under "Something else…" spills out of the day arc. The Planned and actual lanes' hour row lies over its intent buttons, so a click on "Ship Life on Rails" never lands: Playwright reports `<div class="relative h-4 text-[12.5px] text-dim">… intercepts pointer events`. The owner hits the same thing with a mouse. The flow picks with Enter in the search box, which the picker supports, and the gotcha is in `today.md`. The lead should file this as its own item.

What changed:

- `today` flow: 1500x950, on the test server's fixed clock. At `?at=15:30` it starts a timer from the card ("Something else…", title, search "ship", Enter). At 16:00 it checks the card reads 30m and pauses (open block, `paused_at` 16:00). At 16:10 the card still reads 30m, and Stop closes the block at 15:30-16:00. Then it selects the Dentist block in the lanes (drawn with the selection outline, panel says "from the calendar"), ticks the remember box and relabels it to Health admin. That is checked in `time_block`, in `attribution_rule` (`^Dentist$`) and in the lane's block title. Finally "Earlier" opens a page headed Day.
- The old flow seeded events at Los Angeles midnight while the server lays the day out in `LOR_TZ` (New York here). The flow now reads `LOR_TZ` the way `scripts/serve.mjs` does.
- `goals` flow checks: the wedding card text; the new goal's `weekly_hour_target`; Did it (the notice plus one `habit_completion` row); the selected row (`id=` in the link, `row-sel`, side panel Kind/Counts toward/Tasks); the editor (Title "Wedding vendors", Hours a week field); and `tasks for vendors:` must be exactly `Call the florist, Shortlist venues`. The old regex printed `null` because the rows had not drawn yet when it read them; the flow now waits for the first row.
- `today.md`, `goals.md` and SKILL.md's Drive section describe these flows. The "out of date, use the API" notes are gone.

Evidence: each run used `verify.mjs up` from this branch, then `verify.mjs screen <flow> <dir>`.

Right expectations, `"ok": true` for both:

```
today: ok: the timer card to show Ship Life on Rails — Writing the spec
       ok: the running card to read 30m at 16:00
       ok: an open timer block from 15:30 for Ship Life on Rails, paused at 16:00
       ok: the paused card to still read 30m at 16:10
       ok: the timer block closed at 15:30-16:00, nothing left open
       ok: one Ship Life on Rails block in the Actual lane
       ok: a 14:00-15:00 calendar block for Dentist, labelled Meetings
       ok: the Dentist block drawn as selected
       ok: the block panel to say it came from the calendar
       ok: the Dentist block relabelled to Health admin
       ok: one rule for the exact title Dentist (^Dentist$) -> Health admin
       ok: the lane's Dentist block to read Health admin
       ok: the previous day headed Day
goals: ok: the wedding card with 0 / 6h, Wedding vendors and Call Neha
       ok: a goal Ship Life on Rails at 10 h a week
       ok: one habit_completion for Call Neha
       ok: the Wedding vendors row drawn as selected
       ok: the side panel on Wedding vendors: Project, under the wedding, 2 tasks
       ok: the editor open on Wedding vendors with an Hours a week field
       tasks for vendors: Call the florist, Shortlist venues
       ok: Tasks for Wedding vendors to list Call the florist and Shortlist venues, nothing else
```

Deliberately wrong expectations (temporary edits, rebuilt with `up`, then restored and re-run green). The Dentist block was expected to stay on Meetings after relabelling, and Tasks for vendors was expected to also list Book flights. Both exit `"ok": false`:

```
today: FLOW FAILED: expected the Dentist block relabelled to Health admin; found ... 14:00-15:00 calendar Dentist -> Health admin [committed] ...
goals: FLOW FAILED: expected Tasks for Wedding vendors to list Call the florist and Shortlist venues, nothing else; found ["Call the florist","Shortlist venues"]
```

(The failure text names the right expectation because only the predicate was changed for the test.)

Notes:

- The flow leaves phone and place evidence alone. After `pnpm demo:test` the lane also holds place and phone blocks, and the page shows the demo brief. They sit below the calendar in precedence and do not touch the checks.
- Tooling: `pnpm install` fails in this worktree because corepack's pnpm 12.11.2 cache has no `bin/pnpm.cjs`. I followed `verify.mjs`'s own advice and symlinked the main checkout's `node_modules` (untracked, ignored).

### Responses

1-sb2d.1: fixed in 65053fc3
1-sb2d.2: fixed in 65053fc3
