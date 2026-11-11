---
schema: atelier.item
schema_version: 3
id: lor-99eq
title: 'The morning''s action fits the wall: a pinned line of 60 characters, drawn large, with quiet flags'
status: ready
mission: lor-l9a7
labels:
- chain
- feature
priority: medium
blocked_by: []
implementers: []
ready:
  by: conversion
  at: 2026-11-10T06:11:15.798545438Z
trivial: null
caused_by: null
created_at: 2026-11-10T06:11:15.798545438Z
---

## Goal

The one sentence LoR puts on the fridge is readable whole from across the room, and what waits on Sumeet is under Needs you. Today the brief's first line runs 112 to 208 characters and the wall clamps it at four lines, and on 10 November Needs you was empty while the brief listed about six decisions.

## Done when

- [ ] `lor task flag <id> "<why>" --quiet`, and `--flag "<why>" --quiet` on `lor intent create`, set the flag and mark it notified, so `sendFlags` sends nothing for it. A database test shows the task under Needs you and no `lor:flag` message.
- [ ] On the wall's Today frame, a one thing of 60 characters or fewer is drawn in a larger size that fills the box in at most two lines; longer text keeps today's size and clamp. A pure test covers the size steps, and frames for a 40, a 60 and a 150 character line are rendered and looked at (receipts).
- [ ] The `agent` page's brief section says: after writing the brief, pin its action in 60 characters or fewer with `lor wall pin "<text>" --until 22:00`; flag each decision waiting on him with `--quiet`; a quiet day pins nothing. The edit is made with `lor page update agent` and its version noted in the Report.
- [ ] `skill/SKILL.md` and the CLI help list `--quiet`; the spec's "The wall's one thing" and "Agent runs" say how the line gets there.
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.

## Scope

`cli/lor.mjs`, `src/server/api/v1.ts`, the flag function in `src/server/intents.ts`, `src/server/trmnl/frame.ts`, `tests/trmnl`, `skill/SKILL.md`, the spec, the `agent` page.

## Context

- `wall_pin` and `lor wall pin` exist and already override the automatic choice on the wall and on `/device/home`'s `one` (`src/server/trmnl/pin.ts`, `device.ts`). Nothing new stores the short line.
- The frame's clamp is `-webkit-line-clamp: 4` in `frame.ts` (about line 392).
- Sumeet agreed the brief's new shape on 2026-11-10; the `agent` page is at version 10.
