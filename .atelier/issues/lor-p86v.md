---
created_at: "2026-11-07T22:17:23.851864975+00:00"
id: "lor-p86v"
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
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T22:17:58.360212228+00:00"
status: "done"
title: "What the lor panel and LoR's blocks are in layout.json"
updated_at: "2026-11-08T02:04:20.669059229+00:00"
---

## Context

- Reason for (a): the spec ("Who is who") makes the agent page the one place T3's and LoR's cards sit side by side; a block that is that queue keeps them together wherever it is placed, while (b) would split them across two blocks. LoR's spec calls tasks given to the agent "the work queue" (Agent runs, `lor task queue`), which no screen on the phone shows today.
- Reason for `running`: the timer is already in `today`; Running is the agent page's own section.
- Reason for no field on `lor`: a tab costs no schema and no parser tests, and switching it on the phone needs no file write or Undo. A field can be added later without a version bump if one fixed choice per panel is wanted.
- Rejected: (b) for the reason above; `running` as LoR's timer (a duplicate of `today`); `"beside"` on the `lor` panel (a field before anyone needs it).
- A Notes view is not part of the `lor` panel: see the decision note on lor-clgw.

## Question

What do the `lor` panel type and the `today`, `needs-you`, `running` and `t3-queue` block types mean in `layout.json` version 2, and what fields do they take? The strategy names them but not what each holds. They are a persisted contract: lor-f3vn's minimap writes them, and an agent or a hand edit reads them. `needs-you` and `t3-queue` could split the agent page's queue two ways, and "LoR's own" could mean LoR's data or the launcher's.

## Options

- **`needs-you` and `t3-queue`.** (a) `needs-you` is the agent page's whole Needs you queue (T3's approvals, questions and failures, then LoR's flags), and `t3-queue` is LoR's work queue for the agent (tasks given to the agent, queued or being worked in a T3 thread). (b) `t3-queue` is T3's approvals, questions and failures alone, and `needs-you` is LoR's flags alone.
- **`running`.** T3's turns running now (the agent page's Running section), or LoR's running timer.
- **The `lor` panel's fields.** None beyond `at` and `wide`, with what sits beside Today on the wide form chosen on the panel; or a field naming it (`"beside": "tasks"`).

## Decision

(a), `running` is T3's running turns, and the `lor` panel takes no field of its own (planner, 2026-11-07, within the strategy's authority over block types and field names).

- **`lor` panel:** `{ "at": [c, r], "type": "lor" }`, plus `wide` (lor-b1dx: default `full`). No other field. It is LoR's dashboard shaped for the phone. Narrow: Today, LoR's Needs you (flagged tasks, as the web's Today lists them) with the agent's queue under it, and Due soon (the first six open tasks). Wide: that column, without Due soon, on the left, and on the right Tasks or People, switched by two tabs at the column's top. The tab is screen state, kept in memory for the process and not written to the file; it starts on Tasks.
- **Blocks**, each `{ "type": … }` with no other field and at most once in a stack, as every block:
  - `today`: the agent page's Today: the day arc, the running timer, the one thing, the next events.
  - `running`: T3's turns running now, as the agent page's Running section.
  - `needs-you`: the agent page's Needs you queue, with both sources and the same count, all-clear and unseen rules (`AgentStates`), and the same card actions. The approve, deny and answer actions write to T3, never to LoR, as on the agent page.
  - `t3-queue`: LoR's work queue for the agent: open tasks assigned to the agent, those queued for the next run first, then those being worked in a T3 thread. Read-only.
- A block shows at most a fixed number of rows (`needs-you` 3 cards, `running` 3, `t3-queue` 5), then a line "N more" that moves the grid to the layout's `t3` panel (for `needs-you` and `running`) or `lor` panel (for `t3-queue`) when it has one.
- No `version` bump: new optional panel and block types (layout-v2.md's Rules).

## Notes

- 2026-11-07 [note] Its read-only clauses (needs-you's LoR cards, t3-queue) are replaced by decision lor-dofd after Sumeet's yes to panel writes (2026-11-07); the rest stands.

## Report

lor panel with no field of its own (wide default full), tabs for Tasks or People; needs-you is the agent page's whole queue, running is T3's running turns, t3-queue is LoR's work queue for the agent
