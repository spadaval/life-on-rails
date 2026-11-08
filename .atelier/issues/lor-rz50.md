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
status: "todo"
title: "LoR's panels act on LoR: done, Seen, to the agent, the timer"
updated_at: "2026-11-08T02:04:13.919710789+00:00"
---

## Goal

LoR's panels and blocks act on LoR as the web's Today and Tasks do (Sumeet's yes, 2026-11-07; decision lor-dofd): mark a task done or not, mark a flag Seen, give a task to the agent or take it back, and start, pause and stop the timer. Each write goes through the web's rules on the server (`POST /device/task` from lor-2mh2, and `POST /device/timer`), shows at once, offers Undo where it can be taken back, and puts the row back with the reason when it fails.

## Done when

On `T3_Phone_API35` against the test server (`life_on_rails_test`; never the `lor` CLI, which reaches the real server), unless a line says otherwise:
- [ ] **Done.** Due soon's and the Tasks tab's rows have a tick, and every LoR flag card (the `lor` panel's Needs you, the `needs-you` block and the agent page, one shared card) has Done; a Google task has neither and says "Google Tasks", as the web does. A tap applies at once (the row struck through, the flag card gone) by holding the change over the reader's last reply until the next read, sends `POST /device/task` `done` in the process scope (lor-ozj2's F4), then reads `/device/home` (and `/device/tasks` while a `lor` panel is composed) again. Home's Undo bar says "Done: <title>" with Undo, which sends `reopen` and puts the row back at once. Done on a flag card clears the flag too (`completeTask` sets `flagged_at` and `flag_note` to null, and `reopenTask` doesn't restore them), so its Undo reopens the task without the flag: it comes back under Due soon and Tasks, not under Needs you, and the bar says "Done: <title>. Undo reopens it, without the flag." A row ticked on the phone stays struck through, and the done-today rows `/device/tasks` returns (lor-2mh2's `done_today`) are drawn struck through under the open ones, as the web's Due soon draws them; un-ticking either sends `reopen`, as the web's `TaskCheck` does. Shown on `T3_Phone_API35` against the test server: after a tick, `SELECT` on `life_on_rails_test` shows the `task_done` commitment with the device as `source_ref` and the web's `/tasks` screenshot shows it done; after Undo, the commitment is gone. Screenshots.
- [ ] **Seen.** Each LoR flag card has Seen, which applies at once (the card goes) and sends `seen`. Home's Undo bar says "Seen: <title>" with no Undo (decision note on this item). Shown by the `SELECT` (no `flagged_at`) and the card gone from all three places.
- [ ] **To the agent.** A long press on an owner's open task in the Tasks tab or Due soon offers "Give to the agent", which applies at once (the row tagged "with the agent", and the task under the agent's queue after the tasks already queued, which run oldest first) and sends `queue`; Undo sends `unqueue`. An agent task in the agent's queue (the `lor` panel's and the `t3-queue` block) offers "Take back" from a long press, which sends `unqueue`. Undo of Take back, sending `queue`, is offered only for a task that was queued with no thread; a task a run has already claimed (with a `thread`) gets no Undo, since queueing it again would start a second run on it. Shown by the `SELECT` (`assignee`, `queued_at`) after each and after each Undo.
- [ ] **The timer**, in the `today` block and so in the agent page's Today: with a timer running, Pause (Resume while paused) and Stop; with none, Start, which opens a menu of `/device/state`'s recent intents and "Untitled" (no intent). The menu reads `/device/state` each time it opens, showing "Untitled" at once and "Reading…" until the recent intents arrive; if the read fails, the menu shows the reason in plain words (`LorClient.describe`) under "Untitled", which still starts a timer with no intent. Each sends `POST /device/timer` (`start` with the chosen `intent_id`, `pause`, `resume`, `stop`) and applies at once from its reply, which carries what is running. No Undo, as on the web. Shown by the web's Today screenshot and the `time_block` row after a start, a pause, a resume and a stop.
- [ ] **On failure**, with the test server stopped: a tick, Seen, Give to the agent and Start each put their row back as it was within a second of the failure and show the reason under it in plain words (`LorClient.describe`, as the cards' failures do), and a refusal shows the server's message (a Done on a task made a Google task by SQL after the read: "this task lives in Google Tasks …"). Each row takes one write at a time: its controls are disabled while its call is under way. Screenshots.
- [ ] `LorClient`'s `POST`s are `timer`, `task`, `ingest` and the WhatsApp backup; JVM tests parse `/device/task`'s reply and its refusal (the message shown) from recorded replies.
- [ ] The spec's "The phone" section lists what the panels can change.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`net/LorClient.kt`, the `lor/` package (the reader holding a write over its last reply, the panel's rows), `agent/AgentPage.kt` (the shared flag card and Today's timer), `home/Undo.kt` and `HomeScreen.kt` (home's one Undo bar taking a LoR write's Undo beside a layout edit's), and the spec. Not the server (lor-2mh2), not the T3 panel (lor-iabt, whose left column gets these controls from the shared composables).

## Context

- Worked after lor-lz97 and before lor-iabt on lor-clgw's branch, as a child (ordered by a note on lor-clgw, not by blocking).
- The web's actions this mirrors: Today's Needs you (Done, Seen) and Due soon (the tick) and its timer card, in `src/routes/index.tsx`; Tasks' give-to-the-agent (`bulkTasksFn` with `{ op: "assign" }`, which calls `queueTask` or `assignTask`), in `src/routes/tasks.tsx`.
- Home's Undo bar (`home/Undo.kt`) is the launcher's one place for "made at once, take it back" (launcher polish 4.2): here its Undo sends the inverse write instead of restoring `layout.json`.
- Card actions already run in the process scope with one call per card (lor-ozj2's F4, `AgentModel.act`); the writes follow it.
