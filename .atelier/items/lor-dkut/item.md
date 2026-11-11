---
schema: atelier.item
schema_version: 3
id: lor-dkut
title: The phone syncs layout.json with LoR's versions again
status: ready
mission: lor-ncpw
labels:
- launcher
- feature
priority: medium
blocked_by:
- lor-pmzz
implementers: []
ready:
  by: conversion
  at: 2026-11-08T03:36:23.234298329Z
trivial: null
caused_by: null
created_at: 2026-11-08T03:36:23.234298329Z
---

## Goal

The phone keeps its `layout.json` in step with LoR's versions again, on schema version 2: it uploads each of the owner's edits, takes a newer version on home's resume, says what changed with Undo, and reports a version it can't parse. Bring the phone half of `agent-layout` (`home/LayoutSync.kt`, `LayoutSyncer.kt`, `AgentNotice.kt`, and its changes to `HomeStore.kt`, `HomeActivity.kt`, `HomeModel.kt`, `net/LorClient.kt`) onto today's `HomeStore`.

## Done when

- [ ] `LayoutSyncTest` from `agent-layout` (10 cases) passes against today's store, with cases added for a version 2 body and for a web-written version, in `testDebugUnitTest` on boat.
- [ ] On `T3_Fold_API35` against the test server (`http://10.0.2.2:3857`), from a fresh install with a version 2 `layout.json`, each shown by a screenshot and `lor phone layout history`: the first resume uploads version 1 and the app list; a version saved on the dashboard page (an app moved into a folder) shows on home after a resume, with its note and Undo; Undo makes a new version with the one before; a minimap edit and a drag on home each make a phone version; a broken version saved as JSON is reported as `rejected` and home keeps the last good layout; an edit made with the server stopped goes up on the next resume.
- [ ] A version's notice shows once: after it is dismissed or its Undo used, and after `HomeActivity` is rebuilt, it doesn't show again, and its Undo can't fire later. Rebuild as lor-0xhc does: `adb shell settings put global always_finish_activities 1`, then another app and back (not `cmd uimode`, which `HomeActivity` handles without a rebuild), with 'HomeActivity created' in logcat, then `adb shell settings put global always_finish_activities 0` so later checks run normally. Shown by a screenshot after the rebuild and by `lor phone layout history` showing no new version. (The shared Undo bar has this bug for LoR writes, lor-0xhc; `agent-layout`'s `AgentNotice` is separate from it, so it must not take the bug on.)
- [ ] A version applied while home is locked (`locked: true`) is applied, as `layout-v2.md` says for an agent's or hand edit, and the lock stays as the version says.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

The phone's `home/` and `net/LorClient.kt` as listed in the Goal. Not the server (the item this is blocked by).

## Context

- Port by reading `git diff main...agent-layout -- android`, about 650 lines; `HomeStore` has changed a lot since (schema version 2, `GridEdit`, the lock), so expect to rewrite the store wiring rather than apply hunks.
- `agent-layout`'s choices carry over unless they break on version 2 (its plan's Increment 7, "Built 2026-11-06"): unsent edits replay on LoR's latest only while the phone holds all of them; a resume during a sync runs one more pass; a local file that doesn't parse is replaced by LoR's copy and kept as `layout.json.broken`.
- Panels has landed (lor-uj0r), so the minimap's and widgets' edits go through the sync from the start; port onto today's `HomeStore`, not the one `agent-layout` knew.
- Environment: one emulator at a time; builds on boat (`android/boat-build.sh`).

## Links

Relationships of the old record that the new format has no field for.

- related: lor-0xhc (A LoR write's Undo bar shows again after home is rebuilt)
