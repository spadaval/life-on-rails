---
schema: atelier.item
schema_version: 3
id: lor-9lxd
title: 'The pnpm command works on buildbot again: corepack''s shim cannot run pnpm 12'
status: building
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3Ae517f1eb-1e70-42c5-ab63-9280bb58c045%3Adelegate-task%3Af969b4de-ce6e-4bf1-95fa-56e48d4a572e
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:18:38.330035725Z
---

## Goal

`pnpm` runs on buildbot from any LoR checkout. Today the first `pnpm` on the path fails before it starts, so `pnpm install`, `pnpm test` and `pnpm build` (the deploy) fail unless the caller knows to use another binary. Found on 2026-11-25 by lor-w2tw's implementer, who symlinked the main checkout's `node_modules` to get around it.

What is known:

- `~/.local/bin/pnpm` is first on the path and links to corepack 0.34.0's shim (`~/.local/opt/node-v22.20.0-linux-x64/lib/node_modules/corepack/dist/pnpm.js`).
- `package.json` pins `pnpm@12.11.2`. Corepack fetched it on 2026-11-25 at 18:59 and then fails: `Cannot find module ~/.cache/node/corepack/v1/pnpm/12.11.2/bin/pnpm.cjs`. Its `.corepack` file expects `bin/pnpm.cjs`; the package ships `bin/pnpm.mjs`. The cached 12.18.0 fails the same way from any directory.
- `~/.local/share/vite-plus/bin/pnpm` works and honours the pin (`--version` prints 12.11.2 in the checkout).
- `lor-web` is not affected: its unit runs `node scripts/serve.mjs`.
- Not known: how `pnpm` ran here before 18:59 that day. `~/.cache/node/corepack/v1/lastKnownGood.json` is missing.

## Done when

- [ ] The Report says why `pnpm` worked before and what changed.
- [ ] `pnpm --version` prints 12.11.2 in a fresh LoR worktree, and `pnpm install`, `pnpm test` and `pnpm typecheck` run there, with the path as an agent's shell has it.
- [ ] `AGENTS.md` or the host's notes say which pnpm is the one to use, if that is not simply `pnpm`.

## Scope

The host's pnpm set-up and, if needed, a line in `AGENTS.md`. Not a change of the pinned version unless that is the cause.
