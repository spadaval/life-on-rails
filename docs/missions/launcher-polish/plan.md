# Launcher polish: plan

The intent is in [strategy.md](strategy.md). This file holds the decisions, increments and issues, as [T3 Launcher's plan](../t3-launcher/plan.md) does. LoR has no tracker, so each record carries a status line: `open`, `todo`, `backlog`, `active`, `done` or `superseded`. Dated notes go under the item they are about.

## How to work this mission

The same as T3 Launcher (its "How to work this mission"): work goes to `main` when it works, commits are named after the thing (`Launcher: long-press menu`), and a fresh agent that didn't write the code reviews each increment's commits, after they land, before the increment is marked done. Builds go through `android/boat-build.sh`. Checks run on `T3_Phone_API35` and, for anything about width or folding, `T3_Fold_API35`, against the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`). Nothing goes on Sumeet's phone unless he says so in the current thread; steps that need it carry **[real Fold]**.

Before building a launcher basic, read how Lawnchair (Apache-2.0), Kvaesitso (GPL-3.0) and Neo Launcher (GPL-3.0) do it, and name the source in the file (D1 of T3 Launcher).

The code today: `android/app/src/main/java/com/spadaval/lor/home/` (`HomeScreen.kt` draws the pager, home, folders, dock and drawer; `HomeModel.kt` holds the app list and the layout file; `Layout.kt` parses it; `Icons.kt`, `AppSearch.kt`) and `agent/AgentPage.kt`. The layout schema is in T3 Launcher's plan, Increment 3.2.

## Decisions

### P1: Where settings and phone edits live. Status: done (orchestrator, 2026-11-05; Sumeet may overrule)

Everything about home, including hidden apps, drawer sections and look settings, goes in `layout.json`, as new optional fields of schema version 1. Not in `SharedPreferences`. One file means an agent can change any of it, Increment 7's git sync carries all of it, and there is one place to read when home looks wrong. Fields that hold across layouts (hidden apps, look settings) sit at the top level; fields that differ per layout (home, dock, folders) stay in the layout.

A phone edit rereads the file, applies the change to the parsed `LayoutFile`, serialises it, parses the result again to validate it, then writes a temporary file and renames it over `layout.json`, and records the new modified time so home doesn't reload its own write. A hand edit that touches a folder sets the folder's `pinned` to true.

## Cross-links with T3 Launcher

Agreed with Sumeet, 2026-11-05:
- **Search.** Shortcuts and conversations (`LauncherApps.getShortcuts`) move here, to Increment 5. LoR people and T3 threads in search stay in T3 Launcher Increment 8.
- **Smart folders.** This mission sorts the drawer on the phone (Increment 2) and adds the schema field for drawer sections. T3 Launcher Increment 9 has an agent refine those sections and write their reasons.
- **Hand edits.** This mission writes phone edits to `layout.json` (P1), which answers T3 Launcher Increment 7's open question of whether hand edits reach the file: they do. Whether the repo receives them is Increment 7's.
- **Deferred review items.** The T3 Launcher items listed under Increment 8 here move to this mission.

## Increments

### Increment 1: The long press, and the basics every launcher has. Status: todo

**Goal.** A long press does something everywhere. An app gets its menu, empty home gets the home menu, and hidden apps and a Home settings screen exist. The launcher stops needing adb for the everyday changes.

**Done when.** On `T3_Phone_API35`: a long press on an app on home, in the dock, in a folder and in the drawer opens its menu; App info and Uninstall open the system's screens for that app; a hidden app leaves the drawer and search and comes back from Home settings; Add to home and Add to dock put the app there and the file shows it; the shortcuts listed for an app with shortcuts (Clock, Chrome) open the right screen. A JVM test covers each edit to `LayoutFile` and that a round trip keeps every field.

#### 1.1 Phone edits to `layout.json`. Status: todo
- **Goal.** The write path of P1 in `HomeModel`: `edit(change: (LayoutFile) -> LayoutFile)`, run on home's serial executor, with the reread, validation, atomic rename and stamp. Pure functions in `Layout.kt` for the first edits: add an app to the end of home, add to the dock, remove from home, remove from the dock, hide, unhide. Each refuses with a message instead of passing a limit (24 home items, 5 dock apps) or adding a duplicate.
- **Done when.** `LayoutTest` covers each edit, the limits, a round trip that keeps `expires`, `reason` and `pinned`, and the top-level `hidden` field's parsing errors. On the emulator an edit shows on home without a reload flicker, and `run-as` shows the file changed.
- **Schema addition.** Top-level `hidden` (optional): a list of apps (strings, as in `home`). A hidden app is left out of the drawer and search in every layout. It stays on home or in the dock if it is placed there explicitly.

#### 1.2 The app menu. Status: todo, after 1.1
- **Goal.** A long press on any app (home, dock, folder, drawer, search result) opens a small menu by the icon, with a haptic tick: the app's shortcuts first (`LauncherApps.getShortcuts` with `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED`, launched with `startShortcut`, the static and dynamic ones only, at most 5, ranked as the app ranks them), then App info (`LauncherApps.startAppDetailsActivity`), Uninstall (`Intent.ACTION_DELETE`; add `REQUEST_DELETE_PACKAGES`, an install-time permission; left out for system apps that can't be uninstalled), Hide, and Add to home / Add to dock, or Remove from home / Remove from dock where it already is. A tap anywhere else or back closes it. Read Lawnchair's `PopupContainerWithArrow` and `SystemShortcut`, and Kvaesitso's app context menu, first.
- **Done when.** The increment's checks for the menu pass on the emulator. A shortcut launch is recorded by the launch sensor with `from` set to where the menu was opened (a `shortcut` field added to the payload holding the shortcut id). `LaunchesTest` covers that field.
- **Note.** The clickable tiles lose their single-node semantics if the long press is added as a separate modifier; use `combinedClickable` so TalkBack and agent-device still see one node with both actions (the lesson from 2.2's debug screen).

#### 1.3 The home menu and Home settings. Status: todo, after 1.1
- **Goal.** A long press on empty home opens a menu: Wallpaper (`Intent.ACTION_SET_WALLPAPER`), Home settings, and, once Increment 4 exists, Edit home. Home settings is its own activity in home's task (`home/HomeSettingsActivity`), so back returns to home. It starts with: Hidden apps (each with Unhide), Default home app (`Settings.ACTION_HOME_SETTINGS`), and LoR app settings (the existing tabs). Later increments add their settings to this screen, not to the LoR tabs. The drawer ends with a quiet "Hidden apps (n)" row that opens the same list.
- **Done when.** The increment's checks for hiding pass on the emulator, and the screen survives a rotation.

### Increment 2: An organised drawer. Status: backlog

**Goal.** The drawer opens on sections instead of one A–Z grid: a row of suggested apps, Recent, Frequent, then categories (Work, Communication, Money, Travel, Media, Games, Tools, System, Other). A–Z (with a letter rail for fast scrolling) and Most used are a tap away, and the drawer remembers the view. The work profile has its own section. A newly installed app carries a dot and sits in "New" for three days.

**Provisional Done when.** On the emulator with about 40 apps installed, every app is in exactly one category section, the views switch, the letter rail jumps, and a fresh install shows in New. A JVM test covers categorising and the frequency ranking.

**Unknowns.**
- Where Recent and Frequent come from on the phone. The launch sensor sends launches to LoR and keeps nothing local. Likely a small local count beside it, or `UsageStatsManager` (the app has `PACKAGE_USAGE_STATS`). Read how Kvaesitso ranks.
- The category table: `ApplicationInfo.category` is unset for many apps. A table of common packages in the app fills the gaps; how big it needs to be shows on Sumeet's real app list **[real Fold]** (an adb `pm list packages` from the Fold, with his say-so).
- The schema field for drawer sections, shaped so T3 Launcher Increment 9 can add agent-written sections with reasons and `pinned` without changing it. A section moved or renamed by hand is pinned.

### Increment 3: No blank or raw screens. Status: backlog

**Goal.** A designed state for each of:
- home loading (the last good layout cached, else a quiet skeleton);
- a broken `layout.json` (the last good layout, with a small banner naming the error; this takes over the item in T3 Launcher Increment 7's unknowns);
- empty home ("Long-press to add apps");
- the agent page with T3 not paired (a pairing card, with the QR scan from `141551e`), with LoR unreachable (plain words, the last data greyed, a retry), and with nothing needing him (an all-clear that shows what's next today);
- no search results (search the web, the Play Store, or ask the agent through quick send);
- first run (make it default home, pair T3, check LoR, then the default layout).

**Provisional Done when.** Each state is reached on the emulator by its cause (a broken file, the test server stopped, T3 forgotten, a cleared app) and screenshotted; the impeccable skill's critique has been run on the set and its findings handled.

**Unknowns.** Whether the cached last-good layout is a second file or the last good parse kept in memory. A file survives a restart, which matters for a broken file at boot.

### Increment 4: Edit mode. Status: backlog, after 1

**Goal.** Edit home from the home menu or by dragging an app out of its long-press menu: drag to reorder, drop on an app to make a folder, drag in and out of the dock and folders, drag onto Remove. Folders can be renamed, reordered, and given a cover app. A folder whose last app leaves goes away. Home can have several pages (a schema addition: `home` becomes one page, `pages` holds more). Every edit shows "Undo" for a few seconds.

**Provisional Done when.** Each gesture is driven on the emulator and the file is read after each; undo restores the file byte for byte. A JVM test covers the edits.

**Unknowns.** How Kvaesitso and Neo Launcher do drag and drop in Compose. Read both before choosing; this is the mission's biggest piece. Whether pages are a schema addition or a version bump (comes back to Sumeet if it is a bump).

### Increment 5: Search that finds more. Status: backlog

**Goal.** Fuzzy matching for typos and initials ("yt" finds YouTube); apps' shortcuts and conversation shortcuts (from T3 Launcher Increment 8); contacts, with call and message actions; settings pages ("wifi", "bluetooth"); a calculator and unit answers; recent searches and apps when the field is empty; and an option to open the drawer with the keyboard up.

**Provisional Done when.** On the emulator, a dynamic shortcut from a test app and a conversation shortcut from a messaging app appear and open the right screen, a contact can be called, and "wfi" finds Wi-Fi settings. A JVM test covers ranking across the kinds.

**Unknowns.** How Kvaesitso indexes shortcuts and ranks mixed results. Contacts need `READ_CONTACTS`, which Sumeet grants (strategy, Authority).

### Increment 6: The look. Status: backlog

**Goal.** Labels and scrims follow the wallpaper (`WallpaperColors`), which ends the light-wallpaper contrast gap from 3.2's review. Icon packs (the ADW/Nova `org.adw.launcher.THEMES` intent), themed monochrome icons (Android 13's `monochrome` layer), and icon shape and size. Grid columns per screen, labels on or off, label size. The date header gets styles, a tap that opens the calendar, and the next event under it. Notification dots on apps and folders. Home, dock, folders, drawer and settings take their type and colour from `DESIGN.md`. All of it is set from Home settings.

**Provisional Done when.** Each setting changes home on the emulator and is in the file. A sideloaded icon pack applies. Dots appear for a test notification. Screenshots on a light and a dark wallpaper both read.

**Unknowns.** Notification dots need a `NotificationListenerService` that Sumeet enables in Settings (strategy, Authority). Whether the agent page's veil should also follow the wallpaper.

### Increment 7: Motion and feel. Status: backlog

**Goal.** The drawer follows the finger open and closed (replacing the 56 dp flip in `HomePage`). Apps open from their icon's bounds (`ActivityOptions.makeScaleUpAnimation` or a clip reveal). Folders open from their tile. Haptics on long press, pickup, drop, and the drawer threshold. Predictive back for the drawer, folders and menus. Configurable gestures: swipe down for notifications, double-tap to lock, a gesture for quick send.

**Provisional Done when.** Each motion recorded on the emulator; no animation loops; home-press latency is no worse than before (measured with `am start -W` on the HOME intent before and after).

**Unknowns.** Double-tap to lock needs an accessibility service or device admin; that comes back to Sumeet.

### Increment 8: The Fold, and holding state. Status: backlog

**Goal.** The inner screen gets a layout made for its width: the agent page and home side by side, page dots that line up with the content, the hinge and cutout insets handled. The cover and inner screens can have their own grids. Landscape and large fonts don't overflow. A fold, unfold, theme or font change keeps everything without reloading the app list or icons.

Carried from T3 Launcher's reviews:
- a process-scoped model and icon cache (3.2), which also fixes F4, card actions cancelled by a recreated activity (3.3);
- F2, a one-app default layout that can stick after a failed first load (3.3);
- the icon-cache race, a stale icon after a package change (3.2);
- normalised search labels cached (3.2);
- R3, a duplicate thread when a slow launch is retried; R4, quick-send text lost after a failed send once the sheet is gone; R5, the thumbnail decoded at full size on the main thread (4).

**Provisional Done when.** On `T3_Fold_API35`, folding and unfolding keeps home's pid, scroll and open drawer without reloading icons (logged), each carried item has a fix and a test or emulator check, and the inner-screen layout is screenshotted. **[real Fold]** The same on the device.

### Increment 9: Widgets. Status: backlog

**Goal.** Home hosts third-party Android widgets (`AppWidgetHost`, bind and configure flows, resize), placed in the layout file. Native LoR widgets: the day arc, the one thing, the running timer with pause, the next event, drawn from `/device/home`.

**Provisional Done when.** On the emulator a Clock and a Calendar widget are added, resized, survive a restart, and are in the file; each LoR widget draws from the test server. **[real Fold]** A widget behaves on One UI.

**Unknowns.** How Lawnchair hosts widgets, and how a widget's cell size maps to list-order layout, which has no cells. The schema shape for a widget item.

## Open questions for Sumeet

None yet. Permissions he must grant (contacts, notification access, an accessibility service) are asked for when their increment starts.
