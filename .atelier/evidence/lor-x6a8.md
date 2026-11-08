---
created_at: "2026-11-08T01:03:34.771417026+00:00"
id: "lor-x6a8"
evidence_type: "validation"
commit: "8a63ed601defc89ccbb3d38dc445dc3a8dd9aea0"
result: "pass"
captured_at: "2026-11-08T01:03:34.771371541+00:00"
path: "/tmp/claude-1000/ph30-emu"
agent_identity: "lor-ph30-evidence"
target:
  kind: "issue"
  id: "lor-ph30"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-ph30"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "7.8's home-press measurement (lor-3nhh's method): am start -W HOME with Settings in front, one warm-up then ten, each build installed with install -r and given the version 1 two-page layout first. Base 0f19269 (0c1d07b^2, the main commit last merged into lor-ph30; APK sha256 1a5b3f2b...) against head 8a63ed6 (sha256 0ac13c26...), both built on boat. Two rounds per device, rotated (base, head, head, base), one emulator session per device. T3_Phone_API35 (qdmy-emu/layout-v1-two-pages.json): r1 base 70 55 41 77 76 59 67 69 49 66 median 66.5, head 107 75 69 54 76 60 62 79 66 74 median 71.5 (+7.5%); r2 head 72 66 53 87 54 67 60 49 77 66 median 66.0, base 53 66 81 53 63 62 72 51 51 67 median 62.5 (+5.6%); pooled 20 each: base 64.5, head 66.5 (+3.1%). Each run ended on home (screenshots phone/r*-end.png). T3_Fold_API35 unfolded (device_state 2 before and after; ph30-emu/layout-v1-two-pages-fold.json): r1 base 105 129 119 127 176 102 139 122 109 112 median 120.5, head 137 102 115 146 140 118 121 125 154 44 median 123.0 (+2.1%); r2 head 140 144 129 102 102 105 97 109 123 95 median 107.0, base 144 148 83 164 77 118 142 129 118 75 median 123.5 (-13.4%); pooled base 120.5, head 119.5 (-0.8%). Head is within 10% of base on each device in each round (no worse by more than 10%; the spread run to run is wider than the gaps). On the Fold the two builds draw different screens: the base draws home alone at full width in its wide flow, the head pairs home with page 2. Fold end-state screenshots failed (screencap's multi-display warning), so the end state was checked once after the last run by agent-device (fold/after-latency.png: the base's home)."
updated_at: "2026-11-08T01:03:34.782137937+00:00"
---

7.8's home-press measurement (lor-3nhh's method): am start -W HOME with Settings in front, one warm-up then ten, each build installed with install -r and given the version 1 two-page layout first. Base 0f19269 (0c1d07b^2, the main commit last merged into lor-ph30; APK sha256 1a5b3f2b...) against head 8a63ed6 (sha256 0ac13c26...), both built on boat. Two rounds per device, rotated (base, head, head, base), one emulator session per device. T3_Phone_API35 (qdmy-emu/layout-v1-two-pages.json): r1 base 70 55 41 77 76 59 67 69 49 66 median 66.5, head 107 75 69 54 76 60 62 79 66 74 median 71.5 (+7.5%); r2 head 72 66 53 87 54 67 60 49 77 66 median 66.0, base 53 66 81 53 63 62 72 51 51 67 median 62.5 (+5.6%); pooled 20 each: base 64.5, head 66.5 (+3.1%). Each run ended on home (screenshots phone/r*-end.png). T3_Fold_API35 unfolded (device_state 2 before and after; ph30-emu/layout-v1-two-pages-fold.json): r1 base 105 129 119 127 176 102 139 122 109 112 median 120.5, head 137 102 115 146 140 118 121 125 154 44 median 123.0 (+2.1%); r2 head 140 144 129 102 102 105 97 109 123 95 median 107.0, base 144 148 83 164 77 118 142 129 118 75 median 123.5 (-13.4%); pooled base 120.5, head 119.5 (-0.8%). Head is within 10% of base on each device in each round (no worse by more than 10%; the spread run to run is wider than the gaps). On the Fold the two builds draw different screens: the base draws home alone at full width in its wide flow, the head pairs home with page 2. Fold end-state screenshots failed (screencap's multi-display warning), so the end state was checked once after the last run by agent-device (fold/after-latency.png: the base's home).
