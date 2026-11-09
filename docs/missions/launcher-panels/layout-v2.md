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
  - `stack`: a column of blocks, `"blocks": [block, …]`, at least one, each block type at most once in a stack except `widget`, which may repeat.
  - `t3`: the agent page. No other field but `wide`.
  - `drawer`: the app drawer as a panel. No other field but `wide`.
  - `lor` (a version 2 addition, lor-lz97, decision lor-p86v): LoR's dashboard shaped for the phone. No other field but `wide`. Narrow, one column: Today, LoR's Needs you (the flagged tasks, as the web's Today lists them) with the agent's queue under it, and Due soon (the first six open tasks, then "All N tasks"). Wide (600 dp and up), that column without Due soon on the left, and on the right Tasks or People, switched by two tabs at the column's top. The tab is screen state, kept in memory for the process and never written to the file; it starts on Tasks.
- `wide` (optional, any type; a version 2 addition, lor-j87u, decision lor-b1dx): how the panel sits on the inner screen, `"full"` or `"half"`. Any other value, or a value that isn't a string, is an error naming its place (`layouts[0].panels[2].wide: "Full" is not "full" or "half"`). Absent, it is the type's default: `stack` half, `t3` full, `drawer` half, `lor` full. A row's inner screens are built left to right by column: a `full` panel is one inner screen; a `half` shares one with the next panel in the row, past any gap, when that panel is also a `half` not already paired; otherwise it is alone and drawn in its full layout. So no grid is invalid. The rule is `InnerScreens` in `Inner.kt`, which home draws from lor-el51 on. A converted version 1 file has no `wide`, so its inner screens are the agent page; home and page 2; pages 3 and 4; and so on, with the drawer alone on row 1.
- New panel types are added by the issues that use them.
- A row holds at most 5 stacks with an `apps` block (home and 4 more, as version 1's pages).

### Blocks

A block is `{ "type": … }`.

- `clock`: today's header: the time and date in the style `look.header` names (date, clock or none), the next event, the next alarm, and the weather (lor-sbth; lor-leh4 draws the last three). No other field.
- `apps`: `"items": [item, …]` (optional; absent or empty means none), at most 24, in reading order. The block fills its grid row by row with the columns `look.grid` sets. An item is written as version 1's `home` items are:
  - An app is a string: a package name (that package's launchable activity in the main profile that comes first by label, in `Collator` order) or `package/activity` (`package/.Short` expands as `ComponentName.unflattenFromString` does).
  - A folder has `folder` (its name) and `apps` (one or more apps; folders don't nest). Optional: `reason`, one line shown when it opens; `pinned` (default false), meaning Sumeet arranged it and an agent leaves it alone; and `cover`, one of the folder's `apps`, written as it is there, drawn as its tile (while that app isn't installed, the four-app preview shows; an edit that takes the app out of the folder drops `cover`).
- `dock`: the layout's `dock`. No other field.
- `widget` (a version 2 addition, lor-sbth, decision lor-9wlx): an Android widget, `{ "type": "widget", "id": 42, "provider": "com.google.android.deskclock/com.android.alarmclock.DigitalAppWidgetProvider", "height": 160 }`. All three fields are required, and no other field is allowed.
  - `id`: the appWidgetId the phone's widget host bound, a whole number from 1. An `id` is used once in a layout; the error names both places (`layouts[0].panels[2].blocks[0].id: 42 is taken by panels[1].blocks[1]`).
  - `provider`: the widget provider's component, `package/class` (`package/.Short` expands as `ComponentName.unflattenFromString` does; the phone writes the class in full). A package alone is an error (`layouts[0].panels[1].blocks[1].provider: "com.tesla" is not package/class`).
  - `height`: the block's height in dp, a whole number from 40 to 2000.
  - Size: the width is the column the block is drawn in: the panel's width on the narrow screen and as a half, its flow column in the wide flow (lor-j87u). When the provider's `maxResizeWidth` is set and narrower, the widget is that wide, centred. The phone tells the widget its size whenever either changes, so a fold re-lays it. A new widget's height is the provider's `minHeight` in dp. Resizing changes only the height, between `minResizeHeight` (when set and below `minHeight`, else `minHeight`) and the smaller of `maxResizeHeight` (when set) and the panel's height. There are no cell spans.
  - Gone: when no provider is bound to the `id`, or one other than `provider`, home draws a note in its place, at its height, with Remove. The file is not changed until Sumeet removes it.
  - Ids no layout names are deleted from the widget host once per process, when `layout.json` first parses: every id the host holds that no layout of `layout.json` or `layout.good.json` names. A file that doesn't parse sweeps nothing. Remove doesn't delete the id, so Undo puts the widget back.
- LoR's blocks (a version 2 addition, lor-lz97, decision lor-p86v), each `{ "type": … }` with no other field. Each is the agent page's own section in a card, and shows at most a fixed number of rows, then a line "N more" that moves the grid to the layout's `t3` panel (for `needs-you` and `running`) or `lor` panel (for `t3-queue`) when it has one:
  - `today`: the agent page's Today: the day arc, the running timer, the one thing, the next events.
  - `needs-you`: the agent page's Needs you queue, T3's approvals, questions and failures, then LoR's flags, with the same count, all-clear and unseen rules and the same card actions; at most 3 cards. Approve, deny and answer write to T3, as on the agent page.
  - `running`: T3's turns running now, as the agent page's Running section; at most 3.
  - `t3-queue`: LoR's work queue for the agent: open tasks given to it, those queued for the next run first, then those being worked in a T3 thread; at most 5.
- New block types are added by the issues that use them.

### What the phone's edits mean

Launcher polish's rules carry over, with "home" meaning every `apps` block of the active layout's grid:

- `hidden` and Hide apply to every `apps` block, the dock and folders, in every layout.
- An app is on home once (any `apps` block, folders included) and in the dock once. This is a rule of the edits; the parser doesn't refuse duplicates.
- A folder an edit touches becomes `pinned`; a folder an edit empties is removed.
- Edits by place address a panel's cell, the block, and the index in it, so removing one panel moves no other item's place.
- Add to home appends to the first stack with room in the start row, by column; when all are full, it makes a new stack. An app in a folder on home (and not on home itself) moves out of the folder, from the menu or a drag from the drawer onto home, and the Undo bar names the folder ("YouTube Music is on home, out of Google."); one on home itself is refused (lor-83uq).
- A drop past the last page makes a new stack of `apps` and `dock` after the last panel in the start row (Launcher polish 4.5).
- An extra page goes when an edit empties or made it, not when it was already empty: after Remove from home, Hide, the open folder's + and a drop, a stack of only `apps` and `dock` whose `apps` block is empty is removed, unless the layout before the edit held such an empty stack at that cell, or it is at a `start` cell. So a page an edit or a drag empties goes, a page a drag made and then left goes (the drop compares with the file the drag began on), and an empty stack the minimap made stays. Home's stack stays, even empty. The parser accepts an empty one.
- The minimap's edits (lor-f3vn; `GridEdit.kt`):
  - A new or moved panel goes in a cell in reach: columns 0 to one past the last panel's, and rows 0 to one past the last panel's, of the active layout. A hand-written far cell still draws; the phone never writes one.
  - A layout holds one `t3` panel, one `drawer` panel and one `lor` panel.
  - A new stack is an empty `apps` block and `dock`, as a new page is. A block added to a stack is a type that takes no field (`clock`, an empty `apps`, `dock`, and LoR's four), and a stack holds each type once. A `widget` isn't added here ("a widget is added from the home menu"), but it moves and is removed like any block.
  - A panel moved onto another swaps with it. A `start` that named a moved panel's cell names its new cell. Setting the wide start on the narrow start's cell clears `start.wide`, and so does setting the narrow start on the wide start's cell.
  - A panel a `start` names can't be removed ("home starts there; choose another start first"), and a stack's last block can't be moved out or removed ("a stack needs a block; remove the panel instead"). Removing a stack or an `apps` block takes its apps off home.
  - No edit puts a block below a stack's `dock` or moves a `dock` up ("the dock stays under the other blocks"), so the file's order matches what home draws. A stack a hand edit left with its dock elsewhere can still be edited.
  - The row limit holds in the rows an added or moved stack or `apps` block lands in.
  - An edit of a panel or a block names its cell (and the block's index) and what the minimap drew there, and is refused ("Home changed; try again") when the file holds something else there.
- The widget edits (`WidgetEdit.kt`) address a widget by its `id`, in the active layout, so other edits don't move it:
  - Add puts a new `widget` block in a stack before its `apps` block, else before its `dock`, else last. It refuses an `id` the layout already holds.
  - Remove takes the block out; Resize sets its `height`. Each refuses an `id` the active layout doesn't hold ("widget 9 isn't on home"), and Remove refuses a stack's last block, as the minimap's does. Resizing to the height it has changes nothing.
  - Like the minimap's, these edits don't remove an extra page they empty.
- The phone writes only fields that differ from their defaults (`start.wide` only when set, a panel's `wide` only when it differs from its type's default, `look` and `drawer` only when not default), and writes `start`, `dock` and `panels` in every layout. So a `wide` the file states that equals the type's default is dropped at the next write; any other is kept.

Home draws the grid (lor-qdmy): one cell on screen, the narrow `start` after a home press. A swipe moves to the nearest cell that way in the row (the ends stop), or up and down by decision lor-y7qi's rule (the next row that way with a panel, wrapping; back to the cell a move came from, else the same column or the nearest, the left one on a tie). A move follows the finger. Sideways, a release commits past 30% of the screen's width or faster than 800 dp/s. Up and down (to the drawer too), it commits once the finger has gone 110 dp toward the other cell, or on a release faster than 400 dp/s toward it after 24 dp; a release faster than 400 dp/s back does not (lor-83uq; `FollowMotion` in `home/Motion.kt`). Each stack draws its `dock` last, under its other blocks, wherever the file puts it; the other blocks are drawn top to bottom in the file's order: `clock` as home's header wherever it stands, `apps` taking the space between. A stack whose first block other than the dock isn't a `clock` starts where home's grid would.

## Reading a version 1 file

A version 1 file is parsed with version 1's rules (its errors name version 1's places, as `layouts[0].home[3]`) and each layout is converted to the default grid:

- the `t3` panel at `[0, 0]`, left of home;
- home at `[1, 0]`: a stack of `clock`, `apps` (version 1's `home`) and `dock`;
- each of version 1's `pages` at `[2, 0]`, `[3, 0]` and on: a stack of `apps` (the page's `items`) and `dock`;
- one `drawer` panel at `[1, 1]`, in home's column on row 1 (decision lor-y7qi: every page reaches it by the grid's movement rule);
- `start` is `{ "narrow": [1, 0] }`;
- `name`, `expires` and `dock` as they were, and the top-level fields unchanged; no panel gets a `wide`.

Reading converts in memory only: the file stays version 1 until the phone next writes it, and then it is version 2. `layout.good.json` and the broken-file restore take a version 1 file the same way, and Undo of the first version 2 write puts the version 1 text back, byte for byte. The first run's default layout is this grid, with home's apps and no extra pages.
