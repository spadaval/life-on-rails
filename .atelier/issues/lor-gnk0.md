---
created_at: "2026-11-07T20:51:02.369425400+00:00"
id: "lor-gnk0"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-gnk0
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
status: "in_progress"
title: "Home feels finished: a pressed state, and folders like Smart Launcher's"
updated_at: "2026-11-07T23:00:02.922176446+00:00"
---

## Goal

Home feels finished in the two places you touch most. A pressed icon answers the finger at once, and folders look and open the way Smart Launcher's do: a lighter closed tile, and an open folder that fills the screen with its name editable in place and a + to add apps. As Smart Launcher does, the wallpaper behind an open folder is blurred where the device allows it (`DESIGN.md`'s Elevation & Depth: blur as a backdrop). Both gaps come from the Smart Launcher comparison (`docs/research/launcher-vs-smart-launcher.md`).

## Done when

- [x] A pressed state on every app and folder tile (home, dock, folders, drawer, search results): the tile scales down slightly while held and returns when released, one-shot, in `DESIGN.md`'s motion, nothing looping. It doesn't delay the launch, the long press, or the pickup of a drag, and the launch animation from the icon's bounds still starts from the right place. Recorded on `T3_Phone_API35`, and checked again with the animator duration scale at 0, where it shows no motion.
- [x] The closed folder tile is lighter: its 2×2 preview of mini icons without the filled backing, or the nearest form `DESIGN.md` allows, with the choice and its reason written on this item. Labels and the preview still follow the wallpaper's ink, at 4.5:1 or more on the light and the dark wallpapers (measured from the pixels, as 6.3 did). The folder's `cover` app still replaces the preview.
- [x] An open folder fills the screen as a dark glass surface: Deep Space, translucent enough that the wallpaper shows through blurred where the device supports cross-window blur, and dark enough that its labels keep `SurfaceInk` (as the folder panel's do) at 4.5:1 or more over the light wallpaper, measured from the pixels without blur (the worst case) and again with blur where the emulator shows it. The screenshots say which path they show, blurred or dimmed only. Where blur isn't available the same face over the dimmed wallpaper must look right on its own. It still opens from its tile as Increment 7 built. Its name is edited in place, and the folder menu's "Rename…" now opens the folder with that field focused instead of a dialog, so every edit keeps a menu way. A + opens a picker of apps to add. Each edit writes `layout.json` with Undo; tests cover the edit functions. Screenshots on `T3_Phone_API35` and `T3_Fold_API35`, as the grid then draws a folder.
- [x] Dragging out of a full-screen folder has a rule that works without space outside the card: the folder closes and the drag goes on over home once the finger has been 400 ms (Launcher3's `ON_EXIT_CLOSE_DELAY`) outside the folder's grid of apps, above its name row or below its last row (a folder too full to leave room below scrolls, keeping a strip free); the rule is written on this item. Checked on the emulator: an app dragged out that way lands on home, and the last one out takes the folder away. Tapping the dimmed space outside the grid closes the folder.
- [x] Dragging into a folder, a folder's predictive back, and TalkBack's names on its tiles still work (emulator check).

## Context

- lor-sqfe switches home's `windowSoftInputMode` from `adjustPan` to `adjustResize`, after which the keyboard only reports its height. The open folder's in-place name field must stay above the keyboard under either mode. If this item lands before lor-sqfe, `adjustPan` still applies then, and lor-sqfe's change must keep the field clear.

## Unknowns

- Blurring the wallpaper behind home's own window (`Window.setBackgroundBlurRadius`, API 31 and up, when cross-window blur is enabled): whether the emulator shows it, and what One UI does (the real Fold's answer goes in lor-f6zd, after merge).
- Where the pressed scale is applied so it covers every tile without a second click handler: each tile keeps its one click node for TalkBack and agent-device (`combinedClickable` on home, dock and folder tiles; `clickable` on search rows).

## Background

- Increment 7 of Launcher polish built the folder opening from its tile (`folder/FolderAnimationManager.java`'s 200 ms, from Launcher3). Increment 4.1 added `cover`. Increment 6 made the folder panel a framed, chamfered surface.
- Smart Launcher, as seen on Sumeet's Pixel: the closed tile is a bare 2×2 of mini icons; the open folder is full-screen over a tinted, blurred wallpaper, with the name editable in place and a + to add apps.
- Sits after lor-eh0y because it touches the tiles and folder code that lor-qdmy moves onto the grid. It may land before or after lor-ph30; lor-ph30's fold checks keep whichever open folder exists then. lor-f3vn waits for it, so the layout lock covers its folder edits.

## Notes

- 2026-11-07 [decision] The pressed state is the click node's own Indication (ui/Press.kt, PressScale): what the node draws scales to 0.9 about its centre over Motion.QUICK (150 ms) while pressed, back on release or cancel. Tiles (home, dock, folder, drawer, search's apps, the folder tile) pass PressScale.Tile to their existing combinedClickable; search's rows pass PressScale.Row (0.97) to their existing clickable.
  Reason: An Indication rides the one click node, so no second handler, no extra semantics node, and the tap, long press and drag pickup see the same events. It changes drawing only, so layout bounds (the launch's source rect and drag hit-testing) don't move. Idea from Launcher3's FastBitmapDrawable (Apache-2.0, via Lawnchair): the icon animates its scale on the pressed state; Launcher3 scales up, DESIGN.md's quiet feedback shrinks.
  Rejected: A graphicsLayer scale driven by collectIsPressedAsState in each tile (state in every tile, and a scaled layer moves what hit-testing sees mid-press)
- 2026-11-07 [decision] The closed folder tile is a bare 2x2 of mini icons, (icon - 4 dp) / 2 each with a 4 dp gap, in the icon's square, with no backing or edge; the cover app still replaces it. Only the label is drawn in the wallpaper's ink. DESIGN.md's Over the wallpaper list drops 'the folder tile's preview fill'.
  Reason: It is Smart Launcher's form and DESIGN.md allows it (nothing in it asks for a frame on a tile). The old 9% ink fill and Bulkhead Edge were what made the tile heavy. Labels keep their 4.5:1 (measured 19.4:1 on white, 15.3:1 on #101010).
  Rejected: Keeping a hairline Edge square without the fill (still reads as a box over a photo)
- 2026-11-07 [decision] The open folder's glass is Deep Space at 72% (Palette.FOLDER_GLASS, DESIGN.md's backdrop value) over the wallpaper blurred by 24 dp; where cross-window blur is off it lies over black at 50% (FOLDER_DIM) instead. Home's own cells fade out as the folder opens (alpha 1 - shown), so only the wallpaper is behind the glass.
  Reason: Starlight on 72% Deep Space over white reads 6.4:1 computed and 6.5:1 measured, above 4.5:1 unblurred. Without hiding home, its icons showed through the glass unblurred (window blur only reaches what is behind the window), which read as clutter, not glass; Smart Launcher hides home too. The agent-sorted reason line under the name is drawn in Starlight 13 sp, since Dust needs 82% to reach 4.5:1 over white.
  Rejected: A face of 82% so Dust asides read (the wallpaper would barely show)
- 2026-11-07 [decision] Blur uses FLAG_BLUR_BEHIND with blurBehindRadius on home's window, the radius following the folder's motion (0 to 24 dp), cleared when it closes; rememberCrossWindowBlur follows WindowManager's cross-window blur listener.
  Reason: Window.setBackgroundBlurRadius had no effect on the emulator: Android applies background blur only to translucent windows, and home's window is opaque (windowIsTranslucent false, it shows the wallpaper). Blur-behind is applied by the window manager to what lies behind the window, the wallpaper, and SurfaceFlinger showed backgroundBlurRadius=63 on the phone emulator. Making home's window translucent would change how every app opens over it.
  Rejected: Window.setBackgroundBlurRadius (no blur on an opaque window)|windowIsTranslucent=true on home
- 2026-11-07 [decision] The drag-out rule (FolderExit in Drag.kt): the folder's area is its name row and its grid of apps as laid out (the visible part when it scrolls). Once the held finger has been outside that area, above the name row, below the last row or off its sides, for 400 ms together (Launcher3's ON_EXIT_CLOSE_DELAY), the folder closes and the drag goes on over home. The area is centred in the screen above a 96 dp strip that is always kept free; a folder too tall for the rest scrolls inside it.
  Reason: A full-screen folder has no space outside a card, so the rule needs an edge inside the screen; the name row and the apps are what a finger can be over on purpose, and the strip guarantees room below even for a full folder.
  Rejected: Closing only on the screen's edges (too far for a thumb)
- 2026-11-07 [decision] Rename in place: the folder's name is a BasicTextField centred in the name row; Done, a tap away, the + or the folder closing writes it with Undo (blank or unchanged writes nothing). The folder menu's Rename… now opens the folder from its tile with the field focused and the name selected (HomeUi.folderRename). The open folder is matched by its place and by its name or its apps (OpenFolder.isOf), so a rename, an Undo of one, an added app and a drag out keep it open.
  Reason: One menu way per edit, as Done when asks, without a dialog. Matching by name alone closed the folder on its own rename.
  Rejected: Keeping the dialog beside the in-place field
- 2026-11-07 [decision] The + opens a picker (AppPicker) of every visible main-profile app not in the folder, by name, multi-select, added as one edit with Undo (addToFolder). An app already elsewhere on home moves into the folder and its row says where it is now ('On home', 'In Travel'); the dock keeps its own. The open folder follows its place, which moves up when an app ahead of it left (home draws the result at once through drag.landing, as a drop does).
  Reason: An app is on home once (the drop rule); hiding apps already on home would leave most of them unpickable. One edit for a batch gives one Undo.
  Rejected: Adding one app per tap and closing
- 2026-11-07 [decision] The open folder is 4 columns on a narrow screen and 6 where the panel is 600 dp or wider, at most 100 dp a column, centred.
  Reason: A full-screen folder on the Fold's inner screen at 4 columns spread tiles 200 dp apart.
- 2026-11-07 [handoff] Implemented on lor-gnk0 (db26b1d code, 9245942 records); every Done when line ticked, see ## Report. Boat assembleDebug testDebugUnitTest on db26b1d: 236 tests, 0 failures. Review not yet requested. The branch merges cleanly with main (13 commits ahead, lor-j87u's).

## Report

Every Done when line holds on `T3_Phone_API35`, with the Fold screenshots on `T3_Fold_API35`; receipts lor-deqx to lor-uog6, files in `/tmp/claude-1000/gnk0-emu/`. JVM tests: `HomeEditTest.theOpenFoldersPlusAddsAppsAndMovesThemFromElsewhereOnHome` and `renamingInPlaceWritesTheTrimmedName`, `DragTest.aHeldAppLeavesAFullScreenFolderAfter400msOutsideItsApps`, `LookTest.anOpenFoldersLabelsReadOnItsGlassOverWhite`; the boat build of db26b1d ran `assembleDebug testDebugUnitTest`, 236 tests, 0 failures.

- **Pressed state** (lor-deqx): a tile's icon goes from 137 to 122 px over about 160 ms while held and back on release, measured frame by frame from recordings; at the animator duration scale 0 it jumps in one frame each way. The same 0.9 on the dock, the drawer and search's apps. A tap still launches from the icon, and the long press and drags still work.
- **Closed folder** (lor-er0u): a bare 2×2 of mini icons. Its label reads 19.4:1 on a white wallpaper and 15.3:1 on `#101010`.
- **Open folder** (lor-txuu, lor-x711): full-screen Deep Space glass at 72% over the wallpaper, blurred by 24 dp (SurfaceFlinger showed the radius), or over a 50% black dim with blur off. Labels read 6.5:1 over white with or without blur, and 11.2:1 dimmed. It opens from its tile and closes into it. The name is edited in place, and the menu's Rename… opens it focused. The + adds apps. Each edit was read from `layout.json`, and Undo works with the folder still open.
- **Drags** (lor-lm2s): an app held 400 ms above the name row or below the last row leaves, and lands on home. The last one out takes the folder away. Dragging into a folder, predictive back, a tap on the glass and TalkBack's names all work.

### Deviations

- Home's own cells fade out behind an open folder. Window blur only reaches what is behind the window (the wallpaper), so home's icons showed through the glass sharp.
- The blur is `FLAG_BLUR_BEHIND`, not `Window.setBackgroundBlurRadius` as Unknowns guessed. The latter does nothing on home's opaque window.
- The folder's agent-sorted reason line is Starlight at 13 sp, not Dust: Dust would need the glass at 82% to keep 4.5:1 over white.

### Decisions

Eight decision notes below: the pressed state as an Indication, the bare tile, the glass and the fade of home, blur-behind, the drag-out rule, rename in place, the + picker, and the columns.

### Open items

- **Undo of an add that moved an app from ahead of the folder** puts the folder back one place later, which closes it, since it is matched by place. Undo of an add with nothing moved keeps it open (checked).
- **A one-app folder** shows one small icon in the tile's top-left quarter. That is easy to miss; Launcher3 draws the same.
- **The name field** shows as two TalkBack nodes: the text field ("Tools") and a "Folder name" group beside it.
- **The soft keyboard over the field** was seen only on the Fold (Gboard, `adjustPan`). On the phone emulator the default IME is agent-device's invisible helper. lor-sqfe's `adjustResize` still has to keep the field clear.
- **The real Fold's blur** belongs to lor-f6zd. **Home-press latency** wasn't re-measured; the change adds a layer over home's cells and one effect.
- **Outside the worktree:** `/tmp/claude-1000/gnk0-emu/` (screenshots, recordings, scripts, wallpapers) and `/tmp/claude-1000/gnk0-build/`. The phone emulator's `layout.json`, animator scale, IME and wallpaper were put back. The wallpaper was re-set from its original file, so its crop may differ slightly. The phone emulator is running and the emulator lock is released.
