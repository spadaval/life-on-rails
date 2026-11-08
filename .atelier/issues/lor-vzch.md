---
created_at: "2026-11-08T06:38:07.148428971+00:00"
id: "lor-vzch"
issue_type: "task"
labels:
- "verification"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "review"
    id: "lor-t8jr"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "verify-lor screen follows a shell LOR_URL to whatever server it names"
updated_at: "2026-11-08T06:40:06.549894354+00:00"
---

## Goal

`verify-lor screen` must keep the flow's `lor` calls on the throwaway server when the shell has `LOR_URL` set.

## Done when

- [ ] `run()` drops `LOR_URL` from every child, the same way it drops an inherited `DATABASE_URL`.
- [ ] A test shows `screen` does not pass a shell `LOR_URL` through to `cli/lor.mjs`.

## Context

Found in review of lor-sfh9. `LOR_URL=http://127.0.0.1:1 LOR_PORT=3857 node cli/lor.mjs status` tried port 1 and did not use 3857. The variable is unset in the login environment and is not in `~/.config/life-on-rails/env`. The browser half of `screen` uses `LOR_UI_BASE` and is unaffected.
