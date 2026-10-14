# Life on Rails v2 — agent notes

Single-user self-hosted personal OS. The design and build order are in `docs/v2-product-engineering-spec.md`; read it first.

> **The code lags the spec.** `e050ace` is a scaffold that has never run end to end, and it still contains things the spec has dropped: the in-app agent and chat, the `gog` wrapper, `user_profile`, `title_embedding`, the job watchdog and tick route, the Linux activity agent. The spec's "Foundation code to remove or change" lists them. Do not build on those.

## Rules

- Personal project, one box, no users: no PR plan, review gate, or staged rollout. Work goes to `main` when it works.
- First make the box run (Postgres, a server that listens, the scheduler, a reboot survived). Nothing else is built until then.
- Add a table when the code that uses it is written.
- Prefer the simplest shape: no fallbacks, watchdogs, locks, retention, or permission layers until their absence is felt.
- T3 Code is where the agent lives. LoR is its memory, the owner's dashboard, and the senses. Do not add chat, an approval queue, notifications, or a model runtime to LoR.
- A `proposal` is only LoR suggesting what the owner's time, habits, or metrics were.
- LoR does not mirror the mailbox: it records that a message arrived and wakes the agent.
- LoR talks to Google through its own read-only OAuth login and never runs `gog`.
- Never import from `archive/2023/` (frozen 2023 draft).
- `pnpm dev` is UI-only. The scheduler starts only from the production server entry.
- Scoring reads **commitments only**, never observations. `time_block.committed` is denormalized; truth = `commitment`.
- The scorer recomputes hours from `[t, t_end]` intervals with a sweep-line merge. `commitment.value` is display-only.
- `proposal kind=time_attribution` always has non-null `time_block_id` + `intent_id`.
- No overlapping actual seconds: calendar fill is remainder-only (`occupied` = non-calendar blocks).
- Config is the env file only.
- Binds `127.0.0.1:3847`. Anything beyond loopback is authenticated by Tailscale identity.
- Never log tokens or store them in tables. Titles and subjects may be logged.

## Commands

- `pnpm test` — vitest, pure functions (no database, no network)
- `pnpm typecheck` — tsc --noEmit
- `pnpm db:generate` — drizzle-kit generate (no DB needed)
- `pnpm db:migrate` — needs Postgres 18 via Unix socket
- `pnpm dev` — UI-only Vite dev on 127.0.0.1:3847
- `pnpm start` — production server. **Broken as landed** (fixed in the first build step): it runs `.output/server/index.mjs`, but the build writes `dist/server/server.js`, which opens no socket.
