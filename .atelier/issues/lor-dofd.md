---
created_at: "2026-11-08T02:04:13.860196821+00:00"
id: "lor-dofd"
issue_type: "decision"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-p86v"
    type: "related"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T02:04:20.688586556+00:00"
status: "done"
title: "What LoR's panels may change, and through which route"
updated_at: "2026-11-08T02:04:20.688586556+00:00"
---

## Context

- One route: the actions share the id, the reply and the errors; a route per action would repeat them five times. The web's server functions are TanStack server functions, reached only from the web app, and the phone talks to `/device/*`.
- Seen without Undo: the web has none, and taking it back would be `flagTask`, which clears `notified_at`, so the channel would send the flag to WhatsApp again, and the original flag time would be lost. A second rule that restores a flag exactly would be a rule the web doesn't have.
- The web's People view has no action that logs a contact: last-in-touch comes only from mail, calls, texts, WhatsApp and meetings. Its actions are a person's mail priority, keep-in-touch cadence and birthday, which are settings rather than a glance's action; none is planned for the People tab.
- Left on the web: Today's metric prompt, confirming and relabelling blocks, and Tasks' due date, Later, parent and delete. Each needs a picker or a form, which the dashboard, a tap away, already has.
- Rejected: a route per action (the same id, reply and errors five times); Undo for Seen through `flagTask` (re-sends the WhatsApp flag); the People tab's settings (a form, not a glance).

## Question

Sumeet said yes to panels that write to LoR's data (2026-11-07: "I don't really see why we're making the panels read-only... Making the panels interactive is not that difficult"). Which writes do the panels get, how do they reach the server without a second copy of its rules, and which can be taken back? This changes decision lor-p86v's read-only clauses and adds to the phone's `/device/` contract.

## Options

- **Which writes.** What the web's Today, Tasks and People let him do, as far as it makes sense on a panel; or only what was asked first (done, Seen, the timer, to the agent).
- **The route.** One `POST /device/task` with an `action`, or a route per action, or the web's server functions called from the phone.
- **Undo.** For every write, or where the write has a clean inverse the web also offers.

## Decision

The asked set, through one route, with Undo where the web's own inverse write restores the task, and saying what it doesn't restore (planner, 2026-11-07, on Sumeet's yes and the strategy's revised Authority).

- **Writes:** a task done (`completeTask`) and not done (`reopenTask`), a flag Seen (`unflagTask`), a task given to the agent (`queueTask`) and taken back (`assignTask(…, "owner")`), and the timer's start, pause, resume and stop.
- **Route:** `POST /device/task` `{ id, action: done | reopen | seen | queue | unqueue, device? }`, replying with the task after the write; the timer keeps `POST /device/timer`. Both call the functions the web's server functions call, with `via: "ui"` and the device as `sourceRef`.
- **Undo:** done and reopen undo each other on a task row. Done on a flag card also clears the flag (`completeTask`), which `reopenTask` doesn't restore, so its Undo reopens the task without the flag and says so. Queue and unqueue undo each other, except that Take back on a task a run has already claimed (with a thread) has no Undo, since queueing it again would start a second run. Seen has no Undo; the timer has none (as on the web).
- **lor-p86v's clauses change:** `needs-you`'s LoR flag cards get Done and Seen, and `t3-queue` gets Take back, so neither is read-only any more. Everything else in lor-p86v stands.

## Report

Done, reopen, Seen, to the agent and back, and the timer, through POST /device/task (one route, the web's intents.ts functions) and POST /device/timer; Undo for done/reopen (a flag card's Done reopens without the flag) and queue/unqueue (none after a run claimed the task); no Undo for Seen or the timer
