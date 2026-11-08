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
status: "in_progress"
title: "Give agents one verify skill that drives LoR's server, web screens and launcher and proves what they changed"
updated_at: "2026-11-08T05:52:25.603740925+00:00"
---

## Goal

Authors should catch their own behaviour bugs before review. In the 2026-11-08 review audit, review caught launcher bugs the author's emulator evidence missed (lor-glg9: page 1 drawn over home mid-slide; lor-aow8: an item shrinking the moment it is picked up), and the pieces agents use today (`scripts/ui-server.sh`, `scripts/ui-check.mjs`, the emulator recipe in AGENTS.md) are spread out and redone by hand each time. Build a project verify skill to the Agent Factory verify-skill standard, which atelier-l58v in the atelier repository (`~/projects/atelier`) adds to the skill.

## Done when

- [ ] A skill at `.claude/skills/verify-lor/` (also found by agents that read `.agents/skills`, if the repository uses that) has Launch, Doctor, Drive, Evidence and Cleanup sections and drives three surfaces: the API on a throwaway test server, the web screens, and the Android launcher on the local emulator.
- [ ] A control CLI in the skill does the repeatable steps with one command each, at least: `doctor` (test database, ports, emulator and what is missing, with the fix), `up` and `down` for a throwaway server on the test database and port 3857, built and served from a copy of the checkout outside it (the `/tmp/lor-ui` recipe in AGENTS.md) so `dist/` under `lor-web` is never touched, `seed` from `seeds/`, `api <method> <path>`, `screen <name> <out>` for the web screens, and `install` for the debug launcher on the emulator. Output is machine-readable where it reports state; destructive commands take `--dry-run`; errors say what to do instead.
- [ ] Every command targets only the test database (`life_on_rails_test`) and the throwaway server: `seed` and `api` refuse a database URL or port that is the real one (the env file's `DATABASE_URL`, port 3847 or 3848), and `install` sets the app's server to `http://10.0.2.2:3857` without clearing app data (clearing restores `Prefs.DEFAULT_SERVER`, the real server). A test of the CLI shows each refusal.
- [ ] The launcher is driven through T3 Code's device tools (`device_open`, then the `agent-device` command it returns), not raw adb input, and the skill says so; the CLI only builds, installs, points the app at the test server and collects logs. Gradle builds run only through `android/boat-build.sh`, never `./gradlew` on this machine, and AGENTS.md's phone section says the same instead of its local `./gradlew` recipe.
- [ ] A feature map under the skill's `references/features/` covers LoR as it behaves on `main`, one file per area: the five web screens `scripts/ui-check.mjs` drives (tasks, goals, people, notes, today), the API surface `cli/lor.mjs` calls, and the launcher. Each file says how to reach the area as a user, how to drive it with the CLI or device tools, what result proves it works, and gotchas. Behaviour that exists only on in-flight branches (lor-clgw, lor-f3vn) is left out.
- [ ] The skill is proven once end to end in a fresh session: doctor, launch, drive one feature on each surface, capture evidence (an API response, a web screenshot, a launcher screenshot), clean up, and confirm the evidence survived cleanup. The receipts are recorded.
- [ ] AGENTS.md's "How work lands" says to run the skill on the surfaces a change touches before `atelier review request`, and names the evidence to attach.
- [ ] AGENTS.md points to the skill from "Checking the screens" and "The phone app" instead of repeating the recipes it replaces.

## Scope

The skill, its CLI and feature map, AGENTS.md, and small fixes to `scripts/ui-server.sh` or `scripts/ui-check.mjs` the CLI needs. No product behaviour changes. Not the lor-uj0r work in flight.

## Context

Lauren Tan's pstack treats a verify skill as infrastructure: a control CLI agents call instead of throwaway scripts, and a feature map kept current. Agent Factory's gardener procedure (added by atelier-l58v in the atelier repository) keeps the feature map current once it exists.
