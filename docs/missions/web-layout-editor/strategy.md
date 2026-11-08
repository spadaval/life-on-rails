---
status: draft
last_changed: "2026-11-07: created from Sumeet's direction in the thread about a cloud layout editor"
---

# Why

Home is now a grid of panels in `layout.json`, but the only way to arrange it is on the phone: a minimap on a 6-inch screen, or an agent writing JSON it cannot see. Sumeet wants to arrange home in the LoR dashboard on a big screen, see it exactly as the phone will draw it, at the phone's real size, and send it to the phone. Most of the way there exists: the `agent-layout` branch (T3 Launcher's Increment 7, reviewed four times, never merged) keeps every version of the layout in LoR and has the phone apply a new version with Undo, and most of home's drawing code is plain Compose with no Android in it.

# Outcome

- **Home's layout lives in LoR.** Every version, from the phone, an agent or the web, is one row in one history, and any version can be reverted. A new version reaches the phone the next time home comes to the front, and the phone says what changed, with Undo.
- **A page in the dashboard draws home as the phone does,** each cell of the grid, on the cover screen and the inner screen, with the phone's icons (its icon pack and themed icons), its wallpaper, its font and its widgets as they last looked.
- **At real size.** Once calibrated by holding the phone against the monitor, the drawn phone is as big as the real one.
- **Home is edited there:** panels, blocks, apps, folders and the dock, with the same edits and the same refusals as the phone's minimap and drags, saved as a new version.

Worth shipping on its own: the sync with a plain history page (the agent can edit home again, and a version can be reverted from the web), then the real-size view, before editing on the web.

# Success Criteria

- round-trip: On `T3_Fold_API35` against the test server, an edit made on the web page (an app moved into a folder, a stack added in an empty cell and set Half) shows on the emulator's home after the next resume, with the version's note; Undo on the phone puts it back; and the web's history shows all three versions. Shown by the mission's acceptance validation item, with screenshots.
- same-pixels: For one layout and one device's uploads, the web's drawing of each cell, rendered at the phone's pixel size, and the emulator's screenshot of that cell put every tile, label and panel edge within 2 px of each other, on both of the Fold's screens. Shown by the acceptance validation item, with an overlay diff of each cell. Widgets are compared by their frame only, since the web shows a snapshot.

# Shape

- **One drawing, two platforms** (Sumeet, 2026-11-07, decision lor-djn2). Home's drawing and editing code moves into a Kotlin Multiplatform module that builds for Android and for the browser (Compose Multiplatform, `wasmJs`). The web runs the phone's own code: the grid, its panels and blocks, `InnerScreens`, `HomeLook`, the parser and `GridEdit`'s edits. There is no TypeScript copy of the layout math or the schema. What only the phone has (icons from `LauncherApps` and icon packs, widget hosting, the wallpaper, launching apps) is passed in by each platform; on the web, it is what the phone uploaded.
- **LoR holds the versions,** as T3 Launcher's Increment 7 designed it and `agent-layout` built it: `layout_version` rows numbered per device, a base check on every write, Undo as a new version, `rejected` for a version the phone can't parse, and the phone's `layout.json` as its offline copy. The web is one more writer beside the phone and the agent. No accept step: a layout is easy to undo (memory: reversible changes apply at once with Undo).
- **The phone owns the schema.** The shared parser is the phone's `Layout.kt`, so the web refuses what the phone would refuse, before saving. LoR's server still checks only that a body is a JSON object of a known version under 256 KB.
- **A mirror, not a live view.** The web draws from what the phone last uploaded: launchable apps, each app's icon as home draws it, the wallpaper, each screen's size in pixels, dp and physical dpi with its insets, the font, and widget snapshots. A widget is drawn from its last snapshot, else its preview image (`AppWidgetProviderInfo.previewImage`), else a box with the app's icon, as Launcher3's `PendingAppWidgetHostView` draws a widget still loading.
- **Real size by the phone itself.** The browser cannot know a monitor's pixel size, so the page asks once: hold the phone against the screen and drag until the drawn one matches. The phone's physical size comes from its own report. The scale is kept in that browser.
- **Saving is pushing.** A save writes a version. The phone takes it on home's next resume, as it takes the agent's. No push channel.
- **Native wins where they disagree.** The shared module must not make the phone slower, bigger or less native; if a choice favours the web's fidelity over the phone, the phone wins.

# Scope

In: bringing `agent-layout`'s sync and `lor phone layout` up to schema version 2 and onto `main`; a history page in the dashboard (versions, view, revert); the shared Kotlin module and the Android app on it; the wasm build and how it reaches `pnpm build`; the phone's uploads listed under Shape and their storage in LoR; the real-size view with calibration, both Fold screens and folding; editing panels, blocks, apps, folders and the dock on the web.

Out:
- adding a widget from the web (binding a widget needs the phone's widget host and its permission dialog), though a placed widget can be moved or removed;
- Home settings' look, hidden apps and drawer sections on the web (an agent or the phone sets them; later if missed);
- named and temporary layouts, and agent-sorted folders (T3 Launcher's Increments 9 and 10, still later);
- a live stream of the phone's screen.

Environment: the dashboard in desktop Chrome; `T3_Phone_API35` and `T3_Fold_API35` against the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`); Gradle builds, Android and wasm, on boat. The real phone and Fold only when Sumeet says so, in `needs-human` validation items after merge, outside the mission, each with an emulator stand-in run before merge.

# Authority

The orchestrator may change the increments, their order and their issues, and choose within this Shape: module and package names, the page's route, the upload formats and tables, library versions, and which files move into the shared module. It records each choice on its item.

These come back to Sumeet:
- giving up the shared Kotlin drawing for another approach, if the spike or the split finds it unworkable;
- installing anything on his phone, and any permission he must grant;
- a change that makes home on the phone slower to open or visibly different;
- a change to `layout.json` that changes a version 2 field's meaning;
- adding to or dropping from the Outcome.

# Risks and Unknowns

- **Compose Multiplatform against the app's Compose.** The app uses AndroidX Compose (BOM `2026.09.00`) and Kotlin 2.2.21. A Compose Multiplatform release must match both, or the app moves. The spike, lor-llmf, finds out first.
- **The split touches every home file,** so it waits until no Panels branch is open on `home/` (`lor-f3vn`, `lor-sbth`, `lor-clgw`), and is done in one go. `HomeScreen.kt` (1,612 lines) holds 17 uses of a `Context` to separate out.
- **The wasm build's cost:** bundle size and first load in the dashboard, Gradle time on boat for each deploy, and whether the built bundle is committed or fetched from boat at deploy. The spike measures and the first increment that ships it decides.
- **Text.** The same layout code draws text through a different engine in the browser (Skia). The 2 px criterion assumes glyphs differ only inside their boxes; the spike checks it.
- **The Fold's two wallpapers.** One UI can set the cover and the inner screen apart; whether `WallpaperManager` gives both is untested.
- **Reading the wallpaper** needs All files access on Android 13 and later. The app already declares it for WhatsApp backups (`collect/WhatsAppBackup.kt`); if it isn't granted on the phone, that is Sumeet's to grant.
