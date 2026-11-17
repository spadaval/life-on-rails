---
schema: atelier.item
schema_version: 3
id: lor-03c2
title: boat-build.sh says the boat VM is stopped instead of failing silently
status: draft
mission: null
labels:
- tools
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T14:42:10.706640504Z
---

## Goal

`android/boat-build.sh` fails with a sentence when the boat VM is stopped. Today the first `boat exec` fails under `set -e` and the script exits with no line saying why, which cost an implementer time on 2026-11-17 (lor-3hnk).

## Done when

- [ ] Run against a stopped VM, the script prints that the VM is stopped and the `boat` command that starts it, and exits non-zero. Or it starts the VM itself and says so.
- [ ] Run against a running VM, nothing changes.

## Scope

`android/boat-build.sh`.

## Context

- Seen during lor-3hnk on 2026-11-17.
