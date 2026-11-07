---
schema: atelier.review
schema_version: 2
id: lor-5nya
title: finding on lor-lz97 @4220a2d8
target: lor-lz97
identity: reviewer-sbth-clgw-plan-1
commit: 4220a2d8f2c9a8b5f26f4c0fe132985e7c524299
created_at: 2026-11-07T22:23:32.828801918Z
event:
  event: finding
  recommendation: fix_now
---

R6 The 'Against the web' Done when line runs node scripts/ui-check.mjs today, tasks and people on the same test server, but those flows wipe and reseed the test database (today: DELETE FROM commitment, time_block, google_event, intent; tasks: DELETE FROM intent, entity; people: DELETE FROM entity, pages, gmail observations) and then click through edits (start and stop a timer, bulk reschedule, a keep-in-touch change). The web screenshots then show ui-check's data, not the data the emulator showed, so the line cannot show 'the same flagged tasks, open tasks and people on both'. Fix: screenshot the web's /, /tasks, /people?filter=overdue and /people?filter=birthdays on the test data the emulator used without running the flows (a navigate-only flow or a one-off playwright call), or seed with a flow first and point the emulator at the result.
