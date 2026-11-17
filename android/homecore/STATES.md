# Named states

Every state the launcher lab and the debug app can open by name. The data is in
`src/commonMain/kotlin/com/spadaval/lor/lab/`: `Fixture.kt` is the shape, `Parts.kt` the parts
states are made of, and `Fixtures.kt` the states. `FixtureCatalogueTest` keeps this file and
`Fixtures.all` the same.

## Opening one

- In the lab (`android/web-dev.sh`, then `http://127.0.0.1:3860/`): the State dropdown, or the hash
  `#<version>/<state>/<screen>/<wallpaper>`, as in `#current/overfull/inner/pale`. The screen
  (`cover`, `inner`) and the wallpaper (`dark`, `pale`, `mixed`) may be left off.
- A one-off: JSON in the state's place. With `base`, it starts from that named state and changes
  what it names, objects key by key: `#current/{"base":"demo","t3":{"status":"NOT_PAIRED"}}/inner`.
  Without `base`, it is the whole fixture, and what it leaves out takes the defaults. Or drop a
  `.json` file on the page. The JSON button shows the open state's full JSON to start from.
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
- `sensors-failed`: Sensors after a failed sync with two permissions missing.
- `home-settings-tabs`: Home settings with hidden apps and four drawer tabs.

## Surfaces in the module

| Surface | States it draws |
| --- | --- |
| The kit sheet (`current`, until home moves) | One look for every state that opens nothing (`where.open` `NOTHING`); its title names the state and the screen. |
| The LoR app's tabs (`current`): `where.open` `APP_TAB`, `where.target` `timer`, `sensors`, `settings` or `t3` (Settings' T3 debug view) | Every state, as one-offs over it; the links below. |
| Home settings (`current`): `where.open` `HOME_SETTINGS` | `home-settings-tabs`, and every state as a one-off; the links below. |
| The gesture test surface (`gestures`) | One look for every state: a pager, a list, a tile with a long-press menu, two panes on the inner screen. Not a launcher; `scripts/lab/run.mjs` is proven on it. |

A screen in each of its states, by link (`android/web-dev.sh`, then `http://127.0.0.1:3860/` and the hash;
`shots/lab.json` has them all):

- Timer idle: `#current/{"base":"demo","where":{"open":"APP_TAB","target":"timer"}}`; running:
  `{"base":"demo","timer":{"state":"RUNNING","intent":"Offline maps spec","title":"Tile sources","start":"2026-11-17T19:12:00Z"},"where":{"open":"APP_TAB","target":"timer"}}`;
  paused: the same with `"state":"PAUSED","pausedAt":"2026-11-17T19:47:00Z"`; unreachable: `"timer":{"state":"UNREACHABLE"}`.
- Sensors all allowed: `{"base":"demo","where":{"open":"APP_TAB","target":"sensors"}}`; some not allowed, the sync fine:
  `{"base":"sensors-failed","phone":{"sync":{"uploaded":72,"pending":0,"errors":[]}}}`; a failed sync with errors: `sensors-failed`.
- Settings saved, testing, failed: `{"base":"demo","settings":{"result":"Saved."},"where":{"open":"APP_TAB","target":"settings"}}`,
  with `"Checking…"`, or `"Cannot connect: failed to connect to /10.0.2.2 (port 3857) after 10000ms"`.
- Home settings with hidden apps, four drawer tabs and an icon-pack list: `home-settings-tabs`; its broken-layout banner:
  `{"base":"failed","where":{"open":"HOME_SETTINGS"}}`; a first run's: `{"base":"empty","where":{"open":"HOME_SETTINGS"}}`.
- T3 debug paired: `{"base":"demo","where":{"open":"APP_TAB","target":"t3"}}`; not paired: the same over `empty`.

Both screen sizes are the link's `cover` and `inner`, for every state, and either turned on its side
with `landscape` as the link's fifth part (`#current/demo/inner/dark/landscape`).

## Gestures

The lab takes the mouse as a finger (a drag is a swipe, a held press a long press, the wheel
scrolls) and touch as itself. Its toolbar has Home (the state drawn afresh), Back (Compose's back
handlers, through the navigation-event dispatcher), Fold and Unfold, Rotate, and the animation speed
(1, a quarter, none); the keys are H, Escape or B, F, R and S. Fold, Rotate and a new wallpaper keep
the surface's place; Home and a new state or version start it again.

A script plays gestures with a recording to look at:
`node scripts/lab/run.mjs scripts/lab/gestures/drag.mjs --version gestures --state demo --out /tmp/drag`
writes `recording.webm`, `strip.png` (the screen every 50 ms from the first touch until it settles),
`final.png` and `run.json`. The helpers are in `scripts/lab/gesture.mjs`; a script names places
in dp or by an anchor a surface sets with `Modifier.labAnchor("name")`. `scripts/lab/gestures/`
has one script per helper, for `gestures`. Each item that moves a surface into the module writes
that surface's scripts and anchors.

## To come, with the item that moves the surface

The launcher and app states lor-q165 opens by link, by these names. Each is added to `Fixtures.kt`
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

Widgets (decided here, for lor-xdoi to build): a fixture's layout names a widget by its provider,
and in the lab a widget is to be drawn as a labelled box of its height, not a snapshot image.
Nothing draws widgets in the module yet.
