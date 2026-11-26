---
schema: atelier.item
schema_version: 3
id: lor-rdbu
title: Keep concurrent UI checks from replacing each other's server and test data
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:56:21.342257937Z
---

## Goal

UI verification can run in several threads without one replacing another's server or deleting its fixtures. During lor-zk52's review, a concurrent `verify.mjs up` interrupted demo setup with "that slug is taken", and another screen flow replaced the demo goals before a follow-up layout check. The implementer's Report records the same collisions. Both the listener on :3857 and `life_on_rails_test` are shared; another port alone still shares destructive fixture setup.

lor-3ydk covers refusing evidence from the wrong checkout's server. This item covers keeping concurrent runs from interfering in the first place.

## Done when

- [ ] A verification session keeps its server and fixture data stable from setup through collection. A competing session either uses isolated resources or waits/refuses with a useful explanation; it cannot replace the first session's listener or fixtures.
- [ ] The commands that reset or mutate verification data, including screen flows, demo setup, database tests and catalogue variants, participate in the same coordination or isolation strategy. Sequential checks still work without manual cleanup after failure.
- [ ] A deterministic concurrency check starts two sessions and proves that the second cannot change the first session's data or serving checkout while it collects evidence.
- [ ] The verify-lor instructions explain session ownership and how to recover from an interrupted session. Real database and service-port refusals remain intact.

## Scope

The shared verification resources and their callers: `.claude/skills/verify-lor/verify.mjs`, `scripts/ui-server.sh`, fixture-loading screen and screenshot commands, and database-test setup. Choose the smallest coordination or isolation change that covers the writers; do not change production service behavior.
