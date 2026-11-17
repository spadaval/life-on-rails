---
schema: atelier.mission
schema_version: 3
id: lor-ncpw
title: Edit home from LoR's web, drawn as the phone draws it
status: ready
labels:
- launcher
lead: null
ready:
  by: conversion
  at: 2026-11-08T17:57:12.134104993Z
created_at: 2026-11-08T03:36:23.147268461Z
---

## Why

Home is now a grid of panels in `layout.json`, but the only way to arrange it is on the phone: a minimap on a 6-inch screen, or an agent writing JSON it cannot see. Sumeet wants to arrange home in the LoR dashboard on a big screen, see it exactly as the phone will draw it, at the phone's real size, and send it to the phone. Most of the way there exists: the `agent-layout` branch (T3 Launcher's Increment 7, reviewed four times, never merged) keeps every version of the layout in LoR and has the phone apply a new version with Undo, and most of home's drawing code is plain Compose with no Android in it.

## Outcome

- **Home's layout lives in LoR.** Every version, from the phone, an agent or the web, is one row in one history, and any version can be reverted. A new version reaches the phone the next time home comes to the front, and the phone says what changed, with Undo.
- **A page in the dashboard draws home as the phone does,** each cell of the grid, on the cover screen and the inner screen, with the phone's icons (its icon pack and themed icons), its wallpaper, its fonts (Saira, which ships in the app) and its widgets as they last looked.
- **At real size.** Once calibrated by holding the phone against the monitor, the drawn phone is as big as the real one.
- **Home is edited there:** panels, blocks, apps and folders, with the same edits and the same refusals as the phone's minimap and drags, saved as a new version.

Worth shipping on its own: the sync with a plain history page (the agent can edit home again, and a version can be reverted from the web), then the real-size view, before editing on the web.

## Success criteria

- round-trip: On `T3_Fold_API35` against the test server, one save on the web page holding two edits (an app moved into a folder, and a stack added in an empty cell and set Half) shows on the emulator's home after the next resume, with the version's note; Undo on the phone puts it back; and the web's history shows three versions: the phone's before, the web's, and the Undo. Shown by the mission's acceptance validation item, with screenshots.
- same-pixels: For one test layout with every panel type and block, and one device's uploads, the web's drawing of every cell, rendered at the phone's pixel size, and the emulator's screenshot of that cell put every tile, label and panel edge within 2 px of each other, on both of the Fold's screens. Shown by the acceptance validation item lor-nnn5, by someone who built none of it, with an overlay diff of each cell. Panels and blocks whose content is live are compared by their frame only: widgets, the `t3` panel, the `lor` panel and LoR's blocks, and the clock block.

## Scope

In: bringing `agent-layout`'s sync and `lor phone layout` up to schema version 2 and onto `main`; a history page in the dashboard (versions per device, view, revert, and editing a version as JSON); the shared Kotlin module and the Android app on it; the wasm build and how it reaches `pnpm build`, with the spec's Stack line updated for it; the phone's uploads listed under Shape and their storage in LoR; the real-size view with calibration, both Fold screens and folding; editing panels, blocks, apps and folders on the web (home has no dock after lor-5qkq).

Out:
- adding a widget from the web (binding a widget needs the phone's widget host and its permission dialog), though a placed widget can be moved or removed;
- Home settings' look, hidden apps and drawer tabs on the web (an agent or the phone sets them; later if missed);
- named and temporary layouts, and agent-sorted folders (T3 Launcher's Increments 9 and 10, still later);
- a live stream of the phone's screen.

Environment: the dashboard in desktop Chrome; `T3_Phone_API35` and `T3_Fold_API35` against the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`); Android Gradle builds on boat; the browser bundle in GitHub Actions (decision lor-kbog). The real phone and Fold only when Sumeet says so, in `needs-human` validation items after merge, outside the mission, each with an emulator stand-in run before merge.

## Authority

The orchestrator may change the increments, their order and their issues, and choose within this Shape: module and package names, the page's route, the upload formats and tables, library versions, and which files move into the shared module. It records each choice on its item.

These come back to Sumeet:
- giving up the shared Kotlin drawing for another approach, if the spike or the split finds it unworkable;
- installing anything on his phone, and any permission he must grant;
- a change that makes home on the phone slower to open or visibly different;
- a change to `layout.json` that changes a version 2 field's meaning;
- adding to or dropping from the Outcome.
