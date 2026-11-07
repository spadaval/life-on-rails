---
status: draft
last_changed: "2026-11-06: the Fold and widgets moved to the Atelier mission lor-uj0r, Home as a grid of panels (Sumeet's direction); 2026-11-05: created from Sumeet's direction (the T3 Launcher concept is proven; make it a nice launcher)"
---

# Why

The T3 Launcher mission ([../t3-launcher/strategy.md](../t3-launcher/strategy.md)) proved the concept: LoR's app is the default home on the Fold, the agent page and quick send work, and Sumeet uses it. As an ordinary launcher it is thin. Nothing answers a long press, so the only way to change home is to edit `layout.json` over adb. The drawer is one alphabetical waterfall of icons. Empty, loading and error states are blank or raw. There is no customisation, motion or widget hosting. This mission makes it a launcher he'd pick on its merits.

Source: this thread, 2026-11-05 ("Let's move to making the launcher actually nice").

# Outcome

LoR's home is a launcher Sumeet calls nice to live in:

- **Home is changed on the phone.** A long press on an app gives its menu (App info, Uninstall, Hide, the app's shortcuts, add to or remove from home and dock). Edit mode moves, groups and removes items by drag, and every change can be undone. Each change is written to `layout.json`, so the file stays the one source of truth.
- **The drawer is organised.** Apps sit in sections (recent, frequent, categories sorted on the phone) instead of one A–Z list, with A–Z and most-used views a tap away, hidden apps out of sight, and the work profile apart.
- **Search finds more than app names.** It finds typos and initials, apps' shortcuts and conversations, contacts, settings pages, and quick answers, and hands off to the web or quick send when nothing matches.
- **No screen is blank or raw.** Loading, broken layout, empty home, unpaired T3, unreachable LoR, nothing needing him, no search results and first run each have a designed state.
- **It looks and feels finished.** Wallpaper-aware contrast, icon packs and themed icons, grid and label settings, the "Elite: Mostly Harmless" look from `DESIGN.md`, notification dots, haptics, a drawer that follows the finger, and launch animations from the icon.
- **The Fold and widgets** moved on 2026-11-06 to the mission lor-uj0r, Home as a grid of panels ([its strategy](../launcher-panels/strategy.md)), which replaces Increments 8 and 9 here.

Worth shipping on its own: the long-press menu, hidden apps and the organised drawer, before any of the rest.

# Success Criteria

- `no-adb`: every field of `layout.json` that says what home holds or how it looks can be changed on the phone, without adb or an agent: home and its pages, the dock, folders (name, apps, order, cover app), hidden apps, drawer sections and look settings (widgets and panels are lor-uj0r's). Left out: `version`; `active`, `layouts` and `expires`, which T3 Launcher Increment 10 gives a phone UI; a folder's or section's `reason`, which agents write; and `pinned`, which hand edits set and nothing edits directly. Shown at close on the emulator, one change per field, reading the file after each.
- `nice`: Sumeet says it's nice to live in, or names what isn't. A named human judgment, asked for at close on the build he is running then; how long he has run it is his to say, and the mission does not wait for a trial.

# Shape

- **The same app and the same rules as T3 Launcher.** LoR's Android app (`android/`, package `com.spadaval.lor`), Kotlin and Compose. D1 still holds: before building a launcher basic, read how Lawnchair, Kvaesitso and Neo Launcher do it, borrow what fits, and name the source and licence in the file.
- **`layout.json` is the one source of truth for what home is.** Phone edits, agent edits (T3 Launcher Increment 7) and hand edits all go through it. New fields are optional additions to schema version 1, as 3.2's rules allow. Per-device look settings (icon pack, grid sizes, label size) live in the file too, so an agent can change them and Increment 7's sync carries them. A hand edit to a folder sets its `pinned` flag.
- **The phone sorts the drawer on its own.** Categories come from Android's `ApplicationInfo.category` plus a small table in the app, and recent and frequent from what the phone records. The agent-written folders and reasons of T3 Launcher Increment 9 refine this later; nothing here waits for them.
- **Native and instant wins.** Home must answer at once. A feature that costs home-press or drawer-open latency is cut or deferred.
- **Calm.** Motion is one-shot and follows the finger. Nothing loops or pulses (LoR's rule, as in T3 Launcher).

# Scope

In: home, the dock, folders, the drawer, search, the agent page's empty and error states, the first run, the launcher's own settings, icons, theming, motion, widget hosting, Fold layouts, and the robustness items deferred from T3 Launcher's reviews.

Out: Live Updates (T3 Launcher Increment 6). Also out, moved from T3 Launcher to a later mission of their own when it closed (2026-11-05, its plan's "Handed on"): the agent-edited layout repo and its approval flow (its Increment 7), LoR people and T3 threads in search (8), agent-sorted folders with reasons (9), named and temporary layouts (10). Checks only the real Fold can answer go to a later quality mission, listed there too.

Out entirely: generative widgets, app groups in split view, and the phone as an agent output (later missions, as T3 Launcher says); root; replacing Recents or gesture navigation; LoR server changes beyond new read endpoints.

Environment: the emulator (`T3_Phone_API35`, `T3_Fold_API35`) for everything; the real Fold only when Sumeet says so. Builds on boat (`android/boat-build.sh`), never Gradle on buildbot.

# Authority

Without asking, the orchestrator may change increments, their order and their issues, and make choices inside an increment (libraries, schema field names, wording, the category table). It records each choice under its item in [plan.md](plan.md).

These come back to Sumeet:
- Installing anything on his phone.
- A permission that needs an adb grant or that he must grant in Settings (notification access for dots, an accessibility service for double-tap to lock, contacts).
- A change to `layout.json` that changes an existing field's meaning (a version bump).
- Adding to or dropping from the Outcome.

# Risks and Unknowns

- **Drag and drop in Compose is the largest piece**, with no Launcher3 to lean on. Kvaesitso's edit mode is the first thing to read.
- **Phone edits and agent edits can collide** once T3 Launcher Increment 7 lands. This mission writes the file atomically and rereads before each write; merging belongs to Increment 7.
- **Widget hosting on One UI** may behave differently from the emulator. Third-party widgets come late for this reason.
- **Icon packs and themed icons** vary a lot between packs; the emulator has none installed, so a test pack must be sideloaded.
