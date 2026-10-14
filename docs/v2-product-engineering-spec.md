# Life on Rails v2: design and build order

| | |
| --- | --- |
| **Owner** | Sumeet Padavala |
| **Rewritten** | 2026-10-14, as one description of the current design |
| **Repo** | `/home/supadava/projects/life-on-rails` |
| **Runs on** | `buildbot` (Ubuntu 26.04, headless, Tailscale `buildbot.tail47515a.ts.net`) |
| **Code so far** | `e050ace`: 2023 app archived, v2 scaffolded, nothing has run end to end |

This is a personal project on one box with one user. There is no PR plan, review gate, or staged rollout: work goes to `main` when it works. The earlier, much longer spec (history, superseded decisions, the 2023 post-mortem) is not carried here; the v1 text is in git at `e050ace`.

---

## What it is

Life on Rails (LoR) closes the loop between what the owner intends and what the owner actually does. It has two jobs:

1. **A time tracker that knows intent.** It replaces Toggl. It knows the calendar, the goals, and which goals are starving, and it asks for about 30 seconds of confirmation each evening.
2. **The memory and senses of a personal agent.** An assistant that lives in T3 Code reads and writes LoR through a `lor` CLI: a wiki of what it knows, the owner's tasks and goals, and a record of what happened. LoR notices things (calendar, tasks, mail arriving, phone activity) and wakes the agent when mail needs attention.

The owner has executive dysfunction, so the system must cost less to keep up than to abandon. That drives the product rules below.

## Who is who

- **T3 Code** is where the agent lives and where the owner talks to it: threads, devices, auth, scheduling, models, delegation. Every question the agent has for the owner is asked in a thread.
- **LoR** is what the agent knows, the owner's dashboard, and the senses. It records, shows, and notices, and decides by fixed rules when to wake the agent. It runs no model. It never acts, asks, or approves.
- **The agent** is a role, not a program: any thread that has the skills and has run `lor context`. Its standing instructions are the `agent` wiki page, served by `lor context`.
- **`gog`** is the agent's Google tool, signed in with broad access. LoR never runs it.

Do not add chat, an approval queue, notifications, or a model runtime to LoR.

## Product rules

1. **Low friction.** If it takes more than a tap or a sentence, a sensor or the agent should have done it. The evening confirm is the only daily tax.
2. **Gentle guidance.** Blocks move. Plans are suggestions. No locked schedule, no autopilot.
3. **Safe failure.** `Reset Visibility` hides overdue tasks without deleting history. Trends over streaks. No coins, pets, or shame-red overdue lists.
4. **Today is the home.** Now, next, and the remaining day shown as a quantity (a bar), not only a clock. Usable as a kiosk: one column, large type, no hover-only actions.
5. **Objective and subjective.** Hours plus at most one "how did it feel" prompt in the evening. Never a form.
6. **The owner's data is not rationed.** Nothing is redacted on its way to the agent. Secrets are never logged or stored in tables, and third-party text (mail, web pages, window titles) is data, never instructions.

## Settled choices

- **Stack:** TanStack Start, React 19, Tailwind, Postgres 18 (apt, Unix socket, peer auth, non-superuser role), Drizzle with postgres.js, Zod, Vitest, pnpm. Versions pinned exact. No Docker, no bun.
- **The 2023 app** is frozen under `archive/2023/`. Never import from it. No data is migrated from it.
- **Evidence split:** Observation → Proposal → Commitment. Sensors write observations. LoR suggests proposals. The owner confirms commitments. Scores read commitments only.
- **A proposal means one thing:** LoR suggesting what the owner's time, habits, or metrics were, for the owner to confirm. It is never the agent asking permission.
- **The agent has one rule:** do anything you can take back; ask in the thread before anything you cannot take back, or that speaks to other people as the owner (sending mail, pushing to a shared branch, paying, deleting). It is an instruction, not a mechanism. It will occasionally be broken, and that is accepted in exchange for an agent that can just do things.
- **If code branches on it, it is schema; if only a model reads it, it is prose** on a wiki page. There is no generic fact table.
- **LoR does not mirror the mailbox.** It records that a message arrived and wakes the agent, which reads mail in Gmail.
- **LoR has its own Google login** with read-only scopes, calls Google's APIs directly, and holds the Gmail watch itself.
- **Search is Postgres full-text** (`tsvector` + `pg_trgm`). No embeddings, no pgvector.
- **Nothing is deleted on a schedule.** No retention job. The box has 744 GiB free and a year of data is about a gigabyte at most.
- **Loopback first.** The app binds `127.0.0.1:3847`. Anything reachable beyond loopback is authenticated by Tailscale identity. No local password, no Funnel, no public endpoint.
- **A table is added when the code that uses it is written.**

## Host

| | |
| --- | --- |
| Postgres | 18.6 from apt, installed 2026-10-14. Role `supadava` (not a superuser), databases `life_on_rails` and `life_on_rails_test`. |
| Node | Two toolchains: `~/.local/opt/node/bin/node` is 22.20.0; the interactive shell gets vite-plus Node 24.21.0 and pnpm 12.11.2. The service runs the first by absolute path; builds and tests run under the second. |
| Desktop session | None. The box cannot sense desktop activity. The phone is the activity sensor that matters. |
| Tailscale | `buildbot` `100.124.153.80`, `legend` (Windows), `pixel-9a` (Android). Serve already maps `/` to `127.0.0.1:3773` (T3 Code). **Do not hijack it.** |
| systemd | `Linger=yes` for `supadava`, so user units survive logout. |
| `gog` | v0.43.0, signed in as the owner with read-write calendar, `gmail.modify`, tasks. The agent's. |

---

## Architecture

Two processes: `postgresql`, and `lor-web` (a systemd user unit) which serves the UI and `/api/v1`, runs the jobs, and holds the Gmail watch.

```
Google Calendar, Tasks  ──poll──┐
Gmail watch → Pub/Sub   ──pull──┤
phone sensor (tailnet)  ─ingest─┤→  lor-web  ⇄  Postgres
manual timer (UI)       ────────┘     │  ▲
                                 wake │  │ lor CLI → /api/v1
                                      ▼  │
                                 T3 Code threads (the agent, with gog)
```

**Server entry.** As landed, `pnpm build` writes `dist/server/server.js`, which exports a fetch handler and opens no socket, while `start` runs `.output/server/index.mjs`, which does not exist. The production entry (1) serves that handler and `dist/client` on `LOR_BIND:LOR_PORT`, (2) pings the database and exits non-zero if it is down, and (3) starts the scheduler, once. `srvx` is already in the dependency tree and is the default host for the handler. The systemd unit runs `node` by absolute path on the built entry, not `pnpm`, with `Restart=on-failure` and `EnvironmentFile=%h/.config/life-on-rails/env`.

**Two listeners, one process.**

- `127.0.0.1:3847` serves everything (UI, `/api/v1`, `/health`) with no identity check. It is for use on the box and is never a Serve target.
- `127.0.0.1:3848` is for the owner's other devices. It serves the UI and `/ingest` only, and refuses any request whose `Tailscale-User-Login` header is not `LOR_OWNER_LOGIN`. It is published on its own Serve port, `tailscale serve --bg --http=3847 http://127.0.0.1:3848`, so the dashboard is `http://buildbot:3847`. A port, not a path under T3 Code's `/`: a path prefix would send the app's asset requests to T3. This listener cannot exempt loopback, because Serve's proxy connects from loopback.

**Local dates.** Neither Node on this box has `Temporal`. One helper module (`src/shared/localTime.ts`) answers "which local day is this instant" and "when does local day D start" using `Intl` in `LOR_TZ`, and everything that needs a day boundary uses it. Days are 23 or 25 hours long twice a year.

**`pnpm dev` is UI only.** Vite's reloads would kill the clock, so the scheduler is started only from the production entry.

**Scheduler.** In-process timers. Each job has an in-memory "already running" flag, so a slow run is skipped rather than doubled. Each run writes a `job_run` row. On boot, any row still marked `running` is set to `error` (there is one process, so it cannot be live), and the overnight close runs once if yesterday has no successful run. There is no watchdog, no tick endpoint, and no database lock: if the process dies, systemd restarts it.

| Job | When | Does |
| --- | --- | --- |
| `calendar_poll` | every 300 s | `events.list` per calendar, `singleEvents=true`, from the start of local yesterday to the end of local +2 days. Upserts `google_event` and a `source=calendar` observation, then runs calendar fill. The full-window poll is also how moved and cancelled events are retracted. |
| `tasks_poll` | every 900 s | Upserts `task` intents by `google_task_id`. See "Google Tasks". |
| `mail_check` | on push, at startup, after watch renewal | See "Mail". |
| `mail_watch_renew` | daily | Re-registers the Gmail watch, which lapses after 7 days. |
| `wake_flush` | 180 s quiet or 900 s max | Posts queued mail to the agent. Rows leave the queue only when the POST succeeds. |
| `score_tick` | hourly | Recomputes scores. |
| `overnight_close` | 01:30 local | Merges adjacent blocks, creates missing proposals, snapshots scores. Does not confirm anything and calls no model. |

**Health.** `GET /health` returns 200 whenever the process is listening, with the database state and each job's last successful run. A job is `stale` when that run is older than three intervals. The Capture cockpit shows the same thing plus each source's last arrival and the Google auth state. The cockpit is the only alerting.

**Config** is the env file `~/.config/life-on-rails/env` (mode 0600) and nothing else. There is no profile row; a settings screen can add one later.

```
DATABASE_URL=postgres:///life_on_rails?host=/var/run/postgresql   # socket + peer auth; never 127.0.0.1
LOR_BIND=127.0.0.1
LOR_PORT=3847
LOR_SERVE_PORT=3848           # the identity-checked listener behind tailscale serve
LOR_OWNER_LOGIN=sumeet.padavala@gmail.com   # the only Tailscale login that listener accepts
LOR_TZ=America/Los_Angeles
LOR_WAKE_START=08:00          # the owner's waking day
LOR_WAKE_END=22:00
LOR_SCORING_WEEKS=2
LOR_CALENDAR_POLL_S=300
LOR_TASKS_POLL_S=900
LOR_AGENT_TOKEN=              # bearer for /api/v1; the lor CLI reads it from this file
LOR_WAKE_URL=                 # T3 webhook that delivers new mail to the agent's main thread
LOR_WAKE_QUIET_S=180
LOR_WAKE_MAX_S=900
GOOGLE_ACCOUNT=sumeet.padavala@gmail.com
GOOGLE_OAUTH_CLIENT=/home/supadava/.config/life-on-rails/google-client.json
GOOGLE_TOKEN_FILE=/home/supadava/.config/life-on-rails/google-token.json
GOOGLE_PUBSUB_SUBSCRIPTION=   # projects/<project>/subscriptions/<name>
```

postgres.js treats an empty host as TCP localhost, so `src/db/client.ts` forces the socket directory when the URL has none.

---

## Data model

Single user, so no owner column anywhere. Ids are UUIDv7. Timestamps are `timestamptz`.

`src/db/schema.ts` in the foundation commit is the reference for the tables that already exist: `intent`, `intent_link`, `tag`, `intent_tag`, `observation`, `proposal`, `commitment`, `time_block`, `time_block_observation`, `attribution_rule`, `google_sync_state`, `google_event`, `job_run`, `score_snapshot`. The first migration is regenerated (no database exists yet) with these changes:

- **Drop** `user_profile`, `title_embedding`, `conversation`, `message`, and the `vector` extension.
- **`proposal_kind`** is `time_attribution` only. The other kinds go: the agent writes links directly, the morning plan is a message in the thread, and nothing in LoR suggests habit completions or metric values (the owner records those, in the evening confirm or by asking the agent).
- **`intent_status`** gains `waiting` (a task blocked on someone else) now, because adding a value to an enum later is awkward.
- **`proposal` and `commitment`** gain `via text` (`ui` | `agent` | `rule` | `job`) and `source_ref text` (for agent writes, the T3 thread id).

Added later, each with the code that uses it:

```sql
-- with the memory API
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- trusted: the database owner can create it

CREATE TABLE entity (
  id         uuid PRIMARY KEY,
  kind       text NOT NULL,                 -- person | org | place | thing
  name       text NOT NULL,
  aliases    text[] NOT NULL DEFAULT '{}',
  priority   boolean NOT NULL DEFAULT false,   -- mail from this entity skips the wake debounce
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX entity_name_trgm ON entity USING gin (name gin_trgm_ops);

CREATE TABLE entity_identifier (            -- how an entity is recognised in raw data
  entity_id uuid NOT NULL REFERENCES entity(id) ON DELETE CASCADE,
  kind      text NOT NULL,                  -- email | phone | handle | domain
  value     text NOT NULL,                  -- lowercased
  PRIMARY KEY (kind, value)
);

CREATE TABLE page (
  id           uuid PRIMARY KEY,
  slug         text NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9][a-z0-9/-]*$'),
  title        text NOT NULL,
  entity_id    uuid UNIQUE REFERENCES entity(id) ON DELETE SET NULL,   -- the page is about this entity
  intent_id    uuid UNIQUE REFERENCES intent(id) ON DELETE SET NULL,   -- ... or this goal or project
  head_version int  NOT NULL,
  body         text NOT NULL,               -- copy of the head version, kept here for search
  in_digest    boolean NOT NULL DEFAULT false,   -- body is included in every `lor context`
  search       tsvector GENERATED ALWAYS AS (
                 setweight(to_tsvector('english', title), 'A') || to_tsvector('english', body)) STORED,
  updated_at   timestamptz NOT NULL DEFAULT now(),
  CHECK (num_nonnulls(entity_id, intent_id) <= 1)
);
CREATE INDEX page_search_idx ON page USING gin (search);
CREATE INDEX page_title_trgm ON page USING gin (title gin_trgm_ops);

CREATE TABLE page_version (                 -- every write is a whole new copy
  page_id    uuid NOT NULL REFERENCES page(id) ON DELETE CASCADE,
  version    int  NOT NULL,
  title      text NOT NULL,
  body       text NOT NULL,
  via        text NOT NULL,                 -- ui | agent
  source_ref text,                          -- T3 thread id
  note       text,                          -- one-line edit summary
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (page_id, version)
);
-- also: observation.search, a generated tsvector over title, with a GIN index

-- with the world model
ALTER TABLE intent
  ADD COLUMN waiting_on_entity_id uuid REFERENCES entity(id) ON DELETE SET NULL,
  ADD COLUMN waiting_since timestamptz,
  ADD COLUMN assignee   text NOT NULL DEFAULT 'owner',   -- owner | agent
  ADD COLUMN thread_ref text,                           -- the T3 thread working an agent-assigned task
  ADD COLUMN via        text NOT NULL DEFAULT 'ui',
  ADD COLUMN source_ref text;

-- with calendar and tasks
ALTER TABLE intent ADD COLUMN google_task_id text;
CREATE UNIQUE INDEX intent_google_task_idx ON intent (google_task_id) WHERE google_task_id IS NOT NULL;

-- with mail
CREATE TABLE mail_state (                   -- one row
  id         boolean PRIMARY KEY DEFAULT true CHECK (id),
  history_id text,
  watch_expires_at timestamptz,
  last_push_at     timestamptz
);
CREATE TABLE wake_queue (                   -- arrivals waiting to wake the agent, one row per Gmail thread
  gmail_thread_id text PRIMARY KEY,
  observation_ids uuid[] NOT NULL,
  first_seen      timestamptz NOT NULL,
  last_seen       timestamptz NOT NULL,
  priority        boolean NOT NULL DEFAULT false
);
```

**Intent graph.** Node types: goal, project, habit, task, metric. Links (`intent_link.kind`): `measures`, `blocks`, `derived_from`. **`from_id` is the child or source; `to_id` is the parent or target.** Calendar title patterns live on `attribution_rule.match_title`, not as links. Tags are filters, never score inputs. The migration seeds two system intents that cannot be deleted: `Uncategorized` (manual and activity default) and `Meetings` (calendar default).

**Deletes.** `commitment.intent_id`, `commitment.time_block_id`, and `time_block.intent_id` are `RESTRICT` (history cannot vanish; archive instead). `proposal.time_block_id` is `CASCADE` (retracting a fill block takes its proposals with it). `proposal.intent_id`, `commitment.proposal_id`, `time_block.commitment_id`, and `observation.intent_hint` are `SET NULL`. The cycle proposal → time_block → commitment → proposal is created nullable-first, with the constraints added afterwards; writers insert block, proposal, commitment in that order and set `time_block.commitment_id` last, in one transaction.

**Observation envelope** (`src/shared/observation.ts`): `id`, `t_start`, `t_end`, `device`, `source` (`calendar` | `google_tasks` | `gmail` | `android` | `linux_activity` | `windows_activity` | `manual` | `agent` | `system`), `app`, `calendar_id`, `title`, `url`, `payload`, `intent_hint`. Every sensor uses it. Ingest is idempotent on `id`; resending an id with a later `t_end` extends the row and never shortens it.

---

## Time tracking

### Evidence

- **Observation:** a sensor fact. Never scored.
- **Proposal:** a suggested label for a block. It always has a non-null `time_block_id` and `intent_id`.
- **Commitment:** confirmed by the owner, or by an `attribution_rule` with `auto_apply=true`. Only commitments are scored.

A user-visible interval is always a `time_block`. Accepting its proposal inserts a `commitment kind=time_spent` and sets the denormalized `time_block.committed` and `commitment_id`. `commitment` is the truth; writers update both in one transaction.

### Today

- **Running block:** what is happening now, with elapsed time. Tap to recategorize or split. Pause and resume.
- **Fixing a block:** change its start or end, or delete it. A timer left running overnight is repaired this way.
- **Timeline:** two lanes, planned (calendar) and actual (`time_block` rows). With no activity sensor, the actual lane is pre-filled from the calendar as uncommitted blocks with pending proposals.
- **Remaining day:** a bar from now to the end of the waking day.
- **Gap chips:** empty stretches between events, sized in minutes, optionally with "fits: <starving habit or task>".
- **Recategorize:** last five intents plus search (`r`, then type).
- **Evening confirm:** one timeline of proposed labels. Enter accepts everything labeled; unlabeled meetings commit to `Meetings`. One optional mood or energy prompt.
- **Reset Visibility:** moves overdue active tasks and projects (and any task the owner selects) to `status=later` or sets `snoozed_until`. Goals, habits, and metrics are never moved. Nothing is deleted.
- **With the agent:** tasks assigned to the agent and their state.
- Rows written by the agent are badged and have one-tap undo.

### State machine

Invariants:

- **At most one open block:** `CREATE UNIQUE INDEX time_block_one_open ON time_block ((true)) WHERE t_end IS NULL`. Paused is `t_end IS NULL AND paused_at IS NOT NULL`.
- `startManualBlock` closes the open block and opens the new one in **one transaction** with `SELECT … FOR UPDATE`, so two rapid taps cannot create two open blocks.
- **No overlapping actual seconds.** Calendar blocks are fill only: they occupy time not already taken (see "Calendar fill").
- `time_block.committed` is set only by recategorize, confirm, or an `auto_apply` rule. Never by a poll, ingest, or the overnight close.

| Event | Rows written |
| --- | --- |
| `startManualBlock` | One transaction: lock and close any open block; insert `time_block` (`origin=manual`, `intent_id` = chosen or `Uncategorized`, uncommitted) and a `source=manual` observation. Then sync fill. |
| `stopRunningBlock` | Set `t_end=now`. Insert a pending proposal (confidence 1.0). Then sync fill. |
| `pauseRunningBlock` / `resumeRunningBlock` | Set or clear `paused_at`; on resume add the pause to `idle_s`. `t_end` stays null. |
| `editBlock({blockId, tStart?, tEnd?})` | Move a closed non-calendar block's start or end. A `time_spent` commitment on it moves with it. Then sync fill. |
| `deleteBlock` | Delete a non-calendar block, and its commitment first if it has one. Then sync fill. |
| Calendar poll | Upsert events and observations, then sync fill. Calendar blocks always have `t_end` set. |
| `recategorizeBlock` | Close if open. Reject other pending proposals for the block. Insert `commitment time_spent` (`intent_id` required; `value` is a display copy of the hours). Set `time_block.intent_id`, `committed`, `commitment_id`. An earlier `time_spent` for the block is replaced. Then sync fill if the block is a calendar block. |
| `splitBlock({blockId, at})` | Left keeps the id and ends at `at`; right is a new block with the same origin, intent, and committed state. Observations are re-pointed by midpoint. A commitment is split the same way. |
| `confirmDay` | Each accepted proposal runs the `recategorizeBlock` transaction with the proposal's intent. Each rejected proposal becomes `rejected`. `acceptAllPending` accepts every other pending proposal for that date. Gaps stay gaps. |
| Overnight close | Merge adjacent uncommitted non-calendar blocks with the same intent and a gap of at most 120 s (`origin=merged`). Create missing proposals. |

Activity blocks from sensors are not in this table yet; see "The phone".

**Calendar fill.** One function, `syncCalendarFill(window)`, decides what calendar blocks should exist and changes only what differs. It runs after every calendar poll and after any change to a non-calendar block or to a commitment on a calendar block.

- **Taken time** is every non-calendar block plus every committed calendar block. An open block is taken from its start with no end, so while a timer runs there is no fill ahead of it; stopping the timer brings the fill back.
- **Events that fill:** timed events that are not cancelled, not declined by the owner, not marked free (`transparency=transparent`), and of type `default` or `fromGmail`. All-day events, focus time, out-of-office, and working-location entries show in the planned lane only.
- **Two events at once:** the shorter event gets the overlapping time (ties go to the earlier start). A one-hour meeting inside an afternoon-long hold keeps its hour.
- **Wanted blocks:** for each event, shortest first, its span minus taken time minus what shorter events already got. Each remaining fragment is one closed, uncommitted `origin=calendar` block with a pending proposal (`intent_id` from a matching rule, else `Meetings`).
- **Changing only what differs:** an existing uncommitted calendar block whose event and exact start and end are still wanted is left untouched, with its id and its proposals (including a rejected one). Others are deleted, and their proposals with them. Missing ones are inserted. A poll that finds nothing changed writes nothing.
- **Committed calendar blocks are never rewritten.** If their event moved or was cancelled, Today shows a mismatch chip.

Google gives every instance of a recurring event its own id, so `google_event` is keyed on `(calendar_id, event_id)` and a moved event is an update, not a new row. Fragments of one event share `(calendar_id, calendar_event_id)` on `time_block`, so there is no unique index on that pair.

**Whose label wins,** highest first: (1) the owner's commitment; (2) an `auto_apply` rule, which also commits; (3) an activity rule match (app, exact title, or URL glob), proposal only; (4) the calendar event covering at least half the block, proposal only; (5) unattributed, shown as a gap. The calendar never overwrites (1) or (2).

### Success measures

All counts are unique wall-clock seconds after merging overlapping intervals, never a sum of block durations.

- **Calendar-confirm rate** = `(|C ∩ E| + |M \ E|) / (|E| + |M \ E|)`, where `E` is timed non-cancelled events in the waking day, `C` is committed seconds, and `M` is committed manual seconds. Undefined (shown as `—`) on an empty day. Target: 0.8 confirmed the same evening.
- **Attribution coverage** = committed seconds linked to an intent ÷ waking seconds. Shown as `—` until an activity sensor exists. Target then: 0.7 on weekdays.
- **Goal hours adequacy** = committed hours on a goal ÷ its weekly target. Every goal with `weight ≥ 2` has a target and a starve flag below 50%.

---

## Scoring

Pure functions in `src/server/scoring/formulas.ts`. Recomputed on every commitment write, hourly, and overnight; one `score_snapshot` per intent per local date, with the parts stored so `lor score explain` and the UI show the same numbers. `W` = `LOR_SCORING_WEEKS`; the window is the last `7W` local days.

- **Metric:** mean of committed values normalized to 0–1 between `metric_min` and `metric_max` (five-point: 1 and 5). No values in the window means **undefined**, not zero.
- **Habit:** `adherence = min(C / E, 1)`, where `E = W ×` (expected completions per week) and `C` is committed completions. `quality` = mean raw score of metrics linked to it by `measures`. `score = α·adherence + (1−α)·quality`, with `α = completion_weight` (default 0.7). With no linked metrics, the score is adherence.
- **Task:** 1 if a `task_done` commitment exists, else 0. Overdue is a flag, not a score.
- **Project:** `0.6 × tasks done ÷ tasks + 0.4 × min(committed hours ÷ estimate_hours, 1)`. A missing term is dropped. Hours are those committed directly to the project.
- **Goal:** `β·children + (1−β)·hours`, with `β = goal_mix` (default 0.7; not `completion_weight`). `children` is the weighted mean of raw child scores over `measures` links into the goal; undefined children are skipped, and no children means the term is dropped. `hours = min(unique hours ÷ (weekly_hour_target × W), 1)` over the goal and all its descendants. A missing term is dropped and the other takes the full weight. Nothing to mix means undefined.
- **Descendant walk:** follow `measures` links from parent to children with a visiting set; an edge that would re-enter it is dropped and logged, so cycles terminate.
- **Hours are recomputed from `[t, t_end]` intervals with a sweep-line merge.** `commitment.value` is display only.
- **Known and intended:** a project whose hours all sit on child tasks scores 0 on its hours term while its parent goal counts them. The explanation shows both.
- **Decay on silence:** `d` = whole days since the last relevant commitment. `δ = 1` for `d ≤ 2`, else `exp(−(d−2)/7)`. Applied **once, last, on the node being displayed**. Parents always mix raw child scores.
- **Confidence chip:** high if the last commitment is under 2 days old and the trailing 7-day confirm rate is at least 0.5; medium if there is any commitment in the window; low otherwise.

---

## Memory and the agent interface

### What is stored where

| Knowledge | Home |
| --- | --- |
| About the owner: preferences, health, circumstances | The `me` page |
| Who the agent is, the one rule, what mail is worth raising | The `agent` page |
| A person, organisation, or place | The page whose `entity_id` is that entity |
| A goal or project: context, decisions and why | The page whose `intent_id` is that intent |
| Any other subject | A `topics/…` page |
| What happened on a day or in a week | `days/2026-10-14`, `weeks/2026-W42`, written by the nightly run |
| Something to do, or owed by someone else | A `task` intent (`status=waiting` with `waiting_on_entity_id` for the second) |
| How to do a kind of work | An edit to the relevant skill |
| Worth keeping, no home yet | Appended to `inbox`; the nightly run files it |

The seed creates `me`, `agent`, and `inbox`. `me` and `agent` are `in_digest`.

### Pages

A page is plain markdown, one per subject, complete in itself. Every write stores the whole body as a new `page_version`, so history, diff, and revert need no machinery, and a bad edit is one revert away. No page is protected.

Links are `[[slug]]` and citations are footnotes, both as ordinary text in the body:

```md
Priya moved to Austin in March [^move] and prefers calls to email [^calls]. See [[projects/austin-trip]].

[^move]: mail:18c2f4a9d0e1b2c3 "we close on the house March 3"
[^calls]: thread:3f3320a0 @2026-10-13
```

Citation targets are `mail:<gmail-id>`, `obs:<id>`, `thread:<id>`, and `url:<https://…>`. The server stores these as written and does not parse or check them; the Memory UI turns them into links when it renders. Cite sources, never another page.

Write rules:

| Operation | Behaviour |
| --- | --- |
| `create` | Refused (exit 6, naming the existing page) if the slug, `entity_id`, or `intent_id` is taken. |
| `update --base <n>` | Refused as `stale-base`, with the current head, if `n` is not the head version. Otherwise one transaction: insert version `n+1`, set `page.body` and `head_version`. Whole body on stdin, or `--find/--replace` where the text occurs exactly once. `--dry-run` prints the diff. |
| `update --append` | No `--base`; the server appends to the head inside the transaction. For `inbox` and running logs. |
| set `in_digest` | The owner only, in the UI. |
| any agent write | Refused without `--thread`. |

The agent searches for an existing page before creating one and prefers updating it.

### `lor` CLI

`cli/lor.mjs`, plain JavaScript with no build step, symlinked as `~/.local/bin/lor`. It reads `LOR_AGENT_TOKEN` and `LOR_PORT` from the env file, calls `http://127.0.0.1:$LOR_PORT/api/v1/…`, and prints JSON, or markdown with `--md`. Exit codes: `0` ok, `2` usage, `3` empty, `4` auth, `5` not found, `6` refused by a write rule, `8` server unavailable.

```
# orientation
lor context [--task <id>]               # the digest; run first in every session
lor status                              # job and sensor health
lor changes --since <rfc3339>           # what arrived or changed after that time, by kind

# read
lor search <query> [--in page,obs,intent,entity] [--from <t>] [--to <t>] [--limit N]
lor page search <query>
lor page read <slug> [--version N] [--history]
lor entity show <id|name|email>         # its page, identifiers, recent mail arrivals, linked intents
lor day [<date>]                        # planned, actual, commitments, and the day page
lor graph [--starving] [--waiting]
lor intent show <id>
lor score explain <intent_id>

# write: free for the agent
lor page create <slug> --title "..." [--entity <id> | --intent <id>] < body.md
lor page update <slug> --base <N> [--note "..."] < body.md
lor page update <slug> --base <N> --find "..." --replace "..."
lor page update <slug> --append < fragment.md
lor entity upsert --kind K --name "..." [--email a@b] [--alias "..."]
lor log "<text>" [--intent <id>]        # a source=agent observation: something the agent did or noticed
lor intent create --type T --title "..." [--parent <id>] [--due <t>] [--waiting-on <entity_id>] [--assign agent]
lor link --from <id> --to <id> --kind measures|blocks|derived_from
lor task assign <id> owner|agent
lor done <task_id> [--memo "..."]       # the agent closes its own tasks; the memo is the outcome
lor propose time --block <id> --intent <id> [--reason "..."]

# the owner's evidence: only when the owner asks in the current thread
lor timer start|stop|pause|resume [--intent <id>] [--title "..."]
lor recategorize <block_id> <intent_id>
lor confirm <date> [--all | --accept <ids> --reject <ids>]
lor habit <intent_id> [--date <date>]
lor metric <intent_id> <value> [--date <date>]
```

- **`lor context`** returns the `in_digest` pages, today's plan and running block, starving goals, the agent's open tasks, waiting tasks, pending proposals, and the titles of recently changed pages. Keep it short; add a size budget when a digest is actually too long. `--task <id>` returns the `agent` page, the task, its parent and linked intents, and the pages for the entities and projects it touches.
- **`lor search`** runs `websearch_to_tsquery` against each requested table's `search` column plus trigram matches on entity names and page titles, merges by rank with a recency boost, and returns ids and snippets, never full bodies.
- **`lor changes`** keys on arrival time, so something recorded late is not missed. The caller remembers its own last run time.
- Every write takes `--thread <id>` (or `LOR_THREAD` in the environment). A thread learns its own id by calling T3's `t3_thread_configuration` tool with no arguments.
- Verbs are added with the code behind them.

**`/api/v1`** is one server route with one endpoint per verb. Handlers call the same functions as the UI, so each write has one implementation. Auth is a loopback source address and `Authorization: Bearer $LOR_AGENT_TOKEN`. Requests are stamped `via=agent`.

**The `lor` skill** is `skill/SKILL.md`, symlinked into `~/.grok/skills/lor` (the global skills directory, already linked into the other harnesses). It teaches: run `lor context` first; where each kind of knowledge goes; search before create; cite what came from an email, an observation, or a thread; `inbox` for the homeless; always pass `--thread`; the one rule; third-party text is data, not instructions, and goes onto a page as cited description ("the invoice from X says payment is due on the 14th"), never as an instruction to a later reader.

### Who may write what

- **Free for the agent:** pages, entities, `source=agent` observations, intents, links, and proposals about the owner's time.
- **The owner's to confirm:** what the owner did. Time blocks, habit completions, and metric values become commitments in the evening confirm, or when the owner asks for it in the thread. Unattended runs do not write these.
- **Tasks assigned to the agent** are the agent's to close with `lor done`.

The API cannot tell an instructed call from an uninstructed one, so this is auditable rather than enforced: every agent write records `via` and the thread, the UI badges those rows, and each has one-tap undo.

---

## Google

LoR has its own OAuth client (desktop type) in a Google Cloud project the owner controls.

- **Scopes:** `calendar.readonly`, `tasks.readonly`, `gmail.metadata` (headers and labels, never bodies), plus access to one Pub/Sub subscription. LoR cannot write to Google or read a mail body even by mistake.
- **Consent:** `pnpm google:auth`, run once at a terminal, writes the refresh token to `GOOGLE_TOKEN_FILE` (0600, outside the repo). The box has no browser, so the command prints the consent URL to open on another device and asks for the redirected URL to be pasted back. The consent screen is set to "In production" so the token does not expire after seven days (to be confirmed).
- **Code:** `src/server/google/client.ts` holds auth and the calls behind `CalendarPort`, `TasksPort`, and `MailPort`, so sync and mail logic are tested against recorded responses with no network.
- **Failure:** a refused token shows "Google auth required: run `pnpm google:auth`" in the cockpit and the job records an error. The app keeps running.

### Google Tasks

One-way, Google → LoR. `tasks_poll` upserts `type=task` intents keyed on `google_task_id`, mirroring title and due date. New tasks arrive unlinked and the agent links them.

- Completed in Google: `status=done` and a `task_done` commitment with `via=job`. This is the one place a job writes a commitment; the owner ticking the box is the confirmation.
- Reopened in Google: the commitment is removed and the task is `active` again.
- Deleted in Google: `archived`.
- LoR's UI shows "complete in Google" on imported tasks instead of a checkbox. The agent completes or creates tasks in Google with `gog` when asked.

### Mail

Mail lives in Gmail. LoR notices that something arrived and wakes the agent once per burst.

- **Push.** LoR calls Gmail `users.watch` for the `INBOX` label, which publishes to a Pub/Sub topic when the inbox changes. `lor-web` pulls that subscription in a loop over Pub/Sub's plain HTTP API (a request that waits for messages, repeated), which needs no extra library. The connection is outbound, so nothing listens on the internet. A notification carries no content; it triggers a check.
- **Check.** `history.list` from the stored history id, for messages added to `INBOX` only, then `messages.get format=metadata` for each. Sent mail and drafts are skipped. It runs on a push, once at startup, and after the daily watch renewal. There is no polling. Nothing is lost while LoR is down: Pub/Sub holds notifications and the check reads from the stored position. If Gmail has expired the history id (about a week), list recent messages and take those with no observation.
- **Arrival.** One observation per message: `source=gmail`, `title` = subject, `payload = {gmail_id, thread_id, from, labels, bulk}`. Idempotent on the Gmail id. That is all LoR knows about a message.
- **Bulk.** A message is `bulk` if it has `List-Unsubscribe`, `Precedence: bulk|list|junk`, or a promotions, social, updates, or forums label, unless the sender is a known entity. Bulk is recorded and never wakes the agent. About 78% of the inbox is bulk; roughly ten messages a day are not.
- **Wake.** Non-bulk arrivals go into `wake_queue`, coalesced by Gmail thread. The queue is flushed after `LOR_WAKE_QUIET_S` of quiet, or when its oldest entry is `LOR_WAKE_MAX_S` old, or at once for a sender whose entity has `priority`. A flush is one POST of the Gmail ids to `LOR_WAKE_URL`, which delivers them to the agent's main thread. One flush in flight at a time. If the POST fails (T3 is down, or it already holds 20 deliveries for a busy thread), the rows stay queued for the next flush.
- **The agent reads the mail** in Gmail with `gog`, decides what matters using the `agent` page, and either does nothing, notes it for the morning brief, acts, or asks the owner. What it learns goes onto pages, cited as `mail:<gmail-id>`.
- **Senders.** Unknown senders do not create entities; the agent creates entities for people who matter.
- **Not built:** backfill, a mail index, stored bodies, label tracking, a Mail tab, any model in LoR.

### Agent runs

These live in T3, not in this repo. A T3 project `assistant`, rooted at `~/google/assistant/`, holds an `AGENTS.md` that says to run `lor context` and follow the `agent` page. Neither exists yet; both are created with the memory API.

| Run | Started by | Does |
| --- | --- | --- |
| Main thread | The owner, or a mail wake | The conversation. Updates pages, creates and links tasks, reads new mail, drafts replies, asks before sending. Hands errands to workers. |
| Worker | The main thread | One task assigned to the agent, in whichever project the work belongs to. Starts with `lor context --task <id>`, ends with `lor done <id> --memo "…"`. LoR stores the worker's thread on the task. |
| Nightly | 02:00 local | Writes the day page; files `inbox` entries; links unlinked tasks; notes stale waiting tasks. Reads `lor changes --since` its last run. |
| Morning brief | Start of the waking day | Today's plan, the starving goal, waiting tasks that are due or stale, mail noted overnight, and any change to an `in_digest` page. |

Every run uses the owner's one `gog` login.

The mail wake is a T3 webhook task created from the main thread, so its runs are queued into that thread. T3 serves the hook on the box at `http://127.0.0.1:3773/api/hooks/<id>/<token>`; the URL contains the token, so it is a secret. The hook is tied to that one thread: if the owner starts a new main thread, the hook is recreated there. That thread grows with every wake, which is the reason to add a triage tier if it becomes a problem.

---

## UI

| Route | What |
| --- | --- |
| `/` Today | See "Today" above. |
| `/graph` | Intents grouped by type, with links, score chips, and starve flags. A plain list, no canvas. |
| `/memory` | **Pages:** a tree by slug prefix, a reader that renders links and footnotes, a markdown editor that saves through the same `--base` check as the agent, history with diff and revert, the `in_digest` toggle, and who last changed each `in_digest` page. **People:** entities, their identifiers, page, recent mail arrivals, and the `priority` flag. **Timeline:** every observation for a day. |
| `/capture` | The cockpit: jobs, sources, Google auth, Gmail watch expiry and last push. |

The foundation's `/chat` route is deleted.

## The phone

`buildbot` is headless, so the owner's day happens on the phone.

- **The dashboard** is the Serve listener described in "Architecture". It is built before Today, because Today is used from the phone and the laptop.
- **Ingest** rides the same listener: the sensor POSTs observation envelopes to `http://buildbot:3847/ingest`, and the Serve identity check is the only auth.
- **Android sensor:** an Expo module on `pixel-9a` that uploads app-usage events in batches (read retroactively, so it tolerates being offline), sleep and exercise from Health Connect, places and movement, and a quick-settings tile for the timer. No accessibility-service capture. It is hosted in a thin mobile-only fork of T3 Code or a minimal standalone app, decided while building it.

**Activity blocks need a design before this is built.** The time-block rules above cover manual and calendar blocks only. Open problems:

- Usage events arrive about 15 minutes late and already closed, so there is no "open activity block".
- The one-open-block index means an activity block cannot be open while a timer runs, and what happens when activity overlaps a manual block is undecided.
- A day of phone use is dozens of short app sessions. They need rolling up into a handful of blocks, or the evening confirm stops being 30 seconds.
- Waking hours taken from sleep data change daily and need somewhere to live; config is the env file only.

---

## Security

Privacy of the owner's own data is not a constraint. What matters is credentials, reachability, and third-party text steering an agent that can act.

| Risk | Position |
| --- | --- |
| **A crafted email steers the agent into acting.** | **Accepted by the owner.** Every run reads full third-party text and holds read-write Google access through `gog`. The mitigations are soft: the skill says such text is data, the one rule says to ask before anything irreversible, and bulk mail never reaches a model. Nothing prevents it. Do not add a permission layer. |
| The agent does something unwanted | The one rule. Accepted. What it writes in LoR is versioned or undoable and badged. |
| The agent, or text from an email, rewrites what every run loads | Only the owner sets `in_digest`. The content is not locked: unattended runs are told not to edit those pages, the Memory UI shows who last changed each, and the morning brief reports changes. |
| Secrets in git or logs | Credential files are 0600 and outside the repo. `scrubLog` on structured logs. Never stored in tables. |
| Reachable by others | Binds loopback by default. `/api/v1` also checks the source address. Off-box access is by Tailscale identity only. |
| LoR writing to Google or reading mail | Not possible with its scopes. |

LoR's only egress is Google's APIs and the POST to the T3 webhook.

## Tests and backup

- `pnpm test`: pure functions (scoring, intervals, fill, parsers, zod). No database, no network.
- `pnpm test:db`: runs against `life_on_rails_test` over the socket (`LOR_TEST_DATABASE_URL`), migrations applied once, tables truncated between tests. (Not a rolled-back transaction per test: the code under test opens its own transactions, and the two-rapid-taps case needs two connections that commit.) Skips loudly when the variable is unset. The invariants live in Postgres (the `--base` check, the one-open-block index, the delete rules, generated search columns), so write paths are tested here.
- Google code is tested against recorded API responses in `tests/google/fixtures/`, with tokens removed.
- **Backup:** a nightly systemd timer runs `scripts/backup-postgres.sh` (`pg_dump -Fc`, keep 7). Restore it once into a scratch database to know it works. Undoing a bad migration is: stop the unit, restore the dump.
- **Logs:** JSON lines to journald. Titles and subjects may be logged; secrets never.

## Foundation code to remove or change

| Path | Change |
| --- | --- |
| `src/server/agent/*`, `src/server/functions/chat.ts`, `src/routes/chat.tsx` | Delete. `scrubLog` moves to `src/server/log.ts`. |
| `src/server/google/gog.ts`, `scripts/capture-gog-fixture.sh`, `tests/google/*` | Replace with LoR's Google client; fixtures become recorded API responses. `calendarSync.ts` keeps its shape. |
| `src/db/schema.ts`, `src/db/migrate.ts`, `drizzle/0000_*` | Apply the drops above (including the vector extension and HNSW index) and regenerate. |
| `src/server/jobs/scheduler.ts` | In-memory running flag instead of `pg_try_advisory_xact_lock`; started from the server entry. |
| `src/routes/internal/jobs/tick.ts`, `scripts/systemd/lor-web-watchdog.*` | Delete. |
| `agents/linux-activity/`, `scripts/systemd/lor-linux-activity.service` | Delete; rewritten if a desktop session ever exists. |
| `src/routes/ingest.ts`, `src/server/capture/*` | Drop the heartbeat, `LOR_ACTIVITY_ENABLED`, idle, and bearer-token handling. Ingest is not wired until the phone work. |
| `src/server/attribution/fill.ts`, `segment.ts` | Fill becomes the single `syncCalendarFill(window)`. Activity segmentation waits for the phone design. |
| `package.json` `start`, `scripts/systemd/lor-web.service` | `node` on the built server entry. |
| `.env.example`, `README.md` | Match "Config" above. |

---

## Build order

A default, not a contract. Each item says what gets built and how to tell it works.

### 1. Make the box run

- Install PostgreSQL 18; create a non-superuser role and the databases `life_on_rails` and `life_on_rails_test`.
- Regenerate the first migration with the drops in "Data model". `pnpm db:migrate` and `pnpm db:seed` run against the socket.
- The production server entry; fix `start`; one Node for the shell and the unit.
- The scheduler with one trivial `heartbeat` job.
- The `test:db` harness, with first cases for the migration, the one-open-block index, the delete rules, and the observation upsert.
- The nightly backup, restored once.
- The deletions in "Foundation code to remove or change".

**Works when:** after a reboot and without an interactive login, `/health` reports the database up and a recent `heartbeat` run; killing the node process gets it restarted; both test suites pass.

### 2. Memory API and the `lor` CLI

- Tables: `entity`, `entity_identifier`, `page`, `page_version`; `observation.search`. The seed pages.
- Page `create`, `read`, `search`, `update` in its three forms. Entity `upsert` and `show`. `log`, `search`, `context`, `status`, `changes`, `day`.
- `/api/v1`, the CLI, and the skill.
- In T3: create the `assistant` project, write the `agent` page, start the main thread and the nightly schedule.

**Works when:** from a fresh T3 thread the agent creates a page for a person with a thread citation and updates it. A second thread finds it by search and reads its history, and a write against the old version is refused as `stale-base`. Both threads passed a real thread id.

### 3. Calendar and tasks

- Register LoR's OAuth client and run `pnpm google:auth`.
- The Google client behind `CalendarPort` and `TasksPort`; fixtures re-recorded from the API, including a recurring series.
- `calendar_poll` and `tasks_poll`. No time blocks are created until Today exists.
- The cockpit shows both jobs and the auth state.

**Works when:** after a reboot both polls succeed with no interactive login. A task ticked in Google is done in LoR within one poll, and unticking it reverses that.

Items 2 and 3 do not depend on each other.

### 4. The dashboard on other devices

- The `:3848` listener with the identity check, and `tailscale serve --bg --http=3847 http://127.0.0.1:3848`. T3 Code's `/` on port 80 is left as it is.

**Works when:** the cockpit opens at `http://buildbot:3847` from the phone and the laptop; a request to `:3848` without the owner's identity header is refused; `/api/v1` is not reachable there.

### 5. World model and scores

- Create, edit, link, and archive intents. The new `intent` columns.
- `/graph` as a list, with agent-written rows badged and undoable.
- Score recompute and snapshots. `pnpm db:seed` can load a `seeds/*.json` graph.
- CLI: `graph`, `intent show|create`, `link`, `task assign`, `done`, `score explain`, `habit`, `metric`, `context --task`.

**Works when:** the owner's real goals are entered and `lor score explain` matches the Graph list. The main thread assigns a task to the agent, a worker completes it, and the outcome memo shows on the task.

### 6. Today

- The state machine, `syncCalendarFill`, `confirmDay`, block edit and delete, the remaining-day bar, gap chips, `Reset Visibility`. The calendar poll starts creating fill blocks.
- CLI: `timer`, `recategorize`, `confirm`, `propose time`.

**Works when:** Toggl stays closed for a working week. Two rapid timer taps leave one open block. A manual block started inside a meeting removes the fill ahead of it, and stopping it restores the rest of the meeting. Two polls in a row with an unchanged calendar leave every block id as it was. This is the largest piece of work.

### 7. Mail

- Set up the Pub/Sub topic and subscription and add `gmail.metadata` to LoR's login. Do this first; there is no polling fallback.
- The Gmail watch, the Pub/Sub pull loop, the daily renewal, `mail_check`.
- Bulk rules, `wake_queue`, `entity.priority`, the debounced POST.
- In T3: create the webhook task from the main thread, and write the "what mail is worth raising" section of the `agent` page.

**Works when:** a non-bulk message is on the Timeline within seconds and reaches the main thread within the debounce window, where the agent reads it in Gmail. A newsletter is recorded as bulk and starts nothing. Mail the owner sends is not recorded. Two messages in one thread a minute apart cause one wake. After LoR has been stopped for an hour, the messages that arrived meanwhile appear and cause one wake.

### 8. Memory UI

- `/memory` with Pages, People, and Timeline.

**Works when:** the owner reverts an agent edit in one tap, and edits the `agent` page so that the next run follows the new text.

### 9. Overnight close

- Block merging, missing proposals, score snapshots, catch-up on boot.

**Works when:** a day left untouched is ready for confirm by morning, and a boot that missed 01:30 runs the close once.

### 10. The phone sensor

- Design activity blocks first (see "The phone").
- `/ingest` on the Serve listener; the Android sensor.

**Works when:** a day of phone use shows up as a handful of activity blocks with proposals, and attribution coverage is reported for the first time.

### When wanted

Not scheduled. Each is written when its absence is felt.

- **Wiki maintenance:** rename and merge (rewriting inbound links), backlinks, lint, entity merge. These are what a server-side index of links and citations is for; add `page_link` and `page_citation` then.
- **Library:** books, articles, and papers as `document` rows with text stored per part, cited down to the page with quotes checked against the source. Until then the agent reads a PDF with `pdftotext` and cites it as `url:` or a thread.
- **A mail triage tier:** a cheaper model reading each batch before it reaches the main thread, if mail clutters that thread.
- **Weekly review:** weekly hour adequacy, applying `seeds/*.json` from the UI, a settings screen, a graph canvas, a life-in-weeks strip.
- **Desktop activity:** a Linux agent if a desktop session ever exists, and a Windows agent on `legend`, both on the phone's ingest path. Idle detection comes with them.
- **A digest size budget, retention, embeddings:** when a digest is too long, the disk notices, or search demonstrably misses things.

## Not doing

Multi-user, billing, or hosting for anyone else. A mail client. Two-way Tasks sync. Writing to Google from LoR. Calendar push. Calendar autopilot. iOS. Keystroke, clipboard, or microphone capture for now. Local-first sync, CRDTs, Flutter. Any agent framework in LoR.

## Open questions

1. **Pub/Sub prerequisites.** A topic and pull subscription in LoR's Google Cloud project; permission for Gmail to publish to it; how LoR authenticates to Pub/Sub (the owner's OAuth login with a Pub/Sub scope, or a service account key); whether `gmail.metadata` is enough for `users.watch` and `history.list`.
2. **OAuth consent screen.** `gmail.metadata` is a restricted scope. Whether an unverified app can be "In production" with it for one user, so the refresh token does not expire after seven days.
3. **Host for the phone pieces.** A thin T3 mobile fork or a standalone app. Unverified: whether a self-built T3 client pairs with a stock server, and whether this box can build an Android app.
4. **Activity blocks.** The design called for in "The phone".
5. **Drive files as sources.** Not yet.

Checked on 2026-10-14: T3 serves webhooks directly on the box, with no relay needed; `srvx` 0.11.22 is in `node_modules`; `tailscale whois` works for this user without root.

## Alternatives set aside

- **Extend the 2023 Next app.** Two routers, two ORMs, wrong scoring, built as a SaaS. Rewrite was cheaper.
- **ActivityWatch plus Notion.** No world model and no planned-versus-actual; the join is the product.
- **LoR reading Google through `gog`.** It asked one tool to be both the agent's full-access hands and LoR's read-only reader.
- **The agent inside LoR.** LoR would have to build a tool loop, chat, scheduling, notifications, and a phone client that T3 already has. Reversible later: an in-app agent would call the same `/api/v1`.
- **A deep fork of T3 Code** with LoR's screens built in. Constant merge conflicts against a fast-moving UI. A thin mobile-only fork remains an option for the phone.
- **An approval queue or permission tiers for the agent.** Judged worse than the occasional unwanted action.
- **A Hono worker plus a Vite SPA.** Would isolate the clock from dev reloads; not worth a second process while `pnpm dev` is simply not the clock.
