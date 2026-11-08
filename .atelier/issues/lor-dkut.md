---
created_at: "2026-11-08T03:36:23.234298329+00:00"
id: "lor-dkut"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-f3vn"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
  - kind: "issue"
    id: "lor-pmzz"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-sbth"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The phone syncs layout.json with LoR's versions again"
updated_at: "2026-11-08T03:36:23.234298329+00:00"
---

## Goal

The phone keeps its `layout.json` in step with LoR's versions again, on schema version 2: it uploads each of the owner's edits, takes a newer version on home's resume, says what changed with Undo, and reports a version it can't parse. Bring the phone half of `agent-layout` (`home/LayoutSync.kt`, `LayoutSyncer.kt`, `AgentNotice.kt`, and its changes to `HomeStore.kt`, `HomeActivity.kt`, `HomeModel.kt`, `net/LorClient.kt`) onto today's `HomeStore`.

## Done when

- [ ] `LayoutSyncTest` from `agent-layout` (10 cases) passes against today's store, with cases added for a version 2 body and for a web-written version, in `testDebugUnitTest` on boat.
- [ ] On `T3_Fold_API35` against the test server (`http://10.0.2.2:3857`), from a fresh install with a version 2 `layout.json`, each shown by a screenshot and `lor phone layout history`: the first resume uploads version 1 and the app list; a version saved on the dashboard page (an app moved into a folder) shows on home after a resume, with its note and Undo; Undo makes a new version with the one before; a minimap edit and a drag on home each make a phone version; a broken version saved as JSON is reported as `rejected` and home keeps the last good layout; an edit made with the server stopped goes up on the next resume.
- [ ] A version applied while home is locked (`locked: true`) is applied, as `layout-v2.md` says for an agent's or hand edit, and the lock stays as the version says.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

The phone's `home/` and `net/LorClient.kt` as listed in the Goal. Not the server (the item this is blocked by).

## Context

- Port by reading `git diff main...agent-layout -- android`, about 650 lines; `HomeStore` has changed a lot since (schema version 2, `GridEdit`, the lock), so expect to rewrite the store wiring rather than apply hunks.
- `agent-layout`'s choices carry over unless they break on version 2 (its plan's Increment 7, "Built 2026-11-06"): unsent edits replay on LoR's latest only while the phone holds all of them; a resume during a sync runs one more pass; a local file that doesn't parse is replaced by LoR's copy and kept as `layout.json.broken`.
- Waits for the Panels branches that change the same files, `lor-f3vn` (the minimap) and `lor-sbth` (widgets), so their edits go through the sync from the start.
- Environment: one emulator at a time; builds on boat (`android/boat-build.sh`).
