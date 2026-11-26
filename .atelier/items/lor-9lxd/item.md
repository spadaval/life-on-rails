---
schema: atelier.item
schema_version: 3
id: lor-9lxd
title: 'The pnpm command works on buildbot again: corepack''s shim cannot run pnpm 12'
status: in_review
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

- [x] The Report says why `pnpm` worked before and what changed.
- [x] `pnpm --version` prints 12.11.2 in a fresh LoR worktree, and `pnpm install`, `pnpm test` and `pnpm typecheck` run there, with the path as an agent's shell has it.
- [x] `AGENTS.md` or the host's notes say which pnpm is the one to use, if that is not simply `pnpm`.

## Scope

The host's pnpm set-up and, if needed, a line in `AGENTS.md`. Not a change of the pinned version unless that is the cause.

## Report

`pnpm` works again in every LoR checkout: it now resolves to Vite+'s `~/.local/share/vite-plus/bin/pnpm`, which honours the `pnpm@12.11.2` pin. The fix is on the host only; nothing in the repository changed.

**Why it worked before.** Both `~/.profile` and `~/.bashrc` source `~/.config/vite-plus/env` after they add `~/.local/bin`, and that script moves `~/.local/share/vite-plus/bin` to the front of `PATH`. So `pnpm` and `node` came from Vite+, and corepack's shims in `~/.local/bin` (made on 2026-10-11 by `corepack enable`) were never reached. The pin has been 12.11.2 since the first v2 commit (e050ace5), and corepack 0.34.0 has never been able to run pnpm 12: its shim requires `bin/pnpm.cjs`, which pnpm 12 does not ship (it ships `bin/pnpm.mjs`).

**What changed.** On 2026-11-21 at 23:12 the Antigravity CLI installer (`~/.local/bin/agy`, same timestamp) appended `export PATH="/home/supadava/.local/bin:$PATH"` to the end of both files, after the Vite+ line. That put `~/.local/bin` back in front, so its corepack `pnpm` shim (and Node 22.20 instead of Vite+'s Node 26.13) won. The profile backups (`~/.profile.bak-2026-11-04`, `~/.bashrc.bak.1791694316`) differ from the pre-fix files by exactly those lines plus older unrelated additions, and `agy` carries the same timestamp. That shells started before 2026-11-21 kept working, and that this is why it surfaced days later, is an inference: past shell environments cannot be observed now. The corepack cache does not date the regression. Corepack fetched 12.11.2 on 2026-11-25 at 18:59, the first time this checkout's pin reached the shim. Its 12.18.0 package was cached on 2026-11-09, from some unpinned use; the 2026-11-22 02:09 time on that directory belongs to a later `pnpm-native` download, not the fetch.

**What I did.**
- Removed the two Antigravity lines from `~/.profile` and `~/.bashrc`. `~/.local/bin` is already on `PATH` earlier in each file, so `agy` still resolves. Backups: `~/.profile.bak-2026-11-25`, `~/.bashrc.bak-2026-11-25`.
- Ran `corepack disable --install-directory ~/.local/bin`, which removed its `pnpm`, `pnpx`, `yarn` and `yarnpkg` shims. They could not run the pinned pnpm anyway, and with them gone a future installer that reorders `PATH` cannot shadow Vite+'s `pnpm` again. Nothing referenced them (systemd user units, `scripts/`, `android/*.sh`); `lor-web` runs `~/.local/opt/node/bin/node` directly. `corepack enable --install-directory ~/.local/bin` undoes it.

**Checked.** In a fresh `git worktree add --detach … main`, under `bash -ic`, `bash -lic` and `env -i HOME=$HOME bash -lic`, and in this agent's existing shell (whose `PATH` predates the fix), `which pnpm` is `~/.local/share/vite-plus/bin/pnpm` and `pnpm --version` prints 12.11.2. There, `pnpm install` finished ("Done in 1.1s using pnpm v12.11.2"), `pnpm test` passed 24 files and 208 tests, and `pnpm typecheck` ran and failed only on `src/routeTree.gen.ts` being missing (errors in `src/routes/tasks.tsx` and `src/routes/trmnl/$.ts`). That failure is lor-iurl, already filed, and not about pnpm.

**Docs.** The one to use is simply `pnpm`, so `AGENTS.md` is unchanged. One thing worth knowing: `node` in agent shells is Vite+'s Node 26.13 again, as it was before 2026-11-21, not the 22.20 under `~/.local/opt/node` that `lor-web` runs.

### Responses

1-gwik.1: fixed in ea306798
