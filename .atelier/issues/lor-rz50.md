---
created_at: "2026-11-08T02:04:13.919710789+00:00"
id: "lor-rz50"
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
    id: "lor-2mh2"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-dofd"
    type: "implements"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "LoR's panels act on LoR: done, Seen, to the agent, the timer"
updated_at: "2026-11-08T09:24:36.610903746+00:00"
---

## Goal

LoR's panels and blocks act on LoR as the web's Today and Tasks do (Sumeet's yes, 2026-11-07; decision lor-dofd): mark a task done or not, mark a flag Seen, give a task to the agent or take it back, and start, pause and stop the timer. Each write goes through the web's rules on the server (`POST /device/task` from lor-2mh2, and `POST /device/timer`), shows at once, offers Undo where it can be taken back, and puts the row back with the reason when it fails.

## Done when

On `T3_Phone_API35` against the test server (`life_on_rails_test`; never the `lor` CLI, which reaches the real server), unless a line says otherwise:
- [x] **Done.** Due soon's and the Tasks tab's rows have a tick, and every LoR flag card (the `lor` panel's Needs you, the `needs-you` block and the agent page, one shared card) has Done; a Google task has neither and says "Google Tasks", as the web does. A tap applies at once (the row struck through, the flag card gone) by holding the change over the reader's last reply until the next read, sends `POST /device/task` `done` in the process scope (lor-ozj2's F4), then reads `/device/home` (and `/device/tasks` while a `lor` panel is composed) again. Home's Undo bar says "Done: <title>" with Undo, which sends `reopen` and puts the row back at once. Done on a flag card clears the flag too (`completeTask` sets `flagged_at` and `flag_note` to null, and `reopenTask` doesn't restore them), so its Undo reopens the task without the flag: it comes back under Due soon and Tasks, not under Needs you, and the bar says "Done: <title>. Undo reopens it, without the flag." A row ticked on the phone stays struck through, and the done-today rows `/device/tasks` returns (lor-2mh2's `done_today`) are drawn struck through under the open ones, as the web's Due soon draws them; un-ticking either sends `reopen`, as the web's `TaskCheck` does. Shown on `T3_Phone_API35` against the test server: after a tick, `SELECT` on `life_on_rails_test` shows the `task_done` commitment with the device as `source_ref` and the web's `/tasks` screenshot shows it done; after Undo, the commitment is gone. Screenshots.
- [x] **Seen.** Each LoR flag card has Seen, which applies at once (the card goes) and sends `seen`. Home's Undo bar says "Seen: <title>" with no Undo (decision note on this item). Shown by the `SELECT` (no `flagged_at`) and the card gone from all three places.
- [x] **To the agent.** A long press on an owner's open task in the Tasks tab or Due soon offers "Give to the agent", which applies at once (the row tagged "with the agent", and the task under the agent's queue after the tasks already queued, which run oldest first) and sends `queue`; Undo sends `unqueue`. An agent task in the agent's queue (the `lor` panel's and the `t3-queue` block) offers "Take back" from a long press, which sends `unqueue`. Undo of Take back, sending `queue`, is offered only for a task that was queued with no thread; a task a run has already claimed (with a `thread`) gets no Undo, since queueing it again would start a second run on it. Shown by the `SELECT` (`assignee`, `queued_at`) after each and after each Undo.
- [x] **The timer**, in the `today` block and so in the agent page's Today: with a timer running, Pause (Resume while paused) and Stop; with none, Start, which opens a menu of `/device/state`'s recent intents and "Untitled" (no intent). The menu reads `/device/state` each time it opens, showing "Untitled" at once and "Reading…" until the recent intents arrive; if the read fails, the menu shows the reason in plain words (`LorClient.describe`) under "Untitled", which still starts a timer with no intent. Each sends `POST /device/timer` (`start` with the chosen `intent_id`, `pause`, `resume`, `stop`) and applies at once from its reply, which carries what is running. No Undo, as on the web. Shown by the web's Today screenshot and the `time_block` row after a start, a pause, a resume and a stop.
- [x] **On failure**, with the test server stopped: a tick, Seen, Give to the agent and Start each put their row back as it was within a second of the failure and show the reason under it in plain words (`LorClient.describe`, as the cards' failures do), and a refusal shows the server's message (a Done on a task made a Google task by SQL after the read: "this task lives in Google Tasks …"). Each row takes one write at a time: its controls are disabled while its call is under way. Screenshots.
- [x] `LorClient`'s `POST`s are `timer`, `task`, `ingest` and the WhatsApp backup; JVM tests parse `/device/task`'s reply and its refusal (the message shown) from recorded replies.
- [x] The spec's "The phone" section lists what the panels can change.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`net/LorClient.kt`, the `lor/` package (the reader holding a write over its last reply, the panel's rows), `agent/AgentPage.kt` (the shared flag card and Today's timer), `home/Undo.kt` and `HomeScreen.kt` (home's one Undo bar taking a LoR write's Undo beside a layout edit's), and the spec. Not the server (lor-2mh2), not the T3 panel (lor-iabt, whose left column gets these controls from the shared composables).

## Context

- Worked after lor-lz97 and before lor-iabt on lor-clgw's branch, as a child (ordered by a note on lor-clgw, not by blocking).
- The web's actions this mirrors: Today's Needs you (Done, Seen) and Due soon (the tick) and its timer card, in `src/routes/index.tsx`; Tasks' give-to-the-agent (`bulkTasksFn` with `{ op: "assign" }`, which calls `queueTask` or `assignTask`), in `src/routes/tasks.tsx`.
- Home's Undo bar (`home/Undo.kt`) is the launcher's one place for "made at once, take it back" (launcher polish 4.2): here its Undo sends the inverse write instead of restoring `layout.json`.
- Card actions already run in the process scope with one call per card (lor-ozj2's F4, `AgentModel.act`); the writes follow it.

## Notes

- 2026-11-08 [decision] Seen has no Undo: its bar says 'Seen: <title>' alone, as decision lor-dofd rules (the open question on lor-clgw stands for Sumeet).
  Reason: Taking Seen back would be flagTask, which clears notified_at and re-sends the WhatsApp flag, and loses the flag's time and note.
  Rejected: Undo through flagTask (re-sends the flag)
- 2026-11-08 [decision] A write is held over the reader's replies until a read that began after the write ended has the server's word; reads are numbered as they begin (LorReads.begun, Kept.goodRead), so 'began after' is exact.
  Reason: A clock in milliseconds can't order a read and a write that end and begin in the same millisecond (the first JVM test run failed on exactly that); a counter can.
  Rejected: Compare elapsedRealtime of the read's start with the write's end
  Rejected: Drop the hold when the write's reply arrives (a read already under way would bring the old state back)
- 2026-11-08 [decision] A task in Due soon or the Tasks tab counts as the owner's, and so offers Give to the agent, only when /device/home's agent_queue is read and has fewer than 10 tasks and not this one.
  Reason: /device/tasks carries no assignee, and this item changes no server code; queueing a task a run has claimed would start a second run, so when the queue may be cut off at 10 the phone offers nothing rather than guess.
  Rejected: Add assignee to /device/tasks (a server change, outside Scope)
- 2026-11-08 [decision] The Tasks tab draws /device/tasks' done_today under the open groups as DONE TODAY, struck through, and a ticked row stays struck in place until the reread moves it there.
  Reason: The Done when line asks that a row ticked on the phone stays struck through and done-today rows are drawn under the open ones; one group serves both without a second process-wide list.
- 2026-11-08 [decision] The flag card's Open button gives way to Seen and Done; its tile and text still open the task in the dashboard.
  Reason: Three buttons squeeze the card's text in a block; Open was a second way to do what the tile and text already do.

## Report

All eight Done when lines hold on `1170d3e`, checked on `T3_Phone_API35` against the test server (`:3857`, `life_on_rails_test`) with the APK built from that tree on boat (`rz50-b3`). Done, its Undo, un-ticking and the flag card's Done that reopens without the flag are receipt lor-vt3j (with the Tasks tab in the wide form, in landscape); Seen lor-m9q1; Give to the agent and Take back, from the panel and the `t3-queue` block, lor-0rxp; the timer from the `today` block lor-4mze; the failures, the disabled row under way and the 409 refusal lor-v9uo. `assembleDebug testDebugUnitTest` passed on boat (380 tests, 0 failures). `DeviceWritesTest` parses `/device/task`'s recorded replies, its 409 refusal (the message shown) and the timer's reply; `LorWritesTest` covers the hold over each reply, its giving way only to a read begun after the write, the queue order, a failed write putting the row back and a failed Undo keeping the first write.

### Decisions

- Five decision notes of 2026-11-08 on this item: Seen without Undo; the hold keyed to numbered reads; when a task counts as the owner's (Give to the agent); the Tasks tab's DONE TODAY group; Done and Seen in place of the flag card's Open.

### Surprises

- Stop after a few seconds leaves no block: the server drops a block that short. The web's Stop does the same.
- The first JVM run failed because a read and a write in the same millisecond can't be ordered by time, which led to numbering reads (decision note).
- The test replies (`lor/task-*.json`, `timer-paused.json`) were recorded from the test server with `curl`, not from `tests/db/device.test.ts`, so this item adds no server or server-test code.

### Open items

- A row's failure reason stays under it until that row's next write, even after LoR answers again (lor-v9uo, 19: the tick and Seen failures still show after the server came back). The Done when line doesn't ask for it to clear; clearing it on the next good read would be a small follow-up.
- Give to the agent isn't offered when `agent_queue` holds 10 tasks (it may be cut off), until `/device/tasks` carries an assignee.
- If home's activity is rebuilt (not a fold, which home handles in place), the last write's line comes back on the Undo bar for 4 s, with its Undo, because `LorWrites.bar` outlives the activity and `HomeScreen`'s `LaunchedEffect` runs again. Not seen in the checks; a fix is to show a line only within 4 s of the write.
- Real phone and Fold: lor-clgw's end-to-end Fold line and lor-l2zj cover the panels after merge.

