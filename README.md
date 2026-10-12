# Life on Rails v2 — self-hosted personal operating system

Intention-aware time tracker on one box (`buildbot`, `127.0.0.1:3847`).
Spec: `docs/v2-product-engineering-spec.md`. Agent notes: `AGENTS.md`.
Predecessor draft (frozen): `archive/2023/`.

## Stack (exact pins in `package.json`)

TanStack Start + React 19 + Tailwind 4 + PostgreSQL 18 (apt, Unix socket) +
Drizzle + postgres.js + Zod 4 + Vitest. pnpm. Node 22+.

## Quick start (operator)

```bash
pnpm install
cp .env.example ~/.config/life-on-rails/env   # 0600; fill keys, GOG_KEYRING_*, DATABASE_URL
chmod 600 ~/.config/life-on-rails/env
pnpm db:migrate
pnpm start                 # dogfood: 127.0.0.1:3847, owns the job clock
# pnpm dev is UI-only (HMR); do not rely on it for calendar_poll
```

Postgres host runbook (needs `sudo apt`, not a code PR): see spec
“Host, install, rollout”. Port `3847` is chosen to avoid the existing
Tailscale Serve `/` → `127.0.0.1:3773`; a later `/lor` path must not reset `/`.

## Layout

`src/shared/` envelope + ingest contract · `src/db/` Drizzle schema/client/migrate/seed ·
`src/server/{google,capture,attribution,scoring,agent,jobs,functions}/` ·
`src/routes/` Start routes · `agents/linux-activity/` on-box sensor daemon ·
`tests/` vitest suites · `seeds/` graph seed JSON · `scripts/` runbook helpers +
systemd units.
