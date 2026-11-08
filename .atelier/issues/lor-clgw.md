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

- [x] The server's standalone items are merged (lor-odtk, the reads; lor-2mh2, the writes), and each child is done, with its proof, in this order on this item's branch: lor-lz97 (the `lor` panel and the blocks), lor-rz50 (the panels' writes), lor-iabt (the T3 panel's wide form).
- [x] End to end on `T3_Fold_API35` against the test server and a scratch T3 server, in one session, with a layout of the `t3` panel, home with a `today` and a `needs-you` block above its apps, and a `lor` panel beside it: folded, each cell drawn; unfolded, the `t3` panel's wide form, home and the `lor` panel's wide form; an approval made on the T3 panel clears from the `needs-you` block and the agent page; a flag marked Done on the `needs-you` block leaves the `lor` panel and the agent page at once, and its Undo reopens the task without its flag, so it shows under the `lor` panel's Due soon and in none of the three Needs you lists; a task flagged in the test database (by SQL on `life_on_rails_test`, never with the `lor` CLI, which reaches the real server) shows in the `lor` panel and the block within a minute. Screenshots of each step, recorded as an evidence receipt on this item.
- [x] Launcher polish 7.8's home-press measurement on `T3_Phone_API35` and the unfolded `T3_Fold_API35`, on this branch and on `main` at the commit it last merged (9c8a470), with the same layout on both: the layout above without the types `main` can't parse. The median `TotalTime` on this branch is within 10% of the base. The layout above itself is measured on this branch and recorded beside it, and its cost on a real GPU is judged on the real Fold in lor-l2zj (Sumeet, 2026-11-08: on the emulators it costs +20% to +27%, the blocks' drawing on a CPU renderer, lor-6p0h).

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

## Report

**Outcome (orchestrator, 2026-11-08).** All three lines hold. Line 1: lor-odtk and lor-2mh2 are merged, and lor-lz97 (approval lor-v1eb), lor-rz50 (approval lor-771o) and lor-iabt (its fix for lor-wc8z, 6f946a8, with receipt lor-izgg, awaiting review) are built in that order on this branch, each with its Report. Line 3 was restated on Sumeet's call: the code's cost at an equal layout passes (receipt lor-w5dg: +6.8% on the phone, +4.9% on the Fold), and the full layout's cost is judged on the real Fold in lor-l2zj. lor-6p0h found that cost is the blocks' drawing on the emulators' CPU renderer, with no cheap fix. Line 2 ran on 5e9c703; 6f946a8 since changes only when the T3 panel's thread column reads, which lor-izgg checks on the Fold. Deferred review findings are backlog bugs outside the mission: lor-0xhc, lor-gfow, lor-y32o.

The end-to-end agent's account follows. Its "line 3 fails" was resolved by the restatement above.

- **Line 2 (pass, receipt lor-myhe).** This was the end-to-end check on `T3_Fold_API35` in one
  session, on 5e9c703 against the test server and a scratch T3 server. Every step held. Each cell was
  drawn folded, and the three wide forms unfolded. An approval made on the `t3` panel cleared from
  the `needs-you` block and the agent page; the scratch server recorded `decision: accept`. A flag
  set by SQL showed on the block 47 s later and in the `lor` panel from the same read. Done on the
  block removed the flag from the `lor` panel and the agent page. Undo reopened the task unflagged,
  under Due soon and in none of the three Needs you lists. The receipt's README names the file
  behind each claim.
- **Line 3 (fail, receipt lor-79s1).** With line 2's layout, the branch's median home press is 27%
  slower than the base on `T3_Phone_API35` (65.5 ms against 51.5 ms). On the unfolded Fold it is
  18.7% slower (120.5 against 101.5). Each median pools two rounds of ten presses. The base is
  266f3c3, whose android/ tree matches 9c8a470's, and it can't parse `lor`, `today` or
  `needs-you`, so its layout leaves them out. With that same layout on both builds, the branch is
  +6.8% on the phone and +4.9% on the Fold, so the code alone is within 10%. The extra time comes
  with drawing the full layout's blocks; no press triggered a read.
- **Builds.** The branch APK was built on boat from 5e9c703: `assembleDebug testDebugUnitTest`
  passed, 384 tests. It is byte-identical to lor-iabt's `iabt-b3`. The base build passed 345 tests.

### Deviations

- **Undo in a second pass.** The Undo bar shows for 4 s (`SHOWN_MS` in `home/Undo.kt`), which is
  too short to visit the `lor` panel and the agent page first. So "leaves at once" was checked on one
  SQL-flagged task ("Return the library books"). Undo was checked on a second ("Approve the dentist
  reschedule"): Done, then Undo 2 s later.
- **The SQL flag doubles as the Done flag.** The test database's existing flag, "Renew the car
  insurance", is a Google task, which has no Done. The task flagged by SQL was therefore also the one
  marked Done.
- **Line 3 had to compare different layouts.** The line's own comparison, branch with the full
  layout against base with a parseable one, mixes the code's cost with the layout's. The third
  configuration (both builds, parseable layout) separates the two.

### Open items

- **Line 3 needs a decision.** It fails as written, and nothing on this branch was changed. The
  measurements say the cost is drawing `today`, `needs-you` and the `lor` panel, not a regression
  at an equal layout. Either the line is restated as the code's cost at an equal layout (+6.8% and
  +4.9%, which pass), or the blocks' first frame is made cheaper. That choice is the plan owner's.
- **Both checks predate 6f946a8.** That commit, from lor-iabt's review, changes when
  `agent/ThreadColumn.kt` reads. Line 2's `t3` wide-form steps ran before it. The home press doesn't
  draw the `t3` panel.
- **Test data afterwards.** "Return the library books" was set back to active by SQL. "Approve the
  dentist reschedule" is active and unflagged, as before, through the app's own Undo. The
  scratch-server approval thread "Approve the clgw e2e check" is completed.
