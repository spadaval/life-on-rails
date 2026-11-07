# Life on Rails v2 — agent notes

Single-user self-hosted personal OS. What LoR is, what it is not, and how it sits next to T3 Code and the agent are in `docs/v2-product-engineering-spec.md` ("Who is who", "Settled choices"), with the state of the build; read it first. A change that crosses one of those decisions changes the spec too.

## Rules

- Personal project, one box, no users: no staged rollout. Work is tracked in Atelier (`.atelier/`), worked the Agent Factory way (see "How work lands").
- Prefer the simplest shape: no fallbacks, watchdogs, retention, or permission layers until their absence is felt.
- Add a table when the code that uses it is written.
- Never import from `archive/2023/` (frozen 2023 draft).
- `pnpm dev` is UI-only. The scheduler starts only from `scripts/serve.mjs`.
- Scoring reads **commitments only**, never observations. `time_block.committed` is denormalized; truth = `commitment`.
- The scorer recomputes hours from `[t, t_end]` intervals with a sweep-line merge. `commitment.value` is display-only. A pause is never inside a block.
- No overlapping actual seconds. Calendar, phone (`auto_activity`), and place blocks are fill: `syncFill` runs calendar, then phone, then place fill, each deciding which of its blocks should exist and changing only what differs. Precedence: timer/manual and anything committed, then calendar, then phone, then place.
- Every writer of `time_block` calls `lockBlocks(tx)` first and re-syncs fill in the same transaction.
- Local-day arithmetic goes through `src/shared/localTime.ts`. Never add 24 hours to move a day.
- Queries are SQL through postgres.js; pass JSON with `sql.json(...)`, never a pre-stringified string.
- Server functions return JSON-safe values only (ISO strings, not Dates).
- Config is the env file only (`~/.config/life-on-rails/env`); every comment in it on its own line.
- Binds `127.0.0.1:3847`. The `:3848` listener is the only thing `tailscale serve` targets, and it checks the owner's identity header. The one listener on the home network is the TRMNL X's (`LOR_TRMNL_BIND:3849`, off when unset): the firmware's `/api/setup|display|log` and its images, every call keyed; and the firmware build in `LOR_TRMNL_FIRMWARE` by its exact name, unkeyed because the updater sends no headers. Nothing else.
- Credentials live in mode-0600 files under `~/.config/life-on-rails/`: the env file for keys set by hand, a file of their own for tokens LoR obtains at runtime (like `google-token.json`), which LoR writes when it gets them. Never in tables or logs. Titles and subjects may be logged.

## How work lands

Since 2026-11-06 LoR uses Atelier, with records under `.atelier/`.

- Start a session with `atelier status`; before handoff commit the records and run `atelier land`. Active work may remain open, with unfinished code handed off honestly. The Claude Code hooks run both.
- One issue, in order: `atelier start <id>` (an item with a recorded branch switches to it; otherwise on `main` it creates a branch named `<id>`, and on another branch it takes that branch), implement, tick every Done when line and write the `## Report`, `atelier review request <id>`, fix and re-review, approve, then `atelier merge <id>`, which marks the work done. Work has no close step. Only a code change makes an approval stale, though a rewritten Done when line still gets a fresh one. Work that changes no code and can only happen after a merge or release is a loose end, unless it is an independent validation; a change with no issue takes the light path. Agent Factory's Atelier reference has the detail and defines check, review, land, done and close.
- **A non-trivial change goes on a work branch**, one worktree per agent working in parallel. A fresh agent that didn't write it reviews it before `atelier merge` puts it on `main`. A fix made after review is reviewed again, but only the fix's diff. Sumeet reviews after merge.
- **Straight to `main`:** planning records and mission strategies (`docs/missions/<slug>/strategy.md`, linked from the mission), and trivial changes, recorded with `atelier review trivial --base <commit>`. A non-trivial planning change is still reviewed by a fresh agent before the work it defines starts, and a mission moves to ready only after Sumeet approves it (`atelier review approve <mission>`).
- **Evidence:** record a receipt only for a check nothing else records, such as an emulator or device check, screenshots, a measurement or a failure. Don't record `pnpm test` or a boat build passing; name the test that proves the claim in the handoff note.
- **A check that only the real phone or Fold can answer** is a loose end, `blocked_by` the item it follows; its emulator stand-in stays a Done when line of the item and runs before merge.
- The launcher missions before Atelier (`docs/missions/t3-launcher`, `launcher-polish`) keep their `plan.md` as their record. The Panels mission is `lor-uj0r`.

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

The in-app browser preview may be off for a thread. `scripts/ui-server.sh` starts a throwaway server on the **test** database at `:3857`, and `node scripts/ui-check.mjs <tasks|goals|people|notes|today> <out-dir>` drives it in the system Chrome and writes screenshots. Never point either at the real database. Both serve `dist/` of the checkout they run from, and `pnpm build` in the repo swaps the assets under the running `lor-web` (blank pages until it restarts), so to check unfinished work, rsync the repo (without `node_modules`, `dist`, `.git`) to `/tmp/lor-ui`, symlink `node_modules`, and build and run the scripts there.

## The phone app

`android/` is a Kotlin + Compose app (the phone's sensors, the timer tile, settings). `cd android && ./gradlew assembleDebug testDebugUnitTest`. Install over wireless adb (`~/Android/Sdk/platform-tools/adb`, phone at `100.83.51.27` over Tailscale; find the port with `nmap -p 30000-50000`). Try it first on the emulator (`sg kvm -c start-t3-emulator`) pointed at the test server (`http://10.0.2.2:3857`), never the real one. The server side is `src/server/device.ts`.

## Talking to the running service

`lor` (symlinked from `cli/lor.mjs`) is the agent's CLI; `skill/SKILL.md` teaches it. `lor status` shows every job. Logs: `journalctl --user -u lor-web -f`; the unattended runs: `journalctl --user -u assistant-mail -u assistant-nightly -u assistant-brief`. A run reaches the owner with `lor message` (WhatsApp), or by flagging a task (`lor task flag`, or `--flag` on create), which the channel sends. The channel's code is `src/server/channel/`.

## The wall display

A TRMNL X (10.3" e-paper, 1872x1404) on the fridge, pointed at LoR as its own server. Every 15 minutes it calls `/api/display`; LoR draws `src/server/trmnl/frame.ts` in the system Chrome (`playwright-core`) and serves a 2-bit grayscale PNG. Three screens: a timed wake draws Today (the day arc and the one thing, which `lor wall pin` overrides). The device runs our firmware build (`~/projects/trmnl-firmware`, branch `lor-prefetch`, version suffix `-lor<N>`, bootloader with app rollback): each response carries the other two screens, stored without being shown, so left and right on the touch bar flip through all three without reaching LoR; a middle tap fetches the screen being browsed (its `Browsing` header) fresh; holding the middle opens a menu of Diagnostics, Reset Wi-Fi, Shut Down. A new build confirms itself after its first fetch and draw; a restart before that boots the previous one. LoR offers the build named by `LOR_TRMNL_FIRMWARE` over the air to a device reporting any other version. Building, releasing over the air, flashing over USB and testing rollback are in that repo's `scripts/lor/README.md`. Check a change with `pnpm test` (`tests/trmnl`) and by rendering a frame to a file; the firmware source is `github.com/usetrmnl/trmnl-firmware`.

## Design

The look is "Elite: Mostly Harmless": `DESIGN.md` is the system, `docs/design/README.md` the short history (including the unbuilt "Astral" alternative). The impeccable design skill is vendored at `.claude/skills/impeccable/`.
