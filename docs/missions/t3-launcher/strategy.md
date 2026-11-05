---
status: draft
last_changed: "2026-11-05: Sumeet settled D1 (grow LoR's app), the Inbox (`assistant`) and scope (smart folders, named and temporary layouts, shortcut search are in)"
---

# Why

Sumeet's phone (a Galaxy Z Fold 7 on One UI 9) is where he steers agents when he is away from his machines. Today, T3 approvals, questions and failed turns are push notifications mixed in with everything else. Sending a prompt means opening the T3 app. LoR's Needs you and next action live on a web page. A home screen he owns can put the work that needs him one swipe from home, and let him fire off a prompt without opening an app. The home screen itself becomes something agents can change for him.

Sources: T3 threads "Agent-first Android launcher vision" (201b0ae4, 2026-11-02) and "Decide Launcher design" (f8deacb5, 2026-11-05).

# Outcome

LoR's Android app (`android/`, package `com.spadaval.lor`) is Sumeet's default home app on the Fold, and he uses it every day:

- **The left-most page is the agent page.** It shows the queue (T3 approvals, questions and failures; LoR Needs you and the next action), running turns, Today, and a quick-send button.
- **Home is an ordinary, good launcher.** Its layout is a JSON file in a git repo. Agents edit that file, the launcher shows each change for approval, and a revert undoes it.
- **A prompt can be sent from anywhere.** A quick-send sheet opens from home, from a Quick Settings tile, and from the side key as the default assistant, with the screen attached.
- **Running turns show as Live Updates.**
- **The LoR sensor records how each app was launched.**
- **The drawer sorts apps into smart folders,** each with its reason, and leaves folders he arranged by hand alone.
- **Search finds every app's shortcuts and conversations,** not just apps, through `LauncherApps`.
- **Layouts are named and switchable,** and an agent can propose a temporary one with an expiry (a trip, an on-call week) that removes itself.

A partial outcome worth shipping: the agent page and quick send in a home app he keeps as default, even before the agent-edited layout exists.

# Shape

- **For Sumeet only, on his Fold.** No distribution, no Play policy, no onboarding. Sideload-only and adb-granted permissions are allowed. LoR is assumed in full.
- **A separate native app, grown from LoR's.** It is not a feature of his T3 Code fork and does not replace the T3 app. Deep thread work goes to the T3 app through deep links, and on the inner screen the T3 app opens in the adjacent pane. Decided (D1 in [plan.md](plan.md)): grow LoR's app, and read and borrow from open launchers (Lawnchair, Kvaesitso and others) rather than building every launcher basic from scratch.
- **It is a client of both servers and changes neither server's role.** It speaks a small part of T3 Code's WebSocket protocol directly in Kotlin: Effect RPC JSON over WebSocket, with session auth paired like the mobile app. It calls `orchestration.subscribeShell`, `orchestration.subscribeThread` or `getThreadProjection`, `orchestration.dispatchCommand` (approve, answer, follow-up) and `orchestration.launchThread`. T3 is the reference: `apps/server/src/ws.ts`, `packages/contracts/src/orchestrationV2.ts` and `rpc.ts`, and `packages/client-runtime`. The launcher reads that code and never changes it.
- **T3 and LoR cards meet only in the launcher.** LoR's server gains no approval queue and approves nothing (spec, "Who is who"). It may expose what the agent page reads, such as Needs you and the brief's first line, through `/device/*`.
- **Prompts go through T3.** A prompt starts a new thread, in the first of: the project named in the prompt, the project on screen, or the `assistant` project on buildbot, which runs LoR's agent and can move coding work to the right project.
- **Native and instant.** Home must answer at once and never take the phone down with it. A choice that costs home-press latency loses to one that doesn't.
- **LoR's rules hold.** Calm, never guilt-shaped, no continuously animating UI. What an agent changes can be seen and undone.

# Scope

In: the home app on the Fold's cover and inner screens. That covers the agent page, home from a layout file, the app drawer and search, quick send (sheet, tile, home button, assistant role), Live Updates, the launch-source sensor, the agent-edited layout repo, smart folders, shortcut and conversation search, named and temporary layouts, and the LoR server endpoints the app reads.

Out:
- Notification triage and Do Not Disturb takeover. Android's own notification management stays.
- Accessibility-service agents acting inside apps. Context for "this" comes from the assistant's screen capture.
- Root, and replacing Recents or gesture navigation.
- One UI's inner-screen taskbar, which Sumeet turns off anyway.
- Running a T3 server on the phone.
- DeX is not pursued. The thread called it "probably not", and it was never decided.
- Later missions (Sumeet, 2026-11-05): app groups that open together in split view, generative widgets, and the phone as an output for agents.
- Changes to the T3 Code repo or his fork. Any fork change the launcher needs, such as a deep link that doesn't exist yet, is a separate item in that repo.

Environment: the real Fold 7 for anything One UI decides. The local emulator (`T3_Phone_API35`, a plain phone on API 35) for everything else. Heavy Gradle builds run on boat (the `boat` skill), not buildbot. Builds go on Sumeet's phone only when he says so.

# Authority

Without asking, the orchestrator may change the increment breakdown and order, add or split issues, and make choices inside one increment (libraries, module layout, card wording). It records each with a decision note.

These come back to Sumeet:
- D1 and any other decision record.
- Installing anything on his phone.
- Adding a permission that needs an adb grant.
- Anything that changes LoR's server behaviour beyond new read endpoints for the app.
- Adding to or dropping from the Outcome.

# Risks and Unknowns

- **One UI may not treat a third-party home well.** The risks are rough gesture handoff, being reset to One UI Home, battery killing, and odd handling of the two screens. Increment 1 measures this on the device before any heavy work.
- **The T3 protocol may change upstream** (`orchestrationV2`). A Kotlin check against a real T3 server catches it.
- **Holding a live T3 connection in the background on One UI** is unproven. The launcher is expected to be killed less often than other apps, with Unrestricted battery. Increment 1 checks only that the process survives. Increment 6 checks the connection itself.
- **The Fold arrives on 2026-11-05, but work stays on the emulator until Sumeet says otherwise.** Every device check marked "real Fold" waits for him.
