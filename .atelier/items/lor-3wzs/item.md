---
schema: atelier.item
schema_version: 3
id: lor-3wzs
title: 'The Panes direction on the dashboard prototype: every required screen, state and interaction'
status: building
mission: lor-2803
labels:
- dashboard
- design
priority: medium
blocked_by:
- lor-5uxk
implementers:
- thread:delegated-task:command%3Amcp%3A275230a9-0f8b-446e-b3b1-4303d9895adf%3Adelegate-task%3A4bad2ea8-13a4-4e49-83dd-839a64f6850a
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.385344569Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.810760408Z
---

## Goal

Sumeet can use the Panes direction's dashboard in the prototype: pick it in the Version dropdown and do the brief's ten dashboard interactions by hand, at desktop and at phone width, on the made-up data.

## Done when

- [x] `prototype/dashboard/` has a `panes` direction, in a folder of its own with its own tokens, components and screens, built as `docs/design/lab/brief.md` describes it under "### 3. Panes" and with the brief's "Common ground for every concept" (the one search field that also adds and asks, Sumeet's four look asks, standard controls).
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

Panes is in the prototype's Version control as "Panes: nothing leaves the screen" (`#/panes/`). The code is in `prototype/dashboard/src/directions/panes/`: `tokens.css` and `styles.css`, the font files in `fonts/`, the shared pieces (`panes.tsx`, `ui.tsx`, `rows.tsx`, `details.tsx`, `timer.tsx`, `omnibox.tsx`, `lib.ts`) and the screens in `screens/`. Outside that folder the change is one line in `directions/index.ts` and 16 shots in `shots/prototype.json`. It touches nothing in `src/data/`, `src/shell/` or another direction.

**First, a problem with this machine.** `/tmp` is a 7.5 GB tmpfs with per-user quotas, and Claude Code's temp folder (`/tmp/claude-1000`) hit its 1.5 GB quota partway through. After that, every Bash call lost its output. Most of the space is other sessions' scratchpads: one LoR worktree's scratchpad alone holds 461 MB. I deleted nothing. I worked around it by writing output to `~/.cache/lor-3wzs/` and setting `TMPDIR` there. It will hit every session on this box until someone clears old scratchpads or sets `CLAUDE_CODE_TMPDIR`. That is worth a tooling item.

**The shell.** A quiet nav rail sits on the ground. It holds the wordmark, the one field (find, add, ask), the five pages with counts, and the running timer's chip at its foot. Every page is a list pane and a detail pane, each a frosted layer with its own scroll. The common action is on each row and opens nothing: tick, Done, Seen, the agent's "Yes" on a stretch of time, Switch and Stop. Clicking the rest of a row opens it in the detail pane, and the open row wears amber. Nothing opens by itself.
- **At 1280 and up**, the panes sit side by side. With nothing open, the detail side shows the page's overview, recessed with no glass and no shadow. On Today that is the brief in the morning, and the day so far plus the week in the afternoon.
- **From 720 to 1279**, the detail slides in over the list. The list stays in view behind it, stepped back and blurred.
- **Below 720**, the rail becomes a top bar with the field, the timer chip and the pages as tabs. The list is the page and the detail is a sheet. The sheet rests at half height, with the list above it still live, or goes to full height with a visible button (`sheet=full` in the link). Below 1280, Today and People put their overview at the foot of the list.
- **What is uncommon** sits in the detail pane under its common fields, behind a collapsed "More": rename, waiting on, Later, delete, split, a stretch's delete, the weekly aim, and a person's cadence and priority.

**The look, against the brief.**
- **Type.** One sans, Hanken Grotesk, at 12 (labels, meta), 14 (secondary, controls), 15 (body, rows), 18 (section heads, the readout, page titles) and 28 (the open item's title, 18 in the phone sheet). No other size is used. Saira Condensed caps are used only for pane titles: the page title and the open item's kicker.
- **The font file.** `node_modules` has only Saira, so I downloaded Hanken Grotesk's latin 400, 500 and 600 woff2 files and its licence (`OFL.txt`, the SIL Open Font License) from `@fontsource/hanken-grotesk@5.1.0` on jsDelivr. They are vendored in `fonts/`, about 40 KB, so no font request leaves `/lab/`. The impeccable hook flags the font as "outside DESIGN.md". I left that finding standing rather than adding an ignore to the shared `.impeccable/config.json`, which is outside this item's scope. The brief lets each direction choose its own type.
- **Colour.** Panes are layers of the ground's colour at rising lightness, over two faint glows so the glass has something to frost. Amber marks the selected row, the open item's top edge and the running timer. Cyan marks the agent: its flags, its guesses on time, and Ask. There are three warm greys, and one red for late due dates, errors and LoR not answering.
- **Depth**, in five levels: the ground (the rail lies on it), a pane (the list), a raised row (hover and selection), a sheet (the open item, lifted, with blur and a two-part shadow), and floating layers (the field's choices, pickers, the toast).
- **Corners** are 6 to 26px. Motion runs 150 to 220ms and only says what changed: a pane sliding in, the sheet rising, a pane stepping back.
- **The signature move.** A pane with another over it steps back (scaled to 97–98.5%) and blurs slightly, while the one in front stays sharp. You see it in three places: the list behind the detail from 720 to 1279, the list behind the full-height sheet, and a task opened from a goal or a person, which stacks a second pane over the first at every width. The stacked pane has a back pill naming what is behind it. See the shot "panes: a task stacked over its goal".

### Where each required state appears

All four are on Today, and each is opened by a link:
- **Nothing wants you** (`#/panes/?scene=quiet`): a calm card under the timer row says "Nothing wants you. No flags and nothing due today. Next on the calendar: 16:30 Dentist." Time to label and Later today follow it.
- **An empty day at 09:00** (`?scene=empty`): the Time group says "Nothing recorded yet. A timer, your phone and the calendar fill this in as the day goes." The overview reads "None yet" recorded. The timer row offers three recent labels and Start….
- **LoR's last data shown as stale** (`?state=stale`): a red-edged banner across the top of the work area, above both panes, says "LoR has not answered since 14:51. What you see is what it last said. Ticks, timers and labels wait until it is back; search still works." It has a Try again button.
  - Every write control is disabled, with that reason as its tooltip: checkboxes, Done, Seen, the timer's Switch and Stop in the row and the chip, label pickers, and the detail pane's fields.
  - The timer's dot and readout turn grey. Search still opens results, and Add and Ask are greyed.
  - The state is Panes' own `state=stale`, kept on every Panes link.
- **A crowded queue** (`?state=crowded`): Needs you holds twelve flags. The list pane scrolls on its own, and its group labels stay pinned at its top, so "Needs you 12" stays visible while you scroll through them. The nav count follows. The extra ten flags are made up in the view over real open tasks, and nothing is written to the store. Seen on a made-up flag hides it, with Undo, and Done ticks the real task.

The quiet and empty states are the shell's scenes. Stale and crowded are Panes' `state=` parameter. T3 cards, T3 disconnected and a failed phone sync do not reach the dashboard: T3 approvals stay on the phone, and the brief puts the sync failure on the phone.

### The ten interactions

I scripted them in Chrome with playwright-core at 1440 (mouse and keys) and at 412 (a touch context that taps, with no keyboard shortcuts), with the Version control visible. Each run passed all its steps with no page errors: 15 steps at 1440 and 17 at 412. Besides the ten, the script checks:
- Back closes a pane opened here, and Escape closes only the pane in front (1440 only).
- Stop works from the People chip, and Ask has no Undo.
- At 412 only: the sheet goes half, full, half, then closes, and a second row tapped above a half sheet replaces the first, with one Back closing it.
- A task stacked over its goal closes to leave the goal open.

1. **Add a task.** Press `q` anywhere, or use "+ Add" on Today and Tasks, which is the tap path. The field reads the text as you type: "Book the ferry to Sintra fri #Lisbon" shows Add first, with the chips "Fri 20 Nov" and "Lisbon trip in April". Enter or a tap adds it, and the toast offers Open and Undo.
2. **Tick and untick.** Tick the row's checkbox and it leaves Due, with Undo. Untick it from "Done today" (collapsed under the list), or from Tasks → Done.
3. **Open a task and change its due date.** Click the row and the task opens in the detail pane. Due is a segmented control (Today, Tomorrow, Next week, No date) plus a date field. Each change saves at once, with Undo. The pane finds its task in the store rather than the visible list, so moving a task out of Due leaves its pane open.
4. **Mark a flagged item done.** Use Done on the flag's row or in its pane. Seen takes the flag down without doing the task. Both offer Undo.
5. **Start and stop a timer.** Stop is on Today's timer row and on the chip on every page. Start from the idle row's recent labels or Start…, or from "Start a timer" on any open task or event. **No Undo**: time has moved on.
6. **Relabel a block.** "Time to label" on Today lists the stretches that need a label ("Show all" lists every stretch). Clicking one opens it in the detail pane with how long it was, where it came from, the agent's guess (Yes to accept, also on the row), and a label picker with recent chips and a search. Earlier and Later step through the day, so a 10-minute stretch is reachable at 412.
7. **Search and open a result.** Press `/` or ⌘K, or tap the field. Results open in their page's detail pane: `maya` opens People with Maya open. A question puts Ask first. **No Undo** for Ask.
8. **Open a person.** Click a name on People and they open beside the list: last in touch and how, cadence, birthday, how to reach them, what waits on them (each task stacks over the person), the latest messages and mail, and their page. "Ask the agent before a call" is the pane's action.
9. **Switch a running timer.** Use Switch on the timer row or the chip, which opens a picker that ends the old timer and starts the new one from now. The timer's own pane has the same picker inline. **No Undo**.
10. **Undo a relabel.** The relabel toast offers Undo, and the stretch reads "Not labelled" again.

Undo is offered only for changes LoR can reverse: tick and untick, Done, Seen, due date, counts toward, notes, rename, waiting on, Later, delete, add, relabel, accepting a guess, split, a habit check-in, the weekly aim, and a person's cadence and priority. It is never offered for starting, stopping or switching a timer, or for a message to the agent. The toast drops its Undo when any other change lands.

**Back and dismissal.** Opening a pane from nothing adds a history entry, so Back closes it. Opening another row while one is open replaces that entry, so Back never walks through every row you looked at. Close is Back when the pane was opened in this tab. On a cold link, Close replaces the entry, so Back leaves the prototype as it arrived. Escape walks a layer stack and closes only the one in front: the field's choices, then a picker, then a stacked pane, then the detail.

### Shots and the lab

`shots/prototype.json` has 16 Panes shots in one block after Queue's. They are Queue's 14 by the same names (the five screens, four states, relabel, switch, search, add), plus "a task stacked over its goal" and "the open task's sheet at full height". The open states are viewport captures, since the panes scroll on their own.

I ran `pnpm shots` (`node scripts/shots.mjs --catalogue shots/prototype.json --base http://127.0.0.1:3863 --only <one shot name>`), one name per run, so three pages at a time. That gave 48 images with no problems, into `~/.local/state/lor-shots/prototype/`. A check that looks for the top band repeating down the image found none, and I opened the ones that matter for the comparison.
- **The gallery index.** Each run rewrites that folder's `index.html` from this branch's catalogue, which lists `current`, Queue and Panes. Shots that another direction's branch put there will be missing from the index, not deleted, until someone runs `shots` from main once all four have merged.
- **The scratch publish.** Following the lead's note, `scripts/lab/publish.sh` ran only against a scratch `LOR_LAB_DIR` (`~/.cache/lor-3wzs/lab`). It published the build and the galleries. Served through `scripts/lab/serve.mjs`'s own `labStatic`, with its Content-Security-Policy, `/lab/dashboard/#/panes/` loads Hanken Grotesk 400/500/600 and Saira Condensed from the build, with no CSP violation, no error and no request outside `/lab/`. The real lab is for the lead to publish from main.

### Its risk: "Two panes are a lot of chrome on a small laptop, and nested scrolls and half-height sheets make Back and dismissal easy to get wrong"

The first half showed up. The second did not reach the screen, but only because I designed against it from the start, and it cost real work.

**Chrome.** I checked 1280×720 and 1366×768 as well as the catalogue's widths. Compare "panes: Tasks with a task open" at 1500 and 1100, and "panes: a task stacked over its goal" at 1100, in `/lab/shots/prototype/`.
- At 1280×720 the rail (216), the list (440) and the detail (576) all fit, but every list loses width. On Today a flag's two Done/Seen buttons squeeze its title and note into about 230px, so "Reply to the App Store review notes" wraps. In the overview, the stat cards and the week sit in a pane half empty below them.
- At 1100 Panes stops being side by side at all. The open item covers about 620 of the list's 848px, and the rest of the list is blurred behind it. To compare two rows you close the pane, which is the old "drops below the list" problem in a nicer form. The list does not move, though, and its scroll position survives.
- A stacked pane covers its parent almost completely: only a 36px sliver and a blur say the goal is behind it. The back pill is what tells you.

**Scrolls, sheets, Back.** Desktop has three scroll regions: the list, the detail, and the picker inside it. At 412 the page scrolls under a fixed half sheet. Two things had to be built in for that to work:
- The list needed 52vh of padding at its foot, so its last rows can rise above the sheet.
- Opening a row scrolls it clear of the sheet's top edge.

Back and Close followed the history rules above, and the scripted checks pass. But every rule there exists because the obvious version was wrong. Pushing an entry on every open made Back walk through rows. Closing with Back after a cold link left the prototype. An Escape handler per layer, firing in React's child-first order, closed the goal before the task stacked over it. So the risk is real. It is answered in this build, but it would need the same care everywhere the shape is reused, the phone included.

### Notes

- **Checks.** `tsc -p prototype/dashboard/tsconfig.json` passes, `vitest run` passes 206 of 206, and `vite build` builds.
- **Tooling outside the repository.** The interaction, quick-look, repeat-check and lab-check scripts are in `~/.cache/lor-3wzs/` (`interact.mjs`, `look.mjs`, `repeat.mjs`, `labcheck.mjs`). The dev server ran on :3863 as asked.
- **Rough where it does not show.** The crowded flags are invented in the view. People's "due a first hello" stands for "never in touch, with a cadence set".
