---
created_at: "2026-11-08T01:17:11.865200219+00:00"
id: "lor-m57k"
evidence_type: "validation"
commit: "7407de57818ee29f5f0b63260d793b1bcba542f5"
result: "pass"
captured_at: "2026-11-08T01:17:11.865148152+00:00"
path: "/tmp/claude-1000/zzyy-emu/01-home-menu-unlocked.png"
agent_identity: "lor-zzyy-author"
target:
  kind: "issue"
  id: "lor-zzyy"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-zzyy"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "T3_Phone_API35 (emulator-5554), APK from boat build of 7407de5's code (assembleDebug testDebugUnitTest), installed with install -r; taps and screenshots through agent-device, drags with adb input motionevent; layout.json read with run-as after each step. The launcher reaches no LoR server, so the test server wasn't needed. Home menu: unlocked it reads Edit home, Wallpaper, Home settings, Lock home (01, d1-home-menu-unlocked.txt); in edit mode, Lock home wrote \"locked\": true, the Undo bar said 'Home is locked.' and edit mode's Done bar was gone (02, 03). Undo right after Lock put the file back to sha256 5c929e57... (the pre-lock sum). Locked, the menu reads Wallpaper, Home settings, Unlock home, no Edit home (04, d11-home-menu-locked.txt). Unlock home removed the field (sha back to 5c929e57..., 'Home is unlocked.', 05) and Undo after it put \"locked\": true back (79ac800c...). The accessibility tree names the entries 'Lock home' and 'Unlock home' (agent-device snapshot; uiautomator dump can't run while agent-device's helper holds UiAutomation)."
updated_at: "2026-11-08T01:17:11.875437984+00:00"
---

T3_Phone_API35 (emulator-5554), APK from boat build of 7407de5's code (assembleDebug testDebugUnitTest), installed with install -r; taps and screenshots through agent-device, drags with adb input motionevent; layout.json read with run-as after each step. The launcher reaches no LoR server, so the test server wasn't needed. Home menu: unlocked it reads Edit home, Wallpaper, Home settings, Lock home (01, d1-home-menu-unlocked.txt); in edit mode, Lock home wrote "locked": true, the Undo bar said 'Home is locked.' and edit mode's Done bar was gone (02, 03). Undo right after Lock put the file back to sha256 5c929e57... (the pre-lock sum). Locked, the menu reads Wallpaper, Home settings, Unlock home, no Edit home (04, d11-home-menu-locked.txt). Unlock home removed the field (sha back to 5c929e57..., 'Home is unlocked.', 05) and Undo after it put "locked": true back (79ac800c...). The accessibility tree names the entries 'Lock home' and 'Unlock home' (agent-device snapshot; uiautomator dump can't run while agent-device's helper holds UiAutomation).
