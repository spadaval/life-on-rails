---
schema: atelier.item
schema_version: 3
id: lor-j4nb
title: Goals in even rows, with habits and projects told apart at a glance
status: approved
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
- lor-w2tw
implementers:
- thread:delegated-task:command%3Amcp%3Aab2feab2-8ed8-40be-ad1b-e17103ce5d1b%3Adelegate-task%3A07e83177-feed-40b0-9af9-55ad32d79658
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.066996549Z
---

## Goal

The Goals page is a tidy grid, and a habit, a project and a task count are told apart without reading a legend.

## Done when

- [x] Goal cards line up in rows: cards in one row start at the same height, and no card leaves a gap above it. The Report says how (equal-height rows, or columns that pack) and shows `/goals?at=15:00&data=base` at 1500px.
- [x] A habit row and a project row differ by more than a square and a dot: each has a clear mark or label, and a habit shows how often it is due ("1 per day") beside its Did it.
- [x] A card's task count is said once. Today each project row shows its count and the card's foot shows a total; the Report says which was kept.
- [x] A goal over its week's target looks different from one under it (today 16 / 15h draws the same full bar as 15 / 15h would, because `Meter` caps its width at the target).
- [x] The editor's Weight field has a line saying what it does, in plain words taken from how scoring uses it.
- [x] Did it, selecting a row and opening the editor still work: the `goals` flow in `scripts/ui-check.mjs`, as lor-w2tw repaired it, asserts each, is changed with the page, and `verify-lor screen goals` passes.
- [x] `node scripts/shots.mjs --only Goals` runs clean, and `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/routes/goals.tsx`, `src/components/IntentEditor.tsx` and the `goals` flow in `scripts/ui-check.mjs`. Nothing about how hours or scores are computed.

## Report

The Goals page now lines up in even rows, says each row's kind in words, and counts each task once. A goal over its target no longer looks like one exactly on it. Round 2 is at commit de999628.

- **Even rows.** I used equal-height rows. The cards' grid no longer sets `items-start`, so each card stretches to the tallest card in its row. Every row starts level, and no card leaves a gap above it. A short card ends with empty space at its foot. See `/goals?at=15:00&data=base` at 1500px in `/tmp/lor-verify/t3-3debaeed-5035163d6aad5ee9/r2-shots/goals@1500.png`.
- **Habit and project told apart.** The square and dot are replaced by a fixed-width kind label in small caps: PROJECT, HABIT, METRIC or GOAL. HABIT is drawn in `or-deep` and the others in `dim`. A habit row shows how often it is due ("1 per day", "3 per week") beside its Did it. When a habit has no frequency, the row shows nothing there. A row's name keeps at least 11rem and wraps instead of truncating. When the details beside it (hours, cadence, Did it, task count) do not fit, they wrap under it, right-aligned. `Panel`'s header now also wraps, so a long hours line drops under the goal's title instead of squeezing it. Checked at 1500, 1280 and 375px: no name is cut, and the page is exactly the viewport's width at each (`/tmp/lor-verify/t3-3debaeed-5035163d6aad5ee9/r2-widths/`, goals-<width>.png and geometry.json).
- **Task count said once.** I kept each project row's own count. The card's foot no longer shows the total. It counts the goal's open tasks that no project or goal row on the card already counts, which means tasks filed on the goal itself or under a habit: "2 tasks outside its projects". On a card with no project rows it reads "2 tasks", and with nothing left over it shows nothing. Every task is counted once, and none goes uncounted. The foot links to `/tasks?for=<goal>&group=for`, where those tasks sit in their own groups. The goal's full total is still in its side panel. A habit's side panel now shows its Tasks line when it has any.
- **Over target.** I fixed `Meter` in `src/components/ui.tsx`, which is outside this item's listed scope, because the cap was in `Meter` itself. Over target, the target becomes a notch at target/hours of the width, and the hours past it are drawn in light `txt/70`. Under target, the meter draws as before. Today's goal meters (`src/routes/index.tsx`) get the same fix. The card's hours line reads "18.7 / 15h this week · 3.7h over" and does not break mid-phrase. The side panel's This week line says the same ("· 3.7h over") unless the goal is already marked under target.
- **Weight hint.** The hints come from the code that reads `intent.weight`. For a goal: at 2 or more, it is marked under target when the last 7 days gave it less than half its hours (`starving`, `src/server/scoring/recompute.ts`), and the heaviest such goal is suggested for free time first (`src/server/today.ts`). For a metric: Today asks for the heaviest metric not yet logged that day. For a project or habit, the hint says weight is not used in scoring yet. That is true: a goal's mix of its children uses the link's weight, not this one. On a goal the field spans both columns, so its hint stays readable.
- **Flow.** The `goals` flow in `scripts/ui-check.mjs` now also asserts the PROJECT and HABIT labels, "1 per day" before Did it, and the Weight hint on a project. It now also files a task, "Buy Neha a card", under the habit, and asserts that the foot reads "2 tasks outside its projects" (Book flights plus that task) with no 4-task total. Its earlier checks of Did it, selecting a row and opening the editor are unchanged and pass. `verify-lor screen goals` passed all 10 checks on de999628 with no browser errors: `/tmp/lor-verify/t3-3debaeed-5035163d6aad5ee9/r2-goals/`.
- **Checks.** `node scripts/shots.mjs --only Goals` against the built test server with the demo set (`LOR_FAKE_NOW=2026-11-17T15:00`) reported `problems: []`. `pnpm typecheck` passed, and `pnpm test` passed 213 tests in 25 files. The goal editor with its hint is in `/tmp/lor-verify/t3-3debaeed-5035163d6aad5ee9/goal-editor/goals.png`.

Two tooling notes, not fixed here:

- The verify-lor skill says to record receipts with `atelier evidence record`, but this Atelier has no `evidence` command, so the evidence paths are listed above instead.
- `:3857` and `life_on_rails_test` are shared by every checkout. Another thread was restarting the server and replacing its data while I worked, so I waited for it to finish before I ran `up`, `demo` and the flow. Once in round 2, another checkout replaced my server between steps, and I ran `demo` against its database. I threw those measurements away and redid every check after confirming the server was mine. lor-3ydk and lor-ujhg cover this.

### Responses

1-122j.1: fixed in a7a5f5ea
1-122j.2: fixed in a7a5f5ea and de999628
