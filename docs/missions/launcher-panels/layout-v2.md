# `layout.json`, schema version 2

Home's layout file. Version 2 makes each layout a sparse grid of panels, so home can be rearranged by editing the file (the Panels mission's [strategy](strategy.md), Shape). It carries over version 1 (T3 Launcher's [plan](../t3-launcher/plan.md), Increment 3.2) and Launcher polish's [Schema additions](../launcher-polish/plan.md#schema-additions), changed where the grid needs it. This file is the source of truth; the parser is `android/app/src/main/java/com/spadaval/lor/home/Layout.kt`. Each issue that adds a field adds it here, as a version 2 addition.

## The file

`layout.json` in the app's files directory (`/data/data/com.spadaval.lor/files/layout.json`; on a debug build `adb shell run-as com.spadaval.lor` reads and writes it). Home re-reads it when it comes back on screen and the file's modified time changed.

```json
{
  "version": 2,
  "active": "default",
  "hidden": ["com.example.game"],
  "drawer": { "view": "sections", "sections": [{ "name": "Work", "category": "work", "apps": ["com.slack"] }] },
  "look": { "header": "clock" },
  "layouts": [
    { "name": "default",
      "expires": "2026-11-18T00:00:00Z",
      "start": { "narrow": [1, 0] },
      "dock": ["com.google.android.dialer", "com.google.android.apps.messaging"],
      "panels": [
        { "at": [0, 0], "type": "t3" },
        { "at": [1, 0], "type": "stack", "blocks": [
            { "type": "clock" },
            { "type": "apps", "items": [
                "com.whatsapp",
                "com.android.chrome/com.google.android.apps.chrome.Main",
                { "folder": "Dev", "apps": ["com.github.android", "org.example/.Term"],
                  "reason": "You open these together.", "pinned": true, "cover": "com.github.android" }] },
            { "type": "dock" }] },
        { "at": [2, 0], "type": "stack", "blocks": [
            { "type": "apps", "items": ["org.example"] },
            { "type": "dock" }] },
        { "at": [1, 1], "type": "drawer" }] }
  ]
}
```

## Rules

- `version` is `2`. A file with `version` `1` is read by converting it (below); any other value is an error.
- Unknown fields are errors, so a typo doesn't silently vanish. A later build may add new optional fields, panel types and block types without bumping `version`; the version changes only when an existing field changes meaning. Every error names its place (`layouts[0].panels[2].blocks[1].items[3]: "WhatsApp" is not a package name or package/activity`), and home shows it in a banner over the last good layout. An app that isn't installed is left out without an error.
- `layouts` holds at least one layout, names unique and non-empty. `active` names the one home shows.

### Top-level fields

These keep version 1's meaning, as Launcher polish's Schema additions describe them: `hidden` (1.1), `drawer` (2.1, and `keyboard` from 5.5) and `look` (Increment 6, with `header` from 6.7). `layout.good.json` and `layout.json.broken` are not part of the schema (Launcher polish 3.1).

`locked` (optional; a version 2 addition, lor-v692, decision lor-73li): the layout lock, `true` or `false`. Any other value is an error (`locked: must be true or false`); absent means false, and the phone writes it only when true. While it is `true`, the phone refuses any edit of its own that changes `active` or `layouts` ("home is locked; unlock it from the home menu") and writes nothing; edits to `hidden`, `drawer` and `look` alone are taken, and so is unlocking. Hide of an app on home or in the dock changes `layouts`, so it is refused. It covers every layout. Undo isn't checked, so Undo after locking unlocks. An agent's or a hand edit of the file isn't stopped.

The top-level `drawer` is the drawer's settings: its view and its sections. The `drawer` panel type is where the drawer sits in a layout's grid. They are different things; the panel draws the drawer that the settings describe.

### A layout

- `name` (required): non-empty, unique among the layouts.
- `expires` (optional): an ISO-8601 date-time with an offset. It's read and kept, but nothing acts on it yet.
- `start` (required): `{ "narrow": [column, row], "wide": [column, row] }`, the cell home starts on for each screen class: `narrow` for screens under 600 dp wide (the phone, the Fold's cover screen), `wide` from 600 dp (the Fold's inner screen). `wide` is optional and is `narrow` when absent. Each must name a cell that holds a panel.
- `dock` (optional): at most 5 apps. A layout has one dock; every `dock` block draws it, so an edit to the dock shows in all of them.
- `panels` (required): the grid's panels, a list in any order.

### Panels

A panel is `{ "at": [column, row], "type": … }`.

- `at`: two whole numbers from 0. The grid is sparse: a cell holds at most one panel, and cells without one are empty. Moving between cells, the inner screen's pairing and the vertical wrap are behaviour, not fields (the strategy's Shape and decision lor-y7qi).
- `type` is one of:
  - `stack`: a column of blocks, `"blocks": [block, …]`, at least one, each block type at most once in a stack.
  - `t3`: the agent page. No other field but `wide`.
  - `drawer`: the app drawer as a panel. No other field but `wide`.
- `wide` (optional, any type; a version 2 addition, lor-j87u, decision lor-b1dx): how the panel sits on the inner screen, `"full"` or `"half"`. Any other value, or a value that isn't a string, is an error naming its place (`layouts[0].panels[2].wide: "Full" is not "full" or "half"`). Absent, it is the type's default: `stack` half, `t3` full, `drawer` half (and `lor` full, when lor-clgw adds that type). A row's inner screens are built left to right by column: a `full` panel is one inner screen; a `half` shares one with the next panel in the row, past any gap, when that panel is also a `half` not already paired; otherwise it is alone and drawn in its full layout. So no grid is invalid. The rule is `InnerScreens` in `Inner.kt`, which home draws from lor-el51 on. A converted version 1 file has no `wide`, so its inner screens are the agent page; home and page 2; pages 3 and 4; and so on, with the drawer alone on row 1.
- New panel types (`lor`) are added by the issues that use them.
- A row holds at most 5 stacks with an `apps` block (home and 4 more, as version 1's pages).

### Blocks

A block is `{ "type": … }`.

- `clock`: today's header, drawn in the style `look.header` names (date, clock or none). No other field. lor-sbth adds the next alarm and weather to it.
- `apps`: `"items": [item, …]` (optional; absent or empty means none), at most 24, in reading order. The block fills its grid row by row with the columns `look.grid` sets. An item is written as version 1's `home` items are:
  - An app is a string: a package name (that package's launchable activity in the main profile that comes first by label, in `Collator` order) or `package/activity` (`package/.Short` expands as `ComponentName.unflattenFromString` does).
  - A folder has `folder` (its name) and `apps` (one or more apps; folders don't nest). Optional: `reason`, one line shown when it opens; `pinned` (default false), meaning Sumeet arranged it and an agent leaves it alone; and `cover`, one of the folder's `apps`, written as it is there, drawn as its tile (while that app isn't installed, the four-app preview shows; an edit that takes the app out of the folder drops `cover`).
- `dock`: the layout's `dock`. No other field.
- New block types (`widget`, `today`, `needs-you`, `running`, `t3-queue`) are added by the issues that use them.

### What the phone's edits mean

Launcher polish's rules carry over, with "home" meaning every `apps` block of the active layout's grid:

- `hidden` and Hide apply to every `apps` block, the dock and folders, in every layout.
- An app is on home once (any `apps` block, folders included) and in the dock once. This is a rule of the edits; the parser doesn't refuse duplicates.
- A folder an edit touches becomes `pinned`; a folder an edit empties is removed.
- Edits by place address a panel's cell, the block, and the index in it, so removing one panel moves no other item's place.
- Add to home appends to the first stack with room in the start row, by column; when all are full, it makes a new stack.
- A drop past the last page makes a new stack of `apps` and `dock` after the last panel in the start row (Launcher polish 4.5).
- An extra page goes when an edit empties or made it, not when it was already empty: after Remove from home, Hide, the open folder's + and a drop, a stack of only `apps` and `dock` whose `apps` block is empty is removed, unless the layout before the edit held such an empty stack at that cell, or it is at a `start` cell. So a page an edit or a drag empties goes, a page a drag made and then left goes (the drop compares with the file the drag began on), and an empty stack the minimap made stays. Home's stack stays, even empty. The parser accepts an empty one.
- The minimap's edits (lor-f3vn; `GridEdit.kt`):
  - A new or moved panel goes in a cell in reach: columns 0 to one past the last panel's, and rows 0 to one past the last panel's, of the active layout. A hand-written far cell still draws; the phone never writes one.
  - A layout holds one `t3` panel and one `drawer` panel.
  - A new stack is an empty `apps` block and `dock`, as a new page is. A block added to a stack is a type that takes no field (`clock`, an empty `apps`, `dock`), and a stack holds each type once.
  - A panel moved onto another swaps with it. A `start` that named a moved panel's cell names its new cell. Setting the wide start on the narrow start's cell clears `start.wide`, and so does setting the narrow start on the wide start's cell.
  - A panel a `start` names can't be removed ("home starts there; choose another start first"), and a stack's last block can't be moved out or removed ("a stack needs a block; remove the panel instead"). Removing a stack or an `apps` block takes its apps off home.
  - The row limit holds in the rows an added or moved stack or `apps` block lands in.
  - An edit of a panel or a block names its cell (and the block's index) and what the minimap drew there, and is refused ("Home changed; try again") when the file holds something else there.
- The phone writes only fields that differ from their defaults (`start.wide` only when set, a panel's `wide` only when it differs from its type's default, `look` and `drawer` only when not default), and writes `start`, `dock` and `panels` in every layout. So a `wide` the file states that equals the type's default is dropped at the next write; any other is kept.

Home draws the grid (lor-qdmy): one cell on screen, the narrow `start` after a home press. A swipe moves to the nearest cell that way in the row (the ends stop), or up and down by decision lor-y7qi's rule (the next row that way with a panel, wrapping; back to the cell a move came from, else the same column or the nearest, the left one on a tie). Each stack draws its blocks top to bottom: `clock` as home's header, `apps` taking the space between, `dock`; a stack without a `clock` starts where home's grid would.

## Reading a version 1 file

A version 1 file is parsed with version 1's rules (its errors name version 1's places, as `layouts[0].home[3]`) and each layout is converted to the default grid:

- the `t3` panel at `[0, 0]`, left of home;
- home at `[1, 0]`: a stack of `clock`, `apps` (version 1's `home`) and `dock`;
- each of version 1's `pages` at `[2, 0]`, `[3, 0]` and on: a stack of `apps` (the page's `items`) and `dock`;
- one `drawer` panel at `[1, 1]`, in home's column on row 1 (decision lor-y7qi: every page reaches it by the grid's movement rule);
- `start` is `{ "narrow": [1, 0] }`;
- `name`, `expires` and `dock` as they were, and the top-level fields unchanged; no panel gets a `wide`.

Reading converts in memory only: the file stays version 1 until the phone next writes it, and then it is version 2. `layout.good.json` and the broken-file restore take a version 1 file the same way, and Undo of the first version 2 write puts the version 1 text back, byte for byte. The first run's default layout is this grid, with home's apps and no extra pages.
