---
schema: atelier.item
schema_version: 3
id: lor-ea15
title: 'Receipt evidence in /tmp/lor-verify is lost at the next reboot: move it somewhere that lasts'
status: draft
mission: null
labels:
- verification
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-08T19:46:11.996927499Z
---

## Goal

Evidence that receipts point at survives a reboot. On 2026-11-08, 52 receipt references under `.atelier/` point into `/tmp/lor-verify/` (`lor-wt-clgw` 787 MB, `lor-wt-f3vn` 230 MB, `lor-wt-sfh9` 45 MB, `uj0r-validation`). /tmp is a tmpfs, so a reboot deletes them, and Sumeet plans to move /tmp to disk. Those copies also hold 1.2 GB of a 7.5 GB /tmp that other agents run out of (lor-pmzz's worker moved its verify copy to `~/.cache` for that reason).

## Done when

- [ ] The verify skill keeps its evidence (screenshots, flow logs, API responses) under a directory that survives a reboot, apart from its build copy, which may stay disposable; `.claude/skills/verify-lor/SKILL.md` says where.
- [ ] The evidence the existing receipts reference is moved there and the receipts' paths still resolve, shown by a script that checks every receipt path under `.atelier/`.
- [ ] The old build copies under `/tmp/lor-verify` are removed once nothing references them.

## Context

Found while running mission lor-ncpw (2026-11-08).

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-pmzz (Layout versions in LoR at schema version 2, with a history page)
