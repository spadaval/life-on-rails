---
created_at: "2026-11-07T21:41:28.189802162+00:00"
id: "lor-nafb"
evidence_type: "validation"
commit: "f61f6b4f4f4d88e819e31da5de0c4ebf35db5ba1"
result: "pass"
captured_at: "2026-11-07T21:41:28.189691085+00:00"
path: "/tmp/claude-1000/qdmy-emu"
agent_identity: "lor-qdmy-author"
target:
  kind: "issue"
  id: "lor-qdmy"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-qdmy"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "T3_Phone_API35, the f61f6b4 code (APK built before its last edit, a comment in Motion.kt), version 1 layout.json with two extra pages (qdmy-emu/layout-v1-two-pages.json), adb input swipe and motionevent, logcat LorGrid and LorMotion. Across: home to the agent page ([1,0] to [0,0]) and home to page 1 to page 2; another right swipe on the agent page and a left swipe on page 2 logged nothing (the ends stop); a 200 px slow swipe let go at 0.185 and stayed. Up and down: from home a swipe up held at 40% put the drawer's top at y 1440 (960 px of finger travel on 2400), and released it arrived at [1,1]; a motionevent swipe down from y 700 to 1900 brought the drawer in from the top, let go at 0.5, and arrived; held at 0.29 it stayed. A swipe from y 5 opened NotificationShade with no grid line. In the drawer an 800 px swipe up and back scrolled the list with no grid line; at its top a swipe down went [1,1] to [1,0]. Every page: from page 2 ([3,0]) a swipe up reached [1,1] and a swipe down from the drawer's top returned to [3,0]. Composition (five panels): at home only [1,0], [0,0], [2,0] composed; at page 2 [3,0], [2,0]; at the drawer reached from page 2 [1,1], [3,0], [1,0]; the drawer is composed when a move toward it starts. Launcher polish 4.5: Calendar held at home's right edge turned to [2,0] and dropped last there; held at page 1's edge it reached the new [4,0], and the drop made it; dragged back with 4.5 s at the left edge it went [4,0], [3,0], [2,0], [1,0] and stayed on home, and [4,0] left the file; with four extra pages a drop at the last page's edge went to its end with no [6,0]; a new page then Undo put the version 1 text back byte for byte. 4.6: Voice Recorder dragged from the drawer landed at the end of home. 7.2: the Google folder grew from its tile and closed into it (rec-folder.mp4). 7.3: an edge back swipe held scaled the drawer, back to the edge left it whole, committed it returned to [1,0]; a home press with Gmail's menu open closed it, HomeActivity kept focus and no QuickSendActivity started; a home press from page 2 slid to start [1,0]. 7.4: Gmail's window grew from its icon (rec-launch.mp4). 7.6: a double tap opened QuickSendActivity. Dots: one per cell of the row, the agent page's square, the lit one moving; none on the drawer. Screenshots and strips (across-strip, vert-strip, shade-strip, back-strip, menu-strip) in qdmy-emu."
updated_at: "2026-11-07T21:41:28.193506959+00:00"
---

T3_Phone_API35, the f61f6b4 code (APK built before its last edit, a comment in Motion.kt), version 1 layout.json with two extra pages (qdmy-emu/layout-v1-two-pages.json), adb input swipe and motionevent, logcat LorGrid and LorMotion. Across: home to the agent page ([1,0] to [0,0]) and home to page 1 to page 2; another right swipe on the agent page and a left swipe on page 2 logged nothing (the ends stop); a 200 px slow swipe let go at 0.185 and stayed. Up and down: from home a swipe up held at 40% put the drawer's top at y 1440 (960 px of finger travel on 2400), and released it arrived at [1,1]; a motionevent swipe down from y 700 to 1900 brought the drawer in from the top, let go at 0.5, and arrived; held at 0.29 it stayed. A swipe from y 5 opened NotificationShade with no grid line. In the drawer an 800 px swipe up and back scrolled the list with no grid line; at its top a swipe down went [1,1] to [1,0]. Every page: from page 2 ([3,0]) a swipe up reached [1,1] and a swipe down from the drawer's top returned to [3,0]. Composition (five panels): at home only [1,0], [0,0], [2,0] composed; at page 2 [3,0], [2,0]; at the drawer reached from page 2 [1,1], [3,0], [1,0]; the drawer is composed when a move toward it starts. Launcher polish 4.5: Calendar held at home's right edge turned to [2,0] and dropped last there; held at page 1's edge it reached the new [4,0], and the drop made it; dragged back with 4.5 s at the left edge it went [4,0], [3,0], [2,0], [1,0] and stayed on home, and [4,0] left the file; with four extra pages a drop at the last page's edge went to its end with no [6,0]; a new page then Undo put the version 1 text back byte for byte. 4.6: Voice Recorder dragged from the drawer landed at the end of home. 7.2: the Google folder grew from its tile and closed into it (rec-folder.mp4). 7.3: an edge back swipe held scaled the drawer, back to the edge left it whole, committed it returned to [1,0]; a home press with Gmail's menu open closed it, HomeActivity kept focus and no QuickSendActivity started; a home press from page 2 slid to start [1,0]. 7.4: Gmail's window grew from its icon (rec-launch.mp4). 7.6: a double tap opened QuickSendActivity. Dots: one per cell of the row, the agent page's square, the lit one moving; none on the drawer. Screenshots and strips (across-strip, vert-strip, shade-strip, back-strip, menu-strip) in qdmy-emu.
