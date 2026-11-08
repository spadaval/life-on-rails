---
created_at: "2026-11-08T03:36:23.212386424+00:00"
id: "lor-pmzz"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Layout versions in LoR at schema version 2, with a history page"
updated_at: "2026-11-08T03:36:23.212386424+00:00"
---

## Goal

LoR keeps every version of the phone's `layout.json` again, now at schema version 2, and the dashboard shows them. Bring the server half of `agent-layout` (T3 Launcher's Increment 7) onto `main`: the `layout_version` and `device_apps` tables, `src/server/layout.ts`, `src/server/deviceLayout.ts`, the `/device/layout*` and `/device/apps` routes and `lor phone layout|apps`. Then add a page in the dashboard that lists a device's versions and saves a new one, so a version can come from the web as well as the phone and the agent.

## Done when

- [ ] A body of schema version 2 is accepted and a version 1 body still is (the phone reads both); any other `version`, a non-object or a body over 256 KB is refused, each by a `pnpm test:db` case.
- [ ] `tests/db/layout.test.ts` from `agent-layout` passes on `main`'s schema, with its eight cases (versions, the stale base, undo, revert, the rejection, the app list), and a case for a version written by the web (`author: 'web'`).
- [ ] `lor phone layout`, `set`, `history`, `revert` and `lor phone apps` work against the test server (`scripts/ui-server.sh`), shown by running each once; `skill/SKILL.md` teaches them.
- [ ] A dashboard page (its route chosen and named on this item) picks a device (the one that synced last by default, as the CLI's `--device` does, with a choice when there are more) and lists its versions newest first: number, author, note, time, and `rejected` with its message. A version opens read-only as formatted JSON. Revert makes a new version, as `lor phone layout revert` does. "Edit as JSON" opens the latest version in a CodeMirror editor; Save asks for a note and writes a version on that base, and a stale base says the layout moved on and reloads. Screenshots of each through `scripts/ui-check.mjs` (a new flow) on the test database.
- [ ] The spec (`docs/v2-product-engineering-spec.md`) says layout versions live in LoR, lists the new routes, and the dashboard page.
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.

## Scope

`src/server/layout.ts`, `src/server/deviceLayout.ts`, `src/server/api/v1.ts` or the device routes as `agent-layout` placed them, `src/db/schema.ts` and a new migration, `cli/lor.mjs`, `skill/SKILL.md`, a new dashboard route and its server functions, `scripts/ui-check.mjs`, tests, the spec. Not the phone (the next item), not a visual editor.

## Context

- Port by reading `agent-layout`'s diff (`git diff main...agent-layout -- src tests cli skill`), not by merging the branch: it is 371 commits behind `main` (2026-11-08) and its docs changes are stale. Its review history is in its `docs/missions/t3-launcher/plan.md`, Increment 7.
- The author list grows to `agent | phone | web`. What the phone shows for a web version is the next item's choice.
- An agent's or a hand edit isn't stopped by the layout lock (`layout-v2.md`, `locked`); neither is a web edit, since it is the owner's.
- The page's look follows `DESIGN.md`; the notes page's CodeMirror (`src/components/NoteEditor.tsx`) shows how CodeMirror is set up here.
