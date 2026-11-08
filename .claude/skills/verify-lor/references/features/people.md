# People

Who matters to the owner and when they were last in touch, with one hub per person showing their page, what waits on them, and their latest messages and mail.

## Reach it

- `/people` lists people. Route: `src/routes/people.tsx`, data from `getPeopleFn` and `getPersonFn` in `src/server/functions/memory.js`, list logic in `src/server/people.ts`.
- Query params: `filter=important|keep-in-touch|overdue|birthdays|all|groups` (no param means `important`, labelled "Who matters"), `q=<text>`, `id=<entity uuid>` opens that person's hub instead of the list.
- No screen-specific keys. `q` opens the global quick-add dialog for tasks.

## What it does

- Filter links: "Who matters", "Keeping in touch", "Due a hello", "Birthdays soon", "Everyone", "Groups". A find box (aria-label "Find a person") with a "Find" button sets `q`.
- "Who matters" shows people with a page, a cadence, the priority flag, made by the agent (not from Google), or in touch within 90 days. "Due a hello" shows people past their cadence, most overdue first. "Birthdays soon" uses a 7-day window.
- The table has Name (a link to the hub, a kind chip for orgs and groups, "has a page", identifiers), Last in touch (how, and "<n> days past the cadence" in orange), Birthday ("in <n> days", "turning <n>"), Keep in touch, and a checkbox "Mail goes straight to the agent".
- The cadence picker is `select[aria-label="Keep in touch with <name>"]` with "No cadence", "Every week", "Every 2 weeks", "Every month", "Every 2 months", "Every 3 months" (value 90), "Every 6 months", "Every year". Changing it saves at once.
- The hub (`?id=`) has "← Everyone", a panel with identifiers, Last in touch per channel, Birthday, Keep in touch and "goes straight to the agent"; "Waiting on them" (task links to `/tasks?...&id=`); Groups or Members; "Their page" with "Open in Notes"; "Messages"; and "Mail" (headers only, linking to Gmail).

## Drive it

- `node .claude/skills/verify-lor/verify.mjs screen people <out-dir>` runs the `people` flow in `scripts/ui-check.mjs` against the test server on port 3857. It prints JSON (`ok`, `shots`, `log`, `browser_errors`) and saves the flow's console output to `<out-dir>/people.log`. `verify.mjs screen /people <out-dir>` instead takes one 1500x950 screenshot of that path without touching data.
- The flow first runs psql on `life_on_rails_test`: DELETE FROM `page` (except slugs `agent`, `me`, `inbox`), clears `intent.waiting_on_entity_id`, DELETE FROM `entity_identifier`, `entity`, and `observation` rows with source `gmail`. It seeds Priya Raman (cadence 30, birthday in two days, a page, a task waiting on her), Jo Bloggs (cadence 14), the org Acme (`domain:acme.com`), and three gmail observations.
- Screenshots: `people-1-who-matters.png`, `people-2-overdue.png`, `people-3-hub.png`.
- Console lines: `acme cadence:` (from the `entity` row), `hub waiting:`, `hub mail:`, plus `shot <name>`.
- Set data up through the API instead (the API refuses writes without a `thread` field; `verify.mjs api` adds `"thread":"verify-lor"` when the body has none):
  - `verify.mjs api POST /entities --body '{"kind":"person","name":"P","emails":["p@example.com"]}'`
  - `POST /entities/<id|name|email>` with `keep_in_touch_days`, `birthday` (`MM-DD`), `priority`, `clear_keep_in_touch`, `clear_birthday`.
  - `POST /pages` with `slug`, `title`, `body`, `entity_id` gives a person a page. `POST /intents` with `waiting_on_entity_id` makes a task wait on them.
  - Read with `GET /people?filter=overdue` and `GET /entities/<id|name|email>`.

## Proves it works

- `people-1-who-matters` lists Priya and Jo; Priya shows "has a page" and a birthday "in 2 days".
- `people-2-overdue` (the "Due a hello" filter highlighted; the URL is not in the shot) lists Priya (last mail 45 days ago against a 30-day cadence) and not Jo (emailed 3 days ago).
- `acme cadence: 90` confirms the select saved to `entity.keep_in_touch_days`.
- `people-3-hub` shows Priya's hub with her page text. `hub waiting:` lists "Get the deposit back from Priya"; `hub mail:` lists the "Lunch?" mail.

## Gotchas

- The flow sets the viewport to 1500x950. The table has `min-w-[860px]` and scrolls sideways on narrow screens.
- Last-in-touch and overdue need contact evidence. With no evidence, a cadence counts as overdue by the full cadence.
- "Today" for birthdays is computed in `LOR_TZ` (default America/New_York) while the browser runs in America/Los_Angeles, so a birthday near midnight can be off by a day.
- Hub sections "Messages", "Mail" and "Groups" only render when they have rows. The flow finds Mail with `section:has(h2:text("Mail"))`.
- `?id=` must be a UUID; the route's validator rejects anything else.
- The flow does not delete intents, so old "Get the deposit back" tasks pile up across runs (no longer waiting on anyone).
- The server and the flow run from a built copy of the checkout (`/tmp/lor-ui-<worktree>` by default), so a code change shows only after `verify.mjs up` again. Write evidence outside that copy, which `down` deletes.
