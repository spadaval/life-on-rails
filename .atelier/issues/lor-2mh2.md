---
created_at: "2026-11-08T02:04:13.888655116+00:00"
id: "lor-2mh2"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-2mh2
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
title: "The server's writes for LoR's panels: POST /device/task"
updated_at: "2026-11-08T02:17:57.003029150+00:00"
---

## Goal

LoR's panels on the phone can do what the web's Today and Tasks let Sumeet do to a task (Sumeet, 2026-11-07: panels may write): mark it done or not done, mark a flag Seen, and give it to the agent or take it back. One `POST` route carries these, calling the same `intents.ts` functions the web's server functions call, so a write from the phone follows the web's rules and refusals. The timer already has its route (`POST /device/timer`), which the panels reuse as it is.

## Done when

- [x] `POST /device/task` takes `{ id: uuid, action: "done" | "reopen" | "seen" | "queue" | "unqueue", device?: string }` and calls, for each action, what the web calls: `done` is `completeTask(sql, id, { via: "ui", sourceRef: device })` (as `taskDoneFn` with `done: true`, with the device as `deviceTimer` records it); `reopen` is `reopenTask` (`taskDoneFn` with `done: false`); `seen` is `unflagTask` (`unflagFn`); `queue` is `queueTask` and `unqueue` is `assignTask(sql, id, "owner")` (as `bulkTasksFn`'s `assign` op, which Tasks uses, and `assignFn` call them). It holds no rule of its own beyond choosing the function.
- [x] It replies with the task after the write: `{ id, title, status, done: boolean, assignee: "owner" | "agent", queued_at: string | null, flagged: boolean }`, ISO strings, JSON-safe. A refusal keeps the route's existing error shape: `400 bad-input` for a bad body or an action the task can't take (queueing a done task), `404 not-found` for an unknown id, `409` with the code and message `Refused` gives (`google-task`: "this task lives in Google Tasks: complete it there …", and the same on reopen).
- [x] `/device/tasks` gains `done_today`: the tasks marked done today, in its rows' shape, so the phone can draw them struck through and un-tick them. The rule moves out of `getTodayFn` (today.ts, the inline `doneToday` set and its filter) into `doneTodayTasks(db, nodes, date): Promise<TaskRow[]>` beside `openTasks` in `src/server/graph.ts`: the tasks with status `done` and a `task_done` commitment inside the local day's bounds (`localDayBounds` from `src/shared/localTime.ts`), mapped with `taskRow`. `getTodayFn` and `deviceTasks` both call it, and `getTodayFn`'s `tasks` is unchanged (open, then done today). A `pnpm test:db` test: a task done today is in `done_today` and not `tasks`, one done yesterday in neither.
- [x] `/device/home`'s `needs_you` rows and `/device/tasks`' rows both carry `google: boolean`, so the phone offers Done only where the web does. (`/device/tasks` already has it from lor-odtk; `needs_you` gains it.)
- [x] `pnpm test:db` tests in `tests/db/device.test.ts`, calling the route's function directly as the other device tests do: `done` writes one `task_done` commitment with `via` `ui` and `source_ref` the device, and a second `done` writes no second one; `reopen` removes it and the task is active again; `done` and `reopen` on a Google task are refused with `google-task` and change nothing; `seen` clears `flagged_at` and `flag_note`, and the task stays open and leaves `needs_you`; `queue` sets the agent and `queued_at` and the task appears in `/device/home`'s `agent_queue` after the tasks queued before it; `unqueue` puts it back with the owner, no `queued_at` and no `thread_ref`; `queue` on a done task is a bad-input refusal; an unknown id is not found; an unknown action is bad input; each reply is JSON-safe. A test that `GET /device/task` answers 404 through the route's handler.
- [x] `deviceTimer`'s existing test also covers `start` with an `intent_id` and `resume` (the actions the `today` block sends besides `toggle` and `pause`).
- [x] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.
- [x] The spec's "The phone" section names `POST /device/task` beside `/device/timer`, gives `GET /device/tasks` its `done_today` list, and says the phone's panels write through the web's rules.

## Scope

`src/server/device.ts`, `src/routes/device/$.ts`, `src/server/graph.ts` (`doneTodayTasks`), `src/server/functions/today.ts` (`getTodayFn` calling it, nothing else changed), `tests/db/device.test.ts`, `docs/v2-product-engineering-spec.md`. Nothing on the phone. No change to `intents.ts`, and no other change to the web's server functions.

## Context

- Merges alone on its own branch, as lor-odtk did, before lor-rz50 starts.
- The web's actions: `taskDoneFn`, `unflagFn`, `bulkTasksFn` (Tasks' `assign` op) and `assignFn` in `src/server/functions/graph.ts`; their rules are in `src/server/intents.ts` (`completeTask`, `reopenTask`, `unflagTask`, `queueTask`, `assignTask`).
- None of these touches `time_block`; the timer's writes stay in `deviceTimer`, which already goes through `startManualBlock`, `pauseRunningBlock`, `resumeRunningBlock` and `stopRunningBlock`, the functions `timerFn` uses.
- `/device/*` is reached through the `:3848` listener with the owner's identity (AGENTS.md); nothing new listens.

## Notes

- 2026-11-07 [decision] deviceTask reads its reply with its own SELECT on intent (id, title, status, assignee, queued_at, flagged_at) and throws NotFound when the row is missing, rather than returning the write function's IntentView.
  Reason: IntentView has no queued_at, which the reply needs, and the five functions return different things (unflagTask returns nothing). unflagTask on an unknown id updates nothing and says nothing, so the read after the write is what turns seen on an unknown id into 404 not-found without adding a rule of the route's own; the other four already throw NotFound from getIntent.
  Rejected: Adding queued_at to IntentView: changes intents.ts, which the item's scope excludes
  Rejected: An existence check before the write: a rule of the route's own, duplicating getIntent
- 2026-11-07 [decision] The body's device is z.string().max(64).nullish(), and an unknown action is a ZodError, which the route already maps to 400 bad-input.
  Reason: Matches deviceTimer's optional device (with ingest's 64-character cap so source_ref stays short), and keeps the route's one error mapping.
- 2026-11-07 [decision] The timer coverage is a second test in the 'device timer and state' block (start with intent_id, pause, resume) rather than more lines in the toggle test.
  Reason: Resume ends the paused block and starts a new one on the same intent (a pause is never inside a block), so the check is about block rows, a different subject from the toggle test's minutes; one rule per test keeps a failure readable.
- 2026-11-07 [handoff] Implemented on branch lor-2mh2 (d281dbc); every Done when line ticked; see ## Report. Review: none yet. Open: getTodayFn's tasks is checked by typecheck and reading only.

## Report

All eight Done when lines hold. `POST /device/task` (`deviceTask` in `src/server/device.ts`, routed in `src/routes/device/$.ts`) calls `completeTask`, `reopenTask`, `unflagTask`, `queueTask` or `assignTask(…, "owner")` and nothing else, then replies `{id, title, status, done, assignee, queued_at, flagged}`. `doneTodayTasks` in `src/server/graph.ts` now owns the done-today rule; `getTodayFn` and `deviceTasks` both call it, and `/device/tasks` gains `done_today`. `needs_you` rows carry `google`. The proof is `tests/db/device.test.ts`: the "device task writes" block (done once with the device as `source_ref`, reopen; Google refusals that change nothing, and `needs_you` saying `google: true`; seen; queue behind earlier queued tasks and unqueue clearing the thread; queue on a done task, unknown id for all five actions, unknown action; the route's 200, 409, 404, 400 replies and `GET /device/task` answering 404), "lists the tasks done today under done_today…" and "starts on a chosen intent and resumes after a pause…". After the last change, `pnpm test` passed 149 tests, `pnpm test:db` 140 (132 before) and `pnpm typecheck` was clean.

Each rule was broken once and its test failed: done without the device, reopen as a no-op, seen as a no-op, queue as a plain assign, unqueue as a no-op, the reply's `flagged` forced false, `needs_you`'s `google` forced false, `doneTodayTasks` without the day bounds, `done_today` empty, the route line removed, the reply without its not-found read, the timer ignoring `intent_id`, and resume as a no-op.

### Deviations

- The timer coverage is a new test beside the toggle test rather than more lines in it (decision note).

### Decisions

- The reply is read with its own `SELECT`, which is also what makes `seen` on an unknown id a 404; the `device` body field is capped at 64 characters (decision notes, 2026-11-07).

### Surprises

- `unflagTask` on an unknown id succeeds silently; the route's read after the write is what reports it.
- A paused-then-resumed timer whose first block has no length loses that block on close, so the new test backdates the start, as the toggle test does.
- One baseline `pnpm test:db` run had a failure that didn't recur in two reruns before any change; it looks like another worktree sharing `life_on_rails_test`.

### Open items

- `getTodayFn`'s `tasks` has no test of its own (it's a server function). The change is a straight swap to `doneTodayTasks`, which keeps the same order and rows, checked by typecheck and by reading only.
- `pnpm`'s pre-run install refuses a worktree's symlinked `node_modules`, so the three checks ran as `npx vitest run`, `npx vitest run --config vitest.db.config.ts` and `npx tsc --noEmit`, the scripts' own commands.
