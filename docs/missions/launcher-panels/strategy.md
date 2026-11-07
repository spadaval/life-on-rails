---
status: draft
last_changed: "2026-11-06: created from Sumeet's direction in the thread 'Launcher usability vs Smart Launcher'; takes over Launcher polish's Fold and widgets outcome"
---

# Why

Home is a fixed arrangement: the agent page, then home's pages, with the drawer under them. On the Fold's inner screen that arrangement is a phone page stretched to about 850 dp, with half of it empty (Launcher polish 6.8's critique). And widgets have nowhere to go, though Sumeet's Smart Launcher home is mostly widgets: the clock and the Tesla one. Rather than settle now what the inner screen and home should hold, Sumeet chose a platform: home becomes a grid of panels that the phone, an agent or a hand edit can rearrange, so the product design can be iterated later as edits to `layout.json`. Research behind it: [launcher-vs-smart-launcher.md](../../research/launcher-vs-smart-launcher.md).

This mission takes over the Fold and widgets parts of [Launcher polish](../launcher-polish/strategy.md)'s outcome, whose Increments 8 and 9 it replaces.

# Outcome

- **Home is a grid of panels, written in `layout.json`.** Each cell holds one panel. A panel is a column of blocks (a stack) or a custom type: `t3` (the agent page), `lor`, `drawer`. Blocks include `clock`, `widget`, `apps`, `dock`, and LoR's own (`today`, `needs-you`, `running`, `t3-queue`).
- **Moving between cells follows the finger** on both axes, as the drawer does since Launcher polish 7.1, and the vertical axis wraps.
- **The inner screen is used.** Each panel is full width or half width there; two neighbouring halves share it; every panel has a narrow and a wide layout.
- **Folding keeps your place.** Unfolding shows the inner screen that holds the cell you were on; after the screen has been off, home starts again at that screen's start cell.
- **The grid is edited on the phone,** in a minimap like Smart Launcher's page manager, with Undo, written to the file.
- **Home hosts Android widgets and a clock block** (time, date, the next event, the next alarm, and weather once it has a source).
- **The day it ships, nothing is worse.** A version 1 file opens as a default grid that looks and behaves like today's home.

Worth shipping on its own: the grid with today's home in it, then the inner screen, before the editor or new blocks.

# Shape

- **Schema version 2** (Sumeet, 2026-11-06). A layout's `panels` replace what `home`, `pages` and `dock` mean. The phone reads a version 1 file by converting it to the default grid and writes version 2 from then on. Version 1's rules hold: strict parsing, every error names its place, unknown fields are errors.
- **A sparse grid.** A panel sits at `[column, row]`. Each screen class has its own `start` cell: `narrow` (the phone, the cover screen) and `wide` (the inner screen).
- **The cover screen shows one cell.** A swipe moves one cell that way and follows the finger. A block that scrolls takes the swipe until it reaches its edge, then the grid moves. The system shade keeps the top edge.
- **The inner screen** is built row by row, left to right: a `full` panel is one inner screen, two neighbouring `half` panels share one, and a `half` with no half beside it uses its full layout. So no grid is invalid and no edit can break the inner screen. Each type has a default (`t3` and `lor` full, `drawer` half) that the file can override. A stack's wide layout flows its blocks into two columns, done once by the platform; `t3`, `lor` and `drawer` design their own.
- **Folding.** Unfolding shows the inner screen holding the current cell. Folding from a full panel shows that panel's cell; from a pair, the half last touched, else the one unfolded from, else the left. A fold or unfold is handled without recreating the activity, and open folders, search text, scroll positions and edit mode survive it. After any screen-off, home goes to the current screen's `start`.
- **Only visible cells and their neighbours are composed,** so a home press stays instant however many panels there are.
- **Native, not web.** LoR's and T3's panels are Compose, drawing from `/device/home` and new read endpoints. No WebView. A panel that would write to LoR's data needs Sumeet's yes first.
- **Phone edits go through the file,** as Launcher polish's P1 says: one `HomeStore`, atomic writes, Undo for every edit.
- **Launcher polish's rules still hold:** read Lawnchair, Kvaesitso and Neo Launcher before building a launcher basic, and name the source and licence (Kvaesitso's widget column is the nearest design to a stack); motion is one-shot and follows the finger; nothing loops or pulses; native and instant wins.

# Scope

In: the grid and its movement, schema version 2, the inner screen, folding, the minimap editor, widget hosting, the clock block, weather for it if Sumeet chooses a source, LoR's and T3's panels in their narrow and wide forms, and new read endpoints on the LoR server for them.

Out:
- writing to LoR's data from a panel (until Sumeet says yes);
- generative widgets;
- the agent-edited layout repository, named and temporary layouts, and agent-sorted folders (handed on from T3 Launcher);
- Launcher polish's carried robustness items that aren't about folding (lor-kxxz, outside the mission);
- root, Recents and gesture navigation.

Environment: `T3_Phone_API35` and `T3_Fold_API35` against the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`). Builds on boat (`android/boat-build.sh`). The real Fold only when Sumeet says so: its checks come after merge, in `needs-human` validation items outside the mission, each with its emulator stand-in run before merge.

# Authority

The orchestrator may change the increments, their order and their issues, and choose within this Shape: field names, block types beyond those named, the default layout's arrangement, gesture thresholds, and the libraries. It records each choice on its item.

These come back to Sumeet:
- installing anything on his phone, and any permission he must grant;
- a weather source that needs an account, a key or money;
- a panel that writes to LoR's data;
- a change to `layout.json` that changes a version 2 field's meaning;
- adding to or dropping from the Outcome.

# Risks and Unknowns

- **Two-axis movement against scrolling blocks.** Nested scrolling with a grid that follows the finger is the hardest gesture work yet. The first increment generalises the drawer's follow (Launcher polish 7's R9) before building on it.
- **The drawer from every page.** Settled by decision lor-y7qi: one drawer panel, reached from every page by the grid's movement rule.
- **Edits by place.** Increment 4's edits address a page and an index; they must address a panel and block without losing their tests.
- **Widget hosting on One UI** may differ from the emulator; widgets come after the inner screen for this reason.
- **Weather** has no source in LoR today.
