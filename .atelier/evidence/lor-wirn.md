---
created_at: "2026-11-08T01:17:11.978651959+00:00"
id: "lor-wirn"
evidence_type: "validation"
commit: "7407de57818ee29f5f0b63260d793b1bcba542f5"
result: "pass"
captured_at: "2026-11-08T01:17:11.978573022+00:00"
path: "/tmp/claude-1000/zzyy-emu/strip-12-14.png"
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
title: "T3_Phone_API35 (emulator-5554), APK from boat build of 7407de5's code (assembleDebug testDebugUnitTest), installed with install -r; taps and screenshots through agent-device, drags with adb input motionevent; layout.json read with run-as after each step. The launcher reaches no LoR server, so the test server wasn't needed. Locked drags, each a motionevent DOWN, 0.9 s hold, moves of 20, 50 px, then far away, screenshot while still pressed, then UP: Gmail on home (11), Phone in the dock (12), the Google folder tile (13), Clock in the open Pair folder (14), F-Droid in the drawer (15). In every screenshot no Remove bar shows, the item is in its place, and the Undo bar says 'Home is locked. Unlock it from the home menu.'; the app's menu stays open and the folder tile opened no menu. layout.json's mtime was the same before and after each (stat %Y.%y: 1794100260 / 20:11:00.35 all five). The open Pair folder showed 'Pair' as text with no 'Folder name' field and no 'Add apps' node (08, d6)."
updated_at: "2026-11-08T01:17:11.986176867+00:00"
---

T3_Phone_API35 (emulator-5554), APK from boat build of 7407de5's code (assembleDebug testDebugUnitTest), installed with install -r; taps and screenshots through agent-device, drags with adb input motionevent; layout.json read with run-as after each step. The launcher reaches no LoR server, so the test server wasn't needed. Locked drags, each a motionevent DOWN, 0.9 s hold, moves of 20, 50 px, then far away, screenshot while still pressed, then UP: Gmail on home (11), Phone in the dock (12), the Google folder tile (13), Clock in the open Pair folder (14), F-Droid in the drawer (15). In every screenshot no Remove bar shows, the item is in its place, and the Undo bar says 'Home is locked. Unlock it from the home menu.'; the app's menu stays open and the folder tile opened no menu. layout.json's mtime was the same before and after each (stat %Y.%y: 1794100260 / 20:11:00.35 all five). The open Pair folder showed 'Pair' as text with no 'Folder name' field and no 'Add apps' node (08, d6).
