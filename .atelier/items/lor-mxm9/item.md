---
schema: atelier.item
schema_version: 3
id: lor-mxm9
title: 'One look for every page: panels by tone, three button levels, four type sizes, styled form controls'
status: approved
mission: lor-40i0
labels: []
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3Aa51dd0ff-c339-42c3-a502-f77c8982c242%3Adelegate-task%3Ad723c0aa-1bab-4ac7-8d2a-d93839f3f259
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:45.914953157Z
---

## Goal

Every dashboard page uses one small set of looks: panels separated by tone with the bronze frame kept for what should stand out, three levels of button, four text sizes, and form controls drawn in the theme. This is the base the page items build on.

## Done when

- [x] `src/styles.css` names a tone for a plain panel and keeps the framed, chamfered panel as a separate class. `Panel` in `src/components/ui.tsx` draws the plain one unless asked for the frame.
- [x] On each of Today, Tasks, Goals and a person's page at 1500 by 950, at most two panels on screen carry the bronze frame, and they are the ones that are open or live (the running timer, an open editor, a selected block). The Report lists which panels keep it and why.
- [x] Buttons are one of three named levels in `src/styles.css`: primary (filled, at most one per panel or toolbar), secondary (outlined) and quiet (text only). Every `<button>` and button-styled link in `src/routes` and `src/components` uses one of them. Done and Did it are the primary of their row; Seen, Close, Pause and Cancel are quiet or secondary. Delete keeps its coral as a named danger variant of secondary (`.btn-danger`, `DESIGN.md`), which is not a fourth level.
- [x] Body text in `src/routes` and `src/components` uses four sizes, each with one job written in `DESIGN.md` (for example: small print, body, emphasis, page title). `grep -o 'text-\[[0-9.]*px\]'` over both directories shows only those four and the arc's display sizes; the Report gives the before and after counts.
- [x] `<select>`, `<input type="date">`, `<input type="time">`, checkboxes and textareas are drawn in the theme on every page that has one, and no control's text is clipped at 1500px (today the habit editor's "per day" select shows "per d").
- [x] Corners take a small radius where Sumeet asked for rounded corners (his look asks of 2026-11-17, `docs/design/lab/brief.md`), and the chamfer stays on the framed panel. `DESIGN.md` says which shape is used where.
- [x] `DESIGN.md` describes the panel tones, the frame's job, the button levels and the type scale as built.
- [x] `node scripts/shots.mjs` runs over `shots/dashboard.json` at 1500px and the Report says the gallery's path. No page is worse at 412px than at the base commit: the Report compares Today and Tasks at 412px before and after.
- [x] `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/styles.css`, `src/components/ui.tsx`, class names across `src/routes` and `src/components`, and `DESIGN.md`. No change to what any page shows, its order, or its copy: those belong to the page items. No new palette and no new typeface.

## Context

- Today's counts in `src/routes` and `src/components`: `text-[13px]` 47, `text-[12.5px]` 42, `text-[14px]` 33, `text-[13.5px]` 14.
- The look's rules are in `DESIGN.md`; the vendored `refactoring-ui` skill's hierarchy, type and depth areas apply.
- Kept on purpose: the arc, the amber, condensed caps for page titles and section heads, the reticle.

## Report

**One conflict in the item, resolved this way.** The item asks for primary to be "filled, at most one per panel or toolbar" and also for Done and Did it to be "the primary of their row". A panel holds several rows (Needs you has three Done buttons, a goal card up to three Did it), so both cannot hold as written. I kept both by giving primary two sizes: full size is the amber fill, at most one per panel or toolbar (Start timer, Save, Create, Done in the bulk bar); the small row size (`.btn-primary.btn-sm`) is an amber outline with amber text, the look the old "amber outline variant" had. So Done, Did it, "Yes, <label>" and Read are `btn-primary` and still the strongest thing in their row, without pouring solid amber down a panel. DESIGN.md's "Two Solid Fills Rule" became "The Solid Fills Rule" to match. If a reviewer prefers Done as a filled button per row, it is one CSS rule to drop.

**What changed.**
- `src/styles.css`: `.panel` is now the plain tone (the `panel` face, 4px radius, no border); `.panel-framed` is the bronze 6px-chamfer frame. `Panel` in `src/components/ui.tsx` draws the plain one unless given `framed`. Buttons are `.btn` plus `.btn-primary`, `.btn-secondary` or `.btn-quiet`, with `.btn-danger` as a coral secondary and `.btn-sm` as the row size. `.seg` is the segmented control (People's filters, Pages/Graph, a task's Mine/The agent's). Type sizes are theme tokens: `text-small` 13px, `text-body` 15px, `text-lead` 18px, `text-title` 22px. Selects get a themed arrow, checkboxes are drawn (well square, amber when checked, dark tick), and inputs, buttons and the Tasks filter dropdowns are all 34px tall.
- `DESIGN.md`: panel tones and the frame's job (Cards / Containers, Elevation), the three levels (Buttons), the type scale (Typography, with the frontmatter tokens), shapes (Shapes: 4px radius, 2px on checkboxes, chamfer only on framed panels and floating menus, data marks square), inputs and segmented controls.
- `src/components/NoteEditor.tsx`: the editor's sizes follow the rendered page's (15px body, 22 / 18 / 15px heads), since `.prose-lor` moved to the scale too.
- New `tests/look.test.ts`: fails on any `.btn` class string in `src/routes` or `src/components` without one of the three levels, and on any literal `text-[Npx]` or Tailwind `text-xs/sm/...` outside the arc's display sizes. It runs in `pnpm test`, so the scale does not drift back.
- Clipping fixed: the habit editor's "per day" select (number field to 64px, select takes the rest), the block editor's time fields ("10:15 AM" was cut to "10:15 AI" at base; now 136px), and Goals this week's hours column ("18.7 / 15h" wrapped onto two lines; now one).

**Frames at 1500 by 950** (gallery below). At most one framed panel on each page; a second appears only when the quick-add dialog opens over an open editor.
- Today: the selected time block's panel (an open editor). Nothing is framed when no block is selected. The running timer is a pill inside the arc, not a panel, so it keeps its amber dot and readout and takes no frame.
- Tasks: the open task's panel (an open editor). "Pick a task" is plain.
- Goals: the selected goal, project or habit, with its editor. Goal cards and "Nothing selected" are plain.
- A person's page: none. Nothing on it is open or live.
- Also: Notes frames the page open for editing (not an old version) and, on the graph, the selected page; the quick-add dialog is framed. The star map's panel is plain.

**Buttons.** Every `.btn` names a level: 12 primary, 43 secondary, 17 quiet, 4 danger (each also secondary). Done (Needs you, the bulk bar) and Did it (goal rows, a habit's panel) are primary; Seen, Close, Pause, Cancel, Clear, dismiss and "take back" are quiet; Stop, Send, Seen in a task's flag box and the rest are secondary. A habit's Did it in its panel drops to secondary while the editor is open, so Save is the one primary there. Former bare text buttons ("Too much? Reset visibility", "Move everything past due to Later", "dismiss", "remove", the chip's ×) are now quiet. `<button>` elements that are content, not buttons, keep no button style: a task row's title and its pick edge, a lane block, the timer's label, a goal's name and the rows under it; Tasks' view tabs are navigation (underline tabs, DESIGN.md Navigation). `src/routes/plaid/$.ts` is a standalone server-rendered Plaid page with its own inline styles and was left alone.

**Type sizes, before and after** (`grep -rho 'text-\[[0-9.]*px\]' src/routes src/components | sort | uniq -c`):
- Before: 13px 47, 12.5px 42, 14px 33, 13.5px 14, 15px 4, 11.5px 3, 12px 1, 14.5px 1, 15.5px 1, 18px 1, 19px 1, 22px 1, plus `text-xs` 2 and `text-sm` 1, and the arc's 56px 2, 62px 1, 20px 1, 21px 1.
- After: only the arc's 56px 2, 62px 1, 20px 1, 21px 1. Everything else uses the tokens: `text-small` 112, `text-body` 36, `text-lead` 3, `text-title` 2. Rule of the mapping: 11.5 to 13.5px became small; 14px became body where it is read (notes, descriptions, tables, definition lists, the arc's subline) and small where it is a figure in a narrow column (Goals this week's hours); 14.5 to 15.5 became body; 18 and 19 became lead; 22 title. Lane labels went from 11.5 to 13px, with the fit estimate raised from 7 to 7.5px a character.

**Screenshots.** `node scripts/shots.mjs --out ~/.local/state/lor-shots/lor-mxm9` over `shots/dashboard.json` at 1500, 1100 and 412px, after shooting the base commit 93d569a3 into the same folder first. The gallery at `/home/supadava/.local/state/lor-shots/lor-mxm9/index.html` flips every shot between base (`previous/`) and this change; a separate copy of the base shots is in `~/.local/state/lor-shots/lor-mxm9-base/`. Every image is exactly its width wide (checked with `file`), so no page scrolls sideways at any width.

**412px, Today and Tasks, before and after.**
- Today: the first run at 412px came out 433px wide, because the free-time chips at 13px no longer fit on one line. The chips now wrap inside the column, and the page is 412px wide again. Everything else reads as at base; the page head's buttons fit on one row where base needed two.
- Tasks: the 13px parent chips took a few characters from task titles at first ("Fin..." where base had "Find ..."). The chip is now capped at 120px below the sm breakpoint (190px above, as before), so titles get more room than at base ("Find a physio ...", "Fix the dripping ..."), and the chip ends in an ellipsis instead of being cut mid-letter. The page head fits on one row where base needed two.

**Checks.** `tsc --noEmit` passes; `vitest run`: 24 files, 208 tests pass (including the new `tests/look.test.ts`).

**Problems met, for others.**
- `pnpm` does not start in this worktree: corepack wants pnpm 12.11.2, which is not in its cache (`Cannot find module .../corepack/v1/pnpm/12.11.2/bin/pnpm.cjs`). I ran the scripts directly (`node .claude/skills/verify-lor/verify.mjs demo`, `node_modules/.bin/tsc`, `node_modules/.bin/vitest`). Worth an item if it is not already known.
- The test database is shared: while I worked, other threads reloaded or emptied `life_on_rails_test` three times, so a shot run could show another thread's data or none. I reloaded the demo set right before each run I kept and checked the images.
- shots.mjs produced a repeated-band image twice for Tasks at 412px; that is lor-jkcf. I retook those shots until they were clean.

