---
schema: atelier.item
schema_version: 3
id: lor-zj7x
title: A demo dataset fills every dashboard screen and the phone's panels on the test server
status: draft
mission: lor-2803
labels:
- design-lab
- tool
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:10.064445737Z
---

## Goal

One command loads a made-up but full life into the test database, so every screen has realistic volume and nothing in a screenshot or prototype is Sumeet's. The audit of 2026-11-16 found the three seeds hold two or three goals each, so most screens render empty on the test server.

## Done when

- [ ] `node .claude/skills/verify-lor/verify.mjs demo` (and `pnpm demo:test`) empties the test database and loads the demo set through LoR's own write paths, in under 20 seconds, and refuses any database but `life_on_rails_test` with the guard the other verify commands use.
- [ ] The set holds: 7 goals with projects and habits (some with hours this week, some with none); about 40 tasks covering open, waiting, later, done today, with the agent, flagged with a note, past due, from Google, and with no date; about 25 people including contacts with cadences, birthdays within 30 days, a group, an organisation and three service short codes; about 30 wiki pages with links between them, two days' write-ups and a pinned page; today's calendar events, timer and phone and place blocks, one running timer variant, one proposal; a short brief and a long Monday brief; messages and mail headers for two people.
- [ ] The test server takes a fixed clock from `LOR_FAKE_NOW` (an ISO time), read only when the database is the test database, so Today can be drawn at 00:05, 09:00, 15:00 and 21:30 from the same data. The demo set's dates are relative to that clock.
- [ ] `GET /device/home` for the demo device returns a full payload (one thing, events, weather, flags, queue, due soon, people), so the phone's panels and the lab's fixtures can be cut from it.
- [ ] A table in `seeds/demo/README.md` says which rows make which screen state, so a state can be found by name.
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass; a test shows the refusal for a non-test database and that `LOR_FAKE_NOW` is ignored on any other database.

## Scope

`seeds/demo/`, a loader under `scripts/`, the verify CLI's `demo` command, the clock read in `src/shared/localTime.ts` or where now is taken. No change to scoring, and no fake clock on the real service.

## Context

- The seeds today are graph-only (`pnpm db:seed`, `scripts/seed-graph.ts`); people, pages, blocks and briefs have no seed.
- Names, numbers and addresses are invented; none comes from the real database.
