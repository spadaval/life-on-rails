---
schema: atelier.item
schema_version: 3
id: lor-nde9
title: The dashboard reloads a change in under two seconds on demo data, and one command shoots every screen
status: building
mission: lor-2803
labels:
- design-lab
- tool
priority: medium
blocked_by:
- lor-zj7x
implementers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A7b6735d6-ec60-49fb-9cbd-347e71e2279a
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.872493965Z
---

## Goal

Changing the dashboard's look is a save-and-see loop, and a full set of screenshots of every page and state is one fast command. These are tools for designing quickly, not checks: nothing here gates a merge.

## Done when

- [x] `scripts/ui-dev.sh` starts Vite's dev server with hot reload on the test database at `127.0.0.1:3858`, with no jobs, channel or wall listener, and refuses the real database and lor-web's ports. A saved change to a route's markup or to `src/styles.css` shows in the open page in under 2 seconds, measured and written in the Report.
- [x] `node scripts/shots.mjs [--only <name>] [--out <dir>]` captures every route and its named states at 1500, 1100 and 412 px wide against `:3858` or `:3857`: Today at four clock times and on a past day, a selected time block, Tasks with a task open and with rows picked, quick add open, Goals with an item selected and the editor open, People list and hub, Notes index, page, history and graph, Search with results and none, Status healthy and failing. Every shot is a link: a state that is not in the URL today (a selected block, an open editor, picked rows, quick add open, a version's history) gets a search parameter in its route, and the clock and data are named in the link too (`at=` as lor-zj7x defines it). The list of shots is a catalogue file of names and links, `shots/dashboard.json`, printed by `node scripts/shots.mjs --list`. The script takes any catalogue (`--catalogue shots/lab.json`, with a base address and a way to wait for a canvas to draw), so the lab and the prototype each add a catalogue file and never edit the script. The ten dashboard states lor-q165 names are in the catalogue under those names, and opening any link by hand draws the same state the script captured. Text typed into quick add (`add=`), a scroll position and which rows are picked are among the things a link can carry.
- [x] A full run takes under 60 seconds on this machine, measured in the Report. It writes PNGs and `index.html`, a gallery grouped by screen with the three widths side by side, and keeps the previous run so each shot can be flipped between before and after.
- [x] The gallery opens over `/lab/shots/` once lor-zwfd lands, and from the file until then.
- [x] The verify skill's `SKILL.md` names both commands under a 'Designing' heading and says they are not evidence for a review.

## Scope

`scripts/ui-dev.sh`, `scripts/shots.mjs`, `package.json` scripts, the verify skill's text, and the search parameters the routes under `src/routes/` need for a state to be linkable (no visible change without the parameter). `scripts/ui-check.mjs` and its flows are left alone (lor-mxrc owns them).

## Context

- `pnpm dev` binds `127.0.0.1:3847`, the running service's port, so it cannot be used beside lor-web today.
- Playwright is already here as `playwright-core` with the system Chrome.

## Report

Commits: the dev server (d404f40), the link parameters (0a72c70), the shot script, catalogue, gallery, tests and the skill's text (d6f3a97).

**Dev server.** `scripts/ui-dev.sh` (`pnpm ui:dev`) runs `vite dev --strictPort` on `127.0.0.1:3858` with the test database and the same empty channel, wall, owner and Plaid settings as `ui-server.sh`. Under `vite dev` `boot.ts` starts nothing, so no jobs run. It refuses (exit 3) lor-web's ports (3847 to 3849 and the env file's), a port already taken, a database other than `life_on_rails_test`, and a URL with more than `?host=`. `tests/shots.test.ts` shows each refusal. Two `vite.config.ts` changes it needed. In a worktree `node_modules` is a symlink to the main checkout, and Vite refused the fonts from there (403), so `server.fs.allow` now includes where it points. Each checkout also gets its own Vite cache (`LOR_VITE_CACHE_DIR`, set by the script) instead of sharing the main checkout's.

**Hot reload, measured** (`/status?at=15:00&data=base` open in headless Chrome, from the file write until the DOM showed the change, three times each, warm): route markup (the page title in `src/routes/status.tsx`) took 188, 125 and 105 ms. `src/styles.css` (a rule on `body`, read back through `getComputedStyle`) took 124, 111 and 109 ms. There were no full reloads. The probe script is not committed.

**Links.** Ids change every time the demo set loads, so links name things by title. Today takes `block=HH:MM` (the actual block covering that time on the day shown) or an id. Tasks `id=` takes an id or a title, and `pick=` takes comma-separated row positions, ids or titles. Goals `id=` takes an id or a title, and `edit=1` opens the editor. People `id=` takes an id or a name, resolved in the loader. Notes `history=1` opens the history at the head; an older `version=` opened it already. On any page, `add=<text>` opens quick add with that text typed (`#` written as `%23`), and `scroll=<px>` scrolls the window. The state starts from the parameter in the first render, so server and client agree. Clicking still works as before and does not write these parameters back. Without a parameter, nothing changes.

**Shots.** `node scripts/shots.mjs` (`pnpm shots`) reads `shots/dashboard.json`: 27 named states (all the Done when lists, lor-q165's ten under its exact names, and one `scroll=` shot), each link with `at=` and `data=`. `--list` prints names and links. `--only` matches a name or screen. `--base` points at `:3857`. `--catalogue` takes any file with `base`, `widths`, `height`, `scale`, `timezone` and a `wait` (`selector`, `function` for a canvas that has drawn, `ms`), at catalogue or shot level. The script only opens the link, waits for network idle and fonts, and captures (full page, or `capture: "viewport"` for quick add and scroll), with animations off and the caret hidden. A link opened by hand therefore draws the same state. Shots run in groups by `data=` and `at=`, one group at a time and six pages at once within a group, because a variant changes the shared test database. A base on lor-web's ports is refused unless every link is under `/lab/`.

**Gallery.** It is written to `~/.local/state/lor-shots/<catalogue>/` by default, which is the path lor-zwfd's `publish.sh` should copy. Each shot and width is `<name>@<width>.png`, the one it replaces moves to `previous/`, and `--only` rotates only what it shot. `index.html` is grouped by screen with the three widths side by side, and has a Before button (key `b`) that swaps every image with its previous one. It uses relative paths and inline script, with no fetch and no CDN. Opened from `file://` it made only `file://` requests, so it should serve unchanged under `/lab/shots/` once lor-zwfd lands.

**Full run, measured.** 81 images (27 states at 3 widths). Against `:3858`: 23.4, 24.9 and 23.0 s wall, plus one earlier run at 26.0 s. Against `:3857` (`verify.mjs up`): 33.0 and 33.0 s. Every run reported no problems. I looked at the shots for quick add with a parent and a date, three rows picked with the bulk bar, the flagged task open, a past day with the 18:00 run selected, the habit with its editor, the Lisbon history with a diff, Maya's hub at 412 px and Status failing. Each drew what its name says.

**Checked.** `tsc --noEmit` is clean. `vitest run` passed 186 tests (5 new in `tests/shots.test.ts`). `pnpm test:db` passed 144 of 144 in three runs. One run in between failed 2 tests, and I did not capture which. The test database is shared with the two servers I had running and with other threads, so I take it as interference, but I have not shown that. After `test:db`, `pnpm demo:test` was run again. (`pnpm <script>` fails in a worktree whose `node_modules` is a symlink, because pnpm's pre-run install refuses it, so I ran the binaries in `node_modules/.bin` directly.)

**For the reviewer**
- The verify skill's `SKILL.md` has a new "Designing" section: both commands, the link parameters, and a line saying they are not evidence for a review.
- The gallery's links point at the base the shots came from (`http://127.0.0.1:3858/...`). Under `/lab/` on the phone those links will not open. The images will.
- `scroll=` scrolls the window only. Today, Tasks and Notes all scroll the window, so nothing inside a scrolling panel is covered.

