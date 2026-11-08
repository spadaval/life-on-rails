---
created_at: "2026-11-08T02:01:01.319886368+00:00"
id: "lor-h13z"
evidence_type: "validation"
commit: "2cc9197d15592d6f14e5c1b2e95fa32d9975b8f1"
result: "pass"
captured_at: "2026-11-08T02:01:01.319828600+00:00"
agent_identity: "lor-q18v-author"
target:
  kind: "issue"
  id: "lor-q18v"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-q18v"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Live check, 2026-11-08 02:00 UTC: scripts/ui-server.sh (:3857, life_on_rails_test, this worktree's build) with GOOGLE_MAPS_KEY from the env file; one location observation for Milpitas (37.4323, -121.8996) inserted. Two curls of /device/home 3 s apart both returned weather {\"temp_c\": 18, \"condition\": \"Clear\", \"is_day\": false, \"observed_at\": \"2026-11-08T02:00:33.294Z\"}. The server log (/tmp/lor-ui-server.log) has exactly one call line, 'weather: 200 in 239 ms'; grep counts for the key and for the coordinates in that log were 0."
updated_at: "2026-11-08T02:01:01.325802019+00:00"
---

Live check, 2026-11-08 02:00 UTC: scripts/ui-server.sh (:3857, life_on_rails_test, this worktree's build) with GOOGLE_MAPS_KEY from the env file; one location observation for Milpitas (37.4323, -121.8996) inserted. Two curls of /device/home 3 s apart both returned weather {"temp_c": 18, "condition": "Clear", "is_day": false, "observed_at": "2026-11-08T02:00:33.294Z"}. The server log (/tmp/lor-ui-server.log) has exactly one call line, 'weather: 200 in 239 ms'; grep counts for the key and for the coordinates in that log were 0.
