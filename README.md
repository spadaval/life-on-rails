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
tailscale serve --bg --http=3847 http://127.0.0.1:3848    # the dashboard on the owner's other devices
```

The last line publishes the owner-only listener on its own Tailscale Serve port, so the
dashboard is `http://buildbot:3847` (or `http://buildbot.tail47515a.ts.net:3847`) from any
of the owner's devices. It does not touch whatever else Serve already publishes.

After a change: `pnpm build && systemctl --user restart lor-web`.

### Google

LoR has its own read-only Google login (calendar, tasks, contacts; plus Gmail headers and
Pub/Sub with `--mail`); it never uses `gog`. Run `pnpm google:auth`:
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
| `pnpm db:seed seeds/<name>.json` | Load a starter graph. |
| `pnpm google:auth [--mail]` | LoR's own Google sign-in. |

- `http://127.0.0.1:3847` is everything, for use on the box. `/health` reports the database and every job.
- `127.0.0.1:3848` is the owner-only listener that `tailscale serve` publishes.
- Logs: `journalctl --user -u lor-web -f`
- Backups: `~/.local/share/life-on-rails/backup/`, nightly, last 7 kept.
  Restore with `pg_restore --clean --if-exists -d life_on_rails <file>` after stopping the unit.

## The phone app

`android/` is the phone's sensor and timer, in Kotlin and Jetpack Compose. Build it with
`cd android && ./gradlew assembleDebug` (the Android SDK is at `~/Android/Sdk`), and install it
over wireless debugging: `adb connect 100.83.51.27:<port>` then
`adb install -r app/build/outputs/apk/debug/app-debug.apk`. `./gradlew testDebugUnitTest`
runs its JVM tests. See "The phone" in the spec.

## The agent

`cli/lor.mjs` is the agent's CLI (symlink it to `~/.local/bin/lor`), and `skill/` is the
`lor` skill (symlink it into the skills directory each harness reads). `lor help` lists the
verbs; `lor status` shows every job and sensor. The assistant itself runs as T3 Code
threads in a project of its own; what it does on each run is written on the `agent` page,
which the owner edits in the Memory screen.

## Layout

See "Where things are" in the spec.
