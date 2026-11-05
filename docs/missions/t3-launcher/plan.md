# T3 Launcher: plan

The intent is in [strategy.md](strategy.md). This file holds the decisions, increments and issues. LoR has no tracker, so each record carries a status line here: `open`, `todo`, `backlog`, `active`, `done` or `superseded`. Dated notes go under the item they are about. Device results go in `device-checks.md` next to this file.

## How to work this mission

- Work goes to `main` when it works, as in the rest of LoR. Commits are named after the thing, for example `Launcher: home intent and app list`.
- A fresh agent that didn't write the code reviews each increment's commits. LoR has no gate, so this can happen after they land on `main`, and the increment isn't marked done until it has.
- Build: `android/boat-build.sh <checkout-root> <out-dir> [gradle tasks]` (default `assembleDebug testDebugUnitTest`). It syncs the checkout's `android/` to the boat VM `lor-android-build` (`bx_fxc6p2bw`) and builds there under a lock. Only on success does it copy the APK and test reports back. It keeps the VM's auto-stop 30 minutes ahead while its build waits or runs, which cuts any longer lifetime set by hand. Each checkout path keeps a build directory under `~/w` on the VM; delete old ones now and then. If the VM is stopped, the script fails rather than resuming it. An agent may `boat resume bx_fxc6p2bw`. Leave stopping to the auto-stop, because other agents' builds may be running or queued on the same VM. A VM stopped for 14 days loses its disk. If that happens, make a new one with `android/boat-setup.sh` (its header gives the steps, including copying buildbot's debug keystore, without which new APKs can't update the installed app) and update the id here. Never run Gradle on buildbot: a Gradle init script refuses it unless `T3_ALLOW_LOCAL_GRADLE=1`, which only Sumeet may set. Bring the APK back and install it on the emulator: `sg kvm -c start-t3-emulator`, adb at `~/Android/Sdk/platform-tools/adb`. Point the app at the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`), never the real one.
- Nothing goes on Sumeet's phone unless he says so in the current thread. The Fold arrives 2026-11-05, but he said to stick to the emulator for now. Steps that need the real Fold carry **[real Fold]** and wait for him.
- Before building any ordinary launcher part, look at how Lawnchair or Kvaesitso does it (D1).

**Builds, 2026-11-05.** The boat trial ended 2026-11-04. Sumeet allowed boat on the paid plan, which has 551 included hours, and asked for a spike on whether boat is needed at all. The VM `lor-android-build` (`default`, 4 vCPU) was set up with `android/boat-setup.sh`: JDK 21, platform 37 and build-tools 37 in `~/opt`, plus a copy of buildbot's `~/.android/debug.keystore`, so its APKs update the installed app. The spike, on that VM:
- A cold first build with dependency downloads took 1m25s.
- A clean build on a fresh daemon took 24 s, 73 CPU-seconds and about 2.5 GB of memory (the Gradle and Kotlin daemons).
- A warm rebuild after a Kotlin change took 2 s.
- A round trip through boat (sync, build, copy back) adds about 20 s.

Building locally instead needs Sumeet to lift the init-script guard. The question is with him.

## Decisions

### D1: What the launcher is built on. Status: done

**Question.** In "Decide Launcher design", the agent first proposed forking an existing launcher (Lawnchair or Kvaesitso) rather than starting from scratch. Later, the agent proposed that LoR's Android app grow into the launcher, and Sumeet agreed ("We're basically turning LoR into the launcher"). The fork idea was never settled, and every later increment depends on the base.

**Options.**

1. **Grow LoR's app** (about 1,700 lines of Kotlin/Compose: the sensors, the tile, sync). Add a `HOME` activity and build home, the drawer and search in Compose. Read Kvaesitso and Lawnchair for patterns and copy small pieces where it helps (Kvaesitso is GPL-3.0, which doesn't matter for a personal build).
   - *For:* the sensor, tile and sync are already there, and this is the app the threads chose. The layout is the agent's JSON file, a model neither fork has, so it's ours either way. The agent page is a Compose pager page rather than Launcher3's overlay-feed mechanism. There's no upstream to rebase against, and builds are small, which matters while boat hours are scarce.
   - *Against:* we build what every launcher already has: the icon grid, folders, hosting widgets, editing on the phone, and handling both screens. That is the largest cost of this option.
2. **Fork Lawnchair** (Launcher3, the AOSP launcher).
   - *For:* stock behaviour, and the thread said it usually has the smoothest gesture handoff of third-party launchers. Folders, widgets, drag and drop, and the two screens all come for free.
   - *Against:* a very large Java/Kotlin codebase with its own SQLite workspace model, which the JSON layout fights. The left page is its feed-overlay mechanism. Tracking upstream costs what Sumeet already pays on T3, though a personal fork could also be frozen. The LoR sensor and tile would have to move into it. Builds are heavy.
3. **Fork Kvaesitso** (modern Kotlin/Compose, built around search).
   - *For:* a clean Compose codebase with strong search and widgets.
   - *Against:* it behaves further from stock, and its layout model isn't ours either. Tracking upstream costs the same as with Lawnchair. Moving the sensor and tile in is cheaper, because it's Compose like LoR.

**Recommendation: grow LoR's app (1).** What makes this launcher worth having is the agent page, the agent-edited layout and the T3 link, and a fork gives none of those. A fork saves building the ordinary launcher parts: the grid, folders, widget hosting, drag and drop, and the two screens. Those are real, but each is a known, bounded piece of Compose work, and drag and drop matters less when agents edit the layout. Gesture feel is the one place a fork could really win, and no launcher can own the gestures without root. Increment 1 compares a minimal LoR home with Lawnchair's release APK on the same device, so this choice rests on what Sumeet feels rather than on claims. If Lawnchair is clearly smoother, choose 2.

**Decision (Sumeet, 2026-11-05).** Grow LoR's app (option 1). Reference open launchers and steal from them: we don't need to build everything from scratch. Before building a launcher basic (grid, folders, widget hosting, drag and drop, two screens, app list and icon loading), read how Lawnchair and Kvaesitso (and other open launchers, such as Neo Launcher) do it and copy what fits, keeping the license and source noted in the file. Increment 1.2 still compares with Lawnchair, but only to calibrate gesture feel, not to reopen D1.

## Increments

### Increment 1: A third-party home works on the Fold. Status: active

**Goal.** Before any heavy work, find out whether a home app we build stays usable on One UI 9: gesture navigation feels acceptable, the app stays the default home with Unrestricted battery, and it behaves on both screens. D1 is settled, so the Lawnchair comparison only calibrates gesture feel.

**Done when.** `device-checks.md` records a pass or fail, with a note, for each check in 1.1 (emulator) and 1.2 (real Fold). Screenshots stay in the working thread, not in git. Sumeet has said whether the gesture feel is acceptable.

**Unknowns.**
- ~~Whether a foldable emulator image is available on this host.~~ Yes (2026-11-05): the SDK's AVD tooling has `pixel_9_pro_fold` and `pixel_fold` profiles, and the API 35 image is installed.
- Whether the side key can open a third-party default assistant on One UI 9.

#### 1.1 A minimal home app, checked on the emulator. Status: done (2026-11-05; reviewed twice, findings fixed)

- **Goal.** Give LoR's app the least that lets it be chosen as home: an activity with `MAIN` + `HOME` + `DEFAULT`, showing every launchable app (`LauncherApps.getActivityList`) in a plain Compose list that opens on a tap. This is the probe Increment 1 needs. With D1 settled, Increment 3 grows it into the real home.
- **Done when.**
  - On the emulator, Settings offers it as the home app, and pressing home after picking it opens it.
  - Tapping an app opens that app.
  - It is still the default home after an emulator reboot.
  - The existing tabs (Timer, Sensors, Settings, Dashboard) still open from its normal launcher icon.
  - `testDebugUnitTest` passes.
- **Scope.** `android/app/src/main/AndroidManifest.xml` and one new activity. Nothing on the server.
- **Context.** `android/app/src/main/java/com/spadaval/lor/MainActivity.kt`. The manifest already declares `QUERY_ALL_PACKAGES`. Optional: if a foldable AVD can be made cheaply, also screenshot it folded and unfolded.
- **Deferred to Increment 3** (review): the list reloads on every package callback on the main thread; load it off the main thread and refresh only the changed package. Work-profile callbacks are also deferred: profile added, removed, available and unavailable.
- **2026-11-05.** `home/HomeActivity.kt` (app list after Kvaesitso, launching after Launcher3) and the HOME activity in the manifest. All five checks pass on the emulator ([device-checks.md](device-checks.md)). The check found that home needs its own task (`taskAffinity=""`), or the LoR icon reopens home instead of the tabs. The foldable AVD was not run (see device-checks.md).

#### 1.2 Fold checks [real Fold]. Status: backlog, waits on Sumeet

Sumeet installs the 1.1 APK and Lawnchair's latest release APK from its GitHub releases, or tells an agent to. Each check is run with One UI Home, the LoR home, and Lawnchair:

1. **Gestures.** Swipe home, swipe-and-hold for Recents, and swipe sideways between apps, on the cover and the inner screen. Sumeet judges whether the roughness is acceptable, and whether Lawnchair is noticeably smoother than the LoR home.
2. **Stays default** (LoR home and Lawnchair only). Set it as default with Unrestricted battery. A day later, is it still the default home? Has the process survived: is the start time from `adb shell ps -A -o PID,STIME,NAME` unchanged? If adb isn't set up on the Fold, Sumeet judges whether the first home press was a cold start. This shows process survival only, not a held connection.
3. **Both screens.** Fold and unfold with an app open and on home. Does home show on each screen, keep its state, and avoid flashing back to One UI Home?
4. **While there.** Does Settings let the side key open the default digital assistant app, rather than only Bixby?

### Increment 2: The launcher speaks T3. Status: active

**Goal.** A Kotlin module in the app holds one authenticated WebSocket connection to a T3 server. It shows which threads are running, waiting on Sumeet (approval or question) or failed. It can approve, answer, send a follow-up, and launch a thread in a project. Everything the agent page and quick send need from T3 rests on this, and it doesn't depend on D1.

**Done when.**
- JVM tests cover the framing and decoding against frames recorded from a real server.
- A live check against a scratch T3 server shows a thread's state changing and a launched thread appearing.
- Both are recorded in this item's notes.

**Unknowns.**
- ~~Whether the Replay provider can produce an approval and a question.~~ It can (2026-11-05), so 2.2 needs no real model:
  - Its `approval` scenario asks only when the runtime mode is `approval-required`, so launch the thread with `orchestration.launchThread` and not `t3 thread start`, which runs in full access. That gives a pending `command` request.
  - The `question` scenario gives a pending `user_input`.
  - A server restart drops pending approvals but keeps questions.
- How the phone pairs with buildbot's live server. From reading the code: a pairing link from T3's Connections settings (or `t3 pair` on buildbot) carries the server's Tailscale HTTPS address. The app accepts that link, a `t3code://pair` link, or an address and a code. T3 Connect's relay is not supported. This is untested against the live server.

**The scratch T3 server.** buildbot runs the live T3 server Sumeet's phone depends on, so follow t3code's AGENTS.md exactly:
- Make a separate worktree: `git worktree add --detach ../t3code-launcher-test` from `/home/supadava/projects/t3code`, then run `vp i` in it. Change no code in it. (It exists now, at `/home/supadava/projects/t3code-launcher-test`, kept for 2.2.)
- Start `vp run dev` there in the background with `T3CODE_TAILSCALE_SERVE=false`. State then lives in that worktree's `.t3`. Never use `~/.t3/userdata`.
- Read the real port from the `[dev-runner]` line. The emulator reaches the server at `http://10.0.2.2:<port>`.
- Mint a pairing token with `node apps/server/src/bin.ts pair`, run in that worktree.
- `vp run dev` registers the free Replay provider. The `test-t3-app` skill shows how to start threads with it.
- Stop the server by the PID captured at start, never by pattern. Remove the worktree when the increment is done.

**Background.** Effect RPC JSON over WebSocket (`apps/server/src/ws.ts`). The methods are `orchestration.subscribeShell`, `orchestration.subscribeThread` or `getThreadProjection`, `orchestration.dispatchCommand` and `orchestration.launchThread` (`packages/contracts/src/rpc.ts`, `orchestrationV2.ts`). Pairing and session auth are in `apps/server/src/auth/` and `packages/client-runtime/src/authorization`. Port only the fields these calls touch. Read `/home/supadava/projects/t3code`, and never edit it.

#### 2.1 Connect, pair and read the shell. Status: done (2026-11-05; reviewed twice, findings fixed or carried into 2.2)

- **Goal.** Pair with a T3 server, open the WebSocket, subscribe to the shell, and keep a list of threads, each with its project and a state: running, waiting for approval, waiting for an answer, failed, or idle. Reconnect after a dropped connection.
- **Done when.**
  - A JVM test decodes recorded `subscribeShell` frames into that list.
  - Against the scratch T3 server, a small debug screen in the app shows its threads, and a Replay thread started in that server's web UI appears as running.
  - Killing the server and restarting it brings the list back without restarting the app.
- **Scope.** New package `com.spadaval.lor.t3`, and a debug screen behind Settings. Nothing in LoR's server. Credentials stay out of logs (LoR rule).
- **2026-11-05: built** (`22a3ed2`). Package `t3`:
  - `Rpc.kt`: the framing.
  - `Shell.kt`: a pure decoder.
  - `Pairing.kt`, `T3Auth.kt`, `T3Store.kt`: pairing and the session.
  - `T3Connection.kt`: one shared connection, reconnecting with backoff from 1 s up to 30 s.

  Also `ui/T3DebugScreen.kt`, behind Settings, which connects only while it's open. OkHttp 4.12 is the one new dependency.
  - **How the app authenticates.** It pairs as a bearer client, where T3's mobile app uses DPoP. The server accepts both, but the token lasts 30 days, so the phone must pair again monthly until something refreshes it.
  - **Proof.**
    - 14 JVM tests pass. The decoder tests read two sessions recorded from the scratch server (`src/test/resources/t3/`).
    - On the emulator, a Replay thread launched on the scratch server showed as Running.
    - Stopping the server showed "Disconnected… retrying" with the list kept. After a restart the list came back live about 16 s after the port opened, in the same app process.
    - The access token, tickets and pairing code appear nowhere in logcat.
  - **The wire format.** Auth first:
    - `POST /oauth/token` (token exchange, with the pairing token as the subject) returns a bearer token.
    - `POST /api/auth/websocket-ticket/rpc` returns a single-use ticket.
    - The socket is `GET /ws?wsTicket=…&orchestrationProtocol=2`. Any other protocol number gets a 426.

    Then the messages. A text frame carries one JSON message or an array of them:
    - The client sends `Request {id, tag, payload, headers}`.
    - The server sends `Chunk {requestId, values}`, `Exit`, `Defect`, and `Pong` in answer to `Ping`.
    - **The client must `Ack {requestId}` every Chunk, or the stream stalls after the first one.**

    Shell items are `snapshot`, `thread.updated`, `thread.removed`, `project.updated`, `project.removed` and `synchronized`. A snapshot that carries `resolvedRepositoryIdentityRoots` updates project details only, with `threads: []`, so it must not replace the list.
  - **How a thread's state is read.** This follows T3's own thread list:
    - a pending `user_input` request is waiting for an answer;
    - any other pending kind except `auth_refresh` is waiting for approval;
    - otherwise the decoder reads `activityRunStatus`, falling back to `status`;
    - archived and deleted threads are left out. Subagents and side chats are not listed, but their requests raise the listed parent, as T3's `pendingSubagentRequestsByThread` does: a side chat's approval or question raises its parent; a live subagent's approval raises every ancestor up the subagent and side-chat chain to the first listed thread, unless the provider created that subagent, and the subagent's questions are skipped, since the coordinator answers them. Each listed thread keeps the child's thread id and request id, for 2.2 to act on.
  - **`10.0.2.2` once failed.** During the live check the emulator didn't reach the host there, not even a plain Python server, so the check used `adb reverse tcp:<port> tcp:<port>` and `http://127.0.0.1:<port>`. That was minutes after an emulator reboot. Later the same day, `10.0.2.2` answered from both the shell and the app's own uid (`run-as com.spadaval.lor`), so `10.0.2.2` stays the default and `adb reverse` is the fallback.
  - **Known differences from T3's list status** (review, deferred to Increment 3's queue design):
    - `lastErrorClass: usage_limit` shows as failed; T3 calls it "limited".
    - Awaited background work, and `blockedOnThreadWait`, show as running; T3 shows "waiting".
    - `pendingGuestMessageCount > 0` isn't mapped; T3 shows "request".

#### 2.2 Act on threads. Status: active (2026-11-05)

- **Goal.** Use `dispatchCommand` to approve or deny an approval, answer a question, and send a follow-up. Use `launchThread` to start a thread in a named project.
- **Done when.** Against a scratch server, each action changes the thread as T3's own UI shows it, using a free or cheapest-model provider.
- **Scope.** The `t3` package and the debug screen: each waiting thread gets its actions there. A launch picks a project from the shell's project list. Increment 3's cards will call the same functions. Replay provides the approval (`approval` scenario, launched in `approval-required` mode) and the question (`question` scenario).
- **Carried from 2.1's review.**
  - A late pairing can stop a newer screen's connection. `pair()` should restart only a loop that was running.
  - `Shell.state()` duplicates the ranking that `threads` uses. Delete it, and point the tests at `threads`.

### Increment 3: A home Sumeet can live in, with the agent page. Status: backlog, blocked by Increment 1

**Goal.**
- The launcher is his default home. The left-most page is the agent page:
  - the queue: T3 approvals, questions and failures from Increment 2, and LoR Needs you and the next action;
  - running turns;
  - Today;
  - a quick-send button.
- Each card has one main action and opens the right place. For a T3 card that's the T3 app at that thread, in the adjacent pane on the inner screen.
- Home shows his apps from a local layout JSON file.
- The drawer and search find any app by name.

**Done when.**
- On the emulator against the LoR test server and a scratch T3 server, approving from a card clears it, and opening a T3 card deep-links into the T3 app.
- The spec's "The phone" section and "Who is who" describe the app as the launcher, a client of both T3 and LoR, holding the queue that LoR's server doesn't (AGENTS.md: a change that crosses the spec changes the spec).
- **[real Fold]** Sumeet uses it as his home for a few days and says whether it's good enough to keep.

**Unknowns.**
- Which LoR endpoint gives the app Needs you, the brief's first line and Today. `/device/state` exists. Something new may be needed under `/device/*`.
- The layout JSON schema. Design it in this increment, so Increment 7 only adds the git sync to it.
- Which T3 app deep links already exist. The T3 app registers `https://app.t3.codes/threads/...` and the `t3code` scheme (`apps/mobile/app.config.ts`). Whether either opens a thread on buildbot's server is untested. Any missing deep link is a fork item outside this mission.
- Whether "an ordinary good launcher" must also host third-party Android widgets from day one.

### Increment 4: Send a prompt from anywhere. Status: backlog

**Goal.** One quick-send sheet, opening over whatever is on screen, from:
- a button on the agent page;
- the home button pressed while already on home;
- a Quick Settings tile;
- the side key, with the app as the default assistant attaching a screen capture as context.

Sending launches a T3 thread in the first of: the project named in the prompt, the project on screen, or the `assistant` project. The sheet shows where it's going, and changing that costs one tap.

**Done when.** Each entry point launches a thread in the expected project on a scratch server, and the assistant entry attaches the capture. **[real Fold]** The side key opens it from inside another app.

**Inbox.** The `assistant` project (Sumeet, 2026-11-05), a new thread per prompt.

**Unknowns.**
- How "the project on screen" is known when the screen isn't the launcher's own.

### Increment 5: LoR knows how each app was launched. Status: backlog

**Goal.** Every launch from the launcher (home, drawer, search, a card) is recorded with its source. The records go through the existing sensor queue to `/device/ingest`, and they show in LoR's timeline.

**Done when.** A JVM test covers the observation shape. A LoR `pnpm test:db` case accepts it. On the emulator against the test server, a launch from search appears with its source.

**Unknowns.**
- Whether this extends the app-session observation or becomes a new `kind`. Decide with the spec's observation envelope.
- The app-usage sensor leaves out "the launcher and system UI" (spec, "The phone"). Once LoR is the launcher, time on its own pages needs a rule: still left out, or recorded.
- Whether a search launch also records what was typed. The thread proposed it, but it wasn't decided.

### Increment 6: Running turns as Live Updates. Status: backlog

**Goal.** While a T3 turn runs, a Live Update shows it in the status bar and on the lock screen, and it ends when the turn ends.

**Done when.** On an Android 16 emulator image, a running turn on a scratch server shows and clears its Live Update. **[real Fold]** It also shows in One UI's Now Bar, or the result says it doesn't. With the launcher in the background for a day, a turn started from another client still shows its Live Update.

**Unknowns.** Whether the connection must be held by a foreground service when the launcher isn't in front. Increment 1 shows only whether the process survives. This increment checks whether the connection does.

### Increment 7: Agents edit the home layout. Status: backlog

**Goal.** The layout JSON lives in a git repo that agents edit from T3 threads. When a new commit arrives, the launcher shows what changes and applies it only when Sumeet accepts. Undo is a revert.

**Done when.** An agent commit moving an app into a folder appears as a proposed change on the emulator. Accepting applies it, and a revert puts it back.

**Unknowns.**
- Which repo holds the file, and how the phone reaches it: over the tailnet, or served through LoR.
- Whether changes made by hand on the phone are written back to the repo. The thread proposed it, but it wasn't decided.

### Increment 8: Search finds shortcuts and conversations. Status: backlog, blocked by Increment 3

**Goal.** Search covers every app's shortcuts (static, dynamic and pinned) and conversation shortcuts through `LauncherApps.getShortcuts`, which only the default home app may call. Typing a person's name shows their chats in each messaging app, their LoR person card and T3 threads that mention them. Choosing a result opens that shortcut directly.

**Done when.** On the emulator, a dynamic shortcut published by a test app and a conversation shortcut from a messaging app both appear in search and open the right screen. A JVM test covers ranking across apps, shortcuts, people and threads.

**Unknowns.**
- Which LoR endpoint gives people for search, and whether T3 thread search uses `orchestration.searchThreads` (adds one method to Increment 2's set).
- How Kvaesitso indexes shortcuts and ranks mixed results. Read it first (D1).

### Increment 9: Smart folders. Status: backlog, blocked by Increments 3 and 7

**Goal.** An agent sorts apps into drawer folders, each with a one-line reason ("Splitwise is in Money: you open it right after paying"), by writing the layout file. Folders Sumeet edits by hand are pinned and the agent leaves them alone. Usage data comes from LoR, which already has app sessions and attribution rules.

**Done when.** On the emulator, an agent run proposes folders as a layout change (Increment 7), each folder shows its reason, and after a hand edit to one folder, the next agent run leaves that folder unchanged.

**Unknowns.** Where the sorting agent runs: a LoR job that starts a T3 thread in `assistant`, or a step of the nightly run.

### Increment 10: Named and temporary layouts. Status: backlog, blocked by Increment 7

**Goal.** The layout file holds several named layouts, and Sumeet switches between them from home. An agent can propose a temporary layout with an expiry (a trip, an on-call week). It's applied only when he accepts, he can end it early, and it reverts by itself when it expires. Nothing switches on a guess.

**Done when.** On the emulator, an agent proposes a temporary layout, accepting applies it, and at the expiry the previous layout returns. Switching between two named layouts by hand works.

**Unknowns.** Whether a temporary layout is a branch, a dated file or an overlay on the base layout. Decide with Increment 7's repo design.

## Later, outside this mission

Sumeet kept these out of this mission on 2026-11-05. Each would be planned as its own mission once this one closes:

- **Groups of apps that open together** in split view.
- **Generative widgets.** Ideally pinned T3 HTML renders, in sandboxed WebViews. This needs "Pin to home" in Sumeet's T3 fork, a change in that repo.
- **The phone as an output for agents.** A few safe tools over the T3 connection, such as showing something on the phone, pinning a card, or asking a yes/no question.

## Open questions for Sumeet

None open. Answered 2026-11-05: D1 is to grow LoR's app and borrow from open launchers; the Fold work waits and agents use the emulator; quick send defaults to the `assistant` project; smart folders, named and temporary layouts, and shortcut search are in this mission.
