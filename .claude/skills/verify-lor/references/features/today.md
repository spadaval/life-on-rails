# Today

The owner's day at a glance: the waking day remaining, the running timer, planned (calendar) and actual (time block) lanes, what needs the owner, tasks due soon, and goals this week.

## Reach it

- `/` is today. Route: `src/routes/index.tsx`, data from `getTodayFn` in `src/server/functions/today.ts` (day model in `src/server/today.ts`).
- Query params: `date=YYYY-MM-DD` shows another day (header "Day" instead of "Today"); `obs=<observation uuid>` opens the day that observation happened on and highlights it in "Recorded".
- "Earlier" (aria-label "Previous day") and "Later" (aria-label "Next day") step a day; "Today" appears on other days.
- Keys: `r` opens relabelling for the selected block, or the running one, and focuses `#relabel-search` (ignored while a field or button has focus). `q` opens the global quick-add dialog.

## What it does

- The day arc shows "<duration> remaining" of the waking day (`LOR_WAKE_START`/`LOR_WAKE_END`, default 08:00 to 22:00 in `LOR_TZ`), "Not yet" before it starts, or "recorded" after it ends, plus the next event.
- Timer card: with no timer, buttons for up to three recent intents and "Something else…" (a title box and an intent picker). While running: the label (click to relabel), elapsed time, "Pause"/"Resume" and "Stop". Every page head also has "Start timer" (`GlobalActions` in `src/routes/__root.tsx`).
- `section[aria-label="Planned and actual"]`: all-day events as chips, a Planned lane of calendar events (solid if they count as time, dashed if not, such as focus time), and an Actual lane of block buttons (solid: confirmed; dashed: a suggestion; outlined: running). Gap chips read "<duration> free at <time>".
- Clicking a block opens its panel: source and state, "Yes, <intent>" / "Not this" for a proposal, "Undo confirm" for a confirmed block, an intent search (`#relabel-search`) with one button per intent, a "Label every “<title>” this way from now on" checkbox for calendar blocks, and for closed blocks "Split at"/"Split"; timer blocks also get Start/End/"Save times" and "Delete".
- "This morning" shows the brief page `briefs/<date>` when it exists. "Ask the agent" (aria-label "Ask the agent", "Send") sends a question; the notice says the answer comes to WhatsApp.
- Three panels: "Needs you" (agent-flagged tasks with "Done" and "Seen", and tasks with the agent), "Due soon" (checkboxes, "All <n> tasks" link), "Goals this week" (goals with an hour target, linking to `/goals?id=`).
- A metric prompt "<metric>?" with buttons 1 to 5 shows on past days, or today from 17:00 in `LOR_TZ`.
- Past days show "The day, written up" (page `days/<date>`, "Open in Notes") and "Recorded" (every observation that day).
- Footer: "Calendar confirmed <pct> · time accounted for <pct>" and "Too much? Reset visibility".

## Drive it

- `node .claude/skills/verify-lor/verify.mjs screen today <out-dir>` runs the `today` flow in `scripts/ui-check.mjs` against the test server on port 3857. It prints JSON (`ok`, `shots`, `log`, `browser_errors`) and saves the flow's console output to `<out-dir>/today.log`. `verify.mjs screen / <out-dir>` instead takes one 1500x950 screenshot of that path without touching data.
- The flow first runs psql on `life_on_rails_test`: DELETE FROM `commitment`, `time_block`, `google_event`, `attribution_rule`, `intent_link`, and non-system `intent` rows. It then inserts a `google_sync_state` row, calendar events (Standup 09:00, Focus time 10:00 to 12:00 as `focusTime`, Afternoon hold 13:00 to 17:00, Dentist 14:00, Dinner with Priya 23:00, all-day "Priya in town", all Los Angeles local), goal "Ship Life on Rails" (10 h/week), project "Health admin" and metric "Energy".
- The flow is written to save `today-1-running`, `today-2-timeline`, `today-3-panel`, `today-4-confirmed`, `today-5-yesterday` and print `confirm card:`, `footer:`, `blocks:` and `rules:`.
- Set data up through the API instead (the API refuses writes without a `thread` field; `verify.mjs api` adds `"thread":"verify-lor"` when the body has none):
  - `verify.mjs api POST /timer/start --body '{"intent_id":"<id>","title":"Writing"}'`, then `/timer/pause`, `/timer/resume`, `/timer/stop`.
  - `POST /blocks/<block_id>/recategorize` (`intent_id`), `POST /proposals` (`block_id`, `intent_id`, `reason`), `POST /confirm` (`date`, `all`, `accept`, `reject`), `POST /intents/<id>/metric` (`value`), `POST /log` (`text`).
  - `GET /day?date=YYYY-MM-DD` returns the day as markdown.

## Proves it works

- On this branch the today flow fails at its first step (see Gotchas): expect `FLOW FAILED: ...` and `today-FAILED.png`, not the five screenshots.
- Checks that match the code: after "Start timer" the timer card shows "Stop"; after "Stop", the block appears in the Actual lane as a suggestion (a pending proposal for its intent, or "Uncategorized" when it had none). Picking an intent in the block panel moves the block to that intent (check `time_block.intent_id`), and the remember checkbox on a calendar block adds an `attribution_rule` row. Focus time draws dashed in Planned and makes no Actual block.

## Gotchas

- The `today` flow in `scripts/ui-check.mjs` is out of date. It clicks `button:has-text("Just start")` and waits for "Running since", "Start a timer", `section[aria-label="Timeline"]`, an `h2` with "to confirm", "Accept all" and "Confirmed.". None of these exist in `src/routes/index.tsx`, which uses `section[aria-label="Planned and actual"]` and has no confirm-all step (the spec says there is no confirm step). Until lor-mxrc fixes it, drive Today with `verify.mjs screen /` and the API.
- This flow keeps the default 412x915 phone viewport (it never calls `setViewportSize`): the left rail becomes a top strip and the global search box is hidden.
- Seeded calendar events turn into Actual blocks only when the calendar fill runs. Jobs are off on the test server (`LOR_NO_JOBS=1` in `scripts/ui-server.sh`), but timer start/stop and block edits call `syncFill` (`resync` in `src/server/attribution/blocks.ts`).
- The flow seeds event times at Los Angeles midnight plus hours, while the server lays out the day in `LOR_TZ` (default America/New_York). Events can sit three hours off and the 23:00 dinner can fall on the next day.
- The page reloads its data every 60 seconds, which can reset a half-finished interaction in a long wait.
- The server and the flow run from a built copy of the checkout (`/tmp/lor-ui-<worktree>` by default), so a code change shows only after `verify.mjs up` again. Write evidence outside that copy, which `down` deletes.
