---
created_at: "2026-11-10T05:48:59.928602471+00:00"
id: "lor-swcg"
issue_type: "feature"
labels:
- "launcher"
priority: "P3"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-13c1"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "A weather icon beside the weather in home's header"
updated_at: "2026-11-10T05:48:59.928602471+00:00"
---

## Goal

Home's header shows an icon for the conditions beside the weather text. Sumeet (2026-11-10): the weather works, "though would be nice if there was an icon there".

## Done when

- [ ] The `clock` block draws an icon for the current conditions from `/device/home`'s `weather`, checked on the emulator against the test server.
