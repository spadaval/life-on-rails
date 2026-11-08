---
created_at: "2026-11-07T00:03:36.787508130+00:00"
id: "lor-sbth"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-sbth
priority: "P2"
relationships:
  blocks: []
  children:
  - kind: "issue"
    id: "lor-leh4"
  - kind: "issue"
    id: "lor-r0f9"
  - kind: "issue"
    id: "lor-vtmn"
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-kqhy"
    type: "related"
  - kind: "issue"
    id: "lor-ph30"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Widget and clock blocks"
updated_at: "2026-11-08T01:05:14.399174791+00:00"
---

## Goal

Home hosts Android widgets and a clock block. A `widget` block (decision lor-9wlx) holds any Android widget, added from the home menu through Android's bind and the widget's configure flows, drawn at its column's width and resized by its height, so Sumeet's Tesla widget can sit on home. The `clock` block shows the time, the date, the next event, the next alarm, and the weather from LoR's server (decision lor-g4ta, from lor-kqhy's choice of Google's Weather API).

## Done when

- [x] Each child is done, with its proof: lor-vtmn (the `widget` block, its edits and the id sweep), then lor-q18v (the weather on the server), lor-r0f9 (widgets on home) and lor-leh4 (the clock block), in that order on this item's branch.
- [x] End to end on `T3_Fold_API35` against the test server, in one session, from a version 1 `layout.json`: folded, the home menu adds the Clock app's clock widget and a list widget to home; home's stack shows the clock block with an event, an alarm and the weather, then both widgets, then the apps; an unfold shows them in home's flow (lor-j87u) with the widgets told their new size; a fold back shows them narrow with the same ids; `am force-stop` and a relaunch show them again. Each step screenshotted, the file read at the end. Recorded as an evidence receipt on this item.
- [ ] Launcher polish 7.8's home-press measurement on `T3_Phone_API35` with the two widgets on home, on this branch and on `main` at the commit it branched from (that build with no widgets), in one emulator session: the median `TotalTime` on this branch is within 10% of the base. Recorded in the same receipt or one beside it.

## Scope

The phone's home (`android/app/src/main/java/com/spadaval/lor/home/`, `net/DeviceHome.kt`, `agent/AgentModel.kt`), the server's `/device/home` and a new `src/server/weather.ts`, `layout-v2.md`, and the spec's Config list. Not: moving widgets between stacks or reordering blocks (lor-f3vn's minimap), LoR's own blocks (`today`, `needs-you`, `running`, `t3-queue`: lor-clgw), generative widgets.

## Context

- Builds on lor-eh0y (the grid) and lor-ph30 (the inner screen; lor-j87u's wide flow decides which column each block is drawn in).
- This item builds two pieces lor-clgw then reuses (review finding lor-1f2f): the process-scoped `/device/home` reader (lor-leh4) and the stack's overflow scroll, the blocks above the dock scrolling together over a fixed dock (lor-r0f9). lor-clgw waits for this item.
- The real Fold's checks (a widget on One UI, the Tesla widget, the bind consent) are the `needs-human` validation item filed with this plan, after merge; these emulator checks are its stand-in.
- Sumeet's setup, asked 2026-11-07: his Google key goes in the env file as `GOOGLE_MAPS_KEY` (lor-g4ta). lor-q18v's last line and lor-leh4's weather line need it.
- Environment: `T3_Phone_API35` and `T3_Fold_API35` against `scripts/ui-server.sh`, one emulator at a time; builds on boat (`android/boat-build.sh`).

## Notes

- 2026-11-07 [decision] Widget hosting borrows from Kvaesitso (GPL-3.0) and Launcher3 as Lawnchair carries it (Apache-2.0), read 2026-11-07 at their default branches. From Kvaesitso: one AppWidgetHost per process, listening while home is on screen (ui/base/AppWidgetHost.kt); the widget view made with the application context in an AndroidView and told its size with updateAppWidgetSize(SizeF) (launcher/widgets/external/AppWidgetHost.kt); the bind and configure flow, allocate, bindAppWidgetIdIfAllowed else ACTION_APPWIDGET_BIND, configure with MODE_BACKGROUND_ACTIVITY_START_ALLOWED on API 34, delete the id on any cancel (sheets/WidgetPickerSheet.kt); the height in dp at the column's width, resized between minResizeHeight and maxResizeHeight (sheets/ConfigureWidgetSheet.kt); and the note with Remove when a widget fails to load (launcher/widgets/external/AppWidget.kt). From Launcher3: host id 1024 and start/stopListening with the activity (widget/LauncherWidgetHolder.java); the sweep of ids the layout doesn't name (model/DatabaseHelper.java removeGhostWidgets); the long press on a widget that doesn't take its taps (CheckLongPressHelper.java); the reconfigurable check (WIDGET_FEATURE_RECONFIGURABLE). Each borrowed file names its source and licence in its header (T3 Launcher's D1: GPL code may be copied in a personal build).
  Reason: Kvaesitso's widget column is the strategy's nearest design to a stack: widgets at the column's width with a height in dp, no cell grid. Launcher3 is the reference for a host that survives restarts and doesn't leak ids. Neither's widget theming (Kvaesitso's Material You colour mapping) fits DESIGN.md, so widgets keep their own colours.
  Rejected: Launcher3's cell spans (spanX, spanY) for widget size: a stack has no cells, and its column changes width between the phone and the wide flow
  Rejected: Kvaesitso's colour mapping (setColorResources from a tonal palette): DESIGN.md has no Material You palette to map to
- 2026-11-07 [decision] Split into four children on this item's branch: lor-vtmn (schema, edits, sweep; pure Kotlin), lor-q18v (the server's weather; TypeScript only), lor-r0f9 (widgets on home), lor-leh4 (the clock block). The clock's next event stays LoR's /device/home events; the next alarm is AlarmManager.getNextAlarmClock, shown within 24 hours; the clock reads /device/home through one process reader shared with AgentModel.
  Reason: Each child is reviewable alone: the contract and its tests, a server change with its own test suites, and two screens with their own emulator proof. The server part has no Android in it and the widget part no server. LoR's events need no READ_CALENDAR permission and match the agent page and the wall; the alarm needs no permission. A clock away from the t3 panel would go stale on AgentModel.last alone, since only visible cells and their neighbours are composed.
  Rejected: One item: a single review over Kotlin parsing, the widget host, the clock and a server module
  Rejected: The phone's own calendar (CalendarContract) for the next event: a permission Sumeet must grant, and a second source of events beside LoR's
- 2026-11-07 [plan] Planned 2026-11-07: children lor-vtmn, lor-q18v, lor-r0f9, lor-leh4 on this item's branch; decisions lor-9wlx (the widget block) and lor-g4ta (the weather's delivery); real-Fold validation lor-13c1 after merge, outside the mission. Set to todo; a fresh agent reviews this plan before lor-vtmn starts, and lor-eh0y and lor-ph30 merge first. Sumeet's setup before lor-q18v's last line: GOOGLE_MAPS_KEY in the env file.
- 2026-11-07 [decision] lor-q18v is a standalone blocker of lor-sbth on its own branch, merging alone. Its live check needs GOOGLE_MAPS_KEY in the env file, which Sumeet sets.
  Reason: lor-q18v is server-only and doesn't depend on the inner screen, so it can run while lor-ph30 is built; as a child it would wait for lor-ph30.
- 2026-11-07 [decision] lor-leh4 is blocked_by lor-q18v instead of the whole increment. lor-vtmn and lor-r0f9 go first on lor-sbth's branch.
  Reason: lor-q18v waits for Sumeet's GOOGLE_MAPS_KEY; only the clock's weather line (lor-leh4) needs it, so the widget work shouldn't wait.
- 2026-11-07 [decision] Children are worked in order on branch lor-sbth: lor-vtmn, lor-r0f9, then lor-leh4 (which also waits for lor-q18v), each reviewed as its own slice.
  Reason: Atelier keeps a child open until its parent merges, so blocked_by between siblings stalls them.
- 2026-11-07 The child line is ticked on the children's records: lor-q18v is done, and every Done when line of lor-vtmn, lor-r0f9 and lor-leh4 is ticked. It is fully met when lor-sbth merges.

## Report

The end to end holds (evidence lor-sj8h). The home-press line does not hold as measured (evidence lor-55b4), so it is not ticked.

- **End to end** on `T3_Fold_API35`, in one session against the test server, starting from a version 1 file. Folded, the home menu added Clock's Digital (id 6) and Markor's todo list (id 7) through Android's bind dialog. Home's stack showed the clock block (the event, "Alarm 7:00", "63° Clear"), both widgets, then the apps. An unfold told both widgets 402 dp, and a fold back told them 419 dp again, with the same ids. After `am force-stop`, home came back in a new process with both widgets, and the sweep deleted nothing. The file at the end holds clock, widget 6, widget 7, apps and dock. Screenshots `01` to `11` and the final file are in `/tmp/claude-1000/sbth-emu/fold/`.
- **Home-press latency** on `T3_Phone_API35` (median `TotalTime`, ten runs after a warm-up with Settings in front). The base is d1d03d6 with no widgets; the head has the two widgets on home.

  | Round | Base | Head | Change |
  |---|---|---|---|
  | 1 | 69.0 ms | 62.0 ms | -10.1% |
  | 2 | 58.0 ms | 65.0 ms | +12.1% |

  Round 2 misses the 10% line. The 20 runs pooled give 61.5 ms against 63.0 ms (+2.4%), and single runs range from 29 to 122 ms. So the rounds don't separate the builds, but the line as written isn't met.

### Open items

- Line 3 needs a decision: more rounds, or accept the pooled median. I added no rounds after seeing the result.
- Unfolded, home with a page beside it is a half of the pair (lor-ph30), so the widgets sit in its narrow column rather than in lor-j87u's full-width two-column flow. lor-ctox showed the full-width flow with `wide` full.
- Markor isn't on the Fold. I installed it from F-Droid for the check and removed it afterwards.

