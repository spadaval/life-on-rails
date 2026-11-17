---
schema: atelier.item
schema_version: 3
id: lor-vx69
title: 'The Queue direction on the dashboard prototype: every required screen, state and interaction'
status: in_review
mission: lor-2803
labels:
- dashboard
- design
priority: medium
blocked_by:
- lor-5uxk
implementers:
- thread:delegated-task:command%3Amcp%3Ac1ec7934-b773-472e-bc30-dc89caf2d5f4%3Adelegate-task%3Ac7b01b7e-4c16-4a7b-b0eb-f7ac1d94cc96
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.322569134Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.759309751Z
---

## Goal

Sumeet can use the Queue direction's dashboard in the prototype: pick it in the Version dropdown and do the brief's ten dashboard interactions by hand, at desktop and at phone width, on the made-up data.

## Done when

- [x] `prototype/dashboard/` has a `queue` direction, in a folder of its own with its own tokens, components and screens, built as `docs/design/lab/brief.md` describes it under "### 1. Queue" and with the brief's "Common ground for every concept" (the one search field that also adds and asks, Sumeet's four look asks, standard controls).
- [x] It draws the brief's dashboard screens (Today in the afternoon and in the morning, Tasks with a task open, Goals with one goal at "no data yet", People with the list and one person) at 1440, 1100 and 412 px.
- [x] It draws the brief's required states that reach the dashboard (nothing wants you, an empty day at 09:00, LoR's last data shown as stale, a crowded queue), each opened by a link, and the Report says where each appears.
- [x] The brief's ten dashboard interactions work by hand at 1440 and at 412 px, Undo is offered only where the brief allows it, and the Report lists each interaction with how it is done in this direction.
- [x] `shots/prototype.json` gains this direction's screens and states, `pnpm shots` captures them, and `scripts/lab/publish.sh` puts the build and the gallery under `/lab/`.
- [x] The direction's "Its risk" from the brief is answered in the Report after using it: whether it showed up, with a screenshot.

## Scope

`prototype/dashboard/` (this direction's folder and its registration) and `shots/prototype.json`. Not the shell, not another direction's folder, not `src/`.

## Context

- Use the `refactoring-ui` skill (`~/.agents/skills/refactoring-ui/`) and the vendored impeccable skill's Operate guidance. Sumeet's aim, 2026-11-17: the app, launcher and dashboard should "feel designed and polished", "make common things easy and uncommon things accessible" and be "a delight to use"; the Elite theme is kept "only in spirit".
- Concept code is rough on purpose. The look is not: this is what Sumeet chooses from, so finish the visual design; the brief's frames in `docs/design/lab/directions/` show organisation only, and their faces are placeholders.
- The other three directions are built at the same time in their own folders. Expect to merge `shots/prototype.json` with theirs.
- T3 approvals and questions stay on the phone; no concept puts them on the dashboard.

## Report

Queue is in the prototype's Version control as "Queue: one list of what wants you" (`#/queue/`). The code is in `prototype/dashboard/src/directions/queue/`: `tokens.css` and `styles.css`, the components (`ui.tsx`, `rows.tsx`, `timer.tsx`, `strip.tsx`, `omnibox.tsx`, `lib.ts`) and the screens (`screens/`). The rest of the change is one line in `directions/index.ts` and 14 shots in `shots/prototype.json`. It touches nothing in `src/data/`, `src/shell/` or another direction.

**The shell.** A slim top bar holds LoR, Today, Tasks, Goals, People, Notes and the one field. Every page is a single column at most 720px wide. Today is the ranked list. The other pages are for managing, and every row on them opens in place, so no editor ever drops below a list. At 412px the nav becomes a scrolling second row under the field, row actions stay visible, Done and Seen grow to 40px buttons on their own line, and popovers become bottom sheets. At 1100 it is the desktop layout with a narrower field.

**The look, against the brief.**
- Type is Saira at 13 (labels, meta), 15 (body, controls), 17 (row titles), 22 (page titles, the readout) and 40 (the lead title, 22 at phone width). No other font size appears in the CSS. Saira Condensed caps are used only for the readout and the lead card's label.
- Amber appears only on the lead card's edge and the running timer (the bar, the chip and its strip block). Cyan marks the agent: its flags, its suggestions on the strip, and its tasks in Tasks. Everything else is three greys, plus one red for errors and LoR not answering.
- Depth has three levels. Rows lie flat on the ground, the lead card is raised with a two-part shadow, and popovers, sheets, the toast and the field's choices float with blur. The field's choices are nearly opaque because they sit inside the blurred top bar, where a second blur does not reach the page.
- Corners are 8 to 18px. Motion runs 150 to 220ms and only says what changed: a row opening, a popover appearing, and the signature move.
- The signature move: the lead card wears the reticle, and clearing it slides the next item up into the card while the reticle relocks. The reticle's ring turns slowly while a timer runs on the lead. Checked at both widths in the morning: Done on the lead brings "Organise Sam's birthday dinner" into the card (labelled "Flagged by the agent"), the card runs its `q-rise` animation, and Undo puts the one thing back.
- Every flag row has a visible ⋯ that opens it in place (its due date, notes and the rest), so nothing hides behind a long press.

**Ranking.** First the one thing, then LoR's flags (oldest first), then his tasks due today or earlier, by due time. T3 cards are not on the dashboard. The snapshot does not record the agent's one thing, so `lib.ts` names it by title ("Draft the offline maps spec", which the day's brief leads with). It is absent in the quiet scene, since "nothing wants you" includes it. "Not today" sets it aside, and Undo brings it back. The one thing is for 14:00, the design review the brief names. Once the clock passes that, it steps aside: it leaves the lead without being ticked or deleted, and stays on Tasks and in the timer bar if a timer runs on it. The morning shot (09:00) shows it leading. The afternoon shot (15:00) shows it stepped aside, with the oldest flag in the card.

### Where each required state appears

All of them appear on Today, each opened by a link:
- **Nothing wants you** (`#/queue/?scene=quiet`): the lead card becomes "All clear · Nothing wants you." with the next calendar event and one soft suggestion (the next task due). The reticle is grey.
- **An empty day at 09:00** (`?scene=empty`): the strip's header says "Nothing recorded yet. A timer, your phone and the calendar fill this in." Only the rest of the calendar is drawn, and the timer bar offers the last three labels and Start…
- **LoR's last data shown as stale** (`?state=stale`): a red-edged banner under the top bar says "LoR has not answered since 14:51. This is what it last said. Ticks, timers and labels wait until it is back." with Try again. Every write control, Not today included, is disabled with that reason as its tooltip, and the timer's dot and readout turn grey. Search still opens results; Add and Ask are greyed. The state is a Queue parameter (`state=stale`), kept on every Queue link.
- **A crowded queue** (`?state=crowded`): twelve waiting, the lead and eleven rows. The list shows seven, then "4 more waiting". The extra flags are made up in the view over open tasks, so nothing is written to the store. Seen on one hides it and offers Undo; Done ticks the real task.

The quiet and empty states are the shell's scenes; stale and crowded are Queue's own `state=` parameter. A failed phone sync and T3 disconnected do not reach the dashboard in Queue: T3 is not on it, and the brief places the sync failure on the phone.

### The ten interactions

I scripted all ten in Chrome at 1440 and at 412 (playwright-core, clicks and keys only). Each run passed 11 of 11 steps: the ten, plus stopping the timer from People. A second script covered the lead card at both widths: Done and Undo, Not today and Undo, Seen then Done when a flag leads, and Done then Undo on a made-up crowded flag. Neither script raised a page error.

1. **Add a task.** Press `q` anywhere (or type into the field). "Book the ferry to Sintra fri #Lisbon" shows Add first, with the title and chips "Fri 20 Nov" and "Lisbon trip in April". Enter adds it. The toast says "Added …" and offers Open and Undo.
2. **Tick and untick.** Tick the row's checkbox and the row leaves the queue; the toast offers Undo. Unticking also works from "Done today" under the list, and from Tasks → Done.
3. **Open a task and change its due date.** Click its title and the row opens in place with Title, Due (Today, Tomorrow, Next week, No date, or a date field), Counts toward, and Notes. Each change saves at once with Undo. A task moved to tomorrow leaves the queue.
4. **Mark a flagged item done.** Use Done on the flag's row, or on the lead card when a flag leads. Undo is offered. Seen (take the flag down) also has Undo.
5. **Start and stop a timer.** Stop is in Today's timer bar, and in the top-bar chip on every other page. To start, use Start… (a picker with recent labels, a search, and "Start without a label"), one of the bar's last three labels, or "Start timer" on the lead card or any open task. **No Undo**, by choice: time has already moved on.
6. **Relabel a block.** Click a block on the strip, or "12 to label". A popover (a sheet at phone width) shows the time, the source and the agent's guess (Yes to accept), with arrows that step through the day's blocks, so a 10-minute block is reachable at 412. Pick a recent label or search for one.
7. **Search and open a result.** Press `/` or ⌘K, or click the field. Plain words find tasks, goals, projects, habits, people and notes. Enter or a click opens the result in place on its page (`maya` opens People with Maya open). A question puts "Ask the agent" first, which sends and says the answer comes on the phone. **No Undo** for Ask.
8. **Open a person.** Click a name on People and the row opens to show the identifiers, their page, what waits on them, the latest messages and mail, the cadence, Priority, and "Ask the agent before a call".
9. **Switch a running timer.** Switch in the bar or the chip opens a picker ("Ends X at 47 min and starts the new one from now"). Picking a label ends the old block and starts the new one. **No Undo**.
10. **Undo a relabel.** The relabel toast offers Undo, and the count goes back from 11 to 12 to label.

Undo is offered only for changes LoR can reverse: tick and untick, Done, Seen, due date, title, notes, counts toward, add, delete, relabel, accepting the agent's guess, a habit check-in, the weekly aim, a person's cadence and priority, and Not today. It is never offered for starting, stopping or switching a timer, or for a message to the agent. The toast drops its Undo as soon as any other change lands, so Undo always takes back the change it names.

### Shots and the lab

`shots/prototype.json` has 14 Queue shots: the five screens, the four states, and four open layers (the relabel popover, the switch picker, search results, and add-as-you-type), each at the catalogue's three widths. I ran `pnpm shots --catalogue shots/prototype.json --base http://127.0.0.1:3861` in three `--only` batches: 42 images, no problems. `scripts/lab/publish.sh` then published the build (stamp `a25c77a`) and the gallery. `http://127.0.0.1:3847/lab/dashboard/#/queue/` loads under the lab's CSP with no errors and no request outside `/lab/`.

`publish.sh` rebuilds all of `/lab/` from the checkout it runs in, so the other three directions publishing from their own branches will each replace `/lab/dashboard/` with a build lacking the others. Before this publish `/lab/` held only main's `current` build and the shots, with no launcher, so nothing was lost here. The full set should be published once from `main` after all four merge.

### Its risk: "A single list hides structure"

It did show up. The afternoon screenshot at 412 (`/lab/shots/prototype/`, "queue: Today in the afternoon…", `queue-today-in-the-afternoon-a-timer-running-two-flags@412.png`) shows it.
- The default afternoon is already nine things: the lead (a flag, once the one thing has stepped aside at 14:00) plus eight rows (the other flag and seven tasks due today or before). At phone width that list fills about 1,400px, and "This week" (the goals) sits at the bottom of a page about 2,000px tall.
- The day's shape is a strip with no words on it, and you tap a block to learn what it was.
- Every look at Today is a look at what is owed. What has gone well appears only as a collapsed "Done today 4" and the one-line week at the foot.
- In the crowded state the fold ("4 more waiting") keeps the list readable, but it also hides those four.

Two things soften it. The lead card always leads, so the first screen is one thing and not ten. And the all-clear state reads as calm rather than empty. But the brief's words hold: the week and the goals are a page away, and the shape of the day needs a tap.

### Notes

- Checks: the prototype's typecheck passes (`tsc -p prototype/dashboard/tsconfig.json`), `vitest run` passes 206 of 206, and `pnpm proto:build` builds. Root `pnpm typecheck` fails with 65 errors, all in `src/routes` and `src/router.tsx` from the missing generated `routeTree.gen`. That is lor-iurl, and none of the errors is in the prototype.

- `scripts/shots.mjs` sometimes draws a page with its first 863px repeated down the image, even in a viewport capture. It does this only when it runs its six pages at once, 2 to 5 of every 36 images. A shot taken on its own was clean every time. Removing the top bar's blur or its sticky position did not change the rate, so it looks like Chrome reusing raster tiles under this session's 4 GB cap, not the page. I re-took the affected Queue shots one at a time until all 42 were clean, checked by a script that looks for the top bar repeating. This is worth a tooling item: lower `PARALLEL` in `scripts/shots.mjs`, or have it re-take a capture that repeats.
- The catalogue's widths are 1500, 1100 and 412 (set by lor-5uxk). The interactions were scripted at 1440.
- In this worktree, `pnpm <script>` first runs an install that fails because `node_modules` is a symlink to the main checkout. It warns and then runs the script anyway. This is worth a tooling item: set `verifyDepsBeforeRun: false`, or let worktrees skip it.
- Concept code is rough where it does not show. The one thing is chosen by title, and the crowded flags are invented in the view.

### Responses

- 1-hiyq.1: fixed in 66209e1
- 1-hiyq.2: fixed in 66209e1
- 1-hiyq.3: fixed in 66209e1
