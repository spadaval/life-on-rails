---
created_at: "2026-11-10T05:21:29.544756981+00:00"
id: "lor-doyc"
issue_type: "task"
labels: []
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Mail retries do not repeat what an earlier attempt already did"
updated_at: "2026-11-10T05:21:29.544756981+00:00"
---

## Goal

A mail run that fails part-way is retried in a fresh session two minutes later, and can repeat what it already did: a second WhatsApp message, a second flagged task (review finding lor-5fz3). Since the runner now clears only the messages a run names as handled, the gap is a run that acted on a message and then died before replying.

## Done when

- [ ] A retried mail run does not repeat a message or flag an earlier attempt already sent for the same Gmail id.
