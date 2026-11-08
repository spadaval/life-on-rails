---
created_at: "2026-11-07T22:17:52.305139416+00:00"
id: "lor-leh4"
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
    id: "lor-g4ta"
    type: "implements"
  - kind: "issue"
    id: "lor-q18v"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The clock block: next alarm and weather"
updated_at: "2026-11-08T02:37:17.388724830+00:00"
---

## Goal

The clock block shows the time, the date, the next event, the next alarm and the weather. It is today's header (`look.header`'s style, the next event from LoR's calendar through `/device/home`) with two new lines: the next alarm from Android, and the weather from LoR's server (decision lor-g4ta). It reads `/device/home` itself while on screen, so it stays current wherever the clock sits on the grid, not only beside the agent page.

## Done when

On `T3_Phone_API35` against the test server, unless a line says otherwise:
- [ ] `DeviceHome.parse` reads `weather` (null or lor-g4ta's object) into `DeviceHome.Weather`. JVM tests: the object, null, and a reply without the field (an older server) as null.
- [ ] The next alarm: `AlarmManager.getNextAlarmClock()`, read again on `ACTION_NEXT_ALARM_CLOCK_CHANGED` while home is started, shown as "Alarm 7:00" (in the 24-hour clock home uses) when it rings within the next 24 hours, else not shown. A tap on the line opens the alarm list (`AlarmClock.ACTION_SHOW_ALARMS`). Set with `am start -a android.intent.action.SET_ALARM` and its extras, screenshotted; an alarm two days away shows no line. A pure function picks the line, with JVM tests for under, at and over 24 hours and for none.
- [ ] The weather line: "18° Partly cloudy", in Fahrenheit when the locale's measurement system is US (`android.icu.util.LocaleData.getMeasurementSystem`), else Celsius, rounded to whole degrees; not shown when `weather` is null or its `observed_at` is more than 2 hours old. A pure function formats it, with JVM tests for both unit systems, rounding, null and a stale reading.
- [ ] Both lines are drawn in each `look.header` style that draws a header (date and clock) and in neither under `none`, in the header's type and the wallpaper's ink, with DESIGN.md's text shadow on light ink. The block's TalkBack description adds them after the date and event. Screenshots of both styles with an event, an alarm and the weather, on the light and the dark wallpaper.
- [ ] The clock reads `/device/home` while it is composed and home is started, no more than once a minute, through one reader in process scope that the agent page's `AgentModel` also uses: with both on screen, one `/device/home` read a minute happens, not two (a `LorReads` log line per read); with only the clock in the composition window (a test layout with the clock on a stack two cells from the `t3` panel), it still updates within a minute of a change on the server (an event added to the test database).
- [ ] The weather line shows the weather the test server returns for the `location` observation lor-q18v inserted (the screenshot beside the `curl` of `/device/home` at the same minute).
- [ ] At font scale 2.0 (`settings put system font_scale 2.0`), on `T3_Phone_API35`, home's stack with the clock block showing all five parts and two widgets is screenshotted: no line of the clock is cut mid-line or drawn over another element, a long event title ends in an ellipsis, and the stack scrolls rather than clipping (lor-r0f9's overflow scroll).
- [ ] On the unfolded `T3_Fold_API35`, home alone at full width draws the clock block in its flow column (lor-j87u) with all five parts, screenshotted.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Header.kt` (the clock block), `net/DeviceHome.kt`, `agent/AgentModel.kt` (the shared reader), and their JVM tests. Not the weather's server side (lor-q18v), not a new block field: the `clock` block keeps no fields.

## Context

- Today the header reads `AgentModel.last`, which is set only while the agent page is composed and reading, so a clock away from the `t3` panel would go stale; that is why the read moves to a shared reader.
- The next event stays LoR's (`/device/home`'s `day.events`): Sumeet's calendars already reach LoR, the agent page and the wall use the same events, and reading the phone's own calendar would need `READ_CALENDAR`, a permission Sumeet would grant. The next alarm needs no permission.
- This item builds the process-scoped reader (review finding lor-1f2f). lor-clgw waits for lor-sbth, and lor-lz97 extends this reader for its blocks and the `lor` panel (`/device/tasks`, `/device/people`) rather than adding a second.
- The weather line needs `GOOGLE_MAPS_KEY` in the env file, as lor-q18v's last line does (Sumeet's setup).
- `layout-v2.md`'s `clock` line is rewritten by lor-vtmn.

## Notes

- 2026-11-07 [note] From lor-q18v's review (finding lor-0ri5): docs/v2-product-engineering-spec.md's GET /device/home line (about line 516) doesn't list the new weather field. lor-leh4 adds it: 'and weather, Google's current conditions at the phone's latest location fix (lor-g4ta), or null'.
