---
schema: atelier.item
schema_version: 3
id: lor-doyc
title: Mail retries do not repeat what an earlier attempt already did
status: draft
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-10T05:21:29.544756981Z
---

## Goal

A mail run that fails part-way is retried in a fresh session two minutes later, and can repeat what it already did: a second WhatsApp message, a second flagged task (review finding lor-5fz3). Since the runner now clears only the messages a run names as handled, the gap is a run that acted on a message and then died before replying.

## Done when

- [ ] A retried mail run does not repeat a message or flag an earlier attempt already sent for the same Gmail id.
