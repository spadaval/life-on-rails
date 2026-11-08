---
created_at: "2026-11-08T01:17:12.012274617+00:00"
id: "lor-zqal"
evidence_type: "validation"
commit: "7407de57818ee29f5f0b63260d793b1bcba542f5"
result: "pass"
captured_at: "2026-11-08T01:17:12.012214284+00:00"
path: "/tmp/claude-1000/zzyy-emu/18-outside-lock-drop-refused.png"
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
title: "T3_Phone_API35 (emulator-5554), APK from boat build of 7407de5's code (assembleDebug testDebugUnitTest), installed with install -r; taps and screenshots through agent-device, drags with adb input motionevent; layout.json read with run-as after each step. The launcher reaches no LoR server, so the test server wasn't needed. Store refusal: unlocked from the home menu, then \"locked\": true written into layout.json with run-as sh -c 'cat >' while home stayed open; a motionevent drag of Gmail to empty home was refused with 'Can't: home is locked; unlock it from the home menu.' (18), and sha256 was 28d0404b... before and after. Unlock home from the menu then worked."
updated_at: "2026-11-08T01:17:12.019638865+00:00"
---

T3_Phone_API35 (emulator-5554), APK from boat build of 7407de5's code (assembleDebug testDebugUnitTest), installed with install -r; taps and screenshots through agent-device, drags with adb input motionevent; layout.json read with run-as after each step. The launcher reaches no LoR server, so the test server wasn't needed. Store refusal: unlocked from the home menu, then "locked": true written into layout.json with run-as sh -c 'cat >' while home stayed open; a motionevent drag of Gmail to empty home was refused with 'Can't: home is locked; unlock it from the home menu.' (18), and sha256 was 28d0404b... before and after. Unlock home from the menu then worked.
