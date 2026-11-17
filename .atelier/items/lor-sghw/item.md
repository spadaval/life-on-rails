---
schema: atelier.item
schema_version: 3
id: lor-sghw
title: 'Text stops shimmering when it scales: TextMotion.Animated everywhere'
status: approved
mission: lor-ncpw
labels:
- launcher
- feature
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A43848bbc-dbe3-4d7a-a236-b2a58c5c0171
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A1af09095-d82d-457d-b850-94578b07cb64
- thread:delegated-task:command%3Amcp%3Ad505dd17-c64d-476e-8d30-f41010a20b4c%3Adelegate-task%3A681708bb-a370-4064-a18f-cde47a85b4ee
ready:
  by: conversion
  at: 2026-11-08T06:02:35.052190583Z
trivial: null
caused_by: null
created_at: 2026-11-08T06:02:35.052190583Z
---

## Goal

Text in the app stops shimmering when it scales, and lays out as the browser will draw it: every text style the app sets uses `TextMotion.Animated` (decision lor-mc37). Today a pressed tile's label, a folder opening and the drawer's predictive back re-hint the text at each in-between size, and glyph widths jump.

## Done when

- [x] Every `TextStyle(` under `android/app/src/main` sets `textMotion = TextMotion.Animated`, shown by a grep listing each with its line. On 2026-11-08 they are `Type`'s seven in `ui/Look.kt`, home's `labelStyle` in `home/HomeScreen.kt`, the header's four in `home/Header.kt` (the line, the clock and both dates), and the LoR panel's one in `lor/LorPanel.kt`. Material's typography in `Look.kt` copies `Type`, so it follows.
- [x] On `T3_Phone_API35` against the test server, home's first page and the drawer screenshotted before and after: no label newly cut by its ellipsis, and the header and labels up to about 4% wider (Sumeet accepted the measured 0.8% to 3.5% on 2026-11-17). Both pairs recorded as an evidence receipt on this item.
- [x] On the emulator with animations slowed (`settings put global animator_duration_scale 10`), before and after: a tile pressed and held, a folder opened and closed, and a predictive back on the drawer, each captured as a run of frames (`adb exec-out screencap -p` in a loop, or `screenrecord --bit-rate 20000000`). In every frame where the labels can be measured, a label's measured width divided by that frame's scale stays within 1 px of its width at rest after the change; before, the jumps are larger. The Report names the frames that could not be measured, and why. The widths per frame, before and after, recorded as an evidence receipt.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`android/app/src/main/java/com/spadaval/lor/ui/Look.kt`, `home/HomeScreen.kt`'s `labelStyle`, `home/Header.kt`'s styles and `lor/LorPanel.kt`'s. Not the web editor.

## Context

- Measurements and the reasoning are on the decision. The real Fold check is the `needs-human` loose end lor-s42k after merge (sharpness at rest, no shimmer on press, folder and back).
- Panels added one more since this was written: `lor/LorPanel.kt` builds a `TextStyle` of its own (2026-11-08). The grep in the first Done when line finds every one, whatever the list missed.

## Report

Scaled text no longer shimmers. During a press, a folder opening and the drawer's predictive back, every label's width tracks the frame's scale to within 0.74 px in every frame where it could be measured; before the change it jumped by up to 1.22 px. The folder's smallest frames, below about half size, could not be measured (below). The cost is at rest: home's labels are 0.8% to 3.5% wider by their layout rects, and no label or header line is cut by its ellipsis, now checked with the header showing an event and the weather.

**Done-when line 2 was amended on 2026-11-17.** It allowed the header and labels to grow "at most about 1%". The measured growth is 0.8% to 3.5%, and it comes from `TextMotion.Animated` itself, so meeting 1% would mean revisiting decision lor-mc37. Sumeet accepted the measured widths on 2026-11-17, and the line now allows "up to about 4% wider".

**Done-when line 3 was amended on 2026-11-17, on the word of the lead of mission lor-ncpw.** It asked for the bound "in each frame". The folder's smallest frames cannot be measured: the labels there are 17 to 40 px on screen, so a 0.5 px edge error becomes more than 1 px once divided by the scale, and they lie over home's Settings disc and "Google" label. The lead set one bounded attempt, about an hour, to recover the frames the tracker dropped, and accepted this amendment if frames were still left. The line now holds "in every frame where the labels can be measured", and the frames that could not be are named below.

**The change** (d6fc94a; taken over after the first implementer's session hit the 4 GB memory cap while measuring). Every `TextStyle(` under `android/app/src/main` sets `textMotion = TextMotion.Animated`. `grep -rn "TextStyle(" android/app/src/main` lists 13, and each has it:

- `ui/Look.kt:114-120`: `Type`'s seven (display, headline, title, body, bodySmall, label, button). Every Material `Typography` slot (`Look.kt:146-152`) is `Type.x` or `Type.x.copy(...)`, so it carries the setting.
- `home/HomeScreen.kt:182`: home's `labelStyle`.
- `home/Header.kt:193, 204, 205, 207`: the line, the clock, and both dates.
- `lor/LorPanel.kt:447`: the panel's link.

**Builds.** `android/boat-build.sh` (`assembleDebug testDebugUnitTest`) at this branch's head 65049a9 passes, with 422 tests, 0 failures, 0 errors and 0 skipped (`boat-head/build.log` and its test XML). Its APK is byte-identical (sha256 `12957ee0...`) to the "after" APK that was measured. The "before" APK (`d5fc326c...`) was built on boat from c2b5651, this branch's base. Both builds predate lor-3hnk (Kotlin 2.3.20), which merged to main after this branch started. The commits since 65049a9 change only this item's directory.

**Evidence**, three directories under `/tmp/lor-verify/`, each with a `README.txt`:

- `t3-69c6bc16-016824d8bc373a35/sghw-static/`: the static pair (Done-when 2), redone in round 2 against a working test server.
- `t3-894fb3ee-528162b46c30b0ed/sghw/`: the motion runs (Done-when 3), the boat build, and round 1's static pair, whose header shows the date alone because the app did not reach the server. That pair is superseded.
- `t3-69c6bc16-016824d8bc373a35/sghw-folder/`: the folder frames recovered in round 2.

**Static pair (Done-when 2).** The test server was `verify-lor up` from a clean checkout of main 336865b, with `LOR_FAKE_NOW=2026-11-17T09:00`, loaded by `pnpm demo:test`. Both APKs are the files measured in round 1 (their sha256 matches, `install.json`). One script (`static.sh`) installed each and captured it back to back on `T3_Phone_API35` on the Mac. It saved home's first page, the drawer's All tab, their accessibility snapshots and logcat. Both runs' logcat has `LorReads: read /device/home: ok`. Both headers read "Tuesday 17 November. Lunch with Priya at 12:30. 47° Partly cloudy".

- Nothing is cut by an ellipsis, on home or in the drawer, and that includes the header's event and weather lines (`home-pair.png`, `drawer-pair.png`). The longest home label, "Life on Rails", is 178 px in a 270 px cell. The event line is 429 px in a 970 px header.
- Layout widths from the snapshots, in whole px (`widths.txt`): the header 793 to 801 (+1.0%). On home, Phone +2.2%, Messages +3.5%, Chrome +1.8%, Camera +1.8%, Gmail +1.2%, Calendar +1.6%, Maps +2.5%, Photos +2.0%, YouTube +0.8%, Life on Rails +1.7%, Settings +3.4%. The drawer's rects grow +1.2% to +3.0%, except Drive, 73 to 76 (+4.1%).
- Drive's rect is 1 px past "about 4%", and its ink is not. A whole-px rect rounds by up to 1.4% on a 73 px label. On the pixels (`ink.txt`), Drive grows 68.69 to 70.34 px (+2.4%), and every drawer label grows +0.7% to +2.4%.
- The header's lines on the pixels (`bands.py`): the event line 422 to 429 px (+1.7%), the weather line 288 to 292 px (+1.4%).
- The growth comes from `TextMotion.Animated` itself. Hinted text rounds each glyph's advance to whole pixels at these sizes, mostly down, and unhinted text keeps the font's linear advances.

**Motion (Done-when 3).** At `animator_duration_scale 10`, recorded with `screenrecord --bit-rate 20000000`: the Settings tile pressed and held, the Google folder opened and closed, and a predictive back on the drawer's All tab. `measure.py` takes each frame's scale from the icons, which are bitmaps and scale exactly. It measures each label's width to subpixel precision, and reports |width / scale - width at rest| in px. Every frame is in `<run>/<scenario>.csv`. The folder's Maps and Photos come from round 2's `folderbox.py` (below).

| Scenario, counted frames (before / after) | Label | before, max | after, max |
|---|---|---|---|
| Press, 12 / 12 | Settings | 0.82 | 0.50 |
| Folder, 22 / 23 | Google title | 1.02 | 0.60 |
| | Maps | 1.09 | 0.74 |
| | Photos | 0.92 | 0.63 |
| Back, 41 / 44 | Calendar | 1.00 | 0.47 |
| | Camera | 1.22 | 0.38 |
| | Chrome | 1.04 | 0.39 |
| | Clock | 1.07 | 0.36 |

After the change, every label stays within 0.74 px in every measured frame. Before, the folder and back reach 1.02 to 1.22 px. Before, the press peaks under 1 px, at 0.82, but its raw widths show the hinting: they hold, then drop in whole-pixel steps while the scale falls smoothly.

- Before: 114.90, 113.87, 112.83, 112.72, 111.61, 109.40, 109.19, 107.05, 106.99, 106.94, 104.90, 104.80 px, at scale 0.995 to 0.914.
- After: 116.42, 115.86, 114.98, 114.48, 113.23, 111.22, 111.07, 110.01, 108.63, 108.62, 108.00, 107.20 px, at scale 0.993 to 0.914.

**The folder's frames, round 2.** `measure.py` found each label by its shape near where it last was. Where the shrinking folder's labels lie over home's dimmed tiles, it caught home's "Settings" instead, so round 1 counted folder frames at scale 0.7 and above only. `folderbox.py` places each label's box from its own icon instead, scaled. So the box holds only where that label can be. It measures contrast against each column's own background, so home's Settings disc behind Maps does not count. The crops are saved, and each holds only its label (`crops/`). On the frames round 1 counted, it agrees with `folder.csv` within 0.03 px.

- **A frame counts** when both icons are found, each icon's own width agrees with the scale within 0.03, and the scale is 0.55 or more. 0.55 was `measure.py`'s first floor, set before any "after" frame was seen.
- **After:** frames 17 to 31 opening and 43 to 50 closing, 23 frames at scale 0.571 and up. Round 1 counted 18. Frames 17, 18, 48, 49 and 50 are new, and they read at most 0.74 px (Maps, frame 49, which has home's dimmed "Settings" faintly behind it).
- **Before:** frames 19 to 32 and 45 to 52, at scale 0.604 and up. The new frames 50 to 52 add Maps' 1.09 px jump (frames 51 and 52).
- **Not measured, after:** the opening frames up to 16 and the closing frames from 51 on, where the folder shows at scale about 0.5 and below. With icon boxes placed by hand (`after-small.txt`), frames 13, 52 and 53 read up to 2.17 px in rest px, which is 0.58 px or less on screen. At scale 0.23 to 0.44, a half-pixel edge error is 1.1 to 2.2 px once divided by the scale, so these frames can neither meet nor break the bound. Frames 16 and 51 put Maps on home's Settings disc, where the icon is not found. In frames 14 and 15, home's tiles throw the icons' widths off the scale.
- **Not measured, before:** frames 18 and earlier, and 53 on, for the same reasons.

Three counting rules from round 1 still apply to `measure.py`'s frames. The folder's rest frame is frame 36, the first still frame of the open folder (`folder-still.txt`). A frame counts only if the icons agree. The press counts frames before the popover's ring shows.

The measuring runs one video or frame list per process, loads one frame at a time, and is capped at 1 GB with `systemd-run`.

**Emulator left as found, mostly.** The killed session had written a layout with a "Google" folder (Maps and Photos) to the app's `files/layout.json`. I put its saved original back (md5 `baf1f6b8...`), and home draws Maps and Photos loose again. `animator_duration_scale` was already back at 1. LoR is still set as the emulator's default home, as that session set it. The "after" APK is installed, pointed at the test server, which holds the demo set.

**Found along the way, not fixed here:**

- The verify skill (`.claude/skills/verify-lor/SKILL.md`, "Evidence") still tells authors to run `atelier evidence record`, which this Atelier does not have. lor-3hnk's report noted this too.

### Responses

1-vr61.1: fixed in 005b523
1-vr61.2: fixed in 005b523
