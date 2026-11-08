---
created_at: "2026-11-08T05:36:44.996690636+00:00"
id: "lor-sfh9"
issue_type: "feature"
labels:
- "verification"
workflow_branch:
  branch_base: main
  work_branch: lor-sfh9
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T06:39:05.394640805+00:00"
status: "done"
title: "Give agents one verify skill that drives LoR's server, web screens and launcher and proves what they changed"
updated_at: "2026-11-08T06:39:05.394640805+00:00"
---

## Goal

Authors should catch their own behaviour bugs before review. In the 2026-11-08 review audit, review caught launcher bugs the author's emulator evidence missed (lor-glg9: page 1 drawn over home mid-slide; lor-aow8: an item shrinking the moment it is picked up), and the pieces agents use today (`scripts/ui-server.sh`, `scripts/ui-check.mjs`, the emulator recipe in AGENTS.md) are spread out and redone by hand each time. Build a project verify skill to the Agent Factory verify-skill standard, which atelier-l58v in the atelier repository (`~/projects/atelier`) adds to the skill.

## Done when

- [x] A skill at `.claude/skills/verify-lor/` (also found by agents that read `.agents/skills`, if the repository uses that) has Launch, Doctor, Drive, Evidence and Cleanup sections and drives three surfaces: the API on a throwaway test server, the web screens, and the Android launcher on the local emulator.
- [x] A control CLI in the skill does the repeatable steps with one command each, at least: `doctor` (test database, ports, emulator and what is missing, with the fix), `up` and `down` for a throwaway server on the test database and port 3857, built and served from a copy of the checkout outside it (the `/tmp/lor-ui` recipe in AGENTS.md) so `dist/` under `lor-web` is never touched, `seed` from `seeds/`, `api <method> <path>`, `screen <name> <out>` for the web screens, and `install` for the debug launcher on the emulator. Output is machine-readable where it reports state; destructive commands take `--dry-run`; errors say what to do instead.
- [x] Every command targets only the test database (`life_on_rails_test`) and the throwaway server: `seed` and `api` refuse a database URL or port that is the real one (the env file's `DATABASE_URL`, port 3847 or 3848), and `install` sets the app's server to `http://10.0.2.2:3857` without clearing app data (clearing restores `Prefs.DEFAULT_SERVER`, the real server). A test of the CLI shows each refusal.
- [x] The launcher is driven through T3 Code's device tools (`device_open`, then the `agent-device` command it returns), not raw adb input, and the skill says so; the CLI only builds, installs, points the app at the test server and collects logs. Gradle builds run only through `android/boat-build.sh`, never `./gradlew` on this machine, and AGENTS.md's phone section says the same instead of its local `./gradlew` recipe.
- [x] A feature map under the skill's `references/features/` covers LoR as it behaves on `main`, one file per area: the five web screens `scripts/ui-check.mjs` drives (tasks, goals, people, notes, today), the API surface `cli/lor.mjs` calls, and the launcher. Each file says how to reach the area as a user, how to drive it with the CLI or device tools, what result proves it works, and gotchas. Behaviour that exists only on in-flight branches (lor-clgw, lor-f3vn) is left out.
- [x] The skill is proven once end to end in a fresh session: doctor, launch, drive one feature on each surface, capture evidence (an API response, a web screenshot, a launcher screenshot), clean up, and confirm the evidence survived cleanup. The receipts are recorded.
- [x] AGENTS.md's "How work lands" says to run the skill on the surfaces a change touches before `atelier review request`, and names the evidence to attach.
- [x] AGENTS.md points to the skill from "Checking the screens" and "The phone app" instead of repeating the recipes it replaces.

## Scope

The skill, its CLI and feature map, AGENTS.md, and small fixes to `scripts/ui-server.sh` or `scripts/ui-check.mjs` the CLI needs. No product behaviour changes. Not the lor-uj0r work in flight.

## Context

Lauren Tan's pstack treats a verify skill as infrastructure: a control CLI agents call instead of throwaway scripts, and a feature map kept current. Agent Factory's gardener procedure (added by atelier-l58v in the atelier repository) keeps the feature map current once it exists.

## Notes

- 2026-11-08 [note] Handoff (author lor-sfh9): all eight Done when lines ticked; see ## Report. Includes an incident: a test run with the guard disabled wrote 5 rows to the real database at 06:00:24 UTC, removed with Sumeet's approval; the Report says how and what now prevents it. Proof: tests/verify-lor.test.ts (18 cases, pnpm test 167 pass); fresh-session run at 5da1e2e (lor-inh3, lor-8ma4, lor-dwwz, lor-dz4i); final run at be80731 (lor-gkbm). Review outstanding. Follow-up lor-mxrc.

## Report

All eight Done when lines hold. `.claude/skills/verify-lor/` has the skill (Safety, Doctor, Launch, Drive, Evidence, Cleanup), its control CLI `verify.mjs` (`doctor`, `up`, `down`, `seed`, `api`, `screen`, `build`, `install`, `logs`, `evidence`; JSON on stdout, `--dry-run` on everything that changes state, every error ends with what to do instead) and a feature map of seven files (`tasks`, `goals`, `people`, `notes`, `today`, `api`, `launcher`). The repository has no `.agents/skills`, so the skill lives only under `.claude/skills`. `tests/verify-lor.test.ts` (18 cases, in `pnpm test`; 167 pass) shows each refusal: the env file's `DATABASE_URL` and any other database for `seed`, `api` and `up`; ports 3847, 3848, 3849 and the env file's ports for `api`, `screen` and `install`; a listener that is not a throwaway server; a copy dir that overlaps the checkout; evidence inside the copy; a physical serial for `install`; and that `install` keeps app data and points the app at `http://10.0.2.2:3857`. With the database and port guards disabled, 10 of those cases fail. A fresh agent ran the skill end to end from SKILL.md alone at 5da1e2e (receipts lor-inh3 API, lor-8ma4 web, lor-dwwz launcher, lor-dz4i cleanup and survival). I reran the whole sequence after the last change, at be80731 (receipt lor-gkbm): doctor, up, build on boat, install, one feature per surface, evidence, down, and the same 14 files with identical sha256 after cleanup. AGENTS.md says to run the skill before `atelier review request` and which evidence to attach, and points to it from "Checking the screens" and "The phone app", whose local `./gradlew` recipe is gone.

### Incident: a write to the real database

At 06:00:24 UTC I proved the refusal test could fail by disabling `guardDb` in `verify.mjs` and rerunning the test. Two `seed` cases were not dry runs, so with the guard off `seeds/admin.json` loaded into `life_on_rails`: goal "Stay on top of admin" (01a1b9be-611e-7872-96be-4f0fbfceb309), task "Clear inbox to zero" (01a1b9be-6124-7904-bf97-6e7504bc96a1), one `intent_link` and two `score_snapshot` rows. Nothing went to Google (`tasksSync.ts` is one way, Google to LoR), and lor-web logged nothing after 05:55 UTC. With Sumeet's approval, after read-only checks found no other reference to the two intents, one transaction deleted exactly those five rows (2, 1, 2 before; 0, 0, 0 after). What stops a repeat: every test case is now `--dry-run`, so a broken guard prints a plan instead of acting; `run()` refuses to start any child whose `DATABASE_URL` is not `life_on_rails_test` and strips an inherited one, so disabling `guardDb` alone no longer reaches a database (checked with the guard disabled against a database that does not exist: refused, exit 3); SKILL.md says never to weaken a guard to test it against a real target. No command, flag or test in the skill can disable a guard.

### Decisions

- `up` builds in `/tmp/lor-ui-<checkout name>` and calls `node_modules/.bin/vite` and `tsx` directly: `pnpm` runs an install before scripts, which fails on the symlinked `node_modules`.
- `install` writes `server_url` into `shared_prefs/lor.xml` with `run-as` (no product change was allowed, and the app has no intent for it), stopping the app before and after and retrying, because a running home app rewrites its prefs from memory. It sets the URL before `adb install -r` as well as after, because the install restarts LoR at once as the default home.
- `api` also checks that the process on the port is `scripts/serve.mjs` with `LOR_PORT` and a test `DATABASE_URL` in its environment, so a guard on the port number is not the only line.
- `screen` also takes a path (`screen /goals`) for one screenshot without the flows' data wipes.

### Surprises

- The emulator's LoR app had no `server_url` before this work, so it was on `Prefs.DEFAULT_SERVER`, the real server. `doctor` now reports this. The real database has no observation from device `emulator`, so nothing reached it.
- The `today` flow of `scripts/ui-check.mjs` fails on main (it waits for a "Just start" button Today no longer has), and the `goals` flow prints `tasks for vendors: null` though its screenshot is right. Filed as lor-mxrc; the feature map says so and points Today at `screen /` and the API.
- An idle `agent-device` session from an earlier thread (created 04:03 UTC) holds `emulator-5554`; `open --wait 30000` gets past it. The fresh runs also found that the booted emulator's id is `emulator-5554`, not `T3_Phone_API35`, and that `snapshot -i` hides the "Connected" line. Both are in the docs now.

### Open items

- lor-mxrc (backlog): fix the today and goals flows.
- Typecheck fails on main in a fresh worktree (60 errors from the gitignored, ungenerated `src/routeTree.gen.ts`); not checked here, and the CLI is plain `.mjs`.
- Review outstanding: the orchestrator arranges it.
