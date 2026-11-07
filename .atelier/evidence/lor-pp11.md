---
created_at: "2026-11-07T21:09:24.179400117+00:00"
id: "lor-pp11"
evidence_type: "validation"
commit: "3f1380cc77ea54d212e2417c0069940c87ed0bb8"
result: "pass"
captured_at: "2026-11-07T21:09:24.179370762+00:00"
path: "/tmp/claude-1000/eo1g-emu"
agent_identity: "lor-eo1g author agent"
target:
  kind: "issue"
  id: "lor-eo1g"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-eo1g"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "On T3_Phone_API35 (emulator-5554), build 3f1380c's debug APK, 7.1's and 7.3's drawer checks pass again. Drawer top edge measured in each screenshot as the first of 40 rows of Deep Space (12,13,18) at x 30.\n- From home, press at y 1800 moved to 840 (40%) and held: edge at 1440, the finger's travel exactly. Back to 1560 (10%) and held: edge at 2160. Released: \"drawer let go at 0.1 from 0.0, -0.0 dp/s up: closed\".\n- To 40% and released slowly: \"drawer let go at 0.4 from 0.0, -0.0 dp/s up: open\".\n- In the drawer, a pull from y 700 held at three points: edge at 300, 600, 1200 for 300, 600, 1200 px of travel. Released at half: \"drawer let go at 0.5 from 1.0, -0.0 dp/s up: closed\".\n- A fling from short of the commit point (raw touch frames written to /dev/input/event2 under su, 300 px up in about 40 ms): \"drawer let go at 0.114593506 from 0.0, 1589.5149 dp/s up: open\". Slower flicks settled by position as the rule says (496 and 722 dp/s at 0.083: closed).\n- Predictive back (gesture navigation on, edge swipe x 2 to 400 held): the drawer drawn scaled down with the wallpaper round it; moved back to the edge and lifted: open at full size. A second back gesture started about 40 ms after that cancel, inside the cancel's 150 ms ease, and held: drawn scaled, steady.\n- Back committed, then a swipe up from y 2050 caught the drawer mid-close: \"drawer let go at 0.4142535 from 0.030922204, -0.0 dp/s up: open\"; it ended open at full size, and the process (pid 27075) never restarted, no AndroidRuntime lines.\n- A home press (am start HOME) from Settings with the drawer left open: home with no drawer.\nNot shown by a recording: the ease itself frame by frame, and the absence of alternating writers in the 150 ms window (screenshots show only end states). agent-device could not be used: emulator-5554 was held by a stale session (t3-790c7c1b0b7aa1f7623a3400, idle since 2026-11-06), which was left alone; gestures went through adb input motionevent and raw events, screenshots through T3's device_screenshot. Screenshots in /tmp/claude-1000/eo1g-emu (01 to 11), not kept in the repo."
updated_at: "2026-11-07T21:09:24.181524969+00:00"
---

On T3_Phone_API35 (emulator-5554), build 3f1380c's debug APK, 7.1's and 7.3's drawer checks pass again. Drawer top edge measured in each screenshot as the first of 40 rows of Deep Space (12,13,18) at x 30.
- From home, press at y 1800 moved to 840 (40%) and held: edge at 1440, the finger's travel exactly. Back to 1560 (10%) and held: edge at 2160. Released: "drawer let go at 0.1 from 0.0, -0.0 dp/s up: closed".
- To 40% and released slowly: "drawer let go at 0.4 from 0.0, -0.0 dp/s up: open".
- In the drawer, a pull from y 700 held at three points: edge at 300, 600, 1200 for 300, 600, 1200 px of travel. Released at half: "drawer let go at 0.5 from 1.0, -0.0 dp/s up: closed".
- A fling from short of the commit point (raw touch frames written to /dev/input/event2 under su, 300 px up in about 40 ms): "drawer let go at 0.114593506 from 0.0, 1589.5149 dp/s up: open". Slower flicks settled by position as the rule says (496 and 722 dp/s at 0.083: closed).
- Predictive back (gesture navigation on, edge swipe x 2 to 400 held): the drawer drawn scaled down with the wallpaper round it; moved back to the edge and lifted: open at full size. A second back gesture started about 40 ms after that cancel, inside the cancel's 150 ms ease, and held: drawn scaled, steady.
- Back committed, then a swipe up from y 2050 caught the drawer mid-close: "drawer let go at 0.4142535 from 0.030922204, -0.0 dp/s up: open"; it ended open at full size, and the process (pid 27075) never restarted, no AndroidRuntime lines.
- A home press (am start HOME) from Settings with the drawer left open: home with no drawer.
Not shown by a recording: the ease itself frame by frame, and the absence of alternating writers in the 150 ms window (screenshots show only end states). agent-device could not be used: emulator-5554 was held by a stale session (t3-790c7c1b0b7aa1f7623a3400, idle since 2026-11-06), which was left alone; gestures went through adb input motionevent and raw events, screenshots through T3's device_screenshot. Screenshots in /tmp/claude-1000/eo1g-emu (01 to 11), not kept in the repo.
