---
created_at: "2026-11-08T02:34:26.405994439+00:00"
id: "lor-kfk6"
evidence_type: "validation"
commit: "56bc1997e7c17a835f2c132678e994d17b6fc955"
result: "pass"
captured_at: "2026-11-08T02:34:26.405902708+00:00"
path: "/tmp/claude-1000/6get-emu"
agent_identity: "lor-6get-author"
target:
  kind: "issue"
  id: "lor-6get"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-6get"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "T3_Phone_API35, v1 layout with two extra pages (screenshots strip-close.png, strip-settings.png, 29-31 in this directory): the minimap opens from the home menu's Panels (01, 02) and from Home settings' Panels row, which brings HomeActivity to the front (dumpsys: topResumedActivity HomeActivity) with the minimap open (07, 08). From page 2, Done, back and a home press each close it with home still on page 2 (03-06); back from the settings-opened minimap leaves home (09). Removing the stack home is on ([3, 0]) and closing leaves home on [2, 0], GridMoves.stay's nearest in the row (29-31)."
updated_at: "2026-11-08T02:34:26.421869233+00:00"
---

T3_Phone_API35, v1 layout with two extra pages (screenshots strip-close.png, strip-settings.png, 29-31 in this directory): the minimap opens from the home menu's Panels (01, 02) and from Home settings' Panels row, which brings HomeActivity to the front (dumpsys: topResumedActivity HomeActivity) with the minimap open (07, 08). From page 2, Done, back and a home press each close it with home still on page 2 (03-06); back from the settings-opened minimap leaves home (09). Removing the stack home is on ([3, 0]) and closing leaves home on [2, 0], GridMoves.stay's nearest in the row (29-31).
