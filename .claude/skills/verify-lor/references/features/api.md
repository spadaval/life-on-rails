# API

The `/api/v1` surface is the backend of the `lor` CLI (`cli/lor.mjs`): one endpoint per verb, each calling the same server function the web UI calls.

## Reach it

- Router: `src/server/api/v1.ts` (`route(...)` table and `handleApi`), mounted by `src/routes/api/v1/$.ts` for GET, POST, PUT and DELETE.
- Client: `plan()` in `cli/lor.mjs` turns a command line into one METHOD and path; `main()` sends it to `${LOR_URL or http://127.0.0.1:${LOR_PORT}}/api/v1<path>`. `LOR_PORT` defaults to 3847, the real server.
- Auth: every request needs `Authorization: Bearer <LOR_AGENT_TOKEN>`. `lor` reads the token from the environment, then from `~/.config/life-on-rails/env`. The test server loads the same env file, so the same token works there. If the server has no token set, every call is 401.
- `GET /health` (outside `/api/v1`, no token) returns the same body as `GET /api/v1/status`: `ok`, `db`, `now`, `jobs`, `sources`, `google`, `mail`. It is 200 whenever the process is listening; stale sensors show in the body. `scripts/ui-server.sh` polls it to know the test server is up.

## What it does

Paths are relative to `/api/v1`. Every write except `money/sync` must carry a non-empty `thread` string in the JSON body; `lor` fills it from `--thread`, `LOR_THREAD` or `T3CODE_THREAD_ID` and refuses with exit 2 before sending if none is set. Writes are stamped `via=agent` with the thread as the source.

| lor verb | METHOD path | Returns or changes |
|---|---|---|
| **Orientation** | | |
| `context [--task id]` | GET `/context[?task=]` | The digest (with `markdown`), or one task's context markdown |
| `status` | GET `/status` | Job, source, Google and mail health |
| `changes --since t` | GET `/changes?since=` | What arrived or changed since t; 400 without `since` |
| `mail pending` | GET `/mail/pending` | Mail waiting for the agent once the burst is quiet |
| `work next` | GET `/work/next` | `{task}`, the oldest queued task |
| `wall` | GET `/wall` | `{one, pin}` for the wall display |
| **Read** | | |
| `search q` | GET `/search?q=&in=&from=&to=&limit=` | `{results}`; `in` takes page, entity, intent, obs |
| `page search q` | GET `/pages?q=&limit=` | `{results}` (default limit 10) |
| `page read slug` / `page history slug` | GET `/pages/<slug>[?version=]` / `?history=1` | The page, or `{slug, results}` |
| `entity show x` | GET `/entities/<id or name or email>` | The entity |
| `people` | GET `/people?filter=&q=&days=&limit=` | `{results, total, markdown}`; filter defaults to `important` |
| `day [date]` | GET `/day?date=` | That day plus its `days/<date>` page and `markdown` |
| `graph` | GET `/graph[?starving=1][&waiting=1]` | `{results, markdown}` of intents |
| `intent show id` / `score explain id` | GET `/intents/<uuid>` / `/intents/<uuid>/score` | Intent detail / score breakdown |
| `places`, `apps`, `rules` | GET `/places`, `/apps?days=`, `/rules` | Named places, app time (default 14 days), rules |
| `money [accounts\|txns\|spend]` | GET `/money/accounts`, `/money/txns`, `/money/spend` | Accounts, transactions, spending |
| `message history` | GET `/messages?since=&q=&around=&limit=` | `{results}` of channel messages |
| **Write** | | |
| `page create` / `page update` | POST `/pages` / PUT `/pages/<slug>` | Creates or edits a page; update takes `base`, `body`, `find`/`replace`, `append`, `dry_run` |
| `entity upsert` / `set` / `merge` | POST `/entities`, `/entities/<x>`, `/entities/<keep>/merge` | Creates, edits or merges an entity |
| `log "text"` | POST `/log` | An agent log entry |
| `intent create` | POST `/intents` | A new intent; `flag` (tasks only) also flags it |
| `intent status\|due\|short` | POST `/intents/<uuid>/status\|due\|short` | Updates that field |
| `task assign\|queue\|flag\|unflag`, `work claim`, `done` | POST `/intents/<uuid>/assign\|queue\|flag\|unflag\|claim\|done` | Task state |
| `link` / `unlink` | POST `/links` / `/links/remove` | Adds or removes a measures, blocks or derived_from link |
| `place add`, `rule add`, `rule delete` | POST `/places`, POST `/rules`, DELETE `/rules/<uuid>` | Places and rules |
| `mail done ids` | POST `/mail/done` | Takes mail off the queue |
| `message "text"` | POST `/messages` | Sends to Sumeet on WhatsApp |
| `wall pin` / `wall unpin` | POST `/wall/pin` / `/wall/unpin` | The wall display pin |
| `propose time` | POST `/proposals` | A time proposal for a block |
| `money sync` | POST `/money/sync` | Fetches from Plaid now; `{items, added, modified, removed}`; no thread |
| **Owner evidence** (only when the owner asks) | | |
| `timer start\|stop\|pause\|resume` | POST `/timer/<action>` | The running block |
| `recategorize block intent` | POST `/blocks/<uuid>/recategorize` | `{ok: true}` |
| `confirm date` | POST `/confirm` | Accepts or rejects a day's blocks |
| `habit id` / `metric id value` | POST `/intents/<uuid>/habit` / `/metric` | A habit check or metric value |

Status codes from `handleApi`: 200 with the JSON result; 400 `bad-json` or `bad-input` (zod or `BadInput`, the message names the field); 401 `unauthorized`; 404 `not-found` or `no-such-endpoint`; 409 for a `Refused` write rule, with `code` and `detail`; 500 `server-error`. Errors are `{error: {code, message, detail?}}`.

`lor` exit codes: 0 ok, 2 usage or 400, 3 a `results` array that came back empty, 4 for 401, 5 for 404, 6 for 409, 8 server unreachable, 1 anything else (including a 500 or a non-JSON reply).

## Drive it

Use the verify CLI against the throwaway test server on 127.0.0.1:3857 and the test database only:

```
node .claude/skills/verify-lor/verify.mjs api GET /status
node .claude/skills/verify-lor/verify.mjs api POST /intents --body '{"type":"task","title":"x","thread":"verify"}'
echo '{"text":"hi","thread":"verify"}' | node .claude/skills/verify-lor/verify.mjs api POST /log --body - --out /tmp/log.json
```

It prints `{status, body}` JSON. The same calls through `lor` itself: `LOR_PORT=3857 node cli/lor.mjs status`, or `LOR_PORT=3857 node cli/lor.mjs intent create --type task --title x --thread verify`. Never leave `LOR_PORT` unset: `lor` then talks to the real server on 3847.

## Proves it works

- The response status and the body fields the change is about (for example `id`, `title`, `status` on an intent).
- For a write, a read-back GET that shows the change: `GET /intents/<id>` after an intent write, `GET /pages/<slug>` after a page write, `GET /entities/<id>` after an entity write.
- For an error path, the status and `error.code` (and `lor`'s exit code if testing through `lor`).

## Gotchas

- The API is loopback-only. `apiFromLoopbackOnly` in `scripts/serve.mjs` answers 403 with plain text `forbidden` for `/api/` from any other address, and the owner listener on `LOR_SERVE_PORT` returns 404 for `/api/`. Through `lor` that plain-text reply is exit 1.
- Server functions return JSON-safe values: times are ISO strings, not Dates.
- `lor` drops null fields from non-page write responses, and page writes echo only `slug`, `version`, `head_version`, `changed`, `dry_run`, `diff` and `warnings`. The raw `{status, body}` from `verify.mjs api` shows everything.
- `lor` exits 3 on an empty `results` list (search, page search, people, graph, message history). That is not a failure.
- `GET /graph`, `GET /intents/<uuid>` and `GET /intents/<uuid>/score` recompute scores first, so these reads write score rows.
- `POST /money/sync` calls Plaid. `scripts/ui-server.sh` runs the test server with `PLAID_ENV=sandbox` and its own items file (`/tmp/lor-ui-plaid-items.json`), so on the test server it is Sandbox. A failed item makes the call 500.
- `POST /messages` goes to WhatsApp. The test server starts with `LOR_CHANNEL_KEY` and `LOR_CHANNEL_THREAD` empty, so it answers 409 `channel-off`. The WhatsApp message for a flagged task is sent by the channel loop, which the test server does not run.
- The test server runs with `LOR_NO_JOBS=1`, so no scheduler runs there and job states in `status` say nothing about your change.
- `DELETE /rules/<uuid>` does not check `thread` on the server; `lor` still sends one.
- Owner-evidence verbs (timer, recategorize, confirm, habit, metric) and `intent status` / `intent due` are for when Sumeet asks; on the test database they are fine to exercise.
