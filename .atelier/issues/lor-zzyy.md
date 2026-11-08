---
created_at: "2026-11-07T23:36:19.018382223+00:00"
id: "lor-zzyy"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Lock home: no edits on home until it is unlocked"
updated_at: "2026-11-08T01:16:43.010337226+00:00"
---

## Goal

Locking home from the home menu stops every edit on home, as Lawnchair's "Lock home screen" and Smart Launcher's padlock do. A long press still opens an app's menu, without its editing entries. A drag picks nothing up, and the Undo bar says why. Folders still open, but can't be renamed or added to. Unlocking restores editing, from the home menu, or from the minimap's padlock (lor-6get), which Home settings' "Panels" opens even on a grid with no stack and so no home menu. The refusal itself is lor-v692's, in `LayoutStore.edit` (decision lor-73li); this item hides or stills what that rule would refuse, so a locked home rarely shows a refusal.

## Done when

On `T3_Phone_API35` against the test server, unless a line says otherwise:
- [ ] The home menu has "Lock home", which writes `"locked": true` with Undo ("Home is locked."). While home is locked, "Unlock home" takes its place and writes the field away ("Home is unlocked."). Undo after either puts the file back. "Edit home" is hidden while locked, and locking leaves edit mode. The file is read after each step, and both menus are screenshotted.
- [ ] Locked, an app's menu (on home, in the dock, in a folder, in the drawer, in search) shows its shortcuts, App info, Uninstall and Move to section…, and none of Add to home, Remove from home, Add to dock, Remove from dock and Remove from folder. Hide shows only for an app that is on none of home, a folder and the dock. Unlocked, the menu is as today. Shown by a uiautomator dump of the entries for an app on home and an app only in the drawer, locked and unlocked.
- [ ] Locked, a long press then a move on an app on home, in the dock, in an open folder and in the drawer, and on a folder tile, picks nothing up: no Remove bar shows and nothing moves. The Undo bar says "Home is locked. Unlock it from the home menu." once per gesture, and the file's modified time is unchanged (`stat` before and after). Driven with `input motionevent`, with a screenshot of each.
- [ ] Locked, a long press on a folder tile opens no folder menu and the bar says the same. An open folder shows its name as text, not a field, and no +. Unlocked again, the folder menu's Rename… and the + work (the file read after each).
- [ ] Unlocked again, a drag from the drawer onto home adds the app, and Remove from home in a menu takes it off (the file read after each).
- [ ] The store's refusal reaches the screen: with home open and unlocked, `"locked": true` written into the file through `adb shell run-as`, then a drop on home is refused with "Can't: home is locked; unlock it from the home menu.", and the file's bytes are unchanged (`sha256sum`).
- [ ] What the lock leaves alone still writes while locked: the drawer's view switch, a Home settings look change, a section's Move up, and Unhide (the file read after each).
- [ ] TalkBack reads "Lock home" and "Unlock home" by those words (uiautomator dump).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`AppMenu.kt` (`HomeMenu`, `AppMenu`), `HomeScreen.kt` (the folder menu, the open folder's name field and +, the drag sources' pickup, `TopBar`), `Drag.kt`, and the drawer's drag pickup in `DrawerScreen.kt`. Not the minimap's padlock (lor-6get), not `LayoutStore`'s rule (lor-v692).

## Context

- Read 2026-11-07 in Lawnchair (Apache-2.0, at aeb92c1), whose lock is the nearest built one: `LauncherOptionsPopup.kt` toggles `lockHomeScreen` from the home menu and drops edit mode and widgets from it while locked; `Workspace.beginDragShared` starts no drag while locked, though the long press still gives its haptic; `FolderNameEditText` takes no touch; `LawnchairShortcut.kt` drops Customize and Uninstall. Here Uninstall stays, since it isn't a change to the layout. Name the source and licence in the files that follow it.
- Home reads `locked` from the parsed file it already holds (`HomeContext.file`), so nothing new is stored.
- lor-gnk0 built the open folder's in-place name and its +, and lor-73li says which edits the lock covers.
- If lor-r0f9 (widgets on home) is on `main` when this starts, its widget menu's Resize and Remove, and the home menu's "Add widget", are hidden while locked here, with a screenshot of each locked menu. Otherwise the note on lor-r0f9 has it do so.

## Notes

- 2026-11-07 [note] From lor-v692's review (finding lor-69ai): a lock written to the file while home is open makes a drop answer 'Home changed; try again' (dropHeld compares the file with h.original before the lock check), and home then rereads the file. So the Done when line that shows the lock's words must use an app-menu edit such as Remove from home, or lock through the home menu first; a drag after an outside lock shows HOME_CHANGED, which is correct.
- 2026-11-07 [decision] A drop or a folder's + over a file whose only change since home read it is the lock carries the lock into its result (landOver in Drag.kt), so LayoutStore.edit refuses it with LOCKED instead of HOME_CHANGED; any other change is still HOME_CHANGED.
  Reason: Meets the Done when line on the store's refusal as written (a drop, not an app-menu edit, per the lor-69ai note), and the words a locked home gives are the ones that say what to do. The store's one rule stays the only refusal.
  Rejected: Showing the refusal through an app-menu edit as the note suggested: the line asks for a drop, and a drop would keep answering 'Home changed; try again' for a lock set from outside
- 2026-11-07 [decision] Locked, the one pickup point (HomeScreen's dragging handler, which serves home, the dock, the open folder and the drawer) picks nothing up, says LOCKED_SAY in the Undo bar once, ticks Reject, and swallows the rest of the press; the app's menu stays open. A folder tile's long press says it instead of opening its menu, and the handler then stays quiet, so a gesture says it once. DrawerScreen.kt is unchanged.
  Reason: Lawnchair's beginDragShared returns before a drag and leaves the popup; one check where every drag starts can't miss a source, and swallowing the press keeps the grid from moving under it.
  Rejected: A check in each dragSource (dock, tiles, folder, drawer): four places to keep in step
- 2026-11-07 [decision] The home menu shows 'Lock home' last, and locked, 'Unlock home' in its place with 'Edit home' hidden. Edit mode is left by HomeScreen whenever the file it draws is locked, not by the menu entry, so a lock from outside leaves it too. lor-r0f9's 'Add widget' goes behind the same !locked test (comment in HomeMenu).
  Reason: Lawnchair drops edit mode and Widgets from its options popup while locked. One owner for 'no edit mode while locked' covers both ways a lock arrives.
