# The demo set

A made-up life for the test server (`life_on_rails_test`), full enough that every
dashboard screen and the phone's panels draw with realistic volume. Names, numbers,
places and mail addresses are invented; nothing comes from the real database.

```
LOR_FAKE_NOW=2026-11-17T15:00 node .claude/skills/verify-lor/verify.mjs up   # the test server, its clock fixed
LOR_FAKE_NOW=2026-11-17T15:00 pnpm demo:test                                  # empty the test database, load this set
```

`pnpm demo:test` runs `verify.mjs demo`, which runs `scripts/demo.ts`. It refuses any
database but `life_on_rails_test`, empties it, and writes every row through LoR's own
write paths (`createIntent`, `flagTask`, `completeTask`, `upsertEntity`, `createPage` and
`updatePage`, the timer, `ingest` for the phone, `syncCalendars`, `syncTasks` and
`mailCheck` with fake ports), each at the clock time it happened. Two kinds of row have
no write path and are inserted directly: a group's members (only a WhatsApp backup
writes them) and the jobs' last runs. It takes about two seconds.

- `demo.json`: goals, tasks, people, the calendar, the phone's day, messages and mail.
- `pages/*.md`: the wiki. A file is `slug:`, `title:`, optionally `entity:` or `intent:`
  (a key from `demo.json`), then `---` and the body. `lisbon-1..5.md` are the five
  versions of one page; `brief-*.md` and `day-*.md` are dated when loaded.

## The clock

Every time in the set is relative to the clock's local day (`LOR_TZ`): `"d0 09:30"` is
09:30 on the clock's day, `"d-2 18:00"` two days earlier, `"d+3"` a date. The clock is
`LOR_FAKE_NOW` when set, else now, so load with the same `LOR_FAKE_NOW` the server runs
with (or neither). Load again on another day: a set loaded yesterday reads a day stale.

On the test database only (`src/server/clock.ts`):

- `LOR_FAKE_NOW=<time>` fixes the test server's clock for every request.
- `?at=<time>` on any page fixes that tab's clock. Links keep it, and the page's own calls
  carry it, so the tab stays at that time. The page stops its ticking clocks
  (`src/components/clock.tsx`).
- A time is `09:00` (that time on the server clock's day), `2026-11-17T09:00` (local),
  or an instant with an offset.

On any other database, `LOR_FAKE_NOW` and `at=` do nothing.

The day's blocks are written for the whole day. At 09:00, Today shows the rest of the
day's timer, phone and place blocks ahead of now. That is the same data drawn earlier,
not a day in progress.

## Variants

`?data=<name>` on any GET (a page, or `/device/home`) moves the test database to a named
variant: a small change over the same base, applied at the request's clock. One is
applied at a time: asking for another undoes the current one first. `data=base` goes
back to the base. Links keep `data=` like `at=`. (`src/server/demo.ts`)

| Variant | Change over the base |
|---|---|
| `base` | none |
| `running` | a timer on "Draft the offline maps spec", started 47 minutes before the clock |
| `paused` | the same timer, paused 12 minutes before the clock |
| `noflags` | no task flagged (the base has three) |
| `failing` | every job's last run is an error |

## Which rows make which screen state

| Screen state | Link (on `:3857`) | Rows that make it |
|---|---|---|
| Today before the waking day | `/?at=00:05` | wake window 08:00 to 22:00; today's events |
| Today in the morning | `/?at=09:00` | standup at 09:30, so "next … in 30m" |
| Today mid-afternoon | `/?at=15:00` | lunch and design review behind, dentist ahead; timer, phone and place blocks |
| Today in the evening | `/?at=21:30` | Focus time (transparent, so planned but not actual) |
| Today with a running timer | `/?at=15:00&data=running` | variant `running` |
| Today with a paused timer | `/?at=15:00&data=paused` | variant `paused` |
| Today on a past day | `/?date=<d-1>` | yesterday's timer blocks, a run at Kezar track, `days/<d-1>` |
| A suggestion to confirm | `/` | the agent's proposal on the YouTube phone block ("Portuguese lesson") |
| Needs you, three flags | `/`, `/tasks` | "Reply to the App Store review notes", "Find a physio…", "Organise Sam's birthday dinner" |
| Needs you, empty | `/?data=noflags` | variant `noflags` |
| Due soon, past due and done today | `/` | due before today: the tap, the emergency fund, the crash, unit 8, the parking permit; four done today |
| Tasks: waiting | `/tasks?view=waiting` | three, on Lena, Inês and Northwind Accounting |
| Tasks: with the agent | `/tasks?view=agent` | six; three queued this morning, two with a thread |
| Tasks: later | `/tasks?view=later` | four, none of them dated |
| Tasks: done | `/tasks?view=done` | five, four of them today |
| Tasks from Google | `/tasks?from=google` | three from the Google Tasks sync, one past due |
| Tasks with no date | `/tasks` | about a dozen open tasks without a due date |
| Goals with hours this week | `/goals` | Field Guide, half marathon, Portuguese, bookshelf have timer hours |
| Goals with none | `/goals` | house, friends, finances: habits and tasks only |
| Habits and a metric | `/goals` | seven habits with completions over the week; Energy, 1 to 5 |
| People: important | `/people` | twenty people, two organisations, three short codes |
| People: due a hello | `/people?filter=overdue` | cadences past due (Marcus, Aisha, Rosa, Sam, Grandpa Joe, …) |
| People: birthdays | `/people?filter=birthdays` | Sam in 3 days, Theo 12, Clara 18, Priya 26 |
| People: groups | `/people?filter=groups` | Saturday Run Club, with four members and a message |
| A person with messages and mail | `/people` → Maya Okafor or Daniel Reyes | texts, WhatsApp, mail both ways, a meeting (Daniel) |
| Notes, pinned | `/notes` | `agent`, `messaging-rules`, `me` are in the digest |
| A page with history and a diff | `/notes?slug=topics/lisbon-trip` | five versions: owner, agent, owner, agent, owner |
| Notes graph | `/notes?view=graph` | about thirty pages linked with `[[…]]` |
| The brief | `/notes?slug=briefs/<today>` | a short brief today; a long one on this week's Monday |
| Day write-ups | `/notes?slug=days/<d-1>` | `days/<d-1>`, `days/<d-2>` |
| Status healthy | `/status` | each job's last run is ok, dated 23:55 on the demo day so every time that day reads healthy |
| Status failing | `/status?data=failing` | variant `failing` |
| The phone's panels | `/device/home`, `/device/tasks`, `/device/people` | all of the above; weather is live, for the Home place's last location fix |

`/device/home` carries the one thing, the brief, today's events, three flags, the agent's
queue and the weather; the phone's due soon and people come from `/device/tasks` and
`/device/people`, which the same set fills. The weather is a live Google call for the
Home place's coordinates when the env file has `GOOGLE_MAPS_KEY`, and null without one.
