---
schema: atelier.review
schema_version: 3
id: lor-5fz3
title: finding on branch mail-runner @63f6dbc2
branch: mail-runner
identity: sol
commit: 63f6dbc2b92cda9f0bba06f7d891537e9fe5e76e
created_at: 2026-11-10T05:20:18.473627128Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: fable
    commit: 79d309b187dad08f3c566c81feeb648b72e6713d
    recorded_at: 2026-11-10T05:21:32.032526429Z
    follow_up: lor-doyc
    reason: Existing retry design, narrowed by per-message acknowledgment in 79d309b; the rest is follow-up work.
---

medium: runner/mail.sh:10-18 Retrying a failed batch repeats side effects already completed before the failure. The timer runs every two minutes and each attempt starts a fresh Claude session with the whole pending batch; no per-message progress or deduplication is retained. A run that messages the owner or creates a flagged task and then fails can repeat that action on every retry, consuming attention and model allowance. Two stubbed invocations with a simulated delivered-message side effect followed by exit 1 produced the side effect twice and never called mail done. Follow up with per-message handled progress or side-effect deduplication keyed to the Gmail ID, while retaining unhandled messages. DEFER because at-least-once retry is an existing documented design, and this restoration does not need a general retry/watchdog subsystem. Residual related risk: assistant-mail.service has TimeoutStartUSec=infinity, so a hung attempt can hold all later mail until stopped; no live queue was exercised.
