# Life on Rails v2

A self-hosted personal operating system for one person on one box: a time
tracker that knows intent, and the memory and senses of a personal agent that
lives in T3 Code.

- Design and build order: `docs/v2-product-engineering-spec.md`
- Notes for agents working in this repo: `AGENTS.md`
- The frozen 2023 app: `archive/2023/` (never imported)

## Stack

TanStack Start, React 19, Tailwind 4, PostgreSQL 18 (apt, Unix socket, peer
auth), Drizzle for migrations with postgres.js for queries, Zod 4, Vitest, pnpm.
Versions are pinned exact in `package.json`.

The service runs on Node 22 at `~/.local/opt/node/bin/node`, called by absolute
path. Builds and tests run under whatever Node `pnpm` uses in the shell.

## Set up a box

```bash
scripts/install-postgres.sh                  # apt install, role, databases (needs sudo)
pnpm install
install -m 600 .env.example ~/.config/life-on-rails/env   # then fill it in
pnpm db:migrate                              # schema + seed
pnpm build
scripts/install-units.sh                     # lor-web.service + nightly backup timer
```

After a change: `pnpm build && systemctl --user restart lor-web`.

### Google

LoR has its own read-only Google login; it never uses `gog`. Run `pnpm google:auth`:
with no OAuth client yet it prints the steps to create one in the Google Cloud console,
and with one it prints a consent URL to open on any device. `pnpm google:auth --mail`
adds the Gmail-headers and Pub/Sub scopes that mail notifications need. The Capture
page shows whether the login is working.

## Running

| | |
| --- | --- |
| `pnpm start` | The production server: both listeners and the scheduler. What the unit runs. |
| `pnpm dev` | Vite dev server, UI only. It never runs jobs. |
| `pnpm test` | Pure functions. No database, no network. |
| `pnpm test:db` | Write paths against `life_on_rails_test`. |
| `pnpm typecheck` | `tsc --noEmit` |
| `pnpm db:generate` | Generate a migration from `src/db/schema.ts`. |
| `pnpm db:migrate` | Apply migrations and the seed. |

- `http://127.0.0.1:3847` is everything, for use on the box. `/health` reports the database and every job.
- `127.0.0.1:3848` is the owner-only listener that `tailscale serve` publishes.
- Logs: `journalctl --user -u lor-web -f`
- Backups: `~/.local/share/life-on-rails/backup/`, nightly, last 7 kept.
  Restore with `pg_restore --clean --if-exists -d life_on_rails <file>` after stopping the unit.

## Layout

```
src/shared/      types and helpers safe for both sides (envelope, ids, local time)
src/db/          schema, client, migrate, seed
src/server/      env, logging, jobs, and everything that touches the database
src/routes/      TanStack Start routes (UI, /health, /api/v1)
scripts/         production entry (serve.mjs), host setup, systemd units
tests/           pure tests; tests/db/ runs against Postgres
drizzle/         generated SQL migrations
```
