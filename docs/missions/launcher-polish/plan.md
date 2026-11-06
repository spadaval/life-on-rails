# Launcher polish: plan

The intent is in [strategy.md](strategy.md). This file holds the decisions, increments and issues, as [T3 Launcher's plan](../t3-launcher/plan.md) does. LoR has no tracker, so each record carries a status line: `open`, `todo`, `backlog`, `active`, `done` or `superseded`. Dated notes go under the item they are about.

## How to work this mission

The same as T3 Launcher (its "How to work this mission"): work goes to `main` when it works, commits are named after the thing (`Launcher: long-press menu`), and a fresh agent that didn't write the code reviews each increment's commits, after they land, before the increment is marked done. Builds go through `android/boat-build.sh`. Checks run on `T3_Phone_API35` and, for anything about width or folding, `T3_Fold_API35`, against the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`). Nothing goes on Sumeet's phone unless he says so in the current thread; steps that need it carry **[real Fold]**.

Before building a launcher basic, read how Lawnchair (Apache-2.0), Kvaesitso (GPL-3.0) and Neo Launcher (GPL-3.0) do it, and name the source in the file (D1 of T3 Launcher).

The code today: `android/app/src/main/java/com/spadaval/lor/home/` (`HomeScreen.kt` draws the pager, home, folders, dock and drawer; `HomeModel.kt` holds the app list and the layout file; `Layout.kt` parses it; `Icons.kt`, `AppSearch.kt`) and `agent/AgentPage.kt`. Schema version 1 of `layout.json` is written down in T3 Launcher's plan, Increment 3.2; what this mission adds to it is written down under [Schema additions](#schema-additions) below, and each issue that adds a field adds it there.

## Decisions

### P1: Where settings and phone edits live. Status: done (orchestrator, 2026-11-05; Sumeet may overrule)

Everything about home, including hidden apps, drawer sections and look settings, goes in `layout.json`, as new optional fields of schema version 1. Not in `SharedPreferences`. One file means an agent can change any of it, Increment 7's git sync carries all of it, and there is one place to read when home looks wrong. Fields that hold across layouts (hidden apps, look settings) sit at the top level; fields that differ per layout (home, dock, folders) stay in the layout.

One process-wide store owns the file: a `HomeStore` held by `LorApp`, holding the app list, the parsed layout, its modified-time stamp and the edit path, on one serial dispatcher. `HomeModel` (per activity today) becomes a view of it, so home, Home settings and any later screen read and write through the same place and writes stay serialised. The icon cache stays per activity until Increment 8.

A phone edit rereads the file, applies the change to the parsed `LayoutFile`, serialises it, parses the result again to validate it, then writes a temporary file and renames it over `layout.json`, and records the new modified time so home doesn't reload its own write. A hand edit that touches a folder sets the folder's `pinned` to true.

## Cross-links with T3 Launcher

Agreed with Sumeet, 2026-11-05:
- **T3 Launcher is closed** (2026-11-05). Its Increments 7 to 10, which the items below name, moved out to a later mission; their text is under "Handed on" in [its plan](../t3-launcher/plan.md#handed-on-2026-11-05), with the list of real-Fold checks.
- **Search.** Shortcuts and conversations (`LauncherApps.getShortcuts`) move here, to Increment 5. LoR people and T3 threads in search stay in T3 Launcher Increment 8.
- **Smart folders.** This mission sorts the drawer on the phone (Increment 2) and adds the schema field for drawer sections. T3 Launcher Increment 9 has an agent refine those sections and write their reasons.
- **Hand edits.** This mission writes phone edits to `layout.json` (P1), which answers T3 Launcher Increment 7's open question of whether hand edits reach the file: they do. Whether the repo receives them is Increment 7's.
- **Deferred review items.** From T3 Launcher's reviews: the light-wallpaper contrast (3.2) moves to Increment 6 here, and the rest listed under Increment 8 here move there. F2 is in the app-list code that 1.1 moves into `HomeStore`, so 1.1 fixes it. F4 and R3 to R5 are robustness of the agent page and the sheet that home opens, so they sit with the other robustness items in Increment 8. F5 and F7, test gaps in T3 Launcher's own code, stay there, as does 3.3's carried item about the agent page taking a split pane in its own activity, which waits on the real Fold.

## Increments

### Increment 1: The long press, and the basics every launcher has. Status: active

**Goal.** A long press does something everywhere. An app gets its menu, empty home gets the home menu, and hidden apps and a Home settings screen exist. The launcher stops needing adb for the everyday changes.

**Done when.** On `T3_Phone_API35`: a long press on an app on home, in the dock, in a folder and in the drawer opens its menu; App info and Uninstall open the system's screens for that app; a hidden app leaves home, the dock, the drawer and search, and comes back to the drawer from Home settings; Add to home and Add to dock put the app there and the file shows it; the shortcuts listed for an app with shortcuts (Clock, Chrome) open the right screen. JVM tests cover each edit to `LayoutFile` and that a round trip keeps every field.

#### 1.1 `HomeStore` and phone edits to `layout.json`. Status: done
- **Goal.** The store and write path of P1. `HomeStore` in `home/`, created once by `LorApp`, takes over from `HomeModel` the app list, the layout file, its stamp and the serial dispatcher; `HomeModel` keeps only what is per activity (the icon cache) and reads the store's flows. The file steps sit in a piece with no `Context`, `LayoutStore(file: File)` in `Layout.kt`, so the JVM tests can reach them: `LayoutStore.edit(change: (LayoutFile) -> LayoutFile): Result<Long>` rereads, applies, serialises, parses again, writes `layout.json.tmp`, renames it over `layout.json`, and returns the new modified time. `suspend fun HomeStore.edit(change)` runs it on the serial dispatcher and records that time as the stamp. `HomeStore` logs one line (`LorHome`) for each file read and each write. While the app list is empty, `HomeStore` reloads it on the next resume, and it writes no default layout from a list it hasn't fully loaded (F2 from T3 Launcher 3.3). Pure functions in `Layout.kt` for the first edits:
  - add an app to the end of the current layout's home; add to the dock;
  - remove an app from home, from the dock, or from the folder that holds it. A folder left empty is removed (the parser rejects `apps: []`). A folder an edit touches gets `pinned: true`;
  - hide: add the app's package name to `hidden`, and remove from home, the dock and folders in every layout every ref that resolves to an activity of that package; unhide: take it out of `hidden`, nothing else.
  Each refuses with a message instead of passing a limit (24 home items, 5 dock apps) or adding a duplicate.
- **Scope.** Main-profile apps only: the schema can't name a work-profile app (`resolve` in `HomeScreen.kt` matches only `Process.myUserHandle()`), so nothing in this increment edits one. Write `hidden` into [Schema additions](#schema-additions) and point `Layout.kt`'s comment at this plan as well as 3.2.
- **Done when.** `LayoutTest` covers each edit and refusal, an emptied folder going away, `pinned` set on a touched folder, a round trip that keeps `expires`, `reason` and `pinned`, and `hidden`'s parsing errors. A test of `LayoutStore` against a temporary directory shows `edit` leaves the old file in place when the result fails to validate, and replaces it when it passes. A test covers F2: an empty app list writes no default. `assembleDebug testDebugUnitTest` passes on boat and home still draws on the emulator.
- **2026-11-05.** Landed. Choices: `LayoutStore.edit` returns `Result<LayoutStore.Read>` (the edited file and its modified time) rather than `Result<Long>`, so the store shows the edit without rereading the file. The edits take an `AppId` (package and activity) and match refs with `AppRef.matches`, as home resolves them; Add writes the package name when it has one launchable activity, otherwise `package/activity`. `HomeStore` is made lazily by `LorApp.home`, so a sync worker's process doesn't load the app list. `LayoutEditTest` holds the tests. Reviewed with the increment.

#### 1.2 The app menu. Status: todo, after 1.1
- **Goal.** A long press on any app (home, dock, folder, drawer, search result) opens a small menu by the icon, with a haptic tick. Read Lawnchair's `PopupContainerWithArrow`, `SystemShortcut` and `ShortcutRequest`, and Kvaesitso's app context menu, first.
  - **Shortcuts first,** when `LauncherApps.hasShortcutHostPermission()` is true (only the default home has it; otherwise the section is left out). Query `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST` for the activity, as Launcher3's `ShortcutRequest.PUBLISHED` does. Order manifest shortcuts before dynamic ones, then by `rank`; show at most 5. Launch with `startShortcut`, catching its failures (a locked profile, a shortcut that's gone) the way `startActivitySafely` does, with a toast.
  - **Then** App info (`LauncherApps.startAppDetailsActivity`), Uninstall (`Intent.ACTION_DELETE` with `package:` data and `Intent.EXTRA_USER` set to the app's profile, as Launcher3's uninstall does; add `REQUEST_DELETE_PACKAGES`, an install-time permission; left out for system apps that can't be uninstalled), Hide, and Add to home / Add to dock, or Remove from home / Remove from dock / Remove from folder where it already is. For a work-profile app only shortcuts, App info and Uninstall show.
  - A tap anywhere else or back closes it.
  - Use `combinedClickable`, not a separate long-press modifier, so TalkBack and agent-device still see one node with both actions (the lesson from T3 Launcher 2.2's debug screen).
- **Launch records.** A shortcut launch is recorded by the launch sensor with `from` set to where the menu was opened and a new `shortcut` field holding the shortcut id. `LaunchesTest` covers the field, and the spec's sensor table (`docs/v2-product-engineering-spec.md`, Launches) lists it (AGENTS.md: a change that crosses the spec changes the spec).
- **Done when.** The increment's checks for the menu pass on the emulator. After Add to home, home shows the app, `run-as` shows it in the file, and `logcat` shows `HomeStore`'s write line and no read line when home next resumes (home didn't reread its own write).

#### 1.3 The home menu and Home settings. Status: todo, after 1.1
- **Goal.** A long press on empty home opens a menu: Wallpaper (`Intent.ACTION_SET_WALLPAPER`), Home settings, and, once Increment 4 exists, Edit home. Home settings is its own activity in home's task (`home/HomeSettingsActivity`), reading and writing through `HomeStore`, so back returns to home. It starts with: Hidden apps (each with its icon, label and Unhide), Default home app (`Settings.ACTION_HOME_SETTINGS`), and LoR app settings (the existing tabs). Later increments add their settings to this screen, not to the LoR tabs. The drawer ends with a quiet "Hidden apps (n)" row that opens the same list.
- **Done when.** The increment's checks for hiding pass on the emulator: hide from the drawer, the app is gone from the drawer and from search for its name, unhide from Home settings, it's back. The screen keeps its state across a rotation. The spec's launcher line (`docs/v2-product-engineering-spec.md`, "The drawer and search list every launchable app") says hidden apps are left out.

### Increment 2: An organised drawer. Status: backlog

**Goal.** The drawer opens on sections instead of one A–Z grid: a row of suggested apps, Recent, Frequent, then categories (Work, Communication, Money, Travel, Media, Games, Tools, System, Other). A–Z (with a letter rail for fast scrolling) and Most used are a tap away, and the drawer remembers the view. The work profile has its own section. A newly installed app carries a dot and sits in "New" for three days. Sections are edited by hand: move an app to another section from its menu, rename, reorder or hide a section, and make a new one. A section edited by hand is `pinned` and the sorter leaves it alone.

**Provisional Done when.** On the emulator with about 40 apps installed, every app is in exactly one category section, the views switch, the letter rail jumps, a fresh install shows in New, and each hand edit to a section lands in the file and survives the sorter's next run. A JVM test covers categorising and the frequency ranking.

**Unknowns.**
- Where Recent and Frequent come from on the phone. The launch sensor sends launches to LoR and keeps nothing local. Likely a small local count beside it, or `UsageStatsManager` (the app has `PACKAGE_USAGE_STATS`). Read how Kvaesitso ranks.
- The category table: `ApplicationInfo.category` is unset for many apps. A table of common packages in the app fills the gaps; how big it needs to be shows on Sumeet's real app list **[real Fold]** (an adb `pm list packages` from the Fold, with his say-so).
- The schema field for drawer sections, shaped so T3 Launcher Increment 9 can add agent-written sections with reasons and `pinned` without changing it. Written into [Schema additions](#schema-additions).

### Increment 3: No blank or raw screens. Status: backlog

**Goal.** A designed state for each of:
- home loading (the last good layout cached, else a quiet skeleton);
- a broken `layout.json` (the last good layout, with a small banner naming the error; this takes over the item in T3 Launcher Increment 7's unknowns);
- empty home ("Long-press to add apps");
- the agent page with T3 not paired, with LoR unreachable, and with nothing needing him. Each has a line of text today ("T3 isn't paired…", "LoR isn't reachable (…)", "Nothing needs you.", in `AgentPage.kt`); they become a pairing card with the QR scan from `141551e`, plain words with the last data greyed and a retry, and an all-clear that shows what's next today;
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

**Unknowns.**
- How Kvaesitso indexes shortcuts and ranks mixed results.
- Messaging apps move conversations into cached shortcuts, which only `FLAG_MATCH_CACHED` (API 30) returns; the query needs it. The people a shortcut belongs to (`FLAG_GET_PERSONS_DATA`) is a system API needing `ACCESS_SHORTCUTS`, so a person's chats are matched by the shortcut's label. T3 Launcher Increment 8 relies on that matching.
- Contacts need `READ_CONTACTS`, which Sumeet grants (strategy, Authority).

### Increment 6: The look. Status: backlog

**Goal.** Labels and scrims follow the wallpaper (`WallpaperColors`), which ends the light-wallpaper contrast gap from 3.2's review. Icon packs (query both ADW's `org.adw.launcher.THEMES` and Nova's `com.novalauncher.THEME`, as Lawnchair does), themed monochrome icons (Android 13's `monochrome` layer), and icon shape and size. Grid columns per screen, labels on or off, label size. The date header gets styles, a tap that opens the calendar, and the next event under it (from `/device/home`'s `day.events`, which needs no calendar permission). Notification dots on apps and folders. Home, dock, folders, drawer and settings take their type and colour from `DESIGN.md`. All of it is set from Home settings.

**Provisional Done when.** Each setting changes home on the emulator and is in the file. A sideloaded icon pack applies. Dots appear for a test notification. Screenshots on a light and a dark wallpaper both read.

**Unknowns.** Notification dots need a `NotificationListenerService` that Sumeet enables in Settings (strategy, Authority). Whether the agent page's veil should also follow the wallpaper.

### Increment 7: Motion and feel. Status: backlog

**Goal.** The drawer follows the finger open and closed (replacing the 56 dp flip in `HomePage`). Apps open from their icon's bounds (`ActivityOptions.makeScaleUpAnimation` or a clip reveal). Folders open from their tile. Haptics on long press, pickup, drop, and the drawer threshold. Predictive back for the drawer, folders and menus. Configurable gestures: swipe down for notifications, double-tap to lock, a gesture for quick send.

**Provisional Done when.** Each motion recorded on the emulator; no animation loops; home-press latency is no worse than before (the median within 10%): with another app in front, `am start -W -a android.intent.action.MAIN -c android.intent.category.HOME` ten times on the build before and after, comparing the median `TotalTime` (with home already in front it reports nothing).

**Unknowns.** Double-tap to lock needs an accessibility service or device admin, and swipe down for notifications needs either the hidden `StatusBarManager.expandNotificationsPanel` (by reflection, with `EXPAND_STATUS_BAR`) or an accessibility service. An accessibility service comes back to Sumeet.

### Increment 8: The Fold, and holding state. Status: backlog

**Goal.** The inner screen gets a layout made for its width: the agent page and home side by side, page dots that line up with the content, the hinge and cutout insets handled. The cover and inner screens can have their own grids. Landscape and large fonts don't overflow. A fold, unfold, theme or font change keeps everything without reloading the app list or icons.

Carried from T3 Launcher's reviews:
- a process-scoped icon cache (3.2; the model moved into `HomeStore` in 1.1);
- F4, card actions cancelled by a recreated activity (3.3): run them in a process scope (`LorApp`'s, or `HomeStore`'s), as quick send's send already does;
- the icon-cache race, a stale icon after a package change (3.2);
- normalised search labels cached (3.2);
- R3, a duplicate thread when a slow launch is retried; R4, quick-send text lost after a failed send once the sheet is gone; R5, the thumbnail decoded at full size on the main thread (4).

**Provisional Done when.** On `T3_Fold_API35`, a fold, an unfold, a dark-mode switch and a font-scale change each recreate home with its open drawer and scroll kept and with no app-list or icon reload (a debug log line per load, counted before and after), each carried item has a fix and a test or emulator check, and the inner-screen layout is screenshotted. **[real Fold]** The same on the device.

### Increment 9: Widgets. Status: backlog

**Goal.** Home hosts third-party Android widgets (`AppWidgetHost`, bind and configure flows, resize), placed in the layout file. Native LoR widgets: the day arc, the one thing, the running timer with pause, the next event, drawn from `/device/home`.

**Provisional Done when.** On the emulator a Clock and a Calendar widget are added, resized, survive a restart, and are in the file; each LoR widget draws from the test server. **[real Fold]** A widget behaves on One UI.

**Unknowns.** How Lawnchair hosts widgets, and how a widget's cell size maps to list-order layout, which has no cells. The schema shape for a widget item.

## Schema additions

Optional fields this mission adds to `layout.json` schema version 1, each written here by the issue that adds it. Version 1's rules hold: unknown fields are errors, and an addition never changes an existing field's meaning.

- (1.1) Top-level `hidden`: a list of apps, written as in `home`; the phone writes package names. A package name hides every activity of that package, as `AppRef.matches` matches. It applies to main-profile apps only; a work-profile copy of the package stays. A hidden app is left out of the drawer and search in every layout. One placed on home, the dock or in a folder by the file is still shown there; Hide from the menu removes it from those places too.

## Open questions for Sumeet

None yet. Permissions he must grant (contacts, notification access, an accessibility service) are asked for when their increment starts.
