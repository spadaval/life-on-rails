---
schema: atelier.item
schema_version: 3
id: lor-5uxk
title: 'The dashboard prototype: Today, Tasks, Goals and People per direction, switched by a dropdown'
status: in_review
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-npa6
- lor-zwfd
implementers:
- thread:delegated-task:command%3Amcp%3Ad505dd17-c64d-476e-8d30-f41010a20b4c%3Adelegate-task%3A35c79320-8bf5-4eed-ba9f-0119df7c21f5
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.112471157Z
---

## Goal

Sumeet can use each direction's dashboard: click through it, tick things, open a task, at his desk and on his phone, and flip between directions on the same made-up data.

## Done when

- [x] `prototype/dashboard/` is a small Vite and React app with no server: it reads a frozen snapshot of the demo set and keeps changes in memory, so every control works and nothing is saved. A Version dropdown (and the URL hash) switches direction; `current` is the shipped dashboard's look on the same screens.
- [x] A direction is one folder with its own tokens, components and screens; nothing is shared between directions but the data and the shell, so each is free to differ in structure.
- [x] The shell and `current` are built here. Each direction from the brief is then one item, added by the lead, whose Done when is the brief's required dashboard set and dashboard interactions at 1500 and 412 px, with `shots/prototype.json`, a catalogue for lor-nde9's `scripts/shots.mjs` (which this item does not edit), capturing them.
- [x] `pnpm proto:dev` reloads a change in under 2 seconds; `scripts/lab/publish.sh` puts the build under `/lab/dashboard/`.

## Scope

`prototype/dashboard/`, `shots/prototype.json`, and the `proto:dev` and `proto:build` scripts in the root `package.json`. The real dashboard under `src/` is not changed.

## Context

- Prototype code is rough on purpose: no tests, no accessibility pass beyond keyboard focus, no data layer. The chosen direction is rebuilt in `src/` by a later mission; the prototype is its reference.

## Report

The prototype runs: `pnpm proto:dev` serves it at `http://127.0.0.1:3859`, and `pnpm proto:build` writes `prototype/dashboard/dist/`, which `scripts/lab/publish.sh` already copies to `/lab/dashboard/` (lor-zwfd's line). `current` draws Today, Tasks, Goals and People as the shipped dashboard does. Every control it draws works in memory. The prototype makes no call to LoR: it reads a JSON file bundled into it, and no request left `/lab/dashboard/` in the checks below.

**What it is** (`prototype/dashboard/README.md` says how to add a direction)
- `src/data/` is shared by every direction.
  - `snapshot.json` is the demo set as LoR's own read functions return it. `snapshot.ts` remakes it: it refuses any database but `life_on_rails_test`, empties it, loads the demo set dated 2026-11-17, and reads it at 15:00 with the `running` variant and at 09:00 on the base. It ends on the base. It takes 1.8 s and writes 178 KB, all made-up names at example domains.
  - `store.ts` is one in-memory store with every dashboard action from the brief: add, tick and untick, due date, Seen, Done on a flag, start, stop, pause and switch the timer, relabel, retime, split and delete a block, habits, people. Each action keeps the state before it, so `undo()` takes back any change. A direction chooses where to offer Undo.
  - `derive.ts` builds the lists Today needs (as `getTodayFn` does) and a plain-words search over tasks, goals, people and pages.
  - Scenes: `afternoon` (the default: 15:00, a timer running, two flags; the demo set has three, so the scene takes Sam's down), `morning` (09:00 with the brief), `quiet` (no flags, no timer, nothing due) and `empty` (09:00, nothing recorded). A scene drops blocks that start after its clock, because the demo set writes the whole day's phone and place time up front.
- `src/shell/` is shared.
  - The hash is the route: `#/<version>/<path>?<params>`. `scene=`, `still=1` (the clock stops) and `shell=0` (the Version control hides) stay on every link. An unknown version opens `current`.
  - The Version control is a small pill in the bottom-right corner that opens to Version and Scene dropdowns and a Start over link.
  - It links only the showing direction's stylesheets. Each direction exports `{ css: [url], App }` with `import css from "./styles.css?url"`.
- `src/directions/current/`: `src/routes/{index,tasks,goals,people,search,__root}.tsx` and the components they use, copied at 48477ea.
  - They run nearly unchanged. A small router shim stands in for TanStack Router over the hash, and `server/functions/*.ts` stand in for the server functions, under the same names and inputs, over the store.
  - Its Tailwind scans only its own folder (`@import "tailwindcss" source(none)`).
  - Notes is a read-only list and page, so links from People and search land somewhere.
- `src/directions/template/` is a one-page example of the whole contract, light-themed and shown only under `proto:dev`. A direction item copies it.
- `shots/prototype.json` holds `current`'s 11 states at 1500, 1100 and 412 px. A direction item adds its own block.

**Left out of `current`, rather than drawn dead**
- Earlier and Later on Today: the snapshot holds one day.
- The Status link, Gmail and calendar links, and every link in a page that leaves the prototype. Markdown keeps only `#` links.
- Notes' editor, history and graph.
- "Remember this label", which a relabel accepts but which has no later sync to act on.
- "Explain score" shows only the score, confidence and hours, because the snapshot keeps no score parts.
- Goals' hours change when a timer stops or a block is relabelled, but not as the real scorer would recompute them.
- In the morning scene, "Phone today" still counts the whole day's phone time.

**Shown**
- **Interactions.** Driven in Chrome against the dev server (scratchpad script `act.mjs`), all pass:
  - add a task with `q`;
  - tick and untick;
  - open a task and set its due date to Next week;
  - Done on a flag;
  - stop and start the timer;
  - relabel the Instagram block, and undo it through the store;
  - search "lisbon" and open a result;
  - open Maya's hub;
  - pick a version from the dropdown;
  - an unknown version falling back.

  The same script against the built bundle, served through the real `labStatic` handler with its CSP, passes all eight interactions it ran there. Undo was not tried there because the build has no `/src/` to call the store from. The two version checks fail by design there, because the template exists only in dev. Against the built bundle, both Saira fonts loaded, there was no CSP violation and no console error, and no request went outside `/lab/dashboard/`. The stand-in app behind the handler counted no hits.
- **Style isolation.** Switching `current`, then `template`, then `current` without a reload gives a `body` background of `rgb(12,13,18)`, then `rgb(251,250,247)`, then `rgb(12,13,18)`. Only the showing direction's `<link>` stays, and the added task survives the switch.
- **Hot reload**, timed from the file write until the DOM showed the change, three times each, warm, with no full reloads: a TSX edit (`routes/goals.tsx`) took 64, 66 and 63 ms, then 67, 64 and 62 ms. A CSS edit (`styles.css`) took 40, 47 and 35 ms, then 46, 36 and 47 ms.
  - The first run found a bug: a CSS edit re-ran the shell, which linked the stale sheet after Vite's fresh one, so the second edit never showed. The shell now matches sheets by path and ignores Vite's `?t=`, and the timings above are after that fix.
- **Screenshots.** `node scripts/shots.mjs --catalogue shots/prototype.json` made 33 images in 9.9 s with no problems. I looked at Today in the afternoon, Today in the morning at 1100, a selected block, Tasks with a task open, Goals, People, Maya's hub and Today at 412. Each matches the shipped screens, including their faults at phone width.
- **Publish.** `LOR_LAB_DIR=<scratch> scripts/lab/publish.sh` copied `dist/`. The index reads "Dashboard prototype, made … from 0e3ed8b", from the `.lor-stamp` the build writes.
- **Checks.**
  - `tsc -p prototype/dashboard` is clean.
  - Root `pnpm typecheck` is clean after copying the gitignored `src/routeTree.gen.ts` from the main checkout (lor-iurl).
  - `pnpm test` passes 206 of 206.
  - The built JS has no `postgres`, `_serverFn`, `/api/v1` or Gmail strings. Types from `src/server` are imported as types only.

**Outside Scope, on purpose**
- `src/styles.css` gained `@source not "../prototype";` in its own commit, 4ebc950. Without it, the app's Tailwind auto-scan reads the prototype's class names into the shipped CSS. This makes no visible change.
- `snapshot.ts` reloads the shared test database, as `pnpm demo:test` does.

**For the lead**
- Direction items need only `src/data` and `src/shell`. If one needs a new action or scene, it belongs in `src/data`, which every direction shares.
- `pnpm proto:dev` works in this worktree. pnpm warns that its pre-run install failed because of the `node_modules` symlink, then runs the script anyway.


**Round 1** (7379aef). All four findings held, and each was reproduced before fixing. The checks are the reviewer's own scripts from `/tmp/lor-review-5uxk.MVlsKq/`, rerun against a fresh build, published to a scratch lab and served through `labStatic` with its CSP.
- **Ids on plain http.** Ids now come from `crypto.getRandomValues`, which is available on a plain-http origin. On `http://review.invalid:3861` (`isSecureContext` false, `crypto.randomUUID` undefined), adding a task shows "Added …" and Start timer starts it. There were no page errors and no requests outside `/lab/dashboard/`.
- **Stale version loads.** The shell records which version was last picked. A version whose import or stylesheets finish later links nothing and removes nothing. With current's import held while switching to Template and then released, the page stays Template: background `rgb(251, 250, 247)`, `data-version` template, and only template's sheet linked.
- **People's lists.** They are now worked out from the people as edited (`peopleIn` in `src/data/derive.ts`, mirroring `listPeople`'s filters and order). The snapshot's frozen membership is no longer read. A cadence on someone never in touch counts as overdue by the cadence, as the real reader has it.
  - Grandpa Joe set to No cadence leaves Due a hello at once and is in neither list after switching away and back.
  - Ben Okoro (never in touch) set to Every week appears in Due a hello as "7 days past the cadence".
- **Remember checkbox.** `current` no longer draws "Label … this way from now on", and a relabel sends no remember flags.
- **My own checks.** My interaction script passes all 11 checks in dev, and the 9 that apply to the built copy pass behind the lab handler. Its undo step had used a bare `import("/src/data/store.ts")`. After a hot update Vite serves the store as `store.ts?t=…`, so that import made a second, empty store. The script now imports the URL the page actually loaded. This was a fault in the test harness, not the app.

### Responses

- 1-0t9a.1: fixed in 7379aef
- 1-0t9a.2: fixed in 7379aef
- 1-0t9a.3: fixed in 7379aef
- 1-0t9a.4: fixed in 7379aef
