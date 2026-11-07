---
created_at: "2026-11-07T23:35:27.737898826+00:00"
id: "lor-73li"
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
    id: "lor-f3vn"
    type: "related"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T23:35:30.694554577+00:00"
status: "done"
title: "How the layout lock is held and what it stops"
updated_at: "2026-11-07T23:42:51.963096242+00:00"
---

## Context

- Rejected: a preference outside the file, as Lawnchair keeps it. An agent rearranging home couldn't see that Sumeet locked it, and the strategy has phone edits go through the file.
- Rejected: a `locked` on each layout. The lock is about the phone in Sumeet's hand, not one arrangement, and a switch of `active` would silently unlock.
- Rejected: each edit checking the lock itself. Every later edit (lor-r0f9's widget edits, lor-lz97's) would have to remember to, and one that forgot would slip through.
- Rejected: locking Home settings and the drawer too. Lawnchair's lock leaves its settings and drawer alone, and Smart Launcher's padlock locks the layout. Hiding the drawer's view switch would only add a mode to explain.

## Question

How does `layout.json` hold the layout lock that lor-f3vn adds, and which of the phone's edits does it stop? The Outcome asks for a lock that stops every edit on home (drags, Remove, folder changes), set in the minimap and the home menu, as Smart Launcher's padlock does. It is a persisted contract an agent or hand edit reads, and it decides which of `HomeStore`'s edits refuse, including the ones lor-sbth and lor-clgw add later.

## Options

- **Where it lives.** A top-level field of `layout.json`, covering every layout; a field on each layout; or a preference outside the file, as Lawnchair keeps `lock_home_screen` (Apache-2.0, `PreferenceManager2.kt`). The strategy's Shape has phone edits go through the file, and the item's goal names one top-level field.
- **What it stops.** Each edit declares whether it is a home edit and checks the lock itself; or one rule in the edit path, on what an edit changes: an edit whose result differs in the grid is refused, whatever code made it.
- **Whose edits.** Only the phone's edits through `HomeStore`; or also an agent's or a hand edit of the file.

## Decision

A top-level `"locked": true`, enforced by one rule in the edit path on what an edit changes, for the phone's edits only (planner, 2026-11-07, within the strategy's authority over field names; the Outcome already asks for the lock).

- **The field.** `locked` at the top level: `true` or `false`, anything else an error naming its place (`locked: must be true or false`). Absent means false. The phone writes it only when true, as it writes `drawer.keyboard`. No `version` bump (an added optional field).
- **What it stops.** While the file `LayoutStore.edit` reads holds `"locked": true`, an edit whose result differs from that file in `active` or `layouts` is refused with "home is locked; unlock it from the home menu", and nothing is written. That covers every edit to the grid, the dock, folders, `start` and `wide`, the minimap's and those later items add, with no list to keep current. Unlocking changes only `locked`, so it is taken.
- **What it doesn't stop.** Edits to `hidden`, `drawer` and `look` alone: the drawer's view and sections, Unhide, Home settings' look. Hide of an app that is on home or in the dock changes `layouts`, so it is refused; Hide of one that isn't is taken. Undo isn't checked: after Lock, the Undo held is the lock's own, so Undo unlocks. `restore` of a broken file and the first run's default aren't edits. An agent or a hand edit of the file isn't stopped: the lock guards against an accidental touch on the phone, and the file stays the place an agent rearranges home.
- **On screen** (lor-f3vn's children): what the rule would refuse is hidden or does nothing, so a locked home rarely shows a refusal. A long press still opens an app's menu without its editing entries, and a drag picks nothing up and says why in the Undo bar.
- Consequences: `layout-v2.md` documents `locked` under Top-level fields as a version 2 addition; the refusal lives in `LayoutStore.edit`, which `HomeStore.edit` calls, so the JVM tests reach it.

## Notes

- 2026-11-07 [note] Review residual risk (lor-f3vn's plan review): the refusal names the home menu, but a grid with no stack has no home menu; there the minimap's padlock, opened from Home settings' Panels row (lor-6get), unlocks. The contract is unchanged.

## Report

A top-level locked: true; while it is set, LayoutStore.edit refuses any phone edit that changes active or layouts; hidden, drawer and look stay editable; agents and hand edits aren't stopped
