---
schema: atelier.item
schema_version: 3
id: lor-y8lw
title: Running cli/lor.mjs from a checkout no longer writes to the live LoR by default
status: approved
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- mcp:62b4e422-9451-499f-a893-fdabdb8b998d
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T07:15:08.441487455Z
---

## Goal

A command meant for the test server cannot write to the real LoR by leaving a variable out. On 25 Nov 2026 at 7:58 PM, lor-zk52's implementer, in its worktree, ran `node cli/lor.mjs intent create --type goal --title "Hold a conversation in Portuguese"` by hand to get a goal for a layout measurement on `:3857`. It did not set `LOR_PORT=3857`. The CLI read the env file, defaulted to lor-web on 3847 with the real agent token, and made goal `01a2155c` in Sumeet's data (archived by the nightly run; found in LoR task `01a216ab`). `scripts/ui-check.mjs` and the verify skill were not at fault: both pass the test port. The only protection was a sentence in the verify skill's `api.md`, "Never leave `LOR_PORT` unset".

## Done when

- [x] `node cli/lor.mjs <anything but help>` with no `LOR_PORT` or `LOR_URL` in its environment exits 6 before any request, and says how to reach the test server and the real one.
- [x] Run as a file, it also refuses a named server that is lor-web on this machine (3847, 3848, or the env file's `LOR_PORT` and `LOR_SERVE_PORT`).
- [x] The installed `lor` (the symlink) behaves as before, and `scripts/ui-check.mjs` flows still pass through the verify skill.
- [x] `scripts/ui-check.mjs` refuses a `LOR_UI_BASE` on lor-web's ports, since its flows empty tables and write as the agent.
- [x] A test covers the refusals and the allowed paths without reaching a server.

## Scope

`cli/lor.mjs`, `scripts/ui-check.mjs`, a test, and the three documents that describe the CLI. Not the shared test server's locking (lor-rdbu) or which checkout served the evidence (lor-3ydk).

## Report

`cli/lor.mjs` tells the two uses apart by how it was started: the installed `lor` is a symlink (`lstat` of `process.argv[1]`), a checkout's copy under test is run by its own path. Run by its path, it needs the server named in the call's own environment, and it refuses lor-web's ports on any host. The env file no longer supplies a default there, because the env file is what pointed the 25 Nov command at the real server. The installed `lor` is unchanged, so agents in any checkout still reach their memory with `lor`.

`scripts/ui-check.mjs` already passed `LOR_PORT` from its base URL; it now also exits 3 when that base is 3847, 3848 or 3849. `verify.mjs` and `shots.mjs` had their own refusals already.

Checked: `pnpm test` gives 229 passed, with the new `tests/lor-cli.test.ts` (refused with no server, refused on 3847, on the env file's port and on `LOR_URL=http://localhost:3848`, allowed to a named closed port, help still prints, the symlink still takes the env file's port). `pnpm typecheck` passes. By hand in this worktree: `node cli/lor.mjs status` exits 6 with the message, `lor status` exits 0, `LOR_UI_BASE=http://127.0.0.1:3847 node scripts/ui-check.mjs tasks` exits 3. `verify.mjs up` then `screen tasks /tmp/lor-verify/y8lw`: `ok: true`, 24 `ok:` lines, `browser_errors: false`, which exercises the flow's own `lor` calls on `:3857`.

One limit: a thread that exports `LOR_PORT=3857` and then runs the installed `lor` talks to the test server, and one that runs the installed `lor` with nothing set talks to the real one, as designed. The guard covers the source file, which is what test commands use.

**Round 2.** All three findings held. The guard now decides by port alone, on any host, because a host has too many spellings to list; a tailnet name on 3848 is refused too. `scripts/ui-check.mjs` reads the env file once and refuses its `LOR_PORT`, `LOR_SERVE_PORT` and `LOR_TRMNL_PORT` beside 3847 to 3849. `tests/lor-cli.test.ts` runs every child with `--import` of a module that replaces `fetch`, so allowed calls assert their method and address and refused ones assert that no fetch happened; it also runs `ui-check.mjs` against all six ports, where it exits 3 before a browser starts. `pnpm test` gives 230 passed, `pnpm typecheck` passes, and `verify.mjs up` then `screen tasks /tmp/lor-verify/y8lw-r2` gives `ok: true`, 24 `ok:` lines, `browser_errors: false`.

**Round 3.** Finding 4 held: both guards compared port strings. They now compare numbers and take a URL with no port as its scheme's default, the way `scripts/shots.mjs` does. The test's env file sets `LOR_PORT=04000` and `LOR_SERVE_PORT=80`, and the refused cases add `:80`, a URL with no port, and `LOR_PORT=04000`, for the CLI and for `ui-check.mjs`. `pnpm test` gives 230 passed and `pnpm typecheck` passes. The browser flow was not rerun: the change to `ui-check.mjs` is the one comparison, and the test runs it.

### Responses

1-ffbo.1: fixed in 00fb3451
1-ffbo.2: fixed in 00fb3451
1-ffbo.3: fixed in 00fb3451
2-07n5.4: fixed in 259bc81c
