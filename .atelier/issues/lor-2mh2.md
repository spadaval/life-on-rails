---
created_at: "2026-11-08T02:04:13.888655116+00:00"
id: "lor-2mh2"
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
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The server's writes for LoR's panels: POST /device/task"
updated_at: "2026-11-08T02:04:13.888655116+00:00"
---

## Goal

LoR's panels on the phone can do what the web's Today and Tasks let Sumeet do to a task (Sumeet, 2026-11-07: panels may write): mark it done or not done, mark a flag Seen, and give it to the agent or take it back. One `POST` route carries these, calling the same `intents.ts` functions the web's server functions call, so a write from the phone follows the web's rules and refusals. The timer already has its route (`POST /device/timer`), which the panels reuse as it is.

## Done when

- [ ] `POST /device/task` takes `{ id: uuid, action: "done" | "reopen" | "seen" | "queue" | "unqueue", device?: string }` and calls, for each action, what the web calls: `done` is `completeTask(sql, id, { via: "ui", sourceRef: device })` (as `taskDoneFn` with `done: true`, with the device as `deviceTimer` records it); `reopen` is `reopenTask` (`taskDoneFn` with `done: false`); `seen` is `unflagTask` (`unflagFn`); `queue` is `queueTask` and `unqueue` is `assignTask(sql, id, "owner")` (as `bulkTasksFn`'s `assign` op, which Tasks uses, and `assignFn` call them). It holds no rule of its own beyond choosing the function.
- [ ] It replies with the task after the write: `{ id, title, status, done: boolean, assignee: "owner" | "agent", queued_at: string | null, flagged: boolean }`, ISO strings, JSON-safe. A refusal keeps the route's existing error shape: `400 bad-input` for a bad body or an action the task can't take (queueing a done task), `404 not-found` for an unknown id, `409` with the code and message `Refused` gives (`google-task`: "this task lives in Google Tasks: complete it there …", and the same on reopen).
- [ ] `/device/tasks` gains `done_today`: the tasks marked done today, in its rows' shape, as `getTodayFn`'s `tasks` lists them after the open ones (from the same shared function), so the phone can draw them struck through and un-tick them. A `pnpm test:db` test: a task done today is in `done_today` and not `tasks`, one done yesterday in neither.
- [ ] `/device/home`'s `needs_you` rows and `/device/tasks`' rows both carry `google: boolean`, so the phone offers Done only where the web does. (`/device/tasks` already has it from lor-odtk; `needs_you` gains it.)
- [ ] `pnpm test:db` tests in `tests/db/device.test.ts`, calling the route's function directly as the other device tests do: `done` writes one `task_done` commitment with `via` `ui` and `source_ref` the device, and a second `done` writes no second one; `reopen` removes it and the task is active again; `done` and `reopen` on a Google task are refused with `google-task` and change nothing; `seen` clears `flagged_at` and `flag_note`, and the task stays open and leaves `needs_you`; `queue` sets the agent and `queued_at` and the task appears in `/device/home`'s `agent_queue` after the tasks queued before it; `unqueue` puts it back with the owner, no `queued_at` and no `thread_ref`; `queue` on a done task is a bad-input refusal; an unknown id is not found; an unknown action is bad input; each reply is JSON-safe. A test that `GET /device/task` answers 404 through the route's handler.
- [ ] `deviceTimer`'s existing test also covers `start` with an `intent_id` and `resume` (the actions the `today` block sends besides `toggle` and `pause`).
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.
- [ ] The spec's "The phone" section names `POST /device/task` beside `/device/timer`, and says the phone's panels write through the web's rules.

## Scope

`src/server/device.ts`, `src/routes/device/$.ts`, `tests/db/device.test.ts`, `docs/v2-product-engineering-spec.md`. Nothing on the phone. No change to `intents.ts` or the web's server functions.

## Context

- Merges alone on its own branch, as lor-odtk did, before lor-rz50 starts.
- The web's actions: `taskDoneFn`, `unflagFn`, `bulkTasksFn` (Tasks' `assign` op) and `assignFn` in `src/server/functions/graph.ts`; their rules are in `src/server/intents.ts` (`completeTask`, `reopenTask`, `unflagTask`, `queueTask`, `assignTask`).
- None of these touches `time_block`; the timer's writes stay in `deviceTimer`, which already goes through `startManualBlock`, `pauseRunningBlock`, `resumeRunningBlock` and `stopRunningBlock`, the functions `timerFn` uses.
- `/device/*` is reached through the `:3848` listener with the owner's identity (AGENTS.md); nothing new listens.
