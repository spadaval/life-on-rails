---
schema: atelier.item
schema_version: 3
id: lor-zj7x
title: A demo dataset fills every dashboard screen and the phone's panels on the test server
status: building
mission: lor-2803
labels:
- design-lab
- tool
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3Ae823df1c-214b-4cf4-8a65-48a59f8cc549
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:10.064445737Z
---

## Goal

One command loads a made-up but full life into the test database, so every screen has realistic volume and nothing in a screenshot or prototype is Sumeet's. The audit of 2026-11-16 found the three seeds hold two or three goals each, so most screens render empty on the test server.

## Done when

- [x] `node .claude/skills/verify-lor/verify.mjs demo` (and `pnpm demo:test`) empties the test database and loads the demo set through LoR's own write paths, in under 20 seconds, and refuses any database but `life_on_rails_test` with the guard the other verify commands use.
- [x] The set holds: 7 goals with projects and habits (some with hours this week, some with none); about 40 tasks covering open, waiting, later, done today, with the agent, flagged with a note, past due, from Google, and with no date; about 25 people including contacts with cadences, birthdays within 30 days, a group, an organisation and three service short codes; about 30 wiki pages with links between them, two days' write-ups, a pinned page, and one page with five versions written in turn by the owner and the agent so its history and a diff have something to show; today's calendar events, timer and phone and place blocks, one running timer variant, one proposal; a short brief and a long Monday brief; messages and mail headers for two people.
- [x] The test server takes a fixed clock from `LOR_FAKE_NOW` (an ISO time), read only when the database is the test database, so Today can be drawn at 00:05, 09:00, 15:00 and 21:30 from the same data. The demo set's dates are relative to that clock. The page keeps the same clock after it loads: Today's ticking clock (`useNow` in `src/routes/index.tsx`, `useTick` in `__root.tsx`) and every other use of the browser's time read the fixed time the test server hands down, and `?at=<ISO time>` on any page sets it for that tab, on the test database only. A check opens Today at 09:00, waits past one 20-second tick, and finds the remaining time unchanged; on the real database `at=` and `LOR_FAKE_NOW` do nothing.
- [x] `GET /device/home` for the demo device returns a full payload (one thing, events, weather, flags, queue, due soon, people), so the phone's panels and the lab's fixtures can be cut from it.
- [x] The set has named variants a link can ask for with `data=` (a running timer, a paused one, nothing flagged, every job failing), each a small change applied to the same base, on the test database only. A table in `seeds/demo/README.md` says which rows and which variant make which screen state, so a state can be found by name.
- [x] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass; a test shows the refusal for a non-test database and that `LOR_FAKE_NOW` is ignored on any other database.

## Scope

`seeds/demo/`, a loader under `scripts/`, the verify CLI's `demo` command, the clock read in `src/shared/localTime.ts` or where now is taken. No change to scoring, and no fake clock on the real service.

## Context

- The seeds today are graph-only (`pnpm db:seed`, `scripts/seed-graph.ts`); people, pages, blocks and briefs have no seed.
- Names, numbers and addresses are invented; none comes from the real database.

## Report

Three commits: the clock (9d9820d), the demo set and its loader (2712d01), the variants (5594e3a).

**The clock** (`src/server/clock.ts`). `clockNow()` is the owner's now everywhere it means the present: every `now = new Date()` default and request-entry `new Date()` in the server, and the SQL `now()` that a screen compares against or shows (task done `t`, `flagged_at`, `queued_at`, `archived_at`, page versions, `created_at`/`updated_at` on new intents and pages, people's last meeting, the 7-day context window, reset visibility). The gate `fixedClock()` returns a time only when the database name is exactly `life_on_rails_test`. Then it reads `LOR_FAKE_NOW` for the server, and `at=` from the request URL, or from the Referer for a page's server-function calls. The server entry (`src/server.ts`) runs each request inside that clock with AsyncLocalStorage. The shell hands the fixed time to the page, and `useNow` (`src/components/clock.tsx`) replaces `useNow` in `index.tsx`, `useTick` and the sidebar stamp in `__root.tsx`, and `ago()` in Tasks and `<Ago>`. With a fixed time these stop ticking. The root route keeps `at=` and `data=` across links (`retainSearchParams`). A time is `09:00` (on the server clock's day), a local `2026-11-17T09:00`, or an instant with an offset. An owner's edit is never folded into a head version dated after now (`amendable` in `pages.ts`), which a clock set earlier than a head's real stamp would otherwise allow.

These stay on the real clock on purpose: durations and timeouts (`scheduler`, `registry`, the channel's poll), ids (`uuidv7`), logs, Google token expiry, the WhatsApp import's state file, and the sync jobs (`calendarSync`, `tasksSync`, `contactsSync`, `money/sync`, `mailCheck`, `mail/wake`). The test server runs no jobs, and the loader passes those syncs an explicit `now`. `fill.ts` keeps SQL `now()` for `updated_at` and for expiring a superseded proposal: neither is read by a screen.

**The demo set** (`seeds/demo/demo.json`, `seeds/demo/pages/*.md`, `scripts/demo.ts`). `node .claude/skills/verify-lor/verify.mjs demo` (`pnpm demo:test`) refuses any database but `life_on_rails_test` with the same `guardDb()`. `loadDemo()` refuses again before its first query (`refuseUnlessTest`), since `env.ts` defaults `DATABASE_URL` to the real database. It then truncates, runs `ensureSeed`, and writes every row through the write paths, each inside `withClock(<when it happened>)`. Calendar, Google Tasks and mail go through `syncCalendars`, `syncTasks` and `mailCheck` with fake ports. The phone's apps, location fixes, texts, calls and WhatsApp go through `ingest()`, so phone and place blocks come from fill. Two things are inserted directly because nothing but their source writes them: a group's members (only a WhatsApp backup) and each job's last ok run. The ok runs are dated 23:55 on the demo day, so `/status` reads healthy at any `at=` that day. The seed pages `agent`, `me`, `messaging-rules` and `inbox` are overwritten with demo text, so no screenshot shows the owner's own seed pages. Loading takes about 1.4 s, 1.9 s through the CLI.

Contents: 7 goals with 7 projects, 7 habits and a metric (4 goals have hours this week, 3 have none). 42 tasks: 39 own plus 3 synced from Google, covering open, waiting (3), later (4), done today (4), with the agent (6, 3 queued), flagged with a note (3), past due (5) and undated. 20 people with cadences (7 overdue) and birthdays in 3, 12, 18 and 26 days, 2 organisations, 3 short codes and a WhatsApp group. 32 pages with links between them: two pinned besides `me`, two day write-ups, a short brief today and a long one on this week's Monday, and `topics/lisbon-trip` with five versions by owner, agent, owner, agent, owner. Today has calendar events, timer, phone and place blocks, and the agent's proposal on a phone block. Maya and Daniel have texts, WhatsApp and mail both ways. `seeds/demo/README.md` maps links, rows and variants to screen states.

**Variants** (`src/server/demo.ts`): `data=base|running|paused|noflags|failing` on any GET, on the test database only. The current variant is undone first; the `demo_variant` row in `sync_cursor` holds the name, the clock it was applied at, and the undo. An advisory lock serialises a page and its calls.

**Checked**
- `pnpm test` 181 passed, including `tests/clock/clock.test.ts`: `LOR_FAKE_NOW` and `at=` are ignored on the real database and on `_dev`, `_test2` and an unparsable URL. `tests/verify-lor.test.ts` adds `demo --dry-run` refusing the real database and `_dev`.
- `pnpm test:db` 16 files and 144 tests passed, including `tests/db/demo.test.ts` (the loader's refusal, which never touches the database; the contents and timing; a full `/device/home`, `/device/tasks` and `/device/people`) and `tests/db/demoVariants.test.ts`. `pnpm typecheck` is clean.
- On the test server (`LOR_FAKE_NOW=2026-11-17T15:00 verify.mjs up`, then `demo`), `verify.mjs screen clock` passed. At `/?at=09:00` the hero, "next 09:30 Field Guide standup, in 30m" and the sidebar's "Tue 17 Nov · 09:00" were the same before and after a 21-second wait. A Tasks link kept `at=09%3A00`. `/?at=00:05` drew "14H remaining … in 9h 25m".
- By curl, `/device/home?data=running&at=15:00` showed a timer running since 14:13, `data=paused` showed it paused, `data=noflags` showed no flags, and `/status?data=failing` showed "9 jobs need a look". `data=base` brought the server back to "All senses reporting".
- Evidence is in `/tmp/lor-verify/t3-26edc796-f41a31748128767c/`: `clock/` (three PNGs and `clock.log`), `screens/` (tasks, goals, people birthdays, the Lisbon page, failing status), `device-{home,tasks,people}-0900.json`, `device-home-1500-running.json`, `demo-load.json`.

**For the reviewer**
- `/device/home` has no due-soon or people field. The phone reads those from `/device/tasks` and `/device/people` (`LorApp.kt`). I read the Done when's list as the three reads together and did not widen the phone API, which is out of scope. All three return full payloads.
- Weather is a live Google call for the demo's invented Home coordinates, through the env file's `GOOGLE_MAPS_KEY`, with no test-only weather code. The reading therefore changes from day to day. The db test injects a fixed `WeatherCache`.
- `?at=` on the real database is shown by the gate's unit tests. I did not run a server on the real database to try it.
- "From the same data" means Today at 09:00 draws the rest of the day's timer and phone blocks ahead of now, as noted in the README.
- The test database is shared. `pnpm test:db` empties it, so run `pnpm demo:test` again after it.

