# Life on Rails v2 — agent notes

Single-user self-hosted personal OS. Spec: `docs/v2-product-engineering-spec.md` (v1 locked).

## Rules

- Never import from `archive/2023/` (frozen 2023 T3 draft). Grep it for ideas only.
- `pnpm dev` is UI-only (HMR kills the in-process job clock). Dogfood clock = `pnpm start` under `lor-web.service`.
- Scoring reads **commitments only**, never observations. `time_block.committed` is denormalized; truth = `commitment`.
- Scorer recomputes hours from `[t, t_end]` intervals with sweep-line merge. `commitment.value` is display-only.
- `proposal kind=time_attribution` always has non-null `time_block_id` + `intent_id`.
- No overlapping actual seconds: calendar fill is remainder-only (`occupied` = non-calendar blocks).
- Env = boot defaults; `user_profile` row = runtime truth (see spec precedence note).
- v1 binds `127.0.0.1:3847` only. No off-box ingest. No local password. No outbound Google writes (`outbound_enabled=false`).
- Poll path `gog` spawns always use `--json --no-input --readonly --results-only --enable-commands=calendar`.
- Never log `GOG_KEYRING_*`, tokens, or raw window titles at info level.

## Commands

- `pnpm test` — vitest (no live Google, no live xAI)
- `pnpm typecheck` — tsc --noEmit
- `pnpm db:generate` — drizzle-kit generate (no DB needed)
- `pnpm db:migrate` — needs Postgres 18 via Unix socket (host runbook)
- `pnpm dev` — UI-only Vite dev on 127.0.0.1:3847
- `pnpm start` — production server (owns the job clock)
