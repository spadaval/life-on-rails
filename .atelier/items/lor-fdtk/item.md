---
schema: atelier.item
schema_version: 3
id: lor-fdtk
title: 'The Panes direction on the phone in the lab: every required screen, state and interaction'
status: in_review
mission: lor-2803
labels:
- launcher
- design
priority: medium
blocked_by:
- lor-j1fe
implementers:
- thread:delegated-task:command%3Amcp%3A9fa0a269-e48d-4704-afbf-af6a22c90c73%3Adelegate-task%3A7e37ad6d-cf9d-47bf-bd77-533fa0405f32
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.478110201Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.885709623Z
---

## Goal

Sumeet can use the Panes direction's phone in the lab: pick it in the Version dropdown and do the brief's eleven phone interactions by touch, on the cover screen and unfolded.

## Done when

- [x] The lab has a `panes` version, drawn in Compose in `homecore`'s `webMain` (where the concepts live; see the concepts README) from the same fixtures as `current`, built as `docs/design/lab/brief.md` describes it under "### 3. Panes" and with the brief's "Common ground for every concept", using the shared pieces lor-5fk6 made (the navigation shell, the sheet, the fixture-backed store).
- [x] It draws the brief's phone screens: home, the drawer and search with results and with the keyboard up, the agent page and the LoR panel on the cover and the inner screen with both kinds of question, one menu and one sheet, a settings page, the timer, and a thread on the inner screen read and being replied to.
- [x] It draws the brief's required states (nothing wants you, an empty day, a failed phone sync seen from home or the agent page, T3 disconnected, LoR unreachable with stale data, twelve cards waiting, font scale 1.3), each a named state opened by a link, and the Report says where each appears.
- [x] The brief's eleven phone interactions work by touch and by `scripts/lab/gesture.mjs`, with recordings of the drawer swipe and one drag; fold and unfold keep the open thread, a half-written answer and the scroll position.
- [x] `shots/lab.json` gains this version's states on a dark, a pale and a mixed wallpaper, and `scripts/lab/publish.sh` puts the build under `/lab/phone/`.
- [x] The direction's "Its risk" from the brief is answered in the Report after using it: whether it showed up, with a screenshot.

## Scope

`android/homecore/src/webMain/.../concepts/` (this direction's package and its registration) and `shots/lab.json`. The app's own screens are not changed.

## Context

- Use the `refactoring-ui` skill (`~/.agents/skills/refactoring-ui/`) and the vendored impeccable skill's Operate guidance. Sumeet's aim, 2026-11-17: the app, launcher and dashboard should "feel designed and polished", "make common things easy and uncommon things accessible" and be "a delight to use"; the Elite theme is kept "only in spirit".
- Concept code is rough on purpose. The look is not: this is what Sumeet chooses from, so finish the visual design.
- lor-5fk6 builds Queue, the first direction, and the shared pieces. The wasm build here is one at a time (decision lor-0v68), so the lead runs the phone directions one after another.
- Decisions every concept follows are in the brief under "Decisions already made".

## Report

Panes' phone is in the lab as version `panes`. All nineteen of its gesture scripts pass on boat's full bundle (every concept in): they cover the brief's eleven phone interactions, the pager, search unfolded, a question that takes several answers, a stale LoR refusing every write, and the direction's risk. Boat's default build passed with 940 tests and 0 failures, and its bundle draws `#queue/demo`, `#timeline/demo` and `#panes/demo` side by side (`~/.local/state/lor-evidence/lor-fdtk/boat-bundle-queue-timeline-panes.png`).

### Read this first

- **One change outside the Scope, in its own commit (c28ae824).** The dashboard's Panes sets everything in Hanken Grotesk, and the phone lab only had Saira, so the two wouldn't have read as one product. The lab page now loads Hanken Grotesk (OFL; the prototype's woff2 converted to TTF in `wasmJsMain/resources/fonts/`) into `ConceptFonts` in `lab/LabConcepts.kt`. The kit's `Glyph` also gained `ChevronUp` and `Flag`. Both are additive; Queue and Timeline are unchanged.
- **A question opens at full height on the cover, not half.** The brief's half-height sheet left a question's options below the fold (three described options and "Something else" didn't fit beside the transcript). So Answer, and a state that opens a question, rest the sheet at full; a tap on any other row rests it at half. This is the risk showing up in practice; see below.
- **Lab bug found, not fixed:** a horizontal touch swipe that the first or last page doesn't consume (a swipe right on the agent page) makes Chrome navigate back out of the lab, because `index.html` sets no `overscroll-behavior`. It affects every version. My scripts avoid it; it should be filed against the lab page.

### Panes on the phone

Three pages, left to right: the agent page, home, Today (the LoR panel). Every page has the same quiet switcher at the top (Agent with its card count in cyan, Home, Today with what's due in amber) and the same dock at the thumb: the one field that finds, adds and asks, and the timer chip, which shows the running readout with Switch and Stop, or Start.

- **Home**: icons near the thumb and the dock under them. At the top, one glance pane of glass: the time and date, the weather, the next event, and, in red with its fix, whatever makes the numbers untrue (a failed sync, LoR not answering, T3 away; all of them, in that order). Swipe up opens the drawer, swipe down the shade's stand-in, double tap quick send.
- **The agent page**: a list pane (AGENT, its count, "T3 connected · 4 threads", ⋯) leading with T3's cards, each with its common action on the row: Approve and Deny, Answer, Dismiss. Approve and Deny show "Approving…", a bar and Cancel for three seconds and never an Undo. Then every thread, running ones first with a cyan dot. Unfolded, the open card or thread is the right pane: its title at 28, its action, the transcript newest-last, and the reply field. With nothing open, the right pane is recessed and says what the agent is on.
- **Today**: the timer as the first row (an amber card with readout, Switch and Stop while running), then Needs you (LoR's flags with Done and Seen on the row), Due (a check to tick, late in red), and People past their cadence. A row opens to its detail: the main action, the due date as a segmented control, "Start a timer on this", and the uncommon things (Open in LoR, Waiting on someone, Delete) under a collapsed More.
- **The cover**: the same panes become the list with a sheet. It rises to half height with no scrim, so the list above stays live and another row opens into the same sheet; the chevron or a drag takes it to full, and the list behind steps back (scaled, blurred, dimmed). A tap on the stepped-back list brings it to half; ×, Back or a drag below half closes it. On opening, a row that would sit under the sheet scrolls up above it.
- **Search and the drawer**: every app as a grid; the field at the foot, above the keyboard. Typing turns it into search, with results rising from the field so the best app is nearest the thumb, then what the field understood (add a task when it sees a day or #goal, else ask the agent), people, tasks and settings. Unfolded, search is two panes: results left, and on the right the chosen app (the first, until another is picked) with Open, its recent conversations and its shortcuts. On the cover an app's › opens that pane as a sheet.
- **Settings** is shaped like every page: sections (sync, connections, home, sensors) each showing its state ("Failed" in red), and the chosen section beside them unfolded. On the cover the section slides over the list as a second pane and the list behind blurs; "‹ Settings" or Back returns.
- **The look**: the dashboard's Panes tokens at phone size (`panes/PanesLook.kt`): Hanken Grotesk at 12, 14, 15, 18 and 28, each with one job, condensed caps only for pane titles; panes as layers of the ground at rising lightness over the blurred wallpaper with the dashboard's two glows; amber for the selected row (wash and edge), the open item and the running timer; cyan for the agent; one red. Depth is ground, pane, raised row, sheet, floating menu. Every control has pressed, hover and disabled states; motion is 150 to 240 ms. Checked against refactoring-ui's order (hierarchy, spacing, type, colour, depth) on each screen.

### Where each required state appears (`http://127.0.0.1:3860/#` plus the link; the agent page is `{"base":"<state>","where":{"cell":[0,0]}}`, Today `[3,0]`)

- **Nothing wants you** (`panes/quiet`): the switcher shows no counts; the agent page and Today each say "Nothing waits on you" in one calm row. If T3 or LoR can't be read, the row says "Can't tell yet" instead.
- **An empty day at 09:00** (`panes/empty-day`): Today at 9:00, no timer, nothing flagged, the tasks due; the day's events in the overview.
- **A failed phone sync** (`panes/sync-failed`): a red "Phone sync failed · Fix" line in home's glance, and a note with Fix at the top of both the agent page and Today. Each opens settings at the sync, with Allow per missing permission and Sync now.
- **T3 disconnected** (`panes/agent-t3-stale`): the agent page's header says "T3 reconnecting · as of 40 min ago", a red note explains that answers wait, Approve and Deny are disabled, home's glance has "T3 is reconnecting", and quick send fails with the reason.
- **LoR unreachable, stale** (`panes/stale`): home's glance says "LoR isn't answering · Today is as of 13:00"; Today keeps its last list under a red note, with Done, Seen, ticks, the timer and due dates disabled, and every write path (menus, search's Add) refusing at the source with a toast rather than an Undo. `panes/failed` (no data) says "Can't reach LoR" with Settings.
- **Twelve cards waiting** (`panes/overfull`): the switcher reads Agent 12; the agent page lists all twelve, each with its row actions, scrolling in its own pane.
- **Larger text** (`panes/large-text`): everything at 1.3; rows wrap and grow, nothing clips.

**The screens:** home (`panes/demo`); the drawer (`panes/drawer-all-scrolled`); search with results and the keyboard up (`panes/search-mixed`, both screens); the agent page and Today on both screens; a question with descriptions and free text (`{"base":"agent-waiting","where":{"open":"THREAD","target":"t-question"}}`) and one taking several answers (the same over a one-off with `"multiSelect":true`, in `shots/lab.json`); the menu (`panes/app-menu`, and every row's ⋯); the quick-send sheet; settings (`{"base":"sync-failed","where":{"open":"APP_TAB","target":"settings"}}`, both screens); the timer, idle and running; and a thread on the inner screen (`panes/thread-long/inner`). `shots/lab.json` gains 98 rows: 78 over the dark wallpaper (every state on both screens, the agent page and Today in the states that change them, the sheets and questions), 9 pale and 9 mixed. All 98 were captured one page at a time, without problems, to `~/.local/state/lor-evidence/lor-fdtk/shots/`.

### Interactions, by script

In `scripts/lab/gestures/panes/`, each run as `node scripts/lab/run.mjs <script> --version panes --state <s> --out <dir>`, each checking by anchors. All nineteen pass on boat's bundle; recordings, strips and final frames are in `~/.local/state/lor-evidence/lor-fdtk/gestures/`.

1. `drawer` (demo): swipe up raises the drawer with the finger; a swipe down on its header closes it. **Recorded.**
2. `search` (demo): the dock's field opens search with the keyboard up; "stra" puts Strava just above the field; a tap opens it. `search-inner` (demo, inner): "wha" chooses WhatsApp, its pane shows beside the results, and Maya Okafor's conversation opens.
3. `approve` (agent-waiting): Approve on the row, Cancel for three seconds with no Undo, then the card clears and the question moves up.
4. `answer` (agent-waiting): Answer opens the question with its described options; "Something else", free words, Send, the cancel window, cleared. `answer-several`: two options ticked unfolded, then "Send 2 answers".
5. `timer` (demo): Start at the thumb, a label, running in the dock; Switch to No label; Stop with Undo.
6. `tick` (demo): Today from the switcher, a task ticked on its row, Undo.
7. `move` (demo): a long press on Camera opens its menu, which stays; the drag lifts it, the menu goes, and it lands at T3 Code's place with Undo. **The recorded drag.**
8. `fold` (thread-long): the thread in its sheet on the cover, scrolled back, 12 characters of reply typed; unfolded, it is the right pane beside the list with the same draft and scroll; folded again, both kept.
9. `reply` (thread-long, inner): a reply sent beside the list joins the thread.
10. `quick-send` (demo) says it went; `quick-send-fail` (agent-t3-stale) says why not, keeps the words and offers Try again.
11. `sync` (sync-failed): home's glance line, Fix, settings at the sync stacked over its sections, Allow twice, Sync now, synced.

Also `pages` (the pager follows the finger and the switcher says where), `menu` (WhatsApp's long-press menu with its conversations stays, Back closes it), `sheet` (the half sheet, a second row opening into it, full, back to half by a tap on the list, closed by Back without leaving Today), `stale` and `risk`.

### Its risk: "nested scrolls and half-height sheets make Back and dismissal easy to get wrong"

The half of it that applies to a phone showed up, and was mostly contained. Screenshots: `~/.local/state/lor-evidence/lor-fdtk/risk-thread-half-sheet.png` (the approval's thread at half height over the agent list) and `risk-strip.png` (the `risk` run).

- **Nested scrolls, yes.** In the thread's sheet, a drag down on the transcript scrolls the transcript back (forty messages), not the sheet: the sheet only lowers from its handle, its × and chevron, or once the transcript is at its first message. That is the right priority for reading, but it means "drag the sheet down" works on some parts of the sheet and not others. The visible chevron and × are there so dismissal never depends on where the finger lands.
- **Half height, yes.** It suits a task, a flag or a person (the list stays live and another row opens into the same sheet), but not a question: its options didn't fit beside the transcript at half, so questions open at full. And with any sheet up on the cover, the dock is covered, so the timer chip and search are one dismissal away.
- **Back, no.** Each Back undoes one layer, front first: the keyboard, then the sheet, then a settings section; it never leaves the page or jumps to half. `sheet` and `risk` check this.
- **"A lot of chrome", unfolded, only a little.** At 875 dp the list pane (372 dp) and the detail fit well; at font scale 1.3 the list pane gets tight and titles wrap to two lines. The switcher and dock take about 150 dp of every page's height.

### Publishing and memory

- **boat:** `android/boat-build.sh` with its default tasks (the app, both unit-test sets, `:homecore:allTests`, the lab's production bundle with every concept) passed in 3 min 6 s at efc1d5f7: 940 tests, 0 failures. The VM is stopped again.
- **Scratch publish:** boat's bundle was put where `publish.sh` reads the phone lab, published with `LOR_LAB_DIR=~/.cache/lor-fdtk/lab-scratch`, then removed, so this checkout has no `build/dist`. Served through `labStatic` (lor-web's `/lab/` handler and its policy), `/lab/phone/#panes/demo` drew with Hanken Grotesk loaded (`published-panes-demo.png`). I did not publish to the real lab.
- **Local build:** `web-dev.sh --only panes`, first build 1 min 18 s, nine builds, scope peak 4,067 MB of 4,096 MB; stopped. `pnpm test` passes (206); `pnpm typecheck` not run (no TypeScript changed; lor-iurl).
