---
created_at: "2026-11-08T17:49:55.955161885+00:00"
id: "lor-0v68"
issue_type: "decision"
labels:
- "launcher"
- "needs-human"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-8a7n"
    type: "related"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Whether the shared module's browser build may run on this host"
updated_at: "2026-11-08T17:49:55.955161885+00:00"
---

## Context

- Spike lor-8a7n measured the shared module's browser build on this host: a one-line edit reaches a browser bundle in 12 to 14 s, about 4x faster than boat's production build, peaking near 3 GB and holding about 2.6 GB while warm. The production bundle doesn't fit in 4 GB.
- Today `~/.gradle/init.d/no-local-builds.gradle` refuses every Gradle build here unless `T3_ALLOW_LOCAL_GRADLE=1`, which only Sumeet may set, and AGENTS.md says Gradle runs only on boat.
- Moving `/tmp` off tmpfs (planned by Sumeet) frees about 2 GB of RAM; another session's 6.4 GB job is what made the first measurement fail.

## Question

May the shared module's browser build run on this host for fast UI iteration?

## Options

- Keep every Gradle build on boat.
- Allow only `:homecore`'s development browser tasks here: in a capped scope in the agents slice (4 GB), Gradle `-Xmx1g`, Kotlin daemon `-Xmx1536m`, two workers, incremental wasm, a short daemon idle timeout, one at a time and never beside an emulator. The guard is narrowed to allow just those tasks and AGENTS.md says so.

## Decision

Open: Sumeet's call (asked 2026-11-08).
