---
schema: atelier.mission
schema_version: 3
id: lor-5qkq
title: Home and drawer built around what Sumeet opens
status: ready
labels:
- launcher
lead: null
ready:
  by: conversion
  at: 2026-11-09T01:52:07.783492317Z
created_at: 2026-11-09T01:35:04.880290408Z
---

## Why

The launcher works, but it fits the Fold badly. On the Fold, 90 of 144 launches went through search, 39 through home and 9 through the dock (LoR's `launch` observations, 2026-11-05 to 11-07), because home holds apps Sumeet doesn't open: 19 of the 21 apps in its "Google" folder had no session in 26 days, while YouTube Music, his fifth most-used app, is buried in that folder. The drawer is one long list split by a package table that puts most of the phone's 295 launchable apps in Other or System; its bottom bar is a large search field that is usually empty; and the app menu is Material's square dropdown at the tile's left edge, with nothing showing which icon opened it. Two bugs make it worse: work-profile apps can't be put anywhere, and an app in a folder can't be added to home, with a message ("it's already on home") that doesn't say where it is.

## Outcome

- **The drawer opens on a short swipe:** about 110 dp, or a flick, instead of 30% of the screen.
- **No dock.** Home is one stack of apps and folders; the apps a layout's dock held move into its home `apps` block, so none is lost.
- **Home is built from use:** the apps Sumeet opens on the most days sit in the two rows nearest the thumb, the "Google" folder is gone, and small folders hold what he opens in bursts (Money, Travel, Access, Work).
- **Work-profile apps are apps like any other:** on home, in folders, hidden, in the drawer's tabs, with Android's badge.
- **Adding an app that sits in a folder moves it onto home**, with Undo.
- **The drawer is tabs.** A floating bar of tab icons and a separate search button sit over the list, with no solid bar. The first tab is **quick dial**, which changes through the day: a card for what's next from LoR, eight apps for this hour, the four people he has talked to most recently, and new installs. The last tab is All, A–Z with the letter rail. The tabs between hold lists of apps, each with a name and an icon, kept in `layout.json`'s `drawer.tabs`, so LoR and its agent can edit them like the rest of the layout.
- **Search hides behind a button.** A tap folds the tabs away and grows the field out of the button; results run upward from it as today.
- **The app menu is a popover anchored to its icon:** centred on the icon, with an arrow on the icon's centre, chamfered like DESIGN.md's panels, with a shadow.
- **An agent sorts apps into tabs:** it reads the phone's app list from LoR and writes each tab's apps, leaving those Sumeet moved by hand.

## Scope

In: the phone app (`android/`), `layout-v2.md`, DESIGN.md's menu rule, the spec's launcher lines (`docs/v2-product-engineering-spec.md`), and for the agent's sort, LoR's server, CLI and skill as far as the sort needs.

Out: the web editor's dock and sections (lor-ncpw's strategy drops them in this mission's first planning change); named and temporary layouts; swiping between tabs (a sideways swipe moves home's grid); quick dial on the wall display.

Environment: `T3_Phone_API35` and `T3_Fold_API35` against the test server (`http://10.0.2.2:3857`); Android builds on boat (`android/boat-build.sh`). The real Fold only in `needs-human` items after merge, when Sumeet says so.

## Authority

The orchestrator may change the increments, their order and their issues; choose names, icons, thresholds, the quick-dial score's weights and file formats within this Shape; and write the first tab sort and the usage-built home to the Fold once Sumeet says to install. These come back to Sumeet: keeping anything this Shape drops, a change to how home looks beyond the mocks, and installing on his phone.
