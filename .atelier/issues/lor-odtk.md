---
created_at: "2026-11-07T22:17:28.574588572+00:00"
id: "lor-odtk"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The server's reads for LoR's panels: the agent's queue, tasks, people"
updated_at: "2026-11-07T22:17:28.574588572+00:00"
---

## Goal

LoR's server gives the phone's `lor` panel and `t3-queue` block what `/device/home` lacks, read-only: the agent's work queue, the open tasks, and the people to be in touch with. Each reply carries what the web's Today, Tasks and People show for the same rows, so the phone and the web agree.

## Done when

- [ ] `GET /device/home` gains `agent_queue`: `{ id, title, queued_at: string | null, thread: string | null }[]`, the open tasks (`status` active or waiting) with `assignee = 'agent'`; those with `queued_at` first, oldest first, then the rest by `updated_at`, newest first; at most 10. `thread` is `thread_ref` (the T3 thread working it). A `pnpm test:db` test in `tests/db/device.test.ts`: a queued task, a claimed one (`claimTask`: `thread` set, `queued_at` null), an owner's task and a done agent task (both left out), in that order, and the reply JSON-safe.
- [ ] `GET /device/tasks` returns `{ now, timezone, today: "YYYY-MM-DD", open: number, tasks: { id, title, due: "YYYY-MM-DD" | null, overdue: boolean, waiting_on: string | null, parent: string | null, flagged: boolean, agent: boolean, google: boolean }[] }`: the open tasks the web's Today lists in Due soon, in its order (due date, then title), at most 100, with `open` the full count. The selection, order and fields come from one function that `getTodayFn`'s `tasks` also uses (its open part), so the two can't drift. `pnpm test:db` tests: order by due then title; a task with no due date last; a waiting task names who it waits on; a task under a project names the project; done, later and archived tasks left out; the cap of 100 with `open` still counting all; JSON-safe.
- [ ] `GET /device/people` returns `{ today: "YYYY-MM-DD", overdue: Person[], birthdays: Person[] }`, `Person = { id, name, kind, last_at: string | null, last_via: "mail_in" | "mail_out" | "meeting" | "phone" | null, overdue_by: number | null, keep_in_touch_days: number | null, birthday_in: number | null, turning: number | null }`: `listPeople`'s `overdue` filter (most overdue first) and its `birthdays` filter with a 30-day window (soonest first), each at most 30. No identifiers (addresses, numbers) in the reply. `pnpm test:db` tests: a person past their cadence is in `overdue` with `overdue_by`, one within it isn't; a birthday in 5 days is in `birthdays` with `birthday_in` 5 and `turning`, one in 40 days isn't; no `identifiers` key; JSON-safe.
- [ ] Both routes are `GET` in `src/routes/device/$.ts`, which answers `POST /device/tasks` and `POST /device/people` with its 404. The new server code runs no `INSERT`, `UPDATE` or `DELETE`.
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.
- [ ] The spec's "The phone" section names the two endpoints and `agent_queue` beside `/device/home`.

## Scope

`src/server/device.ts`, `src/routes/device/$.ts`, `src/server/functions/today.ts` (the shared open-tasks function, moved out of `getTodayFn` to where both can import it), `tests/db/device.test.ts`, `docs/v2-product-engineering-spec.md`. Nothing on the phone.

## Context

- `deviceHome` in `src/server/device.ts` is the model: one function per read, ISO strings, tested by calling it directly (`tests/db/device.test.ts`).
- `getTodayFn`'s `tasks` (`src/server/functions/today.ts`) builds the open tasks from `getGraph`; `agentTasks` there is the same agent set as `agent_queue` without its order or `queued_at`.
- `listPeople` (`src/server/people.ts`) has the `overdue` and `birthdays` filters and the `Person` fields; the web's People list shows them.
- `/device/*` is reached through the `:3848` listener with the owner's identity, as `/device/home` is (AGENTS.md); nothing new listens.
