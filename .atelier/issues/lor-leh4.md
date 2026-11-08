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
updated_at: "2026-11-08T03:14:19.125623051+00:00"
---

## Goal

The clock block shows the time, the date, the next event, the next alarm and the weather. It is today's header (`look.header`'s style, the next event from LoR's calendar through `/device/home`) with two new lines: the next alarm from Android, and the weather from LoR's server (decision lor-g4ta). It reads `/device/home` itself while on screen, so it stays current wherever the clock sits on the grid, not only beside the agent page.

## Done when

On `T3_Phone_API35` against the test server, unless a line says otherwise:
- [x] `DeviceHome.parse` reads `weather` (null or lor-g4ta's object) into `DeviceHome.Weather`. JVM tests: the object, null, and a reply without the field (an older server) as null.
- [x] The next alarm: `AlarmManager.getNextAlarmClock()`, read again on `ACTION_NEXT_ALARM_CLOCK_CHANGED` while home is started, shown as "Alarm 7:00" (in the 24-hour clock home uses) when it rings within the next 24 hours, else not shown. A tap on the line opens the alarm list (`AlarmClock.ACTION_SHOW_ALARMS`). Set with `am start -a android.intent.action.SET_ALARM` and its extras, screenshotted; an alarm two days away shows no line. A pure function picks the line, with JVM tests for under, at and over 24 hours and for none.
- [x] The weather line: "18° Partly cloudy", in Fahrenheit when the locale's measurement system is US (`android.icu.util.LocaleData.getMeasurementSystem`), else Celsius, rounded to whole degrees; not shown when `weather` is null or its `observed_at` is more than 2 hours old. A pure function formats it, with JVM tests for both unit systems, rounding, null and a stale reading.
- [x] Both lines are drawn in each `look.header` style that draws a header (date and clock) and in neither under `none`, in the header's type and the wallpaper's ink, with DESIGN.md's text shadow on light ink. The block's TalkBack description adds them after the date and event. Screenshots of both styles with an event, an alarm and the weather, on the light and the dark wallpaper.
- [x] The clock reads `/device/home` while it is composed and home is started, no more than once a minute, through one reader in process scope that the agent page's `AgentModel` also uses: with both on screen, one `/device/home` read a minute happens, not two (a `LorReads` log line per read); with only the clock in the composition window (a test layout with the clock on a stack two cells from the `t3` panel), it still updates within a minute of a change on the server (an event added to the test database).
- [x] The weather line shows the weather the test server returns for the `location` observation lor-q18v inserted (the screenshot beside the `curl` of `/device/home` at the same minute).
- [x] At font scale 2.0 (`settings put system font_scale 2.0`), on `T3_Phone_API35`, home's stack with the clock block showing all five parts and two widgets is screenshotted: no line of the clock is cut mid-line or drawn over another element, a long event title ends in an ellipsis, and the stack scrolls rather than clipping (lor-r0f9's overflow scroll).
- [x] On the unfolded `T3_Fold_API35`, home alone at full width draws the clock block in its flow column (lor-j87u) with all five parts, screenshotted.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

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
- 2026-11-07 [decision] The shared reader is LorReads (net/LorReads.kt), one per process as LorApp.reads. Whatever shows LoR calls hold() while composed and home is started; while anything holds it, it reads once a minute (at once if the last read began a minute or more ago), and stops when the last holder lets go. It keeps home, error, current and retrying as Compose state; AgentModel's lor, lorError, lorRead and retrying now read it, and AgentModel.last is gone. lor-lz97 extends it with its own hold for /device/tasks and /device/people.
  Reason: One owner for the reply and the schedule, so the agent page and the clock cause one read between them and a clock away from t3 stays current. Holding by count needs no knowledge of which screens exist. 'current' replaces 'a read ended since the page opened': with a shared reader the clock may have read 20 s before the page opens, and that reply should not show greyed.
  Rejected: Each holder running its own minute loop with a shared result: two reads a minute with both on screen
  Rejected: Keeping AgentModel.last as a companion StateFlow for the clock: it is set only while the agent page is composed
- 2026-11-07 [decision] The alarm and weather lines sit under the event, flush with the date, without a mark, in the event line's type (Saira 15 sp, the wallpaper's ink and shadow), one line each with an ellipsis. Only the alarm line takes its own tap (the alarm list, through a new HomeActions.alarms()); the rest of the block still opens the calendar. TalkBack gets the alarm as a custom action 'Open alarms' on the block's one node.
  Reason: DESIGN.md gives a mark to the calendar only (the cyan diamond); a mark per line would be decoration. The block is one TalkBack node (clearAndSetSemantics), so the alarm's tap needs a custom action to stay reachable.
  Rejected: One combined line 'Alarm 7:00 · 46° Cloudy': the item names two lines
- 2026-11-07 [decision] Edges of the lines: an alarm exactly 24 hours away shows and one already past does not; a reading exactly 2 hours old shows. Degrees round half up (Math.round), Fahrenheit computed from the Celsius the server sends. The unit follows LocaleData.getMeasurementSystem of the default FORMAT locale, read once per composition of the block.
  Reason: 'Within the next 24 hours' read as inclusive; the JVM tests in HeaderTest pin each edge.
- 2026-11-07 [handoff] Implemented on lor-sbth (44252ef code, f72df4c records); every Done when line ticked, see ## Report. Review: none yet. lor-lz97 extends LorApp.reads (LorReads: hold(), retry(), home/error/current/retrying).
- 2026-11-07 [handoff] Page-dot fix added at the coordinator's request: 8d301b6 (HomeScreen.kt StackScroll/FillColumn), receipt lor-moup, Report's Surprises updated. Review: none yet; the slice to review is now e9d358a..8d301b6.

## Report

Every Done when line holds on `lor-sbth` at 44252ef. The clock block shows "Alarm 7:00" from `AlarmManager` within 24 hours (tap: the alarm list) and "46° Cloudy" from `/device/home`'s `weather`, and reads LoR through `LorReads`, the process's one `/device/home` reader, which the agent page also uses. JVM tests on boat (`assembleDebug testDebugUnitTest`, BUILD SUCCESSFUL, all suites 0 failures): `DeviceHomeTest.readsTheWeather` and `noWeatherIsNull` (object, null, older server without the field); `HeaderTest.alarmWithinADay` (under, exactly at and a minute over 24 hours, two days, none, past); `HeaderTest.weatherInEachUnit`, `weatherRoundsToWholeDegrees`, `noOrStaleWeatherIsNotShown`; `LorReadsTest` (when the next read is due). On the emulators, receipts lor-pnlz (alarm: none at two days, shown at 7:00, tap opens the alarm list), lor-ap2z (one read a minute with the agent page and the clock both holding; the clock alone two cells from `t3` picked up a new event 49 s after it was inserted), lor-2p20 (clock and date styles in light and dark ink, nothing under `none`), lor-ipzv (the weather beside the `curl` at the same minute), lor-8qz3 (font scale 2.0 with two widgets: no clock line cut, long title ellipsed, the stack scrolls) and lor-yia7 (the unfolded Fold's flow column with all five parts). The spec's `GET /device/home` line now lists `weather` (finding lor-0ri5), and its home-header sentence says where the event, weather and alarm come from.

### Decisions

Three decision notes above: the reader's shape (`LorReads`, `hold()`, `current`), where the two lines sit and how the alarm's tap reaches TalkBack, and the edges of the 24-hour and 2-hour rules.

### Surprises

- At font scale 2.0 with the stack scrolled to its foot, home's page dots were drawn over the `Settings` label of the last app row (14-font2-tall-scrolled.png): lor-r0f9's scroll ran under the dots, which sit 96 dp above the bars. Fixed here at the coordinator's request (8d301b6): `StackScroll` ends overflowing content with the room the dots need beyond what lies below it (the dock), plus 8 dp, so the last row scrolls clear; a stack that fits measures as before (`FillColumn` adds the pad only when its content exceeds its minimum). Receipt lor-moup: at 2.0 the label ends clear of the dots, and at 1.0 the dots and dock sit on the same pixel rows as before the fix. Proof is the emulator only; `FillColumn`'s measure has no JVM test.
- On the emulator two clock apps (Google Clock, Fossify Clock) both answer `ACTION_SHOW_ALARMS`, so the alarm line's tap opens Android's chooser first. A phone with one clock app goes straight to its list.
- `DISMISS_ALARM` by label removed an alarm within 24 hours but not the Monday one; that one was switched off by hand in Clock.

### Open items

- For lor-lz97: `LorApp.reads` is `LorReads`. Its API: `suspend fun hold()` (hold while the caller runs; cancel to let go), `fun retry()`, and the Compose state `home: DeviceHome?`, `error: String?`, `current: Boolean`, `retrying: Boolean`; `LorReads.dueIn(lastStart, now)` is the pure schedule. Extending it for `/device/tasks` and `/device/people` means a second hold count (or a hold with what it wants) so those reads run only while a `lor` panel holds them.
- A retry pressed while the minute loop is about to read can cause two reads back to back (the loop doesn't re-check the time after waiting on the lock). Harmless; not fixed.
- Review: none yet.
