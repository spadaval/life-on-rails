# Named states

Every state the launcher lab and the debug app can open by name. The data is in
`src/commonMain/kotlin/com/spadaval/lor/lab/`: `Fixture.kt` is the shape, `Parts.kt` the parts
states are made of, and `Fixtures.kt` the states. `FixtureCatalogueTest` keeps this file and
`Fixtures.all` the same.

## Opening one

- In the lab (`android/web-dev.sh`, then `http://127.0.0.1:3860/`): the State dropdown, or the hash
  `#<version>/<state>/<screen>/<wallpaper>`, as in `#current/overfull/inner/pale`. The screen
  (`cover`, `inner`) and the wallpaper (`dark`, `pale`, `mixed`) may be left off.
- A one-off: JSON in the state's place, starting from a named state and changing what it names,
  objects key by key: `#current/{"base":"demo","t3":{"status":"NOT_PAIRED"}}/inner`. Or drop a
  `.json` file on the page. Copy JSON puts the open state's full JSON on the clipboard to start from.
- On a device, in the debug build:
  `adb shell am start -n com.spadaval.lor/.home.HomeActivity --es lab_state overfull`
  (`--es lab_version <name>` for a version other than `current`). It draws in its own process
  (`:lab`), where the app starts no sync and no T3 connection, and reads no server, T3 or layout
  file. A banner says it is a fixture. Back leaves it. Release builds have no such entry.

## What a fixture holds

The layout as `layout.json` holds it; the app list (placeholder icons unless one names a bundled
icon); LoR's `/device/home`, `/device/tasks` and `/device/people` replies as the server sends them,
each loading, read, stale or failed; T3's connection, threads (cards come from their requests),
questions and transcripts; the timer; sensors, the last sync, notification dots and permissions;
settings; what search, the drawer and quick dial read (contacts, shortcuts and conversations,
settings pages, launch counts, recent searches, quick dial's ranking); and where the screen is: the
cell, what is open and what it points at, the scroll, the typed text, the held item.

The default, `demo`, is the demo set's day (`seeds/demo/`) at 15:00 on 2026-11-17, the moment the
dashboard's design tools open on. LoR's replies are in `DemoDay.kt`, written by
`node scripts/lab/demo-fixture.mjs` from a test server with the demo set loaded
(`--check` says whether the checked-in file still matches).

## States

Each is a condition of the whole app; a surface draws the part of it it reads.

- `demo`: The demo day at 15:00: a full home, LoR's replies, T3 live with one approval and a running turn.
- `empty`: The first start: no layout yet, no server, T3 unpaired, nothing counted.
- `loading`: Every read under way: the app list, LoR's three reads, T3 connecting, the timer unknown.
- `stale`: LoR last read two hours ago and timing out since; T3 retrying on what it saw 40 minutes ago.
- `failed`: LoR unreachable, T3's pairing revoked, the last sync failed, two permissions gone, the layout file broken.
- `overfull`: Everything at its most: 200 apps, five full pages, twelve cards, nine flags, long names everywhere.

## Surfaces in the module

| Surface | States it draws |
| --- | --- |
| The kit sheet (`current`, until home moves) | One look for every state; its title names the state and the screen. |

Both screen sizes are the link's `cover` and `inner`, for every state.

## To come, with the item that moves the surface

The ten launcher and app states lor-q165 opens by link, by these names. Each is added to `Fixtures.kt`
and to the list above by the item that moves its surface into the module, with that surface's own
ordinary, empty, loading, stale, failed and overfull states where it has them.

- `home-full` (lor-xdoi): a full home on the cover screen over a pale wallpaper (`#current/home-full/cover/pale`).
- `first-run` (lor-rgx4): a first-run home with the set-up card.
- `drawer-all-scrolled` (lor-xdoi): the drawer's All tab mid-scroll.
- `search-mixed` (lor-xdoi): search with app, contact and settings results.
- `app-menu` (lor-xdoi): an app's long-press menu.
- `agent-waiting` (lor-rgx4): the agent page with one approval, one question and two LoR flags.
- `agent-t3-stale` (lor-rgx4): the agent page with T3 disconnected and stale.
- `thread-long` (lor-rgx4): a thread open on the inner screen with a long transcript.
- `sensors-failed` (lor-t8lq): Sensors after a failed sync with two permissions missing.
- `home-settings-tabs` (lor-t8lq): Home settings with hidden apps and four drawer tabs.

Widgets: a fixture's layout names a widget by its provider, and the lab draws a labelled box of
the widget's height in its place, not a snapshot image (lor-oech's open question, answered there).
