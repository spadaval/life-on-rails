---
created_at: "2026-11-08T17:26:54.028957881+00:00"
id: "lor-8a7n"
issue_type: "spike"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-llmf"
    type: "related"
  - kind: "issue"
    id: "lor-ncpw"
    type: "related"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T17:27:02.667543454+00:00"
status: "superseded"
title: "Spike: the shared module's browser build on this host takes 12 s per edit and 3 GB"
updated_at: "2026-11-08T17:27:02.667543454+00:00"
---

## Goal

Find out what it costs to build only the shared Kotlin module's browser target (`:homecore`, `wasmJs`) on this host, to decide whether that build may run here for fast UI iteration while Android builds stay on boat (Sumeet, 2026-11-08: "My hunch is that compiling just the Kotlin module is very light. Verify that.").

## Done when

- [x] The browser build of the spike's module (`spike/kmp-browser`, `cb91b53`) measured on this host in a capped scope (`systemd-run --user --scope`, agents slice, `MemoryMax=4G`, `MemorySwapMax=512M`): wall time, CPU seconds and the scope's peak memory for a clean compile, a no-op, a one-line edit through compile and through the development bundle, and the production bundle.
- [x] The same steps in more than one Gradle and Kotlin configuration, to find one that fits under the cap.
- [x] The Report says whether the hunch holds and what a rule for this host would need.

## Report

Compiling the module is light in time but not in memory. With incremental wasm linking and a capped Kotlin daemon, a one-line edit reaches a browser bundle in 12 to 14 s on this host, about 4x faster than boat's 45 to 50 s production build, but Gradle and Kotlin hold about 2.6 GB while the loop is warm. The production bundle doesn't fit in 4 GB here and stays in GitHub Actions (decision lor-kbog).

Run with `T3_ALLOW_LOCAL_GRADLE=1` past `~/.gradle/init.d/no-local-builds.gradle`, for this measurement only, at Sumeet's request. Script: the session scratchpad's `wasm-measure.sh`; dependencies were already cached for later runs (the first run's 45 s includes resolving them).

Edit loop (wall, CPU, scope's peak memory):

| Configuration | Compile after an edit | Development bundle after an edit | Production bundle |
|---|---|---|---|
| Project defaults (`-Xmx2g`, Kotlin daemon uncapped) | clean compile 4.8 s, 20 CPU s, 2.3 GB | killed by the 4 GB cap | not reached |
| Lean (Gradle `-Xmx1g`, compiler in process, 2 workers) | 15.9 s, 52 CPU s, 2.0 GB | 53 s, 181 CPU s, 2.2 GB | 103 s, 353 CPU s, 2.8 GB |
| Lean plus `kotlin.incremental.wasm=true` | 14.5 s, 51 CPU s, 2.0 GB | 24 s, 78 to 86 CPU s, 2.2 GB | 98 s, 336 CPU s, 2.7 GB |
| Gradle `-Xmx1g`, Kotlin daemon `-Xmx1536m`, 2 workers, incremental wasm | 6.7 s, 30 CPU s, 2.4 GB | 12.0 to 13.6 s, 32 to 40 CPU s, 2.8 to 3.0 GB | killed by the 4 GB cap |

- In the incremental lean run, a development bundle after an edit was 12.3 s compiling (`compileKotlinWasmJs`), 7.3 s linking (`compileDevelopmentExecutableKotlinWasmJs`) and about 1.5 s of Gradle startup and configuration (`--profile`). The separate warm Kotlin daemon is what brings the compile from 12 s to under 7 s.
- A no-op build is 2.2 s. After the builds, 2.1 GB (lean) to 2.6 GB (daemon) stays resident until the daemons are stopped.
- The host is the constraint. Free memory moved between 2.0 and 10.4 GB during the hour as other sessions ran; another session's `llama-tts` held 6.4 GB. The first attempt was killed by systemd-oomd under that pressure, and two runs by the scope's own cap; the T3 server stayed up each time.
- What a rule for this host would need: the browser build of `:homecore` only, development tasks only (no production bundle, no Android task), in a capped scope in the agents slice, with the daemon settings above and a short idle timeout so 2.6 GB doesn't stay resident, one at a time and never beside an emulator. The guard (`no-local-builds.gradle`) would need to allow it, which is Sumeet's call.
