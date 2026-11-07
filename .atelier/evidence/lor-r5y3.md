---
created_at: "2026-11-07T22:30:00.894474458+00:00"
id: "lor-r5y3"
evidence_type: "validation"
commit: "5bf4b0bffbfeefff3db10ba49d6a904313ee69de"
result: "pass"
captured_at: "2026-11-07T22:30:00.894412452+00:00"
path: "/tmp/claude-1000/qdmy-perf"
agent_identity: "lor-qdmy-author"
target:
  kind: "issue"
  id: "lor-eh0y"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-eh0y"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Home-press latency re-measured on the approved code: before = 7f94a83 (before-build APK), after = 4cbf748 (qdmy-build-layer APK, cbcbb8a's code). T3_Phone_API35 (emulator-5554), one session, lor-3nhh's method (latency.sh as qdmy-perf/bin/lat.sh: install -r over the same data, the same version 1 layout.json with two extra pages loaded and read back as 202062c9 each time, then one warm-up and ten am start -W HOME with Settings in front, TotalTime ms). Round 1 (before, after): before 77 40 84 218 36 40 58 141 113 57, median 67.5; after 91 65 53 52 67 63 72 45 59 79, median 64.0 (-5.2%). Round 2 (after, before): after 52 60 75 66 51 67 62 43 58 87, median 61.0; before 58 84 59 66 49 70 58 40 61 57, median 58.5 (+4.3%). Within 10% both rounds. The emulator's layout.json (e86e7fb8) and the 4cbf748 APK were put back."
updated_at: "2026-11-07T22:30:00.907836597+00:00"
---

Home-press latency re-measured on the approved code: before = 7f94a83 (before-build APK), after = 4cbf748 (qdmy-build-layer APK, cbcbb8a's code). T3_Phone_API35 (emulator-5554), one session, lor-3nhh's method (latency.sh as qdmy-perf/bin/lat.sh: install -r over the same data, the same version 1 layout.json with two extra pages loaded and read back as 202062c9 each time, then one warm-up and ten am start -W HOME with Settings in front, TotalTime ms). Round 1 (before, after): before 77 40 84 218 36 40 58 141 113 57, median 67.5; after 91 65 53 52 67 63 72 45 59 79, median 64.0 (-5.2%). Round 2 (after, before): after 52 60 75 66 51 67 62 43 58 87, median 61.0; before 58 84 59 66 49 70 58 40 61 57, median 58.5 (+4.3%). Within 10% both rounds. The emulator's layout.json (e86e7fb8) and the 4cbf748 APK were put back.
