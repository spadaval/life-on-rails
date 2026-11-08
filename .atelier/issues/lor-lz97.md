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
status: "in_progress"
title: "The lor panel and LoR's blocks on home"
updated_at: "2026-11-08T04:07:26.795908358+00:00"
---

## Goal

LoR shows on home in native Compose: the `lor` panel (LoR's dashboard shaped for the phone, with Tasks or People beside Today on the inner screen) and the `today`, `needs-you`, `running` and `t3-queue` blocks a stack can hold, as decision lor-p86v sets them. This item draws and reads; the next child on this branch, lor-rz50, adds the writes (Sumeet's yes, 2026-11-07).

## Done when

- [ ] `Layout.kt` parses the `lor` panel type (`at`, `wide` with default `full`, no other field) and the four block types (`type` alone), with the same errors as the other types (an unknown field names its place). JVM tests: each parses and round-trips through an edit and Undo; an extra field on each is an error naming its place; `lor` with no `wide` is full in lor-j87u's inner-screen rule, and `"wide": "half"` pairs with a half beside it. `Panel.TYPES` lists `lor`.
- [ ] `layout-v2.md` documents `lor` under Panels and the four blocks under Blocks as version 2 additions, as the decision states them; "New block types (…) are added by the issues that use them" no longer lists a type that exists (lor-sbth has added `widget`).
- [ ] The phone reads `/device/home`'s `agent_queue`, `/device/tasks` and `/device/people` (`LorClient`, `DeviceHome` and two new parsed types beside it), with JVM tests parsing a recorded reply of each, made from the server's `pnpm test:db` fixtures.
- [ ] The agent page, the blocks and the `lor` panel read LoR through the one process-scoped `/device/home` reader that lor-leh4 builds, with no second reader: this item extends it to read `/device/tasks` and `/device/people` only while a `lor` panel is composed and home is started, every minute. All of them draw from it and keep the last good reply greyed with its time when LoR can't be read (launcher polish 3.3's rule). A block or panel showing T3 holds the T3 connection while composed, as the agent page does. Shown by the reader's log line per read: with the agent page, a `today` block and a `lor` panel all composed, one `/device/home` read a minute, and one `/device/tasks` and one `/device/people` read a minute only while the `lor` panel is composed.
- [ ] The four blocks, on `T3_Phone_API35` against the test server and the scratch T3 server, in a stack of `clock`, `today`, `running`, `needs-you`, `t3-queue`, `apps` and `dock`: each draws what the decision says, with test data put in `life_on_rails_test` (by SQL or the test server's own screens, never the `lor` CLI, which reaches the real server) that gives each something to show (a running timer, two running T3 turns, an approval and a question in T3 and two flagged tasks in LoR, a queued and a claimed agent task). `needs-you` shows 3 cards and "2 more", whose tap moves the grid to the `t3` panel; its approve acts as the agent page's does. The stack overflows into lor-r0f9's scroll (the blocks above the dock scroll together, the dock stays at the bottom, `scrollsInGrid` at the edges), reused as it is. Screenshots.
- [ ] The `lor` panel's narrow form on `T3_Phone_API35`: Today, Needs you with the agent's queue under it, and Due soon (six tasks and "All N tasks", which opens the dashboard's Tasks). Its wide form on the unfolded `T3_Fold_API35`: that column without Due soon on the left; on the right the Tasks and People tabs, starting on Tasks, each column scrolling on its own and reporting `scrollsInGrid`. A tab switch writes nothing to `layout.json` (its modified time is unchanged) and survives a fold and unfold. Screenshots of each form and tab.
- [ ] Against the web, on the same data the emulator shows: plain page screenshots of the running test server (`scripts/ui-server.sh` at `:3857`) of `/`, `/tasks`, `/people?filter=overdue` and `/people?filter=birthdays`, taken in the system Chrome (for example `playwright-core` or Chrome's `--headless --screenshot`), without running `scripts/ui-check.mjs`'s flows, which wipe and reseed the test database and click through edits. Beside the emulator's screenshots of the `lor` panel: the same flagged tasks, open tasks in the same order with the same due, waiting and project lines, and the same people past their cadence and birthdays show on both; the handoff lists every difference with its reason.
- [ ] A tap on a row's text opens its place: a task or flag opens the dashboard at `/tasks?id=`, a person at `/people?id=`, an agent task with a thread opens the T3 app at that thread and one without opens the dashboard at `/tasks?id=` (the launch line in logcat each time). Rows have no write controls yet (lor-rz50 adds them).
- [ ] The spec's "The phone" section describes the `lor` panel and the four blocks, and that the agent page shares its LoR read with them.
- [ ] At font scale 2.0 on `T3_Phone_API35`, the blocks' stack and the `lor` panel cut no text mid-line and overlap nothing (screenshots).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/Layout.kt` and its tests, `home/HomeScreen.kt` (`StackPanel` drawing the new blocks, and the `lor` panel in `GridCells`), `agent/AgentPage.kt` and `agent/AgentModel.kt` (Today, Running and Needs you split into composables the blocks reuse), the shared reader lor-leh4 built (extended, not replaced), a new `lor/` package for the panel and the reader, `net/LorClient.kt` and `net/DeviceHome.kt`, `docs/missions/launcher-panels/layout-v2.md`, and the spec's "The phone" section. Not the T3 panel's wide form (the next child), not widgets or the clock block (lor-sbth), not the minimap's palette (lor-f3vn).

## Context

- Builds on lor-ph30 as merged: the `wide` field and the inner-screen rules (lor-j87u), the panels' forms by width (lor-el51: narrow under 600 dp), and F4's process-scoped card actions (lor-ozj2), which the `needs-you` block uses as the agent page does.
- This item waits on lor-sbth (decision lor-tbqc) and reuses two of its pieces: the shared process-scoped `/device/home` reader from lor-leh4, which this item extends with `/device/tasks`, `/device/people` and the `lor` panel's visibility, and the stack's overflow scroll from lor-r0f9. It builds neither again.
- lor-sbth, merged before this item, adds `widget` and the clock block to the same `Blocks` section of `layout-v2.md` and to `StackPanel`; this item adds its blocks beside them.
- The agent page's sections today: `Today`, `TodayBody` and `DayArc`, `RunningRow`, `T3QueueCard` and `QueueCard`, `AgentStates.notes`, all in `agent/`.
- `DESIGN.md` governs the look: section heads as the agent page's, the selected tab as the selection row's segment, nothing animating on its own. The day arc is drawn once per read.
- Environment: `T3_Phone_API35` and `T3_Fold_API35` (one at a time) against `scripts/ui-server.sh` (`http://10.0.2.2:3857`) and a scratch T3 server (`docs/missions/t3-launcher/plan.md`, "The scratch T3 server"). Builds on boat.

## Notes

- 2026-11-07 [decision] One process-scoped LoR reader serves the agent page, the blocks and the lor panel: /device/home every minute while anything showing it is composed and home is started, /device/tasks and /device/people only while a lor panel is composed.
  Reason: With LoR on several cells, a read per composable would multiply the same request; one reader keeps one read a minute and one last-good reply, which the agent page already keeps per process (AgentModel.last). The T3 connection is already shared and counted by holds, so blocks hold it as the agent page does.
  Rejected: An AgentModel per block or panel: the same /device/home read once per composable each minute
- 2026-11-07 [decision] Superseded in part by lor-tbqc: the shared /device/home reader is lor-leh4's (lor-sbth), which this item extends with /device/tasks and /device/people, and the stack's overflow scroll is lor-r0f9's; lor-clgw waits on lor-sbth.
  Reason: Review finding R1 (lor-tbqc): one owner for each piece, decided by the coordinator.
- 2026-11-07 [note] From lor-f3vn's plan: if lor-v692 is on main when this starts, add lor to its addPanel as a once-per-layout type, like t3 and drawer, and the four blocks as types that take no field, with GridEditTest covering them; and if lor-6get and lor-9uc1 are on main, name them in the minimap's palettes (LoR; Today, Needs you, Running, Agent queue), with a screenshot of each palette. If those land after this, they do it.
- 2026-11-07 [decision] The shared reader (LorReads) holds three kept replies, /device/home, /device/tasks and /device/people, each with its own minute; hold(lists = true) asks for the two lists, and a second count of such holds decides whether they are read. A holder asking for the lists wakes the loop, so the lists are read at once while /device/home keeps its minute.
  Reason: lor-leh4's author suggested a hold that says what it wants. Separate minutes keep each read at once a minute without reading /device/home again when a lor panel comes; one Kept type gives each reply the same last-good, error and current rules.
  Rejected: A second reader for the lists: two owners of LoR's reads, against decision lor-tbqc
  Rejected: Reading the lists with every /device/home read: the panel would wait up to a minute for its first list
- 2026-11-07 [decision] Each block and the lor panel make their own AgentModel (rememberAgentModel), which holds the process's reader and, for needs-you and running only, the T3 connection; today, t3-queue and the lor panel hold no T3 connection.
  Reason: AgentModel is light (the reader, connection and card calls are all process-wide), so one per screen keeps the agent page's code path; holding T3 only where T3 is shown keeps a LoR-only stack from opening the socket.
  Rejected: One AgentModel shared by home: a new owner in HomeScreen for state that already lives in LorApp
- 2026-11-07 [decision] The lor panel's Tasks tab lists /device/tasks' open tasks grouped as the web's Tasks groups by due date (Needs you, Past due, Today, This week, Later on, No date), each row titled with a line of its due label, who it waits on, its project and Google Tasks; Due soon writes the web's Due soon line (due 15 Oct · waiting on X · project · Google Tasks). Dates are written in en-GB as the web's Intl does (Sept, not Sep), whatever the phone's locale.
  Reason: The check is against the web on the same data; the web's /tasks groups by due date by default, and /device/tasks carries no assignee, so the web's Open, Waiting and With the agent tabs can't be split on the phone.
  Rejected: One flat list in Due soon's order: matches no screen of the web's Tasks
- 2026-11-07 [decision] LoR's blocks are drawn as cards (framedPanel on StateFill) over the wallpaper; the lor panel has the agent page's veil, and home's page dots use the surface ink on it as on the agent page. The tab segment is DESIGN.md's segmented control with the selection treatment.
  Reason: The agent page's sections assume a dark surface; on a bright wallpaper the blocks' text would not read without one.
- 2026-11-07 [decision] A LoR flag card's text opens its task in the dashboard, as its tile and Open do; T3 cards' text taps are left for lor-iabt, which makes them choose the thread on the wide form.
  Reason: The Done when line asks a flag's text to open /tasks?id=; giving T3 cards a text tap now would be undone by lor-iabt.
