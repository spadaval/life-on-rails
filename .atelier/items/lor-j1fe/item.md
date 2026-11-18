---
schema: atelier.item
schema_version: 3
id: lor-j1fe
title: 'The Timeline direction on the phone in the lab: every required screen, state and interaction'
status: in_review
mission: lor-2803
labels:
- launcher
- design
priority: medium
blocked_by:
- lor-5fk6
implementers:
- thread:delegated-task:command%3Amcp%3A6d389a64-570b-463c-8fd4-a12609b788b5%3Adelegate-task%3Aa0cb5bac-d9df-477d-afee-5f6bd1dc133a
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.446560707Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.861504658Z
---

## Goal

Sumeet can use the Timeline direction's phone in the lab: pick it in the Version dropdown and do the brief's eleven phone interactions by touch, on the cover screen and unfolded.

## Done when

- [x] The lab has a `timeline` version, drawn in Compose in `homecore`'s `webMain` (not `wasmJsMain`: Kotlin 2.3.20's incremental crash, as the concepts README says) from the same fixtures as `current`, built as `docs/design/lab/brief.md` describes it under "### 2. Timeline" and with the brief's "Common ground for every concept", using the shared pieces lor-5fk6 made (the navigation shell, the sheet, the fixture-backed store).
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

Timeline's phone is in the lab as version `timeline`. All fifteen of its gesture scripts pass: they cover the brief's eleven phone interactions, the menu, fold from both screens, relabelling and planning. To make room for it, the lab's development build now compiles one concept at a time (`android/web-dev.sh --only timeline`). With Timeline in it fits under the 4 GB cap, but only at the edge. A fresh session peaked at 3,511 to 3,863 MB. My forty-edit working session reached 4,078 MB with its 512 MB of swap full, and nothing was killed. The cap is still Sumeet's call (lor-0v68). The next concept will fit the same way, because each one builds alone.

### Read this first

- **The route draws data the phone doesn't receive.** `/device/home` sends the day's events and the running timer, but not the blocks recorded before now, and Timeline's route needs them. The concept reads the demo set's afternoon from `timeline/DayModel.kt`, copied from the dashboard prototype's snapshot of the same day. Only states at the demo's 15:00 have blocks: the empty day at 09:00 has none, and a state where LoR never answered has nothing. If Sumeet picks Timeline, the server has to send the day's blocks to the phone. Every other part draws the fixture, as `current` does.
- **The memory switch is outside the Scope, as the lead asked.** It has its own commit, 0b31c602. `-Plor.concepts=<names>` leaves every other concept's package out of `webMain`'s source set. The registry (`Concepts.all`) is now generated from a `concepts` list in `android/homecore/build.gradle.kts`, so that list is now the one line that registers a direction. `Concepts.kt` is gone. Without `--only`, every concept compiles, as boat's bundle does. The rule this adds is in the concepts README: a concept may import only from `kit/` and the lab, never from another concept. Timeline's drawer, sheets, controls and detail started as copies of Queue's in its own package for that reason.
- **The header fix.** 0b31c602's header edit never matched, so its message claimed numbers the header didn't have. f6a43fa6 adds them.

### The loop's memory and speed (`--only timeline`)

| | First build | Edit to a concept file | Scope's peak |
| --- | --- | --- | --- |
| Before Timeline (current and the kit) | 79 s | 13 to 19 s | 3,732 MB after three edits (3,281 MB anon; Kotlin daemon 2,078 MB) |
| With Timeline, fresh session | 36 s | 13 to 18 s | 3,511 MB after four edits (3,011 MB anon), 3,863 MB by the end of that session |
| With Timeline, about forty edits | | 12 to 20 s | 4,078 MB (3,734 MB anon), swap at its 512 MB cap, `oom_kill 0` |

With Queue alone in the build, lor-5fk6 measured 4,085 MB. Leaving one concept out saves little: the Kotlin daemon, holding the whole module, is about 2.1 GB. Restarting web-dev.sh every so often keeps a session well under the cap. The header says so too.

### Timeline on the phone

The organisation is the brief's. Times are the demo's, in New York.

- **Home**: the time, date and weather over the wallpaper. Under them is the strip: a glass card holding the waking day (08:00 to 22:00) as a bar, with tracked time in amber, guesses in faint amber, unlabelled time in grey, calendar events dashed in cyan and now as an amber tick. It also holds the next event ("Dentist at 16:30, in 1 h 29 min, 7 h left") and the timer row: Start, or the running label with its readout and Stop. One chip at the strip's top right shows the most urgent of a failed sync, LoR not answering, setup to finish, or "4 waiting". A tap on the strip opens the day page. The icons sit near the thumb and search at the foot. A swipe up opens the drawer, a swipe down opens the shade's stand-in, and a double tap opens quick send.
- **The day page**, left of home, merges the agent page and the LoR panel. What waits is pinned at the top on a raised panel, so an approval is never in the middle of a scroll. It shows T3's cards first, then LoR's flags. The cover shows two, each with its one-tap actions, and "N more waiting" opens the rest in a sheet. A question card on the cover shows Answer, which opens its options with their descriptions. Under the cards are two buttons: "7 due, no time", which opens the untimed tasks to tick or to plan into the next free gap, and "12 to name", which scrolls the route to the first block without a label. The route scrolls on its own below, opening with now a third of the way down:
  - **The past.** Tracked blocks are amber with an edge. The agent's guesses show "Meetings?" with a tick that says yes. Unlabelled blocks are faint and dashed, with "Name it". Stretches with nothing recorded say so, and one of ten minutes or more can be named. A tap on a block lifts it into a sheet of labels. A count, a yes or a name each offer Undo. "Say yes to every guess" is in the ⋯ menu.
  - **Now.** The amber now line has a reticle on the spine. With nothing running, Start and the recent labels sit under the line, one tap each. Starting grows a block up from the line, showing "RUNNING SINCE", the 48 px readout, Switch and Stop.
  - **The future.** Calendar events are dashed in cyan. Free time shows "1h 29m free" and "Plan a task", which puts an untimed task there, dashed in cyan, with Undo. Nothing goes into free time unasked. Long empty stretches are squeezed, and hour ticks too close together are left out.
- **Unfolded**, the route is the left pane. The right pane holds every waiting card in full, with questions answered in place, then the untimed tasks and what the agent is working on. When a card or thread is open, it fills the right pane, and "‹ Waiting" goes back.
- **The look** uses the dashboard prototype's Timeline tokens (`timeline/TimelineLook.kt`), so the phone and the dashboard read as one product. That means the same warm greys, the same amber, cyan, grey and red jobs, and Saira Condensed with tabular figures for every time. The type has five steps: 12 for meta, 14 for body, 16 for titles, 20 for page titles and 48 for the one readout. Corners grow larger the further a surface floats: 8, 10, 14, 18 and 22 for sheets. There are three depths: the route is flat, the pinned panel and a lifted block are raised, and sheets, menus and the drawer are glass over the blurred wallpaper. I checked each screen against the refactoring-ui order (hierarchy, spacing, type, colour, borders, depth). One call is left as the dashboard has it: a flag's Done is amber like Approve, so two amber buttons can be pinned at once.
- **Common ground.** Search is Queue's one field, which finds, adds and asks. There is one timer, with one vocabulary ("No label yet") and Start, Switch and Stop. There is one settings sheet. Undo appears only where LoR can reverse a change. Approve, Deny and an answer get three seconds to cancel and no Undo. Every card and the day have a visible ⋯, so nothing is reachable only by a gesture.

### Where each required state appears (`http://127.0.0.1:3860/` plus the hash; the day page is the one-off `{"base":"<state>","where":{"cell":[0,0]}}`)

- **Nothing wants you** (`#timeline/quiet`): the strip has no chip. The day page shows one calm line, "Nothing waits on you", and the route gets the room. If LoR or T3 can't be read, the line says "Can't tell yet" instead.
- **An empty day at 09:00** (`#timeline/empty-day`): the route shows one hour of "nothing recorded" before now, then the day's events and free time ahead. The strip's bar is empty up to now.
- **A failed phone sync** (`#timeline/sync-failed`): a red "Sync failed ›" chip on home's strip, and a note at the top of the day page. Both open the settings sheet at the sync, with Allow for each missing permission and Sync now.
- **T3 disconnected** (`#timeline/agent-t3-stale`): a note on the day page says "T3 is reconnecting · cards as T3 had them 40 min ago", with "T3 as of 40 min ago" beside Waiting on you. Approve and Deny are disabled, and quick send fails with the reason.
- **LoR unreachable, stale** (`#timeline/stale`): the strip's chip reads "LoR as of 13:00", and a red stretch marks the bar after 13:00. On the day page, the note sits above a route that ends at 13:00 with a red "No word from LoR since 13:00" in place of what it can't know. With no data at all (`#timeline/failed`), the route says "LoR can't be reached" rather than drawing a day with nothing recorded.
- **Twelve cards waiting** (`#timeline/overfull`, which has twelve T3 cards and nine flags): two are pinned and "19 more waiting" opens the rest. The route keeps half the cover.
- **Larger text** (`#timeline/large-text`): everything at 1.3. On the cover the pinned panel then takes more of the screen, as expected.

**The screens:** home (`#timeline/demo`), the drawer (`#timeline/drawer-all-scrolled`), search with results and the keyboard up (`#timeline/search-mixed`), and the day page on the cover and the inner screen (`#timeline/agent-waiting`, `/inner`). The question there has described options and Other for free text; on the cover it opens from Answer. Also the menu (`#timeline/app-menu`; the day's ⋯ is the other), the quick-send sheet (`{"base":"demo","where":{"open":"QUICK_SEND"}}`), the settings sheet (`#timeline/home-settings-tabs`), the timer (`{"base":"demo","where":{"open":"APP_TAB","target":"timer"}}`, and running on the route in "the day with a timer running" in `shots/lab.json`), and a thread on the inner screen (`#timeline/thread-long/inner`). `shots/lab.json` gained 74 rows: every state on both screens over the dark wallpaper, the day page in the states that change it and with a timer running, the sheets, and home, the day page, search and the menu over the pale and mixed wallpapers. All 74 were captured with no problems to `~/.local/state/lor-evidence/lor-j1fe/shots/`.

### Interactions, by script

These are in `scripts/lab/gestures/timeline/`, each run with `node scripts/lab/run.mjs <script> --version timeline --state <s> --out <dir>`. Each one checks by anchors that the gesture did what it says. All fifteen pass on the final build. Recordings, strips and final frames are in `~/.local/state/lor-evidence/lor-j1fe/gestures/`.

1. `drawer` (demo, and on the inner screen): a swipe up raises the drawer with the finger, and a swipe down closes it. **Recorded.**
2. `search` (demo): the field at the thumb opens search with the keyboard up, "spo" finds Spotify, and a tap opens it.
3. `approve` (agent-waiting): Approve on the pinned approval shows Cancel, then the card clears and the question takes its place.
4. `answer` (agent-waiting): Answer, the described options, Other, free words, Send, the cancel window, cleared.
5. `timer` (demo): Start on the strip, a label, Stop on the strip with Undo. Then on the day page, a recent label under now starts a block that grows from the line, and Stop on the route.
6. `tick` (demo): the "due, no time" button, tick a task, Undo.
7. `move` (demo): a long press opens WhatsApp's menu, which stays. A drag lifts Camera, closes the menu and moves it after T3 Code, with Undo. **The recorded drag.**
8. `fold` and `fold-inner` (thread-long): the thread is scrolled back with 12 characters of reply typed, on the cover in its sheet and on the inner screen in the right pane. Across a fold either way, the other form shows the same draft, the same scroll in the thread and the same position on the route. `fold-inner` then approves, and the pane goes back to the waiting cards with no empty sheet.
9. `reply` (thread-long, inner): a reply sent beside the route joins the thread.
10. `quick-send`: a double tap on home, then words. On `demo` it says the message went; on `agent-t3-stale` it says why not and keeps the words.
11. `sync` (sync-failed): the strip's chip, Allow twice, Sync now, synced.
12. `menu` (demo): WhatsApp's long-press menu stays after the lift, and Back closes it.
13. `relabel` (demo): the 47 minutes with nothing recorded before now opens its sheet, a label counts it as a tracked block, and Undo takes it back. Then the tick on "Beta design review" says yes to "Meetings".
14. `plan` (demo): "Plan a task" on a free gap, then a task, which is planned there dashed in cyan, then Undo.

Getting the fold to keep the route's place took one fix. The taller inner pane pulled the route back to its end, so the route now has room under the day's end and keeps the same position on both screens.

### Its risk: "on a day that is mostly unlabelled, the route asks more than it tells"

It showed up. On the demo afternoon, the 16 blocks before 15:00 are 4 counted, 9 guesses and 3 unlabelled, and 47 minutes before now have nothing recorded. The stretch of route on screen at 15:00 has four rows, and three of them ask something: "Meetings?", "Ship the Field Guide app?" and "47m nothing recorded · Name it". Only "Public beta" tells (`~/.local/state/lor-evidence/lor-j1fe/risk-demo-day-page.png`). The "12 to name" button says the same thing in one figure. Asking is cheap here: a guess takes one tap, "Say yes to every guess" clears nine at once, and the future half of the route (events, free time, Plan a task) tells plainly. Even so, the first thing under the waiting cards is a list of questions about the morning. The empty day (`risk-empty-day.png`) shows the other side: with nothing recorded, the route is mostly what is ahead and reads well. So whether Timeline suits Sumeet depends on how much of his day is already named. A phone that sent fewer, better guesses would help it more than layout changes would.

### Publishing

- **boat:** `android/boat-build.sh` with its default tasks (the app, `testDebugUnitTest`, `testReleaseUnitTest`, `:homecore:allTests` and the lab's production bundle, every concept in) passed in 4 minutes on 06057ca7: 940 tests and 0 failures. Its bundle draws both `#queue/demo` and `#timeline/demo`.
- **The scratch publish:** boat's bundle went into the folder `publish.sh` copies, then `LOR_LAB_DIR=~/.cache/lor-j1fe/lab-scratch scripts/lab/publish.sh` put it at `phone/` and listed it in the index. Afterwards I removed the copy, so this checkout's `build/dist` is as it was. Served through `labStatic` (lor-web's `/lab/` handler, with its Content-Security-Policy), `/lab/phone/#timeline/demo` loaded and drew in 1.3 s on loopback, 9.0 MB, with no console errors (`published-timeline-demo.png`). I did not publish to the real lab; the lead publishes from main.
- `pnpm test` passes (206). I didn't run `pnpm typecheck`: this change touches no TypeScript, and in a fresh checkout it fails on the route tree (lor-iurl). Queue's own scripts can't run on a `--only timeline` build; boat's bundle has Queue unchanged, and nothing under `queue/` changed. web-dev.sh is stopped.
