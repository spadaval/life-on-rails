---
created_at: "2026-11-07T00:03:36.794995466+00:00"
id: "lor-clgw"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-clgw
priority: "P2"
relationships:
  blocks: []
  children:
  - kind: "issue"
    id: "lor-iabt"
  - kind: "issue"
    id: "lor-lz97"
  - kind: "issue"
    id: "lor-rz50"
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-2mh2"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-odtk"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-ph30"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-sbth"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "LoR and T3 panels"
updated_at: "2026-11-08T03:46:49.780707219+00:00"
---

## Goal

LoR and T3 get panels that use the space, in native Compose with no WebView. The `lor` panel is LoR's dashboard shaped for the phone: narrow, Today, Needs you with the agent's queue, and Due soon; wide, that beside Tasks or People. LoR's and the agent page's pieces are also blocks a stack can hold: `today`, `needs-you`, `running`, `t3-queue` (decision lor-p86v). The `t3` panel's wide form puts every thread beside the chosen one, with its last messages, what it waits on, and a reply box of its own. The panels act on LoR as the web's Today and Tasks do: a task done, a flag Seen, a task given to the agent, the timer (Sumeet's yes, 2026-11-07; decision lor-dofd), through the web's rules on the server.

## Done when

- [ ] The server's standalone items are merged (lor-odtk, the reads; lor-2mh2, the writes), and each child is done, with its proof, in this order on this item's branch: lor-lz97 (the `lor` panel and the blocks), lor-rz50 (the panels' writes), lor-iabt (the T3 panel's wide form).
- [ ] End to end on `T3_Fold_API35` against the test server and a scratch T3 server, in one session, with a layout of the `t3` panel, home with a `today` and a `needs-you` block above its apps, and a `lor` panel beside it: folded, each cell drawn; unfolded, the `t3` panel's wide form, home and the `lor` panel's wide form; an approval made on the T3 panel clears from the `needs-you` block and the agent page; a flag marked Done on the `needs-you` block leaves the `lor` panel and the agent page at once, and its Undo reopens the task without its flag, so it shows under the `lor` panel's Due soon and in none of the three Needs you lists; a task flagged in the test database (by SQL on `life_on_rails_test`, never with the `lor` CLI, which reaches the real server) shows in the `lor` panel and the block within a minute. Screenshots of each step, recorded as an evidence receipt on this item.
- [ ] Launcher polish 7.8's home-press measurement on `T3_Phone_API35` and the unfolded `T3_Fold_API35`, with the layout above, on this branch and on `main` at the commit it branched from: the median `TotalTime` on this branch is within 10% of the base. In the same receipt or one beside it.

## Scope

The server's `/device/*` reads and writes, the phone's `home/` (layout and stack), `agent/`, `net/` and a new `lor/` package, `layout-v2.md` and the spec. Not writes the web doesn't offer, or writes needing a form (decision lor-dofd), not a Notes view, not widgets or the clock block (lor-sbth), not the minimap (lor-f3vn).

## Context

- Waits on lor-ph30 and lor-sbth (decision lor-tbqc: its shared `/device/home` reader and the stack's overflow scroll) as merged. From lor-ph30: `wide`, the inner screen's rules and drawing, and folding without recreation.
- The strategy's Shape governs: native, not web; panels write to LoR's data through the server functions the web's actions use.
- The real Fold's check is the `needs-human` validation item lor-l2zj, after merge; the end-to-end check above is its stand-in.

## Notes

- 2026-11-07 [decision] Which LoR views get a phone-shaped form: Today (with LoR's Needs you, the agent's queue and Due soon), Tasks and People, in the lor panel; no Notes view. Each needs from the server beyond /device/home: the agent's queue (added to /device/home as agent_queue, since every LoR block reads that reply anyway), the open tasks (GET /device/tasks) and the people to be in touch with (GET /device/people); Today needs nothing new. Goals this week stays on the web.
  Reason: Today, Tasks and People are lists a glance answers on a phone: what's on, what's due, who's overdue or has a birthday. All three are useful read-only, each row opening its place in the dashboard. Notes is the wiki: reading it well needs Markdown and wiki links drawn natively, a second wiki reader, and its use is editing, a write; the dashboard already opens a page from any link. Endpoints only where /device/home lacks the data, and the agent's queue in /device/home so a stack with a t3-queue block costs no second read.
  Rejected: A Notes view (recent pages, or a page reader): a native Markdown and wiki-link renderer for a view whose point is editing
  Rejected: One /device/lor endpoint holding everything: tasks and people would be read every minute for every block, even with no lor panel on the grid
- 2026-11-07 [note] Sumeet said yes to panels that write to LoR's data (2026-11-07); the writes are planned in decision lor-dofd, lor-2mh2 (server) and lor-rz50 (phone).
- 2026-11-07 [plan] Planned 2026-11-07: children lor-odtk (server reads), lor-lz97 (lor panel and blocks), lor-iabt (T3 panel's wide form), in that order on this item's branch; decision lor-p86v (the lor panel and block types in layout.json); real-Fold validation lor-l2zj after merge, outside the mission. A fresh agent reviews this plan before lor-odtk starts, and lor-ph30 must merge first.
- 2026-11-07 [decision] lor-odtk is a standalone blocker of lor-clgw on its own branch, merging alone (it adds read endpoints and a field to /device/home, nothing on the phone's screen).
  Reason: lor-odtk is server-only and doesn't depend on the inner screen or widgets, so it can run while lor-ph30 is built; as a child it would wait for lor-sbth and lor-ph30.
- 2026-11-07 [decision] Children are worked in order on branch lor-clgw: lor-lz97, then lor-iabt, each reviewed as its own slice.
  Reason: Atelier keeps a child open until its parent merges, so blocked_by between siblings stalls them.
- 2026-11-07 [decision] Panel writes are planned as a standalone server item lor-2mh2 (POST /device/task, merging alone, as lor-odtk did) and a phone child lor-rz50 on this branch, worked after lor-lz97 and before lor-iabt.
  Reason: The server part needs no phone and can merge while lor-lz97 is built; the phone writes are their own reviewable slice rather than six more Done when lines in lor-lz97, which already draws and reads every panel and block. Siblings are ordered by this note, not by blocked_by.
  Rejected: Writes folded into lor-lz97: one slice too large to review in one pass
- 2026-11-07 [note] Question for Sumeet: Seen on a flag has no Undo (decision lor-dofd), because taking it back would re-flag the task and re-send the WhatsApp message, and the flag's time and note would be lost; a mis-tap loses the flag. Is that acceptable, or should Seen get an Undo that restores the flag exactly (a server rule the web doesn't have)?
