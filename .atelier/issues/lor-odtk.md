---
created_at: "2026-11-07T22:17:28.574588572+00:00"
id: "lor-odtk"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-odtk
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The server's reads for LoR's panels: the agent's queue, tasks, people"
updated_at: "2026-11-08T00:00:41.693760516+00:00"
---

## Goal

LoR's server gives the phone's `lor` panel and `t3-queue` block what `/device/home` lacks, read-only: the agent's work queue, the open tasks, and the people to be in touch with. Each reply carries what the web's Today, Tasks and People show for the same rows, so the phone and the web agree.

## Done when

- [x] `GET /device/home` gains `agent_queue`: `{ id, title, queued_at: string | null, thread: string | null }[]`, the open tasks (`status` active or waiting) with `assignee = 'agent'`; those with `queued_at` first, oldest first, then the rest by `updated_at`, newest first; at most 10. `thread` is `thread_ref` (the T3 thread working it). A `pnpm test:db` test in `tests/db/device.test.ts`: a queued task, a claimed one (`claimTask`: `thread` set, `queued_at` null), an owner's task and a done agent task (both left out), in that order, and the reply JSON-safe.
- [x] `GET /device/tasks` returns `{ now, timezone, today: "YYYY-MM-DD", open: number, tasks: { id, title, due: "YYYY-MM-DD" | null, overdue: boolean, waiting_on: string | null, parent: string | null, flagged: boolean, agent: boolean, google: boolean }[] }`: the open tasks the web's Today lists in Due soon, in its order (due date, then title), at most 100, with `open` the full count. The selection, order and fields come from one function that `getTodayFn`'s `tasks` also uses (its open part), so the two can't drift. `pnpm test:db` tests: order by due then title; a task with no due date last; a waiting task names who it waits on; a task under a project names the project; done, later and archived tasks left out; the cap of 100 with `open` still counting all; JSON-safe.
- [x] `GET /device/people` returns `{ today: "YYYY-MM-DD", overdue: Person[], birthdays: Person[] }`, `Person = { id, name, kind, last_at: string | null, last_via: "mail_in" | "mail_out" | "meeting" | "phone" | null, overdue_by: number | null, keep_in_touch_days: number | null, birthday_in: number | null, turning: number | null }`: `listPeople`'s `overdue` filter (most overdue first) and its `birthdays` filter with a 30-day window (soonest first), each at most 30. No identifiers (addresses, numbers) in the reply. `pnpm test:db` tests: a person past their cadence is in `overdue` with `overdue_by`, one within it isn't; a birthday in 5 days is in `birthdays` with `birthday_in` 5 and `turning`, one in 40 days isn't; no `identifiers` key; JSON-safe.
- [x] Both routes are `GET` in `src/routes/device/$.ts`, which answers `POST /device/tasks` and `POST /device/people` with its 404. The new server code runs no `INSERT`, `UPDATE` or `DELETE`.
- [x] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.
- [x] The spec's "The phone" section names the two endpoints and `agent_queue` beside `/device/home`.

## Scope

`src/server/device.ts`, `src/routes/device/$.ts`, `src/server/functions/today.ts` (the shared open-tasks function, moved out of `getTodayFn` to where both can import it), `tests/db/device.test.ts`, `docs/v2-product-engineering-spec.md`. Nothing on the phone.

## Context

- `deviceHome` in `src/server/device.ts` is the model: one function per read, ISO strings, tested by calling it directly (`tests/db/device.test.ts`).
- `getTodayFn`'s `tasks` (`src/server/functions/today.ts`) builds the open tasks from `getGraph`; `agentTasks` there is the same agent set as `agent_queue` without its order or `queued_at`.
- `listPeople` (`src/server/people.ts`) has the `overdue` and `birthdays` filters and the `Person` fields; the web's People list shows them.
- `/device/*` is reached through the `:3848` listener with the owner's identity, as `/device/home` is (AGENTS.md); nothing new listens.

## Notes

- 2026-11-07 [decision] The shared open-tasks function is openTasks (with taskRow for one row) in src/server/graph.ts, not in src/server/functions/today.ts; getTodayFn's tasks and deviceTasks both call it, and the web's Today row field waiting is renamed waiting_on (src/routes/index.tsx) so both replies carry the same fields.
  Reason: No module under src/server imports from src/server/functions/ (those are TanStack server functions); graph.ts already owns GraphNode and taskLine, so the function sits beside the data it reads. One row shape means the fields can't drift either, which a rename-on-map in getTodayFn would have allowed.
  Rejected: Keeping it in functions/today.ts and importing that from device.ts: pulls the server-function module and its imports into the device route
  Rejected: Keeping the web's waiting key and mapping waiting_on to it in getTodayFn: two names for one field
- 2026-11-07 [decision] agent_queue is one SQL read in deviceHome (type task, assignee agent, status active or waiting; ORDER BY queued_at IS NULL, queued_at, updated_at DESC LIMIT 10), not taken from getGraph; devicePeople calls listPeople twice (overdue, and birthdays with a 30-day window), each limited to 30, and maps to the reply's Person fields, dropping identifiers, page, priority and the per-channel dates.
  Reason: /device/home is read every minute; getGraph reads every intent, link and score. listPeople is the People list's own filter and order, so the phone and web agree by construction; its two reads are a small table each at this scale.
  Rejected: getGraph for agent_queue: a full graph read every minute for at most 10 rows
- 2026-11-07 [decision] agent in /device/tasks means what the web's Today row's agent means: the task was made by the agent (via = 'agent'), not assigned to it; agent_queue is the assigned set.
  Reason: The Done when line requires the fields to come from the one function the web uses; the web's tag is made-by-agent.
- 2026-11-07 [handoff] Implemented on branch lor-odtk at bf5d6a3; every Done when line ticked, see ## Report. Proof: tests/db/device.test.ts (device home: the agent's queue; device tasks; device people; device routes) pass, as do pnpm test, pnpm test:db and pnpm typecheck. Review: none yet; a fresh agent should review before atelier merge. Open: not exercised on the running service or the phone.

## Report

Every Done when line holds. `/device/home` now carries `agent_queue` (test "device home: the agent's queue": queued oldest first, then the claimed task with its thread, the owner's and the done agent task left out, JSON-safe; and the cap of 10). `GET /device/tasks` lists the open tasks from `openTasks`, the same function `getTodayFn`'s `tasks` now uses (test "device tasks": due then title, undated last, waiting names Priya, the project named, done, later and archived left out, JSON-safe; and 103 tasks give 100 rows with `open` 103). `GET /device/people` gives Overdue and Birthdays within 30 days from `listPeople` (test "device people": `overdue_by` 10 for a person 40 days out on a 30-day cadence, one called 2 days ago left out, `birthday_in` 5 with `turning`, one at 40 days left out, no `identifiers` key and no number in the JSON). Test "device routes" drives the route handler: both paths answer `GET` with 200 and `POST` with the 404. All in `tests/db/device.test.ts`, all passing. Each of the queue order, the 100 cap and the 30-day window was mutated once and its test failed. `pnpm test` (139), `pnpm test:db` (130, on `life_on_rails_test`) and `pnpm typecheck` pass. The new server code is three `SELECT`-only reads.

### Deviations

- The shared function lives in `src/server/graph.ts`, not `src/server/functions/today.ts`, and the web's Today row field `waiting` is now `waiting_on` (`src/routes/index.tsx`, one use). See the first decision note.

### Decisions

- In Notes: where `openTasks` lives; `agent_queue` by its own SQL and People through `listPeople`; `agent` in `/device/tasks` means made by the agent, as on the web.

### Reply shapes

- `/device/home` adds `agent_queue: { id, title, queued_at: string | null, thread: string | null }[]`.
- `/device/tasks`: `{ now, timezone, today, open, tasks: { id, title, due, overdue, waiting_on, parent, flagged, agent, google }[] }`; `waiting_on` is "someone" for a waiting task with nobody named.
- `/device/people`: `{ today, overdue: Person[], birthdays: Person[] }`, `Person = { id, name, kind, last_at, last_via, overdue_by, keep_in_touch_days, birthday_in, turning }`. Groups can appear in `overdue`, as they do in the web's Overdue filter.

### Open items

- Not checked against the running service or on the phone; lor-lz97 consumes these.
