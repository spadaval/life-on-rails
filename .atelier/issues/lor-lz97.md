---
created_at: "2026-11-07T22:17:28.587363766+00:00"
id: "lor-lz97"
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
    id: "lor-odtk"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-p86v"
    type: "implements"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The lor panel and LoR's blocks on home"
updated_at: "2026-11-07T22:17:51.066018314+00:00"
---

## Goal

LoR shows on home in native Compose: the `lor` panel (LoR's dashboard shaped for the phone, with Tasks or People beside Today on the inner screen) and the `today`, `needs-you`, `running` and `t3-queue` blocks a stack can hold, as decision lor-p86v sets them. Read-only: nothing here writes to LoR.

## Done when

- [ ] `Layout.kt` parses the `lor` panel type (`at`, `wide` with default `full`, no other field) and the four block types (`type` alone), with the same errors as the other types (an unknown field names its place). JVM tests: each parses and round-trips through an edit and Undo; an extra field on each is an error naming its place; `lor` with no `wide` is full in lor-j87u's inner-screen rule, and `"wide": "half"` pairs with a half beside it. `Panel.TYPES` lists `lor`.
- [ ] `layout-v2.md` documents `lor` under Panels and the four blocks under Blocks as version 2 additions, as the decision states them; "New block types (…) are added by the issues that use them" names only `widget` (unless lor-sbth has landed it).
- [ ] The phone reads `/device/home`'s `agent_queue`, `/device/tasks` and `/device/people` (`LorClient`, `DeviceHome` and two new parsed types beside it), with JVM tests parsing a recorded reply of each, made from the server's `pnpm test:db` fixtures.
- [ ] One process-scoped LoR reader replaces `AgentModel`'s own read: it reads `/device/home` every minute while any panel or block showing it is composed and home is started, once however many show, and `/device/tasks` and `/device/people` only while a `lor` panel is composed. The agent page, the blocks and the `lor` panel draw from it, keep the last good reply greyed with its time when LoR can't be read (launcher polish 3.3's rule), and home's header still reads its events. A block or panel showing T3 holds the T3 connection while composed, as the agent page does. Shown by a `LorReads` log line per read: with the agent page, a `today` block and a `lor` panel all composed, one `/device/home` read a minute.
- [ ] The four blocks, on `T3_Phone_API35` against the test server and the scratch T3 server, in a stack of `clock`, `today`, `running`, `needs-you`, `t3-queue`, `apps` and `dock`: each draws what the decision says, with test data put in `life_on_rails_test` (by SQL or the test server's own screens, never the `lor` CLI, which reaches the real server) that gives each something to show (a running timer, two running T3 turns, an approval and a question in T3 and two flagged tasks in LoR, a queued and a claimed agent task). `needs-you` shows 3 cards and "2 more", whose tap moves the grid to the `t3` panel; its approve acts as the agent page's does. The stack's blocks above the dock scroll together when they overflow and report `scrollsInGrid`, the dock staying at the bottom. Screenshots.
- [ ] The `lor` panel's narrow form on `T3_Phone_API35`: Today, Needs you with the agent's queue under it, and Due soon (six tasks and "All N tasks", which opens the dashboard's Tasks). Its wide form on the unfolded `T3_Fold_API35`: that column without Due soon on the left; on the right the Tasks and People tabs, starting on Tasks, each column scrolling on its own and reporting `scrollsInGrid`. A tab switch writes nothing to `layout.json` (its modified time is unchanged) and survives a fold and unfold. Screenshots of each form and tab.
- [ ] Against the web: `node scripts/ui-check.mjs today`, `tasks` and `people` on the same test server, beside the emulator's screenshots of the `lor` panel. The same flagged tasks, open tasks in the same order with the same due, waiting and project lines, and the same people past their cadence and birthdays show on both; the handoff lists every difference with its reason.
- [ ] Every row opens its place and changes nothing: a task or flag opens the dashboard at `/tasks?id=`, a person at `/people?id=`, an agent task with a thread opens the T3 app at that thread and one without opens it at `/tasks?id=` (the launch line in logcat each time). `LorClient`'s only `POST`s are still `timer`, `ingest` and the WhatsApp backup.
- [ ] The spec's "The phone" section describes the `lor` panel and the four blocks, and that the agent page shares its LoR read with them.
- [ ] At font scale 2.0 on `T3_Phone_API35`, the blocks' stack and the `lor` panel cut no text mid-line and overlap nothing (screenshots).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/Layout.kt` and its tests, `home/HomeScreen.kt` (`StackPanel` drawing the new blocks, and the `lor` panel in `GridCells`), `agent/AgentPage.kt` and `agent/AgentModel.kt` (Today, Running and Needs you split into composables the blocks reuse; the LoR read moved out), a new `lor/` package for the panel and the reader, `net/LorClient.kt` and `net/DeviceHome.kt`, `docs/missions/launcher-panels/layout-v2.md`, and the spec's "The phone" section. Not the T3 panel's wide form (the next child), not widgets or the clock block (lor-sbth), not the minimap's palette (lor-f3vn).

## Context

- Builds on lor-ph30 as merged: the `wide` field and the inner-screen rules (lor-j87u), the panels' forms by width (lor-el51: narrow under 600 dp), and F4's process-scoped card actions (lor-ozj2), which the `needs-you` block uses as the agent page does.
- lor-sbth's clock block (lor-leh4, planned at the same time as this item) also needs a shared process-scoped `/device/home` reader. Whichever lands first builds it; the other extends that one reader (here with `/device/tasks`, `/device/people` and the lor panel's visibility) rather than adding a second.
- lor-sbth works in the same `Blocks` section of `layout-v2.md` and in `StackPanel`; whichever lands second merges the other's lines.
- The agent page's sections today: `Today`, `TodayBody` and `DayArc`, `RunningRow`, `T3QueueCard` and `QueueCard`, `AgentStates.notes`, all in `agent/`.
- `DESIGN.md` governs the look: section heads as the agent page's, the selected tab as the selection row's segment, nothing animating on its own. The day arc is drawn once per read.
- Environment: `T3_Phone_API35` and `T3_Fold_API35` (one at a time) against `scripts/ui-server.sh` (`http://10.0.2.2:3857`) and a scratch T3 server (`docs/missions/t3-launcher/plan.md`, "The scratch T3 server"). Builds on boat.

## Notes

- 2026-11-07 [decision] One process-scoped LoR reader serves the agent page, the blocks and the lor panel: /device/home every minute while anything showing it is composed and home is started, /device/tasks and /device/people only while a lor panel is composed.
  Reason: With LoR on several cells, a read per composable would multiply the same request; one reader keeps one read a minute and one last-good reply, which the agent page already keeps per process (AgentModel.last). The T3 connection is already shared and counted by holds, so blocks hold it as the agent page does.
  Rejected: An AgentModel per block or panel: the same /device/home read once per composable each minute
