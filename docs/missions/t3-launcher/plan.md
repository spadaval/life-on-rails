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

Sumeet decided (2026-11-05): builds stay on boat.

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

### Increment 1: A third-party home works on the Fold. Status: done on the emulator (2026-11-05); the Fold checks (1.2) moved to [Handed on](#handed-on-2026-11-05)

**Goal.** Confirm on the device that a home app we build stays usable on One UI 9: gesture navigation feels acceptable, the app stays the default home with Unrestricted battery, and it behaves on both screens. D1 is settled, so the Lawnchair comparison only calibrates gesture feel.

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

#### 1.2 Fold checks [real Fold]. Status: superseded (2026-11-05, Sumeet: moved to [Handed on](#handed-on-2026-11-05), for a later quality mission. The Fold is paired over wireless adb and LoR is its default home.)

Each check is run with the LoR home, and with One UI Home where a comparison helps. Lawnchair is a later triage step (Sumeet, 2026-11-05): only if LoR's gestures feel rough, install it to tell One UI's limits from LoR's own.

1. **Gestures.** Swipe home, swipe-and-hold for Recents, and swipe sideways between apps, on the cover and the inner screen. Sumeet judges whether the roughness is acceptable.
2. **Stays default.** Set it as default with Unrestricted battery. A day later, is it still the default home? Has the process survived: is the start time from `adb shell ps -A -o PID,STIME,NAME` unchanged? This shows process survival only, not a held connection.
3. **Both screens.** Fold and unfold with an app open and on home. Does home show on each screen, keep its state, and avoid flashing back to One UI Home?
4. **While there.** Does Settings let the side key open the default digital assistant app, rather than only Bixby?

### Increment 2: The launcher speaks T3. Status: done (2026-11-05)

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

#### 2.2 Act on threads. Status: done (2026-11-05; reviewed twice. The `stop()` fix has had no emulator run. The second review's wording fixes to these notes went in after it.)

- **Goal.** Use `dispatchCommand` to approve or deny an approval, answer a question, and send a follow-up. Use `launchThread` to start a thread in a named project.
- **Done when.** Against a scratch server, each action changes the thread as T3's own UI shows it, using a free or cheapest-model provider.
- **Scope.** The `t3` package and the debug screen: each waiting thread gets its actions there. A launch picks a project from the shell's project list. Increment 3's cards will call the same functions. Replay provides the approval (`approval` scenario, launched in `approval-required` mode) and the question (`question` scenario).
- **Carried from 2.1's review.**
  - A late pairing can stop a newer screen's connection. `pair()` should restart only a loop that was running.
  - `Shell.state()` duplicates the ranking that `threads` uses. Delete it, and point the tests at `threads`.
- **2026-11-05: built.** In package `t3`:
  - `Commands.kt`: pure encoders for the commands and decoders for their replies.
  - `T3Connection`: `call(tag, payload)` is a unary request through the socket's handler table, like the shell subscription. It fails at once with no open socket, fails when the socket drops, times out after 30 s, and is never retried, since a resent command could act twice. On top of it are `respond(request, approve)`, `questions(request)`, `answer(request, answers)`, `send(threadId, text)`, `launchDefaults(projectId)` and `launch(projectId, text, model?, runtimeMode?)`. Increment 3's cards call these.
  - `Rpc.Exit.error` is now the failure's message, for example "Runtime request … is resolved.", rather than the raw cause.
  - The debug screen: tap a thread to open its actions. An approval gets Approve and Deny. A question loads its choices from the thread and offers a typed answer. Every thread gets a follow-up field. A launch form picks a project from the shell's list and shows that project's defaults. It also takes an optional `instance/model` and an "ask before acting" box for `approval-required`.
  - Both items carried from 2.1 are fixed. `pair()` reconnects only while the connection is wanted (started and not stopped since), so a late pairing starts nothing after its screen closes. `forget()` keeps that flag, so pairing again on the same screen reconnects. `Shell.state()` is gone, and its test goes through `threads`.
  - **The command shapes,** each confirmed against the scratch server. They are ported from T3's `packages/client-runtime/src/operations/commands.ts`.
    - Approve or deny: `dispatchCommand {type:"runtime-request.respond", commandId, threadId, requestId, decision:"accept"|"decline"}`. `threadId` is the thread that owns the request, which for a rolled-up request is the child's (`PendingRequest.threadId`).
    - Answer: the same command, with `answers: {<questionId>: <option value, else its label, or typed text, which wins over a chosen option; a list of options for multi-select>}` in place of `decision`. The questions are not in the shell. They come from `getThreadProjection {threadId}`, in the `turnItems` entry with `type:"user_input_request"` and the matching `requestId`.
    - Follow-up: `dispatchCommand {type:"message.dispatch", commandId, createdBy:"user", creationSource:"mobile", threadId, messageId, text, attachments:[], deliveryIntent:"auto", dispatchMode:{type:"start_immediately"}}`. This is what T3's composer sends by default. It starts a run on an idle thread, and the server decides what an active thread does with it.
    - Launch: `launchThread {commandId, creationSource:"mobile", projectId, title, generateTitle:true, modelSelection:{instanceId, model[, options]}, runtimeMode, interactionMode:"default", workspaceStrategy:{type:"root"}, initialMessage:{messageId, text, attachments:[]}}`. The title follows T3's `deriveThreadTitleSeed`: whitespace collapsed and cut at 50 characters plus "...". The server makes the thread id (`thread:project:<projectId>:<uuid>`) and returns it as `threadId`.
    - Replies: dispatch returns `{sequence}`. Launch returns `{threadId, projection, resumed, sequence}`. A refusal is an Exit `Failure` whose cause holds `Fail {error:{_tag:"OrchestrationV2DispatchCommandError", message}}`.
  - **The model and runtime mode of a launch.** If the caller names neither, the launcher reads `server.getConfig` and takes `settings.projectSettingsOverrides[projectId].defaultModelSelection`. A null override still counts. Without an override it takes `settings.defaultModelSelection`. `settings.fallbackModelSelection` is not a default: T3 switches to it only when the default provider's usage limit is used up, and the launcher doesn't port that switch. The runtime mode comes from the project override's `defaultRuntimeMode`, else `settings.defaultRuntimeMode`. This is the settings part of what T3's new-task flow resolves (`resolveProjectSettings`). The launcher does not copy the rest of T3's choice: the usage-limit switch, the sticky last-used model, the first available model, skipping an override whose instance is disabled, and the project's legacy `defaultModelSelection`. With no default it fails with "The server has no default model", and does not guess. A fresh dev server has no default, so the scratch server was given one with `server.updateSettings`. Check that buildbot's live server has a default before quick send relies on it.
  - **Proof.**
    - 26 JVM tests pass on a fresh boat build after the last code change; 8 of them are new, in `CommandsTest`. The command tests compare each payload with a request the server accepted (for the launch, the recorded request had no `generateTitle`; the emulator run is what shows the server taking `generateTitle:true`), recorded in `src/test/resources/t3/` (`dispatch.ndjson`, `launch.ndjson`, `projection-question.ndjson`, `server-config.ndjson`), ids aside. They also decode the recorded replies, including a refusal. Two mutations made two of the tests fail: the follow-up's `dispatchMode`, and the launch's `interactionMode`.
    - On the emulator, against the scratch server at `10.0.2.2`, after pairing again from the debug screen, the app did each action. `getThreadProjection` and the shell, read over the server's API, showed the result each time:
      1. It launched two Replay `approval` threads in `approval-required` mode, and both showed "Waiting for approval". Approve left the request `resolved` with decision `accept`, and the run completed with "Created the requested probe file." Deny gave `decline`. Both threads showed Idle in the app.
      2. It launched a Replay `question` thread. The form showed the question and its two options. Answering "UI flexibility" stored `answers: {"schema_vs_ui_flexibility": "UI flexibility"}`, and the run completed.
      3. A follow-up to the idle approved thread added the user message and started a second run. That run asked again, and the app showed "Waiting for approval".
      4. A launch in the project `launcher-scratch`, added for this check, with no model named, used the project override `devReplay/multi-turn`. The thread came out with `creationSource: mobile` and listed under that project.
    - The access token, tickets and OAuth calls appear nowhere in logcat.
  - **Learned.**
    - The whole thread row was clickable, which merged its buttons into one accessibility node, so neither agent-device nor TalkBack could reach them. Only the heading is clickable now.
    - Commands are not idempotent across reconnects, so the client never resends one. A caller that sees "dropped before T3 answered" or the 30 s timeout doesn't know whether the command ran, and should read the shell before retrying.
    - `t3 project add --base-dir .t3 <path>` adds a project to the scratch server.
  - **Review fixes, 2026-11-05.** The fallback model no longer fills a missing default. `stop()` drops the call queue, and a stopped loop fails, rather than sends, a call it still has queued.
  - **Carried into Increment 3** (review):
    - The cards should disable an action while its call runs. The debug screen doesn't, so a double tap sends twice.
    - A launch always sends `workspaceStrategy: root`. T3 resolves a new thread's env mode as: the project override, then the server's `defaultThreadEnvMode` (null by default), then the project's `.t3` file, with `local` built in. A new project with no threads is forced to `local` (`resolveNewThreadEnvMode`, `packages/shared/src/projectSettings.ts`). With nothing configured, `root` matches T3. The gap matters only where worktree is configured: quick send should follow the same chain. buildbot's live settings (read from `~/.t3/userdata`, 2026-11-05): `defaultModelSelection` is `claudeAgent/claude-opus-5-5`, `defaultThreadEnvMode` is unset, and the `assistant` project has no model or env-mode override and no `t3.json`, so its threads run `local` on Opus 5.5 and `root` matches.
    - The question card should port T3's `resolvePendingUserInputAnswer` (`packages/client-runtime/src/state/pendingUserInput.ts`):
      - Typed text wins over chosen options for every kind of question, and is sent as a plain string.
      - It is trimmed only when the question has no `initialAnswer`.
      - It is dropped when `allowCustomAnswer` is false.
      - Read that function for how `initialAnswer` seeds the answer.
    - The card should load a question's choices when the connection is Live, and again after a reconnect.

### Increment 3: A home Sumeet can live in, with the agent page. Status: done (2026-11-05); the few days as home on the Fold moved to [Handed on](#handed-on-2026-11-05)

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

**Decisions inside this increment** (orchestrator, 2026-11-05; Sumeet may overrule):
- **Third-party widgets wait.** Home hosts no third-party Android widgets in this increment. The LoR "Next" card is native, and generated widgets are a later mission. Widget hosting can come later, with how Lawnchair does it in hand.
- **LoR cards only read and open.** The agent page reads LoR through one new read endpoint. Its LoR cards open the LoR dashboard rather than writing anything, because a phone write path would change LoR's server behaviour, which is Sumeet's call (strategy, Authority).
- **The T3 connection runs while home is on screen.** Home holds it while it's visible and stops it a little after it isn't. Keeping it in the background is Increment 6's question.
- **The layout file supports later increments.** Its schema leaves room for named layouts, folders with a reason and a pinned flag, and an expiry, so that Increments 7, 9 and 10 add behaviour rather than migrate the format.

**Facts found 2026-11-05.**
- **The data for the agent page.** `/device/state` returns the timer and intents only.
  - `needsOwner(tx)` (`src/server/intents.ts`) gives Needs you.
  - `wallOne(now)` (`src/server/trmnl/device.ts`) gives "the one thing": the pin, else the running timer, else an event that is on or soon, else the brief's first line, else the oldest flag. The wall uses it; the web UI does not.
  - `getToday` (`src/server/today.ts`) gives the wake window, the time remaining, the plan and the running timer.

  All three are reads and can be reused.
- **T3 deep links.** The T3 app opens a thread at `t3code://threads/<environmentId>/<threadId>`, or at the verified app link `https://app.t3.codes/threads/<environmentId>/<threadId>`. Each segment is percent-encoded, so `:` becomes `%3A`. The scheme differs by build variant: `t3code`, `t3code-preview` or `t3code-dev`. The packages are `com.t3tools.t3code`, `.preview` and `.dev`. The environment id names the server and is required, and an environment the app isn't paired with shows "Thread unavailable". The launcher needs the paired server's environment id from T3. The T3 activity is `singleTask` and doesn't declare `resizeableActivity`, so whether it opens in the adjacent pane needs checking.
- **Seed data for checks.** The LoR test server (`scripts/ui-server.sh`, `:3857`) has no standing seed. `scripts/ui-check.mjs`'s `tasks` flow makes a flagged task, and its `today` flow makes calendar events.

#### 3.1 A read endpoint for the agent page. Status: done (2026-11-05, `83cc904`, reviewed)
- **Goal.** `GET /device/home?device=` returns what the agent page shows from LoR in one call: the one thing (`wallOne`), Needs you (`needsOwner`), and enough of Today to draw the day arc. That means the wake window, now, the fraction remaining, the running timer, and the next few timed events. Plus the brief's first line.
- **Done when.** A `pnpm test:db` case covers the response. `pnpm test` and `pnpm typecheck` pass. The spec's `/device/*` list names the endpoint.
- **Scope.** `src/server/device.ts`, `src/routes/device/$.ts`, tests and the spec line. No writes.
- **Built 2026-11-05.** `deviceHome` in `src/server/device.ts` returns `{now, timezone, one: {kicker, text, why} | null, brief: string | null, needs_you: [{id, title, note, flagged_at}], day: {wake_start, wake_end, remaining_s, remaining_fraction, running: {title, start, paused, paused_at} | null, events: [{title, start, end}]}}`, times as ISO strings. `events` are timed events not yet over (the one under way included), at most 5. `?device=` is accepted and unused. `wallOne` was split into `briefLine` and `oneFrom` (`src/server/trmnl/device.ts`) so both read once and pick the same way.

#### 3.2 The home shell: pages, layout file, drawer and search. Status: done (2026-11-05, reviewed, fixes in `237a6cb`)
- **Goal.** `HomeActivity` becomes a real home:
  - Pages swipe sideways. The agent page is left-most (a placeholder until 3.3), then home.
  - Home draws the apps, folders and dock from a local layout JSON file in the app's storage. The first run writes a default.
  - Swiping up opens the drawer: every app with its icon, and a search field that finds an app by name.
  - Icons load off the main thread and are cached. The app list refreshes on package changes without stalling home (the review item deferred from 1.1).
  - It works on a phone screen and on a foldable's inner screen.
  - Before writing each basic part (grid, folder, drawer, search, icon loading), read how Kvaesitso and Lawnchair do it and note the source in the file (D1).
- **Done when.** On the emulator: apps from the layout file show on home with their icons, a folder opens, the dock works, the drawer lists every app, typing finds an app, and tapping opens it. A JVM test covers parsing and validating the layout file. The layout schema is written down in this item. The same checks pass on a foldable AVD (`pixel_9_pro_fold`), folded and unfolded.
- **Scope.** `android/` only.
- **The layout file, schema version 1.** `layout.json` in the app's files directory (`/data/data/com.spadaval.lor/files/layout.json`; on a debug build `adb shell run-as com.spadaval.lor` reads and writes it). Home re-reads it when it comes back on screen and the file's modified time changed.
  ```json
  {
    "version": 1,
    "active": "default",
    "layouts": [
      { "name": "default",
        "expires": "2026-11-18T00:00:00Z",
        "home": ["com.whatsapp",
                 "com.android.chrome/com.google.android.apps.chrome.Main",
                 { "folder": "Dev", "apps": ["com.github.android", "org.example/.Term"],
                   "reason": "You open these together.", "pinned": true }],
        "dock": ["com.google.android.dialer", "com.google.android.apps.messaging"] }
    ]
  }
  ```
  - `version` must be `1`. `layouts` holds at least one layout, names unique and non-empty. `active` names the one home shows.
  - An app is a string: a package name (that package's launchable activity in the main profile that comes first by label, in `Collator` order) or `package/activity` (`package/.Short` expands as `ComponentName.unflattenFromString` does).
  - `home` (optional, at most 24 items) is in reading order; home fills its grid row by row: 5 columns when home is at least 600 dp wide inside its insets, else 4. On the Fold that is 4 on the cover screen and 5 on the inner screen. An item is an app or a folder.
  - A folder has `folder` (its name) and `apps` (one or more apps; folders don't nest). Optional: `reason`, one line shown when it opens, and `pinned` (default false), meaning Sumeet arranged it and an agent leaves it alone (Increment 9).
  - `dock` (optional) holds at most 5 apps.
  - `expires` (optional) is an ISO-8601 date-time with an offset. It's read and kept, but nothing acts on it until Increment 10.
  - Unknown fields are errors, so a typo doesn't silently vanish. A later build may add new optional fields without bumping `version`; the version changes only when an existing field changes meaning. Every error names its place (`layouts[0].home[3]: "WhatsApp" is not a package name or package/activity`), and home shows it in place of the grid. An app that isn't installed is left out without an error.
  - The first run writes a default: the dock is the default dialer, SMS app, browser and camera (resolved from intents), home holds a few common apps that are installed, then a Google folder with the rest of Google's apps.
- **2026-11-05.** Built in `android/app/src/main/java/com/spadaval/lor/home/`: `Layout.kt` (the schema, parsing and validation), `HomeModel.kt` (the app list and layout file, off the main thread), `Icons.kt`, `AppSearch.kt`, `HomeScreen.kt` (pager, home, folder, dock, drawer) and `HomeActivity.kt`. Tests: `LayoutTest`, `AppSearchTest`.
  - *Borrowed* (each file names its source). Kvaesitso (GPL-3.0): the per-package app list refresh (`AppRepository.kt`), icons read on `Dispatchers.IO` into an `LruCache` (`LauncherApp.loadIcon`, `IconService`), and accent-stripping normalisation (`CompatStringNormalizer`). Launcher3 / Lawnchair (Apache-2.0): word-break prefix matching (`StringMatcherUtility.java`), the keyboard's Go opening the first result (`AllAppsSearchBarController`), the dock as a hotseat under the grid, a folder icon previewing four apps on a 2x2 grid that opens as a panel over a scrim (`FolderIcon`), a home press closing what's open (`Launcher.onNewIntent`), the dock resolved from intents (`default_workspace`), profile broadcasts reloading the list, and `startActivitySafely`'s caught launch failures. Ours: the JSON layout in list order (Launcher3 uses an SQLite table of cells), and the search ranking (starts with, then a later word, then for 3+ characters anywhere in the label).
  - *Carried from 1.1 and done:* the app list loads off the main thread, a package change reloads only that package, and work-profile broadcasts (added, removed, available, unavailable, unlocked) reload the list.
  - *Proof.* `boat-build.sh` passed `assembleDebug testDebugUnitTest` (34 tests, 8 of them new). On `T3_Phone_API35` (1080x2400), with the final APK, a script ran each check and read the foreground app from `agent-device appstate`: a home app (Gmail), an app in the Google folder (Clock), a dock app (Chrome), and the search "you" then a tap on YouTube (YouTube's notification prompt was in front; `dumpsys activity recents` shows YouTube on top). The drawer showed the 22 apps that `cmd package query-activities` lists with `LAUNCHER`. A hand-written layout (another active layout, a `package/.Activity` ref, a folder with a reason, a missing app) showed after a trip to another app and back, and swapping the file back redrew home with the right icons. A broken file showed its error on home. No crashes in `logcat -b crash`. The same script passed on a new `T3_Fold_API35` AVD (`pixel_9_pro_fold`, API 35 Google APIs) unfolded (2076x2152: date left, grid and dock right, 10-column drawer) and folded (1080x2424). The home process kept the same pid across the fold. Screenshots stay in the working thread.
  - *Found and fixed while checking:* after the layout changed, icons stayed in their old places, because `produceState` keeps its value when its key changes. Also the system bar icons were dark over the drawer; home now always uses light ones, like its labels.
  - *Review fixes (2026-11-05).* The date header turns over at midnight: it rereads the date on resume and on the system's date, time and zone broadcasts. A layout file over 256 KB, nested too deeply, or with anything after the root object shows as Broken. Any failure reading the file shows as Broken, and the layout is read in its own step after the app list, so a failed app load can't leave home on Loading. With no app list, the first run writes no default and tries again on the next resume. Tests cover the size cap, trailing content and deep nesting.
  - *Deferred from the review* (moved to Launcher polish, 2026-11-05: the light-wallpaper contrast to its Increment 6, the rest, the overflow included, to its Increment 8).
    - A process-scoped model and icon cache, so a recreated activity doesn't reload everything. Wait for the density check on the real Fold.
    - An icon-cache race: a package change can evict an icon while an older load is still writing it back, leaving the stale icon cached.
    - Landscape and large font sizes can overflow home and the drawer. Lock home to portrait or let it scroll.
    - Search normalises every label on each keystroke. Cache the normalised labels if it shows on a long app list.
    - Home's white labels lose contrast on a light wallpaper. The agent page has a veil (3.3); home has only a text shadow.
  - *Open.* Pulling the drawer down past the top of its list to close it was not tried on a device; back and a home press were. The page dots are centred on the whole inner screen while the dock sits on the right. Neither the hinge nor the inner screen's insets are handled specially. The `T3_Fold_API35` AVD is left for later checks, shut down.

#### 3.3 The agent page. Status: done (2026-11-05, reviewed with 3.2's fixes; F3 and F6 fixed in Increment 4)
- **From 3.1's review.**
  - Draw the timer from `day.running`, not `one`. While paused, `one` still says "Running since".
  - `day.running.title` is the intent's name, so a timer started from the tile shows "Uncategorized". Add the block's own label to `/device/home` if the card wants it.
  - `/device/home` follows `getToday`'s calendar day, so a wake end after midnight would mislabel the late hours. The current window ends at 22:00.
- **Goal.** The mockup's agent page (`/home/supadava/design/t3-launcher/t3-launcher-screens.html`, "Agent page" and "Agent page + T3"):
  - Today's arc and Next, from `/device/home`;
  - running turns;
  - the queue: T3 approvals, questions and failures, then LoR Needs you;
  - a quick-send button, which launches into the `assistant` project for now, until Increment 4's sheet.

  Each card has one action, built on Increment 2's functions with the carried review items:
  - a T3 approval card approves;
  - a question card opens its choices;
  - a failed or running card, or a tap on any card's icon, opens the T3 app at that thread, in the adjacent pane on a large screen;
  - a LoR card opens the dashboard.

  Calm, with no animation that runs continuously (strategy).
- **Done when.** Increment 3's first Done when holds, on the emulator, against the LoR test server and a scratch T3 server.
- **Built 2026-11-05.** `android/app/src/main/java/com/spadaval/lor/agent/`: `AgentPage.kt` (the page), `AgentModel.kt` (its state and card actions), `AgentQueue.kt` (which threads become cards, pure) and `T3App.kt` (opening the T3 app). Also `net/DeviceHome.kt` (`/device/home`), `t3/T3Links.kt` (thread deep links) and `t3/UserInput.kt` (answering a question, ported from T3's `pendingUserInput.ts`). Tests: `AgentQueueTest`, `DeviceHomeTest`, `T3LinksTest`, `UserInputTest`.
  - The timer comes from `day.running`; `one` is shown only when it isn't the timer. The block's own label was not added to `/device/home`.
  - The queue is approvals, then questions, then failures, then LoR Needs you. A failure shows for 24 hours after its thread last changed. A card hides as soon as its call succeeds, and its buttons are disabled while the call runs.
  - `T3Connection` is now held: each `start` is one hold and each `stop` lets one go, so home and the debug screen share it. The agent page holds it while home is started and lets go 30 s after. It reads LoR once a minute while home is started.
  - The deep link needs the server's environment id. It comes from `GET /.well-known/t3/environment`, which needs no credential.
  - Quick send refuses a project set to start threads in a worktree, since the launcher can't prepare one. A question's `initialAnswer` and an option without a `value` follow T3's rules.
  - `FLAG_ACTIVITY_LAUNCH_ADJACENT` is set only at 600 dp wide or more. On a phone it split the screen with the last app used.
  - The page sits on a dark veil, so it reads over a light wallpaper. On a large screen its content keeps to the left 520 dp.
- **Proof.** `boat-build.sh` passed `assembleDebug testDebugUnitTest` (49 tests). Against the LoR test server and the scratch T3 server, on `T3_Phone_API35`: the approve card cleared on Approve, a question card's choices opened and its answer cleared it, quick send started a thread in `assistant` and it showed under Running, a T3 card's icon opened the T3 app at that thread, and a LoR card opened the dashboard at the task. On `T3_Fold_API35`, unfolded and folded: the page draws, and the home process kept its pid across the fold. Unfolded, a T3 card opens the thread in a split pane; folded, it opens full screen. Details in `device-checks.md`.
- **Carried forward.**
  - On the inner screen the split pairs T3 with the last app used, not with home: home can't be in a split on stock Android 15. Whether One UI does better is **[real Fold]**. If it doesn't, the agent page could live in its own activity that can take a split.
  - The page dots are centred on the whole inner screen while the page keeps to the left (as in 3.2).
  - Nothing shows the block's own label for a timer started from the tile; it still says "Uncategorized".
- **Deferred from the review of 3.2's fixes and 3.3** (2026-11-05; F3 and F6 were fixed in Increment 4, F5 and F7 in Increment 6; moved to Launcher polish: F2 to its 1.1, F4 to its Increment 8):
  - F2: if `loadAll()` fails on the first run, `_apps` stays empty and only a profile broadcast reloads it; one app update then puts a single app in `_apps`, and the next resume writes a one-app default layout that sticks (`HomeModel.kt`). Reload the app list on resume while it's empty.
  - F4: card actions run in the page's `rememberCoroutineScope`, so a dark-mode, font-scale or density change recreates the activity, cancels a respond that has already gone out, and re-enables the card. Belongs with the deferred process-scoped model.
  - F5: `LayoutTest`'s deep-nesting case hits the JVM org.json's depth-512 `JSONException`, not the `StackOverflowError` catch. A real test parses about 400 levels on a thread with a 64 KB stack. **Done (2026-11-05, Increment 6), as far as the JVM allows:** it can't reach the catch. Below depth 512 the JVM's org.json catches a `StackOverflowError` itself in `JSONTokener.nextValue` and throws a `JSONException` ("JSON Array or Object depth too large to process."), even at 480 levels on a thread with a 16 KB stack (tried). The test keeps its 100,000 levels and now says it checks only "not JSON, no crash"; the catch is for Android's org.json, which has no limit.
  - F7: `AgentQueueTest` doesn't check the order within a kind (most recently updated first). Add an assertion if it's cheap. **Done (2026-11-05, Increment 6):** `withinAKindTheMostRecentlyUpdatedComesFirst` feeds approvals, questions and failures interleaved in the shell's order and checks each kind keeps it.

#### 3.4 The spec says what the phone is now. Status: done 2026-11-05
- **Goal.** The second Done when of Increment 3: "The phone" and "Who is who" describe the app as the launcher. It's a client of both T3 and LoR, and it holds the queue that LoR's server doesn't.

### Increment 4: Send a prompt from anywhere. Status: done on the emulator (2026-11-05, reviewed, fixes in `7956097`); the side key moved to [Handed on](#handed-on-2026-11-05)

**Goal.** One quick-send sheet, opening over whatever is on screen, from:
- a button on the agent page;
- the home button pressed while already on home;
- a Quick Settings tile;
- the side key, with the app as the default assistant attaching a screen capture as context.

Sending launches a T3 thread in the first of: the project named in the prompt, the project on screen, or the `assistant` project. The sheet shows where it's going, and changing that costs one tap.

**Done when.** Each entry point launches a thread in the expected project on a scratch server, and the assistant entry attaches the capture. **[real Fold]** The side key opens it from inside another app.

**Inbox.** The `assistant` project (Sumeet, 2026-11-05), a new thread per prompt.

**Unknowns.**
- How "the project on screen" is known when the screen isn't the launcher's own. Answered in 4.3.

**Facts found 2026-11-05.**
- **Images on a launch.** T3's clients make the thread id themselves (a UUID, `makeTurnCommandMetadata` in `apps/mobile/src/lib/commandMetadata.ts`), upload each image with `assets.persistChatAttachments {threadId, messageId, attachments: [{type:"image", name, mimeType, sizeBytes, dataUrl}]}`, and put the stored attachments it returns (`{type, id, name, mimeType, sizeBytes}`) in `launchThread`'s `initialMessage.attachments`, with the same `threadId` and `messageId` (`persistAttachments` and `startThreadTurn` in `packages/client-runtime/src/operations/commands.ts`). The server checks that the data URL's type matches `mimeType` and that the decoded size equals `sizeBytes`, and caps an image at 10 MiB. A bearer session may call it (`AuthOrchestrationOperateScope`).
- **A screen capture comes only through a voice interaction session.** An activity that answers `ACTION_ASSIST` is offered as the assistant but gets no screenshot. A `VoiceInteractionService` with a `VoiceInteractionSessionService` gets `onHandleAssist` (the app's `AssistStructure` and `AssistContent`) and `onHandleScreenshot`. Settings lists it as a digital assistant only if its metadata also names a `RecognitionService`, so the app carries an empty one.
- **What the screen can say about a project** (checked on the emulator). `AssistContent` carries the foreground activity's intent and, from a browser such as Chrome, the page's web URI. `AssistStructure` carries the visible views. T3's Android app adds nothing of its own (no `onProvideAssistContent`, no `FLAG_SECURE`), and it gives the assistant almost nothing: over a thread, its structure was 33 bare layout views with no text and no content descriptions (React Native's views don't fill in the structure), and its intent was the launcher's `MAIN`, because a deep link reaches its single activity through `onNewIntent`. Its header does show `<project> · <server>`, but only the accessibility tree has it. Thread `lastVisitedAt` in the shell doesn't help either: opening a thread in the T3 app didn't move it, and it's shared with every other client. The screenshot does come through.

#### 4.1 The sheet, from the agent page and from home. Status: done (2026-11-05, reviewed)
- **Goal.** One quick-send sheet in its own translucent activity (`quicksend/QuickSendActivity`), so it opens over whatever is on screen. It holds the T3 connection while it's shown. It shows where the prompt will go and why ("named", "on screen", "inbox"), with a row of project chips: one tap on another picks it, and that pick wins until the sheet closes. Send starts a new thread every time, closes the sheet, and says where it went. The agent page's button opens it, replacing 3.3's inline panel. A home press while already on home, with nothing open and the home page showing, opens it too (Launcher3's `alreadyOnHome`: the window has focus and the intent wasn't brought to front). A home press from another app, or with the drawer, a folder or the agent page open, does what it did before.
- **The rule for the project named in the prompt.** The prompt starts with `@` and a project's title, ignoring case, followed by a space, punctuation or the end. When titles overlap, the longest wins. The mention is taken out of the text sent. Only the start counts, so `@` in the middle of a sentence never redirects a prompt.
- **Done when.** A JVM test covers the choice: named, then on screen, then `assistant`, and the mention rule's edges. On the emulator, against the scratch server, the agent page's button and a home press on home each open the sheet, and sending starts a thread in the expected project (a named project, and `assistant`).
- **Scope.** `android/` only.

#### 4.2 The Quick Settings tile. Status: done (2026-11-05, reviewed)
- **Goal.** A second tile, "Ask", whose tap collapses the shade and opens the sheet over the app in front.
- **Done when.** On the emulator, the tile added to Quick Settings opens the sheet over another app, and a prompt sent from it starts a thread in `assistant`.

#### 4.3 The assistant, with the screen attached. Status: done on the emulator (2026-11-05, reviewed); the side key moved to [Handed on](#handed-on-2026-11-05)
- **Goal.** LoR's app can be the default digital assistant: a `VoiceInteractionService` whose session opens the sheet over the current app with the screenshot attached, shown as a thumbnail that one tap removes. The image goes to T3 the way T3's clients send one (Facts above). The sheet reads "the project on screen" from what the session hands it.
- **The rule for the project on screen** (as built, after the emulator showed what T3's app hands over; Facts above). A browser showing a T3 thread: any path segment of the page's web URI (`AssistContent.webUri`) that is a thread id the shell knows gives that thread's project. Nothing else counts. The activity's intent data was dropped, since in a single-activity app it is the link that first opened it, stale after any navigation. The screen's text was dropped too: the one app it was for, T3's, gives none, and reading other apps' text for a project name would guess. The tile and home have no screen to read, so they skip this step. Over the T3 app the sheet says "inbox", with the capture attached and one tap to change the project.
- **Done when.** On the emulator, with LoR as the digital assistant, invoking it over a T3 thread in a project other than `assistant` opens the sheet with that project chosen, "on screen", and a send starts a thread there whose first message carries the capture (read from the thread's projection or the scratch server's state DB). **[real Fold]** The side key opens it from inside another app: waits for Sumeet.

**Built 2026-11-05** (4.1 to 4.3 together). `android/app/src/main/java/com/spadaval/lor/quicksend/`: `Destination.kt` (the choice, pure), `QuickSend.kt` (opening the sheet, and the send), `QuickSendActivity.kt` (the sheet) and `Capture.kt` (what the assistant hands the sheet). `assist/Assistant.kt` (the `VoiceInteractionService`, its session, and the empty `RecognitionService`), `res/xml/assistant.xml`, `tile/AskTileService.kt`. Tests: `DestinationTest`, and a new `CommandsTest` case for the image upload.
- **The sheet** is its own task (`taskAffinity` of its own, `singleTask`, out of Recents, translucent), so from home it doesn't bring up the LoR tabs behind it, and over another app that app stays visible. It closes when it leaves the screen. It lists every project as a chip, `assistant` first, and the label above them says where and why: "To launcher-scratch · named · a new thread".
- **A send can't be lost** (review F3 of 3.3): it runs in a process-wide scope and holds the T3 connection itself until T3 answers. Close, the scrim and back are off while it runs. Its result is a toast from the application context, so it shows even if the sheet has gone.
- **Every launch now chooses its thread id** (a UUID, as T3's clients do), with or without an image. Before, the server made `thread:project:<projectId>:<uuid>`; nothing in the launcher read that shape.
- **The image** goes up as a JPEG (quality 85, about 85 KB for a 1080x2400 screen), named `Screen - <app>.jpg`, through `assets.persistChatAttachments`, then with the launch. The thumbnail on the sheet has Remove / Attach.
- **The assistant session** opens the sheet only once the screenshot is in (or after 1.5 s), so the sheet is never in the picture, and hides once it has both the assist data and the screenshot (or after 5 s). It reads `AssistContent.webUri` and the app's label; it doesn't read the views' text.
- **Settings** has a Quick send section: "Add the Ask tile" (`StatusBarManager.requestAddTileService`, Android 13+) and "Choose the assistant" (the system's assistant settings). The About text no longer says the screen is never read.
- **Review fixes from 3.3** (F6): the agent page says "Actions are off until it's back" rather than that actions wait, and its `notice` clears on the next action.
- **Proof.** `boat-build.sh` passed `assembleDebug testDebugUnitTest` (60 tests; 7 in `DestinationTest`, 1 new in `CommandsTest`). On `T3_Phone_API35` against the scratch server, each entry point opened the sheet and started a thread where expected; details in [device-checks.md](device-checks.md). No crashes in `logcat -b crash`; no tokens in logcat.
- **Open.**
  - Over T3's Android app the project on screen is unknown, so the sheet says "inbox" there. The fix is a T3 fork item, outside this mission: `onProvideAssistContent` in T3's `MainActivity` setting `webUri` to the open thread's app link (`https://app.t3.codes/threads/<environment>/<thread>`), which this rule already reads. Recorded under Open questions.
  - The keyboard didn't open by itself on the emulator (`stateVisible` and a focus request; the IME refused). The field has focus, so a tap opens it. Check on the Fold.
  - F3's Close-while-sending was checked by reading the code, not on the device: a Replay launch answers too fast to tap Close in between.
  - The side key, and whether One UI lets a third-party assistant have it, **[real Fold]**.
- **Review, 2026-11-05** (an agent that didn't write the code, reading Increment 4's commits). Fixed in "Launcher: fixes from quick send's review":
  - R1: a swipe home from another app could pop the sheet if gesture navigation focused home before the HOME intent. Run on the emulator with gesture navigation (device-checks.md): no misfire from Settings, Chrome, Clock or the drawer, fast or slow, and the home key on home still opens the sheet. No code change. Found while checking: a home *swipe* on home doesn't open the sheet (the intent comes without focus), so under gesture navigation only the home key does. Recheck on the Fold.
  - R2: the sheet's text, picked project, attach choice and failure are `rememberSaveable`, and the opening counter is saved with the activity (`setIntent` keeps the latest opening), so a fold, unfold or rotation keeps a half-typed prompt. Checked by rotating on the emulator.
  - R6: `DestinationTest`'s on-screen case fed `t3code://` intent links that the session no longer collects; it now checks `webUri` cases only (a browser at `app.t3.codes/threads/…`, a T3 web URL, a gone thread, an unrelated page). The test count above said 8; it was 7.
- **Deferred from the review** (R3 to R5 moved to Launcher polish Increment 8, 2026-11-05).
  - R3: a launch reply slower than 30 s, then a retry, makes a duplicate thread. Fix by reusing one id set per opening of the sheet, together with `reuseExistingThread`.
  - R4: if the sheet finishes on stop mid-send and the send then fails, the text is lost and only a toast remains.
  - R5: the thumbnail decodes the screenshot at full size on the main thread. Decode with an `inSampleSize` of about 8.
  - No action: R7 (assistant session races) and R8 (non-ASCII and punctuation edges in `@project`).

### Increment 5: LoR knows how each app was launched. Status: done (2026-11-05, reviewed, F1 fixed in `6991e4c`)

**Goal.** Every launch from the launcher (home, drawer, search, a card) is recorded with its source. The records go through the existing sensor queue to `/device/ingest`, and they show in LoR's timeline.

**Done when.** A JVM test covers the observation shape. A LoR `pnpm test:db` case accepts it. On the emulator against the test server, a launch from search appears with its source.

**Unknowns.**
- ~~Whether this extends the app-session observation or becomes a new `kind`.~~ A new kind, `launch` (decision below).
- ~~Time on the launcher's own pages.~~ Recorded (decision below).
- ~~Whether a search launch also records what was typed.~~ It does (decision below).

**Decisions inside this increment** (orchestrator, 2026-11-05; Sumeet may overrule):
- **A new observation kind, `launch`, not an extension of the app session.** A launch is an instant on a tap; a session is a span built later from the system's usage events, 15 minutes after the fact, and has its own id (device, package, start). Joining the two would mean matching a tap to a session start that the system reports a few hundred milliseconds later, and a launch that opens an app already on screen, or that fails, has no session to join. `kind = 'app'` is what fill, Today's screen time, pickups and `lor places` read (`attribution/fill.ts`, `today.ts`, `places.ts`), so a separate kind leaves the actual lane and every score untouched, as an observation should. The envelope needs nothing new: `source=android`, `app` = the package opened, `title` = its name, `url` = the link an agent card opened, `t_start = t_end` (an instant, as texts are), and `payload = {kind: "launch", from, package?, activity?, label?, folder?, query?, thread?}`. The id is `Obs.id("launch", device, epoch-ms, package, activity or link)`, made once when the tap happens and stored in the queue row, so every retry sends the same id and lands on the same row (ingest is idempotent on `id`). Ingest needed no change.
- **Time on the launcher's own pages is recorded.** The app-usage sensor left out whatever package was the default home, which since Increment 1 is LoR itself, so it was also dropping time in LoR's own tabs (Timer, Sensors, Dashboard) and the quick-send sheet. Product rule 6 says not to ration the owner's data, and nothing downstream needs home left out: fill absorbs a session under 2 minutes inside a block (`INTERRUPTION_MS`), so the few seconds on home between two apps don't split a block (from the review, fixed 2026-11-05: a short session joins only if it starts within 2 minutes of the block's end, `PICKUP_GAP_MS`, so a blink on home after the phone sat locked no longer stretches the block over the locked time), and longer time on the agent page is real phone time, grey until a rule labels `com.spadaval.lor`. Screen time and pickups on Today now count time on home, which is screen time. Only `com.android.systemui` (shade, lock screen, Recents) stays out. Short hops still drop out under the sensor's 3 s minimum.
- **A search launch records what was typed, as typed.** Rule 6 again, and the code gives no reason not to: the text never leaves the phone except to LoR, it is the owner's own typing in the launcher (not a third party's), and it's one short string per launch. It's `payload.query`, untrimmed. A drawer launch with nothing typed is `from: "drawer"`; anything typed makes it `from: "search"`, including the keyboard's Go opening the first result.
- **Launches are their own switch** in the app's Sensors list ("Launches", on by default, no permission needed), like every other sensor.
- **The day's timeline says where from.** `dayTimeline` (`src/server/memory/timeline.ts`) shows a launch as its app's name with `opened from search "clo"` (or home, dock, folder <name>, drawer, agent card, agent icon); a link nothing resolved shows its URL. This is a display change for the new kind, inside the Outcome ("The LoR sensor records how each app was launched"), not a new server behaviour, so it isn't held for Sumeet.

#### 5.1 The launch observation, recorded at every launch. Status: done (2026-11-05, reviewed)
- **Goal.** `collect/Launches.kt`: a pure `observation(device, at, launch)` building the envelope, and `record(context, launch)`, which puts it in the sensor queue on an IO thread, never throws, and skips it when Launches is switched off. Called after a launch succeeds from:
  - home's grid (`home`), the dock (`dock`), a folder (`folder`, with its name), the drawer (`drawer`), and the drawer with text typed (`search`, with the text);
  - the agent page: a card's own action (`agent_card`: Open on a failed T3 card or a LoR card, a tap on a running turn) and a card's icon (`agent_icon`: "Open in T3", "Open in LoR"). A T3 launch carries the thread id and the deep link; a LoR launch carries the dashboard URL and the browser that took it (null when Android would ask which).
  Quick send isn't a launch and records nothing.
- **Also.** `AppUsageCollector` leaves out only the system UI (decision above).
- **Done when.** A JVM test covers the shape and the id. On the emulator, a search launch is in the queue with its source and query.

#### 5.2 LoR accepts and shows launches. Status: done (2026-11-05, reviewed)
- **Goal.** A `pnpm test:db` case: ingest stores a launch once (a retry stores nothing), keeps `t_start = t_end`, makes no time block, and the day's timeline shows each with where it was opened from. `dayTimeline` gains the launch line. The spec's sensor table, the `kind` list and the Today route's Timeline say what's recorded.
- **Done when.** `pnpm test`, `pnpm test:db` (bar `fill.test.ts`'s known failure) and `pnpm typecheck` pass. On the emulator against the test server, a search launch is in `life_on_rails_test` with its source and shows in `dayTimeline`.

**Built 2026-11-05** (5.1 and 5.2 together). `collect/Launches.kt` (`LaunchFrom`, `Launch`, `Launches`), the call sites in `home/HomeActivity.kt`, `home/HomeScreen.kt` (a `LaunchApp` callback carrying the source), `agent/T3App.kt`, `agent/AgentModel.kt` and `agent/AgentPage.kt`; `Sensor.LAUNCHES` in `data/Prefs.kt`. Server: `src/server/memory/timeline.ts`. Tests: `LaunchesTest` (5), and a `launches` case in `tests/db/device.test.ts`.
- **Proof.**
  - `boat-build.sh` passed `assembleDebug testDebugUnitTest`: 65 tests, 5 new in `LaunchesTest`.
  - `pnpm typecheck` clean; `pnpm test` 138 passed; `pnpm test:db` 119 passed, 1 failed: `fill.test.ts` "fills by precedence…", which failed on `main` before this mission. The new `launches` case passes.
  - On `T3_Phone_API35` against the LoR test server (`/tmp/lor-ui-33`, rebuilt with this change): the drawer, "clo", a tap on Clock. The queue held `{kind: "launch", from: "search", query: "clo", package: "com.google.android.deskclock", activity: "com.android.deskclock.DeskClock", label: "Clock"}` with `t_start = t_end`. A tap on Life on Rails on home queued `from: "home"`. Sync now uploaded both ("2 new on the server"). `life_on_rails_test` has both rows with `kind = 'launch'`, and `dayTimeline` for today lists `Clock · opened from search "clo"` and `Life on Rails · opened from home`. No crashes in `logcat -b crash`; no tokens in logcat.
- **From the review (2026-11-05).** F1 fixed in `6991e4c` (a blink on home after a locked spell stretched the last phone block; see the decision above). Deferred:
  - `dayTimeline` lists newest first with `LIMIT 500`, so a heavy phone day (home time between apps, plus launches) can lose its morning. Check the real row count after a week on the Fold.
  - In a split on the inner screen, `collect/Sessions.kt` tracks one app at a time, so time with T3 beside a resumed home may count as Life on Rails. Check it on the first real day on the Fold.
  - `tests/db/device.test.ts` "Launches are not app sessions" is always true. A test that ingests a real app session next to a launch, and checks the block links only the session, would catch a reader that took both kinds.
- **Open.**
  - The Today page draws "Recorded" (the timeline) only for days gone by, so a launch shows there from the next day. The check read `dayTimeline` directly.
  - Agent-page launches (`agent_card`, `agent_icon`), the dock and folders were checked by the JVM and db tests, not on the emulator.
  - Home time as app sessions wasn't run on the emulator (its App usage sensor is off there). The first real day of it on the Fold will show whether the agent page needs a rule of its own.

### Increment 6: Running turns as Live Updates. Status: done on the emulator (2026-11-05, reviewed, fixes in the same commit); the Now Bar and a day in the background moved to [Handed on](#handed-on-2026-11-05)

**Goal.** While a T3 turn runs, a Live Update shows it in the status bar and on the lock screen, and it ends when the turn ends.

**Done when.** On an Android 16 emulator image, a running turn on a scratch server shows and clears its Live Update, including one started from another client while the launcher is in the background. (The Now Bar and a day in the background on the real Fold moved to [Handed on](#handed-on-2026-11-05).)

**Unknowns.** Whether the connection must be held by a foreground service when the launcher isn't in front. Increment 1 shows only whether the process survives. This increment checks whether the connection does.

**Built 2026-11-05.** `android/app/src/main/java/com/spadaval/lor/live/`: `LiveTurns.kt` (which threads show and what each says, pure) and `LiveUpdates.kt` (posting and cancelling the notifications). `LorApp` starts it for the process. `Shell.kt` now carries each thread's `runLive` and `activityRunStartedAt`. Settings has a Live Updates section. The manifest gains `POST_NOTIFICATIONS` and `POST_PROMOTED_NOTIFICATIONS`. Tests: `LiveTurnsTest` (6).
- **Which turns show.** One notification per listed thread with a turn under way: running, or waiting on Sumeet for an approval or an answer. A request raised from a side chat or subagent onto an idle parent counts (review fix). A thread's own request left over from a cancelled run doesn't count. An idle or failed thread has none, so its update ends when the turn does. The text is "<project> · Running", "Waiting for your approval" or "Waiting for your answer". The chip (`setShortCriticalText`) says "Approve" or "Answer"; a turn that is just running shows the icon alone. The time shown is the run's start.
- **How it posts.** The notification is tagged with the thread id, ongoing, `CATEGORY_PROGRESS`, `setRequestPromotedOngoing(true)` on Android 16, on a low-importance channel (`running_turns`: no sound, vibration or heads-up), with no progress bar, since a turn has no measurable progress. It is reposted only when what it says, or its tap target, changes. A tap opens the T3 app at the thread, as the agent page's cards do. On a process start it first clears anything it left before.
- **The connection.** `LiveUpdates` holds the shared `T3Connection` for the life of the process, so it is always on once paired. While it is Connecting or Retrying, what's shown stays until the next snapshot corrects it. Unpaired or rejected, everything clears. No foreground service.
- **Settings** says whether it's on, and offers "Allow notifications" or "Turn on Live Updates" (`ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`) for whichever is off.
- **What changed from the inherited code** (written by an earlier agent and left uncommitted, never built). It built and its tests passed as left. The changes: the log line fires only when the count changes, not on every shell change. Then the two review fixes below.

**Proof.** `boat-build.sh` passed `assembleDebug testDebugUnitTest`: 72 tests, 6 in `LiveTurnsTest`, plus F7's new `AgentQueueTest` case. On `T3_Phone_API36` (Android 16) against the scratch server, every turn was started from the CLI, another client: a running turn posted a `PROMOTED_ONGOING` notification and cleared when it ended, with home in front and with the Settings app in front. The chip showed over another app, and the promoted notification sat at the top of the shade. A tap opened the T3 app at the thread. After 15 minutes in the background, a new turn still showed within 2 s, in the same process. Details in [device-checks.md](device-checks.md).

**The unknown, answered on the emulator.** Over 15 minutes in the background with the screen on, the connection survived without a foreground service. LoR is the home app, so its process stays in state HOME rather than cached, and the cached-app freezer leaves it alone. Doze, a day, and One UI's own process rules are for the Fold (Handed on, item 6).

**Review, 2026-11-05** (an agent that didn't write the code, reading the diff). Fixed:
- R1: the environment id for the tap was cached for the life of the process, so pairing with another server kept opening threads at the old one. It now comes from `T3Connection.environmentId()`, which caches it per server, and a changed id reposts.
- R2: a side chat's approval or question raised onto an idle parent showed nothing, though that is when the chip helps most. It now counts as a turn under way (new test).

Deferred:
- R3: while the environment id can't be read, each shell change retries the HTTP call before posting. Fixed for the common case by R1's per-server cache.
- R4: granting notifications in Settings posts nothing until the next shell change. A running turn changes the shell often, so this is cosmetic.
- R5: the tap's `FLAG_ACTIVITY_LAUNCH_ADJACENT` is chosen when the notification is built, so a fold or unfold after that keeps the old choice. From the shade over another app it splits T3 beside that app. **[real Fold]**
- R6: while the connection retries (offline, Doze, the T3 host down), a finished turn keeps its update until the next snapshot. As designed, but Doze on the Fold could stretch it to hours.
- Not a bug: home's 30 s let-go no longer stops the connection, since Live Updates always holds it. The socket, pinged every 20 s, runs whenever the process does. Battery cost is for the Fold.

**Open.**
- No chip on home: Android hides an app's own Live Update chip while it is in front, and LoR is home. The small icon still shows, and the agent page lists the running turns.
- Doze wasn't tried on the emulator.

## Handed on (2026-11-05)

Sumeet closed this mission on 2026-11-05 with Increments 1 to 6 done on the emulator. Two groups of work leave it, each to be planned as its own mission later. Their text stays here so the links into it keep working.

### Real Fold checks, for a later quality mission

Every check that only the real Fold can answer. Each stays written up under its increment; this is the list.

- **1.2, all four checks.** Gesture feel on the cover and inner screens; still the default home a day later (process at setup: pid 21534, started 2026-11-05 18:18:52 EST, `device-checks.md`); fold and unfold on home and over an app; whether the side key can open a third-party default assistant.
- **3.** A few days with LoR as home, and whether it's good enough to keep. Whether One UI lets the T3 app open in a split beside home (stock Android 15 can't); if not, the agent page could move to an activity that can take a split.
- **4.** The side key opens quick send from inside another app. Whether the sheet's keyboard opens by itself. Whether a home *swipe* on home should open the sheet (on the emulator only the home key does; loosening it risks review R1's misfire).
- **5.** After a week of real use: the day timeline's 500-row limit, whether time with T3 beside home in a split counts as Life on Rails, and whether the agent page needs an attribution rule of its own.
- **6.** Whether a running turn shows in One UI's Now Bar, and whether a turn started from another client still shows after a day with the launcher in the background (on the emulator the connection lived 15 minutes without a foreground service). How long a finished turn's update outlives Doze (review R6), the battery cost of the always-on socket, and whether the tap should split T3 beside the app in front on the inner screen (review R5).
- **The T3 fork item** (Increment 4, Open questions): `onProvideAssistContent` in the fork's `MainActivity`, so quick send over T3's app picks the open thread's project.

### Agent-edited layouts: Increments 7 to 10

Agents editing the layout through a git repo, then search for people and threads, smart folders and named layouts, which build on it. [Launcher polish](../launcher-polish/plan.md) refers to these by number.

#### Increment 7: Agents edit the home layout. Status: backlog, moved out of this mission (2026-11-05)

**Goal.** The layout JSON lives in a git repo that agents edit from T3 threads. When a new commit arrives, the launcher shows what changes and applies it only when Sumeet accepts. Undo is a revert.

**Done when.** An agent commit moving an app into a folder appears as a proposed change on the emulator. Accepting applies it, and a revert puts it back.

**Unknowns.**
- Which repo holds the file, and how the phone reaches it: over the tailnet, or served through LoR.
- Whether changes made by hand on the phone are written back to the repo. The thread proposed it, but it wasn't decided. Half answered (2026-11-05): [Launcher polish](../launcher-polish/plan.md) writes every phone edit to `layout.json` (its P1), so hand edits reach the file; whether the repo receives them is this increment's.
- ~~From 3.2's review: when the file breaks, keep showing the last good layout and show the error beside it.~~ Moved to Launcher polish, Increment 3 (2026-11-05).

#### Increment 8: Search finds people and threads. Status: backlog, moved out of this mission (2026-11-05); blocked by Launcher polish Increment 5

**2026-11-05 (Sumeet).** Apps' shortcuts and conversation shortcuts moved to [Launcher polish](../launcher-polish/plan.md), Increment 5, as core launcher search. This increment keeps what only LoR and T3 can give.

**Goal.** Typing a person's name shows their LoR person card and T3 threads that mention them, beside the chats Launcher polish's search already finds for them. Choosing a result opens it.

**Done when.** On the emulator against the LoR test server and a scratch T3 server, a person and a thread appear in search and open the right screen. A JVM test covers ranking people and threads against apps and shortcuts.

**Unknowns.**
- Which LoR endpoint gives people for search, and whether T3 thread search uses `orchestration.searchThreads` (adds one method to Increment 2's set).

#### Increment 9: Smart folders. Status: backlog, moved out of this mission (2026-11-05); blocked by Increment 7 and Launcher polish Increment 2

**2026-11-05.** [Launcher polish](../launcher-polish/plan.md) Increment 2 sorts the drawer into sections on the phone and adds the schema field for them. This increment has an agent refine those sections and write their reasons.

**Goal.** An agent refines the drawer's sections (Launcher polish Increment 2's field), each with a one-line reason ("Splitwise is in Money: you open it right after paying"), by writing the layout file. Sections Sumeet edits by hand are pinned, by Launcher polish Increment 2's rule, and the agent leaves them alone. Usage data comes from LoR, which already has app sessions and attribution rules.

**Done when.** On the emulator, an agent run proposes sections as a layout change (Increment 7), each section shows its reason, and after a hand edit to one section, the next agent run leaves that section unchanged.

**Unknowns.**
- Where the sorting agent runs: a LoR job that starts a T3 thread in `assistant`, or a step of the nightly run.
- ~~Drawer folders or home folders.~~ Drawer sections, with the field added by Launcher polish Increment 2 (2026-11-05).

#### Increment 10: Named and temporary layouts. Status: backlog, moved out of this mission (2026-11-05); blocked by Increment 7

**Goal.** The layout file holds several named layouts, and Sumeet switches between them from home. An agent can propose a temporary layout with an expiry (a trip, an on-call week). It's applied only when he accepts, he can end it early, and it reverts by itself when it expires. Nothing switches on a guess.

**Done when.** On the emulator, an agent proposes a temporary layout, accepting applies it, and at the expiry the previous layout returns. Switching between two named layouts by hand works.

**Unknowns.** Whether a temporary layout is a branch, a dated file or an overlay on the base layout. Decide with Increment 7's repo design. Either way a layout needs a "previous" or base field, so an expiry knows what to return to (from 3.2's review).

## Later, outside this mission

Sumeet kept these out of this mission on 2026-11-05. Each would be planned as its own mission once this one closes:

- **Groups of apps that open together** in split view.
- **Generative widgets.** Ideally pinned T3 HTML renders, in sandboxed WebViews. This needs "Pin to home" in Sumeet's T3 fork, a change in that repo.
- **The phone as an output for agents.** A few safe tools over the T3 connection, such as showing something on the phone, pinning a card, or asking a yes/no question.

- **Sign in with T3 Connect** (Sumeet, 2026-11-05: wanted eventually, not now). Pairing needs a URL and a code; Connect's sign-in (Clerk, then the relay finds buildbot and mints credentials for the same WebSocket) would replace that. Considered and set aside the same day: going through T3's `/mcp` server instead, as Grok does. MCP can't list or approve permission requests (by design), can't stream thread state for the queue and Live Updates, and its sessions are refused on the RPC WebSocket, so it would add a second connection and login rather than simplify the first.

## Open questions for Sumeet

- **Increment 4: a T3 fork item for "the project on screen"** (handed on with the Fold checks, 2026-11-05). T3's Android app hands an assistant nothing that names the thread it shows, so quick send over it goes to `assistant` (one tap changes it). Adding `onProvideAssistContent` to the fork's `MainActivity`, setting `webUri` to the open thread's link, would make the launcher's existing rule pick the thread's project. Worth a fork item? Default until then: the Inbox.

Answered 2026-11-05: D1 is to grow LoR's app and borrow from open launchers; the Fold work waits and agents use the emulator; quick send defaults to the `assistant` project; smart folders, named and temporary layouts, and shortcut search are in this mission (apps' shortcut search and the drawer's sorting on the phone moved to Launcher polish, 2026-11-05).
