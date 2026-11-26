---
schema: atelier.item
schema_version: 3
id: lor-zuqn
title: 'Today leads with what needs you: lists under the arc, a one-line brief, one timer'
status: approved
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
- lor-w2tw
implementers:
- thread:delegated-task:command%3Amcp%3Ae517f1eb-1e70-42c5-ab63-9280bb58c045%3Adelegate-task%3Aa9276fd7-b621-4958-a181-dba39194efba
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:45.954561521Z
---

## Goal

Opening Today shows what needs Sumeet without scrolling, and nothing on the page is said twice.

## Done when

- [x] Needs you, Due soon and Goals this week come directly after the arc, the timeline strip and an open block editor. The brief, the day record and the Ask box come after them.
- [x] At 1500 by 950, `/?at=15:00&data=running` shows the Needs you head and its first item without scrolling. The Report has the screenshot.
- [x] The morning brief shows its first sentence on one line with a control that opens the rest; opening and closing it works by mouse and by keyboard, and its state survives a data refresh on the page.
- [x] On Today the running timer appears once: the card under the arc, which has Pause and Stop, stays, and the page head's chip is not drawn on Today. On every other page the chip stays as it is, a link to Today.
- [x] "with the agent" in Needs you is a list, one task a line, each opening its task, instead of a run-on paragraph.
- [x] The footer's "Calendar confirmed N% · time accounted for N%" is gone from Today and both numbers are on `/status` with a sentence each saying what they measure. "Too much? Reset visibility" stays.
- [x] `shots/dashboard.json` has a shot of Today with the brief open, and `node scripts/shots.mjs --only Today` runs clean.
- [x] The `today` flow in `scripts/ui-check.mjs`, as lor-w2tw repaired it, is changed with the page so it drives the new order and the brief's open and close, and `verify-lor screen today` passes.
- [x] `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/routes/index.tsx`, `src/routes/status.tsx`, `GlobalActions` in `src/routes/__root.tsx` (it takes a flag, or reads the route, so Today keeps Add task and Search and drops only the timer chip), `shots/dashboard.json`, the `today` flow in `scripts/ui-check.mjs`, and the verify skill's `references/features/today.md`. Not the timeline strip (lor-auhc) and not the block editor's behaviour.

## Context

- The order today is in `Today` in `src/routes/index.tsx`: arc, lanes, block panel, brief, day record, Ask, then the three lists.
- The two percentages come from `t.confirm_rate` and `t.coverage`.

## Report

Today now opens on what needs Sumeet: at 1500 by 950, `/?at=15:00&data=running` on the demo set shows the Needs you head at about 640px and its first item ("Organise Sam's birthday dinner") ending at about 730px, with no scrolling. Screenshot: `/tmp/lor-verify/t3-e2e303f2-1331f518926b970a/zuqn/today-running-1500x950.png` (the full page is `today.png` beside it).

What changed, in commit f29caa98:

- **Order.** In `Today` (`src/routes/index.tsx`) the three lists follow the arc, the lanes and an open block panel; the brief, the day record and the Ask box come after them.
- **The brief** shows its first sentence on one line, cut with an ellipsis if longer, with a quiet "The rest" button (`aria-expanded`, `aria-controls="brief-rest"`) that opens the rest and becomes "Less". A native button, so Enter and Space work. Its state lives in the component, which a data refresh does not remount; the flow proves it stays open across Pause's refresh. The sentence split is `splitBrief` in `src/components/format.ts`, tested in `tests/shared/brief.test.ts` (not split inside "12.5" or "8.30"; a one-sentence brief has no button). `brief=1` opens it by link, following `edit=1` and `history=1`. "from the agent" moved from the panel aside into the opened text ("From the agent's morning run.") so the closed line stays one line.
- **One timer.** `GlobalActions` takes `timerChip` (default true). Today passes `timerChip={!t.is_today}`, so on today the chip is gone and the card under the arc remains, while a past day's head still shows the chip linking to Today, since that page has no card. With no timer running, Today's head still shows "Start timer", as the Scope asked ("drops only the timer chip").
- **with the agent** is a `<ul>`, one link a line to `/tasks?view=agent&id=<id>`.
- **The percentages** left Today's footer, which now holds only "Too much? Reset visibility". `/status` has a "Today so far" panel with Calendar confirmed and Time accounted for, each with a sentence. The numbers come from a new `todayMeasures` in `src/server/today.ts`, which runs the same `measures` Today used over today's waking time so far; `getCaptureStatus` adds them, and `getStatus` (`/health`, `lor status`) is unchanged.
- **shots/dashboard.json** has "Today with the morning brief open" (`/?brief=1&at=15:00&data=base`). "Today scrolled to the lists" (scroll=700) is renamed "Today scrolled to the brief and the Ask box", because that is what it now shows.

Checks:

- `node .claude/skills/verify-lor/verify.mjs screen today` passes on the test server built from this tree. Every check is `ok` in `/tmp/lor-verify/t3-e2e303f2-1331f518926b970a/zuqn/today.log`. New checks: the order after the lanes; the first Needs you item inside 950px; no percentages in the footer; "with the agent" as two task links; the brief closed on load, its first sentence on one line, opened and closed by click, Enter and Space; still open after Pause; no chip in Today's head while running, and a chip linking to `/` on `/tasks`; the open block panel between the lanes and Needs you; both measures with their sentences on `/status`. Screenshots `today-1-open` to `today-8-status`, `today-1b-brief-open` among them.
- `node scripts/shots.mjs --only Today --base http://127.0.0.1:3857` ran clean: 10 shots, 30 images, `problems: []`. The brief-open shot at 1500 is copied to `/tmp/lor-verify/t3-e2e303f2-1331f518926b970a/zuqn/today-with-the-morning-brief-open@1500.png`.
- `pnpm typecheck` and `pnpm test` pass (25 files, 212 tests). Typecheck needs `src/routeTree.gen.ts`, which is gitignored and generated only by a Vite build; I copied it from the verify copy's build.

Noticed:

- The first `verify.mjs demo` failed with `demo: no intent 01a2…`, and a second run straight after succeeded. Most likely another thread was writing the shared `life_on_rails_test` at the same moment. I did not file it, because I could not show the cause.
- The picker under "Something else…" still spills out of the arc (the Gotcha in `today.md`). It is unchanged and out of scope.

The finding's fix: the brief's button also shows when the first sentence alone overflows its line ("More"), measured with a ResizeObserver while closed, and `aria-controls` names the sentence and the rest. The `today` flow now ends at 412px with a one-sentence brief: it is cut off, has the button, opens by Enter to the whole sentence, and closes by click. The re-run of `verify-lor screen today` (33 checks ok) is in `/tmp/lor-verify/t3-e2e303f2-1331f518926b970a/zuqn-r2/` with `today-9-brief-one-sentence-412.png`. `pnpm typecheck` and `pnpm test` (212) pass.

### Responses

1-ywgv.1: fixed in bebe23c4

