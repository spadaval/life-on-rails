# Tasks

Every task in one Linear-style list, so the owner can see what is open, filter it, and change many tasks at once.

## Reach it

- `/tasks` opens the Open view. Route: `src/routes/tasks.tsx`, data from `getGraphFn` in `src/server/functions/graph.ts`.
- Query params (all optional): `view=open|waiting|agent|later|done`, `for=<intent id>` (a goal also matches its projects' tasks), `due=overdue|today|week|dated|none`, `from=agent|you|google`, `flagged=true`, `q=<text>`, `group=due|for|status|none`, `sort=due|created|updated|title`, `id=<task id>` (opens the side panel).
- Keys on this screen, only when no field has focus: `j`/`ArrowDown` and `k`/`ArrowUp` move, `Enter` or `o` opens the panel, `x` selects, `d` marks done (or the selection), `/` focuses the filter box, `Esc` closes the panel or clears the selection.
- `q` on any screen opens the quick-add dialog (`QuickAddDialog` in `src/components/QuickAdd.tsx`, mounted in `src/routes/__root.tsx`).

## What it does

- Quick add box at the top: `getByRole("combobox", { name: "Add a task" })`. `#name` picks what it counts toward from a list (`#quick-add-choices`, Enter or Tab takes the highlighted one), `@agent` gives it to the agent, `@person` makes it waiting on that person, and date words set the due date. Each understood part shows as a `.chip` ("For ...", "Due ...", "With the agent", "Waiting on ..."), and its button with aria-label "Keep it as text" drops it. Enter adds the task.
- View tabs in `nav[aria-label="Which tasks"]`: Open, Waiting, With the agent, Later, Done, each with a count.
- Filter bar: text box (aria-label "Filter by text"), then hidden selects with aria-labels "For", "Due", "From", a "Needs you" toggle, "Clear", and "Group" and "Sort" on the right.
- Rows are `li[id^=task-]`. Clicking a row's title opens the side panel; the 5px left edge button (aria-label "Select <title>") or shift/cmd/ctrl-click selects it.
- With rows selected, a bulk bar shows "<n> selected" with Done, Due (Today, Tomorrow, Next week, No date), Later, For, "Give to the agent", "Take back", Delete and Clear. Its notice reads e.g. "Rescheduled: 2." in `role="status"`.
- The side panel edits title, Status (Open, Waiting, Later), Due (date input plus Today/Tomorrow/Next week), For, Whose ("Mine" / "The agent's"), Waiting on, and Notes, and says who added it. Tasks from Google Tasks cannot be renamed, re-dated or deleted here.
- "Move everything past due to Later" at the foot of the Open view.

## Drive it

- `node .claude/skills/verify-lor/verify.mjs screen tasks <out-dir>` runs the `tasks` flow in `scripts/ui-check.mjs` against the test server on port 3857. It prints JSON (`ok`, `shots`, `log`, `browser_errors`) and saves the flow's console output to `<out-dir>/tasks.log`. `verify.mjs screen /tasks <out-dir>` instead takes one 1500x950 screenshot of that path without touching data.
- The flow first runs psql on `life_on_rails_test`: DELETE FROM `intent_link`, `proposal`, `commitment`, `time_block_observation`, `time_block`, `attribution_rule`, `intent` (non-system rows), `entity_identifier`, `entity`, after clearing `time_block.commitment_id`. It then seeds a goal "Neha and the wedding", project "Wedding vendors", project "Life on Rails", person "Dad", three tasks (one flagged, one past due) and a Google task "Renew passport (from Google)".
- Screenshots: `tasks-1-open`, `tasks-2-suggest`, `tasks-3-parsed`, `tasks-4-dialog`, `tasks-5-filter-goal`, `tasks-6-panel`, `tasks-7-bulk`, `tasks-8-after-bulk`, `tasks-9-agent` (all `.png`).
- Console lines: `chips:`, `created:`, `counts toward:`, `waiting:`, `literal:`, `under the goal:`, `notice:`, plus `shot <name>` after each screenshot.
- Set data up through the API instead (the API refuses writes without a `thread` field; `verify.mjs api` adds `"thread":"verify-lor"` when the body has none):
  - `verify.mjs api POST /intents --body '{"type":"task","title":"X","parent_id":"<id>","due_at":"2026-11-10T17:00:00Z","assignee":"agent","flag":"why"}'`
  - `due_at` is parsed as an RFC 3339 time. A bare date means midnight UTC, which is the evening before in America/New_York.
  - `POST /intents/<id>/done`, `/intents/<id>/due` (`{"due_at":...}`), `/intents/<id>/status`, `/intents/<id>/assign`, `/intents/<id>/flag`, `/intents/<id>/unflag`; `GET /graph`, `GET /intents/<id>`.

## Proves it works

- `tasks-3-parsed` shows chips for the parent, the date and "With the agent"; the `chips:` line lists them.
- `created:` prints the new row with assignee `agent` and the weekday; `counts toward:` prints `Wedding vendors`.
- `waiting:` shows status `waiting` for "Get the deposit back"; `literal:` shows `Watch Friday Night Lights|t` (no due date because the Due chip was dismissed).
- `tasks-4-dialog` shows the quick-add dialog over Today. `tasks-5-filter-goal` and `under the goal:` list only tasks under the goal, including the project's tasks.
- `tasks-7-bulk` shows the bulk bar with 2 selected; `notice:` prints "Rescheduled: 2." and `tasks-8-after-bulk` shows them moved.
- `tasks-9-agent` shows the "With the agent" view with the florist task.

## Gotchas

- The flow sets the viewport to 1500x950 for this screen. The side panel sits beside the list only at the `xl` breakpoint.
- The browser runs in America/Los_Angeles, but the server computes "today" and due dates in `LOR_TZ` (default America/New_York, possibly set in `~/.config/life-on-rails/env`). The `created:` query formats in America/New_York for that reason.
- A date-only due is stored as 23:59 local in `LOR_TZ`.
- The bulk Due select is found with `label:has(select[aria-label="Due"])` `.last()`, because the filter bar has a select with the same label. That is fragile if the order changes.
- Single-key shortcuts do nothing while a field has focus, so press `Escape` or click elsewhere first.
- The server and the flow run from a built copy of the checkout (`/tmp/lor-ui-<worktree>` by default), so a code change shows only after `verify.mjs up` again. Write evidence outside that copy, which `down` deletes.
