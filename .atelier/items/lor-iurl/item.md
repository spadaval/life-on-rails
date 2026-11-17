---
schema: atelier.item
schema_version: 3
id: lor-iurl
title: pnpm typecheck passes in a fresh checkout without a build first
status: ready
mission: null
labels:
- tools
priority: medium
blocked_by: []
implementers: []
ready:
  by: lead of lor-2803, from the design audit
  at: 2026-11-17T14:42:51.248900263Z
trivial: null
caused_by: null
created_at: 2026-11-17T14:42:10.730610865Z
---

## Goal

`pnpm typecheck` passes in a fresh checkout or worktree. Today it fails there because `src/routeTree.gen.ts` is gitignored and only a Vite run writes it, so every implementer in a new worktree copies the file from the main checkout to get past it.

## Done when

- [ ] In a new `git worktree` with dependencies installed and no build run, `pnpm typecheck` passes: the script generates the route tree first, or the file is committed.
- [ ] `pnpm build` and `pnpm dev` still regenerate it.

## Scope

`package.json`'s `typecheck` script, `.gitignore`, the TanStack Router generator config.

## Context

- Hit by the implementers of lor-zj7x, lor-nde9 and lor-zwfd on 2026-11-17.
