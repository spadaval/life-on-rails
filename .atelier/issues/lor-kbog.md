---
created_at: "2026-11-08T06:02:35.011400325+00:00"
id: "lor-kbog"
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
    id: "lor-llmf"
    type: "related"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T06:02:35.343056208+00:00"
status: "done"
title: "The browser bundle is built in GitHub Actions and fetched at deploy when the shared module changed"
updated_at: "2026-11-08T06:02:35.343056208+00:00"
---

## Context

- `spadaval/life-on-rails` is public, so Actions minutes are free, and the standard Linux runner (4 vCPU, 16 GB) is larger than boat's `default` VM (4 vCPU, 8 GB).
- Measured in spike lor-llmf on boat's warm VM: about 45 to 50 s per bundle build, 1 min 48 s the first time (Node.js download). The bundle is 11.4 MB raw, 4.2 MB gzipped: Skia's `.wasm` 8.6 MB (3.3 MB gz, unchanged by our code), home's `.wasm` 2.3 MB (0.73 MB gz), `homecore.js` 0.5 MB (0.1 MB gz). Content-hashed names, so browsers re-download only what changed.
- Today's deploy (`pnpm build && systemctl --user restart lor-web`) needs no push; with this, a deploy that changes the shared module needs the commit pushed and CI finished first. Most deploys don't touch it.
- Open for lor-kaq8: where the deploy keeps the bundle it already has, since `pnpm build` rebuilds `dist/`, and that Actions artifacts expire after 90 days by default (a release asset per build doesn't).
- The same workflow could build the APK and run `testDebugUnitTest`; that is a separate decision.

## Question

How does the browser bundle (the `wasmJsBrowserDistribution` output of the shared Kotlin module) reach LoR's `dist/` at deploy?

## Options

- Commit the built files: rejected outright (Sumeet, 2026-11-08: "why would we ever commit build results?"). It is about 11 MB raw per build.
- Build it on boat at each deploy.
- Build it in GitHub Actions on every push to `main`, and have the deploy fetch it.

## Decision

GitHub Actions (Sumeet, 2026-11-08). A workflow builds `:homecore:wasmJsBrowserDistribution` on each push to `main` and keeps the bundle as an artifact named by commit. The deploy reuses the bundle it already has unless something under the shared module changed since; then it fetches the bundle for that commit (`gh run download`), and fails with a clear message if CI hasn't built it yet. No other build path in the deploy.

## Report

GitHub Actions builds the bundle on push to main; deploy fetches it when the shared module changed (Sumeet, 2026-11-08).
