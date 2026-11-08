# Notes

The wiki the owner and the agent both write: an index by folder, each page in a live-preview editor that saves itself and merges with the agent's edits, and a star-map graph of every page.

## Reach it

- `/notes` shows the index and "Recently changed". Route: `src/routes/notes.tsx`, data from `getNotesFn` in `src/server/functions/memory.js`, editor in `src/components/NoteEditor.tsx`, graph in `src/components/StarMap.tsx`.
- Query params: `slug=<page slug>` opens a page (e.g. `/notes?slug=people%2Fpriya-raman`), `version=<n>` shows an old version, `view=graph` shows the star map (with `slug` it selects that page).
- Keys: `Mod-s` (Ctrl or Cmd + S) saves now. A rendered `[[link]]` follows on a click; while it is being typed, Ctrl or Cmd click follows it. `q` (outside the editor) opens the global quick-add dialog.

## What it does

- "Pages" / "Graph" toggle (`nav[aria-label="Show"]`) and a "New page" form (Title, Slug, Create, Cancel).
- Index panel with a "Filter pages" box, a "Loaded into every agent run" section for digest pages, and one `<details>` per folder. The dated folder `days` is folded unless open and sorted newest first.
- The page editor (`.cm-content`, lines `.cm-line`) saves 1.2 s after typing stops. The status line reads "Saving…", "Not saved yet", "Saved", "Not saved" or "Not saved: <error>".
- `[[` opens page suggestions (`.cm-tooltip-autocomplete`), including "New page". Links render as the page title (`.cm-wikilink`); links to missing pages get `.cm-wikilink-missing`, and clicking one creates the page ("Started “<title>”.").
- Footnote sources such as `mail:<id>` become links carrying `data-url` (mail goes to `mail.google.com`). Lines the agent last wrote carry `.cm-by-agent`.
- When the agent writes while the page is open: with nothing unsaved the new text just appears (on window focus or reload); with unsaved typing the two are merged and a status reads "Merged with the agent's edit."; when both changed the same words, `section[aria-label="Settle the clash"]` offers "Keep yours", "Keep the agent's", "Keep both", then "Done".
- Under the page: "History (<n>)", "Take back the agent's last edit" (when the latest version is the agent's), and "Load into every agent run". History lists "Version <n>" links, a "take back" button per version (notice "Took back version <n>."), and a diff. An old version shows "Revert to this version" and "Back to current".
- A "Connections" panel: "Links here", "Links to", "Not written yet", "See it on the graph".
- Graph: each page is a `[data-slug="<slug>"]` node. One click selects it (side panel with "Read"), a second click opens it. Drag pans, scroll zooms.

## Drive it

- `node .claude/skills/verify-lor/verify.mjs screen notes <out-dir>` runs the `notes` flow in `scripts/ui-check.mjs` against the test server on port 3857. It prints JSON (`ok`, `shots`, `log`, `browser_errors`) and saves the flow's console output to `<out-dir>/notes.log`. `verify.mjs screen /notes <out-dir>` instead takes one 1500x950 screenshot of that path without touching data.
- The flow first runs psql on `life_on_rails_test`: DELETE FROM `page` (except `agent`, `me`, `inbox`), clears `intent.waiting_on_entity_id`, DELETE FROM `entity_identifier`, `entity`, and every `observation` row. It seeds `people/priya-raman` (then an agent edit "HATES phone calls"), `topics/austin`, `topics/travel`, `projects/wedding`, and yesterday's `days/<date>`.
- Screenshots: `notes-1-index`, `notes-2-page`, `notes-2b-suggest`, `notes-2c-links`, `notes-2d-clash`, `notes-2e-history`, `notes-3-graph`, `notes-4-graph-selected`, `notes-5-day-record` (Today at `/?date=<yesterday>`), `notes-6-search` (`/search?q=austin`), all `.png`.
- Console lines: `gmail link:`, `links here:`, `versions after typing:`, `body now:`, `agent edit picked up:`, `agent lines marked:`, `merged:`, `word-level merge:`, `clash settled as mine:`, `took back:`, `versions:`, `started:`, `followed to:`, `opened from graph:`, `search kinds:`.
- Set data up through the API instead (the API refuses writes without a `thread` field; `verify.mjs api` adds `"thread":"verify-lor"` when the body has none):
  - `verify.mjs api POST /pages --body '{"slug":"topics/x","title":"X","body":"# X\n"}'` (optional `entity_id`, `intent_id`, `note`).
  - `PUT /pages/<slug>` with `base` and one of `body`, `append`, or `find` + `replace`. A stale `base` is refused.
  - `GET /pages/<slug>`, `GET /pages/<slug>?history=1`, `GET /pages?q=<text>`.

## Proves it works

- `notes-2-page` shows "HATES phone calls"; `gmail link:` prints a `https://mail.google.com/mail/u/0/#all/...` URL; `links here:` lists the pages linking to Priya.
- `versions after typing:` lists `version:via` pairs. On main (2026-11-08) it printed `1:agent 2:agent 3:ui 4:ui`: the flow waits for "Saved" between its two typed runs, so each run is its own `ui` version. `body now:` shows the inserted Travel link and a `[[` link for Home Network.
- `merged: true`, `word-level merge: true`, `clash settled as mine: true` and `took back: true` confirm the merge paths against the `page` row. `notes-2d-clash` shows the clash panel.
- `started: home-network / Home Network` confirms the missing-link click created a page. `followed to:` ends with `slug=topics%2Ftravel`.
- `notes-3-graph` shows the star map; `notes-4-graph-selected` shows Austin selected; `opened from graph:` is the reader URL.

## Gotchas

- The flow sets the viewport to 1500x950. Below `lg` the index hides while a page is open.
- The flow waits 1600 ms after typing for the autosave; check "Saved" and not "Saving" before reading the database.
- The agent's edit only appears on window focus or reload; the flow dispatches a `focus` event to trigger it.
- Yesterday is computed in the flow as now minus 31 hours (UTC), close to yesterday in Los Angeles. The server decides "yesterday" in `LOR_TZ` (default America/New_York), and Today shows the day record only for dates up to that yesterday.
- The flow deletes every observation, which also empties the Today screen's record for other days.
- The server and the flow run from a built copy of the checkout (`/tmp/lor-ui-<worktree>` by default), so a code change shows only after `verify.mjs up` again. Write evidence outside that copy, which `down` deletes.
