---
created_at: "2026-11-09T01:36:22.879187753+00:00"
id: "lor-bdus"
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
    id: "lor-5qkq"
    type: "advances"
  - kind: "issue"
    id: "lor-72c3"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "No dock: its apps join home"
updated_at: "2026-11-09T01:45:12.461701033+00:00"
---

## Goal

Home has no dock. Sumeet finds it useless, and the usage-built home puts the most-used apps in the rows nearest the thumb instead. The layout's `dock` list and the `dock` block go, and an old file's dock apps move into home so none is lost.

## Done when

- [ ] The parser still reads a layout with `dock` and stacks with a `dock` block, and converts them without losing an app: every `dock` block is dropped, a stack left with no blocks is removed (unless it is at a `start` cell, where it stays as a stack with an empty `apps` block), and each dock app not already on home is appended, in dock order, to the first page (`Layout.pages`, in their order) with room, else to a new page made as Add to home makes one. A dock app that can't be placed (every page full and no room for a new one) makes the conversion refuse, naming it, so home shows the last good file and nothing is overwritten. The phone writes no `dock`. `Layout`'s tests cover a version 1 file, a version 2 file, a start stack with no `apps` block, a stack of only `dock`, a full first page (the app goes to the next page), a full home (refused), and a dock app already on home.
- [ ] Nothing else knows a dock: `StackBlock.Dock`, `Layout.dock`, `MAX_DOCK`, `Spot.Dock`, `Place.Dock`, the menu's Add to and Remove from dock, the drag's dock targets, the minimap's Dock in its palette and drawing, and `LaunchFrom` `dock` are removed, with their tests rewritten or removed. A new page is a stack of only `apps`.
- [ ] On `T3_Phone_API35`, from a version 2 `layout.json` whose dock holds two apps, home shows them at the end of its apps and no dock (screenshot), and `layout.json` read after an edit holds no `dock`.
- [ ] A fresh install's default layout (`HomeStore`'s default) has no dock and no "Google" folder: today it puts every installed `com.google.*` app not on home into one folder, which is how Sumeet's Fold got 21 apps in it, 19 never opened. Those apps stay in the drawer only. A unit test of the default layout shows neither.
- [ ] `layout-v2.md`, the spec and DESIGN.md's mentions of the dock say there is none.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/` and its tests, `collect/Launches.kt` for `from`, the spec, `layout-v2.md`, DESIGN.md. The server keeps accepting `from: dock` in old launch rows.

## Context

Mission lor-5qkq. lor-slux (a palette drop of Dock) is superseded by this item.
