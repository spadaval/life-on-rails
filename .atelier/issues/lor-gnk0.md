---
created_at: "2026-11-07T20:51:02.369425400+00:00"
id: "lor-gnk0"
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
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Home feels finished: a pressed state, and folders like Smart Launcher's"
updated_at: "2026-11-07T20:51:17.879884235+00:00"
---

## Goal

Home feels finished in the two places you touch most. A pressed icon answers the finger at once, and folders look and open the way Smart Launcher's do: a lighter closed tile, and an open folder that fills the screen with its name editable in place and a + to add apps. Both gaps come from the Smart Launcher comparison (`docs/research/launcher-vs-smart-launcher.md`).

## Done when

- [ ] A pressed state on every app and folder tile (home, dock, folders, drawer, search results): the tile scales down slightly while held and returns when released, one-shot, in `DESIGN.md`'s motion, nothing looping. It doesn't delay the launch, the long press, or the pickup of a drag, and the launch animation from the icon's bounds still starts from the right place. Recorded on `T3_Phone_API35`.
- [ ] The closed folder tile is lighter: its 2×2 preview of mini icons without the filled backing, or the nearest form `DESIGN.md` allows, with the choice and its reason written on this item. Labels and the preview still follow the wallpaper's ink, at 4.5:1 or more on the light and the dark wallpapers (measured from the pixels, as 6.3 did). The folder's `cover` app still replaces the preview.
- [ ] An open folder fills the screen over the dimmed (and, where the device supports it, blurred) wallpaper, still opening from its tile as Increment 7 built. Its name is edited in place (replacing the Rename dialog), and a + opens a picker of apps to add. Each edit writes `layout.json` with Undo; tests cover the edit functions. Screenshots on `T3_Phone_API35` and `T3_Fold_API35` (one cell at full width, as lor-eh0y leaves it).
- [ ] Drag into and out of a folder, a folder's predictive back, and TalkBack's names on its tiles still work (emulator check).

## Unknowns

- Blurring the wallpaper behind home's own window (`Window.setBackgroundBlurRadius`, API 31 and up, when cross-window blur is enabled) on the emulator and on One UI; a dim alone is acceptable where it isn't.
- Where the pressed scale is applied so it covers every tile without a second click handler (the tiles keep one `combinedClickable` node for TalkBack and agent-device).

## Background

- Increment 7 of Launcher polish built the folder opening from its tile (`folder/FolderAnimationManager.java`'s 200 ms, from Launcher3). Increment 4.1 added `cover`. Increment 6 made the folder panel a framed, chamfered surface.
- Smart Launcher, as seen on Sumeet's Pixel: the closed tile is a bare 2×2 of mini icons; the open folder is full-screen over a tinted, blurred wallpaper, with the name editable in place and a + to add apps.
- Sits after lor-eh0y because it touches the tiles and folder code that lor-qdmy moves onto the grid.
