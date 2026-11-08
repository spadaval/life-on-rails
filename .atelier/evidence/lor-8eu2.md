---
created_at: "2026-11-08T00:33:51.277923067+00:00"
id: "lor-8eu2"
evidence_type: "validation"
commit: "383b6b68012563028e0fae432de676fd675246e1"
result: "pass"
captured_at: "2026-11-08T00:33:51.277848829+00:00"
path: "/tmp/claude-1000/sqfe-emu"
agent_identity: "lor-sqfe-author"
target:
  kind: "issue"
  id: "lor-sqfe"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-sqfe"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Bottom controls on T3_Phone_API35 (1080x2400, APK of 383b6b6's tree). Upward from the navigation bar: the Search field (y 2169-2316), then the row with Sections, A–Z, Most used and, in Sections only, Jump (y 2037-2163); every control's top is below half the screen's height (1200). Read from agent-device's UiAutomation accessibility tree (01-drawer-sections.json via bin/ctl) because uiautomator dump is killed (exit 137) while agent-device holds the UiAutomation session. Screenshots of each view: 01-drawer-sections, 02-drawer-az (no Jump), 04-drawer-mostused."
updated_at: "2026-11-08T00:33:51.288059561+00:00"
---

Bottom controls on T3_Phone_API35 (1080x2400, APK of 383b6b6's tree). Upward from the navigation bar: the Search field (y 2169-2316), then the row with Sections, A–Z, Most used and, in Sections only, Jump (y 2037-2163); every control's top is below half the screen's height (1200). Read from agent-device's UiAutomation accessibility tree (01-drawer-sections.json via bin/ctl) because uiautomator dump is killed (exit 137) while agent-device holds the UiAutomation session. Screenshots of each view: 01-drawer-sections, 02-drawer-az (no Jump), 04-drawer-mostused.
