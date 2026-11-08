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
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Folding keeps your place: no recreation, no reloads"
updated_at: "2026-11-08T00:01:24.891731007+00:00"
---

## Goal

Folding keeps your place. A fold, an unfold, a dark-mode switch and a font-size change are handled without recreating `HomeActivity`, so open folders, search text, scroll and edit mode stay, and the app list and icons aren't loaded again. A fold or unfold lands on the cell lor-j87u's rule names; after a screen-off, home starts at the start of the screen it comes back on. The robustness items carried from Launcher polish's Increment 8 that a recreated or recomposed home exposes are fixed with it.

## Done when

On `T3_Fold_API35` against the test server, unless a line says otherwise:
- [x] `HomeActivity`'s `android:configChanges` adds `density|uiMode|fontScale` to today's list (decision note on lor-ph30). `HomeActivity` logs one `LorHome` line in `onCreate` and one per `onConfigurationChanged`, naming what changed. Over a fold, an unfold, `cmd uimode night yes` and `no`, and `settings put system font_scale 1.3` and back to 1.0, logcat shows one configuration line for each and no `onCreate` after the first.
- [x] Icons live in one process-scoped cache in `HomeStore`, keyed by app, icon style and size in px, used by home and Home settings alike. The size is computed from the current density each time an icon is drawn, not once in `onCreate`, and each `LorIcons` line names the app and the size in px. The app list logs one line per load. After one fold and unfold to warm the cache, a second fold and unfold, the dark-mode switch and the font change load the app list 0 times and draw 0 icons (lines counted before and after). The handoff names each screen's density (`wm density` folded and unfolded) and shows, from the `LorIcons` lines after the first fold and the first unfold, that each screen's icons were drawn at its own density's size (one set of lines if the two densities are equal, and the receipt says so).
- [x] State survives each of a fold, an unfold, the dark-mode switch and the font change, checked one at a time with a screenshot before and after: the drawer in A–Z scrolled to M; the drawer with `ma` in the search field; a folder open; edit mode on; the agent page scrolled down.
- [x] Where a fold or unfold lands, by `LorGrid` lines: from page 3 folded, an unfold shows the inner screen holding page 3; from home's pair after a tap on page 2's empty space, a fold shows page 2; from home's pair reached by a swipe and not touched, a fold shows home (the left half); from the agent page at full width, a fold shows the agent page.
- [x] After a screen-off, home is at the start of the screen it comes back on, with everything a home press closes closed: unfolded on pages 3 and 4 with a folder open, `input keyevent SLEEP`, a fold, `input keyevent WAKEUP` shows the `narrow` start with no folder; folded on page 3, sleep, an unfold, wake shows the inner screen holding the `wide` start. A fold or unfold on its own, with the screen on, never goes to `start`.
- [x] F4: a card action's call (approve, answer) runs in a process scope (`LorApp`'s), so leaving the agent page, by a move two cells away or a recreation, doesn't cancel it. A JVM test runs a call whose starting scope is cancelled mid-call and shows the call completes.
- [x] The icon-cache race: a JVM test shows an icon whose draw began before its package was evicted (a package change) is not put in the cache, so the next draw is fresh.
- [x] 4's review item (a): drag sources and zones store only their bounds, and the item under the finger is looked up from the placed layout at pickup. A JVM test of the lookup, and on the emulator an unpinned folder dragged away and back, released, then dragged again, shows that folder under the finger and no "Home changed" refusal.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

The manifest's `HomeActivity` entry, `HomeActivity.kt`, `HomeModel.kt`, `HomeStore.kt`, `Icons.kt`, `HomeSettingsActivity.kt` (its icons), `GridHost.kt` (the touched half, fold and unfold), `Drag.kt` and the drop and source code in `HomeScreen.kt`, `agent/AgentModel.kt`. Not the rest of Launcher polish's carried items (lor-kxxz).

## Context

- The fold and unfold rules trigger on a change in pairing (the smallest width crossing 600 dp), not on the `start` screen class, which rotating a phone also changes; a phone turned to landscape is neither a fold nor an unfold.
- lor-j87u holds the rules for where a fold and an unfold land; this item tracks the touched half (a press anywhere on a half makes it the focus, with no motion) and applies the rules when the screen class changes.
- `HomeUi` (search text, the open folder, edit mode, the grid's place) is a field of `HomeActivity`, and `AgentModel` is remembered by `AgentPage`: handled in place, both survive as they are; nothing new needs `rememberSaveable`. That holds while each panel keeps its composition key across screen classes (lor-el51's Context).
- A screen-off is `Intent.ACTION_SCREEN_OFF`, registered at runtime by `HomeActivity`; going to the start waits for the screen to come on, when the screen class is known.
- Launcher polish's plan, Increment 8, has the carried items' history; F4 is from T3 Launcher 3.3, the icon cache and its race from 3.2.

## Notes

- 2026-11-07 [decision] The touched half is recorded on the grid finger's down, before any of its early returns: GridPlace.pressed(x, width) makes the half under the press the focus (cell and focus change, nothing moves), split at the middle as the pair is drawn. A press while a move settles, on a full screen, or off the inner screen touches nothing.
  Reason: One handler on home's root sees every press in the Initial pass, including taps on apps, the drawer and empty space, which is what 'a press anywhere on a half' asks; a per-cell pointer modifier would add a node to each cell and miss presses the drag handler consumes. A press during a move takes the move, so it isn't on a settled half.
  Rejected: A pointer modifier on each cell in GridCells
- 2026-11-07 [decision] A screen-off sets a flag; the next ACTION_SCREEN_ON with the flag set runs ui.homePressed(animate = false). Both are registered at runtime in HomeActivity.onCreate and unregistered in onDestroy.
  Reason: The decision note on lor-ph30 asks for the start of the screen home comes back on. On the emulator a posture change turns the screen on 40-100 ms after the configuration change, so the start read then is the new screen's. Closing everything at screen-off as well would only repeat the same press.
  Rejected: Going to start at ACTION_SCREEN_OFF: the screen class is the old one then
- 2026-11-07 [decision] The process icon cache is an IconCache keyed by app, IconStyle and size in px, least recently used out past 48 MB, replacing the per-activity LruCache of 300 that a style change emptied. The race guard is a per-package eviction count: a draw takes it as a ticket when it starts, and put refuses when the package was evicted since. AppIcon reads the draw size from LocalIconDrawSize (56 dp on home, 40 dp in Home settings) times LocalDensity at draw time.
  Reason: With the style in the key, a style change needs no clearing and home and Home settings, whose styles agree, share entries. Sizing by bytes holds every app at three sizes (two densities and Home settings') at about 85 KB each. A per-package count keeps another package's change from discarding a draw. IconCache has no Android types, so IconCacheTest reaches it.
- 2026-11-07 [decision] 4's review item (a): sources and drop zones hold their bounds and a Slot (the item's itemKeys key in its block, the dock, or its folder; a drawer app by HomeApp.key). A pickup looks the slot up in the file home draws then (Layout.find) and resolves the app or folder from the app list; each move resolves the zones (DropMark) against the held item's working copy (Layout.zones). Tiles' composition keys are the same file keys.
  Reason: A slot names what a tile is, so a tile whose item changed in place (a folder pinned by a move away and back) is picked up as it is now, and an apps block's space decides InApps or NewPanel from the layout drawn, not the one when it was laid out. Resolving zones per move costs a key list per block, under a millisecond for 24 items.
  Rejected: rememberUpdatedState inside each onGloballyPositioned callback: keeps a snapshot, only fresher, and the item says no snapshot
- 2026-11-07 [decision] F4: card calls run in LorApp.scope (SupervisorJob on Main) through one CardCalls held for the process (LorApp.cardCalls), which holds busy, failures and answered. Every AgentModel reads it, so a page built again (a move two cells away and back, a recreation) shows a call still under way as busy and won't send it twice. Corrected after review finding lor-0fbs, which found the first version's per-model CardCalls let a rebuilt page send again.
  Reason: The item asks that leaving the page not cancel the call, as quick send's send already runs. AgentPage keeps its AgentModel only while the page is composed, so state held per model is lost on a move away and back while the call can run for up to 30 s; held for the process, the once-at-a-time rule holds across pages.
  Rejected: A CardCalls per AgentModel (the first version): a rebuilt page starts with empty busy and answered
- 2026-11-07 [decision] The agent page's fold keeps the place read last: FormScroll maps the full form's two columns (Today, the queue) to the single column and back: the queue scrolled q means Today's height plus q in the single column, else Today's own scroll; a column at its end stays at the other form's end. A third ScrollState holds the single column, so an unfold gives both columns back as they were. This is in AgentPage.kt, outside the item's listed Scope (lor-ph30's Scope holds it).
  Reason: The emulator check of the Done when line 'the agent page scrolled down' failed on 309485f: the single column took Today's scroll, and Today rarely scrolls, so a scrolled queue came back at the top after a fold. Text wraps differently in the two forms, so the place is kept approximately; the ends are exact.
  Rejected: Leave lor-el51's rule (the single column takes the left column's scroll): fails the line whenever the queue is what was scrolled
- 2026-11-07 [handoff] Implemented on lor-ph30 (code 309485f, 677524c); see ## Report. Review: none yet. Receipts lor-ax8p, lor-v9wu, lor-f0c5, lor-b6x9, lor-ultr.

## Report

Folding keeps your place: on the Fold emulator a fold, an unfold, dark mode and a font change are handled in place, with one `HomeActivity` for the whole session, no app-list reload and no icon drawn again, and the drawer's A–Z place, search text, an open folder, edit mode and the agent page's scroll all survive (receipts lor-ax8p, lor-v9wu). Folds land where lor-j87u's rules say, including the half last touched (lor-f0c5), and after a screen-off home comes back at the start of the screen it wakes on (lor-b6x9). Every Done when line holds. The pure parts are tested and pass in boat's `assembleDebug testDebugUnitTest` on 677524c (276 tests, 0 failures): `GridPlaceTest.aPressOnAHalfIsWhereAFoldLands`, `aPairReachedBySwipeAndUntouchedFoldsToItsLeft` and `aPressTouchesNothingOffAPairOrDuringAMove` (the touched half); `CardCallsTest.aCallCompletesAfterThePageThatStartedItIsGone` (F4); `IconCacheTest.anIconWhoseDrawBeganBeforeItsPackageChangedIsNotKept` and the size, eviction and byte-limit tests (the process cache and its race); `DragTest.aFolderDraggedAwayAndBackIsPickedUpAsItIsNow`, `slotsFollowTheirItemAndGoWithIt` and `marksResolveToTheZonesOfTheLayoutDrawn` (4's review item (a), with the emulator drag in lor-ultr); `AgentStatesTest.aFoldKeepsThePlaceReadLastAndAnUnfoldGivesItBack`. Screenshots and logs are in `/tmp/claude-1000/ozj2-emu/`.

Densities: `wm density` is 390 folded and 390 unfolded, so both screens draw icons at the same size, 137 px (56 dp); the LorIcons lines after the first fold and unfold are one set, and receipt lor-ax8p says so.

### Deviations

- `agent/AgentPage.kt` changed, outside this item's listed Scope (lor-ph30's holds it). The first emulator run failed the line "the agent page scrolled down": folded, the single column took Today's scroll and showed the top of the queue. `FormScroll` now keeps the place read last across a fold and gives both columns back on unfold (decision note).
- Folds were made with agent-device's `fold closed|open` (the emulator's posture), not `cmd device_state state`; a stale `cmd device_state` override blocks posture changes, so it was reset first. Dark mode and font scale used the commands the lines name. The two multi-step drags used `input motionevent` (agent-device's drag has no holds on the way).

### Decisions

Six decision notes above: where the touched half is recorded, when a screen-off goes to start, the icon cache's shape and race guard, slots for drag sources and zones, F4's scope, and the agent page's scroll across forms.

### Surprises

- On the emulator a posture change wakes the screen itself, 40 to 100 ms after the configuration change, so the `WAKEUP` key was redundant and the start was always read after the new configuration. Whether One UI orders these the same way is for the real-Fold item lor-znfb.
- A home press from agent-device while home is at rest opens quick send (as designed), which swallowed the first drag after a load.

### Open items

- "Used by home and Home settings alike" is shown by the code (`HomeSettingsActivity` draws from `store.icons` at 40 dp); Home settings had no hidden apps on the emulator, so it drew no icons there.
- Several tiles showing one app at once (the dock drawn in each composed cell) each start a draw before the first lands, so the first draw of a size logs about 34 lines for 22 apps. Later draws hit the cache. Not deduplicated; a follow-up if it matters.
- The agent page's place across a fold is kept by scroll offset, so it is close rather than exact in the middle of the queue (text wraps differently in the two forms); the ends are exact.

### Fixes after review

- lor-0fbs (fix_now), fixed in dd1eb62: card calls lived in a CardCalls per AgentModel, which AgentPage keeps only while the page is composed, so a move two cells away and back built an empty one and a second tap could send again. One CardCalls is now held for the process (`LorApp.cardCalls`) and every AgentModel reads it; the F4 decision note is corrected. `CardCallsTest.aPageBuiltAgainSeesTheCallUnderWayAndDoesNotSendItTwice` drives the shared instance (busy seen, a second act sends nothing, answered after). It tests CardCalls itself, since AgentModel needs a Context; that the model takes LorApp's instance is shown by the code.
- Merge of main (0c1d07b, lor-gnk0, lor-v692, lor-odtk): lor-gnk0's full-screen FolderPanel is ported to Slot, DropMark and `Source(b, slot, 1)`, keeping its FolderExit rule and strip; DragTest keeps both sides' tests; lor-v692's `stripEmptyPanels(h.original.current)` in `dropHeld` is kept. Boat's `assembleDebug testDebugUnitTest` on the merged tree: 297 tests, 0 failures. On T3_Phone_API35 (receipt lor-s96u): apps dragged out of a folder above and below land on home, the last app out of a two-app folder takes the folder away, a drag onto a folder adds to it, and the unpinned folder dragged away and back is picked up again as itself with no refusal.

