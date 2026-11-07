---
created_at: "2026-11-07T22:02:21.566426574+00:00"
id: "lor-ozj2"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-el51"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Folding keeps your place: no recreation, no reloads"
updated_at: "2026-11-07T22:02:21.566426574+00:00"
---

## Goal

Folding keeps your place. A fold, an unfold, a dark-mode switch and a font-size change are handled without recreating `HomeActivity`, so open folders, search text, scroll and edit mode stay, and the app list and icons aren't loaded again. A fold or unfold lands on the cell lor-j87u's rule names; after a screen-off, home starts at the start of the screen it comes back on. The robustness items carried from Launcher polish's Increment 8 that a recreated or recomposed home exposes are fixed with it.

## Done when

On `T3_Fold_API35` against the test server, unless a line says otherwise:
- [ ] `HomeActivity`'s `android:configChanges` adds `density|uiMode|fontScale` to today's list (decision note on lor-ph30). `HomeActivity` logs one `LorHome` line in `onCreate` and one per `onConfigurationChanged`, naming what changed. Over a fold, an unfold, `cmd uimode night yes` and `no`, and `settings put system font_scale 1.3` and back to 1.0, logcat shows one configuration line for each and no `onCreate` after the first.
- [ ] Icons live in one process-scoped cache in `HomeStore`, keyed by app, icon style and size in px, used by home and Home settings alike, and logging one `LorIcons` line per icon drawn. The app list logs one line per load. After one fold and unfold to warm the cache, a second fold and unfold, the dark-mode switch and the font change load the app list 0 times and draw 0 icons (lines counted before and after).
- [ ] State survives each of a fold, an unfold, the dark-mode switch and the font change, checked one at a time with a screenshot before and after: the drawer in A–Z scrolled to M; the drawer with `ma` in the search field; a folder open; edit mode on; the agent page scrolled down.
- [ ] Where a fold or unfold lands, by `LorGrid` lines: from page 3 folded, an unfold shows the inner screen holding page 3; from home's pair after a tap on page 2's empty space, a fold shows page 2; from home's pair reached by a swipe and not touched, a fold shows home (the left half); from the agent page at full width, a fold shows the agent page.
- [ ] After a screen-off, home is at the start of the screen it comes back on, with everything a home press closes closed: unfolded on pages 3 and 4 with a folder open, `input keyevent SLEEP`, a fold, `input keyevent WAKEUP` shows the `narrow` start with no folder; folded on page 3, sleep, an unfold, wake shows the inner screen holding the `wide` start. A fold or unfold on its own, with the screen on, never goes to `start`.
- [ ] F4: a card action's call (approve, answer) runs in a process scope (`LorApp`'s), so leaving the agent page, by a move two cells away or a recreation, doesn't cancel it. A JVM test runs a call whose starting scope is cancelled mid-call and shows the call completes.
- [ ] The icon-cache race: a JVM test shows an icon whose draw began before its package was evicted (a package change) is not put in the cache, so the next draw is fresh.
- [ ] 4's review item (a): drag sources and zones store only their bounds, and the item under the finger is looked up from the placed layout at pickup. A JVM test of the lookup, and on the emulator an unpinned folder dragged away and back, released, then dragged again, shows that folder under the finger and no "Home changed" refusal.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

The manifest's `HomeActivity` entry, `HomeActivity.kt`, `HomeModel.kt`, `HomeStore.kt`, `Icons.kt`, `HomeSettingsActivity.kt` (its icons), `GridHost.kt` (the touched half, fold and unfold), `Drag.kt` and the drop and source code in `HomeScreen.kt`, `agent/AgentModel.kt`. Not the rest of Launcher polish's carried items (lor-kxxz).

## Context

- lor-j87u holds the rules for where a fold and an unfold land; this item tracks the touched half (a press anywhere on a half makes it the focus, with no motion) and applies the rules when the screen class changes.
- `HomeUi` (search text, the open folder, edit mode, the grid's place) is a field of `HomeActivity`, and `AgentModel` is remembered by `AgentPage`: handled in place, both survive as they are; nothing needs `rememberSaveable`.
- A screen-off is `Intent.ACTION_SCREEN_OFF`, registered at runtime by `HomeActivity`; going to the start waits for the screen to come on, when the screen class is known.
- Launcher polish's plan, Increment 8, has the carried items' history; F4 is from T3 Launcher 3.3, the icon cache and its race from 3.2.
