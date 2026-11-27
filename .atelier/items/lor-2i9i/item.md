---
schema: atelier.item
schema_version: 3
id: lor-2i9i
title: lor log - reads the text from stdin, so a bare '-' is never logged
status: in_review
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- mcp:e83becf5-0f39-492b-8996-4af352c8194d
ready: null
trivial: null
caused_by: null
created_at: 2026-11-27T07:20:16.243663865Z
---

## Goal

An agent that writes `lor log - <<EOF`, the form `lor message -` documents, logs its text and not a dash. The 27 Nov 2026 nightly run did exactly that and LoR recorded an agent observation whose whole text was `-` (LoR task `01a21bd2-71e4`, the entry is `01a21bd2-20c8`): `lor log` took its first word as the text and ignored stdin, so what the run meant to record was lost without an error.

## Done when

- [x] `lor log -` sends the text on stdin, as `lor message -` does.
- [x] A lone `-` with nothing on stdin exits 2 with a message and makes no request, for both verbs, so a bare `-` or an empty entry cannot be recorded.
- [x] `lor log "<text>"` behaves as before.
- [x] A test covers these without reaching a server, and `lor help`, the skill and the spec say `lor log -` reads stdin.

## Scope

`cli/lor.mjs`, `tests/lor-cli.test.ts`, and the lines on `lor log` in `skill/SKILL.md` and `docs/v2-product-engineering-spec.md`. Not the server's `/log` or `/messages` validation, and not the stray `-` entry already in LoR.

## Report

`lor log` and `lor message` now take their text through one function, `readText` in `cli/lor.mjs`: a lone `-` reads stdin (or `--file`), anything else is the words joined by spaces.

Three behaviours changed beyond the task's wording, all on purpose:

- Text read from stdin is trimmed, for `lor message -` too. A heredoc ends in a newline, and neither a log entry nor a WhatsApp message wants it.
- Empty or whitespace-only stdin is refused with exit 2 for both verbs. Before, `lor message - < /dev/null` sent an empty text to the server.
- `lor log` joins all its words, as `lor message` already did. Before, `lor log moved the file` recorded only `moved`, the same silent loss in another form.

Checked in this worktree: `pnpm test` gives 233 passed (230 before, three new in `tests/lor-cli.test.ts`, which assert the request body's `text` through a replaced `fetch`). `tsc --noEmit` passes, with `src/routeTree.gen.ts` copied from the main checkout because a fresh worktree has no generated route tree. By hand: `echo | LOR_PORT=3999 node cli/lor.mjs log - --thread t` exits 2 with `"-" reads the text from stdin, and stdin was empty`.

The installed `lor` is a symlink to the main checkout's `cli/lor.mjs`, so the fix is live as soon as this merges; nothing needs a build or a restart. `~/.claude/skills/lor` should be checked after merge to see whether it is a copy of `skill/SKILL.md` or a link to it.
