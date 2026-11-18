# Design concepts in the lab

Each direction in `docs/design/lab/brief.md` is one version of the launcher lab, next to `current`
in the Version dropdown. Queue (`queue/`) is the first and Timeline (`timeline/`, lor-j1fe) the
second. Panes and Context (lor-fdtk, lor-8aqh) come next and reuse the pieces in `kit/`.

The code lives in `webMain`, not `wasmJsMain`. The app never compiles either, so a concept never
ships. `webMain` is used because Kotlin 2.3.20 crashes on an incremental compile whose changed files
are all in `wasmJsMain`, and `android/web-dev.sh` then fails every build until it restarts. Edits
here take 14 to 16 s warm. After editing `lab/Main.kt` or another `wasmJsMain` file, restart
web-dev.sh.

## Adding a direction

1. Make a package beside `queue/`, with a `Version`:
   `val version = Version("timeline", "Concept: Timeline. …") { TimelinePhone(it) }`.
2. Add it to `concepts` in `android/homecore/build.gradle.kts`, as `"timeline" to "Timeline"` (the
   package's name and its object's). That one line is the registration: the build generates
   `Concepts.all` from the list, and the lab page hands it to `Lab.concepts` before anything reads
   the registry.
3. Open it: `android/web-dev.sh --only timeline`, then `http://127.0.0.1:3860/#timeline/demo`. Every named state
   works, including the brief's required ones: `quiet`, `empty-day`, `sync-failed`, `large-text`,
   `stale`, `failed`, `overfull` (twelve cards), `agent-waiting`, `agent-t3-stale`, `thread-long`.

A direction's root composable gets a `LabScene`: the fixture, the screen (cover or inner) and the
wallpaper. Draw the same fixture as `current` and read nothing else.

## Building one concept at a time

`android/web-dev.sh --only <concept>` compiles only that concept's package (the Gradle property
`lor.concepts`, a comma-separated list): the lab holds current, the kit and that concept, and the
others are left out of the source set and the Version dropdown. Use it while working on a concept.
With Queue alone in, a development session peaked at 4,085 MB of the scope's 4,096 MB; the cap is
decision lor-0v68's, so the build is made smaller instead. web-dev.sh's header has the numbers.

Without `--only` every concept is compiled, as boat's full bundle (`android/boat-build.sh`) is.
Each package must therefore compile on its own: a concept imports from `kit/` and the lab, never
from another concept. Copy what you want from another direction into your package.

A bundle built with `--only` holds one concept, so it is not the lab to publish under `/lab/phone/`.

## The kit

Each piece takes its look from a `KitLook` (`kit/Look.kt`) that the direction provides around its
tree: `CompositionLocalProvider(LocalKitLook provides MyLook.kit) { … }`. No piece carries a
direction's colours, so Timeline can use the sheet without looking like Queue.

| Piece | File | What it gives |
| --- | --- | --- |
| `ConceptStore` | `Store.kt` | The fixture as state, and every tap's effect: tick, Done, Seen, due dates, adding a task, the one timer (start, stop, switch), approvals and answers to T3, quick send, sync and permissions, moving home's icons. LoR's changes offer Undo. T3's go through `sendSoon`, which gives a few seconds to cancel and never shows Undo, as the brief requires. It also keeps what must survive a fold: `open` (the open item), `drafts`, and list positions (`list(name)`). |
| `ShellState`, `PageShell` | `Shell.kt` | Pages side by side that follow the finger (a pager), a drawer that rises with the finger on the home page, a swipe down on home kept for the shade (the brief's first decision), and a double tap on home. `Modifier.dragsDrawer` lets the drawer's header close it too. |
| `Sheet` | `Sheet.kt` | A bottom sheet with rest points (`detents`, fractions of the screen; Panes wants a half-height one), drag to dismiss by its handle or its list, Back, and a centred, narrower form on the inner screen. It resizes between rest points, so its foot stays on screen. |
| `Backdrop`, `BlurredWallpaper` | `Look.kt` | Wrap the pages in `Backdrop`: while a sheet is up they blur, and the wallpaper turns to its blurred copy. The lab page provides the wallpaper (`LocalWallpaper` in `lab/LabConcepts.kt`). |
| `Menu`, `MenuItem` | `Menu.kt` | A menu beside its anchor, kept on screen, closed by a tap outside or Back. |
| `ToastHost` | `Toast.kt` | The store's notice at the foot of the screen, with Undo when there is one. |
| `StandInKeyboard` | `Keyboard.kt` | A drawn keyboard (236 dp) under the focused field, so "with the keyboard up" can be judged. It types into a `TextFieldState`, as does the computer's keyboard. |
| `Icon`, `Glyph` | `Icons.kt` | Line icons drawn on a 24-unit grid, no icon font. |
| `AppIcon`, `FolderIcon` | `AppIcon.kt` | The same placeholder app icons for every direction, so icons don't tell concepts apart. |

## Rules the scripts depend on

- Keep screen state above anything that branches on the screen. Queue's `QueueUi` is remembered per
  fixture at the root, so a fold keeps the page, the sheet, the drafts and the scroll. The lab keys
  a version on its version, its state and Home presses, not on the screen.
- Name what a script touches with `Modifier.labAnchor("…")`, and write the interaction's script in
  `scripts/lab/gestures/<direction>/` as you build it. Queue's thirteen are the model:
  `node scripts/lab/run.mjs scripts/lab/gestures/queue/approve.mjs --version queue --state agent-waiting --out <dir>`.
  Two nodes may carry one name, as a sheet does while a pane takes its place; the name stays while
  either is on screen.
- `node scripts/lab/snap.mjs --out <dir> queue/demo 'queue/agent-waiting/inner/pale'` screenshots
  links one at a time, cropped to the phone, for a quick look while building.
