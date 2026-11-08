---
schema: atelier.review
schema_version: 2
id: lor-0ri5
title: finding on lor-q18v @b2caee4e
target: lor-q18v
identity: reviewer-q18v-1
commit: b2caee4e6d7daf6ad7918a7080f0872950c05260
created_at: 2026-11-08T00:11:10.674923010Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: orchestrator
    commit: c734540c08831f05bdc506af1967fde767fdbae2
    recorded_at: 2026-11-08T00:11:41.293483795Z
    follow_up: lor-leh4
    reason: A one-clause spec gap with no code misled today; lor-leh4, the consumer, adds it.
---

docs/v2-product-engineering-spec.md:516 lists what GET /device/home carries (down to agent_queue) but not the new weather field, so the spec's launcher section is now incomplete for whoever builds against it (lor-leh4). Fix: add 'and weather, Google's current conditions at the phone's latest location fix (decision lor-g4ta), or null' to that line. Defer: Scope named only the Config list, the omission misleads no current code, and lor-leh4 touches the same contract.
