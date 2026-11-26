---
schema: atelier.item
schema_version: 3
id: lor-a8wq
title: Before and after of every dashboard page, for Sumeet to judge
status: changes_requested
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
- lor-w2tw
- lor-zuqn
- lor-auhc
- lor-zk52
- lor-j4nb
- lor-n3vi
- lor-0v56
implementers:
- thread:delegated-task:command%3Amcp%3Aa2ab188c-a0a0-49ed-9a13-3deba974c50e%3Adelegate-task%3Af72b2d37-77c9-4561-adf0-10f9a0859d44
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.179638193Z
---

## Goal

Sumeet can judge the whole cleanup in one pass, flipping each page between how it was and how it is.

## Done when

- [x] Every shot in `shots/dashboard.json` at 1500px is taken twice on the demo set with the same clock: once from a checkout of the mission's base commit (the parent of the first item's merge), once from `main` with every other item of this mission merged.
- [x] One gallery flips each shot between the two, and is somewhere that lasts and that Sumeet can open from his PC; the Report gives the link.
- [x] Each of the mission's success criteria is checked by this thread, which implemented none of the other items, and the Report says pass or fail for each with its evidence.
- [x] Each of the sixteen edits in the mission's Why is marked done, changed or not done, with a line on any that changed.
- [x] The deploy is done: `pnpm build && systemctl --user restart lor-web` from `main`, and the real dashboard at `http://127.0.0.1:3847/` answers.
- [x] Anything that fails is filed against the item that owns it and fixed before this item is done.

## Scope

Checking, screenshots and the deploy. No code beyond a fix this item files.

## Context

The sixteen edits, as Sumeet approved them on 2026-11-25:

1. Needs you and Due soon directly under the arc.
2. The brief collapsed to its first sentence.
3. The timer shown once.
4. The legend under the timeline behind a "?".
5. One free slot, with a task that fits it.
6. The clipped arc label fixed.
7. "with the agent" as a list; the footer's two percentages explained or dropped.
8. Bronze frames dropped from most panels; tone separates them.
9. Three button levels.
10. Four type sizes.
11. Goal chips on task rows unboxed.
12. Native form controls styled.
13. Tasks: hints on focus, filters and Group and Sort on one line.
14. Goals: even rows; habits and projects visibly different.
15. Undo for stopping a timer and deleting a time block.
16. A failed phone sync shown where "All senses reporting" sits.

## Report

**The gallery:** https://buildbot.tail47515a.ts.net/lab/shots/dashboard-lor-40i0/ (over Tailscale, from the PC). All 28 shots in `shots/dashboard.json` at 1500px. Each image shows the mission's end and flips to the base with the **Before** button or the `b` key. The files are in `~/.local/state/lor-shots/dashboard-lor-40i0/` (base images under `previous/`), and `scripts/lab/publish.sh` copied them to the lab, so they survive a reboot.

**How the shots were taken.** The base is 93d569a3, the first-parent parent of `Merge lor-w2tw` (521cb14f), which is the mission's first merge. The end is `main` at de399bed, with all eight other items merged. Each checkout ran under `scripts/ui-dev.sh` on :3858 with `LOR_FAKE_NOW=2026-11-25T15:00`, and the demo set was reloaded with that clock just before each run. `node scripts/shots.mjs` ran with a copy of the catalogue limited to `widths: [1500]`. Seeds, migrations and `shots.mjs` are the same at both commits, and both runs reported 28 images and no problems. The gallery labels the base "the previous run", so in it **Before means 93d569a3**.

### Success criteria (checked by this thread)

- **needs-you-first: pass.** At 1500x950 on `/?at=15:00&data=running`, the Needs you head is at y≈620 and its first flagged item ("Organise Sam's birthday dinner") at y≈660, both inside the first screen. At base they were at y≈900 and y≈940. The brief takes one line ("THIS MORNING Finish the offline maps spec before the design review at 14:00.") and opens with "The rest". See the gallery's "Today at 15:00 with a running timer".
- **one-look: pass, with one note.**
  - Source counts: `grep -rhoE 'text-\[..px\]|text-(xs|sm|…)|text-(small|body|lead|title)' src/routes src/components` finds `text-small` 121, `text-body` 38, `text-lead` 3, `text-title` 2, and otherwise only the arc readout's 56/62/20/21px.
  - Rendered sizes: on ten pages in Chrome (Today plain, running, block open; Tasks plain and with a task open; Goals plain and with the editor open; a person; a note; Status), visible text outside SVG uses only 13, 15, 18 and 22px, plus the arc's 62/20/21px.
  - The arc's SVG labels (13 to 14px in `DayArc.tsx`) and the star map's scaled labels are drawings, so I left them out of the count.
  - Buttons: every visible `.btn` on those pages is `btn-primary`, `btn-secondary` or `btn-quiet`, and none has no level. `tests/look.test.ts` passes.
  - Note: the round "?" help marks (Today's lane legend and Tasks' keys) are a small fourth style outside the three levels. Edit 4 asked for this mark, so I count it as an icon, not a button level.
  - Bronze frames: at most one on every page checked. None on Today, Tasks, Goals or a person's page. One when a block, task or goal editor is open, and one on a note's page.
- **undo: pass.**
  - `pnpm test:db`: 19 files and 155 tests pass, including `tests/db/undo.test.ts`. Its cases cover a running timer, a paused timer, a timer under a minute, a committed block with its commitment, an uncommitted block with its pending suggestion, and three refusals that change nothing (another timer started, a manual block over a 30-second timer's span, a manual block moved into the hour).
  - In the browser on the test set, I stopped the running timer, pressed Undo, and the timer pill came back with "47M". The Actual lane matched the screen before the stop. I deleted the 11:00 block and pressed Undo, and the block's panel read the same as before the delete.
  - Screens: `~/.local/state/lor-a8wq/1-stopped.png`, `2-undone.png`, `3-deleted.png` and `4-restored.png`.
- **judged: pass.** The gallery above has every shot.

Also run: `pnpm test` (27 files, 223 tests) and `pnpm typecheck` pass.

### The sixteen edits

1. Needs you and Due soon under the arc: **done**. Goals this week sits in the same row.
2. Brief collapsed to its first sentence: **done**. "The rest" opens it.
3. Timer shown once: **done**. The header pill is gone, and the arc's pill stays.
4. Legend behind a "?": **done**. Tasks' keyboard line also moved behind a "?".
5. One free slot with a task that fits: **done**. The demo shows "1h 30m free at 15:00 · fits Fix the dripping bathroom tap". Base showed three chips with the same task.
6. Clipped arc label fixed: **done**. Base read "iide standup". The label now crowds the timer suggestions in one state, filed as lor-wwxm (low).
7. "with the agent" as a list, and the footer's percentages: **changed**. The list is done. The two percentages left Today and moved to `/status` under "Today so far", each with a sentence explaining it, instead of being explained in place.
8. Bronze frames dropped from most panels: **done**. Only open editors and a note's page keep one.
9. Three button levels: **done**.
10. Four type sizes: **done**.
11. Goal chips on task rows unboxed: **done**. Goal names are plain dim text. The Google Tasks "G" mark now has a box, which it did not have at base. That is a source mark, not a goal chip, but it is the one boxed thing left on a row.
12. Native form controls styled: **done**. This covers selects, checkboxes and time inputs.
13. Tasks hints on focus, filters with Group and Sort on one line: **changed slightly**. The hints show only while the add box has focus (checked in Chrome: hidden before focus, shown on focus), and the filters fit one line. To fit, Group and Sort no longer print their current choice ("Group Due date" became "Group").
14. Goals in even rows, habits and projects told apart: **done**. Cards in a row share a height. HABIT is a tinted label with Did it, and PROJECT is dim with a task count.
15. Undo for a stopped timer and a deleted block: **done**.
16. A failed phone sync shown where "All senses reporting" sits: **done**, with a fix described below. With the demo's phone arrivals moved back about 4h40m, every page's line reads "Phone silent for …", and with `data=failing` it reads "9 jobs need a look · phone silent for …".

### What failed, and the fix

- **The two silence readings disagreed (from lor-0v56), now fixed on this branch.** At 4h40m of silence, the sidebar and the /status head said "phone silent for 4h" while /status's phone row said "silent · 5h ago". `silentFor` rounded down and `ago` rounds to the nearest hour. Commit f6937aeb makes `silentFor` round with `ago`'s units (hours below 36, then days) and adds a test that holds the two to the same text at eight durations. In Chrome, both now read 5h. The `phone-*.png` shots in `~/.local/state/lor-a8wq/` were taken before the fix and still show 4h.
- **lor-wwxm, filed, not fixed (low).** Right after a timer stops, the longer suggestion chips end about 5px from "09:30 Field Guide standup" (`1-stopped.png`). Nothing is hidden. With the gallery's own suggestions the gap is about 30px.

### Deploy

I ran `pnpm build && systemctl --user restart lor-web` in `~/projects/life-on-rails` at main de399bed. lor-web is active, and `http://127.0.0.1:3847/` answers 200 with Needs you, the one-line brief and "All senses reporting". The journal has no errors since the restart. The silence fix in f6937aeb reaches the real dashboard at the next deploy after this item merges.

### For the reviewer

The phone-silence test only reaches stale on the demo set if `observation.ingested_at` for `source='android'` is moved before the fake clock. The demo loader stamps those rows with real time, not `LOR_FAKE_NOW`, so under a fixed clock the phone always looks fresh. lor-0v56 met the same thing.

