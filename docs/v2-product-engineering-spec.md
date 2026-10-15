# Life on Rails v2: design and state of the build

| | |
| --- | --- |
| **Owner** | Sumeet Padavala |
| **Rewritten** | 2026-10-14, as one description of the current design; updated the same day as it was built |
| **Repo** | `/home/supadava/projects/life-on-rails` |
| **Runs on** | `buildbot` (Ubuntu 26.04, headless, Tailscale `buildbot.tail47515a.ts.net`) |
| **State** | Built and running on `buildbot` through the overnight close. Signed in to Google (calendar, tasks, contacts, mail headers) on 2026-10-14. See "State of the build". |

This is a personal project on one box with one user. There is no PR plan, review gate, or staged rollout: work goes to `main` when it works. The earlier, much longer spec (history, superseded decisions, the 2023 post-mortem) is not carried here; the v1 text is in git at `e050ace`.

---

## What it is

Life on Rails (LoR) closes the loop between what the owner intends and what the owner actually does. It has two jobs:

1. **A time tracker that knows intent.** It replaces Toggl. It knows the calendar, the goals, and which goals are starving, and it asks for about 30 seconds of confirmation each evening.
2. **The memory and senses of a personal agent.** An assistant the owner talks to in T3 Code reads and writes LoR through a `lor` CLI: a wiki of what it knows, the owner's tasks and goals, the people in the owner's life, and a record of what happened. LoR notices things (calendar, tasks, contacts, mail arriving, phone activity) and holds mail that needs attention until the agent's mail run takes it.

The owner has executive dysfunction, so the system must cost less to keep up than to abandon. That drives the product rules below.

## Who is who

- **T3 Code** is where the owner talks to the agent: threads, devices, auth, models, delegation, the morning brief. Every question the agent has for the owner is asked in a thread.
- **The runner** (`runner/`, systemd timers) starts the unattended runs, nightly and mail, as headless Claude Code. It is not part of LoR and does not touch T3: what needs the owner it flags in LoR.
- **LoR** is what the agent knows, the owner's dashboard, and the senses. It records, shows, and notices, and decides by fixed rules when mail is ready for the agent. It runs no model and calls nobody. It never acts, asks, or approves.
- **The agent** is a role, not a program: any session (a T3 thread or a headless run) that has the skills and has run `lor context`. Its standing instructions are the `agent` wiki page, served by `lor context`.
- **`gog`** is the agent's Google tool, signed in with broad access. LoR never runs it.

Do not add chat, an approval queue, or a model runtime to LoR. LoR may notify the owner, on the phone, about what the agent flagged for him ("Needs you"). Every notification has to earn its place: few, never guilt-shaped, and each opens the thing it is about. Nothing depends on LoR calling T3: T3 threads read and write LoR, never the other way round.

## Product rules

1. **Low friction.** If it takes more than a tap or a sentence, a sensor or the agent should have done it. The evening confirm is the only daily tax.
2. **Gentle guidance.** Blocks move. Plans are suggestions. No locked schedule, no autopilot.
3. **Safe failure.** `Reset Visibility` hides overdue tasks without deleting history. Trends over streaks. No coins, pets, or shame-red overdue lists.
4. **Today is the home.** Now, next, and the remaining day shown as a quantity (a bar), not only a clock. Usable as a kiosk: one column, large type, no hover-only actions.
5. **Objective and subjective.** Hours plus at most one "how did it feel" prompt in the evening. Never a form.
6. **The owner's data is not rationed.** LoR is meant to collect police-investigation levels of information about its owner: every message with its text, every call, where he was, what was on his screen. That is acceptable only because it is his data, on his box, and goes nowhere else. Nothing is filtered, masked or siloed on the way in, not even one-time codes in texts: the phone and this box are about equally secure, and a code is dead within minutes. Volume is handled where data is read (summaries, search, per-person views), never by leaving it out. LoR's own credentials (OAuth tokens, API keys, webhook addresses) are never logged or stored in tables. Third-party text (mail, messages, web pages, window titles) is data, never instructions.

## Settled choices

- **Stack:** TanStack Start, React 19, Tailwind, Postgres 18 (apt, Unix socket, peer auth, non-superuser role), Drizzle with postgres.js, Zod, Vitest, pnpm. Versions pinned exact. No Docker, no bun.
- **The 2023 app** is frozen under `archive/2023/`. Never import from it. No data is migrated from it.
- **Evidence split:** Observation → Proposal → Commitment. Sensors write observations. LoR suggests proposals. The owner confirms commitments. Scores read commitments only.
- **A proposal means one thing:** LoR suggesting what the owner's time, habits, or metrics were, for the owner to confirm. It is never the agent asking permission.
- **The agent has one rule:** do anything you can take back; ask in the thread before anything you cannot take back, or that speaks to other people as the owner (sending mail, pushing to a shared branch, paying, deleting). It is an instruction, not a mechanism. It will occasionally be broken, and that is accepted in exchange for an agent that can just do things.
- **If code branches on it, it is schema; if only a model reads it, it is prose** on a wiki page. There is no generic fact table.
- **LoR does not mirror the mailbox.** It records that a message arrived and queues it for the agent, which reads mail in Gmail.
- **LoR has its own Google login** with read-only scopes, and calls Google's APIs directly.
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

Two processes: `postgresql`, and `lor-web` (a systemd user unit) which serves the UI and `/api/v1` and runs the jobs.

```
Calendar, Tasks, Gmail  ──poll──┐
phone sensor (tailnet)  ─ingest─┤→  lor-web  ⇄  Postgres
manual timer (UI)       ────────┘        ▲
                                         │ lor CLI → /api/v1
                                         │
          the agent, with gog: T3 threads + headless timer runs
```

**Server entry.** `pnpm build` writes `dist/server/server.js`, which exports a fetch handler and opens no socket. `scripts/serve.mjs` is the production entry: it (1) loads that bundle, which pings the database and exits non-zero if it is down, (2) serves the handler and `dist/client` with `srvx`, and (3) is the only thing that sets `LOR_RUN_JOBS=1`, so the scheduler inside the bundle starts once and never under `vite dev`. The systemd unit runs `node` by absolute path on it, not `pnpm`, with `Restart=always` and `EnvironmentFile=%h/.config/life-on-rails/env`.

**Two listeners, one process.**

- `127.0.0.1:3847` serves everything (UI, `/api/v1`, `/health`) with no identity check. It is for use on the box and is never a Serve target.
- `127.0.0.1:3848` is for the owner's other devices. It serves the UI and `/ingest` only, and refuses any request whose `Tailscale-User-Login` header is not `LOR_OWNER_LOGIN`. It is published on its own Serve port, `tailscale serve --bg --http=3847 http://127.0.0.1:3848`, so the dashboard is `http://buildbot:3847`. A port, not a path under T3 Code's `/`: a path prefix would send the app's asset requests to T3. This listener cannot exempt loopback, because Serve's proxy connects from loopback.

**The wall display's listener.** When `LOR_TRMNL_BIND` is set, a third listener on `LOR_TRMNL_BIND:LOR_TRMNL_PORT` (3849) serves the TRMNL X on the home network, which cannot join the tailnet. It routes only the firmware's `/api/setup`, `/api/display`, `/api/log` (to `/trmnl/*`) and `/trmnl/image/*`. Setup hands `LOR_TRMNL_KEY` to the MAC in `LOR_TRMNL_MAC` only (a refused setup logs the MAC); display, log, and the frame image all require it as `Access-Token`. Each display call renders a fresh frame. A timed wake gets Today and `refresh_rate` 900; a wake whose `Update-Source` is a touch (the middle of the bar) redraws the screen last served, or, if that was drawn under 90 seconds ago, the one after it (Today, Goals, Tasks); a non-Today screen gets 300, so Today comes back. Tap and hold send the same request, and hold is the firmware's power-off, so the frame's age is the only way to tell "refresh" from "next". Frames are named `plugin-<screen>-<epoch>`, the firmware's shape, so its on-device cache keeps one per screen for left and right. If rendering fails, the last frame of that screen stays up with its own "Updated" time.

**Local dates.** Neither Node on this box has `Temporal`. One helper module (`src/shared/localTime.ts`) answers "which local day is this instant" and "when does local day D start" using `Intl` in `LOR_TZ`, and everything that needs a day boundary uses it. Days are 23 or 25 hours long twice a year.

**`pnpm dev` is UI only.** Vite's reloads would kill the clock, so the scheduler is started only from the production entry.

**Scheduler.** In-process timers. Each job has an in-memory "already running" flag, so a slow run is skipped rather than doubled. Each run writes a `job_run` row; a job with nothing to do (no mail queued) records no run. On boot, any row still marked `running` is set to `error` (there is one process, so it cannot be live), and a daily job whose time was missed runs once. There is no watchdog, no tick endpoint, and no job lock in the database: if the process dies, systemd restarts it. A repeated failure is logged once, not every interval.

| Job | When | Does |
| --- | --- | --- |
| `calendar_poll` | every 300 s | `events.list` for each calendar ticked in Google Calendar, `singleEvents=true`, from the start of local yesterday to the end of local +2 days. Upserts `google_event` and a `source=calendar` observation, then runs calendar fill. The full-window poll is also how moved and cancelled events are retracted. |
| `tasks_poll` | every 900 s | Upserts `task` intents by `google_task_id`. See "Google Tasks". |
| `contacts_poll` | every 6 h | Google Contacts → entities, incrementally with a sync token. See "People". |
| `mail_check` | every 60 s, once `--mail` is signed in | See "Mail". |
| `score_tick` | hourly | Recomputes scores. |
| `overnight_close` | 01:30 local | Merges adjacent blocks, creates missing proposals, snapshots scores, wakes snoozed tasks. Does not confirm anything and calls no model. |
| `heartbeat` | every 300 s | Proves the clock is alive. |

**Health.** `GET /health` returns 200 whenever the process is listening, with the database state and each job's last successful run. A job is `stale` when that run is older than three intervals. The Capture cockpit shows the same thing plus each source's last arrival and the Google auth state. The cockpit is the only alerting.

**Config** is the env file `~/.config/life-on-rails/env` (mode 0600) and nothing else. There is no profile row; a settings screen can add one later.

```
DATABASE_URL=postgres:///life_on_rails?host=/var/run/postgresql   # socket + peer auth; never 127.0.0.1
LOR_TEST_DATABASE_URL=postgres:///life_on_rails_test?host=/var/run/postgresql   # pnpm test:db only
LOR_BIND=127.0.0.1
LOR_PORT=3847
LOR_SERVE_PORT=3848           # the identity-checked listener behind tailscale serve
LOR_OWNER_LOGIN=sumeet.padavala@gmail.com   # the only Tailscale login that listener accepts
LOR_TZ=America/New_York
LOR_WAKE_START=08:00          # the owner's waking day
LOR_WAKE_END=22:00
LOR_SCORING_WEEKS=2
LOR_CALENDAR_POLL_S=300
LOR_TASKS_POLL_S=900
LOR_CONTACTS_POLL_S=21600
LOR_MAIL_POLL_S=60
LOR_PHONE_COUNTRY_CODE=1      # for numbers written without one
LOR_AGENT_TOKEN=              # bearer for /api/v1; the lor CLI reads it from this file
LOR_WAKE_QUIET_S=180
LOR_WAKE_MAX_S=900
GOOGLE_ACCOUNT=sumeet.padavala@gmail.com
GOOGLE_OAUTH_CLIENT=/home/supadava/.config/life-on-rails/google-client.json
GOOGLE_TOKEN_FILE=/home/supadava/.config/life-on-rails/google-token.json
```

The comments above are for this document: in the real file every comment is on its own line, because systemd does not strip trailing ones. postgres.js treats an empty host as TCP localhost, so `src/db/client.ts` forces the socket directory when the URL has none.

---

## Data model

Single user, so no owner column anywhere. Ids are UUIDv7. Timestamps are `timestamptz`. `src/db/schema.ts` is the reference; `pnpm db:generate` turns it into the SQL under `drizzle/`.

| Table | Holds |
| --- | --- |
| `intent`, `intent_link`, `tag`, `intent_tag` | The world model. `intent` also carries `status` (including `waiting`), `waiting_on_entity_id`, `google_task_id`, `assignee`, `thread_ref`. |
| `observation` | Sensor facts, one envelope for every source. Never scored. Generated columns: `search`; `kind` (`app`, `location`, `steps`, `call`, `sms`, `whatsapp`, `mail`; null for calendar, agent and manual rows); `parties`, who a call, text, WhatsApp message or mail was with, as identifiers (`phone:+1415…`, `email:a@b.com`, `domain:b.com`, `whatsapp_group:…@g.us`; a group message is with the group); and `author`, who wrote an incoming message or mail. `parties` and `author` come from the SQL functions `observation_parties` and `observation_author`. |
| `time_block`, `time_block_observation` | The actual lane. |
| `proposal` | A suggested label for a block. One kind (`time_attribution`); `time_block_id` is never null. |
| `commitment` | What the owner confirmed: `time_spent`, `habit_completion`, `metric_value`, `task_done`, `note`. The only input to scores. |
| `attribution_rule` | App, title, or URL patterns that label blocks. |
| `google_sync_state`, `google_event` | The calendar window, with what decides fill (`transparency`, `event_type`, `self_response`) and who else attended (`attendees`). |
| `entity`, `entity_identifier`, `entity_member` | People, organisations, places, things and groups, how they are recognised (email, phone in E.164, domain, WhatsApp group id), and who is in a group (by identifier, matched at read time). `entity` also carries `google_contact_id`, the birthday, `keep_in_touch_days`, and `priority` (skips the mail wait). |
| `sync_cursor` | Where an incremental sync got to (the Google Contacts sync token). |
| `page`, `page_version` | The wiki. |
| `mail_state`, `wake_queue` | Where the mail check got to, and arrivals waiting for the agent's mail run. |
| `job_run`, `score_snapshot` | What ran, and each intent's score per local date with its explanation. |

`intent`, `intent_link`, `proposal`, and `commitment` carry `via` (`ui` | `agent` | `rule` | `job`) and `source_ref` (for agent writes, the T3 thread id).

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
- **Needs you:** tasks the agent flagged (`--flag`), each with why, until the owner marks it Done or Seen. The phone notifies once per flag.
- **With the agent:** tasks assigned to the agent and their state.
- What the agent wrote says so, and can be deleted like anything else: deleting and undoing an addition are the same act. (Reverting a page edit and un-confirming a block are different, and keep their own controls.)

### State machine

Invariants:

- **At most one open block:** `CREATE UNIQUE INDEX time_block_one_open ON time_block ((true)) WHERE t_end IS NULL`. Paused is `t_end IS NULL AND paused_at IS NOT NULL`.
- Every writer of time blocks takes one advisory transaction lock first (`lockBlocks`), so they run one at a time: two rapid taps on Start leave one open block. Fill is re-synced inside the same transaction, so the invariant below holds at every commit.
- **A pause is never inside a block.** Pausing marks the open block; resuming ends it at the pause and starts a new block with the same label. Hours can therefore always be computed from intervals.
- A timer stopped within a minute was a mis-tap and is dropped.
- **One pending suggestion per block.** A newer one (from the agent, say) expires the older.
- **No overlapping actual seconds.** Calendar blocks are fill only: they occupy time not already taken (see "Calendar fill").
- `time_block.committed` is set only by recategorize, confirm, or an `auto_apply` rule. Never by a poll, ingest, or the overnight close.

| Event | Rows written |
| --- | --- |
| `startManualBlock` | One transaction: lock and close any open block; insert `time_block` (`origin=manual`, `intent_id` = chosen or `Uncategorized`, uncommitted) and a `source=manual` observation. Then sync fill. |
| `stopRunningBlock` | Set `t_end=now` (or the pause time, if paused). Insert a pending proposal (confidence 1.0). Then sync fill. |
| `pauseRunningBlock` | Set `paused_at`. `t_end` stays null. |
| `resumeRunningBlock` | End the paused block at `paused_at`; insert a new open block with the same intent and title. |
| `uncommitBlock` | Delete the block's commitment and put its label back as a pending suggestion. This is the undo for a confirmation, including one the agent made. |
| `editBlock({blockId, tStart?, tEnd?})` | Move a closed non-calendar block's start or end. A `time_spent` commitment on it moves with it. Then sync fill. |
| `deleteBlock` | Delete a non-calendar block, and its commitment first if it has one. Then sync fill. |
| Calendar poll | Upsert events and observations, then sync fill. Calendar blocks always have `t_end` set. |
| `recategorizeBlock` | Close if open. Reject other pending proposals for the block. Insert `commitment time_spent` (`intent_id` required; `value` is a display copy of the hours). Set `time_block.intent_id`, `committed`, `commitment_id`. An earlier `time_spent` for the block is replaced. Then sync fill if the block is a calendar block. |
| `splitBlock({blockId, at})` | Left keeps the id and ends at `at`; right is a new block with the same origin, intent, and committed state. Observations are re-pointed by midpoint. A commitment is split the same way. Two halves of an unconfirmed calendar block are joined again by the next fill sync unless one of them is relabelled. |
| `confirmDay` | Each accepted proposal runs the `recategorizeBlock` transaction with the proposal's intent. Each rejected proposal becomes `rejected`. `all` accepts every other pending proposal for that date whose block has ended. Gaps stay gaps. |
| Overnight close | Merge adjacent uncommitted timer blocks (`manual`, `merged`) with the same intent and a gap of at most 120 s (`origin=merged`). Create missing proposals. |

Phone and place blocks are fill like calendar blocks: they cannot be moved or deleted, only labelled; see "The phone".

**Calendar fill.** One function, `syncCalendarFill(window)`, decides what calendar blocks should exist and changes only what differs. It runs first inside `syncFill`, which also runs phone and place fill, after every calendar poll, every phone upload, and any block write.

- **Taken time** is every timer or manual block plus every committed block. Phone and place fill rank below the calendar, so they never take time from it. An open block is taken from its start with no end, so while a timer runs there is no fill ahead of it; stopping the timer brings the fill back.
- **Events that fill:** timed events that are not cancelled, not declined by the owner, not marked free (`transparency=transparent`), and of type `default` or `fromGmail`. All-day events, focus time, out-of-office, and working-location entries show in the planned lane only.
- **Two events at once:** the shorter event gets the overlapping time (ties go to the earlier start). A one-hour meeting inside an afternoon-long hold keeps its hour.
- **Wanted blocks:** for each event, shortest first, its span minus taken time minus what shorter events already got. Each remaining fragment of a minute or more is one closed, uncommitted `origin=calendar` block with a pending proposal (`intent_id` from a matching rule, else `Meetings`).
- **Auto-apply rules** commit their blocks after the event has ended, never before.
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
- **Project:** `0.6 × tasks done ÷ tasks + 0.4 × min(committed hours ÷ estimate_hours, 1)`. A missing term is dropped. Hours are those committed directly to the project, counted from its start rather than the window, because an estimate is a total.
- **Goal:** `β·children + (1−β)·hours`, with `β = goal_mix` (default 0.7; not `completion_weight`). `children` is the weighted mean of raw child scores over `measures` links into the goal; undefined children are skipped, and no children means the term is dropped. `hours = min(unique hours ÷ (weekly_hour_target × W), 1)` over the goal and all its descendants. A missing term is dropped and the other takes the full weight. Nothing to mix means undefined.
- **Descendant walk:** follow `measures` links from parent to children with a visiting set; an edge that would re-enter it is dropped and logged, so cycles terminate.
- **Hours are recomputed from `[t, t_end]` intervals with a sweep-line merge.** `commitment.value` is display only.
- **Known and intended:** a project whose hours all sit on child tasks scores 0 on its hours term while its parent goal counts them. The explanation shows both.
- **Decay on silence:** `d` = whole local days since the last relevant commitment, or since the intent was created if there is none. `δ = 1` for `d ≤ 2`, else `exp(−(d−2)/7)`. Applied **once, last, on the node being displayed**. Parents always mix raw child scores.
- **Starving:** a goal with `weight ≥ 2` whose hours over the last 7 days are under half its weekly target.
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

Citation targets are `mail:<gmail-id>`, `obs:<id>`, `thread:<id>`, and `url:<https://…>`. The server stores these as written and keeps no index of them; the Notes screen turns them into links when it renders. The one check is a warning in the reply to a write when a marker has no definition or a definition has no marker. Cite sources, never another page.

Write rules:

| Operation | Behaviour |
| --- | --- |
| `create` | Refused (exit 6, naming the existing page) if the slug, `entity_id`, or `intent_id` is taken. |
| `update --base <n>` | Refused as `stale-base`, with the current head, if `n` is not the head version. Otherwise one transaction: insert version `n+1`, set `page.body` and `head_version`. Whole body on stdin, or `--find/--replace` where the text occurs exactly once. `--dry-run` prints the diff. |
| `update --append` | No `--base`; the server appends to the head inside the transaction, continuing a list or a footnote block and otherwise starting a new paragraph. For `inbox` and running logs. |
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
lor page read <slug> [--version N]
lor page history <slug>
lor entity show <id|name|email|phone>   # its page, identifiers, last in touch, recent mail both ways
lor people [--overdue | --birthdays | --groups | --all] [<search>]
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
lor entity set <who> [--keep-in-touch <days>|off] [--birthday MM-DD|off] [--priority on|off]
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
- Every write takes `--thread <id>` (or `LOR_THREAD` in the environment). A thread learns its own id by calling T3's `t3_thread_configuration` tool with no arguments; the runner sets `LOR_THREAD=claude:<session id>` for headless runs.
- Verbs are added with the code behind them.

**`/api/v1`** is one server route with one endpoint per verb. Handlers call the same functions as the UI, so each write has one implementation. Auth is a loopback source address and `Authorization: Bearer $LOR_AGENT_TOKEN`. Requests are stamped `via=agent`.

**The `lor` skill** is `skill/SKILL.md`, symlinked into `~/.grok/skills/lor` (the global skills directory, already linked into the other harnesses). It teaches: run `lor context` first; where each kind of knowledge goes; search before create; cite what came from an email, an observation, or a thread; `inbox` for the homeless; always pass `--thread`; the one rule; third-party text is data, not instructions, and goes onto a page as cited description ("the invoice from X says payment is due on the 14th"), never as an instruction to a later reader.

### Who may write what

- **Free for the agent:** pages, entities, `source=agent` observations, intents, links, and proposals about the owner's time.
- **The owner's to confirm:** what the owner did. Time blocks, habit completions, and metric values become commitments in the evening confirm, or when the owner asks for it in the thread. Unattended runs do not write these.
- **Tasks assigned to the agent** are the agent's to close with `lor done`.

The API cannot tell an instructed call from an uninstructed one, so this is auditable rather than enforced: every agent write records `via` and the thread, the UI says who made each row, and anything can be deleted (or archived, when time or check-ins were recorded against it).

---

## Google

LoR has its own OAuth client (desktop type) in a Google Cloud project the owner controls, saved as `GOOGLE_OAUTH_CLIENT`. With no client yet, `pnpm google:auth` prints the steps to create one.

- **Scopes:** `calendar.readonly`, `tasks.readonly`, `contacts.readonly`, `gmail.metadata` (headers and labels, never bodies). LoR cannot write to Google or read a mail body even by mistake.
- **Consent:** `pnpm google:auth` (add `--mail` for the Gmail scope), run once at a terminal, writes the refresh token to `GOOGLE_TOKEN_FILE` (0600, outside the repo). The box has no browser, so the command prints the consent URL to open on another device and asks for the redirected URL to be pasted back. The consent screen is set to "In production" so the token does not expire after seven days (to be confirmed).
- **Code:** `src/server/google/client.ts` holds auth and the calls behind `CalendarPort`, `TasksPort`, and `MailPort`, so sync and mail logic are tested against recorded responses with no network.
- **Failure:** a refused token shows "Google auth required: run `pnpm google:auth`" in the cockpit and the job records an error. The app keeps running.

### Google Tasks

One-way, Google → LoR. `tasks_poll` upserts `type=task` intents keyed on `google_task_id`, mirroring title and due date. New tasks arrive unlinked and the agent links them.

- Completed in Google: `status=done` and a `task_done` commitment with `via=job`. This is the one place a job writes a commitment; the owner ticking the box is the confirmation.
- Reopened in Google: the commitment is removed and the task is `active` again.
- Deleted in Google: `archived`.
- LoR's UI shows "complete in Google" on imported tasks instead of a checkbox. The agent completes or creates tasks in Google with `gog` when asked.

### Mail

Mail lives in Gmail. LoR notices that something arrived and hands it to the agent's mail run once per burst.

- **Poll.** `mail_check` runs every `LOR_MAIL_POLL_S` (60 s). Gmail push would need a Cloud Pub/Sub topic, a subscription, and a billing account; a minute of latency is invisible behind the wake's quiet period, so LoR polls. Push can come back if the minute is ever felt.
- **Check.** `history.list` from the stored history id, keeping messages added to `INBOX` or `SENT`, then `messages.get format=metadata` for each. Drafts and mail filtered past the inbox are skipped.
- **Sent mail** is recorded the same way, with `direction: "sent"` and the recipients' addresses (`to_emails`), so "last in touch" counts the owner's replies. It never wakes the agent. Nothing is lost while LoR is down: the check reads from the stored position. If Gmail has expired the history id (about a week), list recent messages and take those with no observation.
- **Arrival.** One observation per message: `source=gmail`, `title` = subject, `payload = {gmail_id, thread_id, direction: "in", from, from_email, labels, bulk}`. Idempotent on the Gmail id. That is all LoR knows about a message.
- **Bulk.** A message is `bulk` if it has `List-Unsubscribe`, `Precedence: bulk|list|junk`, or a promotions, social, updates, or forums label, unless the sender is a known entity. Bulk is recorded and never wakes the agent. About 78% of the inbox is bulk; roughly ten messages a day are not.
- **Wake.** Non-bulk arrivals go into `wake_queue`, coalesced by Gmail thread. The queue is due after `LOR_WAKE_QUIET_S` of quiet, or when its oldest entry is `LOR_WAKE_MAX_S` old, or at once for a sender whose entity has `priority`. LoR calls nobody: every two minutes `assistant-mail.timer` runs `runner/mail.sh`, which asks `lor mail pending` (empty until due) and, when there is something, starts a headless run with the Gmail ids. When the run exits cleanly the runner calls `lor mail done <observation ids>`; a failed run leaves the rows for the next tick, and mail that arrived meanwhile stays queued. systemd never starts a second run while one is going.
- **The agent reads the mail** in Gmail with `gog`, decides what matters using the `agent` page, and either does nothing, notes it for the morning brief, acts, or asks the owner. What it learns goes onto pages, cited as `mail:<gmail-id>`.
- **Senders.** Unknown senders do not create entities; the agent creates entities for people who matter.
- **Not built:** backfill, a mail index, stored bodies, label tracking, a Mail tab, any model in LoR.

### People

The people in the owner's life are entities. Google Contacts is where the owner keeps them, so LoR imports it; what LoR adds is when the owner was last in touch with each one, and who is overdue.

- **Import.** `contacts_poll` reads "My Contacts" through the People API (not "Other contacts", which is every address ever emailed), incrementally with a sync token; an expired token means one full listing. Each card becomes an entity: a person, or an organisation when it has only a company name. Names and nicknames become the name and aliases; addresses and numbers become identifiers, numbers in one form (E.164, using `LOR_PHONE_COUNTRY_CODE` for numbers written without a country code). Google is the source for the birthday, and for the name unless one was set in LoR. Notes, postal addresses, and photos stay in Google.
- **Merging.** A card whose address or number belongs to someone the agent already created is linked to that entity rather than duplicated. An address on two cards stays with the first. A card deleted in Google is unlinked from its entity, which is kept with its page and history. The agent can also merge two entities (`lor entity merge`): identifiers, members, page, waiting tasks and place rules move to the kept one, which keeps its name; two Google-linked entities are refused (merge them in Google).
- **Names.** A name set in LoR (`lor entity set --name`) is pinned (`entity.name_pinned`): a contacts sync keeps it and files Google's name as an alias, because the owner's contact names are often nicknames he wants to keep.
- **Last in touch** is worked out when it is read, by matching identifiers, so mail that arrived before a contact was imported still counts. The channels: mail from them, mail the owner sent them, a meeting they attended that has ended (unless the owner declined), and a call, text or one-to-one WhatsApp message. It is one rule: an observation whose `parties` include one of the entity's identifiers. An organisation matches by its domain. A group message is with the group, so it counts for the group entity and not for each member.
- **Keeping in touch.** The owner can set a cadence per person (`keep_in_touch_days`). Anyone past it is overdue; never in touch counts as overdue by the cadence. The digest lists the most overdue and the birthdays in the next seven days, and the morning brief mentions them.
- **Who shows.** The People screen shows, by default, people who matter: those with a page, a cadence, or priority, those the agent created, and anyone in touch in the last 90 days. Everyone else is a search away.
- **Groups.** Each WhatsApp group with a message becomes a `group` entity, found by its chat id. WhatsApp is the source of its name (a rename keeps the old name as an alias) and of its current members, refreshed on each import into `entity_member`. Groups are their own list on the People screen, not among the people; a cadence works on a group too.
- **CLI:** `lor people [--overdue | --birthdays | --groups | --all] [<search>]`, `lor entity set <who> --keep-in-touch <days>|off --birthday MM-DD|off --priority on|off`; `lor entity show` includes the last contact.

### Agent runs

The owner talks to the agent in T3: the project `assistant`, rooted at `~/google/assistant/`, whose `AGENTS.md` says to load the `lor` skill, run `lor context`, and follow the `agent` page. Unattended runs start in the same directory as headless Claude Code (`claude -p`, Opus, permissions bypassed), so they have the same skills and page. What each run does is written on the `agent` page, so the owner can change it in Notes.

| Run | Started by | Does |
| --- | --- | --- |
| Main thread (T3) | The owner | The conversation. Updates pages, creates and links tasks, drafts replies, asks before sending. Hands errands to workers. |
| Worker (T3) | The main thread | One task assigned to the agent, in whichever project the work belongs to. Starts with `lor context --task <id>`, ends with `lor done <id> --memo "…"`. LoR stores the worker's thread on the task. |
| Morning brief (T3) | 08:00, a T3 schedule bound to the main thread | Today's plan, the starving goal, the first action, tasks due or stale, mail noted overnight, and any change to an `in_digest` page. |
| Nightly (headless) | `assistant-nightly.timer`, 02:00 Eastern | Writes the day page; files `inbox` entries; links unlinked tasks; notes stale waiting tasks. Reads `lor changes --since 24h`. |
| Mail (headless) | `assistant-mail.timer`, every 2 min, only when `lor mail pending` is due | Reads the new mail in Gmail; does nothing, notes it for the brief, acts, or flags a task for the owner. |

Every run uses the owner's one `gog` login.

**Why headless.** Unattended runs in T3 were turns in a chat thread: the mail ones would have filled the main thread, and LoR depended on a T3 hook address and T3's scheduler. Now the conversation is T3's and nothing else is. A headless run's record is what it wrote to LoR (credited to `claude:<session id>`); its transcript is in `~/.claude/projects/`, and `claude -r <session id>` in `~/google/assistant` reopens it. Logs: `journalctl --user -u assistant-mail -u assistant-nightly`.

**Reaching the owner.** A run that needs the owner flags a task: `lor intent create --type task --title "..." --flag "<why>"` or `lor task flag <id> "<why>"`. It shows under Needs you on Today and in `lor context` (so any T3 thread knows), and the phone app notifies once: each sync calls `POST /device/notifications`, which hands over flagged tasks not yet handed over and marks them (`intent.notified_at`). The phone syncs about every 15 minutes, so that is the latency; nothing pushes. The app asks for none of these until notifications are allowed on the phone.

--- | --- | --- |
| Main thread | The owner, or a mail wake | The conversation. Updates pages, creates and links tasks, reads new mail, drafts replies, asks before sending. Hands errands to workers. |
| Worker | The main thread | One task assigned to the agent, in whichever project the work belongs to. Starts with `lor context --task <id>`, ends with `lor done <id> --memo "…"`. LoR stores the worker's thread on the task. |
| Nightly | 02:00 local, a fresh thread | Writes the day page; files `inbox` entries; links unlinked tasks; notes stale waiting tasks. Reads `lor changes --since 24h`. |
| Morning brief | Start of the waking day, in the main thread | Today's plan, the starving goal, the first action, tasks due or stale, mail noted overnight, and any change to an `in_digest` page. |

Every run uses the owner's one `gog` login.

**Schedules are created from the main thread,** by the agent, with T3's `schedule_task`, so they inherit that thread's model. LoR does not create them.

**The mail wake** is a T3 webhook task created from the main thread, so its runs are queued into that thread. T3 serves the hook on the box at `http://127.0.0.1:3773/api/hooks/<id>/<token>`; the address contains the token, so it is a secret. T3 tells an agent the address only when T3 Connect is on; otherwise the owner copies it from T3's scheduled-task settings. The hook is tied to one thread: if the owner starts a new main thread, the hook is recreated there. That thread grows with every wake, which is the reason to add a triage tier if it becomes a problem.

---

## UI

Desktop first, with a left rail; on a phone the rail becomes a top strip and the screens stack. Dark only. The look is "Elite: Mostly Harmless" (`docs/design/README.md`): a star atlas in Elite: Dangerous's menu language, turned down.

| Route | What |
| --- | --- |
| `/` Today | See "Today" above. A day gone by also shows the nightly `days/<date>` page and the Timeline: every observation recorded that day (`/?obs=<id>` opens the day of one). The waking day is an arc from rising to setting with the remaining time under it (a straight bar on a phone); planned and actual lanes; Needs you, Due soon (check and un-check), and Goals this week. |
| `/tasks` | Every task, Linear-style. Views: Open, Waiting, With the agent, Later, Done. Filters: what it counts toward (a goal includes its projects' tasks), due, who added it, needs you, text. Group by due, what it counts toward, status, or nothing; sort by due, newest, recently changed, title. Keys: `j`/`k`, `Enter`, `x` select, `d` done, `/` filter, `Esc`. Several selected change together: done, due, Later (until a date), what it counts toward, whose, delete. The side panel edits in place and says who added the task and when. Tasks from Google Tasks keep their title, notes, due date and deletion in Google. |
| Quick add | `q` on any screen, or the box at the top of Tasks, after Todoist: one line, with the understood parts marked as they are typed. `#name` what it counts toward (suggested as typed), `@agent` gives it to the agent, `@person` makes it waiting on them, and a date in words (`today`, `fri`, `next fri`, `in 2 weeks`, `oct 20`, `10/20`; `sat`, `sun` and `wed` only after `on`/`by`/`due`/`next`). Each understood part is a chip whose × keeps it as text. `src/shared/quickAdd.ts`, pure. |
| `/goals` | What the time is for: each goal with its week's hours against target, and the projects, habits and metrics under it. Tasks show only as counts that open Tasks filtered to that goal or project. A side panel for the selected one: its facts, Did it, and the editor. |
| `/people` | A table of who matters, keeping in touch, due a hello, birthdays soon, everyone, and groups: when the owner was last in touch and how, the birthday, a cadence picker, and the `priority` flag. Each person has a hub (`/people?id=`): their page, last in touch by channel, what waits on them, their latest messages and mail headers, and their groups or members. |
| `/notes` | The wiki. An index by folder (dated folders folded, newest first) with a filter, what links here and what it links to, and the page itself in a CodeMirror 6 editor (`src/components/NoteEditor.tsx`) over the stored markdown, byte for byte. Live preview after Obsidian: away from the cursor's line, markdown symbols hide, `[[slug]]` reads as the page's title, citations as superscript keys, and sources (`mail:`, `url:`, `thread:`, `obs:`) open on a click. `[[` suggests pages (or a new one); following a link to a page nobody wrote starts it. It saves a moment after typing stops, through the same `--base` check as the agent; a run of the owner's typing within ten minutes is one version (the head is rewritten and renumbered, so version numbers can skip and a stale base is still refused). Versions stay whole snapshots; the intelligence is a three-way merge (`src/shared/merge.ts`): by line, and by word where both sides touched the same lines (a paragraph is one line), with an addition next to an edit not counting as a clash. When the page moved on under the owner's typing, the two are merged, and only passages both changed the same words of are put to the owner (keep yours, theirs, or both). The agent's writes are still refused on a stale base, by choice. Lines the agent last wrote carry a thin cyan mark (`pageAuthors`, walking the versions). History with diff, revert, and "take back" on any version: undo what it changed and keep every edit since (`takeBackVersion`; refused when later edits changed the same words), taking back the agent's last edit, the `in_digest` toggle. **Graph:** every page as a body on a star map, every `[[link]]` a line, laid out by a deterministic force layout on the server (`src/shared/wiki.ts`). |
| `/search` | One search over notes, people, goals and tasks, and the record; the global search box lands here. |
| `/status` | The cockpit: database, Google auth, mail, jobs, sources. Linked from the rail's foot, which says whether every job is healthy. |

There is no chat screen: the conversation is a T3 thread.

## The phone

`buildbot` is headless, so the owner's day happens on the phone. The phone runs a small app of its own, `android/` in this repo: Kotlin and Jetpack Compose, sideloaded, package `com.spadaval.lor`. Besides the sensors and the timer it shows the "Needs you" notifications (see "Agent runs"), once the owner allows notifications in its Settings.

**What it collects** (each sensor can be switched off in the app, and each needs a permission the owner grants there):

| Sensor | From | Observation (`source=android`) |
| --- | --- | --- |
| App usage | The system's usage events (Usage access) | One per foreground session: `app` = package, `title` = app name, `payload.kind = "app"`. Back-to-back sessions of one app within 30 s are one; the launcher and system UI are left out. |
| Calls | The call log | `payload = {kind: "call", direction, phone, duration_s, name?}`. Never audio. |
| Texts | The SMS store | `payload = {kind: "sms", direction, phone, text}`. The first run reads the whole store. RCS chats live inside Google Messages and are not visible. |
| Sleep, exercise, steps | Health Connect | `kind: "sleep"` (with minutes per stage), `kind: "exercise"`, and one `kind: "steps"` per local day whose end grows as the count does. |
| Location (off by default) | The fused location provider | One fix per sync: `kind: "location"`, latitude, longitude, accuracy. |
| WhatsApp (off by default) | WhatsApp's nightly local backup, `Android/media/com.whatsapp/WhatsApp/Databases/msgstore.db.crypt15` (All files access) | Not an observation on the phone: when the file changes and the network is unmetered, the app uploads it, still encrypted, to `POST /device/whatsapp-backup?device=`. The server keeps only the newest (`LOR_WHATSAPP_DIR`), and the `whatsapp_import` job decrypts it with the owner's 64-digit backup key (`LOR_WHATSAPP_KEY_FILE`, the key alone, 0600) and records one observation per message: `{kind: "whatsapp", direction, phone, chat, chat_name, group, type, text}`, id from WhatsApp's own message id, re-reading a day behind the newest one recorded. The backup must be WhatsApp's end-to-end encrypted one with the 64-digit key: with a password the key stays with Meta, and without end-to-end encryption it stays in WhatsApp's private folder. |

**Messages.** Texts and WhatsApp messages keep their words (product rule 6). They count toward "last in touch" through the phone number, except group chats; message text is in `observation.search`; `lor entity show` lists the latest few with each person or group (`recent_messages`, text cut to 300 characters, with who wrote each), a group's `members`, and the `groups` a person is in. The day view's timeline shows a day's messages as one line per conversation, with a count. Digests and `lor changes` show counts and numbers, never the words.

**How it gets to LoR.** Every 15 minutes (WorkManager's shortest period) each sensor collects what is new since its own cursor into a local queue, and the queue is uploaded to `POST /device/ingest` in batches. Every observation has a stable id derived from what it is (a call-log row, an app session's start), so a retry or a re-read is the same row, and a session that grew longer extends its row. The queue keeps working offline. The server accepts only device sources, checks the batch's device name, and stores phone numbers in E.164 so calls and texts count toward "last in touch".

**Also in the app:** a Timer tab and a quick-settings tile (tap to start a timer, tap again to stop) through `GET /device/state` and `POST /device/timer`; a Dashboard tab showing the web dashboard; and settings for the server address and the phone's name.

**Auth.** The app talks to `http://buildbot.tail47515a.ts.net:3847`, the Serve listener, so every request carries the owner's Tailscale identity and is refused without it. `/device/*` is not under `/api/`, so it is routed there.

**Building and installing.** `cd android && ./gradlew assembleDebug`, then `adb install -r app/build/outputs/apk/debug/app-debug.apk` over wireless debugging: the phone is paired with this box and reachable over Tailscale at `100.83.51.27` (the port changes each time wireless debugging is switched on; `nmap -p 30000-50000` finds it). `testDebugUnitTest` runs the session-building tests on the JVM. The T3 emulator image (`start-t3-emulator`, run under `sg kvm`) works for trying it against the test database at `http://10.0.2.2:3857`.

**Activity blocks** (`src/server/attribution/activity.ts`, pure; `fill.ts`, the sync). The actual lane fills itself, in order of precedence: the owner's own blocks (timer, manual, anything confirmed), then calendar events, then phone app use, then stays at known places. `syncFill` runs the three fills in that order; each sees what outranks it as taken, keeps a block's id while its span is unchanged, and applies a rule's label only where nobody has suggested another. It runs after every phone upload (over the time the upload covers), after every calendar poll, and inside every block write.

- **Phone blocks** (`origin = auto_activity`). Each app session takes the label of the first rule matching its package (`attribution_rule.match_app`). Same-label sessions under 10 minutes apart join; a session under 2 minutes inside a block belongs to it (a reply in the middle of work); a block with under 3 minutes on screen is a glance and is dropped. The title is the breakdown ("T3 Code 14m · Reddit 1m"), and the block links to its sessions.
- **Place blocks** (`origin = place`). Places are entities of kind `place` with `lat`, `lon`, `radius_m` (default 150 m). A stay is a run of fixes at one place no more than 75 minutes apart, padded 10 minutes at each end but never past a fix somewhere else; fixes vaguer than 250 m are ignored. A stay becomes blocks only where nothing else claims the time, so an afternoon at the office minus the Reddit is "probably working". A rule on the place (`match_place`) labels it, at confidence 0.4. `lor places` also lists unnamed places the phone keeps visiting, with rough hours, so the agent can ask what they are.
- **Unlabelled is grey.** A phone or place block with no rule has no intent and no proposal: it shows on Today, never counts toward "to confirm", and can be labelled by hand, with "remember" making a rule for the app or the place.
- **Glances** are not blocks but are counted: Today and `lor day` show screen time, pickups (a session after 2 minutes of nothing), and glances (pickups over within 2 minutes). They measure distraction, not time.
- **Not yet:** waking hours from sleep, a desktop sensor.
---

## Security

Privacy of the owner's own data is not a constraint: collecting everything is the point (product rule 6). The phone and this box are treated as one trust zone of about equal security. What matters is LoR's credentials, who can reach the box, and third-party text steering an agent that can act.

| Risk | Position |
| --- | --- |
| **A crafted email steers the agent into acting.** | **Accepted by the owner.** Every run reads full third-party text and holds read-write Google access through `gog`. The mitigations are soft: the skill says such text is data, the one rule says to ask before anything irreversible, and bulk mail never reaches a model. Nothing prevents it. Do not add a permission layer. |
| The agent does something unwanted | The one rule. Accepted. What it writes in LoR is versioned or undoable and badged. |
| The agent, or text from an email, rewrites what every run loads | Only the owner sets `in_digest`. The content is not locked: unattended runs are told not to edit those pages, Notes shows who last changed each, and the morning brief reports changes. |
| LoR's credentials in git or logs | Credential files are 0600 and outside the repo. `scrubLog` on structured logs. Never stored in tables. Codes and links inside the owner's own messages and mail are his data, not LoR's credentials, and are stored like the rest. |
| Reachable by others | Binds loopback by default. `/api/v1` also checks the source address. Off-box access is by Tailscale identity only. |
| LoR writing to Google or reading mail | Not possible with its scopes. |

LoR's only egress is Google's APIs.

## Tests and backup

- `pnpm test`: pure functions (scoring, intervals, fill, parsers, zod). No database, no network.
- `pnpm test:db`: runs against `life_on_rails_test` over the socket (`LOR_TEST_DATABASE_URL`), migrations applied once, tables truncated between tests. (Not a rolled-back transaction per test: the code under test opens its own transactions, and the two-rapid-taps case needs two connections that commit.) Skips loudly when the variable is unset. The invariants live in Postgres (the `--base` check, the one-open-block index, the delete rules, generated search columns), so write paths are tested here.
- Google code is tested against API responses in `tests/google/fixtures/`. They are hand-written in the API's shape until LoR's login exists to record real ones.
- `scripts/ui-check.mjs` drives the screens in headless Chrome against a server on the test database (`scripts/ui-server.sh`).
- **Backup:** a nightly systemd timer runs `scripts/backup-postgres.sh` (`pg_dump -Fc`, keep 7). Restore it once into a scratch database to know it works. Undoing a bad migration is: stop the unit, restore the dump.
- **Logs:** JSON lines to journald. Titles and subjects may be logged; secrets never.

## Where things are

```
src/shared/            envelope, ids, local time, line diff
src/db/                schema, client, migrate, seed (and the seed pages)
src/server/            env, logging, status, boot
src/server/jobs/       scheduler, the job list, overnight close
src/server/google/     LoR's login, Calendar/Tasks/Contacts/Gmail calls, the syncs
src/server/attribution/ fill, the block state machine, rules, interval math
src/server/scoring/    formulas (pure) and recompute
src/server/memory/     pages, entities, search, the digest, changes
src/server/mail/       check, bulk rules, the wake queue
runner/                the unattended runs (not LoR): headless Claude Code
src/server/api/v1.ts   one endpoint per lor verb
src/server/functions/  what the screens call
src/routes/            Today, Graph, Memory, Capture, /health, /api/v1
cli/lor.mjs            the agent's CLI
skill/SKILL.md         the lor skill
scripts/               serve.mjs, google-auth, seed-graph, host setup, systemd units, UI check
```

---

## State of the build

Built on 2026-10-14, in this order, each checked as described.

| | Built | Checked by |
| --- | --- | --- |
| 1 | Postgres 18, migrations, the production server, the scheduler, backups | The service survives `kill -9`, exits non-zero with no database, and a nightly dump restores. Not yet checked: an actual reboot. |
| 2 | Memory API, `lor`, the skill, the `assistant` T3 project | Two fresh T3 threads: one stored and updated a person with a thread citation; the other found the page by search, had a stale write refused, and ran a task end to end. |
| 3 | Calendar and tasks sync | Tests against hand-written API responses. **Not yet run against Google:** needs the owner's OAuth client. |
| 4 | The owner-only listener behind `tailscale serve` on port 3847 | A request through Serve from this box got the identity header and the page; without the header it is refused; `/api/` is not routed there. Not yet checked from the phone. |
| 5 | World model, scores, Graph | Tests, and a browser pass that added a goal, a habit, and a task and checked the numbers. |
| 6 | Today | Tests for every state-machine row and fill rule, and a browser pass: timer, fill, relabel, remember a title, accept all. Not yet: a week of real use. |
| 7 | Mail notifications | Tests with a fake mailbox. **Not yet run against Google.** |
| 8 | Memory screen | A browser pass: read, undo the agent's edit, a conflicting save, history, revert, people, timeline, search. |
| 9 | Overnight close | Tests; it ran once on the boot after it was added. |
| 10 | People: contacts import, last in touch, keep-in-touch cadences, birthdays | Tests with a fake Contacts account (import, merge, edits, deletions, expired token) and for every channel of "last in touch"; a browser pass over the People screen. **Not yet run against Google.** |
| 14 | Messages: texts keep their words (whole SMS store on first run); WhatsApp's encrypted backup uploaded by the phone, decrypted and imported by `whatsapp_import`; message text searchable; `recent_messages` on entities; group chats left out of last in touch | Pure tests for crypt15 (format checked against wa-crypt-tools' source: varint header size, IV in field 3, HKDF key) and for reading msgstore (groups, @lid, system notices); database tests for search, recent messages and a repeat import; a 300 MB upload streamed to disk; on the emulator against the test server, a sync uploaded a text with its words and a backup that the job imported. Installed on the phone; the first real backup (14 Oct) imported 164,982 messages from 248 chats. |
| 15 | One model for who an exchange was with: generated `observation.kind`, `parties` and `author`, indexed; last in touch, recent mail and recent messages read them. WhatsApp groups as `group` entities with members (`entity_member`); `lor people --groups`; the timeline groups messages by conversation | Database tests for groups (members, @lid, pending invites, rename), the timeline and mail parties; the people tests unchanged and passing. |
| 13 | Activity blocks: phone app use and stays at known places fill the actual lane by precedence; glances counted | Pure tests for grouping, stays, unknown places, and phone stats; a database test of precedence, rules, the agent's label surviving a re-sync, and "remember"; a browser pass; run over two real days (11 phone blocks). |
| 12 | Needs you: flagged tasks on Today, in the digest, and as phone notifications; unattended runs no longer touch T3 | Tests for flag, notify-once, done, and seen; a browser pass over the Today card; on the emulator against the test server, a sync showed the notification. |
| 11 | The phone app: app usage, calls, texts, Health Connect, location, the timer tile, settings | JVM tests for session building; on the emulator, a sync uploaded app sessions and calls to the test database and the tile started and stopped a timer. Installed on the phone over wireless adb; permissions are granted on the phone. |

**Waiting on the owner**

1. **A reboot,** whenever convenient, to see everything come back without a login.

**Not built**

- **Waking hours from sleep data** (the waking day is still `LOR_WAKE_START`/`LOR_WAKE_END`).

### When wanted

Not scheduled. Each is written when its absence is felt.

- **Wiki maintenance:** rename and merge (rewriting inbound links), backlinks, lint, entity merge. These are what a server-side index of links and citations is for; add `page_link` and `page_citation` then.
- **Library:** books, articles, and papers as `document` rows with text stored per part, cited down to the page with quotes checked against the source. Until then the agent reads a PDF with `pdftotext` and cites it as `url:` or a thread.
- **A mail triage tier:** a cheaper model reading each batch before it reaches the main thread, if mail clutters that thread.
- **Weekly review:** weekly hour adequacy, applying `seeds/*.json` from the UI, a settings screen, a graph canvas, a life-in-weeks strip.
- **Desktop activity:** a Linux agent if a desktop session ever exists, and a Windows agent on `legend`, both on the phone's ingest path. Idle detection comes with them.
- **A digest size budget, retention, embeddings:** when a digest is too long, the disk notices, or search demonstrably misses things.

## Not doing

Multi-user, billing, or hosting for anyone else. A mail client. A contacts editor (contacts are edited in Google). Two-way Tasks sync. Writing to Google from LoR. Calendar push. Calendar autopilot. iOS. Keystroke, clipboard, or microphone capture for now. Local-first sync, CRDTs, Flutter. Any agent framework in LoR.

## Open questions

1. **OAuth consent screen.** `gmail.metadata` is a restricted scope. Whether an unverified app can be "In production" with it for one user, so the refresh token does not expire after seven days.
2. **Drive files as sources.** Not yet.

## Alternatives set aside

- **Extend the 2023 Next app.** Two routers, two ORMs, wrong scoring, built as a SaaS. Rewrite was cheaper.
- **ActivityWatch plus Notion.** No world model and no planned-versus-actual; the join is the product.
- **LoR reading Google through `gog`.** It asked one tool to be both the agent's full-access hands and LoR's read-only reader.
- **The agent inside LoR.** LoR would have to build a tool loop, chat, scheduling, notifications, and a phone client that T3 already has. Reversible later: an in-app agent would call the same `/api/v1`.
- **A fork of T3 Code** with LoR's screens or the phone sensor built in. Constant merge conflicts against a fast-moving UI; the phone has a small app of its own instead.
- **Expo for the phone app.** Every sensor needed native code anyway (usage events, the call log, the tile, background work), so the app is plain Kotlin.
- **An approval queue or permission tiers for the agent.** Judged worse than the occasional unwanted action.
- **A Hono worker plus a Vite SPA.** Would isolate the clock from dev reloads; not worth a second process while `pnpm dev` is simply not the clock.
