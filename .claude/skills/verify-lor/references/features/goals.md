# Goals

What the owner's time is for: each goal with this week's hours against its target, and the projects and habits under it.

## Reach it

- `/goals`. Route: `src/routes/goals.tsx`, data from `getGraphFn` in `src/server/functions/graph.ts`.
- Query params: `id=<intent id>` selects a goal, project or habit and opens its side panel; `done=true` also shows done, paused and later items.
- No screen-specific keys. `q` opens the global quick-add dialog for tasks.

## What it does

- The page head counts open goals, projects and habits.
- "New goal, project or habit" opens a form with `select[aria-label="Kind"]` (goal, project, habit, metric), `input[aria-label="Title"]`, `select[aria-label="Counts toward"]` (not for goals), `input[aria-label="Hours a week"]` (goals and projects), `Times` and `Per` (habits), and an "Add" button. A goal with hours gets weight 2.
- Checkbox "Show done, paused and later" toggles `done=true`.
- One card (`section`) per top-level goal, sorted by hour target. The card's title is a button; its aside reads "<hours> / <target>h this week" with a meter. Under it are rows for its projects, habits and metrics, nested up to depth 4.
- Habit rows have a "Did it" button that logs the habit (notice `Logged “<title>”.`). Project and goal rows show a task count link ("no tasks", "1 task", "<n> tasks") that opens `/tasks?for=<id>`. Tasks themselves are not listed here.
- Items not under any goal go in a "Not under a goal" panel.
- The side panel (in `aside`) shows Kind, This week, Status, Due, Counts toward, How often (habits), Tasks (count link), "Did it" for habits, "Edit" (opens `IntentEditor`) and "Under it".

## Drive it

- `node .claude/skills/verify-lor/verify.mjs screen goals <out-dir>` runs the `goals` flow in `scripts/ui-check.mjs` against the test server on port 3857. It prints JSON (`ok`, `shots`, `log`, `browser_errors`) and saves the flow's console output to `<out-dir>/goals.log`. `verify.mjs screen /goals <out-dir>` instead takes one 1500x950 screenshot of that path without touching data.
- The flow first runs psql on `life_on_rails_test`: DELETE FROM `intent_link`, `proposal`, `commitment`, `time_block_observation`, `time_block`, `attribution_rule`, and non-system `intent` rows, after clearing `time_block.commitment_id`. It seeds goal "Neha and the wedding" (6 h/week), project "Wedding vendors" with tasks "Shortlist venues" and "Call the florist", task "Book flights" and habit "Call Neha" under the goal, and project "Health admin".
- Screenshots: `goals-1.png`, `goals-2-selected.png`, `goals-3-tasks.png`.
- Console lines: `goal card:` (the wedding card's text), `tasks for vendors:` (task titles found on the Tasks page), plus `shot <name>`.
- Set data up through the API instead (the API refuses writes without a `thread` field; `verify.mjs api` adds `"thread":"verify-lor"` when the body has none):
  - `verify.mjs api POST /intents --body '{"type":"goal","title":"G","weekly_hour_target":10}'` (also `project`, `habit` with `frequency` and `frequency_horizon`, `metric`, with `parent_id`).
  - `POST /intents/<id>/habit` logs a habit, `POST /intents/<id>/status` (`active|paused|later|archived`), `POST /links` (`from_id`, `to_id`, `kind`), `GET /graph`, `GET /intents/<id>/score`.

## Proves it works

- `goals-1` shows the wedding card with "0 / 6h this week", Wedding vendors and Call Neha under it, and Health admin under "Not under a goal". `goal card:` prints that text.
- After adding, a button "Ship Life on Rails" appears as a new goal card. "Did it" on Call Neha shows the "Logged" notice.
- `goals-2-selected` shows Wedding vendors selected with its side panel and a "2 tasks" link.
- Following that link lands on `/tasks?for=<id>`. `goals-3-tasks` shows that page with "For Wedding vendors" set and only Call the florist and Shortlist venues listed, not Book flights (which is under the goal, not the project). Judge this from the screenshot: on main (2026-11-08) the flow's own `tasks for vendors:` line printed `null` although the page was right, because its text match does not find the titles (lor-mxrc).

## Gotchas

- The flow sets the viewport to 1500x950. The side panel sits beside the cards only at the `xl` breakpoint.
- "This week" hours are the score's `last_7_days` term (`src/server/graph.ts`), recomputed on every load because `getGraphFn` calls `recomputeScores`. The flow deletes every time block, so each goal reads 0 hours unless you log time first.
- The task count counts only open tasks (status active or waiting), walking every level up.
- The "2 tasks" link is found with `aside a:has-text("2 tasks")`, which depends on the exact count text.
- The flow does not delete `entity` rows, unlike the tasks flow.
- The server and the flow run from a built copy of the checkout (`/tmp/lor-ui-<worktree>` by default), so a code change shows only after `verify.mjs up` again. Write evidence outside that copy, which `down` deletes.
