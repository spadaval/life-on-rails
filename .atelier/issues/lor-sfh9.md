---
created_at: "2026-11-08T05:36:44.996690636+00:00"
id: "lor-sfh9"
issue_type: "feature"
labels:
- "verification"
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Give agents one verify skill that drives LoR's server, web screens and launcher and proves what they changed"
updated_at: "2026-11-08T05:36:44.996690636+00:00"
---

## Goal

Authors should catch their own behaviour bugs before review. In the 2026-11-08 review audit, review caught launcher bugs the author's emulator evidence missed (lor-glg9: page 1 drawn over home mid-slide; lor-aow8: an item shrinking the moment it is picked up), and the pieces agents use today (`scripts/ui-server.sh`, `scripts/ui-check.mjs`, the emulator recipe in AGENTS.md) are spread out and redone by hand each time. Build a project verify skill to the Agent Factory verify-skill standard (atelier-l58v).

## Done when

- [ ] A skill at `.claude/skills/verify-lor/` (also found by agents that read `.agents/skills`, if the repository uses that) has Launch, Doctor, Drive, Evidence and Cleanup sections and drives three surfaces: the API on a throwaway test server, the web screens, and the Android launcher on the local emulator.
- [ ] A control CLI in the skill does the repeatable steps with one command each, at least: `doctor` (test database, ports, emulator and what is missing, with the fix), `up` and `down` for a throwaway server on the test database built from the current checkout (never the real database or `lor-web`), `seed` from `seeds/`, `api <method> <path>`, `screen <name> <out>` for the web screens, and `install` for the debug launcher on the emulator pointed at the test server. Output is machine-readable where it reports state; destructive commands take `--dry-run`; errors say what to do instead.
- [ ] The launcher is driven through T3 Code's device tools (`device_open`, then the `agent-device` command it returns), not raw adb input, and the skill says so; the CLI only builds, installs, points the app at the test server and collects logs. Builds that need Gradle follow AGENTS.md (boat for heavy builds).
- [ ] A feature map under the skill's `references/features/` lists what LoR does from the user's side across web, API and launcher, one file per area, each with how to reach it, how to drive it with the CLI or device tools, what result proves it works, and gotchas.
- [ ] The skill is proven once end to end in a fresh session: doctor, launch, drive one feature on each surface, capture evidence (an API response, a web screenshot, a launcher screenshot), clean up, and confirm the evidence survived cleanup. The receipts are recorded.
- [ ] AGENTS.md points to the skill from "Checking the screens" and "The phone app" instead of repeating the recipes it replaces.

## Scope

The skill, its CLI and feature map, AGENTS.md, and small fixes to `scripts/ui-server.sh` or `scripts/ui-check.mjs` the CLI needs. No product behaviour changes. Not the lor-uj0r work in flight.

## Context

Lauren Tan's pstack treats a verify skill as infrastructure: a control CLI agents call instead of throwaway scripts, and a feature map kept current. The Agent Factory gardener (atelier-l58v) keeps the feature map current once it exists.
