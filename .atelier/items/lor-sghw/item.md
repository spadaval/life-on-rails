---
schema: atelier.item
schema_version: 3
id: lor-sghw
title: 'Text stops shimmering when it scales: TextMotion.Animated everywhere'
status: changes_requested
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
- [x] On the emulator with animations slowed (`settings put global animator_duration_scale 10`), before and after: a tile pressed and held, a folder opened and closed, and a predictive back on the drawer, each captured as a run of frames (`adb exec-out screencap -p` in a loop, or `screenrecord --bit-rate 20000000`). In each frame, a label's measured width divided by that frame's scale stays within 1 px of its width at rest after the change; before, the jumps are larger. The widths per frame, before and after, recorded as an evidence receipt.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`android/app/src/main/java/com/spadaval/lor/ui/Look.kt`, `home/HomeScreen.kt`'s `labelStyle`, `home/Header.kt`'s styles and `lor/LorPanel.kt`'s. Not the web editor.

## Context

- Measurements and the reasoning are on the decision. The real Fold check is the `needs-human` loose end lor-s42k after merge (sharpness at rest, no shimmer on press, folder and back).
- Panels added one more since this was written: `lor/LorPanel.kt` builds a `TextStyle` of its own (2026-11-08). The grep in the first Done when line finds every one, whatever the list missed.

## Report

Scaled text no longer shimmers. During a press, a folder opening and the drawer's predictive back, every label's width tracks the frame's scale to within 0.60 px; before the change it jumped by up to 1.22 px. The cost is at rest: labels on home are 0.8% to 3.5% wider, and no label is cut by its ellipsis.

**Done-when line 2 was amended on 2026-11-17.** It allowed the header and labels to grow "at most about 1%". The measured growth is 0.8% to 3.5%, and it comes from `TextMotion.Animated` itself, so meeting 1% would mean revisiting decision lor-mc37. Sumeet accepted the measured widths on 2026-11-17, and the line now allows "up to about 4% wider". It is ticked on the static pair below, which was not re-measured.

**The change** (d6fc94a; taken over after the first implementer's session hit the 4 GB memory cap while measuring). Every `TextStyle(` under `android/app/src/main` sets `textMotion = TextMotion.Animated`. `grep -rn "TextStyle(" android/app/src/main` lists 13, and each has it:

- `ui/Look.kt:114-120`: `Type`'s seven (display, headline, title, body, bodySmall, label, button). Every Material `Typography` slot (`Look.kt:146-152`) is `Type.x` or `Type.x.copy(...)`, so it carries the setting.
- `home/HomeScreen.kt:182`: home's `labelStyle`.
- `home/Header.kt:193, 204, 205, 207`: the line, the clock, and both dates.
- `lor/LorPanel.kt:447`: the panel's link.

**Builds.** `android/boat-build.sh` (`assembleDebug testDebugUnitTest`) at this branch's head 65049a9 passes, with 422 tests, 0 failures, 0 errors and 0 skipped (`boat-head/build.log` and its test XML). Its APK is byte-identical (sha256 `12957ee0...`) to the "after" APK that was measured. The "before" APK (`d5fc326c...`) was built on boat from c2b5651, this branch's base. Both builds predate lor-3hnk (Kotlin 2.3.20), which merged to main after this branch started.

**Evidence:** `/tmp/lor-verify/t3-894fb3ee-528162b46c30b0ed/sghw/`. `README.txt` describes each file. The device was `T3_Phone_API35` on the Mac, with LoR as home and `server_url` `http://10.0.2.2:3857`. Both final runs show the header as the date alone, with no event line, so the app most likely did not reach the test server in either run. Both runs saw the same header and grid (`header.txt`, `dy.txt`). Before and after were installed and captured back to back by one script. Earlier rounds whose header differed are in `stale/` and were not used.

**Static pair (Done-when 2).** These are home's first page and the drawer's All tab (`home.png`, `drawer.png`).

- Nothing is newly cut by an ellipsis, on home or in the drawer. The longest home label, "Life on Rails", is 178 px in a 270 px cell.
- Widths are the layout rects from the accessibility snapshots, in whole pixels, so 1 px is about 1% on a 100 px label:

| Home | before | after | change |
|---|---|---|---|
| Header "Tuesday 17 November" | 793 | 801 | +1.0% |
| Phone | 93 | 95 | +2.2% |
| Messages | 143 | 148 | +3.5% |
| Chrome | 110 | 112 | +1.8% |
| Camera | 109 | 111 | +1.8% |
| Gmail | 82 | 83 | +1.2% |
| Calendar | 127 | 129 | +1.6% |
| Google | 100 | 102 | +2.0% |
| YouTube | 126 | 127 | +0.8% |
| Life on Rails | 175 | 178 | +1.7% |
| Settings | 118 | 122 | +3.4% |

- Measured on the pixels instead (subpixel ink width at rest), the growth is +2.3% for Settings on home (114.89 to 117.53 px). In the drawer it is +0.7% to +1.6%: Calendar 124.64 to 125.56, Camera 104.03 to 105.71, Chrome 106.47 to 107.65, Clock 71.45 to 72.04. In the open folder it is +0.8% to +1.3%: the Google title 160.98 to 162.23, Maps 74.91 to 75.92, Photos 96.91 to 97.78.
- The growth comes from `TextMotion.Animated` itself. Hinted text rounds each glyph's advance to whole pixels at these sizes, mostly down, and unhinted text keeps the font's linear advances. So meeting 1% would mean revisiting decision lor-mc37, not changing this code. Sumeet accepted the growth on 2026-11-17, and line 2 was amended to match (above).

**Motion (Done-when 3).** At `animator_duration_scale 10`, recorded with `screenrecord --bit-rate 20000000`: the Settings tile pressed and held, the Google folder opened and closed, and a predictive back on the drawer's All tab. `measure.py` takes each frame's scale from the icons, which are bitmaps and scale exactly. It measures each label's width to subpixel precision, and reports |width / scale - width at rest| in px. Every frame is in `<run>/<scenario>.csv`.

| Scenario, counted frames (before / after) | Label | before, max (mean) | after, max (mean) |
|---|---|---|---|
| Press, 12 / 12 | Settings | 0.82 (0.52) | 0.50 (0.23) |
| Folder, 19 / 18 | Google title | 1.02 (0.63) | 0.60 (0.33) |
| | Maps | 0.75 (0.32) | 0.36 (0.16) |
| | Photos | 0.92 (0.46) | 0.42 (0.20) |
| Back, 41 / 44 | Calendar | 1.00 (0.34) | 0.47 (0.14) |
| | Camera | 1.22 (0.42) | 0.38 (0.13) |
| | Chrome | 1.04 (0.38) | 0.39 (0.12) |
| | Clock | 1.07 (0.38) | 0.36 (0.12) |

After the change, every label stays within 0.60 px. Before, the folder and back reach 1.02 to 1.22 px. Before, press peaks under 1 px, at 0.82, but its raw widths show the hinting: they hold, then drop in whole-pixel steps while the scale falls smoothly.

- Before: 114.90, 113.87, 112.83, 112.72, 111.61, 109.40, 109.19, 107.05, 106.99, 106.94, 104.90, 104.80 px, at scale 0.995 to 0.914.
- After: 116.42, 115.86, 114.98, 114.48, 113.23, 111.22, 111.07, 110.01, 108.63, 108.62, 108.00, 107.20 px, at scale 0.993 to 0.914.

Three counting rules were fixed after the first folder numbers. They apply to both runs and never read a label's width:

- **The folder's rest frame is frame 36.** That is the first frame where the open folder is still: frame-to-frame change 0.000 for frames 36 to 40 in both runs (`folder-still.txt`). The automatic choice had picked a frame after the folder closed.
- **A frame counts only if the icons agree.** Each icon's own width ratio must match the scale from the icons' spacing within 0.03. This drops before's frame 18 and after's frames 14 and 15, where the tracker had caught home's tiles behind a small folder.
- **The folder only counts frames at scale 0.7 and above.** The floor was 0.55. These rules were set after the "after" folder run had been seen, so to be plain: below 0.7 the shrinking folder's labels lie over home's dimmed "Settings" label (seen in after's frames 17 and 18, where Maps read 113.88 px over a 76 px rest width while the Google title read normally). This drops after's frames 17 and 18; before had no frame left between 0.55 and 0.7.

The measuring runs one video per process, loads one frame at a time, and is capped at 1 GB with `systemd-run` (`m1.sh`). The killed session had loaded every frame at once.

**Emulator left as found, mostly.** The killed session had written a layout with a "Google" folder (Maps and Photos) to the app's `files/layout.json`. I put its saved original back (md5 `baf1f6b8...`), and home draws Maps and Photos loose again. `animator_duration_scale` was already back at 1. LoR is still set as the emulator's default home, as that session set it, and the "before" APK is still installed.

**Found along the way, not fixed here:**

- The verify skill (`.claude/skills/verify-lor/SKILL.md`, "Evidence") still tells authors to run `atelier evidence record`, which this Atelier does not have. lor-3hnk's report noted this too.
