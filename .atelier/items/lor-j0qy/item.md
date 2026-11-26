---
schema: atelier.item
schema_version: 3
id: lor-j0qy
title: Concurrent verification runs must not replace each other's server or erase fixtures
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:51:23.586552705Z
---

## Goal

An agent can finish a verification session without another checkout replacing its server or resetting its fixtures. During the lor-ug9r review on 2026-11-25, a passing `screen today` and supplemental screenshots were followed by a failed check whose recent-intent chips had disappeared. Process inspection showed another checkout running `verify.mjs up`, another queued for `screen today`, and a third running `vitest.db.config.ts` against the shared test database. A subsequent server boot failed with `duplicate key value violates unique constraint "page_slug_unique"`. The server and fixtures are shared across threads without coordination.

lor-3ydk covers refusing evidence from a different checkout's server, but explicitly excludes coordination. This item covers the competing writers themselves.

## Done when

- [ ] Verification sessions protect their server and fixtures from competing `verify-lor`, UI-flow, demo-loader, and database-test runs, either through a shared exclusive reservation or isolated targets. The chosen behavior and commands are documented.
- [ ] A second checkout attempting a conflicting run receives an actionable refusal or waits visibly; it cannot kill the active session's server, truncate its fixtures, or produce evidence from that session's code.
- [ ] A reproducible check starts competing verification/database-test runs and proves the active session keeps its server identity and fixture rows throughout. Normal single-session verification still succeeds, and existing real-service guards remain intact.

## Scope

Coordinate the existing local verification entry points and `pnpm test:db`. Keep lor-3ydk's evidence identity checks separate. Do not solve this by retrying failed flows, ignoring seed errors, or widening timeouts.
