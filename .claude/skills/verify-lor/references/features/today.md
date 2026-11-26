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

- `node .claude/skills/verify-lor/verify.mjs screen today <out-dir>` runs the `today` flow in `scripts/ui-check.mjs` against the test server on port 3857, at 1500x950. It prints JSON (`ok`, `shots`, `log`, `browser_errors`) and saves the flow's console output to `<out-dir>/today.log`. `verify.mjs screen / <out-dir>` instead takes one 1500x950 screenshot of that path without touching data.
- The flow first runs psql on `life_on_rails_test`: DELETE FROM `intent_link`, `proposal`, `commitment`, `time_block_observation`, `time_block`, `google_event`, `attribution_rule`, and non-system `intent` rows, after clearing `time_block.commitment_id`. It then inserts a `google_sync_state` row, calendar events (Standup 09:00, Focus time 10:00 to 12:00 as `focusTime`, Afternoon hold 13:00 to 17:00, Dentist 14:00 to 15:00, all-day "Priya in town"), goal "Ship Life on Rails" (10 h/week) and project "Health admin". Event times are local to the server's `LOR_TZ`, got the way `scripts/serve.mjs` gets it (the environment, then the env file through Node's parser, then America/New_York), on today's local date.
- It drives the page on the test server's fixed clock, with the date in full (`?at=<date>T15:30`), so the timer holds real minutes and the page, fixtures and checks share one day even when the server runs on `LOR_FAKE_NOW`: at 15:30 it starts a timer from the card ("Something else…", title "Writing the spec", search "ship", Enter); at 16:00 it pauses; at 16:10 it stops. Then it clicks the Dentist block in `section[aria-label="Planned and actual"]`, ticks "Label every “Dentist” this way from now on" and picks Health admin, and finally steps to the previous day.
- Each check prints `ok: <what>`, and the first one that fails ends the flow with `FLOW FAILED: expected <what>; found <what was there>` and `today-FAILED.png`. Screenshots: `today-1-open` to `today-7-yesterday`. `blocks:` lists the day's blocks at the end.
- Set data up through the API instead (the API refuses writes without a `thread` field; `verify.mjs api` adds `"thread":"verify-lor"` when the body has none):
  - `verify.mjs api POST /timer/start --body '{"intent_id":"<id>","title":"Writing"}'`, then `/timer/pause`, `/timer/resume`, `/timer/stop`.
  - `POST /blocks/<block_id>/recategorize` (`intent_id`), `POST /proposals` (`block_id`, `intent_id`, `reason`), `POST /confirm` (`date`, `all`, `accept`, `reject`), `POST /intents/<id>/metric` (`value`), `POST /log` (`text`).
  - `GET /day?date=YYYY-MM-DD` returns the day as markdown.

## Proves it works

- `screen today` exits 0 with every check `ok`: the running card shows "Ship Life on Rails — Writing the spec" and reads 30m at 16:00; pausing leaves an open block from 15:30 with `paused_at` 16:00; at 16:10 the card still reads 30m, and Stop closes the block at 15:30-16:00 (a pause is not time spent) and draws it in the Actual lane; the Dentist calendar block (14:00-15:00, Meetings) is drawn selected and its panel says "from the calendar"; relabelling moves it to Health admin, in the table and in the lane, and adds one `attribution_rule` with `match_title` `^Dentist$`; "Earlier" opens a page headed "Day".
- `screen clock <out-dir>` checks the test server's fixed clock (`?at=`, `src/server/clock.ts`): Today at `?at=09:00` keeps its remaining time, next event and sidebar clock past one 20-second tick, a link keeps `at=`, and `?at=00:05` draws the whole waking day ahead. It changes no rows; load the demo set first (`pnpm demo:test`) so the page has events.
- Focus time draws dashed in Planned and makes no Actual block. Where two events overlap, the shorter keeps the overlap: the Afternoon hold's blocks go around the Dentist hour and around the timer.

## Gotchas

- The picker under "Something else…" spills out of the day arc, and the lanes' hour row lies over its intent buttons, so a click on one does not land (seen 2026-11-25 at 1500x950). The flow picks with Enter in the search box instead. This is a bug in the page, not the flow.
- Calendar events turn into Actual blocks only when the calendar fill runs. Jobs are off on the test server (`LOR_NO_JOBS=1` in `scripts/ui-server.sh`), but timer start/stop and block edits call `syncFill` (`resync` in `src/server/attribution/blocks.ts`); the flow's timer start is what fills the day.
- The flow does not clear phone or place evidence, so after the demo set (`pnpm demo:test`) the Actual lane also has place and phone blocks and the page a brief and phone line. They sit below the calendar in precedence and do not change what the flow checks.
- A timer shorter than a minute is deleted on stop (`MIN_BLOCK_MS`). On a fixed clock start and stop happen at the same instant, which is why the flow moves `?at=` between steps.
- `innerText` carries the CSS uppercase of headings, labels and durations ("30M"); match without case, or use `textContent`.
- The page reloads its data every 60 seconds, which can reset a half-finished interaction in a long wait.
- The server and the flow run from a built copy of the checkout (`/tmp/lor-ui-<worktree>` by default), so a code change, the flow's included, shows only after `verify.mjs up` again. Write evidence outside that copy, which `down` deletes.
