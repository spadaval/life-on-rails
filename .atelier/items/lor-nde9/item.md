---
schema: atelier.item
schema_version: 3
id: lor-nde9
title: The dashboard reloads a change in under two seconds on demo data, and one command shoots every screen
status: draft
mission: lor-2803
labels:
- design-lab
- tool
priority: medium
blocked_by:
- lor-zj7x
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.872493965Z
---

## Goal

Changing the dashboard's look is a save-and-see loop, and a full set of screenshots of every page and state is one fast command. These are tools for designing quickly, not checks: nothing here gates a merge.

## Done when

- [ ] `scripts/ui-dev.sh` starts Vite's dev server with hot reload on the test database at `127.0.0.1:3858`, with no jobs, channel or wall listener, and refuses the real database and lor-web's ports. A saved change to a route's markup or to `src/styles.css` shows in the open page in under 2 seconds, measured and written in the Report.
- [ ] `node scripts/shots.mjs [--only <name>] [--out <dir>]` captures every route and its named states at 1500, 1100 and 412 px wide against `:3858` or `:3857`: Today at four clock times and on a past day, a selected time block, Tasks with a task open and with rows picked, quick add open, Goals with an item selected and the editor open, People list and hub, Notes index, page, history and graph, Search with results and none, Status healthy and failing. Every shot is a link: a state that is not in the URL today (a selected block, an open editor, picked rows, quick add open, a version's history) gets a search parameter in its route, and the clock and data are named in the link too (`at=` as lor-zj7x defines it). The list of shots is a catalogue file of names and links, `shots/dashboard.json`, printed by `node scripts/shots.mjs --list`. The script takes any catalogue (`--catalogue shots/lab.json`, with a base address and a way to wait for a canvas to draw), so the lab and the prototype each add a catalogue file and never edit the script. The ten dashboard states lor-q165 names are in the catalogue under those names, and opening any link by hand draws the same state the script captured. Text typed into quick add (`add=`), a scroll position and which rows are picked are among the things a link can carry.
- [ ] A full run takes under 60 seconds on this machine, measured in the Report. It writes PNGs and `index.html`, a gallery grouped by screen with the three widths side by side, and keeps the previous run so each shot can be flipped between before and after.
- [ ] The gallery opens over `/lab/shots/` once lor-zwfd lands, and from the file until then.
- [ ] The verify skill's `SKILL.md` names both commands under a 'Designing' heading and says they are not evidence for a review.

## Scope

`scripts/ui-dev.sh`, `scripts/shots.mjs`, `package.json` scripts, the verify skill's text, and the search parameters the routes under `src/routes/` need for a state to be linkable (no visible change without the parameter). `scripts/ui-check.mjs` and its flows are left alone (lor-mxrc owns them).

## Context

- `pnpm dev` binds `127.0.0.1:3847`, the running service's port, so it cannot be used beside lor-web today.
- Playwright is already here as `playwright-core` with the system Chrome.
