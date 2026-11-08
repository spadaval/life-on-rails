---
created_at: "2026-11-08T03:34:53.175517716+00:00"
id: "lor-55b4"
evidence_type: "validation"
commit: "db4cb4fe963e9a0c2ab38954c0bbdc435e1effc8"
result: "fail"
captured_at: "2026-11-08T03:34:53.175463615+00:00"
path: "/tmp/claude-1000/sbth-emu/phone"
agent_identity: "lor-sbth-evidence"
target:
  kind: "issue"
  id: "lor-sbth"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-sbth"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "7.8's home-press measurement on T3_Phone_API35 (lor-3nhh's method): am start -W HOME with Settings in front, one warm-up then ten, one emulator session, two rounds rotated (base, head, head, base). Base: d1d03d6 (git merge-base main lor-sbth), APK sha256 c57feb8f..., the version 1 two-page layout (qdmy-emu/layout-v1-two-pages.json), no widgets. Head: db4cb4f, APK sha256 3fd56e90..., the same file with Clock's Digital (id 4, 100 dp) and Markor's todo list (id 5, 80 dp) added to home's stack through the home menu (phone/head-widgets-layout.json, reloaded before each head round). Both built on boat. Round 1: base 110 83 84 53 62 44 52 73 65 93 median 69.0; head 106 93 85 65 52 45 59 46 68 42 median 62.0 (-10.1%). Round 2: head 93 61 60 49 76 69 69 42 104 47 median 65.0; base 122 44 114 53 70 56 61 29 60 46 median 58.0 (+12.1%). Pooled 20 runs each: base 61.5, head 63.0 (+2.4%). Round 2's head median is 12.1% over its base, outside the line's 10%, so the line is not met as measured; single runs range 29 to 122 ms and round 1 went the other way by about as much, so the rounds don't separate the builds. Each run ended on home (phone/ends.png). Afterwards the phone's layout.json and launch-stats.json put back (the sweep deleted ids 4 and 5)."
updated_at: "2026-11-08T03:34:53.189305995+00:00"
---

7.8's home-press measurement on T3_Phone_API35 (lor-3nhh's method): am start -W HOME with Settings in front, one warm-up then ten, one emulator session, two rounds rotated (base, head, head, base). Base: d1d03d6 (git merge-base main lor-sbth), APK sha256 c57feb8f..., the version 1 two-page layout (qdmy-emu/layout-v1-two-pages.json), no widgets. Head: db4cb4f, APK sha256 3fd56e90..., the same file with Clock's Digital (id 4, 100 dp) and Markor's todo list (id 5, 80 dp) added to home's stack through the home menu (phone/head-widgets-layout.json, reloaded before each head round). Both built on boat. Round 1: base 110 83 84 53 62 44 52 73 65 93 median 69.0; head 106 93 85 65 52 45 59 46 68 42 median 62.0 (-10.1%). Round 2: head 93 61 60 49 76 69 69 42 104 47 median 65.0; base 122 44 114 53 70 56 61 29 60 46 median 58.0 (+12.1%). Pooled 20 runs each: base 61.5, head 63.0 (+2.4%). Round 2's head median is 12.1% over its base, outside the line's 10%, so the line is not met as measured; single runs range 29 to 122 ms and round 1 went the other way by about as much, so the rounds don't separate the builds. Each run ended on home (phone/ends.png). Afterwards the phone's layout.json and launch-stats.json put back (the sweep deleted ids 4 and 5).
