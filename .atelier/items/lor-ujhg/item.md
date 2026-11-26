---
schema: atelier.item
schema_version: 3
id: lor-ujhg
title: Concurrent test runs keep each other's fixtures intact
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:36:25.842719420Z
---

## Goal

Running one checkout's verification cannot erase another run's fixtures. All
checkouts currently share `life_on_rails_test`: `screen goals` deletes the
graph, `demo` replaces it, and database tests truncate tables. During the
lor-j4nb review, the Goals flow failed because its freshly created parent
vanished before the next create; the unchanged flow subsequently passed.
Even after serving this build on a separate port, a later goal create
returned `not-found` for the intent it had just inserted. A gallery could
also finish with `problems: []` while drawing another flow's fixtures.

This is separate from lor-3ydk, which checks the serving checkout's identity
and explicitly excludes coordination of shared database users.

## Done when

- [ ] Every destructive test fixture setup, including verify-lor flows,
  demo loading and database tests, participates in one ownership mechanism
  or runs on an isolated test target. A competing run waits or refuses
  before replacing another run's rows, and names the owner when refusing.
- [ ] Ownership covers setup through evidence collection, is released on
  failure, and cannot affect the production database or server.
- [ ] A deterministic concurrent-run check reproduces the competing setup
  and proves one run's parent intents and screenshots survive intact, or
  the competing run is refused before mutation.

## Scope

The shared test database's callers in verify-lor, `scripts/ui-check.mjs`,
the demo loader, and the database test harness. Keep this separate from
the serving checkout identity check in lor-3ydk.
