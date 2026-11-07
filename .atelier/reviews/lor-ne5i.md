---
schema: atelier.review
schema_version: 2
id: lor-ne5i
title: finding on lor-q18v @4220a2d8
target: lor-q18v
identity: reviewer-sbth-clgw-plan-1
commit: 4220a2d8f2c9a8b5f26f4c0fe132985e7c524299
created_at: 2026-11-07T22:23:32.721970785Z
event:
  event: finding
  recommendation: fix_now
---

R2 The last Done when line says the server log shows one Google call after two curls, but lor-g4ta and this item's cache line log only a failed or timed-out call; a successful call logs nothing, so 'a second curl adds no Google call' cannot be read from the log. Fix: in lor-g4ta and lor-q18v, log one line per Google call (status and duration, no key and no coordinates), and keep the failure line.
