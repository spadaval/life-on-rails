# Life on Rails v2 — agent notes

Single-user self-hosted personal OS. The design and the state of the build are in `docs/v2-product-engineering-spec.md`; read it first.

## Rules

- Personal project, one box, no users: no PR plan, review gate, or staged rollout. Work goes to `main` when it works.
- Prefer the simplest shape: no fallbacks, watchdogs, retention, or permission layers until their absence is felt.
- Add a table when the code that uses it is written.
- T3 Code is where the agent lives. LoR is its memory, the owner's dashboard, and the senses. Do not add chat, an approval queue, notifications, or a model runtime to LoR.
- A `proposal` is only LoR suggesting what a block of the owner's time was.
- LoR does not mirror the mailbox: it records that a message arrived (headers, never a body) and wakes the agent.
- LoR talks to Google through its own read-only OAuth login and never runs `gog`.
- Never import from `archive/2023/` (frozen 2023 draft).
- `pnpm dev` is UI-only. The scheduler starts only from `scripts/serve.mjs`.
- Scoring reads **commitments only**, never observations. `time_block.committed` is denormalized; truth = `commitment`.
- The scorer recomputes hours from `[t, t_end]` intervals with a sweep-line merge. `commitment.value` is display-only. A pause is never inside a block.
- No overlapping actual seconds. Calendar blocks are fill only: `syncCalendarFill` decides which should exist and changes only what differs. Taken time = non-calendar blocks + committed calendar blocks.
- Every writer of `time_block` calls `lockBlocks(tx)` first and re-syncs fill in the same transaction.
- Local-day arithmetic goes through `src/shared/localTime.ts`. Never add 24 hours to move a day.
- Queries are SQL through postgres.js; pass JSON with `sql.json(...)`, never a pre-stringified string.
- Server functions return JSON-safe values only (ISO strings, not Dates).
- Config is the env file only (`~/.config/life-on-rails/env`); every comment in it on its own line.
- Binds `127.0.0.1:3847`. The `:3848` listener is the only thing `tailscale serve` targets, and it checks the owner's identity header.
- Never log tokens or store them in tables. Titles and subjects may be logged.

## Commands

- `pnpm test` — vitest, pure functions (no database, no network)
- `pnpm test:db` — write paths against `life_on_rails_test` (tables truncated between tests)
- `pnpm typecheck` — tsc --noEmit
- `pnpm db:generate` — a migration from `src/db/schema.ts` (no DB needed)
- `pnpm db:migrate` — apply migrations and the seed
- `pnpm db:seed seeds/<name>.json` — load a graph seed
- `pnpm build && systemctl --user restart lor-web` — deploy
- `pnpm start` — what the unit runs: both listeners and the scheduler
- `pnpm dev` — UI-only Vite dev on 127.0.0.1:3847
- `pnpm google:auth [--mail]` — LoR's own Google sign-in

## Checking the screens

The in-app browser preview may be off for a thread. `scripts/ui-server.sh` starts a throwaway server on the **test** database at `:3857`, and `node scripts/ui-check.mjs <graph|today|memory> <out-dir>` drives it in the system Chrome and writes screenshots. Never point either at the real database.

## The phone app

`android/` is a Kotlin + Compose app (the phone's sensors, the timer tile, settings). `cd android && ./gradlew assembleDebug testDebugUnitTest`. Install over wireless adb (`~/Android/Sdk/platform-tools/adb`, phone at `100.83.51.27` over Tailscale; find the port with `nmap -p 30000-50000`). Try it first on the emulator (`sg kvm -c start-t3-emulator`) pointed at the test server (`http://10.0.2.2:3857`), never the real one. The server side is `src/server/device.ts`.

## Talking to the running service

`lor` (symlinked from `cli/lor.mjs`) is the agent's CLI; `skill/SKILL.md` teaches it. `lor status` shows every job. Logs: `journalctl --user -u lor-web -f`.
