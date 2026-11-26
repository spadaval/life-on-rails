---
schema: atelier.mission
schema_version: 3
id: lor-40i0
title: 'The dashboard cleaned up where it stands: what needs you first, fewer boxes, undo'
status: done
labels:
- dashboard
lead: e05499fa-7ab0-4d4d-87b3-517340323121
planners:
- e05499fa-7ab0-4d4d-87b3-517340323121
created_at: 2026-11-25T23:37:29.082451781Z
---

## Why

Sumeet asked for this on 2026-11-25. He looked at the design lab's four concepts (mission lor-2803) and rejected all of them: "very sterile, very enterprisey, very devoid of style and personality", and of the concept pages, "I kinda hate every single thing on that page". He also said the shipped dashboard "probably did veer a little too far into the whimsical". His direction: "Let's try starting from the current one and making targeted edits", to "clean it up a little and make it a little more useful". He was shown the sixteen edits below as a list and said "This seems good, do it."

The shipped dashboard's structure and character hold up: the day arc, the amber, the condensed heads. What makes it rough is repetition and noise. On Today, the morning brief and the Ask box sit above Needs you and Due soon (`src/routes/index.tsx`), so with a long brief the things that want him start about 1,800px down (the audit of 16 November, as `docs/design/lab/brief.md` sums it up). The running timer shows twice. A legend is printed permanently under the timeline. Every free-time chip suggests the same task, because `src/server/today.ts` asks for one suggestion and gives it to every gap. Every panel has the same bronze frame, every button the same outline, and the pages use 13, 12.5, 14 and 13.5px text 136 times between them, so nothing leads. Stopping a timer and deleting a time block cannot be undone.

## Outcome

- **Today shows what needs him first.** Needs you, Due soon and Goals this week sit directly under the arc and the timeline. The morning brief is one line that opens on a click. The running timer appears once on the page.
- **The timeline strip says less.** No permanent legend, one free slot with a task that fits it, and no arc label cut off at the edge.
- **Panels differ by tone, not by frame.** The bronze chamfered frame is kept for what should stand out; everything else is a tone of the ground.
- **Buttons have three levels** and text has four sizes, on every page.
- **Tasks and Goals read cleaner.** Goal names on task rows are plain dim text, the add hints show only while adding, the filters fit one line, and goal cards line up in rows with habits and projects visibly different.
- **A stopped timer and a deleted time block can be undone.**
- **A phone that has stopped reporting shows on every page,** in the sidebar's health line.
- **The look stays Mostly Harmless.** The arc, the amber, the condensed caps heads, the reticle and the voice ("Rising", "Setting", "All senses reporting") are kept.

## Success criteria

- needs-you-first: On the demo set at 1500 by 950, `/?at=15:00&data=running` shows the Needs you head and its first flagged item without scrolling, and the brief takes one line until clicked. Shown by lor-zuqn's Report, with the screenshot.
- one-look: Across `src/routes` and `src/components`, body text uses four sizes (display sizes for the arc's readout aside), every button is one of three named levels, and on Today, Tasks, Goals and a person's page at most two panels per screen carry the bronze frame. Shown by lor-mxm9's Report, with the counts and the screenshots.
- undo: On the test server, stopping a timer and then Undo puts it back as it was, running or paused, a timer under a minute included; deleting a time block and then Undo restores it with its times, intent, commitment and proposals; the fill around both is as it was; and an undo that would overlap a later write is refused and changes nothing. Shown by lor-n3vi's Report and its `pnpm test:db` tests.
- judged: Sumeet has a gallery that flips every shot in `shots/dashboard.json` at 1500px between the mission's base commit and its end. Shown by lor-a8wq.

## Scope

In: `src/routes/` (Today, Tasks, Goals, People, Notes, search, status, the shell), `src/components/`, `src/styles.css`, `DESIGN.md` where a rule in it changes, `shots/dashboard.json` where a state is added, and the server functions the undo and the phone health line need (`src/server/attribution/blocks.ts`, `src/server/today.ts`, `src/server/device.ts`, the shell's loader).

Out:
- Phone-width layout. Sumeet, 2026-11-25: "Phone-width isn't all that important - the launcher panel is the primary interface on a phone anyway." Nothing here may make a page worse at 412px than it is at the base commit, and nothing has to make it better.
- The phone app, the launcher and the wall display.
- The design lab's concepts and its `current` copy in `prototype/dashboard/`, which stays as the look at 48477ea.
- New features beyond the two under "more useful": undo, and the phone health line.
- A new palette, a new typeface or a new layout for any page. This is a cleanup of the look in `DESIGN.md`, not a new one.

Environment: the dev server on the test database (`scripts/ui-dev.sh`, `:3858`) with the demo set (`pnpm demo:test`), screenshots by `node scripts/shots.mjs`, and checks through the verify skill (`.claude/skills/verify-lor/SKILL.md`). Its `today` flow fails on main at its first step and its `goals` flow asserts nothing, so lor-w2tw, which repairs both, is this mission's and comes before the items that rely on them. Each page item then keeps its flow current with the page. Never `pnpm build` in the main checkout.

## Authority

The lead may change the items, their order and their wording; choose the four type sizes, the tones, the radius, the three button styles and which panels keep the frame; and choose how long Undo is offered and how stale a phone is before the health line says so. These come back to Sumeet: dropping the arc, the amber, the condensed heads or the voice; removing a thing a page shows rather than moving or collapsing it (the footer's two percentages are the one exception, see lor-zuqn); and anything that changes what scoring counts.

## Result

Done and deployed on 2026-11-26: `lor-web` runs main at d50b7d37. The gallery at `/lab/shots/dashboard-lor-40i0/` flips all 28 dashboard shots at 1500px between the mission's start (93d569a3) and its end. lor-a8wq checked the four success criteria itself and all pass.

Of the sixteen edits Sumeet approved, fourteen were built as approved and two came out differently:

- The footer's "Calendar confirmed" and "time accounted for" percentages moved to `/status`, each with a sentence saying what it measures, instead of being dropped.
- To fit Tasks' filters on one line, Group and Sort show their value only when it is not the view's default.

Stop stayed on Today's timer card only; the page head's chip gained none. The phone health line reports silence past 2 hours, chosen from two weeks of upload gaps (longest ordinary gap 46 minutes).

Found and fixed alongside, outside the plan: `pnpm` on buildbot (lor-9lxd), Today's timer picker ignoring mouse clicks (lor-ug9r), and free slots losing the behind-goal suggestion that lor-auhc's wording dropped (lor-yijs).

Not done: phone-width layout, by Sumeet's call. Left open: lor-wwxm (an arc label crowds the timer suggestions after a stop) and lor-c8zb (Tasks' Group and Sort at phone width). The shared test server and database cost reruns throughout; lor-3ydk, lor-j0qy, lor-rdbu, lor-ujhg and lor-uuut all report it.
