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

Home feels finished in the two places you touch most. A pressed icon answers the finger at once, and folders look and open the way Smart Launcher's do: a lighter closed tile, and an open folder that fills the screen with its name editable in place and a + to add apps. Smart Launcher blurs the wallpaper behind an open folder; LoR dims it instead, since `DESIGN.md` rules out blur. Both gaps come from the Smart Launcher comparison (`docs/research/launcher-vs-smart-launcher.md`).

## Done when

- [ ] A pressed state on every app and folder tile (home, dock, folders, drawer, search results): the tile scales down slightly while held and returns when released, one-shot, in `DESIGN.md`'s motion, nothing looping. It doesn't delay the launch, the long press, or the pickup of a drag, and the launch animation from the icon's bounds still starts from the right place. Recorded on `T3_Phone_API35`, and checked again with the animator duration scale at 0, where it shows no motion.
- [ ] The closed folder tile is lighter: its 2×2 preview of mini icons without the filled backing, or the nearest form `DESIGN.md` allows, with the choice and its reason written on this item. Labels and the preview still follow the wallpaper's ink, at 4.5:1 or more on the light and the dark wallpapers (measured from the pixels, as 6.3 did). The folder's `cover` app still replaces the preview.
- [ ] An open folder fills the screen as a dark surface (Deep Space under the veil, so its labels keep `SurfaceInk` as the folder panel's do), with the wallpaper dimmed behind it and no blur, still opening from its tile as Increment 7 built. Its name is edited in place, and the folder menu's "Rename…" now opens the folder with that field focused instead of a dialog, so every edit keeps a menu way. A + opens a picker of apps to add. Each edit writes `layout.json` with Undo; tests cover the edit functions. Screenshots on `T3_Phone_API35` and `T3_Fold_API35`, as the grid then draws a folder.
- [ ] Dragging out of a full-screen folder has a rule that works without space outside the card: the folder closes and the drag goes on over home once the finger has been 400 ms (Launcher3's `ON_EXIT_CLOSE_DELAY`) outside the folder's grid of apps, above its name row or below its last row (a folder too full to leave room below scrolls, keeping a strip free); the rule is written on this item. Checked on the emulator: an app dragged out that way lands on home, and the last one out takes the folder away. Tapping the dimmed space outside the grid closes the folder.
- [ ] Dragging into a folder, a folder's predictive back, and TalkBack's names on its tiles still work (emulator check).

## Unknowns

- Where the pressed scale is applied so it covers every tile without a second click handler: each tile keeps its one click node for TalkBack and agent-device (`combinedClickable` on home, dock and folder tiles; `clickable` on search rows).

## Background

- Increment 7 of Launcher polish built the folder opening from its tile (`folder/FolderAnimationManager.java`'s 200 ms, from Launcher3). Increment 4.1 added `cover`. Increment 6 made the folder panel a framed, chamfered surface.
- Smart Launcher, as seen on Sumeet's Pixel: the closed tile is a bare 2×2 of mini icons; the open folder is full-screen over a tinted, blurred wallpaper, with the name editable in place and a + to add apps.
- Sits after lor-eh0y because it touches the tiles and folder code that lor-qdmy moves onto the grid. It may land before or after lor-ph30; lor-ph30's fold checks keep whichever open folder exists then. lor-f3vn waits for it, so the layout lock covers its folder edits.
