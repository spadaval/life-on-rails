---
created_at: "2026-11-08T17:49:55.955161885+00:00"
id: "lor-0v68"
issue_type: "decision"
labels:
- "launcher"
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
closed_at: "2026-11-08T18:10:47.266381057+00:00"
status: "done"
title: "Whether the shared module's browser build may run on this host"
updated_at: "2026-11-08T18:10:47.266381057+00:00"
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

The second (Sumeet, 2026-11-08: "yes, let's run the browser build on this host... development/iteration tools can run on this host, but anything heavier should run on boat"). AGENTS.md ("Rules" and "The phone app") and Sumeet's CLAUDE.md say so. `~/.gradle/init.d/no-local-builds.gradle` now allows a build only when every task is one of `:homecore`'s wasm development tasks (`compileKotlinWasmJs`, `compileDevelopmentExecutableKotlinWasmJs`, `wasmJsBrowserDevelopment*`, `wasmJsNodeTest`) and the Gradle process's own cgroup is capped at 4 GB of memory and 512 MB of swap or less (task options such as `--tests` pass); it refuses everything else, the production bundle and Android tasks included. Checked by dry runs in the spike worktree on 2026-11-08. `wasmJsBrowserDevelopmentRun` (a webpack dev server that stays up) and `wasmJsNodeTest` are allowed but weren't measured: the spike has no Node test target yet, and lor-xdoi measures both. The script that runs the build with the measured settings is lor-xdoi's.

## Report

Allowed: `:homecore` wasm development tasks on this host, capped at 4 GB of memory and 512 MB of swap, enforced by the Gradle guard (Sumeet, 2026-11-08).
